package com.mosque.prayerclock.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.AppTheme
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.SoundType
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val prayerTimesRepository: PrayerTimesRepository,
        private val hijriDateRepository: HijriDateRepository,
        private val weatherRepository: WeatherRepository,
        @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    ) : ViewModel() {
        val settings =
            settingsRepository
                .getSettings()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AppSettings(),
                )

        // SIWAK: status sinkronisasi manual
        private val _isSyncingPrayer = MutableStateFlow(false)
        val isSyncingPrayer: StateFlow<Boolean> = _isSyncingPrayer.asStateFlow()

        private val _isSyncingWeather = MutableStateFlow(false)
        val isSyncingWeather: StateFlow<Boolean> = _isSyncingWeather.asStateFlow()

        fun syncPrayerTimesNow() {
            viewModelScope.launch {
                _isSyncingPrayer.value = true
                try {
                    prayerTimesRepository.invalidatePrayerTimesCache()
                    // Paksa ambil ulang dari jaringan (jalur AlAdhan koordinat selalu network)
                    prayerTimesRepository
                        .getTodayPrayerTimesFromSettings()
                        .first { it !is NetworkResult.Loading }
                    // SIWAK: prefetch 7 hari ke depan agar tersedia offline seminggu penuh
                    val s = settings.value
                    prayerTimesRepository.prefetchWeekPrayerTimes(s.latitude, s.longitude)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Sync jadwal gagal: ${e.message}")
                } finally {
                    _isSyncingPrayer.value = false
                }
            }
        }

        fun syncWeatherNow() {
            viewModelScope.launch {
                _isSyncingWeather.value = true
                try {
                    val s = settings.value
                    // SIWAK: pakai jalur yang sama dgn layar utama (WeatherAPI/OpenWeather), BUKAN
                    // getCurrentWeatherByCity (yang menuju MosqueClock backend Sri Lanka & gagal)
                    // Pakai jalur koordinat (WeatherAPI -> OpenWeatherMap) dgn lokasi masjid
                    weatherRepository
                        .refreshWeatherByLocation(s.latitude, s.longitude)
                        .first { it !is NetworkResult.Loading }
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Sync cuaca gagal: ${e.message}")
                } finally {
                    _isSyncingWeather.value = false
                }
            }
        }

        fun updateLanguage(language: Language) {
            viewModelScope.launch {
                settingsRepository.updateLanguage(language)
            }
        }

        fun updateCity(city: String) {
            viewModelScope.launch {
                settingsRepository.updateCity(city)
            }
        }

        fun updateLocationCity(
            city: String,
            latitude: Double,
            longitude: Double,
        ) {
            viewModelScope.launch {
                settingsRepository.updateLocationCity(city, latitude, longitude)
            }
        }

        fun updateCoordinates(
            latitude: Double,
            longitude: Double,
        ) {
            viewModelScope.launch {
                settingsRepository.updateCoordinates(latitude, longitude)
            }
        }

        fun updateCountry(country: String) {
            viewModelScope.launch {
                settingsRepository.updateCountry(country)
            }
        }

        fun updateMosqueName(mosqueName: String) {
            viewModelScope.launch {
                settingsRepository.updateMosqueName(mosqueName)
            }
        }

        fun updateShowIqamahCycle(show: Boolean) {
            viewModelScope.launch { settingsRepository.updateShowIqamahCycle(show) }
        }

        fun updatePrayerOffsetEnabled(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.updatePrayerOffsetEnabled(enabled) }
        }

        fun updateClockType(clockType: ClockType) {
            viewModelScope.launch {
                settingsRepository.updateClockType(clockType)
            }
        }

        fun updateTheme(theme: AppTheme) {
            viewModelScope.launch {
                settingsRepository.updateTheme(theme)
            }
        }

        fun updateShowSeconds(showSeconds: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateShowSeconds(showSeconds)
            }
        }

        fun updateShow24HourFormat(show24Hour: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateShow24HourFormat(show24Hour)
            }
        }

        fun updateManualTime(
            timeType: String,
            time: String,
        ) {
            viewModelScope.launch {
                settingsRepository.updateManualTime(timeType, time)
            }
        }

        fun updateIqamahGap(
            prayerType: String,
            gap: Int,
        ) {
            viewModelScope.launch {
                settingsRepository.updateIqamahGap(prayerType, gap)
            }
        }

        fun updatePrayerOffset(
            prayerType: String,
            minutes: Int,
        ) {
            viewModelScope.launch {
                settingsRepository.updatePrayerOffset(prayerType, minutes)
            }
        }

        fun updateWeatherCity(weatherCity: String) {
            viewModelScope.launch {
                settingsRepository.updateWeatherCity(weatherCity)
            }
        }

        fun updateWeatherCountry(weatherCountry: String) {
            viewModelScope.launch {
                settingsRepository.updateWeatherCountry(weatherCountry)
            }
        }

        fun updateWeatherProvider(provider: WeatherProvider) {
            viewModelScope.launch {
                settingsRepository.updateWeatherProvider(provider)
            }
        }

        fun updateShowWeather(showWeather: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateShowWeather(showWeather)
            }
        }

        fun updatePrayerServiceType(serviceType: PrayerServiceType) {
            viewModelScope.launch {
                settingsRepository.updatePrayerServiceType(serviceType)
            }
        }

        fun updateSelectedZone(zone: Int) {
            viewModelScope.launch {
                settingsRepository.updateSelectedZone(zone)
            }
        }

        fun updateSelectedRegion(region: String) {
            viewModelScope.launch {
                settingsRepository.updateSelectedRegion(region)
            }
        }

        fun updateUseApiForHijriDate(useApi: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateUseApiForHijriDate(useApi)
            }
        }

        fun updateHijriProvider(provider: HijriProvider) {
            viewModelScope.launch {
                settingsRepository.updateHijriProvider(provider)
            }
        }

        fun updateHijriDate(
            day: Int,
            month: Int,
            year: Int,
        ) {
            viewModelScope.launch {
                val currentDate =
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                settingsRepository.updateHijriDate(
                    day,
                    month,
                    year,
                    currentDate.toString(),
                )
            }
        }

        fun updateSoundEnabled(soundEnabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateSoundEnabled(soundEnabled)
            }
        }

        fun updateFullScreenCountdownEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateFullScreenCountdownEnabled(enabled)
            }
        }

        fun updateFullScreenCountdownMinutes(minutes: Int) {
            viewModelScope.launch {
                settingsRepository.updateFullScreenCountdownMinutes(minutes)
            }
        }

        fun updateShowSunrise(show: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateShowSunrise(show)
            }
        }

        fun updateShowImsak(show: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateShowImsak(show)
            }
        }

        fun updateClockSizePercent(percent: Int) {
            viewModelScope.launch {
                settingsRepository.updateClockSizePercent(percent)
            }
        }

        fun updateHijriDayOffset(offset: Int) {
            viewModelScope.launch {
                settingsRepository.updateHijriDayOffset(offset)
            }
        }

        fun updateNightModeEnabled(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.updateNightModeEnabled(enabled) }
        }

        fun updateNightStartHour(hour: Int) {
            viewModelScope.launch { settingsRepository.updateNightStartHour(hour) }
        }

        fun updateNightEndHour(hour: Int) {
            viewModelScope.launch { settingsRepository.updateNightEndHour(hour) }
        }

        fun updateNightDimLevel(level: Int) {
            viewModelScope.launch { settingsRepository.updateNightDimLevel(level) }
        }

        fun updateMosqueLogoUri(uri: String) {
            viewModelScope.launch { settingsRepository.updateMosqueLogoUri(uri) }
        }

        fun updateBackgroundMainUri(uri: String) {
            viewModelScope.launch { settingsRepository.updateBackgroundMainUri(uri) }
        }

        fun updateBackgroundPrayerUri(uri: String) {
            viewModelScope.launch { settingsRepository.updateBackgroundPrayerUri(uri) }
        }

        fun updateBackgroundIqamahUri(uri: String) {
            viewModelScope.launch { settingsRepository.updateBackgroundIqamahUri(uri) }
        }

        fun updateRunningTextMessages(text: String) {
            viewModelScope.launch { settingsRepository.updateRunningTextMessages(text) }
        }

        fun updateColorTheme(themeId: String) {
            viewModelScope.launch {
                settingsRepository.updateColorTheme(themeId)
            }
        }

        fun updateWeatherApiKey(apiKey: String) {
            viewModelScope.launch {
                settingsRepository.updateWeatherApiKey(apiKey)
            }
        }

        fun updateOpenWeatherMapApiKey(apiKey: String) {
            viewModelScope.launch {
                settingsRepository.updateOpenWeatherMapApiKey(apiKey)
            }
        }

        fun updateMosqueClockBackendUrl(url: String) {
            viewModelScope.launch {
                settingsRepository.updateMosqueClockBackendUrl(url)
            }
        }

        fun updateMosqueClockBackendApiKey(apiKey: String) {
            viewModelScope.launch {
                settingsRepository.updateMosqueClockBackendApiKey(apiKey)
            }
        }

        fun updateJummahDurationMinutes(minutes: Int) {
            viewModelScope.launch {
                settingsRepository.updateJummahDurationMinutes(minutes)
            }
        }

        fun updateDuaDisplayDurationMinutes(minutes: Int) {
            viewModelScope.launch {
                settingsRepository.updateDuaDisplayDurationMinutes(minutes)
            }
        }

        fun updateShowJummahScreen(show: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateShowJummahScreen(show)
            }
        }

        fun updateAzanSoundEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateAzanSoundEnabled(enabled)
            }
        }

        fun updateIqamahSoundEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateIqamahSoundEnabled(enabled)
            }
        }

        fun updateAzanSoundType(soundType: SoundType) {
            viewModelScope.launch {
                settingsRepository.updateAzanSoundType(soundType)
            }
        }

        fun updateIqamahSoundType(soundType: SoundType) {
            viewModelScope.launch {
                settingsRepository.updateIqamahSoundType(soundType)
            }
        }

        fun updateAzanSoundUri(uri: String) {
            viewModelScope.launch {
                settingsRepository.updateAzanSoundUri(uri)
            }
        }

        /** Tes suara adzan/iqamah dengan pengaturan yang sedang tersimpan (sama dengan mode terhubung). */
        fun testSound(adzan: Boolean) {
            viewModelScope.launch {
                val current = settingsRepository.getSettings().first()
                val sm = com.mosque.prayerclock.service.SoundManager(appContext)
                if (adzan) {
                    sm.playAzanSound(current, com.mosque.prayerclock.data.model.PrayerType.DHUHR)
                } else {
                    sm.playIqamahSound(current, com.mosque.prayerclock.data.model.PrayerType.DHUHR)
                }
            }
        }

        fun updateIqamahSoundUri(uri: String) {
            viewModelScope.launch {
                settingsRepository.updateIqamahSoundUri(uri)
            }
        }

        fun updateJummaNightBayanEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateJummaNightBayanEnabled(enabled)
            }
        }

        fun updateJummaNightBayanMinutes(minutes: Int) {
            viewModelScope.launch {
                settingsRepository.updateJummaNightBayanMinutes(minutes)
            }
        }

        fun updateAutoUpdateCheckEnabled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.updateAutoUpdateCheckEnabled(enabled)
            }
        }

        suspend fun updateSkippedUpdateVersion(version: String) {
            settingsRepository.updateSkippedUpdateVersion(version)
        }

        /**
         * Clear all cached data (both prayer times and hijri dates)
         */
        suspend fun clearAllCachedData() {
            try {
                Log.d("SettingsViewModel", "🧹 Starting cache cleanup...")
                
                // Clear prayer times cache
                prayerTimesRepository.clearAllPrayerTimesCache()
                
                // Clear hijri dates cache
                hijriDateRepository.clearAllHijriCache()
                
                Log.d("SettingsViewModel", "✅ All cached data cleared successfully")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Failed to clear cached data", e)
                throw e // Re-throw so the UI can handle the error
            }
        }

        /**
         * Get cache statistics
         */
        suspend fun getCacheStats(): CacheStats {
            return try {
                CacheStats(
                    prayerTimesCount = prayerTimesRepository.getPrayerTimesCacheCount(),
                    hijriDatesCount = hijriDateRepository.getHijriCacheCount()
                )
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Failed to get cache stats", e)
                CacheStats(0, 0)
            }
        }
    }

/**
 * Cache statistics
 */
data class CacheStats(
    val prayerTimesCount: Int,
    val hijriDatesCount: Int,
)


