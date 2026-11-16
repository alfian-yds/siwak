package com.mosque.prayerclock.ui.server

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.server.ApiResult
import com.mosque.prayerclock.server.PairingStartRequest
import com.mosque.prayerclock.server.ServerState
import com.mosque.prayerclock.server.ServerStore
import com.mosque.prayerclock.server.ServerSyncManager
import com.mosque.prayerclock.server.SiwakAgentService
import com.mosque.prayerclock.server.SiwakApi
import com.mosque.prayerclock.service.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

sealed class PairingUi {
    data object Loading : PairingUi()

    data class ShowCode(val code: String, val expiresAtMs: Long) : PairingUi()

    data class Error(val message: String) : PairingUi()

    data object Connecting : PairingUi()

    data object Done : PairingUi()
}

/** Layar Hubungkan (App Flow §2.2): minta kode, tampilkan, periksa tiap 3 detik, ganti bila kedaluwarsa. */
@HiltViewModel
class PairingViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: ServerStore,
        private val api: SiwakApi,
        private val sync: ServerSyncManager,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<PairingUi>(PairingUi.Loading)
        val ui: StateFlow<PairingUi> = _ui.asStateFlow()
        val server: StateFlow<ServerState?> = store.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        private var job: Job? = null

        fun start() {
            job?.cancel()
            job = viewModelScope.launch { pairLoop() }
        }

        fun setServerUrl(url: String) {
            viewModelScope.launch {
                store.setServerUrl(url)
                start()
            }
        }

        private suspend fun pairLoop() {
            while (viewModelScope.isActive) {
                _ui.value = PairingUi.Loading
                val baseUrl = store.snapshot().serverUrl
                val nonce = randomNonce()
                val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80)
                val started = api.startPairing(baseUrl, PairingStartRequest(nonce, BuildConfig.VERSION_NAME, model))
                val data = (started as? ApiResult.Ok)?.data
                if (data?.pairingId == null || data.code == null) {
                    _ui.value = PairingUi.Error(describe(started, baseUrl))
                    delay(10_000)
                    continue
                }
                val expiresAt = SystemClock.elapsedRealtime() + parseRemainingMs(data.expiresAt)
                _ui.value = PairingUi.ShowCode(data.code, expiresAt)

                while (SystemClock.elapsedRealtime() < expiresAt) {
                    delay(3_000)
                    val res = api.pairingStatus(baseUrl, data.pairingId, nonce)
                    val st = (res as? ApiResult.Ok)?.data ?: continue
                    when (st.status) {
                        "claimed" -> {
                            if (st.deviceId != null && st.deviceToken != null) {
                                finish(st.deviceId, st.deviceToken)
                                return
                            }
                        }
                        "expired" -> break
                    }
                }
                // Kode kedaluwarsa: minta kode baru otomatis (TRD §6).
            }
        }

        private suspend fun finish(
            deviceId: String,
            token: String,
        ) {
            _ui.value = PairingUi.Connecting
            store.savePairing(deviceId, token)
            // Ambil pengaturan pertama sekarang agar layar langsung menampilkan data masjid.
            runCatching { sync.syncOnce(force = true) }
            SiwakAgentService.start(context)
            _ui.value = PairingUi.Done
        }

        private fun describe(
            res: ApiResult<*>,
            baseUrl: String,
        ): String =
            when (res) {
                is ApiResult.NetworkError -> "Tidak bisa menghubungi $baseUrl. Periksa internet TV dan alamat server."
                is ApiResult.HttpError -> res.message ?: "Server menolak permintaan (${res.status})."
                else -> "Jawaban server tidak dikenali."
            }

        private fun randomNonce(): String {
            val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
            return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        /** Sisa waktu kode dari jam server; jam TV yang meleset tidak memengaruhi. */
        private fun parseRemainingMs(iso: String?): Long {
            val parsed =
                runCatching {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .parse(iso!!)!!
                        .time
                }.getOrNull()
            val remaining = parsed?.let { it - System.currentTimeMillis() } ?: 0L
            return if (remaining in 30_000L..(15 * 60_000L)) remaining else 10 * 60_000L
        }
    }

/** PIN dan Pengaturan terbatas untuk TV terhubung (F-TV-17, TRD §10). */
@HiltViewModel
class ConnectedSettingsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: ServerStore,
        private val sync: ServerSyncManager,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val server: StateFlow<ServerState?> = store.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        val status = sync.status

        private val _unlocked = MutableStateFlow(false)
        val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()
        private val _pinMessage = MutableStateFlow<String?>(null)
        val pinMessage: StateFlow<String?> = _pinMessage.asStateFlow()
        private var wrong = 0
        private var lockedUntil = 0L

        fun checkPin(pin: String) {
            val now = SystemClock.elapsedRealtime()
            if (now < lockedUntil) {
                _pinMessage.value = "Terlalu banyak percobaan. Coba lagi dalam ${((lockedUntil - now) / 60_000) + 1} menit."
                return
            }
            val s = server.value ?: return
            val salt = s.pinSalt
            val hash = s.pinHash
            if (salt == null || hash == null) {
                // PIN belum diterima dari server (sinkron pertama belum selesai).
                _pinMessage.value = "PIN belum diterima dari server. Tunggu sinkron atau periksa internet TV."
                return
            }
            if (sha256Hex(salt + pin).equals(hash, ignoreCase = true)) {
                wrong = 0
                _pinMessage.value = null
                _unlocked.value = true
            } else {
                wrong++
                if (wrong >= 5) {
                    wrong = 0
                    lockedUntil = now + 5 * 60_000
                    _pinMessage.value = "PIN salah 5 kali. Menu terkunci 5 menit. PIN bisa dilihat di admin web."
                } else {
                    _pinMessage.value = "PIN salah. Sisa ${5 - wrong} percobaan."
                }
            }
        }

        fun lock() {
            _unlocked.value = false
        }

        fun syncNow() {
            sync.requestSync()
            viewModelScope.launch { runCatching { sync.syncOnce(force = true) } }
        }

        fun testSound(adzan: Boolean) {
            viewModelScope.launch {
                val settings = settingsRepository.getSettings().first()
                val sm = SoundManager(context)
                if (adzan) sm.playAzanSound(settings, PrayerType.DHUHR) else sm.playIqamahSound(settings, PrayerType.DHUHR)
            }
        }

        fun unpair(onDone: () -> Unit) {
            viewModelScope.launch {
                sync.unpairFromTv()
                onDone()
            }
        }

        private fun sha256Hex(s: String): String =
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

/** Keadaan koneksi untuk navigasi utama: TV terhubung atau mode mandiri. */
@HiltViewModel
class AppServerViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        store: ServerStore,
        sync: ServerSyncManager,
    ) : ViewModel() {
        val server: StateFlow<ServerState?> = store.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        val unpairedEvents: StateFlow<Int> = sync.unpairedEvents

        /** Memastikan layanan latar berjalan bila TV terhubung (dipanggil saat SIWAK tampil). */
        fun ensureAgent() {
            viewModelScope.launch { if (server.first { it != null }!!.isPaired) SiwakAgentService.start(context) }
        }
    }

/** Membandingkan versi "2.0.0-alpha" secara numerik pada tiga angka pertama. */
fun isOlderVersion(
    current: String,
    minimum: String,
): Boolean {
    fun nums(v: String) = Regex("[0-9]+").findAll(v.substringBefore('-')).map { it.value.toInt() }.toList() + listOf(0, 0, 0)
    val a = nums(current)
    val b = nums(minimum)
    for (i in 0 until 3) if (a[i] != b[i]) return a[i] < b[i]
    return false
}
