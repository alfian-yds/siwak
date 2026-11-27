package com.mosque.prayerclock.ui.screens

import androidx.compose.material.icons.rounded.*
import com.mosque.prayerclock.ui.tv.tvFocusHighlight

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import com.mosque.prayerclock.ui.tv.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.mosque.prayerclock.ui.tv.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import com.mosque.prayerclock.ui.tv.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.mosque.prayerclock.ui.localizedStringResource as stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mosque.prayerclock.BuildConfig
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.IndonesianCities
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.ui.components.AppPickerDialog
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.PrayerZones
import com.mosque.prayerclock.data.model.SoundType
import com.mosque.prayerclock.data.model.WeatherProvider
import com.mosque.prayerclock.utils.PermissionsHelper
import com.mosque.prayerclock.data.service.ApkDownloader
import com.mosque.prayerclock.data.service.UpdateChecker
import com.mosque.prayerclock.data.service.UpdateInfo
import com.mosque.prayerclock.ui.components.MarkdownText
import com.mosque.prayerclock.ui.theme.AppColorThemes
import com.mosque.prayerclock.utils.LauncherHelper
import com.mosque.prayerclock.viewmodel.CacheStats
import com.mosque.prayerclock.viewmodel.SettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onConnectServer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSyncingPrayer by viewModel.isSyncingPrayer.collectAsStateWithLifecycle()
    val isSyncingWeather by viewModel.isSyncingWeather.collectAsStateWithLifecycle()

    // Handle back button to navigate back to main screen, not exit app
    BackHandler { onNavigateBack() }

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
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
            SettingsHeader(onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { SettingsGroupHeader("Koneksi") }
                item {
                    ConnectServerCard(onConnectServer)
                }
                item { SettingsGroupHeader("Jadwal") }
                item {
                    MosqueNameSetting(
                        mosqueName = settings.mosqueName,
                        onMosqueNameChange = viewModel::updateMosqueName,
                    )
                }
                item {
                    PrayerServiceSettings(
                        selectedServiceType = settings.prayerServiceType,
                        selectedZone = settings.selectedZone,
                        selectedRegion = settings.selectedRegion,
                        selectedCity = settings.city,
                        selectedLatitude = settings.latitude,
                        selectedLongitude = settings.longitude,
                        backendUrl = settings.mosqueClockBackendUrl,
                        backendApiKey = settings.mosqueClockBackendApiKey,
                        onServiceTypeChange = viewModel::updatePrayerServiceType,
                        onZoneChange = viewModel::updateSelectedZone,
                        onRegionChange = viewModel::updateSelectedRegion,
                        onCityCoordChange = viewModel::updateLocationCity,
                        onCoordChange = viewModel::updateCoordinates,
                        prayerLastSyncMillis = settings.prayerLastSyncMillis,
                        isSyncingPrayer = isSyncingPrayer,
                        onSyncPrayer = viewModel::syncPrayerTimesNow,
                        onBackendUrlChange = viewModel::updateMosqueClockBackendUrl,
                        onBackendApiKeyChange = viewModel::updateMosqueClockBackendApiKey,
                    )
                }
                if (settings.prayerServiceType == PrayerServiceType.MANUAL) {
                    item {
                        ManualPrayerTimesSettings(
                            settings = settings,
                            onUpdateManualTime = viewModel::updateManualTime,
                        )
                    }
                }
                item {
                    DisplayVisibilitySettings(
                        showSunrise = settings.showSunrise,
                        showImsak = settings.showImsak,
                        onShowSunriseChange = viewModel::updateShowSunrise,
                        onShowImsakChange = viewModel::updateShowImsak,
                    )
                }
                item {
                    PrayerOffsetSettings(
                        settings = settings,
                        onUpdateOffset = viewModel::updatePrayerOffset,
                        onEnabledChange = viewModel::updatePrayerOffsetEnabled,
                    )
                }
                item {
                    HijriDateSettings(
                        hijriProvider = settings.hijriProvider,
                        day = settings.manualHijriDay,
                        month = settings.manualHijriMonth,
                        year = settings.manualHijriYear,
                        hijriDayOffset = settings.hijriDayOffset,
                        onHijriProviderChange = { viewModel.updateHijriProvider(it) },
                        onManualChange = { d, m, y -> viewModel.updateHijriDate(d, m, y) },
                        onHijriDayOffsetChange = viewModel::updateHijriDayOffset,
                    )
                }
                item {
                    IqamahGapSettings(
                        settings = settings,
                        onUpdateIqamahGap = viewModel::updateIqamahGap,
                        showIqamahCycle = settings.showIqamahCycle,
                        onShowIqamahCycleChange = viewModel::updateShowIqamahCycle,
                    )
                }
                item {
                    FullScreenCountdownSettings(
                        fullScreenCountdownEnabled = settings.fullScreenCountdownEnabled,
                        fullScreenCountdownMinutes = settings.fullScreenCountdownMinutes,
                        onFullScreenCountdownEnabledChange = viewModel::updateFullScreenCountdownEnabled,
                        onFullScreenCountdownMinutesChange = viewModel::updateFullScreenCountdownMinutes,
                    )
                }
                item {
                    PrayerDisplaySettings(
                        jummahDurationMinutes = settings.jummahDurationMinutes,
                        duaDisplayDurationMinutes = settings.duaDisplayDurationMinutes,
                        showJummahScreen = settings.showJummahScreen,
                        onJummahDurationChange = viewModel::updateJummahDurationMinutes,
                        onDuaDisplayDurationChange = viewModel::updateDuaDisplayDurationMinutes,
                        onShowJummahScreenChange = viewModel::updateShowJummahScreen,
                    )
                }
                item {
                    JummaNightBayanSettings(
                        jummaNightBayanEnabled = settings.jummaNightBayanEnabled,
                        jummaNightBayanMinutes = settings.jummaNightBayanMinutes,
                        onJummaNightBayanEnabledChange = viewModel::updateJummaNightBayanEnabled,
                        onJummaNightBayanMinutesChange = viewModel::updateJummaNightBayanMinutes,
                    )
                }
                item { SettingsGroupHeader("Suara") }
                item {
                    AzanSoundSettings(
                        azanSoundEnabled = settings.azanSoundEnabled,
                        azanSoundType = settings.azanSoundType,
                        azanSoundUri = settings.azanSoundUri,
                        onAzanSoundEnabledChange = viewModel::updateAzanSoundEnabled,
                        onAzanSoundTypeChange = viewModel::updateAzanSoundType,
                        onAzanSoundUriChange = viewModel::updateAzanSoundUri,
                        onTest = { viewModel.testSound(true) },
                    )
                }
                item {
                    IqamahSoundSettings(
                        iqamahSoundEnabled = settings.iqamahSoundEnabled,
                        iqamahSoundType = settings.iqamahSoundType,
                        iqamahSoundUri = settings.iqamahSoundUri,
                        onIqamahSoundEnabledChange = viewModel::updateIqamahSoundEnabled,
                        onIqamahSoundTypeChange = viewModel::updateIqamahSoundType,
                        onIqamahSoundUriChange = viewModel::updateIqamahSoundUri,
                        onTest = { viewModel.testSound(false) },
                    )
                }
                item { SettingsGroupHeader("Tampilan") }
                item {
                    MediaSettings(
                        logoUri = settings.mosqueLogoUri,
                        backgroundUri = settings.backgroundMainUri,
                        backgroundPrayerUri = settings.backgroundPrayerUri,
                        backgroundIqamahUri = settings.backgroundIqamahUri,
                        onLogoChange = viewModel::updateMosqueLogoUri,
                        onBackgroundChange = viewModel::updateBackgroundMainUri,
                        onBackgroundPrayerChange = viewModel::updateBackgroundPrayerUri,
                        onBackgroundIqamahChange = viewModel::updateBackgroundIqamahUri,
                    )
                }
                item {
                    ColorThemeSettings(
                        selectedThemeId = settings.colorTheme,
                        onThemeChange = viewModel::updateColorTheme,
                    )
                }
                item {
                    LanguageSetting(
                        selectedLanguage = settings.language,
                        onLanguageChange = viewModel::updateLanguage,
                    )
                }
                item {
                    ClockFormatSettings(
                        showSeconds = settings.showSeconds,
                        show24Hour = settings.show24HourFormat,
                        onShowSecondsChange = viewModel::updateShowSeconds,
                        onShow24HourChange = viewModel::updateShow24HourFormat,
                    )
                }
                item {
                    NightModeSettings(
                        enabled = settings.nightModeEnabled,
                        startHour = settings.nightStartHour,
                        endHour = settings.nightEndHour,
                        dimLevel = settings.nightDimLevel,
                        onEnabledChange = viewModel::updateNightModeEnabled,
                        onStartHourChange = viewModel::updateNightStartHour,
                        onEndHourChange = viewModel::updateNightEndHour,
                        onDimLevelChange = viewModel::updateNightDimLevel,
                        showDimLevel = settings.nightModeEnabled || settings.dimDaysEnabled,
                    )
                }
                item {
                    DimDaysSettings(
                        enabled = settings.dimDaysEnabled,
                        weekdays = com.mosque.prayerclock.domain.DimDays.parseWeekdays(settings.dimWeekdays),
                        nationalHolidays = settings.dimNationalHolidays,
                        cutiBersama = settings.dimCutiBersama,
                        brightenForPrayer = settings.dimBrightenForPrayer,
                        onEnabledChange = viewModel::updateDimDaysEnabled,
                        onWeekdaysChange = viewModel::updateDimWeekdays,
                        onNationalHolidaysChange = viewModel::updateDimNationalHolidays,
                        onCutiBersamaChange = viewModel::updateDimCutiBersama,
                        onBrightenForPrayerChange = viewModel::updateDimBrightenForPrayer,
                    )
                }
                item {
                    WeatherSettings(
                        showWeather = settings.showWeather,
                        onToggleShowWeather = viewModel::updateShowWeather,
                        weatherCity = settings.weatherCity,
                        onWeatherCityChange = viewModel::updateWeatherCity,
                        provider = settings.weatherProvider,
                        onProviderChange = viewModel::updateWeatherProvider,
                        weatherApiKey = settings.weatherApiKey,
                        openWeatherMapApiKey = settings.openWeatherMapApiKey,
                        onWeatherApiKeyChange = viewModel::updateWeatherApiKey,
                        onOpenWeatherMapApiKeyChange = viewModel::updateOpenWeatherMapApiKey,
                        weatherLastSyncMillis = settings.weatherLastSyncMillis,
                        isSyncingWeather = isSyncingWeather,
                        onSyncWeather = viewModel::syncWeatherNow,
                    )
                }
                item { SettingsGroupHeader("Konten") }
                item {
                    RunningTextSettings(
                        text = settings.runningTextMessages,
                        onTextChange = viewModel::updateRunningTextMessages,
                    )
                }
                item { SettingsGroupHeader("Sistem") }
                item {
                    com.mosque.prayerclock.ui.server.PermissionsCard()
                }
                item {
                    SystemSettingsAccess()
                }
                item {
                    DataManagementSettings(
                        viewModel = viewModel,
                    )
                }
                item {
                    AboutSettings(
                        autoUpdateCheckEnabled = settings.autoUpdateCheckEnabled,
                        onAutoUpdateCheckEnabledChange = viewModel::updateAutoUpdateCheckEnabled,
                    )
                }
            }
        }
    }
}

/** Ikon flat untuk judul kartu pengaturan (menggantikan emoji), warnanya mengikuti tema. */
@Composable
internal fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(end = 12.dp).size(28.dp),
    )
}

/** Judul kelompok. Urutan dan nama kartu sama dengan menu Pengaturan di website (Jadwal, Suara, Tampilan, Konten). */
@Composable
internal fun SettingsGroupHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp),
    )
}

@Composable
private fun LongPressIconButton(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    stepDelay: Long = 100L, // Repeat interval in milliseconds
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val longPressListener by rememberUpdatedState(onLongPress)

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(500) // Initial delay before starting repeat (long press threshold)
            while (isPressed) {
                longPressListener()
                delay(stepDelay.coerceIn(1L, Long.MAX_VALUE))
            }
        }
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        interactionSource = interactionSource,
    ) {
        content()
    }
}

@Composable
internal fun SettingsHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onNavigateBack,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
        ) { Text(stringResource(R.string.back)) }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.settings),
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MosqueNameSetting(
    mosqueName: String,
    onMosqueNameChange: (String) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                SettingsIcon(Icons.Rounded.Mosque)
                Text(
                    text = stringResource(R.string.mosque_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            ImprovedTextField(
                value = mosqueName,
                onValueChange = onMosqueNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.mosque_name_hint),
                keyboardType = KeyboardType.Text,
            )
        }
    }
}

@Composable
private fun LanguageSetting(
    selectedLanguage: Language,
    onLanguageChange: (Language) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                SettingsIcon(Icons.Rounded.Language)
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Language.values().forEach { language ->
                SelectableSettingsRow(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageChange(language) },
                    text = language.displayName,
                )
            }
        }
    }
}

@Composable
private fun PrayerServiceSettings(
    selectedServiceType: PrayerServiceType,
    selectedZone: Int,
    selectedRegion: String,
    selectedCity: String,
    selectedLatitude: Double,
    selectedLongitude: Double,
    backendUrl: String,
    backendApiKey: String,
    onServiceTypeChange: (PrayerServiceType) -> Unit,
    onZoneChange: (Int) -> Unit,
    onRegionChange: (String) -> Unit,
    onCityCoordChange: (String, Double, Double) -> Unit,
    onCoordChange: (Double, Double) -> Unit,
    prayerLastSyncMillis: Long,
    isSyncingPrayer: Boolean,
    onSyncPrayer: () -> Unit,
    onBackendUrlChange: (String) -> Unit,
    onBackendApiKeyChange: (String) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                SettingsIcon(Icons.Rounded.EventNote)
                Text(
                    text = stringResource(R.string.prayer_service_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = stringResource(R.string.prayer_service_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // SIWAK: hanya tampilkan Otomatis (Kemenag) & Manual; opsi Sri Lanka disembunyikan
            val serviceOptions = listOf(PrayerServiceType.AL_ADHAN_API, PrayerServiceType.MANUAL)
            serviceOptions.forEach { serviceType ->
                SelectableSettingsRow(
                    selected = selectedServiceType == serviceType,
                    onClick = { onServiceTypeChange(serviceType) },
                    text = serviceType.displayName,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backend configuration for MosqueClock API
            if (selectedServiceType == PrayerServiceType.MOSQUE_CLOCK_API) {
                Text(
                    text = "Backend Configuration",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.backend_url_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Example: http://192.168.1.100:8000/ or https://api.yourdomain.com/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                ImprovedTextField(
                    value = backendUrl,
                    onValueChange = onBackendUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.backend_url_hint),
                    keyboardType = KeyboardType.Uri,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.backend_api_key_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Kunci autentikasi backend",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                ImprovedTextField(
                    value = backendApiKey,
                    onValueChange = onBackendApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.backend_api_key_hint),
                    keyboardType = KeyboardType.Text,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Zone selection for MosqueClock backend and ACJU Direct (both use Sri Lankan zones)
            if (selectedServiceType == PrayerServiceType.MOSQUE_CLOCK_API ||
                selectedServiceType == PrayerServiceType.ACJU_DIRECT
            ) {
                ZoneSelection(
                    selectedZone = selectedZone,
                    onZoneChange = onZoneChange,
                )
            }

            // SIWAK: pemilih Kota/Kabupaten Indonesia (menetapkan koordinat) untuk metode Kemenag
            if (selectedServiceType == PrayerServiceType.AL_ADHAN_API) {
                Spacer(modifier = Modifier.height(8.dp))
                KotaSelection(
                    selectedCity = selectedCity,
                    onCityCoordChange = onCityCoordChange,
                )
                Spacer(modifier = Modifier.height(12.dp))
                CoordinateInput(
                    latitude = selectedLatitude,
                    longitude = selectedLongitude,
                    onCoordChange = onCoordChange,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SyncRow(
                    lastSyncMillis = prayerLastSyncMillis,
                    isSyncing = isSyncingPrayer,
                    onSync = onSyncPrayer,
                )
            }
        }
    }
}

@Composable
private fun ZoneSelection(
    selectedZone: Int,
    onZoneChange: (Int) -> Unit,
) {
    var showZoneDropdown by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Sri Lankan Zone",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showZoneDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selectedZoneData = PrayerZones.zones.find { it.id == selectedZone }
                    Text(
                        text =
                            selectedZoneData?.let { "${it.name}: ${it.description}" }
                                ?: "Select Zone",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text("▼", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            DropdownMenu(
                expanded = showZoneDropdown,
                onDismissRequest = { showZoneDropdown = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                PrayerZones.zones.forEach { zone ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = zone.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = zone.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.7f,
                                        ),
                                )
                            }
                        },
                        onClick = {
                            onZoneChange(zone.id)
                            showZoneDropdown = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionSelection(
    selectedRegion: String,
    onRegionChange: (String) -> Unit,
) {
    val thirdPartyRegions =
        remember {
            listOf(
                "Colombo",
                "Kandy",
                "Galle",
                "Jaffna",
                "Kuala Lumpur",
                "Penang",
                "Singapore",
                "Jakarta",
                "Chennai",
                "Mumbai",
                "Delhi",
                "Dubai",
                "Riyadh",
                "Doha",
                "London",
                "New York",
                "Toronto",
            )
        }

    var showRegionDropdown by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "City/Region",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showRegionDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            if (selectedRegion.isNotEmpty()) {
                                selectedRegion
                            } else {
                                "Select Region"
                            },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text("▼", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            DropdownMenu(
                expanded = showRegionDropdown,
                onDismissRequest = { showRegionDropdown = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                thirdPartyRegions.forEach { region ->
                    DropdownMenuItem(
                        text = { Text(region) },
                        onClick = {
                            onRegionChange(region)
                            showRegionDropdown = false
                        },
                    )
                }
            }
        }
    }
}

// SIWAK: pemilih Kota/Kabupaten Indonesia; saat dipilih menetapkan koordinat (lat/long)
@Composable
private fun KotaSelection(
    selectedCity: String,
    onCityCoordChange: (String, Double, Double) -> Unit,
) {
    var showDropdown by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.select_city_kabupaten),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedCity.ifEmpty { stringResource(R.string.select_city_hint) },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text("▼", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                IndonesianCities.list.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name) },
                        onClick = {
                            onCityCoordChange(c.name, c.latitude, c.longitude)
                            showDropdown = false
                        },
                    )
                }
            }
        }
    }
}

// SIWAK: input koordinat manual (lat/long) untuk akurasi maksimum
@Composable
private fun CoordinateInput(
    latitude: Double,
    longitude: Double,
    onCoordChange: (Double, Double) -> Unit,
) {
    var latText by remember(latitude) { mutableStateOf(latitude.toString()) }
    var lonText by remember(longitude) { mutableStateOf(longitude.toString()) }

    Column {
        Text(
            text = stringResource(R.string.manual_coordinates),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.manual_coordinates_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.latitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ImprovedTextField(
                    value = latText,
                    onValueChange = {
                        latText = it
                        val lat = it.toDoubleOrNull()
                        val lon = lonText.toDoubleOrNull()
                        if (lat != null && lon != null) onCoordChange(lat, lon)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "-7.8014",
                    keyboardType = KeyboardType.Text,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ImprovedTextField(
                    value = lonText,
                    onValueChange = {
                        lonText = it
                        val lat = latText.toDoubleOrNull()
                        val lon = it.toDoubleOrNull()
                        if (lat != null && lon != null) onCoordChange(lat, lon)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "110.3647",
                    keyboardType = KeyboardType.Text,
                )
            }
        }
    }
}

// SIWAK: baris "Update terakhir" + tombol sinkronisasi manual
@Composable
private fun SyncRow(
    lastSyncMillis: Long,
    isSyncing: Boolean,
    onSync: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.last_updated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text =
                    if (lastSyncMillis > 0L) {
                        formatLastSync(lastSyncMillis)
                    } else {
                        stringResource(R.string.never_synced)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlinedButton(onClick = onSync, enabled = !isSyncing) {
            Text(
                if (isSyncing) {
                    stringResource(R.string.syncing)
                } else {
                    stringResource(R.string.sync_now)
                },
            )
        }
    }
}

private fun formatLastSync(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("in", "ID"))
    return sdf.format(java.util.Date(millis))
}

@Composable
private fun LocationSettings(
    city: String,
    country: String,
    onCityChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
) {
    val cities =
        remember {
            listOf(
                "Colombo" to "Sri Lanka",
                "Kandy" to "Sri Lanka",
                "Galle" to "Sri Lanka",
                "Jaffna" to "Sri Lanka",
                "Kuala Lumpur" to "Malaysia",
                "Penang" to "Malaysia",
                "Johor Bahru" to "Malaysia",
                "Singapore" to "Singapore",
                "Jakarta" to "Indonesia",
                "Bandung" to "Indonesia",
                "Surabaya" to "Indonesia",
                "Chennai" to "India",
                "Mumbai" to "India",
                "Delhi" to "India",
                "Bangalore" to "India",
                "Hyderabad" to "India",
                "Dubai" to "UAE",
                "Abu Dhabi" to "UAE",
                "Riyadh" to "Saudi Arabia",
                "Jeddah" to "Saudi Arabia",
                "Mecca" to "Saudi Arabia",
                "Medina" to "Saudi Arabia",
                "Doha" to "Qatar",
                "Kuwait City" to "Kuwait",
                "London" to "UK",
                "Manchester" to "UK",
                "Birmingham" to "UK",
                "Toronto" to "Canada",
                "Vancouver" to "Canada",
                "New York" to "USA",
                "Los Angeles" to "USA",
                "Chicago" to "USA",
                "Sydney" to "Australia",
                "Melbourne" to "Australia",
                "Perth" to "Australia",
            )
        }

    var showCityDropdown by remember { mutableStateOf(false) }

    SettingsCard {
        Column {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // City Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showCityDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (city.isNotEmpty()) "$city, $country" else "Select City",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text("▼", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                DropdownMenu(
                    expanded = showCityDropdown,
                    onDismissRequest = { showCityDropdown = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    cities.forEach { (cityName, countryName) ->
                        DropdownMenuItem(
                            text = { Text("$cityName, $countryName") },
                            onClick = {
                                onCityChange(cityName)
                                onCountryChange(countryName)
                                showCityDropdown = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSettings(
    showWeather: Boolean,
    onToggleShowWeather: (Boolean) -> Unit,
    weatherCity: String,
    onWeatherCityChange: (String) -> Unit,
    provider: WeatherProvider,
    onProviderChange: (WeatherProvider) -> Unit,
    weatherApiKey: String,
    openWeatherMapApiKey: String,
    onWeatherApiKeyChange: (String) -> Unit,
    onOpenWeatherMapApiKeyChange: (String) -> Unit,
    weatherLastSyncMillis: Long,
    isSyncingWeather: Boolean,
    onSyncWeather: () -> Unit,
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsIcon(Icons.Rounded.WbSunny)
                    Column {
                        Text(
                            text = stringResource(R.string.weather),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.weather_city_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = showWeather, onCheckedChange = onToggleShowWeather)
            }
            if (showWeather) {
                // SIWAK: update terakhir + sinkron manual cuaca
                SyncRow(
                    lastSyncMillis = weatherLastSyncMillis,
                    isSyncing = isSyncingWeather,
                    onSync = onSyncWeather,
                )
                // Provider selection (radio)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.provider_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WeatherProvider.values().forEach { p ->
                        SelectableSettingsRow(
                            selected = provider == p,
                            onClick = { onProviderChange(p) },
                            text = p.displayName,
                        )
                    }
                }

                // Show API key field for WeatherAPI.com
                if (provider == WeatherProvider.WEATHER_API) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.weather_api_key_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.weather_api_free_key),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImprovedTextField(
                        value = weatherApiKey,
                        onValueChange = onWeatherApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.weather_api_key_hint),
                        keyboardType = KeyboardType.Text,
                    )
                }

                // Show API key field for OpenWeatherMap
                if (provider == WeatherProvider.OPEN_WEATHER_MAP) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.openweathermap_api_key_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.openweather_free_key),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImprovedTextField(
                        value = openWeatherMapApiKey,
                        onValueChange = onOpenWeatherMapApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.openweathermap_api_key_hint),
                        keyboardType = KeyboardType.Text,
                    )
                }

                // SIWAK: cuaca memakai LOKASI yang sama dengan jadwal shalat (Lokasi & Jadwal),
                // jadi tak perlu input lokasi lagi di sini.
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.weather_uses_shared_location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun ManualPrayerTimesSettings(
    settings: AppSettings,
    onUpdateManualTime: (String, String) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                SettingsIcon(Icons.Rounded.Timer)
                Text(
                    text = stringResource(R.string.prayer_times_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            PrayerTimeInput(
                label = stringResource(R.string.imsak),
                time = settings.manualImsak,
                onTimeChange = { onUpdateManualTime("imsak", it) },
            )

            PrayerTimeInput(
                label = stringResource(R.string.prayer_azan_label, stringResource(R.string.fajr)),
                time = settings.manualFajrAzan,
                onTimeChange = { onUpdateManualTime("fajrAzan", it) },
            )

            PrayerTimeInput(
                label = stringResource(R.string.sunrise),
                time = settings.manualSunrise,
                onTimeChange = { onUpdateManualTime("sunrise", it) },
            )

            PrayerTimeInput(
                label = stringResource(R.string.prayer_azan_label, stringResource(R.string.dhuhr)),
                time = settings.manualDhuhrAzan,
                onTimeChange = { onUpdateManualTime("dhuhrAzan", it) },
            )

            PrayerTimeInput(
                label = stringResource(R.string.prayer_azan_label, stringResource(R.string.asr)),
                time = settings.manualAsrAzan,
                onTimeChange = { onUpdateManualTime("asrAzan", it) },
            )

            PrayerTimeInput(
                label = stringResource(R.string.prayer_azan_label, stringResource(R.string.maghrib)),
                time = settings.manualMaghribAzan,
                onTimeChange = { onUpdateManualTime("maghribAzan", it) },
            )

            PrayerTimeInput(
                label = stringResource(R.string.prayer_azan_label, stringResource(R.string.isha)),
                time = settings.manualIshaAzan,
                onTimeChange = { onUpdateManualTime("ishaAzan", it) },
            )
        }
    }
}

@Composable
private fun IqamahGapSettings(
    settings: AppSettings,
    onUpdateIqamahGap: (String, Int) -> Unit,
    showIqamahCycle: Boolean,
    onShowIqamahCycleChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                SettingsIcon(Icons.Rounded.HourglassTop)
                Text(
                    text = stringResource(R.string.iqamah_gaps_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            IqamahGapInput(
                label = stringResource(R.string.fajr),
                gap = settings.fajrIqamahGap,
                onGapChange = { onUpdateIqamahGap("fajr", it) },
            )

            IqamahGapInput(
                label = stringResource(R.string.dhuhr),
                gap = settings.dhuhrIqamahGap,
                onGapChange = { onUpdateIqamahGap("dhuhr", it) },
            )

            IqamahGapInput(
                label = stringResource(R.string.asr),
                gap = settings.asrIqamahGap,
                onGapChange = { onUpdateIqamahGap("asr", it) },
            )

            IqamahGapInput(
                label = stringResource(R.string.maghrib),
                gap = settings.maghribIqamahGap,
                onGapChange = { onUpdateIqamahGap("maghrib", it) },
            )

            IqamahGapInput(
                label = stringResource(R.string.isha),
                gap = settings.ishaIqamahGap,
                onGapChange = { onUpdateIqamahGap("isha", it) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.iqamah_cycle_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.iqamah_cycle_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Switch(checked = showIqamahCycle, onCheckedChange = onShowIqamahCycleChange)
            }
        }
    }
}

@Composable
private fun JummaNightBayanSettings(
    jummaNightBayanEnabled: Boolean,
    jummaNightBayanMinutes: Int,
    onJummaNightBayanEnabledChange: (Boolean) -> Unit,
    onJummaNightBayanMinutesChange: (Int) -> Unit,
) {
    SettingsCard {
        Column {
            // Header with toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsIcon(Icons.Rounded.NightsStay)
                    Column {
                        Text(
                            text = stringResource(R.string.jumma_bayan_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.jumma_bayan_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(
                    checked = jummaNightBayanEnabled,
                    onCheckedChange = onJummaNightBayanEnabledChange,
                )
            }

            // Description
            if (jummaNightBayanEnabled) {
                Text(
                    text = stringResource(R.string.jumma_bayan_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                // Duration input
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.bayan_duration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    NumberPickerField(
                        value = jummaNightBayanMinutes,
                        onValueChange = onJummaNightBayanMinutesChange,
                        modifier = Modifier.width(160.dp),
                        range = 15..90,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.unit_minute_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerDisplaySettings(
    jummahDurationMinutes: Int,
    duaDisplayDurationMinutes: Int,
    showJummahScreen: Boolean,
    onJummahDurationChange: (Int) -> Unit,
    onDuaDisplayDurationChange: (Int) -> Unit,
    onShowJummahScreenChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                SettingsIcon(Icons.Rounded.Alarm)
                Text(
                    text = stringResource(R.string.prayer_display_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // SIWAK: sakelar Layar Jumat DULU; menu Jumat lain muncul hanya bila aktif
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.show_jumuah_screen_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.show_jumuah_screen_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Switch(
                    checked = showJummahScreen,
                    onCheckedChange = onShowJummahScreenChange,
                )
            }

            if (showJummahScreen) {
                Text(
                    text = stringResource(R.string.jumuah_duration_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.jumuah_duration_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    NumberPickerField(
                        value = jummahDurationMinutes,
                        onValueChange = onJummahDurationChange,
                        modifier = Modifier.width(160.dp),
                        range = 30..120,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.unit_minute_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.dua_duration_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dua_duration_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                NumberPickerField(
                    value = duaDisplayDurationMinutes,
                    onValueChange = onDuaDisplayDurationChange,
                    modifier = Modifier.width(160.dp),
                    range = 1..15,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.unit_minute_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun PrayerTimeInput(
    label: String,
    time: String,
    onTimeChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        TimePickerField(
            value = time,
            onValueChange = onTimeChange,
            modifier = Modifier.width(200.dp),
        )
    }
}

@Composable
private fun IqamahGapInput(
    label: String,
    gap: Int,
    onGapChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        NumberPickerField(
            value = gap,
            onValueChange = onGapChange,
            modifier = Modifier.width(160.dp),
            range = 0..60,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.unit_minute_short),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

// SIWAK: koreksi menit per-waktu (offset ±) untuk mencocokkan jadwal Kemenag setempat
@Composable
private fun PrayerOffsetSettings(
    settings: AppSettings,
    onUpdateOffset: (String, Int) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Column {
            // Header + sakelar aktif/nonaktif
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    SettingsIcon(Icons.Rounded.Tune)
                    Text(
                        text = stringResource(R.string.prayer_offset_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Switch(checked = settings.prayerOffsetEnabled, onCheckedChange = onEnabledChange)
            }
            Text(
                text = stringResource(R.string.prayer_offset_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (settings.prayerOffsetEnabled) {
            PrayerOffsetInput(stringResource(R.string.imsak), settings.imsakOffset) { onUpdateOffset("imsak", it) }
            PrayerOffsetInput(stringResource(R.string.fajr), settings.fajrOffset) { onUpdateOffset("fajr", it) }
            PrayerOffsetInput(stringResource(R.string.sunrise), settings.sunriseOffset) { onUpdateOffset("sunrise", it) }
            PrayerOffsetInput(stringResource(R.string.dhuhr), settings.dhuhrOffset) { onUpdateOffset("dhuhr", it) }
            PrayerOffsetInput(stringResource(R.string.asr), settings.asrOffset) { onUpdateOffset("asr", it) }
            PrayerOffsetInput(stringResource(R.string.maghrib), settings.maghribOffset) { onUpdateOffset("maghrib", it) }
            PrayerOffsetInput(stringResource(R.string.isha), settings.ishaOffset) { onUpdateOffset("isha", it) }
            }
        }
    }
}

@Composable
private fun PrayerOffsetInput(
    label: String,
    offset: Int,
    onOffsetChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        NumberPickerField(
            value = offset,
            onValueChange = onOffsetChange,
            modifier = Modifier.width(160.dp),
            range = -15..15,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.unit_minute_short),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ClockTypeSetting(
    selectedClockType: ClockType,
    onClockTypeChange: (ClockType) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                SettingsIcon(Icons.Rounded.Schedule)
                Text(
                    text = stringResource(R.string.clock_type),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            ClockType.values().forEach { clockType ->
                SelectableSettingsRow(
                    selected = selectedClockType == clockType,
                    onClick = { onClockTypeChange(clockType) },
                    text =
                        when (clockType) {
                            ClockType.DIGITAL -> stringResource(R.string.digital_clock)
                            ClockType.ANALOG -> stringResource(R.string.analog_clock)
                            ClockType.BOTH -> stringResource(R.string.clock_both_auto)
                        },
                )
            }
        }
    }
}

@Composable
private fun ClockFormatSettings(
    showSeconds: Boolean,
    show24Hour: Boolean,
    onShowSecondsChange: (Boolean) -> Unit,
    onShow24HourChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                SettingsIcon(Icons.Rounded.Numbers)
                Text(
                    text = "Jam",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.show_seconds),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Switch(
                    checked = showSeconds,
                    onCheckedChange = onShowSecondsChange,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.hour_format_24),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Switch(
                    checked = show24Hour,
                    onCheckedChange = onShow24HourChange,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    isSelected: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
        ) { content() }
    }
}

@Composable
private fun HijriDateSettings(
    hijriProvider: com.mosque.prayerclock.data.model.HijriProvider,
    day: Int,
    month: Int,
    year: Int,
    hijriDayOffset: Int,
    onHijriProviderChange: (com.mosque.prayerclock.data.model.HijriProvider) -> Unit,
    onManualChange: (Int, Int, Int) -> Unit,
    onHijriDayOffsetChange: (Int) -> Unit,
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.Rounded.NightsStay)
                Text(
                    text = stringResource(R.string.hijri_date_source),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // SIWAK: hanya Otomatis (hisab, Al-Adhan) & Manual; opsi Sri Lanka disembunyikan
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    com.mosque.prayerclock.data.model.HijriProvider.AL_ADHAN_API,
                    com.mosque.prayerclock.data.model.HijriProvider.MANUAL,
                ).forEach { provider ->
                    SelectableSettingsRow(
                        selected = hijriProvider == provider,
                        onClick = { onHijriProviderChange(provider) },
                        text = provider.displayName,
                    )
                }
            }

            // SIWAK: penyesuaian ±hari untuk mode hisab (Otomatis)
            if (hijriProvider == com.mosque.prayerclock.data.model.HijriProvider.AL_ADHAN_API) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.hijri_offset_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.hijri_offset_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    NumberPickerField(
                        value = hijriDayOffset,
                        onValueChange = onHijriDayOffsetChange,
                        modifier = Modifier.width(160.dp),
                        range = -3..3,
                    )
                }
            }

            if (hijriProvider == com.mosque.prayerclock.data.model.HijriProvider.MANUAL) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberPickerField(
                        value = day,
                        onValueChange = { onManualChange(it, month, year) },
                        range = 1..30,
                    )
                    NumberPickerField(
                        value = month,
                        onValueChange = { onManualChange(day, it, year) },
                        range = 1..12,
                    )
                    NumberPickerField(
                        value = year,
                        onValueChange = { onManualChange(day, month, it) },
                        range = 1300..1600,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImprovedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var showDialog by remember { mutableStateOf(false) }

    // Clickable card that displays current value
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .tvFocusHighlight().clickable { showDialog = true },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = value.ifEmpty { placeholder },
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (value.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }

            // Edit icon indicator
            Text(
                text = "✏️",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }

    // Show dialog when clicked
    if (showDialog) {
        TextInputDialog(
            title = label ?: "Edit",
            initialValue = value,
            placeholder = placeholder,
            keyboardType = keyboardType,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onValueChange(newValue)
                showDialog = false
            },
        )
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var textValue by remember { mutableStateOf(initialValue) }
    val focusManager = LocalFocusManager.current

    // Handle back button to dismiss dialog
    BackHandler { onDismiss() }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val availableWidth = maxWidth
            val availableHeight = maxHeight

            // Calculate dynamic sizes based on screen dimensions
            val dialogWidth = (availableWidth * 0.85f).coerceAtMost(600.dp)

            val dialogPadding = (availableWidth * 0.04f).coerceIn(16.dp, 32.dp)

            val titleFontSize =
                with(density) {
                    val sp = (availableWidth.toPx() * 0.025f).toSp()
                    sp.value.coerceIn(18f, 28f).sp
                }

            val textFieldFontSize =
                with(density) {
                    val sp = (availableWidth.toPx() * 0.022f).toSp()
                    sp.value.coerceIn(16f, 24f).sp
                }

            val buttonFontSize =
                with(density) {
                    val sp = (availableWidth.toPx() * 0.02f).toSp()
                    sp.value.coerceIn(14f, 22f).sp
                }

            val verticalSpacing = (availableHeight * 0.02f).coerceIn(12.dp, 24.dp)

            Card(
                modifier =
                    Modifier
                        .width(dialogWidth)
                        .padding(dialogPadding),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(dialogPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontSize = titleFontSize,
                            ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(verticalSpacing))

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(placeholder)
                        },
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = keyboardType,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onConfirm(textValue)
                                },
                            ),
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textFieldFontSize,
                            ),
                    )

                    Spacer(modifier = Modifier.height(verticalSpacing * 1.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val buttonHeight = (availableHeight * 0.06f).coerceIn(48.dp, 64.dp)

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = buttonHeight),
                        ) {
                            Text(
                                text = "Batal",
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontSize = buttonFontSize,
                                    ),
                            )
                        }

                        Button(
                            onClick = { onConfirm(textValue) },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = buttonHeight),
                        ) {
                            Text(
                                text = "Save",
                                style =
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontSize = buttonFontSize,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var isInitialized by remember { mutableStateOf(false) }

    // Parse initial value
    LaunchedEffect(value) {
        if (value.isNotEmpty()) {
            val parts = value.split(":")
            if (parts.size >= 2) {
                hours = parts[0].padStart(2, '0')
                minutes = parts[1].padStart(2, '0')
            } else {
                // Handle cases where value might be malformed
                hours = "00"
                minutes = "00"
            }
        } else {
            hours = "00"
            minutes = "00"
        }
        isInitialized = true
    }

    // Update value when hours or minutes change (only after initialization)
    LaunchedEffect(hours, minutes, isInitialized) {
        if (isInitialized) {
            val h = hours.toIntOrNull()?.coerceIn(0, 23) ?: 0
            val m = minutes.toIntOrNull()?.coerceIn(0, 59) ?: 0
            onValueChange(String.format("%02d:%02d", h, m))
        }
    }

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hours picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LongPressIconButton(
                    onClick = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = (currentHour + 1) % 24
                        hours = String.format("%02d", newHour)
                    },
                    onLongPress = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = (currentHour + 1) % 24
                        hours = String.format("%02d", newHour)
                    },
                ) {
                    Text(
                        text = "+",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = hours.padStart(2, '0'),
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                LongPressIconButton(
                    onClick = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = if (currentHour <= 0) 23 else currentHour - 1
                        hours = String.format("%02d", newHour)
                    },
                    onLongPress = {
                        val currentHour = hours.toIntOrNull() ?: 0
                        val newHour = if (currentHour <= 0) 23 else currentHour - 1
                        hours = String.format("%02d", newHour)
                    },
                ) {
                    Text(
                        text = "−",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Text(
                    text = "HH",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }

            Text(
                text = ":",
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )

            // Minutes picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LongPressIconButton(
                    onClick = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = (currentMin + 1) % 60
                        minutes = String.format("%02d", newMin)
                    },
                    onLongPress = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = (currentMin + 1) % 60
                        minutes = String.format("%02d", newMin)
                    },
                ) {
                    Text(
                        text = "+",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = minutes.padStart(2, '0'),
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                LongPressIconButton(
                    onClick = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = if (currentMin <= 0) 59 else currentMin - 1
                        minutes = String.format("%02d", newMin)
                    },
                    onLongPress = {
                        val currentMin = minutes.toIntOrNull() ?: 0
                        val newMin = if (currentMin <= 0) 59 else currentMin - 1
                        minutes = String.format("%02d", newMin)
                    },
                ) {
                    Text(
                        text = "−",
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                Text(
                    text = "MM",
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun NumberPickerField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decrease button
        LongPressIconButton(
            onClick = {
                val newValue = (value - 1).coerceIn(range)
                onValueChange(newValue)
            },
            onLongPress = {
                val newValue = (value - 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value > range.first,
        ) {
            Text(
                text = "−",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color =
                    if (value > range.first) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }

        // Number display (card-based for TV/remote friendliness)
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(
                modifier = Modifier.width(64.dp).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Increase button
        LongPressIconButton(
            onClick = {
                val newValue = (value + 1).coerceIn(range)
                onValueChange(newValue)
            },
            onLongPress = {
                val newValue = (value + 1).coerceIn(range)
                onValueChange(newValue)
            },
            enabled = value < range.last,
        ) {
            Text(
                text = "+",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color =
                    if (value < range.last) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }
    }
}

@Composable
private fun SelectableSettingsRow(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .tvFocusHighlight().selectable(
                    selected = selected,
                    onClick = onClick,
                ).background(
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.2f,
                            ) // Bright highlight for selected
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(8.dp),
                ).border(
                    width = if (selected) 2.dp else 0.dp,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme
                                .primary // Bright border for selected
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(8.dp),
                ).padding(
                    horizontal = 12.dp,
                    vertical = 12.dp,
                ), // More padding for better touch target
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor =
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                ),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary // Highlight selected text
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun FullScreenCountdownSettings(
    fullScreenCountdownEnabled: Boolean,
    fullScreenCountdownMinutes: Int,
    onFullScreenCountdownEnabledChange: (Boolean) -> Unit,
    onFullScreenCountdownMinutesChange: (Int) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsIcon(Icons.Rounded.Timer)
                    Column {
                        Text(
                            text = stringResource(R.string.fullscreen_countdown_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.fullscreen_countdown_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = fullScreenCountdownEnabled, onCheckedChange = onFullScreenCountdownEnabledChange)
            }

            // SIWAK: durasi (menit) sebelum Adzan, dapat diatur
            if (fullScreenCountdownEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.countdown_minutes_before),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    NumberPickerField(
                        value = fullScreenCountdownMinutes,
                        onValueChange = onFullScreenCountdownMinutesChange,
                        modifier = Modifier.width(160.dp),
                        range = 1..30,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.unit_minute_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// SIWAK: sakelar tampil/sembunyi Terbit & Imsak di layar utama
@Composable
private fun DisplayVisibilitySettings(
    showSunrise: Boolean,
    showImsak: Boolean,
    onShowSunriseChange: (Boolean) -> Unit,
    onShowImsakChange: (Boolean) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                SettingsIcon(Icons.Rounded.Visibility)
                Text(
                    text = stringResource(R.string.display_visibility_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.show_imsak_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = showImsak, onCheckedChange = onShowImsakChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.show_sunrise_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = showSunrise, onCheckedChange = onShowSunriseChange)
            }
                    }
    }
}

// SIWAK: running text pengumuman (banyak pesan dipisah "||")
@Composable
private fun RunningTextSettings(
    text: String,
    onTextChange: (String) -> Unit,
) {
    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Text(text = "📢", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(end = 12.dp))
                Text(
                    text = stringResource(R.string.running_text_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.running_text_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ImprovedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.running_text_hint),
                keyboardType = KeyboardType.Text,
            )
        }
    }
}

// SIWAK: media — upload logo & background
@Composable
private fun MediaSettings(
    logoUri: String,
    backgroundUri: String,
    backgroundPrayerUri: String,
    backgroundIqamahUri: String,
    onLogoChange: (String) -> Unit,
    onBackgroundChange: (String) -> Unit,
    onBackgroundPrayerChange: (String) -> Unit,
    onBackgroundIqamahChange: (String) -> Unit,
) {
    val context = LocalContext.current
    fun persist(uri: android.net.Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
    }
    val logoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) { persist(uri); onLogoChange(uri.toString()) }
        }
    val bgLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) { persist(uri); onBackgroundChange(uri.toString()) }
        }
    val bgPrayerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) { persist(uri); onBackgroundPrayerChange(uri.toString()) }
        }
    val bgIqamahLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) { persist(uri); onBackgroundIqamahChange(uri.toString()) }
        }

    SettingsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(text = "🖼️", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(end = 12.dp))
                Text(
                    text = stringResource(R.string.media_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            ImagePickerRow(
                label = stringResource(R.string.logo_label),
                uri = logoUri,
                onPick = { logoLauncher.launch(arrayOf("image/*")) },
                onClear = { onLogoChange("") },
            )
            ImagePickerRow(
                label = stringResource(R.string.background_label),
                uri = backgroundUri,
                onPick = { bgLauncher.launch(arrayOf("image/*")) },
                onClear = { onBackgroundChange("") },
            )
            ImagePickerRow(
                label = stringResource(R.string.background_prayer_label),
                uri = backgroundPrayerUri,
                onPick = { bgPrayerLauncher.launch(arrayOf("image/*")) },
                onClear = { onBackgroundPrayerChange("") },
            )
            ImagePickerRow(
                label = stringResource(R.string.background_iqamah_label),
                uri = backgroundIqamahUri,
                onPick = { bgIqamahLauncher.launch(arrayOf("image/*")) },
                onClear = { onBackgroundIqamahChange("") },
            )
        }
    }
}

@Composable
private fun ImagePickerRow(
    label: String,
    uri: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (uri.isEmpty()) stringResource(R.string.no_image) else stringResource(R.string.image_set),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        if (uri.isNotEmpty()) {
            OutlinedButton(onClick = onClear) { Text(stringResource(R.string.clear_image)) }
            Spacer(modifier = Modifier.width(8.dp))
        }
        OutlinedButton(onClick = onPick) { Text(stringResource(R.string.choose_image)) }
    }
}

// SIWAK: Mode Istirahat Malam
@Composable
private fun NightModeSettings(
    enabled: Boolean,
    startHour: Int,
    endHour: Int,
    dimLevel: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit,
    onDimLevelChange: (Int) -> Unit,
    showDimLevel: Boolean = enabled,
) {
    SettingsCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = "🌙", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.night_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.night_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                NightHourRow(stringResource(R.string.night_start), startHour, onStartHourChange)
                NightHourRow(stringResource(R.string.night_end), endHour, onEndHourChange)
            }
            if (showDimLevel) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.night_dim_level),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    NumberPickerField(
                        value = dimLevel,
                        onValueChange = onDimLevelChange,
                        modifier = Modifier.width(160.dp),
                        range = 50..100,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// SIWAK 2.1: redup sepanjang hari pada hari tertentu (akhir pekan, tanggal merah).
@Composable
private fun DimDaysSettings(
    enabled: Boolean,
    weekdays: Set<Int>,
    nationalHolidays: Boolean,
    cutiBersama: Boolean,
    brightenForPrayer: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onWeekdaysChange: (Set<Int>) -> Unit,
    onNationalHolidaysChange: (Boolean) -> Unit,
    onCutiBersamaChange: (Boolean) -> Unit,
    onBrightenForPrayerChange: (Boolean) -> Unit,
) {
    val dayNames =
        listOf(
            1 to R.string.day_short_mon,
            2 to R.string.day_short_tue,
            3 to R.string.day_short_wed,
            4 to R.string.day_short_thu,
            5 to R.string.day_short_fri,
            6 to R.string.day_short_sat,
            7 to R.string.day_short_sun,
        )
    SettingsCard {
        Column {
            DimSwitchRow(
                title = stringResource(R.string.dim_days_title),
                desc = stringResource(R.string.dim_days_desc),
                checked = enabled,
                onChange = onEnabledChange,
                emoji = "📅",
            )
            if (enabled) {
                Text(
                    text = stringResource(R.string.dim_days_weekly),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayNames.forEach { (day, label) ->
                        val selected = day in weekdays
                        OutlinedButton(
                            onClick = { onWeekdaysChange(if (selected) weekdays - day else weekdays + day) },
                            colors =
                                androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                ),
                        ) { Text(stringResource(label)) }
                    }
                }
                DimSwitchRow(stringResource(R.string.dim_days_holidays), null, nationalHolidays, onNationalHolidaysChange)
                DimSwitchRow(stringResource(R.string.dim_days_cuti), null, cutiBersama, onCutiBersamaChange)
                DimSwitchRow(stringResource(R.string.dim_days_brighten), stringResource(R.string.dim_days_brighten_desc), brightenForPrayer, onBrightenForPrayerChange)
            }
        }
    }
}

@Composable
private fun DimSwitchRow(
    title: String,
    desc: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    emoji: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (emoji != null) Text(text = emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(end = 12.dp))
            Column {
                Text(
                    text = title,
                    style = if (emoji != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (desc != null) {
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NightHourRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        NumberPickerField(value = value, onValueChange = onChange, modifier = Modifier.width(160.dp), range = 0..23)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = ":00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
private fun ColorThemeSettings(
    selectedThemeId: String,
    onThemeChange: (String) -> Unit,
) {
    val allThemes = AppColorThemes.getAllThemes()
    var isExpanded by remember { mutableStateOf(false) }

    SettingsCard {
        Column {
            // Header with icon and title
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .tvFocusHighlight().clickable { isExpanded = !isExpanded }
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsIcon(Icons.Rounded.Palette)
                    Column {
                        Text(
                            text = stringResource(R.string.color_theme_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.color_theme_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        // Show currently selected theme name
                        val currentTheme = allThemes.find { it.id == selectedThemeId }
                        currentTheme?.let {
                            Text(
                                text = "Current: ${it.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Expandable theme grid
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Display themes in a vertical list for TV UI
                    allThemes.forEach { theme ->
                        ThemeOption(
                            theme = theme,
                            isSelected = theme.id == selectedThemeId,
                            onSelect = { onThemeChange(theme.id) },
                        )
                        if (theme != allThemes.last()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: com.mosque.prayerclock.ui.theme.ColorTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .tvFocusHighlight().clickable { onSelect() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border =
            if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Color preview squares
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 12.dp),
            ) {
                // Show 3 key colors as preview
                ColorPreviewBox(color = theme.primaryAccent)
                ColorPreviewBox(color = theme.surfacePrimary)
                ColorPreviewBox(color = theme.azanTime)
            }
        }
    }
}

@Composable
private fun ColorPreviewBox(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(color, shape = RoundedCornerShape(4.dp))
                .border(
                    width = 1.dp,
                    color =
                        androidx.compose.ui.graphics.Color.White
                            .copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp),
                ),
    )
}

@Composable
internal fun SystemSettingsAccess() {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }

    // Show app picker dialog
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                LauncherHelper.launchApp(context, app.resolveInfo)
            },
        )
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.Rounded.Settings)
                Column {
                    Text(
                        text = stringResource(R.string.system_settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.system_settings_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date & Time Settings Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openDateTimeSettings(context)) {
                        Toast.makeText(context, "Setelan tanggal & waktu tidak bisa dibuka di TV ini", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(Icons.Rounded.Alarm)
                    Column {
                        Text(
                            text = stringResource(R.string.date_time_settings),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_date_time_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // General Android Settings Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openAndroidSettings(context)) {
                        Toast.makeText(context, "Setelan Android tidak bisa dibuka di TV ini", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(Icons.Rounded.Settings)
                    Column {
                        Text(
                            text = stringResource(R.string.android_settings),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_android_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // App Drawer Button
            OutlinedButton(
                onClick = {
                    showAppPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(Icons.Rounded.Apps)
                    Column {
                        Text(
                            text = stringResource(R.string.open_app_drawer),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_app_drawer_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Switch Launcher Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openLauncherChooser(context)) {
                        Toast.makeText(context, "Tidak bisa mengganti layar utama di TV ini", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(Icons.Rounded.Home)
                    Column {
                        Text(
                            text = stringResource(R.string.choose_launcher),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.choose_launcher_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // File Manager Button
            OutlinedButton(
                onClick = {
                    if (!LauncherHelper.openFileManager(context)) {
                        // Toast already shown in LauncherHelper
                        // No need for duplicate message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsIcon(Icons.Rounded.Folder)
                    Column {
                        Text(
                            text = stringResource(R.string.open_file_manager),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.open_file_manager_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataManagementSettings(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheStats by remember { mutableStateOf<CacheStats?>(null) }
    var isClearing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    // SIWAK: status cache cuaca dari DataStore
    val dmSettings by viewModel.settings.collectAsStateWithLifecycle()

    // Load cache stats when component is first composed
    LaunchedEffect(Unit) {
        cacheStats = viewModel.getCacheStats()
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.Rounded.Storage)
                Column {
                    Text(
                        text = "Data tersimpan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Jadwal shalat, tanggal Hijriah, dan cuaca yang disimpan di TV agar tetap jalan tanpa internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cache Statistics
            if (cacheStats != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Jadwal shalat tersimpan:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${cacheStats!!.prayerTimesCount} entries",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Tanggal Hijriah tersimpan:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${cacheStats!!.hijriDatesCount} entries",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // SIWAK: status cache cuaca (tersimpan agar tampil saat offline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Cuaca Tersimpan:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (dmSettings.cachedWeatherJson.isNotBlank()) "1 entry" else "0 entries",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Clear Cache Button
            OutlinedButton(
                onClick = {
                    showConfirmDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClearing && (cacheStats?.let { it.prayerTimesCount + it.hijriDatesCount } ?: 0) > 0,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Menghapus…")
                    } else {
                        Text("Hapus semua data tersimpan")
                    }
                }
            }

            Text(
                text = "Jadwal shalat dan tanggal Hijriah tersimpan akan dihapus, lalu diambil ulang dari sumber jadwal saat dibutuhkan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        Dialog(onDismissRequest = { showConfirmDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingsIcon(Icons.Rounded.WarningAmber)
                        Text(
                            text = "Hapus semua data tersimpan?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        text = "Jadwal shalat dan tanggal Hijriah tersimpan akan dihapus. Aplikasi akan mengambil data baru dari sumber jadwal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )

                    if (cacheStats != null) {
                        Text(
                            text = "• ${cacheStats!!.prayerTimesCount} prayer time entries\n• ${cacheStats!!.hijriDatesCount} hijri date entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                showConfirmDialog = false
                                isClearing = true
                                scope.launch {
                                    try {
                                        viewModel.clearAllCachedData()
                                        // Refresh stats
                                        cacheStats = viewModel.getCacheStats()
                                        Toast.makeText(
                                            context,
                                            "Data tersimpan sudah dihapus",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Gagal menghapus data: ${e.message}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } finally {
                                        isClearing = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text("Hapus")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AboutSettings(
    autoUpdateCheckEnabled: Boolean = true,
    onAutoUpdateCheckEnabledChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var updateCheckError by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var isFileAlreadyDownloaded by remember { mutableStateOf(false) }
    var hasInstallPermission by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val apkDownloader = remember { ApkDownloader() }
    val downloadProgress by apkDownloader.downloadProgress.collectAsState()

    // Get current version from BuildConfig
    val currentVersion = BuildConfig.VERSION_NAME

    // Check install permission on composition
    LaunchedEffect(Unit) {
        hasInstallPermission = apkDownloader.canInstallPackages(context)
    }

    // Check if APK is already downloaded when update info changes
    LaunchedEffect(updateInfo) {
        if (updateInfo?.hasUpdate == true) {
            isFileAlreadyDownloaded =
                apkDownloader.isApkAlreadyDownloaded(
                    context = context,
                    version = updateInfo!!.latestVersion,
                )
            // Also re-check permission
            hasInstallPermission = apkDownloader.canInstallPackages(context)
        }
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIcon(Icons.Rounded.Info)
                Column {
                    Text(
                        text = stringResource(R.string.about_app),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Version Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = currentVersion,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Developer Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.developer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.developer_name),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // SIWAK: atribusi ke proyek asli (syarat lisensi MIT)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.based_on_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            // SIWAK: atribusi data cuaca (wajib menurut ketentuan OpenWeather) + tautan dokumen legal
            Text(
                text = stringResource(R.string.weather_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = stringResource(R.string.legal_links),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            // Self-update features only available for GitHub/sideload distribution
            // Play Store builds use Google Play for updates (per Google policy)
            if (BuildConfig.ENABLE_SELF_UPDATE) {
                Spacer(modifier = Modifier.height(12.dp))

                // Auto Update Check Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_update_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.auto_update_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                    Switch(
                        checked = autoUpdateCheckEnabled,
                        onCheckedChange = onAutoUpdateCheckEnabledChange,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Check for Updates Button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            updateCheckError = false
                            try {
                                val updateChecker = UpdateChecker()
                                val result = updateChecker.checkForUpdates(currentVersion)
                                updateInfo = result
                                if (result == null) {
                                    updateCheckError = true
                                }
                            } catch (e: Exception) {
                                updateCheckError = true
                            } finally {
                                isChecking = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(stringResource(R.string.checking_updates))
                        } else {
                            Text(stringResource(R.string.check_for_updates))
                        }
                    }
                }
            }

            // Update Status (only for GitHub/sideload distribution)
            if (BuildConfig.ENABLE_SELF_UPDATE) {
                if (updateInfo != null && !isChecking) {
                    if (updateInfo!!.hasUpdate) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.update_available, updateInfo!!.latestVersion),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            text = "Current: v$currentVersion",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Install permission warning (Android 8.0+)
                                if (!hasInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor =
                                                    MaterialTheme.colorScheme.errorContainer.copy(
                                                        alpha = 0.3f,
                                                    ),
                                            ),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                        ) {
                                            Text(
                                                text = "Install Permission Required",
                                                style =
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text =
                                                    "Untuk memasang pembaruan, izinkan SIWAK memasang aplikasi.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    apkDownloader.requestInstallPermission(context)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                    ),
                                            ) {
                                                Text("Grant Install Permission")
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Download and Install Button
                                Button(
                                    onClick = {
                                        // Check permission first
                                        if (!hasInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            apkDownloader.requestInstallPermission(context)
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Izinkan pemasangan dari sumber ini agar SIWAK bisa diperbarui",
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            return@Button
                                        }

                                        if (isFileAlreadyDownloaded) {
                                            // If file is already downloaded, just install it
                                            val fileName = apkDownloader.getApkFileName(updateInfo!!.latestVersion)
                                            val file = File(context.getExternalFilesDir(null), fileName)

                                            if (!file.exists()) {
                                                Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
                                                isFileAlreadyDownloaded = false
                                                return@Button
                                            }

                                            val uri: Uri =
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file,
                                                    )
                                                } else {
                                                    Uri.fromFile(file)
                                                }

                                            // Use ACTION_INSTALL_PACKAGE for better compatibility (Android 8.0+)
                                            val installIntent =
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                                    }
                                                } else {
                                                    Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                }

                                            if (installIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(installIntent)
                                                Toast.makeText(context, "Membuka pemasang aplikasi…", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Pemasang aplikasi tidak ditemukan di TV ini", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            // Download the file
                                            isDownloading = true
                                            apkDownloader.downloadApk(
                                                context = context,
                                                downloadUrl = updateInfo!!.downloadUrl,
                                                version = updateInfo!!.latestVersion,
                                                onComplete = {
                                                    isDownloading = false
                                                    isFileAlreadyDownloaded = true
                                                },
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isDownloading,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp).padding(end = 8.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                            if (downloadProgress.progress > 0) {
                                                Text("Downloading ${downloadProgress.progress}%")
                                            } else {
                                                Text("Downloading...")
                                            }
                                        } else {
                                            Text(
                                                if (isFileAlreadyDownloaded) {
                                                    stringResource(R.string.install_update)
                                                } else {
                                                    stringResource(R.string.download_update)
                                                },
                                            )
                                        }
                                    }
                                }

                                // Download Progress Bar
                                if (isDownloading && downloadProgress.progress > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = downloadProgress.progress / 100f,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        text = "${apkDownloader.formatBytes(
                                            downloadProgress.bytesDownloaded,
                                        )} / ${apkDownloader.formatBytes(downloadProgress.totalBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    )
                                }

                                // Release Notes (if available)
                                if (updateInfo!!.releaseNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "What's New:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    // Show full release notes with markdown formatting
                                    MarkdownText(
                                        markdown = updateInfo!!.releaseNotes,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.no_updates_available),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                if (updateCheckError && !isChecking) {
                    Text(
                        text = stringResource(R.string.update_check_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GitHub Repository Button — SIWAK
            run {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alfian-yds/siwak"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingsIcon(Icons.Rounded.Code)
                        Text(stringResource(R.string.github_repository))
                    }
                }
            }
        }
    }
}

@Composable
private fun AzanSoundSettings(
    azanSoundEnabled: Boolean,
    azanSoundType: SoundType,
    azanSoundUri: String,
    onAzanSoundEnabledChange: (Boolean) -> Unit,
    onAzanSoundTypeChange: (SoundType) -> Unit,
    onAzanSoundUriChange: (String) -> Unit,
    onTest: () -> Unit = {},
) {
    val context = LocalContext.current
    
    // Permission launcher for audio access
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Izin diberikan. Silakan pilih file audio lagi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Izin ditolak, file audio tidak bisa dibuka.", Toast.LENGTH_LONG).show()
            }
        }
    
    // Audio file picker launcher using OpenDocument for persistent access
    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                // Take persistable URI permission for background service access
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    onAzanSoundUriChange(it.toString())
                    Log.d("AzanSoundSettings", "Persistable permission granted for URI: $it")
                    Toast.makeText(context, "Audio adzan dipilih", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("AzanSoundSettings", "Failed to take persistable permission", e)
                    Toast.makeText(context, "File ini tidak bisa dibuka. Coba pilih file lain.", Toast.LENGTH_LONG).show()
                }
            }
        }
    
    // Function to check and request audio permission
    val checkAndRequestAudioPermission: () -> Unit = {
        if (PermissionsHelper.hasAudioPermission(context)) {
            // Permission already granted, open picker
            audioPickerLauncher.launch(arrayOf("audio/*"))
        } else {
            // Request permission
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsIcon(Icons.Rounded.Mosque)
                    Column {
                        Text(
                            text = stringResource(R.string.azan_sound_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.azan_sound_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = azanSoundEnabled, onCheckedChange = onAzanSoundEnabledChange)
            }

            OutlinedButton(onClick = onTest) { Text("Tes suara adzan") }

            // Sound type selection (only shown when enabled)
            if (azanSoundEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sound_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // Sound type options
                    SoundType.values().forEach { type ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .tvFocusHighlight().clickable { onAzanSoundTypeChange(type) }
                                    .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = azanSoundType == type,
                                onClick = { onAzanSoundTypeChange(type) },
                                colors =
                                    RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                            )
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    // Custom audio file selection
                    if (azanSoundType == SoundType.CUSTOM) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp),
                        ) {
                            OutlinedButton(
                                onClick = checkAndRequestAudioPermission,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.select_custom_audio))
                            }

                            if (azanSoundUri.isNotEmpty()) {
                                val fileName =
                                    try {
                                        getFileNameFromUri(context, Uri.parse(azanSoundUri))
                                    } catch (e: Exception) {
                                        "Unknown"
                                    }
                                
                                // Check if URI is still accessible
                                val isAccessible = try {
                                    context.contentResolver.openFileDescriptor(Uri.parse(azanSoundUri), "r")?.close()
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                                
                                if (isAccessible) {
                                    Text(
                                        text = stringResource(R.string.audio_file_selected, fileName),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                } else {
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text(
                                            text = "File not accessible: $fileName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        Text(
                                            text = "Pilih ulang file audionya",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_audio_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to get the actual filename from a content URI
 */
private fun getFileNameFromUri(
    context: Context,
    uri: Uri,
): String {
    var result = "Unknown"
    
    // Try to query the content resolver for the display name
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = it.getString(displayNameIndex) ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error getting filename from URI", e)
        }
    }
    
    // Fallback to last path segment
    if (result == "Unknown") {
        result = uri.lastPathSegment ?: "Unknown"
    }
    
    return result
}

@Composable
private fun IqamahSoundSettings(
    iqamahSoundEnabled: Boolean,
    iqamahSoundType: SoundType,
    iqamahSoundUri: String,
    onIqamahSoundEnabledChange: (Boolean) -> Unit,
    onIqamahSoundTypeChange: (SoundType) -> Unit,
    onIqamahSoundUriChange: (String) -> Unit,
    onTest: () -> Unit = {},
) {
    val context = LocalContext.current
    
    // Permission launcher for audio access
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Izin diberikan. Silakan pilih file audio lagi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Izin ditolak, file audio tidak bisa dibuka.", Toast.LENGTH_LONG).show()
            }
        }
    
    // Audio file picker launcher using OpenDocument for persistent access
    val audioPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                // Take persistable URI permission for background service access
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    onIqamahSoundUriChange(it.toString())
                    Log.d("IqamahSoundSettings", "Persistable permission granted for URI: $it")
                    Toast.makeText(context, "Audio iqamah dipilih", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("IqamahSoundSettings", "Failed to take persistable permission", e)
                    Toast.makeText(context, "File ini tidak bisa dibuka. Coba pilih file lain.", Toast.LENGTH_LONG).show()
                }
            }
        }
    
    // Function to check and request audio permission
    val checkAndRequestAudioPermission: () -> Unit = {
        if (PermissionsHelper.hasAudioPermission(context)) {
            // Permission already granted, open picker
            audioPickerLauncher.launch(arrayOf("audio/*"))
        } else {
            // Request permission
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsIcon(Icons.Rounded.NotificationsActive)
                    Column {
                        Text(
                            text = stringResource(R.string.iqamah_sound_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.iqamah_sound_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
                Switch(checked = iqamahSoundEnabled, onCheckedChange = onIqamahSoundEnabledChange)
            }

            OutlinedButton(onClick = onTest) { Text("Tes suara iqamah") }

            // Sound type selection (only shown when enabled)
            if (iqamahSoundEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sound_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // Sound type options
                    SoundType.values().forEach { type ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .tvFocusHighlight().clickable { onIqamahSoundTypeChange(type) }
                                    .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = iqamahSoundType == type,
                                onClick = { onIqamahSoundTypeChange(type) },
                                colors =
                                    RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                            )
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    // Custom audio file selection
                    if (iqamahSoundType == SoundType.CUSTOM) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp),
                        ) {
                            OutlinedButton(
                                onClick = checkAndRequestAudioPermission,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.select_custom_audio))
                            }

                            if (iqamahSoundUri.isNotEmpty()) {
                                val fileName =
                                    try {
                                        getFileNameFromUri(context, Uri.parse(iqamahSoundUri))
                                    } catch (e: Exception) {
                                        "Unknown"
                                    }
                                
                                // Check if URI is still accessible
                                val isAccessible = try {
                                    context.contentResolver.openFileDescriptor(Uri.parse(iqamahSoundUri), "r")?.close()
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                                
                                if (isAccessible) {
                                    Text(
                                        text = stringResource(R.string.audio_file_selected, fileName),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                } else {
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text(
                                            text = "File not accessible: $fileName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        Text(
                                            text = "Pilih ulang file audionya",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_audio_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



/** SIWAK V2: pintu masuk ke layar Hubungkan (App Flow §2.2). */
@Composable
private fun ConnectServerCard(onConnectServer: () -> Unit) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIcon(Icons.Rounded.Link)
                Column {
                    Text(
                        text = "Hubungkan ke website SIWAK",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Atur jadwal, suara, tampilan, pengumuman, slide, dan hadits dari HP lewat siwak.alfianys.id. Pengurus juga bisa memantau dan membuka SIWAK dari jauh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            Button(onClick = onConnectServer, modifier = Modifier.fillMaxWidth()) {
                Text("Hubungkan ke website SIWAK")
            }
        }
    }
}
