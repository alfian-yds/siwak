package com.mosque.prayerclock.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerTimes
import com.mosque.prayerclock.data.model.PrayerTimesWithIqamah
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.ui.components.AnalogClock
import com.mosque.prayerclock.ui.components.DigitalClock
import com.mosque.prayerclock.ui.components.DuaForJoiningSaff
import com.mosque.prayerclock.ui.components.FlipClockDigitPair
import com.mosque.prayerclock.ui.components.FullScreenCountdown
import com.mosque.prayerclock.ui.components.JummahInProgress
import com.mosque.prayerclock.ui.components.NextPrayerSection
import com.mosque.prayerclock.ui.components.PrayerTimeCard
import com.mosque.prayerclock.ui.components.WeatherCard
import com.mosque.prayerclock.ui.components.getCountdownData
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorAzanTime
import com.mosque.prayerclock.ui.theme.ColorIqamahTime
import com.mosque.prayerclock.ui.theme.ColorSunriseTime
import com.mosque.prayerclock.utils.NetworkMonitor
import com.mosque.prayerclock.utils.SystemChangeMonitor
import com.mosque.prayerclock.utils.TimeUtils
import com.mosque.prayerclock.viewmodel.MainUiState
import com.mosque.prayerclock.viewmodel.MainViewModel
import com.mosque.prayerclock.viewmodel.WeatherUiState
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

// Helper data class for returning four values
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

// Helper data class for returning six values
data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
)

// Helper data class for returning seven values
data class Septuple<A, B, C, D, E, F, G>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
)

// Data class for full-screen countdown information
data class FullScreenCountdownData(
    val prayerName: String,
    val prayerType: PrayerType,
    val isIqamah: Boolean,
    val minutes: Long,
    val seconds: Long,
    val azanTime: String, // The actual Azan time to display
)

// Data class for Jummah in progress information
data class JummahData(
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long,
)

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Global synchronized animation state for all prayer cards
    var globalShowAzan by remember { mutableStateOf(true) }

    // Synchronized animation timer — SIWAK: hanya berganti bila sakelar pergantian aktif;
    // jika mati, kartu tetap menampilkan Adzan.
    LaunchedEffect(settings.showIqamahCycle) {
        if (!settings.showIqamahCycle) {
            globalShowAzan = true
        } else {
            while (true) {
                kotlinx.coroutines.delay(8000) // 8 seconds (slower, more readable)
                globalShowAzan = !globalShowAzan
            }
        }
    }

    // Only reload data when prayer-related settings change
    LaunchedEffect(settings.prayerServiceType, settings.city, settings.country, settings.showWeather) {
        viewModel.loadPrayerTimes()
    }

    // SIWAK V2: pengaturan jadwal bisa berubah dari server saat layar ini sedang tampil
    // (koreksi menit, koordinat, jam manual, jeda iqamah, tanggal Hijriah). Hitung ulang semuanya,
    // termasuk cache Subuh besok, agar layar langsung sesuai tanpa menunggu ganti hari.
    val scheduleKey =
        listOf(
            settings.latitude, settings.longitude, settings.prayerOffsetEnabled,
            settings.imsakOffset, settings.fajrOffset, settings.sunriseOffset, settings.dhuhrOffset,
            settings.asrOffset, settings.maghribOffset, settings.ishaOffset,
            settings.manualImsak, settings.manualFajrAzan, settings.manualSunrise, settings.manualDhuhrAzan,
            settings.manualAsrAzan, settings.manualMaghribAzan, settings.manualIshaAzan,
            settings.fajrIqamahGap, settings.dhuhrIqamahGap, settings.asrIqamahGap, settings.maghribIqamahGap, settings.ishaIqamahGap,
            settings.hijriProvider, settings.hijriDayOffset, settings.manualHijriDay, settings.manualHijriMonth,
            settings.manualHijriYear, settings.lastUpdatedGregorianDate,
        ).joinToString("|")
    var lastScheduleKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(scheduleKey) {
        if (lastScheduleKey != null && lastScheduleKey != scheduleKey) viewModel.forceRefresh()
        lastScheduleKey = scheduleKey
    }

    // Monitor system time/date/timezone changes to auto-refresh after clock sync
    // This handles the case where device boots without WiFi and later syncs time via NTP
    LaunchedEffect(Unit) {
        SystemChangeMonitor.observeSystemChanges(context).collect { event ->
            Log.d("MainScreen", "System change event: $event")
            viewModel.onSystemChangeEvent(event)
        }
    }

    // Monitor network connectivity to auto-refresh when WiFi becomes available after boot
    LaunchedEffect(Unit) {
        NetworkMonitor.observeNetworkStatus(context).collect { status ->
            Log.d("MainScreen", "Network status: $status")
            viewModel.onNetworkStatusChange(status)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface,
                            ),
                    ),
                ),
    ) {
        MainLayout(
            uiState = uiState,
            weatherState = weatherState,
            settings = settings,
            viewModel = viewModel,
            onOpenSettings = onOpenSettings,
            globalShowAzan = globalShowAzan,
        )
    }
}

@Composable
fun MainLayout(
    uiState: MainUiState,
    weatherState: WeatherUiState,
    settings: AppSettings,
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    globalShowAzan: Boolean,
) {
    val paddingSize = 12.dp

    // Centralized time source for all components
    var globalCurrentTime by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            globalCurrentTime = Clock.System.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    // Calculate countdown visibility, prayer state, full-screen countdown state, dua display, and Jummah display
    val (
        isCountdownVisibleForWeight,
        currentTimeGlobal,
        isCurrentPrayerGlobal,
        shouldShowFullScreenCountdown,
        fullScreenCountdownData,
        shouldShowDua,
        jummahData,
    ) =
        when (uiState) {
            is MainUiState.Success -> {
                // Use centralized time source
                val currentTime = globalCurrentTime
                val prayerInfoList = createPrayerInfoList(uiState.prayerTimes, currentTime)
                val nextPrayerInfo = prayerInfoList.find { it.type == uiState.nextPrayer }

                // Memoize calculations that don't depend on current time
                val calculatedValues =
                    remember(nextPrayerInfo, currentTime) {
                        nextPrayerInfo?.let { prayer ->
                            val now =
                                currentTime.toLocalDateTime(
                                    TimeZone
                                        .currentSystemDefault(),
                                )
                            val currentTimeString =
                                "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"

                            val isAfterAzan =
                                TimeUtils.compareTimeStrings(
                                    prayer.azanTime,
                                    currentTimeString,
                                ) <= 0
                            val isBeforeIqamah =
                                prayer.iqamahTime?.let { iqamahTime ->
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        iqamahTime,
                                    ) < 0
                                }
                                    ?: false
                            val shouldShowIqamah =
                                isAfterAzan &&
                                    isBeforeIqamah &&
                                    prayer.iqamahTime != null

                            // Determine target time first
                            val targetTime =
                                if (shouldShowIqamah) {
                                    prayer.iqamahTime ?: prayer.azanTime
                                } else {
                                    prayer.azanTime
                                }

                            val countdownData =
                                getCountdownData(targetTime, currentTime)

                            // Determine when prayer is considered "current" vs "next"
                            val isCurrentPrayerLocal =
                                if (prayer.iqamahTime != null) {
                                    // For prayers with Iqamah: current from Azan
                                    // time until Iqamah + Dua duration buffer
                                    val bufferTime =
                                        TimeUtils.addMinutesToTime(
                                            prayer.iqamahTime,
                                            settings.duaDisplayDurationMinutes,
                                        )
                                    isAfterAzan &&
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            bufferTime,
                                        ) <= 0
                                } else {
                                    // For prayers without Iqamah: current from Azan time until Azan + buffer
                                    // Use Jummah duration for Dhuhr on Friday, otherwise 60min for Sunrise
                                    val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
                                    val bufferMinutes =
                                        if (prayer.type == PrayerType.DHUHR && isFriday) {
                                            settings.jummahDurationMinutes
                                        } else {
                                            60 // Default 60 minutes for Sunrise
                                        }
                                    val extendedTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, bufferMinutes)
                                    isAfterAzan &&
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            extendedTime,
                                        ) <= 0
                                }

                            // Determine when to hide countdown (only during buffer periods)
                            val isPastFinalTime =
                                if (prayer.iqamahTime != null) {
                                    // Hide countdown after Iqamah time (during buffer period)
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        prayer.iqamahTime,
                                    ) > 0
                                } else {
                                    // Hide countdown after Azan + buffer for prayers without Iqamah
                                    // Use Jummah duration for Dhuhr on Friday, otherwise 60min for Sunrise
                                    val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
                                    val bufferMinutes =
                                        if (prayer.type == PrayerType.DHUHR && isFriday) {
                                            settings.jummahDurationMinutes
                                        } else {
                                            60 // Default 60 minutes for Sunrise
                                        }
                                    val extendedTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, bufferMinutes)
                                    TimeUtils.compareTimeStrings(
                                        currentTimeString,
                                        extendedTime,
                                    ) > 0
                                }

                            // Show countdown if time is zero or positive, within 59 minutes,
                            // and not in buffer period
                            val isCountdownVisible =
                                countdownData.totalSeconds >= 0 &&
                                    countdownData.totalMinutes < 60 &&
                                    !isPastFinalTime

                            // Check if we should show Dua for joining Saff (configurable duration after Iqamah ends)
                            val shouldShowDua =
                                if (prayer.iqamahTime != null && prayer.type != PrayerType.SUNRISE) {
                                    val iqamahEndTime = TimeUtils.addMinutesToTime(prayer.iqamahTime, 0)
                                    val duaEndTime =
                                        TimeUtils.addMinutesToTime(
                                            iqamahEndTime,
                                            settings.duaDisplayDurationMinutes,
                                        )
                                    val isAfterIqamah =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            iqamahEndTime,
                                        ) >= 0
                                    val isBeforeDuaEnd =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            duaEndTime,
                                        ) < 0
                                    isAfterIqamah && isBeforeDuaEnd
                                } else {
                                    false
                                }

                            // Check if full-screen countdown should be shown
                            // Show for 10 minutes before Azan, OR full Iqamah gap when counting to Iqamah
                            val maxCountdownMinutes =
                                if (shouldShowIqamah && prayer.iqamahTime != null) {
                                    // When showing Iqamah countdown, show for the full Iqamah gap
                                    // Calculate the gap by getting minutes between Azan and Iqamah
                                    val azanParts = prayer.azanTime.split(":")
                                    val iqamahParts = prayer.iqamahTime.split(":")
                                    val azanMinutes = azanParts[0].toInt() * 60 + azanParts[1].toInt()
                                    val iqamahMinutes = iqamahParts[0].toInt() * 60 + iqamahParts[1].toInt()
                                    val gapMinutes = iqamahMinutes - azanMinutes
                                    gapMinutes.toLong()
                                } else {
                                    // SIWAK: menit sebelum Adzan (dapat diatur)
                                    settings.fullScreenCountdownMinutes.toLong()
                                }

                            // SIWAK: pakai TOTAL DETIK agar tepat sesuai menit yang diatur
                            // (sebelumnya totalMinutes dibulatkan ke bawah → muncul ~1 menit lebih awal)
                            val shouldShowFullScreenCountdown =
                                settings.fullScreenCountdownEnabled &&
                                    countdownData.totalSeconds in 0..(maxCountdownMinutes * 60) &&
                                    !isPastFinalTime

                            val fullScreenCountdownData =
                                if (shouldShowFullScreenCountdown) {
                                    FullScreenCountdownData(
                                        prayerName = prayer.name,
                                        prayerType = prayer.type,
                                        isIqamah = shouldShowIqamah,
                                        minutes = countdownData.minutes,
                                        seconds = countdownData.seconds,
                                        azanTime = prayer.azanTime,
                                    )
                                } else {
                                    null
                                }

                            // Check if we should show Jummah in progress screen
                            val jummahData =
                                if (settings.showJummahScreen &&
                                    prayer.type == PrayerType.DHUHR &&
                                    now.dayOfWeek == DayOfWeek.FRIDAY &&
                                    prayer.iqamahTime == null
                                ) {
                                    // It's Jummah (Friday Dhuhr with no Iqamah)
                                    val jummahEndTime =
                                        TimeUtils.addMinutesToTime(prayer.azanTime, settings.jummahDurationMinutes)
                                    val isAfterJummahAzan =
                                        TimeUtils.compareTimeStrings(
                                            prayer.azanTime,
                                            currentTimeString,
                                        ) <= 0
                                    val isBeforeJummahEnd =
                                        TimeUtils.compareTimeStrings(
                                            currentTimeString,
                                            jummahEndTime,
                                        ) < 0

                                    if (isAfterJummahAzan && isBeforeJummahEnd) {
                                        // Calculate countdown to Jummah end
                                        val jummahCountdown = getCountdownData(jummahEndTime, currentTime)
                                        JummahData(
                                            hoursRemaining = jummahCountdown.hours,
                                            minutesRemaining = jummahCountdown.minutes,
                                            secondsRemaining = jummahCountdown.seconds,
                                        )
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }

                            Septuple(
                                isCountdownVisible,
                                currentTime,
                                isCurrentPrayerLocal,
                                shouldShowFullScreenCountdown,
                                fullScreenCountdownData,
                                shouldShowDua,
                                jummahData,
                            )
                        }
                            ?: Septuple(false, currentTime, false, false, null, false, null)
                    }

                calculatedValues
            }
            else -> Septuple(false, Clock.System.now(), false, false, null, false, null)
        }

    // ---- SIWAK V2: Galeri, Mutiara Hadits, dan aturan layar (App Flow §3) -----------------
    val galleryVm: com.mosque.prayerclock.ui.gallery.GalleryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val serverContent by galleryVm.content.collectAsStateWithLifecycle()
    val paired by galleryVm.paired.collectAsStateWithLifecycle()
    val hijriToday by galleryVm.hijri.collectAsStateWithLifecycle()
    val localNow = globalCurrentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val nowMinutes = localNow.hour * 60 + localNow.minute
    val successState = uiState as? MainUiState.Success
    val scene =
        successState?.let { s ->
            val m = com.mosque.prayerclock.domain.Periods::minutes
            val subuh = m(s.prayerTimes.fajrAzan) ?: 270
            val terbit = m(s.prayerTimes.sunrise) ?: 345
            val maghrib = m(s.prayerTimes.maghribAzan) ?: 1080
            val isya = m(s.prayerTimes.ishaAzan) ?: 1150
            com.mosque.prayerclock.ui.gallery.SceneState(
                period = com.mosque.prayerclock.domain.Periods.at(nowMinutes, subuh, terbit, maghrib, isya),
                dayProgress = com.mosque.prayerclock.ui.gallery.sunProgress(nowMinutes, terbit, maghrib),
                hijriDay = hijriToday?.first ?: 15,
                silhouette = settings.illustrationSilhouette,
            )
        } ?: com.mosque.prayerclock.ui.gallery.SceneState(com.mosque.prayerclock.domain.SkyPeriod.SIANG, 0.5f, 15, settings.illustrationSilhouette)
    // Layar konten (rc.3): mati = tampilan seperti v1.x; panel samping = tata letak Galeri.
    val contentOn = paired && settings.contentEnabled
    val galleryEnabled = contentOn && settings.layout == "GALERI" && successState != null
    // Daftar putar dibentuk ulang tiap ada konten baru dan tiap menit (jadwal hari/jam slide).
    val playlist =
        remember(serverContent, nowMinutes, localNow.dayOfWeek) {
            com.mosque.prayerclock.domain.SlidePlaylist.build(
                serverContent.slides,
                serverContent.content,
                localNow.dayOfWeek.ordinal, // kotlinx: MONDAY = 0 … SUNDAY = 6
                nowMinutes,
                serverContent.hadiths,
                settings.contentSlideSeconds,
            )
        }

    // Mutiara Hadits: berkala, hanya untuk TV terhubung yang punya hadits.
    var mutiaraLastEnd by remember { mutableStateOf(System.currentTimeMillis()) }
    var mutiaraUntil by remember { mutableStateOf(0L) }
    var mutiaraIndex by remember { mutableStateOf(0) }
    val nowMs = globalCurrentTime.toEpochMilliseconds()
    // Layar penuh berkala memutar daftar yang sama dengan panel samping (slide, info, hadits), bergantian.
    val contentPool = if (contentOn) playlist else emptyList()
    // Perintah "Tampilkan Mutiara Hadits sekarang" dari admin web: anggap jadwal berkala sudah jatuh tempo.
    val mutiaraRequest by galleryVm.mutiaraRequests.collectAsStateWithLifecycle()
    LaunchedEffect(mutiaraRequest) {
        if (mutiaraRequest > 0L && mutiaraUntil == 0L) {
            mutiaraLastEnd = System.currentTimeMillis() - settings.mutiaraEveryMinutes * 60_000L
        }
    }
    val syncStatus by galleryVm.syncStatus.collectAsStateWithLifecycle()
    val appStartedAt = remember { System.currentTimeMillis() }
    val mutiaraDue =
        contentOn && settings.mutiaraHaditsEnabled && contentPool.isNotEmpty() &&
            (mutiaraUntil > nowMs || nowMs - mutiaraLastEnd >= settings.mutiaraEveryMinutes * 60_000L)
    val nightHours =
        settings.nightModeEnabled && isWithinNightHours(localNow.hour, settings.nightStartHour, settings.nightEndHour)
    val secondsUntilPriority =
        successState?.let { s ->
            val next = createPrayerInfoList(s.prayerTimes, globalCurrentTime).find { it.type == s.nextPrayer }
            com.mosque.prayerclock.domain.Periods.minutes(next?.azanTime)?.let { azan ->
                val lead = if (settings.fullScreenCountdownEnabled) settings.fullScreenCountdownMinutes else 0
                var diff = (azan - lead) * 60L - (nowMinutes * 60L + localNow.second)
                if (diff < 0) diff += 24 * 3600
                diff
            }
        }
    val decision =
        com.mosque.prayerclock.domain.ScreenPriorityResolver.resolve(
            com.mosque.prayerclock.domain.ScreenInputs(
                jumatActive = jummahData != null,
                prayerScreenActive = shouldShowDua,
                iqamahCountdownActive = shouldShowFullScreenCountdown && fullScreenCountdownData?.isIqamah == true,
                adzanCountdownActive = shouldShowFullScreenCountdown && fullScreenCountdownData?.isIqamah != true,
                mutiaraDue = mutiaraDue,
                secondsUntilPriorityScreen = secondsUntilPriority,
                nightHours = nightHours,
            ),
        )
    val showMutiara = decision.screen == com.mosque.prayerclock.domain.TvScreen.MUTIARA_HADITS
    LaunchedEffect(showMutiara, nowMs / 1000) {
        if (showMutiara && mutiaraUntil == 0L) {
            mutiaraUntil = nowMs + settings.mutiaraShowSeconds * 1000L
            mutiaraIndex++
        } else if (mutiaraUntil != 0L && (!showMutiara || nowMs >= mutiaraUntil)) {
            mutiaraUntil = 0L
            mutiaraLastEnd = nowMs
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // SIWAK: background kustom (jika diunggah) di belakang seluruh konten
        if (settings.backgroundMainUri.isNotEmpty()) {
            coil.compose.AsyncImage(
                model = settings.backgroundMainUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.55f,
            )
        }
        // Main layout content
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingSize),
        ) {
            // SIWAK V2: tata letak Galeri (Design Brief §6.2): panel slide 58%, kolom kanan 42%.
            if (galleryEnabled && successState != null) {
                Row(
                    modifier = Modifier.weight(0.75f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    com.mosque.prayerclock.ui.gallery.GalleryPanel(
                        playlist = playlist,
                        paused = decision.slidesPaused,
                        scene = scene,
                        mosqueName = settings.mosqueName,
                        mediaUri = { serverContent.mediaUri(it) },
                        hijriToday = hijriToday,
                        modifier = Modifier.weight(0.58f).fillMaxHeight(),
                    )
                    Column(
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (settings.mosqueName.isNotEmpty()) {
                            MosqueHeader(mosqueName = settings.mosqueName, modifier = Modifier.fillMaxWidth(), logoUri = settings.mosqueLogoUri)
                        }
                        // Jam dalam kotak yang sama dengan tata letak Bar bawah (rc.3), dibuat tinggi agar angka besar.
                        ClockSection(
                            settings = settings,
                            onClockClick = onOpenSettings,
                            currentTime = globalCurrentTime,
                            hijriDateRepository = viewModel.hijriDateRepository,
                            modifier = Modifier.fillMaxWidth().weight(1.45f),
                        )
                        // Waktu shalat berikutnya dan cuaca berdampingan, masing-masing dalam kotak.
                        val nextInfo = createPrayerInfoList(successState.prayerTimes, globalCurrentTime).find { it.type == successState.nextPrayer }
                        val remainingText =
                            nextInfo?.let { info ->
                                com.mosque.prayerclock.domain.Periods.minutes(info.azanTime)?.let { azan ->
                                    val left = ((azan - nowMinutes) % 1440 + 1440) % 1440
                                    if (left >= 60) "${left / 60} jam ${left % 60} menit lagi" else "$left menit lagi"
                                }
                            }
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            com.mosque.prayerclock.ui.components.GalleryNextPrayerCard(
                                title = nextInfo?.let { "Adzan ${it.name}" } ?: "Waktu shalat",
                                time = nextInfo?.azanTime ?: "--:--",
                                remaining = remainingText,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            if (settings.showWeather && weatherState is WeatherUiState.Success) {
                                com.mosque.prayerclock.ui.components.GalleryWeatherCard(
                                    info = weatherState.weatherInfo,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            } else
            // Main content row with revamped layout
            Row(
                modifier = Modifier.weight(0.75f),
                horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing between columns
                verticalAlignment = Alignment.Top,
            ) {
                // Left column - Header and Clock section
                Column(
                    modifier = Modifier.weight(1.95f), // SIWAK: jam utama lebih lebar; kolom kanan lebih ramping
                    verticalArrangement = Arrangement.Top, // Remove spacing between header and clock
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Mosque header at top of left column
                    if (settings.mosqueName.isNotEmpty()) {
                        MosqueHeader(
                            mosqueName = settings.mosqueName,
                            modifier = Modifier.fillMaxWidth(),
                            logoUri = settings.mosqueLogoUri,
                        )
                    }

                    // Clock section below header
                    ClockSection(
                        settings = settings,
                        onClockClick = onOpenSettings,
                        currentTime = globalCurrentTime,
                        hijriDateRepository = viewModel.hijriDateRepository,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Right column - Next Prayer and Weather section
                Column(
                    modifier =
                        Modifier
                            .weight(1.0f)
                            .fillMaxHeight(),
                    // Fill available height for proper scaling
                    verticalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing to give more room
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Next prayer timer - Auto-scales based on available space
                    when (uiState) {
                        is MainUiState.Loading -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(if (settings.showWeather) 4f else 1f),
                                // Adjust weight based on weather visibility
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        is MainUiState.Success -> {
                            NextPrayerSection(
                                prayerTimes = uiState.prayerTimes,
                                nextPrayer = uiState.nextPrayer,
                                nextDayFajr = uiState.nextDayFajr,
                                show24Hour = settings.show24HourFormat,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(if (settings.showWeather) 4f else 1f),
                                // Dynamic weight
                                onPrayerTransition = { viewModel.updateNextPrayer() },
                                showCountdown = isCountdownVisibleForWeight,
                                currentTime = currentTimeGlobal,
                                isCurrentPrayer = isCurrentPrayerGlobal,
                            )
                        }
                        is MainUiState.Error -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(if (settings.showWeather) 4f else 1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = getFriendlyErrorMessage(uiState.message),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    // Weather section (if enabled and successful) - Auto-scales with remaining space
                    if (settings.showWeather) {
                        when (weatherState) {
                            is WeatherUiState.Loading -> {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                    // Takes proportional space
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            is WeatherUiState.Success -> {
                                WeatherCard(
                                    weatherInfo = weatherState.weatherInfo,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                    // Takes proportional space
                                )
                            }
                            is WeatherUiState.Error -> {
                                // Hide weather section completely on error
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prayer times at bottom with animation
            when (uiState) {
                is MainUiState.Success -> {
                    AnimatedPrayerTimesSection(
                        prayerTimes = uiState.prayerTimes,
                        nextPrayer = uiState.nextPrayer,
                        show24Hour = settings.show24HourFormat,
                        globalShowAzan = globalShowAzan,
                        showImsak = settings.showImsak,
                        showSunrise = settings.showSunrise,
                        showCycleLabel = settings.showIqamahCycle,
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // SIWAK: running text pengumuman (banyak pesan, dipisah "||")
            val runningMessages =
                settings.runningTextMessages
                    .split("||")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (runningMessages.isNotEmpty()) {
                RunningTextBar(messages = runningMessages)
            }
        }

        // Full-screen countdown overlay (shown when enabled and 10 minutes or less remain)
        if (shouldShowFullScreenCountdown && fullScreenCountdownData != null) {
            FullScreenCountdown(
                prayerName = fullScreenCountdownData.prayerName,
                prayerType = fullScreenCountdownData.prayerType,
                isIqamah = fullScreenCountdownData.isIqamah,
                minutes = fullScreenCountdownData.minutes,
                seconds = fullScreenCountdownData.seconds,
                azanTime = fullScreenCountdownData.azanTime,
                show24Hour = settings.show24HourFormat,
                modifier = Modifier.fillMaxSize(),
                backgroundUri = settings.backgroundIqamahUri,
            )
        }

        // Dua for joining Saff (shown for configurable duration after Iqamah ends)
        if (shouldShowDua) {
            DuaForJoiningSaff(
                modifier = Modifier.fillMaxSize(),
                backgroundUri = settings.backgroundPrayerUri,
            )
        }

        // Jummah in progress screen (shown during Jummah prayer time if enabled)
        if (jummahData != null) {
            JummahInProgress(
                hoursRemaining = jummahData.hoursRemaining,
                minutesRemaining = jummahData.minutesRemaining,
                secondsRemaining = jummahData.secondsRemaining,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // SIWAK V2: layar penuh berkala (prioritas 5): satu konten per kemunculan, bergantian,
        // masuk dan keluar dengan pudar agar tidak muncul tiba-tiba.
        val fullscreenItem = contentPool.getOrNull(mutiaraIndex.mod(contentPool.size.coerceAtLeast(1)))
        androidx.compose.animation.AnimatedVisibility(
            visible = showMutiara && fullscreenItem != null,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(700)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(700)),
        ) {
            val item = remember(mutiaraIndex) { fullscreenItem }
            if (item != null) {
                val next = successState?.let { s -> createPrayerInfoList(s.prayerTimes, globalCurrentTime).find { it.type == s.nextPrayer } }
                val clockText = "%02d:%02d".format(localNow.hour, localNow.minute)
                val nextText = next?.let { "${it.name} ${it.azanTime}" }
                if (item.content.type == "hadith") {
                    com.mosque.prayerclock.ui.gallery.MutiaraHaditsScreen(
                        hadith = item.content,
                        scene = scene,
                        clockText = clockText,
                        nextPrayerText = nextText,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    com.mosque.prayerclock.ui.gallery.ContentFullscreen(
                        item = item,
                        scene = scene,
                        mediaUri = { serverContent.mediaUri(it) },
                        hijriToday = hijriToday,
                        clockText = clockText,
                        nextPrayerText = nextText,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // SIWAK: Mode Istirahat Malam — redup hanya di atas tampilan utama; layar kembali terang
        // otomatis saat hitung mundur, layar shalat, atau layar Jumat (App Flow §3.2).
        if (decision.dimForNight) {
            NightModeOverlay(
                dimLevel = settings.nightDimLevel,
                currentTime = globalCurrentTime,
            )
        }

        // SIWAK V2: penanda kecil bila server tidak terjangkau > 5 menit; tampilan tetap memakai data terakhir.
        if (paired) {
            val lastContact = if (syncStatus.lastOkAt > 0) syncStatus.lastOkAt else appStartedAt
            if (nowMs - lastContact > 5 * 60_000L) {
                com.mosque.prayerclock.ui.server.ServerOfflineChip(
                    sinceMs = syncStatus.lastOkAt.takeIf { it > 0 },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 84.dp),
                )
            }
        }

        // SIWAK V2: banner pengumuman segera, lapisan paling atas (tidak ikut redup malam) untuk
        // prioritas 4–6; disembunyikan saat shalat, hitung mundur iqamah, dan Jumat (App Flow §3.2).
        val activeAnnouncements =
            if (paired) com.mosque.prayerclock.domain.AnnouncementRules.active(serverContent.announcements, nowMs) else emptyList()
        com.mosque.prayerclock.ui.announcement.AnnouncementBanner(
            announcement =
                if (com.mosque.prayerclock.domain.AnnouncementRules.visibleOn(decision.screen)) {
                    com.mosque.prayerclock.domain.AnnouncementRules.current(activeAnnouncements, nowMs)
                } else {
                    null
                },
            // rc.3: banner menempel di bawah, menutupi teks berjalan agar tampilan tetap rapi.
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// SIWAK: apakah jam sekarang berada dalam rentang malam (mendukung lintas tengah malam)
private fun isWithinNightHours(
    hour: Int,
    start: Int,
    end: Int,
): Boolean =
    if (start == end) {
        false
    } else if (start < end) {
        hour in start until end
    } else {
        hour >= start || hour < end
    }

// SIWAK: overlay gelap untuk Mode Istirahat Malam (hanya meredupkan, tanpa jam tambahan)
@Composable
private fun NightModeOverlay(
    dimLevel: Int,
    currentTime: Instant,
) {
    val alpha = (dimLevel.coerceIn(0, 100)) / 100f
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = alpha)),
    )
}

// SIWAK: baris running text pengumuman (marquee bergulir), banyak pesan bergabung
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RunningTextBar(messages: List<String>) {
    val joined = messages.joinToString("        •        ")
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f),
                    androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = joined,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
        )
    }
}

@Composable
private fun MosqueHeader(
    mosqueName: String,
    modifier: Modifier = Modifier,
    logoUri: String = "",
) {
    if (mosqueName.isNotEmpty()) {
        val fontSize = 22.sp // Compact size for same-line layout

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            // Logo on the left — SIWAK: pakai logo unggahan bila ada, jika tidak logo bawaan
            if (logoUri.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = logoUri,
                    contentDescription = "Logo Masjid",
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.mosque_logo),
                    contentDescription = "Mosque Logo",
                    modifier = Modifier.size(48.dp), // Compact size
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Mosque name on the right
            Text(
                text = mosqueName,
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun ClockSection(
    settings: AppSettings,
    onClockClick: () -> Unit,
    hijriDateRepository: HijriDateRepository? = null,
    modifier: Modifier = Modifier,
    currentTime: Instant? = null,
) {
    Card(
        modifier =
            modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClockClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            val boxPadding = 6.dp

            Box(
                modifier = Modifier.padding(boxPadding),
                contentAlignment = Alignment.Center,
            ) {
                // Handle cycling logic for BOTH option with smooth transitions
                // rc.3: jam analog tidak lagi dipakai; selalu digital.
                val actualClockType =
                    when (ClockType.DIGITAL) {
                        ClockType.BOTH -> {
                            // Cycle between Digital and Analog every 2 minutes
                            var currentType by remember { mutableStateOf(ClockType.DIGITAL) }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(120000) // 2 minutes (120 seconds)
                                    currentType =
                                        if (currentType == ClockType.DIGITAL) {
                                            ClockType.ANALOG
                                        } else {
                                            ClockType.DIGITAL
                                        }
                                }
                            }

                            currentType
                        }
                        else -> settings.clockType
                    }

                // Smooth transition between clock types
                AnimatedContent(
                    targetState = actualClockType,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(1000)) togetherWith
                            fadeOut(animationSpec = tween(1000))
                    },
                    label = "clock_transition",
                ) { clockType ->
                    when (clockType) {
                        ClockType.DIGITAL -> {
                            DigitalClock(
                                show24Hour = settings.show24HourFormat,
                                showSeconds = settings.showSeconds,
                                hijriDateRepository = hijriDateRepository,
                                currentTime = currentTime,
                                clockSizeScale = settings.clockSizePercent / 100f,
                                hijriDayOffset = settings.hijriDayOffset,
                            )
                        }
                        ClockType.ANALOG -> {
                            AnalogClock(
                                modifier = Modifier,
                                currentTime = currentTime,
                                hijriDateRepository = hijriDateRepository,
                            )
                        }
                        ClockType.BOTH -> {
                            // This case should not be reached due to the logic above
                            DigitalClock(
                                show24Hour = settings.show24HourFormat,
                                showSeconds = settings.showSeconds,
                                hijriDateRepository = hijriDateRepository,
                                currentTime = currentTime,
                                clockSizeScale = settings.clockSizePercent / 100f,
                                hijriDayOffset = settings.hijriDayOffset,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getFriendlyErrorMessage(technicalMessage: String): String =
    when {
        // Network connectivity issues
        technicalMessage.contains("failed to connect", ignoreCase = true) ||
            technicalMessage.contains("connection refused", ignoreCase = true) ||
            technicalMessage.contains("timeout", ignoreCase = true) ||
            technicalMessage.contains("unable to resolve host", ignoreCase = true) -> {
            "Unable to connect to prayer time service.\nPlease check your internet connection."
        }

        // API or server issues
        technicalMessage.contains("500", ignoreCase = true) ||
            technicalMessage.contains("502", ignoreCase = true) ||
            technicalMessage.contains("503", ignoreCase = true) ||
            technicalMessage.contains("server error", ignoreCase = true) -> {
            "Prayer time service is temporarily unavailable.\nPlease try again later."
        }

        // Data not found
        technicalMessage.contains("404", ignoreCase = true) ||
            technicalMessage.contains("not found", ignoreCase = true) ||
            technicalMessage.contains("no data", ignoreCase = true) -> {
            "Prayer times not available for your location.\nPlease check your settings."
        }

        // General network errors
        technicalMessage.contains("network", ignoreCase = true) -> {
            "Network connection problem.\nPlease check your internet and try again."
        }

        // Generic fallback for any other technical errors
        else -> {
            "Unable to load prayer times.\nPlease check your internet connection and try again."
        }
    }

@Composable
fun AnimatedPrayerTimesSection(
    prayerTimes: PrayerTimesWithIqamah,
    nextPrayer: PrayerType?,
    show24Hour: Boolean = false,
    currentTime: Instant = Clock.System.now(),
    globalShowAzan: Boolean = true,
    showImsak: Boolean = false,
    showSunrise: Boolean = true,
    showCycleLabel: Boolean = true,
) {
    val prayerInfoList = createPrayerInfoList(prayerTimes, currentTime, showImsak, showSunrise)

    val horizontalPadding = 0.5.dp
    val itemSpacing = 0.5.dp

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        prayerInfoList.forEach { prayerInfo ->
            PrayerTimeCard(
                prayerInfo = prayerInfo,
                isNext = prayerInfo.type == nextPrayer,
                show24Hour = show24Hour,
                globalShowAzan = globalShowAzan,
                showCycleLabel = showCycleLabel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun createPrayerInfoList(
    prayerTimes: PrayerTimesWithIqamah,
    currentTime: Instant = Clock.System.now(),
    showImsak: Boolean = false,
    showSunrise: Boolean = true,
): List<PrayerInfo> {
    val currentDate = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val isFriday = currentDate.dayOfWeek == DayOfWeek.FRIDAY

    return buildList {
        // SIWAK: Imsak opsional (jika diaktifkan) di paling depan
        if (showImsak && prayerTimes.imsak.isNotEmpty()) {
            add(
                PrayerInfo(
                    type = PrayerType.IMSAK,
                    azanTime = prayerTimes.imsak,
                    iqamahTime = null,
                    name = localizedStringResource(R.string.imsak),
                ),
            )
        }
        add(
            PrayerInfo(
                type = PrayerType.FAJR,
                azanTime = prayerTimes.fajrAzan,
                iqamahTime = prayerTimes.fajrIqamah,
                name = localizedStringResource(R.string.fajr),
            ),
        )
        // SIWAK: Terbit opsional (dapat disembunyikan)
        if (showSunrise) {
            add(
                PrayerInfo(
                    type = PrayerType.SUNRISE,
                    azanTime = prayerTimes.sunrise,
                    iqamahTime = null,
                    name = localizedStringResource(R.string.sunrise),
                ),
            )
        }
        add(
            PrayerInfo(
                type = PrayerType.DHUHR,
                azanTime = prayerTimes.dhuhrAzan,
                iqamahTime =
                    if (isFriday) {
                        null
                    } else {
                        prayerTimes.dhuhrIqamah
                    }, // No Iqamah on Friday (Bayan instead)
                name =
                    if (isFriday) {
                        localizedStringResource(R.string.jummah)
                    } else {
                        localizedStringResource(R.string.dhuhr)
                    },
            ),
        )
        add(
            PrayerInfo(
                type = PrayerType.ASR,
                azanTime = prayerTimes.asrAzan,
                iqamahTime = prayerTimes.asrIqamah,
                name = localizedStringResource(R.string.asr),
            ),
        )
        add(
            PrayerInfo(
                type = PrayerType.MAGHRIB,
                azanTime = prayerTimes.maghribAzan,
                iqamahTime = prayerTimes.maghribIqamah,
                name = localizedStringResource(R.string.maghrib),
            ),
        )
        add(
            PrayerInfo(
                type = PrayerType.ISHA,
                azanTime = prayerTimes.ishaAzan,
                iqamahTime = prayerTimes.ishaIqamah,
                name = localizedStringResource(R.string.isha),
            ),
        )
    }
}
