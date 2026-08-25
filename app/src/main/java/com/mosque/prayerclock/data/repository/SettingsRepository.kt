package com.mosque.prayerclock.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.AppTheme
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.FontSize
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.SoundType
import com.mosque.prayerclock.data.model.WeatherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val prayerTimesCacheInvalidatorProvider: Provider<PrayerTimesCacheInvalidator>,
    ) {
        private object PreferencesKeys {
            val LANGUAGE = stringPreferencesKey("language")
            val CITY = stringPreferencesKey("city")
            val COUNTRY = stringPreferencesKey("country")
            val LATITUDE = doublePreferencesKey("latitude")
            val LONGITUDE = doublePreferencesKey("longitude")
            val MOSQUE_NAME = stringPreferencesKey("mosque_name")
            val CLOCK_TYPE = stringPreferencesKey("clock_type")
            val THEME = stringPreferencesKey("theme")
            val FONT_SIZE = stringPreferencesKey("font_size")
            val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
            val SHOW_24_HOUR_FORMAT = booleanPreferencesKey("show_24_hour_format")
            val MANUAL_FAJR_AZAN = stringPreferencesKey("manual_fajr_azan")
            val MANUAL_SUNRISE = stringPreferencesKey("manual_sunrise")
            val MANUAL_DHUHR_AZAN = stringPreferencesKey("manual_dhuhr_azan")
            val MANUAL_ASR_AZAN = stringPreferencesKey("manual_asr_azan")
            val MANUAL_MAGHRIB_AZAN = stringPreferencesKey("manual_maghrib_azan")
            val MANUAL_ISHA_AZAN = stringPreferencesKey("manual_isha_azan")
            val FAJR_IQAMAH_GAP = intPreferencesKey("fajr_iqamah_gap")
            val DHUHR_IQAMAH_GAP = intPreferencesKey("dhuhr_iqamah_gap")
            val ASR_IQAMAH_GAP = intPreferencesKey("asr_iqamah_gap")
            val MAGHRIB_IQAMAH_GAP = intPreferencesKey("maghrib_iqamah_gap")
            val ISHA_IQAMAH_GAP = intPreferencesKey("isha_iqamah_gap")

            // SIWAK: koreksi menit per-waktu
            val PRAYER_OFFSET_ENABLED = booleanPreferencesKey("prayer_offset_enabled")
            val IMSAK_OFFSET = intPreferencesKey("imsak_offset")
            val SHOW_IQAMAH_CYCLE = booleanPreferencesKey("show_iqamah_cycle")
            val MANUAL_IMSAK = stringPreferencesKey("manual_imsak")
            val FAJR_OFFSET = intPreferencesKey("fajr_offset")
            val SUNRISE_OFFSET = intPreferencesKey("sunrise_offset")
            val DHUHR_OFFSET = intPreferencesKey("dhuhr_offset")
            val ASR_OFFSET = intPreferencesKey("asr_offset")
            val MAGHRIB_OFFSET = intPreferencesKey("maghrib_offset")
            val ISHA_OFFSET = intPreferencesKey("isha_offset")
            val REFRESH_INTERVAL = longPreferencesKey("refresh_interval")
            val SHOW_WEATHER = booleanPreferencesKey("show_weather")
            val WEATHER_CITY = stringPreferencesKey("weather_city")
            val WEATHER_COUNTRY = stringPreferencesKey("weather_country")
            val WEATHER_PROVIDER = stringPreferencesKey("weather_provider")
            val USE_API_FOR_HIJRI_DATE = booleanPreferencesKey("use_api_for_hijri_date")
            val HIJRI_PROVIDER = stringPreferencesKey("hijri_provider")
            val MANUAL_HIJRI_DAY = intPreferencesKey("manual_hijri_day")
            val MANUAL_HIJRI_MONTH = intPreferencesKey("manual_hijri_month")
            val MANUAL_HIJRI_YEAR = intPreferencesKey("manual_hijri_year")
            val LAST_UPDATED_GREGORIAN_DATE = stringPreferencesKey("last_updated_gregorian_date")
            val PRAYER_SERVICE_TYPE = stringPreferencesKey("prayer_service_type")
            val SELECTED_ZONE = intPreferencesKey("selected_zone")
            val SELECTED_REGION = stringPreferencesKey("selected_region")
            val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
            val FULL_SCREEN_COUNTDOWN_ENABLED = booleanPreferencesKey("full_screen_countdown_enabled")
            val FULL_SCREEN_COUNTDOWN_MINUTES = intPreferencesKey("full_screen_countdown_minutes")
            val SHOW_SUNRISE = booleanPreferencesKey("show_sunrise")
            val SHOW_IMSAK = booleanPreferencesKey("show_imsak")
            val PRAYER_LAST_SYNC = longPreferencesKey("prayer_last_sync")
            val WEATHER_LAST_SYNC = longPreferencesKey("weather_last_sync")
            val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
            val CLOCK_SIZE_PERCENT = intPreferencesKey("clock_size_percent")
            val HIJRI_DAY_OFFSET = intPreferencesKey("hijri_day_offset")
            val NIGHT_MODE_ENABLED = booleanPreferencesKey("night_mode_enabled")
            val NIGHT_START_HOUR = intPreferencesKey("night_start_hour")
            val NIGHT_END_HOUR = intPreferencesKey("night_end_hour")
            val NIGHT_DIM_LEVEL = intPreferencesKey("night_dim_level")
            val MOSQUE_LOGO_URI = stringPreferencesKey("mosque_logo_uri")
            val BACKGROUND_MAIN_URI = stringPreferencesKey("background_main_uri")
            val BACKGROUND_PRAYER_URI = stringPreferencesKey("background_prayer_uri")
            val BACKGROUND_IQAMAH_URI = stringPreferencesKey("background_iqamah_uri")
            val RUNNING_TEXT_MESSAGES = stringPreferencesKey("running_text_messages")
            val COLOR_THEME = stringPreferencesKey("color_theme")
            val WEATHER_API_KEY = stringPreferencesKey("weather_api_key")
            val OPENWEATHERMAP_API_KEY = stringPreferencesKey("openweathermap_api_key")
            val MOSQUE_CLOCK_BACKEND_URL = stringPreferencesKey("mosque_clock_backend_url")
            val MOSQUE_CLOCK_BACKEND_API_KEY = stringPreferencesKey("mosque_clock_backend_api_key")
            val JUMMAH_DURATION_MINUTES = intPreferencesKey("jummah_duration_minutes")
            val DUA_DISPLAY_DURATION_MINUTES = intPreferencesKey("dua_display_duration_minutes")
            val SHOW_JUMMAH_SCREEN = booleanPreferencesKey("show_jummah_screen")
            // Enhanced notification settings
            val AZAN_SOUND_ENABLED = booleanPreferencesKey("azan_sound_enabled")
            val IQAMAH_SOUND_ENABLED = booleanPreferencesKey("iqamah_sound_enabled")
            val AZAN_SOUND_TYPE = stringPreferencesKey("azan_sound_type")
            val IQAMAH_SOUND_TYPE = stringPreferencesKey("iqamah_sound_type")
            val AZAN_SOUND_URI = stringPreferencesKey("azan_sound_uri")
            val IQAMAH_SOUND_URI = stringPreferencesKey("iqamah_sound_uri")
            // Jumma Night Bayan settings
            val JUMMA_NIGHT_BAYAN_ENABLED = booleanPreferencesKey("jumma_night_bayan_enabled")
            val JUMMA_NIGHT_BAYAN_MINUTES = intPreferencesKey("jumma_night_bayan_minutes")
            // Auto-update settings
            val AUTO_UPDATE_CHECK_ENABLED = booleanPreferencesKey("auto_update_check_enabled")
            val SKIPPED_UPDATE_VERSION = stringPreferencesKey("skipped_update_version")
        }

        fun getSettings(): Flow<AppSettings> =
            dataStore.data.map { preferences ->
                AppSettings(
                    language =
                        Language.values().find {
                            it.code == preferences[PreferencesKeys.LANGUAGE]
                        }
                            ?: Language.INDONESIAN,
                    city = preferences[PreferencesKeys.CITY] ?: "Yogyakarta",
                    country = preferences[PreferencesKeys.COUNTRY] ?: "Indonesia",
                    latitude = preferences[PreferencesKeys.LATITUDE] ?: -7.7956, // Yogyakarta
                    longitude = preferences[PreferencesKeys.LONGITUDE] ?: 110.3695,
                    mosqueName =
                        preferences[PreferencesKeys.MOSQUE_NAME]
                            ?: "Masjid",
                    clockType =
                        ClockType.values().find {
                            it.name == preferences[PreferencesKeys.CLOCK_TYPE]
                        }
                            ?: ClockType.DIGITAL,
                    theme =
                        AppTheme.values().find {
                            it.name == preferences[PreferencesKeys.THEME]
                        }
                            ?: AppTheme.DARK,
                    fontSize =
                        FontSize.values().find {
                            it.name == preferences[PreferencesKeys.FONT_SIZE]
                        }
                            ?: FontSize.MEDIUM,
                    showSeconds = preferences[PreferencesKeys.SHOW_SECONDS] ?: true,
                    show24HourFormat =
                        preferences[PreferencesKeys.SHOW_24_HOUR_FORMAT]
                            ?: false,
                    colorTheme = preferences[PreferencesKeys.COLOR_THEME] ?: "classic_mosque",
                    manualFajrAzan = preferences[PreferencesKeys.MANUAL_FAJR_AZAN] ?: "05:30",
                    manualSunrise = preferences[PreferencesKeys.MANUAL_SUNRISE] ?: "06:15",
                    manualDhuhrAzan = preferences[PreferencesKeys.MANUAL_DHUHR_AZAN] ?: "12:15",
                    manualAsrAzan = preferences[PreferencesKeys.MANUAL_ASR_AZAN] ?: "15:30",
                    manualMaghribAzan =
                        preferences[PreferencesKeys.MANUAL_MAGHRIB_AZAN]
                            ?: "18:30",
                    manualIshaAzan = preferences[PreferencesKeys.MANUAL_ISHA_AZAN] ?: "19:45",
                    fajrIqamahGap = preferences[PreferencesKeys.FAJR_IQAMAH_GAP] ?: 20,
                    dhuhrIqamahGap = preferences[PreferencesKeys.DHUHR_IQAMAH_GAP] ?: 10,
                    asrIqamahGap = preferences[PreferencesKeys.ASR_IQAMAH_GAP] ?: 10,
                    maghribIqamahGap = preferences[PreferencesKeys.MAGHRIB_IQAMAH_GAP] ?: 10,
                    ishaIqamahGap = preferences[PreferencesKeys.ISHA_IQAMAH_GAP] ?: 10,
                    prayerOffsetEnabled = preferences[PreferencesKeys.PRAYER_OFFSET_ENABLED] ?: false,
                    imsakOffset = preferences[PreferencesKeys.IMSAK_OFFSET] ?: 0,
                    showIqamahCycle = preferences[PreferencesKeys.SHOW_IQAMAH_CYCLE] ?: true,
                    manualImsak = preferences[PreferencesKeys.MANUAL_IMSAK] ?: "04:20",
                    fajrOffset = preferences[PreferencesKeys.FAJR_OFFSET] ?: 0,
                    sunriseOffset = preferences[PreferencesKeys.SUNRISE_OFFSET] ?: 0,
                    dhuhrOffset = preferences[PreferencesKeys.DHUHR_OFFSET] ?: 0,
                    asrOffset = preferences[PreferencesKeys.ASR_OFFSET] ?: 0,
                    maghribOffset = preferences[PreferencesKeys.MAGHRIB_OFFSET] ?: 0,
                    ishaOffset = preferences[PreferencesKeys.ISHA_OFFSET] ?: 0,
                    refreshInterval =
                        preferences[PreferencesKeys.REFRESH_INTERVAL]
                            ?: 24 * 60 * 60 * 1000L,
                    showWeather = preferences[PreferencesKeys.SHOW_WEATHER] ?: false,
                    weatherCity = preferences[PreferencesKeys.WEATHER_CITY] ?: "",
                    weatherCountry =
                        preferences[PreferencesKeys.WEATHER_COUNTRY]
                            ?: "Indonesia",
                    weatherProvider =
                        WeatherProvider.values().find {
                            it.name == preferences[PreferencesKeys.WEATHER_PROVIDER]
                        }
                            ?: WeatherProvider.WEATHER_API,
                    hijriProvider =
                        HijriProvider.values().find {
                            it.name == preferences[PreferencesKeys.HIJRI_PROVIDER]
                        }
                            ?: HijriProvider.AL_ADHAN_API,
                    manualHijriDay = preferences[PreferencesKeys.MANUAL_HIJRI_DAY] ?: 7,
                    manualHijriMonth = preferences[PreferencesKeys.MANUAL_HIJRI_MONTH] ?: 2,
                    manualHijriYear = preferences[PreferencesKeys.MANUAL_HIJRI_YEAR] ?: 1447,
                    lastUpdatedGregorianDate =
                        preferences[PreferencesKeys.LAST_UPDATED_GREGORIAN_DATE]
                            ?: "2025-08-01",
                    prayerServiceType =
                        PrayerServiceType.values().find {
                            it.name == preferences[PreferencesKeys.PRAYER_SERVICE_TYPE]
                        }
                            ?: PrayerServiceType.AL_ADHAN_API,
                    selectedZone = preferences[PreferencesKeys.SELECTED_ZONE] ?: 1,
                    selectedRegion = preferences[PreferencesKeys.SELECTED_REGION] ?: "Yogyakarta",
                    soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                    fullScreenCountdownEnabled = preferences[PreferencesKeys.FULL_SCREEN_COUNTDOWN_ENABLED] ?: true,
                    fullScreenCountdownMinutes = preferences[PreferencesKeys.FULL_SCREEN_COUNTDOWN_MINUTES] ?: 10,
                    showSunrise = preferences[PreferencesKeys.SHOW_SUNRISE] ?: true,
                    showImsak = preferences[PreferencesKeys.SHOW_IMSAK] ?: false,
                    prayerLastSyncMillis = preferences[PreferencesKeys.PRAYER_LAST_SYNC] ?: 0L,
                    weatherLastSyncMillis = preferences[PreferencesKeys.WEATHER_LAST_SYNC] ?: 0L,
                    cachedWeatherJson = preferences[PreferencesKeys.CACHED_WEATHER_JSON] ?: "",
                    clockSizePercent = preferences[PreferencesKeys.CLOCK_SIZE_PERCENT] ?: 100,
                    hijriDayOffset = preferences[PreferencesKeys.HIJRI_DAY_OFFSET] ?: 0,
                    nightModeEnabled = preferences[PreferencesKeys.NIGHT_MODE_ENABLED] ?: false,
                    nightStartHour = preferences[PreferencesKeys.NIGHT_START_HOUR] ?: 22,
                    nightEndHour = preferences[PreferencesKeys.NIGHT_END_HOUR] ?: 4,
                    nightDimLevel = preferences[PreferencesKeys.NIGHT_DIM_LEVEL] ?: 85,
                    mosqueLogoUri = preferences[PreferencesKeys.MOSQUE_LOGO_URI] ?: "",
                    backgroundMainUri = preferences[PreferencesKeys.BACKGROUND_MAIN_URI] ?: "",
                    backgroundPrayerUri = preferences[PreferencesKeys.BACKGROUND_PRAYER_URI] ?: "",
                    backgroundIqamahUri = preferences[PreferencesKeys.BACKGROUND_IQAMAH_URI] ?: "",
                    runningTextMessages = preferences[PreferencesKeys.RUNNING_TEXT_MESSAGES] ?: "",
                    weatherApiKey = preferences[PreferencesKeys.WEATHER_API_KEY] ?: "",
                    openWeatherMapApiKey = preferences[PreferencesKeys.OPENWEATHERMAP_API_KEY] ?: "3c2ad345d6952077f51037b1c0f8de88",
                    mosqueClockBackendUrl = preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_URL] ?: "",
                    mosqueClockBackendApiKey = preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_API_KEY] ?: "",
                    jummahDurationMinutes = preferences[PreferencesKeys.JUMMAH_DURATION_MINUTES] ?: 60,
                    duaDisplayDurationMinutes = preferences[PreferencesKeys.DUA_DISPLAY_DURATION_MINUTES] ?: 5,
                    showJummahScreen = preferences[PreferencesKeys.SHOW_JUMMAH_SCREEN] ?: true,
                    azanSoundEnabled = preferences[PreferencesKeys.AZAN_SOUND_ENABLED] ?: true,
                    iqamahSoundEnabled = preferences[PreferencesKeys.IQAMAH_SOUND_ENABLED] ?: true,
                    azanSoundType =
                        SoundType.values().find {
                            it.name == preferences[PreferencesKeys.AZAN_SOUND_TYPE]
                        }
                            ?: SoundType.COUNTDOWN_TICKING,
                    iqamahSoundType =
                        SoundType.values().find {
                            it.name == preferences[PreferencesKeys.IQAMAH_SOUND_TYPE]
                        }
                            ?: SoundType.COUNTDOWN_TICKING,
                    azanSoundUri = preferences[PreferencesKeys.AZAN_SOUND_URI] ?: "",
                    iqamahSoundUri = preferences[PreferencesKeys.IQAMAH_SOUND_URI] ?: "",
                    jummaNightBayanEnabled = preferences[PreferencesKeys.JUMMA_NIGHT_BAYAN_ENABLED] ?: false,
                    jummaNightBayanMinutes = preferences[PreferencesKeys.JUMMA_NIGHT_BAYAN_MINUTES] ?: 30,
                    autoUpdateCheckEnabled = preferences[PreferencesKeys.AUTO_UPDATE_CHECK_ENABLED] ?: true,
                    skippedUpdateVersion = preferences[PreferencesKeys.SKIPPED_UPDATE_VERSION] ?: "",
                )
            }

        suspend fun updateLanguage(language: Language) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.LANGUAGE] = language.code }
        }

        suspend fun updateCountry(country: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.COUNTRY] = country }
        }

        suspend fun updateCity(city: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.CITY] = city }
        }

        // SIWAK: set kota + koordinat sekaligus (dipakai pemilih Kota/Kabupaten Indonesia)
        suspend fun updateLocationCity(
            city: String,
            latitude: Double,
            longitude: Double,
        ) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.CITY] = city
                preferences[PreferencesKeys.SELECTED_REGION] = city
                preferences[PreferencesKeys.LATITUDE] = latitude
                preferences[PreferencesKeys.LONGITUDE] = longitude
            }
        }

        // SIWAK: input koordinat manual (lat/long) tanpa mengubah nama kota
        suspend fun updateCoordinates(
            latitude: Double,
            longitude: Double,
        ) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LATITUDE] = latitude
                preferences[PreferencesKeys.LONGITUDE] = longitude
            }
        }

        suspend fun updateMosqueName(mosqueName: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.MOSQUE_NAME] = mosqueName }
        }

        suspend fun updateClockType(clockType: ClockType) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.CLOCK_TYPE] = clockType.name }
        }

        suspend fun updateShowIqamahCycle(show: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_IQAMAH_CYCLE] = show }
        }

        suspend fun updatePrayerOffsetEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.PRAYER_OFFSET_ENABLED] = enabled }
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateTheme(theme: AppTheme) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.THEME] = theme.name }
        }

        suspend fun updateFontSize(fontSize: FontSize) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.FONT_SIZE] = fontSize.name }
        }

        suspend fun updateShowSeconds(showSeconds: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_SECONDS] = showSeconds }
        }

        suspend fun updateShow24HourFormat(show24Hour: Boolean) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOW_24_HOUR_FORMAT] = show24Hour
            }
        }

        suspend fun updateRefreshInterval(refreshInterval: Long) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.REFRESH_INTERVAL] = refreshInterval
            }
        }

        suspend fun updateManualTime(
            timeType: String,
            time: String,
        ) {
            dataStore.edit { preferences ->
                when (timeType) {
                    "imsak" -> preferences[PreferencesKeys.MANUAL_IMSAK] = time
                    "fajrAzan" -> preferences[PreferencesKeys.MANUAL_FAJR_AZAN] = time
                    "sunrise" -> preferences[PreferencesKeys.MANUAL_SUNRISE] = time
                    "dhuhrAzan" -> preferences[PreferencesKeys.MANUAL_DHUHR_AZAN] = time
                    "asrAzan" -> preferences[PreferencesKeys.MANUAL_ASR_AZAN] = time
                    "maghribAzan" -> preferences[PreferencesKeys.MANUAL_MAGHRIB_AZAN] = time
                    "ishaAzan" -> preferences[PreferencesKeys.MANUAL_ISHA_AZAN] = time
                }
            }
            // Invalidate prayer times cache when manual times are updated
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateIqamahGap(
            prayerType: String,
            gap: Int,
        ) {
            dataStore.edit { preferences ->
                when (prayerType) {
                    "fajr" -> preferences[PreferencesKeys.FAJR_IQAMAH_GAP] = gap
                    "dhuhr" -> preferences[PreferencesKeys.DHUHR_IQAMAH_GAP] = gap
                    "asr" -> preferences[PreferencesKeys.ASR_IQAMAH_GAP] = gap
                    "maghrib" -> preferences[PreferencesKeys.MAGHRIB_IQAMAH_GAP] = gap
                    "isha" -> preferences[PreferencesKeys.ISHA_IQAMAH_GAP] = gap
                }
            }
            // Invalidate prayer times cache when iqamah gaps are updated
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        // SIWAK: koreksi menit per-waktu (offset ±)
        suspend fun updatePrayerOffset(
            prayerType: String,
            minutes: Int,
        ) {
            dataStore.edit { preferences ->
                when (prayerType) {
                    "imsak" -> preferences[PreferencesKeys.IMSAK_OFFSET] = minutes
                    "fajr" -> preferences[PreferencesKeys.FAJR_OFFSET] = minutes
                    "sunrise" -> preferences[PreferencesKeys.SUNRISE_OFFSET] = minutes
                    "dhuhr" -> preferences[PreferencesKeys.DHUHR_OFFSET] = minutes
                    "asr" -> preferences[PreferencesKeys.ASR_OFFSET] = minutes
                    "maghrib" -> preferences[PreferencesKeys.MAGHRIB_OFFSET] = minutes
                    "isha" -> preferences[PreferencesKeys.ISHA_OFFSET] = minutes
                }
            }
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateHijriDate(
            day: Int,
            month: Int,
            year: Int,
            gregorianDate: String,
        ) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.MANUAL_HIJRI_DAY] = day
                preferences[PreferencesKeys.MANUAL_HIJRI_MONTH] = month
                preferences[PreferencesKeys.MANUAL_HIJRI_YEAR] = year
                preferences[PreferencesKeys.LAST_UPDATED_GREGORIAN_DATE] = gregorianDate
            }
        }

        suspend fun updateUseApiForHijriDate(useApi: Boolean) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USE_API_FOR_HIJRI_DATE] = useApi
            }
        }

        suspend fun updateHijriProvider(provider: HijriProvider) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HIJRI_PROVIDER] = provider.name
            }
        }

        suspend fun updateShowWeather(showWeather: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_WEATHER] = showWeather }
        }

        suspend fun updateWeatherCity(city: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.WEATHER_CITY] = city }
        }

        suspend fun updateWeatherCountry(country: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.WEATHER_COUNTRY] = country }
        }

        suspend fun updateWeatherProvider(provider: WeatherProvider) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.WEATHER_PROVIDER] = provider.name
            }
        }

        suspend fun updatePrayerServiceType(serviceType: PrayerServiceType) {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PRAYER_SERVICE_TYPE] = serviceType.name
            }
            // Invalidate prayer times cache when prayer service type changes
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateSelectedZone(zone: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SELECTED_ZONE] = zone }
            // Invalidate prayer times cache when zone changes (affects cache key)
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateSelectedRegion(region: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SELECTED_REGION] = region }
            // Invalidate prayer times cache when region changes (affects cache key)
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateSoundEnabled(soundEnabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SOUND_ENABLED] = soundEnabled }
        }

        suspend fun updateFullScreenCountdownEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.FULL_SCREEN_COUNTDOWN_ENABLED] = enabled }
        }

        suspend fun updateFullScreenCountdownMinutes(minutes: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.FULL_SCREEN_COUNTDOWN_MINUTES] = minutes }
        }

        suspend fun updateShowSunrise(show: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_SUNRISE] = show }
        }

        suspend fun updateShowImsak(show: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_IMSAK] = show }
        }

        suspend fun updatePrayerLastSync(millis: Long) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.PRAYER_LAST_SYNC] = millis }
        }

        suspend fun updateClockSizePercent(percent: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.CLOCK_SIZE_PERCENT] = percent }
        }

        suspend fun updateHijriDayOffset(offset: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.HIJRI_DAY_OFFSET] = offset }
        }

        suspend fun updateNightModeEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.NIGHT_MODE_ENABLED] = enabled }
        }

        suspend fun updateNightStartHour(hour: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.NIGHT_START_HOUR] = hour }
        }

        suspend fun updateNightEndHour(hour: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.NIGHT_END_HOUR] = hour }
        }

        suspend fun updateNightDimLevel(level: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.NIGHT_DIM_LEVEL] = level }
        }

        suspend fun updateMosqueLogoUri(uri: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.MOSQUE_LOGO_URI] = uri }
        }

        suspend fun updateBackgroundMainUri(uri: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.BACKGROUND_MAIN_URI] = uri }
        }

        suspend fun updateBackgroundPrayerUri(uri: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.BACKGROUND_PRAYER_URI] = uri }
        }

        suspend fun updateBackgroundIqamahUri(uri: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.BACKGROUND_IQAMAH_URI] = uri }
        }

        suspend fun updateRunningTextMessages(text: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.RUNNING_TEXT_MESSAGES] = text }
        }

        suspend fun updateWeatherLastSync(millis: Long) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.WEATHER_LAST_SYNC] = millis }
        }

        suspend fun updateCachedWeatherJson(json: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.CACHED_WEATHER_JSON] = json }
        }

        suspend fun updateColorTheme(themeId: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.COLOR_THEME] = themeId }
        }

        suspend fun updateWeatherApiKey(apiKey: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.WEATHER_API_KEY] = apiKey }
        }

        suspend fun updateOpenWeatherMapApiKey(apiKey: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.OPENWEATHERMAP_API_KEY] = apiKey }
        }

        suspend fun updateMosqueClockBackendUrl(url: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_URL] = url }
        }

        suspend fun updateMosqueClockBackendApiKey(apiKey: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.MOSQUE_CLOCK_BACKEND_API_KEY] = apiKey }
        }

        suspend fun updateJummahDurationMinutes(minutes: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.JUMMAH_DURATION_MINUTES] = minutes }
        }

        suspend fun updateDuaDisplayDurationMinutes(minutes: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.DUA_DISPLAY_DURATION_MINUTES] = minutes }
        }

        suspend fun updateShowJummahScreen(show: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_JUMMAH_SCREEN] = show }
        }

        suspend fun updateAzanSoundEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.AZAN_SOUND_ENABLED] = enabled }
        }

        suspend fun updateIqamahSoundEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.IQAMAH_SOUND_ENABLED] = enabled }
        }

        suspend fun updateAzanSoundType(soundType: SoundType) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.AZAN_SOUND_TYPE] = soundType.name }
        }

        suspend fun updateIqamahSoundType(soundType: SoundType) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.IQAMAH_SOUND_TYPE] = soundType.name }
        }

        suspend fun updateAzanSoundUri(uri: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.AZAN_SOUND_URI] = uri }
        }

        suspend fun updateIqamahSoundUri(uri: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.IQAMAH_SOUND_URI] = uri }
        }

        suspend fun updateJummaNightBayanEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.JUMMA_NIGHT_BAYAN_ENABLED] = enabled }
            // Invalidate prayer times cache when Jumma Night Bayan setting changes
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateJummaNightBayanMinutes(minutes: Int) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.JUMMA_NIGHT_BAYAN_MINUTES] = minutes }
            // Invalidate prayer times cache when Jumma Night Bayan duration changes
            prayerTimesCacheInvalidatorProvider.get().invalidatePrayerTimesCache()
        }

        suspend fun updateAutoUpdateCheckEnabled(enabled: Boolean) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.AUTO_UPDATE_CHECK_ENABLED] = enabled }
        }

        suspend fun updateSkippedUpdateVersion(version: String) {
            dataStore.edit { preferences -> preferences[PreferencesKeys.SKIPPED_UPDATE_VERSION] = version }
        }
    }
