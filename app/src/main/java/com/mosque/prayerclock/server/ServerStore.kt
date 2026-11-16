package com.mosque.prayerclock.server

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mosque.prayerclock.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverDataStore by preferencesDataStore(name = "siwak_server")

/** Keadaan koneksi TV ke server SIWAK (TRD §11.1 `ServerStore`). */
data class ServerState(
    val serverUrl: String,
    val deviceId: String?,
    val deviceToken: String?,
    val mosqueName: String?,
    val deviceName: String?,
    val configVersion: Long?,
    val contentVersion: Long?,
    val weatherStamp: String?,
    val pinSalt: String?,
    val pinHash: String?,
    val autoReturnEnabled: Boolean,
    val autoReturnMinutes: Int,
    val lastSyncAt: Long,
    val lastContactAt: Long,
    val minAppVersion: String?,
    val runningTextsJson: String,
    val weatherJson: String,
) {
    val isPaired: Boolean get() = !deviceToken.isNullOrBlank()
}

/**
 * Data server disimpan terpisah dari pengaturan tampilan (`AppSettings`) agar melepas TV tidak
 * menghapus pengaturan terakhir (F-TV-14). Kunci perangkat dilindungi sandbox aplikasi (TRD §11.2).
 */
@Singleton
class ServerStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val gson = Gson()

        private object Keys {
            val SERVER_URL = stringPreferencesKey("server_url")
            val DEVICE_ID = stringPreferencesKey("device_id")
            val DEVICE_TOKEN = stringPreferencesKey("device_token")
            val MOSQUE_NAME = stringPreferencesKey("mosque_name")
            val DEVICE_NAME = stringPreferencesKey("device_name")
            val CONFIG_VERSION = longPreferencesKey("config_version")
            val CONTENT_VERSION = longPreferencesKey("content_version")
            val WEATHER_STAMP = stringPreferencesKey("weather_stamp")
            val PIN_SALT = stringPreferencesKey("pin_salt")
            val PIN_HASH = stringPreferencesKey("pin_hash")
            val AUTO_RETURN_ENABLED = booleanPreferencesKey("auto_return_enabled")
            val AUTO_RETURN_MINUTES = intPreferencesKey("auto_return_minutes")
            val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
            val LAST_CONTACT_AT = longPreferencesKey("last_contact_at")
            val MIN_APP_VERSION = stringPreferencesKey("min_app_version")
            val RUNNING_TEXTS_JSON = stringPreferencesKey("running_texts_json")
            val WEATHER_JSON = stringPreferencesKey("weather_json")
            val PENDING_RESULTS_JSON = stringPreferencesKey("pending_results_json")
        }

        val defaultServerUrl: String = BuildConfig.SIWAK_SERVER_URL

        val state: Flow<ServerState> =
            context.serverDataStore.data.map { p ->
                ServerState(
                    serverUrl = p[Keys.SERVER_URL]?.takeIf { it.isNotBlank() } ?: defaultServerUrl,
                    deviceId = p[Keys.DEVICE_ID],
                    deviceToken = p[Keys.DEVICE_TOKEN],
                    mosqueName = p[Keys.MOSQUE_NAME],
                    deviceName = p[Keys.DEVICE_NAME],
                    configVersion = p[Keys.CONFIG_VERSION],
                    contentVersion = p[Keys.CONTENT_VERSION],
                    weatherStamp = p[Keys.WEATHER_STAMP],
                    pinSalt = p[Keys.PIN_SALT],
                    pinHash = p[Keys.PIN_HASH],
                    autoReturnEnabled = p[Keys.AUTO_RETURN_ENABLED] ?: false,
                    autoReturnMinutes = p[Keys.AUTO_RETURN_MINUTES] ?: 10,
                    lastSyncAt = p[Keys.LAST_SYNC_AT] ?: 0L,
                    lastContactAt = p[Keys.LAST_CONTACT_AT] ?: 0L,
                    minAppVersion = p[Keys.MIN_APP_VERSION],
                    runningTextsJson = p[Keys.RUNNING_TEXTS_JSON] ?: "[]",
                    weatherJson = p[Keys.WEATHER_JSON] ?: "",
                )
            }

        suspend fun snapshot(): ServerState = state.first()

        suspend fun setServerUrl(url: String) {
            context.serverDataStore.edit { it[Keys.SERVER_URL] = url.trim().trimEnd('/') }
        }

        suspend fun savePairing(deviceId: String, token: String) {
            context.serverDataStore.edit {
                it[Keys.DEVICE_ID] = deviceId
                it[Keys.DEVICE_TOKEN] = token
                // Versi dikosongkan agar heartbeat pertama memicu pengambilan bundle.
                it.remove(Keys.CONFIG_VERSION)
                it.remove(Keys.CONTENT_VERSION)
                it.remove(Keys.WEATHER_STAMP)
            }
        }

        /** Menyimpan bagian bundle yang dipakai di luar `AppSettings`. */
        suspend fun saveBundle(bundle: BundleDto) {
            context.serverDataStore.edit {
                bundle.configVersion?.let { v -> it[Keys.CONFIG_VERSION] = v }
                bundle.contentVersion?.let { v -> it[Keys.CONTENT_VERSION] = v }
                bundle.mosque?.name?.let { v -> it[Keys.MOSQUE_NAME] = v }
                bundle.device?.name?.let { v -> it[Keys.DEVICE_NAME] = v }
                bundle.device?.pinSalt?.let { v -> it[Keys.PIN_SALT] = v }
                bundle.device?.pinHash?.let { v -> it[Keys.PIN_HASH] = v }
                val auto = bundle.settings?.kiosk?.autoReturn
                it[Keys.AUTO_RETURN_ENABLED] = auto?.enabled ?: false
                it[Keys.AUTO_RETURN_MINUTES] = (auto?.minutes ?: 10).coerceIn(1, 120)
                it[Keys.RUNNING_TEXTS_JSON] = gson.toJson(bundle.runningTexts ?: emptyList<RunningTextDto>())
                it[Keys.LAST_SYNC_AT] = System.currentTimeMillis()
            }
        }

        suspend fun saveWeather(stamp: String?, json: String) {
            context.serverDataStore.edit {
                if (stamp != null) it[Keys.WEATHER_STAMP] = stamp else it.remove(Keys.WEATHER_STAMP)
                it[Keys.WEATHER_JSON] = json
            }
        }

        suspend fun markContact(minAppVersion: String?) {
            context.serverDataStore.edit {
                it[Keys.LAST_CONTACT_AT] = System.currentTimeMillis()
                if (minAppVersion != null) it[Keys.MIN_APP_VERSION] = minAppVersion
            }
        }

        /** Melepas TV dari server. Alamat server dipertahankan; pengaturan tampilan tidak disentuh. */
        suspend fun clearPairing() {
            context.serverDataStore.edit { p ->
                val keep = p[Keys.SERVER_URL]
                p.clear()
                if (keep != null) p[Keys.SERVER_URL] = keep
            }
        }

        // ---- Hasil perintah yang menunggu dikirim (bertahan walau proses dimulai ulang) ----

        private val resultListType = object : TypeToken<List<CommandResult>>() {}.type

        private fun MutablePreferences.readResults(): MutableList<CommandResult> =
            this[Keys.PENDING_RESULTS_JSON]
                ?.let { runCatching { gson.fromJson<List<CommandResult>>(it, resultListType) }.getOrNull() }
                ?.toMutableList() ?: mutableListOf()

        suspend fun addPendingResult(result: CommandResult) {
            context.serverDataStore.edit { p ->
                val list = p.readResults().filterNot { it.id == result.id }.toMutableList()
                list.add(result)
                p[Keys.PENDING_RESULTS_JSON] = gson.toJson(list.takeLast(20))
            }
        }

        suspend fun pendingResults(): List<CommandResult> {
            val prefs: Preferences = context.serverDataStore.data.first()
            return prefs[Keys.PENDING_RESULTS_JSON]
                ?.let { runCatching { gson.fromJson<List<CommandResult>>(it, resultListType) }.getOrNull() }
                ?: emptyList()
        }

        suspend fun removePendingResults(ids: Set<String>) {
            if (ids.isEmpty()) return
            context.serverDataStore.edit { p ->
                p[Keys.PENDING_RESULTS_JSON] = gson.toJson(p.readResults().filterNot { it.id in ids })
            }
        }
    }
