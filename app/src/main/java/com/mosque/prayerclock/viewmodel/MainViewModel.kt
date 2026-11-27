package com.mosque.prayerclock.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.data.repository.PrayerTimesRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.data.repository.WeatherRepository
import com.mosque.prayerclock.domain.NextPrayerRule
import com.mosque.prayerclock.utils.NetworkStatus
import com.mosque.prayerclock.utils.SystemChangeEvent
import com.mosque.prayerclock.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val prayerTimesRepository: PrayerTimesRepository,
        private val settingsRepository: SettingsRepository,
        private val weatherRepository: WeatherRepository,
        val hijriDateRepository: HijriDateRepository,
    ) : ViewModel() {
        // Add unique identifier to track ViewModel instances
        private val viewModelId = System.currentTimeMillis().toString().takeLast(4)

        val settings =
            settingsRepository
                .getSettings()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = AppSettings(),
                )

        private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
        val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

        private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading())
        val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

        @Volatile private var isLoadingPrayerTimes: Boolean = false

        // Cache tomorrow's prayer times to avoid repeated fetching after Isha
        private var cachedTomorrowFajr: PrayerTimesWithIqamah? = null
        private var cachedTomorrowDate: String? = null

        // Network status tracking for auto-refresh after connectivity restored
        private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unavailable)
        val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

        // Track the date when prayer times were last successfully loaded
        // Used to detect if we need to refresh after a time sync
        @Volatile private var lastLoadedDate: String? = null

        // Event to trigger Hijri date refresh in UI components
        private val _hijriRefreshTrigger = MutableSharedFlow<Unit>(replay = 0)
        val hijriRefreshTrigger = _hijriRefreshTrigger.asSharedFlow()

        init {
            // SIWAK: tampilkan cuaca dari cache dulu (offline / sebelum jaringan siap)
            loadCachedWeather()
            // Waktu "berikutnya" dan pergantian hari diperiksa berkala di sini, tidak bergantung pada komponen
            // yang sedang tampil (tata letak Galeri, layar shalat, dan hitung mundur layar penuh tidak memicunya).
            viewModelScope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(TICK_MS)
                    try {
                        tick()
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "Pemeriksaan berkala gagal: ${e.message}")
                    }
                }
            }
        }

        private suspend fun tick() {
            val state = _uiState.value as? MainUiState.Success ?: return
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            if (lastLoadedDate != null && lastLoadedDate != today) {
                // Tengah malam terlewati: muat jadwal hari yang baru.
                Log.i("MainViewModel", "Hari berganti ($lastLoadedDate -> $today), memuat jadwal baru")
                loadPrayerTimes()
                return
            }
            refreshNextPrayer(state)
        }

        /** Hitung ulang waktu berikutnya; setelah Isya, sertakan jadwal Subuh besok. */
        private suspend fun refreshNextPrayer(state: MainUiState.Success) {
            val next = calculateNextPrayer(state.prayerTimes)
            val tomorrow =
                if (next == PrayerType.FAJR && isAllPrayersCompleted(state.prayerTimes)) {
                    state.nextDayFajr ?: getTomorrowPrayerTimesOptimized(settings.value)
                } else {
                    null
                }
            if (next == state.nextPrayer && tomorrow == state.nextDayFajr) return
            // Jangan menimpa jadwal yang baru dimuat di antara perhitungan ini.
            _uiState.update { cur ->
                if (cur is MainUiState.Success && cur.prayerTimes == state.prayerTimes) cur.copy(nextPrayer = next, nextDayFajr = tomorrow) else cur
            }
        }

        /**
         * Handle system change events (time/date/timezone changes).
         * Called from UI when SystemChangeMonitor emits events.
         */
        fun onSystemChangeEvent(event: SystemChangeEvent) {
            Log.i("MainViewModel", "System change event received: $event")

            when (event) {
                is SystemChangeEvent.TimeChanged,
                is SystemChangeEvent.DateChanged,
                is SystemChangeEvent.TimezoneChanged,
                is SystemChangeEvent.SignificantTimeJump -> {
                    // Check if the current date is different from when we last loaded
                    val currentDate = Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()

                    val needsRefresh = lastLoadedDate != currentDate || lastLoadedDate == null

                    Log.i(
                        "MainViewModel",
                        "Time sync detected - lastLoadedDate: $lastLoadedDate, currentDate: $currentDate, needsRefresh: $needsRefresh"
                    )

                    if (needsRefresh) {
                        refreshAllData()
                    }
                }
            }
        }

        /**
         * Handle network status changes.
         * Called from UI when NetworkMonitor emits events.
         */
        fun onNetworkStatusChange(status: NetworkStatus) {
            val previousStatus = _networkStatus.value
            _networkStatus.value = status

            Log.i("MainViewModel", "Network status changed: $previousStatus -> $status")

            // When network becomes available, check if we need to refresh data
            if (status == NetworkStatus.Available && previousStatus == NetworkStatus.Unavailable) {
                Log.i("MainViewModel", "Network restored - checking if data refresh needed")

                viewModelScope.launch(Dispatchers.IO) {
                    // Check if we have valid cached prayer times for today
                    val hasValidCache = prayerTimesRepository.hasCachedPrayerTimesForCurrentSettings()

                    if (!hasValidCache) {
                        Log.i("MainViewModel", "No valid cache found after network restore - refreshing data")
                        refreshAllData()
                    } else {
                        Log.d("MainViewModel", "Cache is valid - no refresh needed")
                    }
                }
            }
        }

        /**
         * Refresh all data: prayer times, Hijri date, and weather.
         * Invalidates caches and triggers fresh fetch.
         */
        private fun refreshAllData() {
            Log.i("MainViewModel", "Refreshing all data...")

            viewModelScope.launch(Dispatchers.IO) {
                // Invalidate prayer times cache to force fresh fetch
                prayerTimesRepository.invalidatePrayerTimesCache()

                // Also clear tomorrow's cache
                cachedTomorrowFajr = null
                cachedTomorrowDate = null

                // Reset loading flag to allow new load
                isLoadingPrayerTimes = false

                // Trigger prayer times reload
                loadPrayerTimes()

                // Emit event to trigger Hijri date refresh in UI components
                _hijriRefreshTrigger.emit(Unit)

                Log.i("MainViewModel", "Data refresh triggered")
            }
        }

        /**
         * Force refresh of all data. Can be called from UI if needed.
         */
        fun forceRefresh() {
            Log.i("MainViewModel", "Force refresh requested")
            refreshAllData()
        }

        // Function to recalculate next prayer - can be called from UI when needed
        fun updateNextPrayer() {
            val currentState = _uiState.value as? MainUiState.Success ?: return
            viewModelScope.launch(Dispatchers.IO) { refreshNextPrayer(currentState) }
        }

        fun loadPrayerTimes() {
            if (isLoadingPrayerTimes) return

            isLoadingPrayerTimes = true

            viewModelScope.launch(Dispatchers.IO) {
                val currentSettings = settings.value

                // Use the centralized repository method that handles all prayer service types
                prayerTimesRepository.getTodayPrayerTimesFromSettings().collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _uiState.value = MainUiState.Loading
                        }
                        is NetworkResult.Success -> {
                            // Track the date when prayer times were successfully loaded
                            lastLoadedDate = Clock.System
                                .now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date
                                .toString()

                            // Convert to PrayerTimesWithIqamah using current settings
                            // Iqamah times are calculated dynamically from settings
                            val prayerTimesWithIqamah = result.data.withIqamahTimes(currentSettings)

                            val nextPrayer = calculateNextPrayer(prayerTimesWithIqamah)

                            // If next prayer is Fajr and all today's prayers are done, get tomorrow's times
                            val nextDayFajr =
                                if (nextPrayer == PrayerType.FAJR && isAllPrayersCompleted(prayerTimesWithIqamah)) {
                                    getTomorrowPrayerTimesOptimized(currentSettings)
                                } else {
                                    null
                                }

                            _uiState.value =
                                MainUiState.Success(
                                    prayerTimes = prayerTimesWithIqamah,
                                    nextPrayer = nextPrayer,
                                    nextDayFajr = nextDayFajr,
                                )

                            Log.d("MainViewModel", "Prayer times loaded successfully for date: $lastLoadedDate")
                        }
                        is NetworkResult.Error -> {
                            Log.e("MainViewModel", "Prayer times loading failed: ${result.message}")
                            _uiState.value = MainUiState.Error(result.message)
                        }
                    }
                }

                // SIWAK: cuaca memakai LOKASI yang sama dengan jadwal shalat (koordinat), bukan nama kota.
                // Refresh berkala otomatis (30 menit) + fetch langsung; hasil di-cache utk offline.
                if (currentSettings.showWeather) {
                    weatherRepository.startHourlyWeatherRefreshByLocation(
                        currentSettings.latitude,
                        currentSettings.longitude,
                    ) { result ->
                        handleWeatherResult(result)
                    }
                } else {
                    weatherRepository.stopAllWeatherRefreshJobs()
                    _weatherState.value = WeatherUiState.Error("Weather disabled")
                }

                // SIWAK: prefetch 7 hari jadwal shalat di latar (offline seminggu penuh)
                if (currentSettings.prayerServiceType == com.mosque.prayerclock.data.model.PrayerServiceType.AL_ADHAN_API) {
                    launch(Dispatchers.IO) {
                        try {
                            prayerTimesRepository.prefetchWeekPrayerTimes(
                                currentSettings.latitude,
                                currentSettings.longitude,
                            )
                        } catch (e: Exception) {
                            Log.w("MainViewModel", "Prefetch 7 hari gagal: ${e.message}")
                        }
                    }
                }
                isLoadingPrayerTimes = false
            }
        }

        private fun loadWeatherData(
            city: String,
            country: String,
            provider: WeatherProvider,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                when (provider) {
                    WeatherProvider.WEATHER_API -> {
                        weatherRepository.getCurrentWeather(city, country).collect { result ->
                            handleWeatherResult(result)
                        }
                    }
                    WeatherProvider.OPEN_WEATHER_MAP -> {
                        weatherRepository.getCurrentWeather(city, country).collect { result ->
                            handleWeatherResult(result)
                        }
                    }
                }
            }
        }

        private fun handleWeatherResult(result: NetworkResult<WeatherInfo>) {
            when (result) {
                is NetworkResult.Loading -> {
                    // SIWAK: jangan tampilkan loading kalau sudah ada data (cache/hasil sukses)
                    if (_weatherState.value !is WeatherUiState.Success) {
                        _weatherState.value = WeatherUiState.Loading()
                    }
                }
                is NetworkResult.Success -> {
                    _weatherState.value = WeatherUiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    // SIWAK: pertahankan cuaca terakhir (cache) saat gagal/offline
                    if (_weatherState.value !is WeatherUiState.Success) {
                        _weatherState.value = WeatherUiState.Error(result.message)
                    }
                }
            }
        }

        // SIWAK: muat cuaca dari cache (offline / saat awal buka) sebelum jaringan siap
        private fun loadCachedWeather() {
            viewModelScope.launch {
                try {
                    val json = settingsRepository.getSettings().first().cachedWeatherJson
                    if (json.isNotEmpty() && _weatherState.value !is WeatherUiState.Success) {
                        val info = com.google.gson.Gson().fromJson(json, WeatherInfo::class.java)
                        if (info != null) _weatherState.value = WeatherUiState.Success(info)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Load cached weather failed: ${e.message}")
                }
            }
        }

        // Weather job management moved to WeatherRepository

        private companion object {
            const val TICK_MS = 5_000L
        }

        override fun onCleared() {
            super.onCleared()
        }

        private fun calculateNextPrayer(prayerTimes: PrayerTimesWithIqamah): PrayerType? =
            nextPrayerAt(prayerTimes, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))

        /**
         * Waktu shalat yang sedang berlangsung atau berikutnya pada [now]. Dipakai juga oleh layar setiap detik
         * agar pergantian (mis. akhir layar shalat) langsung terlihat tanpa menunggu pemeriksaan berkala.
         */
        fun nextPrayerAt(
            prayerTimes: PrayerTimesWithIqamah,
            now: kotlinx.datetime.LocalDateTime,
        ): PrayerType {
            val currentTime = "%02d:%02d".format(now.hour, now.minute)
            val s = settings.value
            return NextPrayerRule.calculate(
                slots =
                    listOf(
                        NextPrayerRule.Slot(PrayerType.FAJR, prayerTimes.fajrAzan, prayerTimes.fajrIqamah),
                        NextPrayerRule.Slot(PrayerType.SUNRISE, prayerTimes.sunrise, null),
                        NextPrayerRule.Slot(PrayerType.DHUHR, prayerTimes.dhuhrAzan, prayerTimes.dhuhrIqamah),
                        NextPrayerRule.Slot(PrayerType.ASR, prayerTimes.asrAzan, prayerTimes.asrIqamah),
                        NextPrayerRule.Slot(PrayerType.MAGHRIB, prayerTimes.maghribAzan, prayerTimes.maghribIqamah),
                        NextPrayerRule.Slot(PrayerType.ISHA, prayerTimes.ishaAzan, prayerTimes.ishaIqamah),
                    ),
                now = currentTime,
                isFriday = now.dayOfWeek == DayOfWeek.FRIDAY,
                prayerScreenMinutes = s.duaDisplayDurationMinutes,
                jummahMinutes = s.jummahDurationMinutes,
            )
        }

        private fun isAllPrayersCompleted(prayerTimes: PrayerTimesWithIqamah): Boolean {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentTime = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"

            // Check if current time is after Isha Azan (all prayers for the day are done)
            return TimeUtils.compareTimeStrings(currentTime, prayerTimes.ishaAzan) > 0
        }

        private suspend fun getTomorrowPrayerTimesOptimized(settings: AppSettings): PrayerTimesWithIqamah? {
            val tomorrow =
                Clock.System.now().plus(
                    1,
                    DateTimeUnit.DAY,
                    TimeZone.currentSystemDefault(),
                )
            val tomorrowDate =
                tomorrow
                    .toLocalDateTime(
                        TimeZone.currentSystemDefault(),
                    ).date
                    .toString()

            // Check if we already have cached tomorrow's prayer times for today
            if (cachedTomorrowFajr != null && cachedTomorrowDate == tomorrowDate) {
                return cachedTomorrowFajr
            }

            return try {
                // Fetch tomorrow's prayer times
                val result = prayerTimesRepository.getTomorrowPrayerTimesFromSettings()

                // Wait for the first emission from the Flow
                var tomorrowTimes: PrayerTimesWithIqamah? = null
                result.collect { networkResult ->
                    when (networkResult) {
                        is NetworkResult.Success -> {
                            // Convert to PrayerTimesWithIqamah using current settings
                            tomorrowTimes = networkResult.data.withIqamahTimes(settings)

                            // Cache the result to avoid repeated fetching
                            cachedTomorrowFajr = tomorrowTimes
                            cachedTomorrowDate = tomorrowDate
                            return@collect
                        }
                        is NetworkResult.Error -> {
                            // Repository handles all modes, including manual, so errors are genuine
                            Log.e("MainViewModel", "Failed to fetch tomorrow's prayer times: ${networkResult.message}")
                            tomorrowTimes = null
                            return@collect
                        }
                        is NetworkResult.Loading -> {
                            // Continue waiting
                        }
                    }
                }
                tomorrowTimes
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching tomorrow's prayer times: ${e.message}")
                null
            }
        }
    }

sealed class MainUiState {
    object Loading : MainUiState()

    data class Success(
        val prayerTimes: PrayerTimesWithIqamah,
        val nextPrayer: PrayerType?,
        val nextDayFajr: PrayerTimesWithIqamah? = null, // For when next prayer is tomorrow's Fajr
    ) : MainUiState()

    data class Error(
        val message: String,
    ) : MainUiState()
}

sealed class WeatherUiState {
    class Loading : WeatherUiState()

    data class Success(
        val weatherInfo: WeatherInfo,
    ) : WeatherUiState()

    data class Error(
        val message: String,
    ) : WeatherUiState()
}
