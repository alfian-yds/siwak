package com.mosque.prayerclock.data.repository

import android.util.Log
import com.mosque.prayerclock.data.cache.PrayerTimesCacheInvalidator
import com.mosque.prayerclock.data.database.PrayerTimesDao
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.network.AlAdhanData
import com.mosque.prayerclock.data.network.MosqueClockApi
import com.mosque.prayerclock.data.network.NetworkResult
import com.mosque.prayerclock.data.network.PrayerTimesApi
import com.mosque.prayerclock.data.network.toPrayerTimes
import com.mosque.prayerclock.data.scraping.DirectScrapingService
import com.mosque.prayerclock.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class PrayerTimesRepository
    @Inject
    constructor(
        private val api: PrayerTimesApi, // Keep for backward compatibility
        private val mosqueClockApi: MosqueClockApi, // New MosqueClock API
        private val directScrapingService: DirectScrapingService, // Direct PDF scraping
        private val dao: PrayerTimesDao,
        private val settingsRepository: SettingsRepository,
    ) : PrayerTimesCacheInvalidator {
        // Smart caching to prevent unnecessary API calls
        // Tracks what was fetched for today to avoid refetching on non-prayer-related settings changes
        // Repository-level in-memory cache for instant access (no DB queries)
        @Volatile private var cachedPrayerTimes: PrayerTimes? = null

        @Volatile private var cacheDate: String? = null

        @Volatile private var cacheProviderKey: String? = null

        // "MANUAL", "AL_ADHAN_API:Colombo", "MOSQUE_CLOCK_API:1", "DIRECT_SCRAPING:1"
        @Volatile private var isCurrentlyFetching: Boolean = false

        /**
         * Get cached prayer times instantly without any DB queries
         * Returns null if cache is invalid (different date/provider)
         */
        suspend fun getCachedPrayerTimes(): PrayerTimes? {
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()
            val currentProviderKey = getCurrentProviderKey()

            return if (cacheDate == today && cacheProviderKey == currentProviderKey) {
                cachedPrayerTimes
            } else {
                null
            }
        }

        /**
         * Generate provider-specific cache key
         */
        private suspend fun getCurrentProviderKey(): String {
            val settings = settingsRepository.getSettings().first()
            return when (settings.prayerServiceType) {
                PrayerServiceType.MANUAL -> "MANUAL"
                PrayerServiceType.AL_ADHAN_API -> "AL_ADHAN_API:${settings.selectedRegion}"
                PrayerServiceType.ACJU_DIRECT -> "ACJU_DIRECT:${settings.selectedZone}"
                PrayerServiceType.MOSQUE_CLOCK_API -> "MOSQUE_CLOCK_API:${settings.selectedZone}"
            }
        }

        /**
         * Generate composite database ID: "date_providerKey"
         */
        private fun generateDatabaseId(
            date: String,
            providerKey: String,
        ): String = "${date}_$providerKey"

        /**
         * Get prayer times from database using provider-specific lookup
         */
        private suspend fun getPrayerTimesFromDatabase(
            date: String,
            providerKey: String,
        ): PrayerTimes? = dao.getPrayerTimesByDateAndProvider(date, providerKey)

        /**
         * Manually invalidate the prayer times cache
         * Call this when you want to force a fresh fetch regardless of cache state
         */
        override fun invalidatePrayerTimesCache() {
            Log.d("PrayerTimesRepository", "Prayer times cache manually invalidated")
            cachedPrayerTimes = null
            cacheDate = null
            cacheProviderKey = null
        }

        /**
         * Check if we have valid cached data for the current settings
         */
        suspend fun hasCachedPrayerTimesForCurrentSettings(): Boolean = getCachedPrayerTimes() != null

        // Main method that chooses service based on settings with intelligent caching
        fun getTodayPrayerTimesFromSettings(): Flow<NetworkResult<PrayerTimes>> =
            flow {
                Log.d(
                    "PrayerTimesRepository",
                    "🚀 getTodayPrayerTimesFromSettings() called - checking repository cache",
                )
                emit(NetworkResult.Loading())

                try {
                    // First check: Repository-level instant cache (no DB queries)
                    val cachedData = getCachedPrayerTimes()
                    if (cachedData != null) {
                        emit(NetworkResult.Success(cachedData))
                        return@flow
                    }

                    val settings = settingsRepository.getSettings().first()
                    val today =
                        Clock.System
                            .now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                            .toString()
                    val currentProviderKey = getCurrentProviderKey()

                    // Check if another thread is already fetching the same data
                    if (isCurrentlyFetching) {
                        while (isCurrentlyFetching) {
                            kotlinx.coroutines.delay(100) // Wait 100ms
                            val freshCache = getCachedPrayerTimes()
                            if (freshCache != null) {
                                emit(NetworkResult.Success(freshCache))
                                return@flow
                            }
                        }
                    }

                    // Set fetching flag
                    isCurrentlyFetching = true

                    // Fetch data based on service type and emit results
                    when (settings.prayerServiceType) {
                        PrayerServiceType.ACJU_DIRECT -> {
                            Log.d(
                                "PrayerTimesRepository",
                                "🔄 Using ACJU DIRECT SCRAPING for prayer times (Zone: ${settings.selectedZone})",
                            )
                            // Use direct PDF scraping
                            getTodayPrayerTimesByDirectScraping(settings.selectedZone).collect { result ->
                                emit(result)
                                // Cache successful results
                                if (result is NetworkResult.Success) {
                                    Log.d("PrayerTimesRepository", "💾 Caching ACJU_DIRECT data")
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "📋 Original scraping data: date='${result.data.date}', fajr='${result.data.fajrAzan}', dhuhr='${result.data.dhuhrAzan}'",
                                    )

                                    // FIX: Normalize date format and add provider context
                                    val databaseId = generateDatabaseId(today, currentProviderKey)
                                    val normalizedPrayerTimes =
                                        result.data.copy(
                                            id = databaseId,
                                            date = today,
                                            providerKey = currentProviderKey,
                                        )
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "🔧 Normalized: '${result.data.date}' -> '$today', Provider: '$currentProviderKey', ID: '$databaseId'",
                                    )

                                    // Update repository cache for instant access
                                    cachedPrayerTimes = normalizedPrayerTimes
                                    cacheDate = today
                                    cacheProviderKey = currentProviderKey
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "⚡ Repository cache updated - instant access enabled",
                                    )

                                    // Save scraping data to database for persistence across app restarts (provider-specific)
                                    try {
                                        dao.insertPrayerTimes(normalizedPrayerTimes)
                                        Log.d("PrayerTimesRepository", "💾 ACJU_DIRECT data saved to database")
                                    } catch (e: Exception) {
                                        Log.e(
                                            "PrayerTimesRepository",
                                            "❌ Failed to save ACJU_DIRECT data to database",
                                            e,
                                        )
                                    }
                                }
                            }
                        }
                        PrayerServiceType.MOSQUE_CLOCK_API -> {
                            Log.d(
                                "PrayerTimesRepository",
                                "🔄 Using MOSQUE CLOCK API for prayer times (Zone: ${settings.selectedZone})",
                            )
                            // Use MosqueClock backend API
                            getTodayPrayerTimesByZone(settings.selectedZone).collect { result ->
                                emit(result)
                                // Cache successful results
                                if (result is NetworkResult.Success) {
                                    Log.d("PrayerTimesRepository", "💾 Caching MOSQUE_CLOCK_API data")
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "📋 Original API data: date='${result.data.date}', fajr='${result.data.fajrAzan}', dhuhr='${result.data.dhuhrAzan}'",
                                    )

                                    // FIX: Normalize date format and add provider context
                                    val databaseId = generateDatabaseId(today, currentProviderKey)
                                    val normalizedPrayerTimes =
                                        result.data.copy(
                                            id = databaseId,
                                            date = today,
                                            providerKey = currentProviderKey,
                                        )
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "🔧 Normalized: '${result.data.date}' -> '$today', Provider: '$currentProviderKey', ID: '$databaseId'",
                                    )

                                    // Update repository cache for instant access
                                    cachedPrayerTimes = normalizedPrayerTimes
                                    cacheDate = today
                                    cacheProviderKey = currentProviderKey
                                    Log.d(
                                        "PrayerTimesRepository",
                                        "⚡ Repository cache updated - instant access enabled",
                                    )

                                    // Save API data to database for persistence across app restarts (provider-specific)
                                    try {
                                        dao.insertPrayerTimes(normalizedPrayerTimes)
                                        Log.d(
                                            "PrayerTimesRepository",
                                            "💾 MOSQUE_CLOCK_API data saved to database with provider context",
                                        )
                                    } catch (e: Exception) {
                                        Log.e("PrayerTimesRepository", "❌ Database save failed: ${e.message}", e)
                                    }
                                }
                            }
                        }
                        PrayerServiceType.AL_ADHAN_API -> {
                            // SIWAK: Al-Adhan by coordinates (Kemenag method), auto-adjusts to location
                            getPrayerTimesByLocation(settings.latitude, settings.longitude).collect { result ->
                                when (result) {
                                    is NetworkResult.Success -> {
                                        // FIX: Normalize date format and add provider context
                                        val databaseId = generateDatabaseId(today, currentProviderKey)
                                        val normalizedPrayerTimes =
                                            result.data.copy(
                                                id = databaseId,
                                                date = today,
                                                providerKey = currentProviderKey,
                                            )

                                        // Update repository cache for instant access
                                        cachedPrayerTimes = normalizedPrayerTimes
                                        cacheDate = today
                                        cacheProviderKey = currentProviderKey

                                        // Save API data to database for persistence across app restarts
                                        try {
                                            dao.insertPrayerTimes(normalizedPrayerTimes)
                                        } catch (e: Exception) {
                                            Log.e("PrayerTimesRepository", "❌ Database save failed: ${e.message}", e)
                                        }
                                        emit(NetworkResult.Success(normalizedPrayerTimes))
                                    }
                                    is NetworkResult.Error -> {
                                        // SIWAK: gagal sinkron → utamakan jadwal HARI INI yang sudah di-prefetch
                                        // (tepat), lalu fallback ke jadwal tersimpan TERBARU (mis. kemarin).
                                        val exactToday = dao.getPrayerTimesByDateAndProvider(today, currentProviderKey)
                                        val fallback = exactToday ?: dao.getMostRecentPrayerTimes(currentProviderKey)
                                        if (fallback != null) {
                                            val adjusted =
                                                fallback.copy(
                                                    id = generateDatabaseId(today, currentProviderKey),
                                                    date = today,
                                                    providerKey = currentProviderKey,
                                                )
                                            cachedPrayerTimes = adjusted
                                            cacheDate = today
                                            cacheProviderKey = currentProviderKey
                                            Log.w(
                                                "PrayerTimesRepository",
                                                "⚠️ Sinkron gagal — pakai jadwal tersimpan (${fallback.date}) sebagai fallback",
                                            )
                                            emit(NetworkResult.Success(adjusted))
                                        } else {
                                            emit(result)
                                        }
                                    }
                                    is NetworkResult.Loading -> emit(result)
                                }
                            }
                        }
                        PrayerServiceType.MANUAL -> {
                            // Create manual prayer times from settings
                            val basePrayerTimes = createManualPrayerTimes(settings, today)
                            val databaseId = generateDatabaseId(today, currentProviderKey)
                            val manualPrayerTimes =
                                basePrayerTimes.copy(
                                    id = databaseId,
                                    providerKey = null, // Manual entries don't have provider context
                                )
                            Log.d("PrayerTimesRepository", "⚡ Caching MANUAL data (memory only - no database)")
                            Log.d(
                                "PrayerTimesRepository",
                                "📋 Manual data: date='${manualPrayerTimes.date}', fajr='${manualPrayerTimes.fajrAzan}', dhuhr='${manualPrayerTimes.dhuhrAzan}'",
                            )

                            // Update repository cache for instant access (MANUAL entries stay in memory only)
                            cachedPrayerTimes = manualPrayerTimes
                            cacheDate = today
                            cacheProviderKey = currentProviderKey
                            Log.d(
                                "PrayerTimesRepository",
                                "⚡ Repository cache updated - MANUAL data cached in memory only",
                            )

                            emit(NetworkResult.Success(manualPrayerTimes))
                        }
                    }

                    // Clear fetching flag
                    isCurrentlyFetching = false
                    Log.d("PrayerTimesRepository", "Prayer times fetch completed - repository cache updated")
                } catch (e: Exception) {
                    Log.e("PrayerTimesRepository", "Error in getTodayPrayerTimesFromSettings", e)
                    isCurrentlyFetching = false // Clear flag on error
                    // SIWAK: exception (mis. jaringan) → coba jadwal tersimpan terakhir sebagai fallback
                    val fallback =
                        try {
                            val pk = getCurrentProviderKey()
                            val td =
                                Clock.System
                                    .now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date
                                    .toString()
                            dao.getMostRecentPrayerTimes(pk)?.let { recent ->
                                recent.copy(id = generateDatabaseId(td, pk), date = td, providerKey = pk)
                            }
                        } catch (inner: Exception) {
                            null
                        }
                    if (fallback != null) {
                        Log.w("PrayerTimesRepository", "⚠️ Exception saat sinkron — pakai jadwal tersimpan sebagai fallback")
                        emit(NetworkResult.Success(fallback))
                    } else {
                        emit(NetworkResult.Error("Failed to get prayer times: ${e.message}"))
                    }
                }
            }

        // Method to get tomorrow's prayer times based on settings
        fun getTomorrowPrayerTimesFromSettings(): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val settings = settingsRepository.getSettings().first()
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

                when (settings.prayerServiceType) {
                    PrayerServiceType.ACJU_DIRECT -> {
                        // Use direct scraping for tomorrow
                        emitAll(getTomorrowPrayerTimesByDirectScraping(settings.selectedZone))
                    }
                    PrayerServiceType.MOSQUE_CLOCK_API -> {
                        // Use MosqueClock API with selected zone for tomorrow
                        emitAll(getPrayerTimesByZoneAndDate(settings.selectedZone, tomorrowDate))
                    }
                    PrayerServiceType.AL_ADHAN_API -> {
                        // SIWAK: Al-Adhan by coordinates for tomorrow (Kemenag method)
                        emitAll(getTomorrowPrayerTimesByCoordinates(settings.latitude, settings.longitude))
                    }
                    PrayerServiceType.MANUAL -> {
                        // Create manual prayer times from settings (same for tomorrow in manual mode)
                        val manualPrayerTimes = createManualPrayerTimes(settings, tomorrowDate)
                        emit(NetworkResult.Success(manualPrayerTimes))
                    }
                }
            }

        fun getTodayPrayerTimesByRegion(region: String): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val today =
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()

                val cachedPrayerTimes = dao.getPrayerTimesByDate(today)
                if (cachedPrayerTimes != null && cachedPrayerTimes.location?.contains(region) == true) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    val response = api.getPrayerTimesByCity(region, getCountryForRegion(region))
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData)
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getTomorrowPrayerTimesByRegion(region: String): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

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

                // Check cache first for tomorrow's date
                val cachedPrayerTimes = dao.getPrayerTimesByDate(tomorrowDate)
                if (cachedPrayerTimes != null && cachedPrayerTimes.location?.contains(region) == true) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    // Format tomorrow's date for Al-Adhan API (dd-MM-yyyy format)
                    val tomorrowLocalDate =
                        tomorrow
                            .toLocalDateTime(
                                TimeZone.currentSystemDefault(),
                            ).date
                    val tomorrowFormatted = "${tomorrowLocalDate.dayOfMonth.toString().padStart(
                        2,
                        '0',
                    )}-${tomorrowLocalDate.monthNumber.toString().padStart(2, '0')}-${tomorrowLocalDate.year}"

                    val response = api.getPrayerTimesByCity(region, getCountryForRegion(region), tomorrowFormatted)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData)
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        // SIWAK: tomorrow's prayer times by coordinates (Kemenag), used for next-prayer across midnight
        fun getTomorrowPrayerTimesByCoordinates(
            latitude: Double,
            longitude: Double,
        ): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val tomorrow =
                    Clock.System.now().plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                val tomorrowLocalDate =
                    tomorrow.toLocalDateTime(TimeZone.currentSystemDefault()).date
                val tomorrowDate = tomorrowLocalDate.toString()

                val cachedPrayerTimes = dao.getPrayerTimesByDate(tomorrowDate)
                if (cachedPrayerTimes != null) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    val tomorrowFormatted =
                        "${tomorrowLocalDate.dayOfMonth.toString().padStart(2, '0')}-${
                            tomorrowLocalDate.monthNumber.toString().padStart(2, '0')
                        }-${tomorrowLocalDate.year}"

                    val response =
                        api.getPrayerTimesByLocationAndDate(tomorrowFormatted, latitude, longitude)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData)
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        /**
         * SIWAK: Prefetch & simpan jadwal shalat 7 hari ke depan (hari ini..+6) untuk provider AL_ADHAN.
         * Dipanggil saat sinkronisasi agar jadwal tersedia OFFLINE seminggu penuh (hari yang tepat,
         * bukan sekadar jadwal kemarin). Hari yang sudah ada di DB dilewati (jadwal 1 lokasi tetap).
         */
        suspend fun prefetchWeekPrayerTimes(
            latitude: Double,
            longitude: Double,
        ) {
            val providerKey = getCurrentProviderKey()
            val tz = TimeZone.currentSystemDefault()
            var saved = 0
            for (offset in 0..6) {
                try {
                    val date =
                        Clock.System
                            .now()
                            .plus(offset.toLong(), DateTimeUnit.DAY, tz)
                            .toLocalDateTime(tz)
                            .date
                    val dateStr = date.toString() // yyyy-MM-dd
                    // Lewati bila hari itu sudah tersimpan (hemat kuota API)
                    if (dao.getPrayerTimesByDateAndProvider(dateStr, providerKey) != null) continue

                    val ddMMyyyy =
                        "${date.dayOfMonth.toString().padStart(2, '0')}-${
                            date.monthNumber.toString().padStart(2, '0')
                        }-${date.year}"
                    val response = api.getPrayerTimesByLocationAndDate(ddMMyyyy, latitude, longitude)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        response.body()?.data?.let { data ->
                            val base = convertAlAdhanToPrayerTimes(data)
                            val normalized =
                                base.copy(
                                    id = generateDatabaseId(dateStr, providerKey),
                                    date = dateStr,
                                    providerKey = providerKey,
                                )
                            dao.insertPrayerTimes(normalized)
                            saved++
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PrayerTimesRepository", "Prefetch jadwal hari +$offset gagal: ${e.message}")
                }
            }
            Log.d("PrayerTimesRepository", "✅ Prefetch 7 hari selesai ($saved hari baru disimpan)")
        }

        fun getTodayPrayerTimes(
            city: String,
            country: String,
        ): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val today =
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()

                val cachedPrayerTimes = dao.getPrayerTimesByDate(today)
                if (cachedPrayerTimes != null) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    val response = api.getPrayerTimesByCity(city, country)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData)
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getPrayerTimesByLocation(
            latitude: Double,
            longitude: Double,
        ): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    val response = api.getPrayerTimes(latitude, longitude)
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val alAdhanData = response.body()?.data
                        if (alAdhanData != null) {
                            val prayerTimes = convertAlAdhanToPrayerTimes(alAdhanData)
                            dao.insertPrayerTimes(prayerTimes)
                            // SIWAK: catat waktu sinkronisasi jadwal (fetch jaringan berhasil)
                            settingsRepository.updatePrayerLastSync(System.currentTimeMillis())
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        val errorMessage = response.body()?.status ?: "Failed to fetch prayer times"
                        emit(NetworkResult.Error(errorMessage, response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getPrayerTimesByDateFlow(date: String): Flow<PrayerTimes?> = dao.getPrayerTimesByDateFlow(date)

        suspend fun cleanOldPrayerTimes() {
            val thirtyDaysAgo =
                (Clock.System.now() - 30.days)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()
            dao.deleteOldPrayerTimes(thirtyDaysAgo)
        }

        // New methods using direct scraping for Sri Lankan prayer times
        fun getTodayPrayerTimesByDirectScraping(zone: Int): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    Log.d("PrayerTimesRepository", "🔄 Attempting direct scraping for zone $zone")
                    val prayerTimes = directScrapingService.getTodayPrayerTimes(zone, false)
                    if (prayerTimes != null) {
                        Log.d("PrayerTimesRepository", "✅ Direct scraping successful")
                        emit(NetworkResult.Success(prayerTimes))
                    } else {
                        Log.w("PrayerTimesRepository", "⚠️ Direct scraping failed, falling back to backend API")
                        // Fallback to backend API - emit with special flag
                        getTodayPrayerTimesByZone(zone).collect { fallbackResult ->
                            if (fallbackResult is NetworkResult.Success) {
                                Log.d("PrayerTimesRepository", "✅ Fallback API successful, marking as backend data")
                                // Create a copy with backend provider context for proper caching
                                val backendData =
                                    fallbackResult.data.copy(
                                        providerKey = "MOSQUE_CLOCK_API:$zone",
                                    )
                                emit(NetworkResult.Success(backendData))
                            } else {
                                emit(fallbackResult)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PrayerTimesRepository", "❌ Direct scraping error, falling back to backend API: ${e.message}")
                    // Fallback to backend API - emit with special flag
                    getTodayPrayerTimesByZone(zone).collect { fallbackResult ->
                        if (fallbackResult is NetworkResult.Success) {
                            Log.d("PrayerTimesRepository", "✅ Fallback API successful, marking as backend data")
                            // Create a copy with backend provider context for proper caching
                            val backendData =
                                fallbackResult.data.copy(
                                    providerKey = "MOSQUE_CLOCK_API:$zone",
                                )
                            emit(NetworkResult.Success(backendData))
                        } else {
                            emit(fallbackResult)
                        }
                    }
                }
            }

        fun getTomorrowPrayerTimesByDirectScraping(zone: Int): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    Log.d("PrayerTimesRepository", "🔄 Attempting direct scraping for tomorrow, zone $zone")
                    val prayerTimes = directScrapingService.getTomorrowPrayerTimes(zone, false)
                    if (prayerTimes != null) {
                        Log.d("PrayerTimesRepository", "✅ Direct scraping successful for tomorrow")
                        emit(NetworkResult.Success(prayerTimes))
                    } else {
                        Log.w(
                            "PrayerTimesRepository",
                            "⚠️ Direct scraping failed for tomorrow, falling back to backend API",
                        )
                        // Fallback to backend API
                        val tomorrow =
                            Clock.System
                                .now()
                                .plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date
                        getPrayerTimesByZoneAndDate(zone, tomorrow.toString()).collect { fallbackResult ->
                            if (fallbackResult is NetworkResult.Success) {
                                Log.d(
                                    "PrayerTimesRepository",
                                    "✅ Fallback API successful for tomorrow, marking as backend data",
                                )
                                // Create a copy with backend provider context for proper caching
                                val backendData =
                                    fallbackResult.data.copy(
                                        providerKey = "MOSQUE_CLOCK_API:$zone",
                                    )
                                emit(NetworkResult.Success(backendData))
                            } else {
                                emit(fallbackResult)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "PrayerTimesRepository",
                        "❌ Direct scraping error for tomorrow, falling back to backend API: ${e.message}",
                    )
                    // Fallback to backend API
                    val tomorrow =
                        Clock.System
                            .now()
                            .plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                    getPrayerTimesByZoneAndDate(zone, tomorrow.toString()).collect { fallbackResult ->
                        if (fallbackResult is NetworkResult.Success) {
                            Log.d(
                                "PrayerTimesRepository",
                                "✅ Fallback API successful for tomorrow, marking as backend data",
                            )
                            // Create a copy with backend provider context for proper caching
                            val backendData =
                                fallbackResult.data.copy(
                                    providerKey = "MOSQUE_CLOCK_API:$zone",
                                )
                            emit(NetworkResult.Success(backendData))
                        } else {
                            emit(fallbackResult)
                        }
                    }
                }
            }

        // New methods using MosqueClock API for Sri Lankan prayer times
        fun getTodayPrayerTimesByZone(zone: Int): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                val today =
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                        .toString()

                // Check cache first
                val cachedPrayerTimes = dao.getPrayerTimesByDate(today)
                if (cachedPrayerTimes != null && cachedPrayerTimes.location?.contains("Zone $zone") == true) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    val response = mosqueClockApi.getTodayPrayerTimes(zone, false)
                    if (response.isSuccessful && response.body() != null) {
                        val backendData = response.body()!!
                        val prayerTimes = backendData.toPrayerTimes()
                        dao.insertPrayerTimes(prayerTimes)
                        emit(NetworkResult.Success(prayerTimes))
                    } else {
                        emit(NetworkResult.Error("Failed to fetch prayer times", response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getPrayerTimesByZoneAndDate(
            zone: Int,
            date: String,
        ): Flow<NetworkResult<PrayerTimes>> =
            flow {
                emit(NetworkResult.Loading())

                // Check cache first
                val cachedPrayerTimes = dao.getPrayerTimesByDate(date)
                if (cachedPrayerTimes != null && cachedPrayerTimes.location?.contains("Zone $zone") == true) {
                    emit(NetworkResult.Success(cachedPrayerTimes))
                    return@flow
                }

                try {
                    val response = mosqueClockApi.getPrayerTimesByZone(zone, date)
                    if (response.isSuccessful && response.body() != null) {
                        val mosqueDataList = response.body()?.data
                        if (mosqueDataList != null && mosqueDataList.isNotEmpty()) {
                            // Take the first prayer time entry (should be for the requested date)
                            val prayerTimes = mosqueDataList.first().toPrayerTimes()
                            dao.insertPrayerTimes(prayerTimes)
                            emit(NetworkResult.Success(prayerTimes))
                        } else {
                            emit(NetworkResult.Error("No prayer times data available"))
                        }
                    } else {
                        emit(NetworkResult.Error("Failed to fetch prayer times", response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        fun getMonthPrayerTimes(
            zone: Int,
            year: Int,
            month: Int,
        ): Flow<NetworkResult<List<PrayerTimes>>> =
            flow {
                emit(NetworkResult.Loading())

                try {
                    // Convert month number to month name for backend API
                    val monthName = getMonthName(month)
                    val response = mosqueClockApi.getMonthPrayerTimes(zone, year, monthName)
                    if (response.isSuccessful && response.body() != null) {
                        // The backend returns a single response with a list of prayer times
                        // We need to extract the prayer_times array from the metadata response
                        // For now, let's handle this as an error since the API structure is complex
                        emit(NetworkResult.Error("Month prayer times not implemented yet"))
                    } else {
                        emit(NetworkResult.Error("Failed to fetch month prayer times", response.code()))
                    }
                } catch (e: Exception) {
                    emit(NetworkResult.Error("Network error: ${e.message}"))
                }
            }

        private fun convertAlAdhanToPrayerTimes(alAdhanData: AlAdhanData): PrayerTimes {
            val timings = alAdhanData.timings
            val date = alAdhanData.date.gregorian.date
            val hijriDate = alAdhanData.date.hijri.date

            // Note: Iqamah times are no longer stored, they will be calculated dynamically from settings
            return PrayerTimes(
                id = "", // Will be set later with proper composite ID
                date = date,
                providerKey = null, // Will be set later with provider context
                fajrAzan = formatTime(timings.fajr),
                dhuhrAzan = formatTime(timings.dhuhr),
                asrAzan = formatTime(timings.asr),
                maghribAzan = formatTime(timings.maghrib),
                ishaAzan = formatTime(timings.isha),
                sunrise = formatTime(timings.sunrise),
                imsak = formatTime(timings.imsak),
                hijriDate = hijriDate,
                location = "${alAdhanData.meta.latitude}, ${alAdhanData.meta.longitude}",
            )
        }

        private fun formatTime(time: String): String {
            // AlAdhan returns time in HH:MM format, sometimes with timezone info
            return time.split(" ").first() // Remove timezone if present
        }

        private fun getCountryForRegion(region: String): String =
            when (region) {
                "Colombo", "Kandy", "Galle", "Jaffna" -> "Sri Lanka"
                "Kuala Lumpur", "Penang" -> "Malaysia"
                "Singapore" -> "Singapore"
                "Jakarta" -> "Indonesia"
                "Chennai", "Mumbai", "Delhi" -> "India"
                "Dubai" -> "UAE"
                "Riyadh" -> "Saudi Arabia"
                "Doha" -> "Qatar"
                "London" -> "UK"
                "New York" -> "USA"
                "Toronto" -> "Canada"
                else -> "Sri Lanka" // Default fallback
            }

        private fun getMonthName(month: Int): String =
            when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                12 -> "December"
                else -> "January"
            }

        /**
         * Create manual prayer times from settings
         */
        private fun createManualPrayerTimes(
            settings: AppSettings,
            date: String? = null,
        ): PrayerTimes {
            // Note: Iqamah times are no longer stored, they will be calculated dynamically from settings
            val currentDate =
                date ?: Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toString()

            return PrayerTimes(
                id = "", // Will be set later with proper composite ID
                date = currentDate,
                providerKey = null, // Manual entries don't have provider context
                fajrAzan = settings.manualFajrAzan,
                dhuhrAzan = settings.manualDhuhrAzan,
                asrAzan = settings.manualAsrAzan,
                maghribAzan = settings.manualMaghribAzan,
                ishaAzan = settings.manualIshaAzan,
                sunrise = settings.manualSunrise,
                imsak = settings.manualImsak, // SIWAK: Imsak manual
                hijriDate = null,
                location = "Manual",
            )
        }

        /**
         * Prefetch all remaining months of the year for offline use
         * This is only applicable when using ACJU Direct scraping
         */
        suspend fun prefetchRemainingYearPrayerTimes(zone: Int): DirectScrapingService.PrefetchResult {
            Log.d("PrayerTimesRepository", "🔄 Starting prefetch for zone $zone")
            return directScrapingService.prefetchRemainingYearPrayerTimes(zone)
        }

        /**
         * Check offline cache status
         */
        suspend fun checkOfflineCacheStatus(zone: Int): DirectScrapingService.CacheStatus {
            return directScrapingService.checkCacheStatus(zone)
        }

        /**
         * Clear all cached prayer times
         */
        suspend fun clearAllPrayerTimesCache() {
            val count = dao.getAllPrayerTimesCount()
            dao.deleteAllPrayerTimes()
            // Clear in-memory cache as well
            cachedPrayerTimes = null
            cacheDate = null
            cacheProviderKey = null
            Log.d("PrayerTimesRepository", "🧹 Cleared all prayer times cache ($count entries)")
        }

        /**
         * Get total count of cached prayer times
         */
        suspend fun getPrayerTimesCacheCount(): Int = dao.getAllPrayerTimesCount()
    }
