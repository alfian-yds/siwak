package com.mosque.prayerclock.server

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.MainActivity
import com.mosque.prayerclock.SplashActivity
import com.mosque.prayerclock.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Sinkronisasi TV dengan server (TRD §7): heartbeat 60 detik, bundle bila versi berubah,
 * cuaca bila stempel berubah, perintah jarak jauh (§7.4), dan "Selalu kembali ke SIWAK".
 * Dijalankan oleh [SiwakAgentService] agar tetap hidup saat SIWAK di latar belakang.
 */
@Singleton
class ServerSyncManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: ServerStore,
        private val api: SiwakApi,
        private val settingsRepository: SettingsRepository,
        private val contentRepository: ServerContentRepository,
    ) {
        companion object {
            private const val TAG = "SiwakSync"
            const val HEARTBEAT_MS = 60_000L
            const val SLOW_HEARTBEAT_MS = 120_000L
            private const val TICK_MS = 30_000L
            private const val OPEN_CHECK_MS = 4_000L
            private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

            fun iso(ms: Long): String = synchronized(ISO) { ISO.format(Date(ms)) }
        }

        /** Keadaan untuk layar Pengaturan terbatas: kontak terakhir & galat terakhir. */
        data class Status(val lastOkAt: Long = 0, val lastError: String? = null, val syncing: Boolean = false)

        private val _status = MutableStateFlow(Status())
        val status: StateFlow<Status> = _status.asStateFlow()

        /** Dipanggil saat TV dilepas dari server (oleh server atau dari menu TV). */
        val unpairedEvents = MutableStateFlow(0)

        private val mutex = Mutex()
        private val gson = Gson()
        private var failures = 0
        private var unauthorizedCount = 0
        private var lastHeartbeatAt = 0L
        private var lastAutoReturnAt = 0L
        private var forceNext = false

        /** Loop utama; berhenti sendiri bila TV tidak lagi terhubung. */
        suspend fun run() {
            while (coroutineContext.isActive) {
                val state = store.snapshot()
                if (!state.isPaired) return
                val interval = if (failures >= 3) SLOW_HEARTBEAT_MS else HEARTBEAT_MS
                val now = SystemClock.elapsedRealtime()
                if (forceNext || lastHeartbeatAt == 0L || now - lastHeartbeatAt >= interval) {
                    lastHeartbeatAt = now
                    try {
                        syncOnce(force = forceNext)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        // Termasuk Error (bukan hanya Exception): loop sinkron tidak boleh mati diam-diam.
                        Log.e(TAG, "sinkron gagal", e)
                        _status.value = _status.value.copy(lastError = e.message, syncing = false)
                    }
                }
                refreshRunningTextsForToday(state)
                checkAutoReturn(state)
                delay(TICK_MS)
            }
        }

        /** Minta sinkron secepatnya (tombol "Sinkron sekarang" di TV, atau setelah terhubung). */
        fun requestSync() {
            forceNext = true
            lastHeartbeatAt = 0L
        }

        /** Satu putaran: heartbeat → (bundle) → (cuaca) → perintah. */
        suspend fun syncOnce(force: Boolean = false) =
            mutex.withLock {
                forceNext = false
                val state = store.snapshot()
                val token = state.deviceToken ?: return@withLock
                _status.value = _status.value.copy(syncing = true)

                val pending = store.pendingResults()
                val request =
                    HeartbeatRequest(
                        appVersion = BuildConfig.VERSION_NAME,
                        configVersion = state.configVersion,
                        contentVersion = state.contentVersion,
                        weatherStamp = state.weatherStamp,
                        deviceTime = iso(System.currentTimeMillis()),
                        health = HealthReport(_status.value.lastError?.take(300), context.filesDir.usableSpace / (1024 * 1024)),
                        app =
                            AppStateReport(
                                foreground = AppForegroundTracker.isForeground,
                                since = iso(AppForegroundTracker.since),
                                canOpenApp = AppForegroundTracker.canOpenFromBackground(context),
                            ),
                        commandResults = pending,
                    )

                when (val res = api.heartbeat(state.serverUrl, token, request)) {
                    is ApiResult.Ok -> {
                        failures = 0
                        unauthorizedCount = 0
                        store.removePendingResults(pending.map { it.id }.toSet())
                        store.markContact(res.data.minAppVersion)
                        val hb = res.data
                        val bundleChanged =
                            force || hb.configVersion != state.configVersion || hb.contentVersion != state.contentVersion
                        var error: String? = null
                        if (bundleChanged) error = applyBundle(state.serverUrl, token)
                        if (force || hb.weatherStamp != state.weatherStamp) fetchWeather(state.serverUrl, token, hb.weatherStamp)
                        _status.value = Status(lastOkAt = System.currentTimeMillis(), lastError = error, syncing = false)
                        hb.commands.orEmpty().forEach { handleCommand(it) }
                    }
                    is ApiResult.HttpError -> {
                        _status.value = _status.value.copy(lastError = res.message ?: "HTTP ${res.status}", syncing = false)
                        // Server menyatakan TV ini tidak terdaftar lagi → kembali ke mode mandiri (F-TV-14).
                        // Dua kali berturut-turut agar gangguan sesaat tidak melepas TV.
                        if (res.status == 401 && ++unauthorizedCount >= 2) becomeStandalone()
                        failures++
                    }
                    is ApiResult.NetworkError -> {
                        failures++
                        _status.value = _status.value.copy(lastError = res.message, syncing = false)
                    }
                }
            }

        /** Ambil bundle lalu terapkan atomik. Mengembalikan pesan galat, atau null bila berhasil. */
        private suspend fun applyBundle(
            serverUrl: String,
            token: String,
        ): String? {
            return when (val res = api.bundle(serverUrl, token)) {
                is ApiResult.Ok -> {
                    val bundle = res.data
                    Log.i(TAG, "bundle diterima: versi ${bundle.configVersion}, ${bundle.slides?.size ?: 0} slide, ${bundle.media?.size ?: 0} media")
                    // Atomik (TRD §7.2): semua media diunduh dulu; bila ada yang gagal, bundle lama tetap dipakai.
                    val files = contentRepository.syncMedia(serverUrl, token, bundle.media.orEmpty())
                        ?: return "Gagal mengunduh media; tampilan lama dipertahankan."
                    val current = settingsRepository.getSettings().first()
                    val next = BundleMapper.toAppSettings(current, bundle, today(bundle.mosque?.timezone)) { id -> id?.let(files::get) }
                    settingsRepository.applyServerSettings(next, BundleMapper.affectsPrayerTimes(current, next))
                    contentRepository.save(bundle, files)
                    store.saveBundle(bundle)
                    Log.i(TAG, "bundle diterapkan: versi ${bundle.configVersion}")
                    null
                }
                is ApiResult.HttpError -> (res.message ?: "Bundle ditolak (${res.status})").also { Log.w(TAG, "bundle gagal: $it") }
                is ApiResult.NetworkError -> res.message.also { Log.w(TAG, "bundle gagal: $it") }
            }
        }

        private suspend fun fetchWeather(
            serverUrl: String,
            token: String,
            stamp: String?,
        ) {
            if (stamp == null) return
            when (val res = api.weatherJson(serverUrl, token)) {
                is ApiResult.Ok -> store.saveWeather(stamp, res.data)
                else -> Log.w(TAG, "cuaca belum tersedia: $res")
            }
        }

        // ---- Perintah jarak jauh (TRD §7.4) ------------------------------------------

        private suspend fun handleCommand(cmd: RemoteCommand) {
            val id = cmd.id ?: return
            Log.i(TAG, "perintah ${cmd.type} ($id)")
            when (cmd.type) {
                "open_app" -> {
                    val result = openApp()
                    store.addPendingResult(CommandResult(id, result == null, result))
                }
                "sync_now" -> {
                    requestSync()
                    store.addPendingResult(CommandResult(id, true, null))
                }
                "restart_app" -> {
                    if (!AppForegroundTracker.canOpenFromBackground(context)) {
                        store.addPendingResult(
                            CommandResult(id, false, "TV belum memberi izin Tampil di atas aplikasi lain, jadi SIWAK tidak bisa dibuka lagi setelah ditutup."),
                        )
                        return
                    }
                    // Hasil dicatat lebih dulu; dikirim pada heartbeat pertama setelah SIWAK menyala lagi.
                    store.addPendingResult(CommandResult(id, true, null))
                    restartApp()
                }
                else -> store.addPendingResult(CommandResult(id, false, "Perintah tidak dikenal oleh versi aplikasi ini"))
            }
            // Laporkan hasil secepatnya, bukan menunggu 60 detik.
            lastHeartbeatAt = 0L
        }

        /** Membawa SIWAK ke depan. Mengembalikan null bila berhasil, atau pesan galat. */
        suspend fun openApp(): String? {
            if (AppForegroundTracker.isForeground) return null
            try {
                val intent =
                    Intent(context, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                context.startActivity(intent)
            } catch (e: Exception) {
                return "Gagal membuka SIWAK: ${e.message}"
            }
            delay(OPEN_CHECK_MS)
            return if (AppForegroundTracker.isForeground) {
                null
            } else if (!AppForegroundTracker.canOpenFromBackground(context)) {
                "Android menolak membuka SIWAK dari latar belakang. Beri izin Tampil di atas aplikasi lain di TV."
            } else {
                "SIWAK belum tampil di layar. TV mungkin sedang mati atau dalam mode siaga."
            }
        }

        private fun restartApp() {
            val intent =
                Intent(context, SplashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val flags = PendingIntent.FLAG_CANCEL_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, 4017, intent, flags)
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2_000, pi)
            Log.i(TAG, "memulai ulang SIWAK")
            Process.killProcess(Process.myPid())
        }

        /** F-TV-20: buka SIWAK lagi bila terlalu lama di latar belakang. */
        private suspend fun checkAutoReturn(state: ServerState) {
            if (!state.autoReturnEnabled || AppForegroundTracker.isForeground) return
            val now = System.currentTimeMillis()
            val limit = state.autoReturnMinutes * 60_000L
            if (now - AppForegroundTracker.since < limit || now - lastAutoReturnAt < limit) return
            lastAutoReturnAt = now
            val err = openApp()
            Log.i(TAG, "selalu kembali ke SIWAK: ${err ?: "berhasil"}")
        }

        /** Teks berjalan bertanggal ikut berganti hari walau TV sedang offline. */
        private suspend fun refreshRunningTextsForToday(state: ServerState) {
            val type = object : TypeToken<List<RunningTextDto>>() {}.type
            val texts = runCatching { gson.fromJson<List<RunningTextDto>>(state.runningTextsJson, type) }.getOrNull() ?: return
            val wanted = BundleMapper.runningTextMessages(texts, today(null))
            val current = settingsRepository.getSettings().first().runningTextMessages
            if (wanted != current) settingsRepository.updateRunningTextMessages(wanted)
        }

        private fun today(timezone: String?): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone(timezone ?: TimeZone.getDefault().id) }
                .format(Date())

        /** Kembali ke mode mandiri dengan pengaturan terakhir. */
        suspend fun becomeStandalone() {
            Log.w(TAG, "TV dilepas dari server; kembali ke mode mandiri")
            store.clearPairing()
            contentRepository.clear()
            unpairedEvents.value++
            SiwakAgentService.stop(context)
        }

        /** Menu TV "Lepas dari server": beri tahu server bila bisa, lalu lepas. */
        suspend fun unpairFromTv() {
            val s = store.snapshot()
            s.deviceToken?.let { api.unpair(s.serverUrl, it) }
            becomeStandalone()
        }
    }
