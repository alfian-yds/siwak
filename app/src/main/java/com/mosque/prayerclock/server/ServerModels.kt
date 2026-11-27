package com.mosque.prayerclock.server

/**
 * Bentuk data rute /api/v1 server SIWAK (TRD §6–§8, Backend Schema §4–§5).
 * Semua field dibuat nullable karena Gson tidak menghormati non-null Kotlin; pemeriksaan
 * dilakukan di BundleMapper. Field yang tidak dikenal diabaikan (TRD §7.3).
 */

data class PairingStartRequest(val nonce: String, val appVersion: String, val model: String)

data class PairingStartResponse(val pairingId: String?, val code: String?, val expiresAt: String?)

data class PairingStatusResponse(
    val status: String?, // pending | expired | claimed
    val code: String? = null,
    val expiresAt: String? = null,
    val deviceId: String? = null,
    val deviceToken: String? = null,
)

data class AppStateReport(val foreground: Boolean, val since: String?, val canOpenApp: Boolean)

data class HealthReport(val lastError: String?, val freeStorageMb: Long?)

data class CommandResult(val id: String, val ok: Boolean, val message: String?)

data class HeartbeatRequest(
    val appVersion: String,
    val configVersion: Long?,
    val contentVersion: Long?,
    val weatherStamp: String?,
    val deviceTime: String,
    val health: HealthReport,
    val app: AppStateReport,
    val commandResults: List<CommandResult>,
)

data class RemoteCommand(val id: String?, val type: String?)

data class HeartbeatResponse(
    val configVersion: Long?,
    val contentVersion: Long?,
    val weatherStamp: String?,
    val serverTime: String?,
    val minAppVersion: String?,
    val commands: List<RemoteCommand>?,
)

data class MosqueDto(
    val id: String?,
    val name: String?,
    val regionLabel: String?,
    val latitude: Double?,
    val longitude: Double?,
    val timezone: String?,
)

data class DeviceDto(val id: String?, val name: String?, val pinSalt: String?, val pinHash: String?)

data class RunningTextDto(val id: String?, val text: String?, val startsOn: String?, val endsOn: String?)

// ---- Pengaturan masjid (Backend Schema §4) ------------------------------------------

data class PrayerTimesDto(
    val imsak: String?,
    val subuh: String?,
    val terbit: String?,
    val dzuhur: String?,
    val ashar: String?,
    val maghrib: String?,
    val isya: String?,
)

data class OffsetsDto(
    val imsak: Int?,
    val subuh: Int?,
    val terbit: Int?,
    val dzuhur: Int?,
    val ashar: Int?,
    val maghrib: Int?,
    val isya: Int?,
)

data class HijriManualDto(val day: Int?, val month: Int?, val year: Int?, val setOn: String?)

data class HijriDto(val source: String?, val dayOffset: Int?, val manual: HijriManualDto?)

data class ScheduleDto(
    val source: String?,
    val manualTimes: PrayerTimesDto?,
    val offsetsEnabled: Boolean?,
    val offsets: OffsetsDto?,
    val showImsak: Boolean?,
    val showTerbit: Boolean?,
    val hijri: HijriDto?,
)

data class IqamahGapsDto(val subuh: Int?, val dzuhur: Int?, val ashar: Int?, val maghrib: Int?, val isya: Int?)

data class EnabledMinutesDto(val enabled: Boolean?, val minutes: Int?)

data class JumatDto(val screenEnabled: Boolean?, val durationMinutes: Int?)

data class PrayerCycleDto(
    val iqamahGapMinutes: IqamahGapsDto?,
    val showIqamahCycle: Boolean?,
    val fullScreenCountdown: EnabledMinutesDto?,
    val prayerScreenMinutes: Int?,
    val jumat: JumatDto?,
    val jumatNightBayan: EnabledMinutesDto?,
)

data class SoundItemDto(val enabled: Boolean?, val type: String?, val mediaId: String?)

data class SoundDto(val enabled: Boolean?, val adzan: SoundItemDto?, val iqamah: SoundItemDto?)

data class ClockDto(val type: String?, val showSeconds: Boolean?, val use24h: Boolean?, val sizePercent: Int?)

data class NightModeDto(val enabled: Boolean?, val startHour: Int?, val endHour: Int?, val dimPercent: Int?)

/** Redup sepanjang hari pada hari tertentu (2.1). weekdays: 1 = Senin … 7 = Ahad. */
data class DimDaysDto(
    val enabled: Boolean?,
    val weekdays: List<Int>?,
    val nationalHolidays: Boolean?,
    val cutiBersama: Boolean?,
    val extraDates: List<String>?,
    val brightenForPrayer: Boolean?,
)

/** Tanggal merah dari server. kind: LIBUR (libur nasional) atau CUTI (cuti bersama). */
data class HolidayDto(val date: String?, val kind: String?)

data class DisplayMediaDto(
    val logoId: String?,
    val backgroundMainId: String?,
    val backgroundPrayerId: String?,
    val backgroundIqamahId: String?,
)

data class IllustrationDto(val silhouette: String?, val useMosquePhoto: Boolean?)

data class MutiaraDto(val enabled: Boolean?, val everyMinutes: Int?, val showSeconds: Int?)

/** Layar konten (rc.3): mati = tampilan seperti v1.x. */
data class ContentSettingsDto(val enabled: Boolean?, val slideSeconds: Int?)

data class DisplayDto(
    val layout: String?,
    val colorTheme: String?,
    val language: String?,
    val clock: ClockDto?,
    val nightMode: NightModeDto?,
    val dimDays: DimDaysDto? = null,
    val media: DisplayMediaDto?,
    val showWeather: Boolean?,
    val illustration: IllustrationDto?,
    val mutiaraHadits: MutiaraDto?,
    val content: ContentSettingsDto? = null,
)

data class KioskDto(val autoReturn: EnabledMinutesDto?)

data class SettingsDto(
    val schedule: ScheduleDto?,
    val prayerCycle: PrayerCycleDto?,
    val sound: SoundDto?,
    val display: DisplayDto?,
    val kiosk: KioskDto?,
)

/** Slide (Backend Schema §5.1). daysMask: bit 0 = Senin … bit 6 = Minggu. */
data class SlideDto(
    val id: String?,
    val contentId: String?,
    val durationSec: Int?,
    val daysMask: Int?,
    val timeFrom: String?,
    val timeTo: String?,
)

/** Konten: hadits, panel informasi (isi di [body]), atau gambar. */
data class ContentDto(
    val id: String?,
    val type: String?,
    val category: String?,
    val arabicText: String?,
    val translation: String?,
    val sourceRef: String?,
    // JsonElement, bukan JsonObject: Gson versi aplikasi ini gagal membaca `null` ke JsonObject.
    val body: com.google.gson.JsonElement?,
    val mediaId: String?,
) {
    /** Isi panel informasi/gambar, atau null untuk hadits. */
    val bodyObject: com.google.gson.JsonObject? get() = body?.takeIf { it.isJsonObject }?.asJsonObject
}

data class MediaRefDto(val id: String?, val kind: String?, val mime: String?, val sha256: String?, val bytes: Long?)

/** Pengumuman segera (F-TV-11). Waktu dalam ISO-8601 UTC. */
data class AnnouncementDto(val id: String?, val text: String?, val startsAt: String?, val expiresAt: String?)

data class BundleDto(
    val bundleSchema: Int?,
    val configVersion: Long?,
    val contentVersion: Long?,
    val mosque: MosqueDto?,
    val device: DeviceDto?,
    val settings: SettingsDto?,
    val runningTexts: List<RunningTextDto>?,
    val slides: List<SlideDto>? = null,
    val content: List<ContentDto>? = null,
    val announcements: List<AnnouncementDto>? = null,
    val media: List<MediaRefDto>? = null,
    val holidays: List<HolidayDto>? = null,
)

/** Cuaca dari server (Backend Schema §5.4). */
data class WeatherDto(
    val cellKey: String?,
    val fetchedAt: String?,
    val temperatureC: Double?,
    val feelsLikeC: Double?,
    val humidity: Int?,
    val windKmh: Double?,
    val iconCode: String?,
    val description: String?,
    val observedAt: Long?,
    val sunrise: Long?,
    val sunset: Long?,
    val cloudsPercent: Int?,
    val visibilityKm: Double?,
)
