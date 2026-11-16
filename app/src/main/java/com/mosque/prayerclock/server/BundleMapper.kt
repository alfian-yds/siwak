package com.mosque.prayerclock.server

import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.SoundType

/**
 * Pemetaan pengaturan server ke `AppSettings` aplikasi TV (Backend Schema §4.1).
 * Fungsi murni: tanpa Android, mudah diuji. Nilai di luar batas diabaikan (dipakai nilai lama),
 * sehingga data rusak dari server tidak membuat TV menampilkan jadwal yang salah.
 */
object BundleMapper {
    private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    private fun Int?.inRange(
        min: Int,
        max: Int,
        fallback: Int,
    ): Int = if (this != null && this in min..max) this else fallback

    private fun String?.time(fallback: String): String = if (this != null && TIME.matches(this)) this else fallback

    private fun soundType(
        type: String?,
        customUri: String?,
        fallback: SoundType,
    ): SoundType =
        when (type) {
            "BEEP" -> SoundType.TRADITIONAL_BEEP
            "TICK" -> SoundType.COUNTDOWN_TICKING
            // Audio kustom hanya dipakai bila berkasnya sudah terunduh; bila belum, bip agar tetap berbunyi.
            "CUSTOM" -> if (customUri != null) SoundType.CUSTOM else SoundType.TRADITIONAL_BEEP
            else -> fallback
        }

    /**
     * @param mediaUri id media → URI berkas lokal yang sudah terunduh dan terverifikasi (null bila tidak ada).
     */
    fun toAppSettings(
        current: AppSettings,
        bundle: BundleDto,
        today: String,
        mediaUri: (String?) -> String? = { null },
    ): AppSettings {
        val m = bundle.mosque
        val s = bundle.settings
        val sch = s?.schedule
        val man = sch?.manualTimes
        val off = sch?.offsets
        val hijri = sch?.hijri
        val hijriManual = hijri?.manual?.takeIf { it.day in 1..30 && it.month in 1..12 && it.year in 1400..1600 }
        val cyc = s?.prayerCycle
        val gaps = cyc?.iqamahGapMinutes
        val snd = s?.sound
        val disp = s?.display
        val clock = disp?.clock
        val night = disp?.nightMode
        val media = disp?.media
        val adzanUri = snd?.adzan?.takeIf { it.type == "CUSTOM" }?.mediaId.let(mediaUri)
        val iqamahUri = snd?.iqamah?.takeIf { it.type == "CUSTOM" }?.mediaId.let(mediaUri)
        val mutiara = disp?.mutiaraHadits
        val lat = m?.latitude?.takeIf { it in -90.0..90.0 }
        val lon = m?.longitude?.takeIf { it in -180.0..180.0 }

        return current.copy(
            mosqueName = m?.name?.takeIf { it.isNotBlank() } ?: current.mosqueName,
            city = m?.regionLabel ?: "",
            latitude = lat ?: current.latitude,
            longitude = lon ?: current.longitude,
            // Jadwal
            prayerServiceType =
                when (sch?.source) {
                    "KEMENAG" -> PrayerServiceType.AL_ADHAN_API
                    "MANUAL" -> PrayerServiceType.MANUAL
                    else -> current.prayerServiceType
                },
            manualImsak = man?.imsak.time(current.manualImsak),
            manualFajrAzan = man?.subuh.time(current.manualFajrAzan),
            manualSunrise = man?.terbit.time(current.manualSunrise),
            manualDhuhrAzan = man?.dzuhur.time(current.manualDhuhrAzan),
            manualAsrAzan = man?.ashar.time(current.manualAsrAzan),
            manualMaghribAzan = man?.maghrib.time(current.manualMaghribAzan),
            manualIshaAzan = man?.isya.time(current.manualIshaAzan),
            prayerOffsetEnabled = sch?.offsetsEnabled ?: current.prayerOffsetEnabled,
            imsakOffset = off?.imsak.inRange(-15, 15, current.imsakOffset),
            fajrOffset = off?.subuh.inRange(-15, 15, current.fajrOffset),
            sunriseOffset = off?.terbit.inRange(-15, 15, current.sunriseOffset),
            dhuhrOffset = off?.dzuhur.inRange(-15, 15, current.dhuhrOffset),
            asrOffset = off?.ashar.inRange(-15, 15, current.asrOffset),
            maghribOffset = off?.maghrib.inRange(-15, 15, current.maghribOffset),
            ishaOffset = off?.isya.inRange(-15, 15, current.ishaOffset),
            showImsak = sch?.showImsak ?: current.showImsak,
            showSunrise = sch?.showTerbit ?: current.showSunrise,
            // Hijriah
            hijriProvider =
                when (hijri?.source) {
                    "HISAB" -> HijriProvider.AL_ADHAN_API
                    "MANUAL" -> if (hijriManual != null) HijriProvider.MANUAL else current.hijriProvider
                    else -> current.hijriProvider
                },
            hijriDayOffset = hijri?.dayOffset.inRange(-2, 2, current.hijriDayOffset),
            manualHijriDay = hijriManual?.day ?: current.manualHijriDay,
            manualHijriMonth = hijriManual?.month ?: current.manualHijriMonth,
            manualHijriYear = hijriManual?.year ?: current.manualHijriYear,
            lastUpdatedGregorianDate =
                hijriManual?.setOn?.takeIf { DATE.matches(it) } ?: current.lastUpdatedGregorianDate,
            // Siklus adzan–iqamah
            fajrIqamahGap = gaps?.subuh.inRange(0, 60, current.fajrIqamahGap),
            dhuhrIqamahGap = gaps?.dzuhur.inRange(0, 60, current.dhuhrIqamahGap),
            asrIqamahGap = gaps?.ashar.inRange(0, 60, current.asrIqamahGap),
            maghribIqamahGap = gaps?.maghrib.inRange(0, 60, current.maghribIqamahGap),
            ishaIqamahGap = gaps?.isya.inRange(0, 60, current.ishaIqamahGap),
            showIqamahCycle = cyc?.showIqamahCycle ?: current.showIqamahCycle,
            fullScreenCountdownEnabled = cyc?.fullScreenCountdown?.enabled ?: current.fullScreenCountdownEnabled,
            fullScreenCountdownMinutes =
                cyc?.fullScreenCountdown?.minutes.inRange(1, 30, current.fullScreenCountdownMinutes),
            duaDisplayDurationMinutes = cyc?.prayerScreenMinutes.inRange(1, 15, current.duaDisplayDurationMinutes),
            showJummahScreen = cyc?.jumat?.screenEnabled ?: current.showJummahScreen,
            jummahDurationMinutes = cyc?.jumat?.durationMinutes.inRange(30, 120, current.jummahDurationMinutes),
            jummaNightBayanEnabled = cyc?.jumatNightBayan?.enabled ?: current.jummaNightBayanEnabled,
            jummaNightBayanMinutes = cyc?.jumatNightBayan?.minutes.inRange(0, 120, current.jummaNightBayanMinutes),
            // Suara
            soundEnabled = snd?.enabled ?: current.soundEnabled,
            azanSoundEnabled = snd?.adzan?.enabled ?: current.azanSoundEnabled,
            azanSoundType = soundType(snd?.adzan?.type, adzanUri, current.azanSoundType),
            azanSoundUri = adzanUri ?: "",
            iqamahSoundEnabled = snd?.iqamah?.enabled ?: current.iqamahSoundEnabled,
            iqamahSoundType = soundType(snd?.iqamah?.type, iqamahUri, current.iqamahSoundType),
            iqamahSoundUri = iqamahUri ?: "",
            // Tampilan
            colorTheme = disp?.colorTheme?.takeIf { it.matches(Regex("^[a-z_]{3,40}$")) } ?: current.colorTheme,
            language =
                when (disp?.language) {
                    "id" -> Language.INDONESIAN
                    "en" -> Language.ENGLISH
                    else -> current.language
                },
            clockType =
                when (clock?.type) {
                    "DIGITAL" -> ClockType.DIGITAL
                    "ANALOG" -> ClockType.ANALOG
                    else -> current.clockType
                },
            showSeconds = clock?.showSeconds ?: current.showSeconds,
            show24HourFormat = clock?.use24h ?: current.show24HourFormat,
            clockSizePercent = clock?.sizePercent?.takeIf { it in setOf(100, 115, 130) } ?: current.clockSizePercent,
            nightModeEnabled = night?.enabled ?: current.nightModeEnabled,
            nightStartHour = night?.startHour.inRange(0, 23, current.nightStartHour),
            nightEndHour = night?.endHour.inRange(0, 23, current.nightEndHour),
            nightDimLevel = night?.dimPercent.inRange(0, 100, current.nightDimLevel),
            showWeather = disp?.showWeather ?: current.showWeather,
            // Media dari server menggantikan pilihan lokal; kosong = memakai bawaan.
            mosqueLogoUri = if (disp != null) mediaUri(media?.logoId) ?: "" else current.mosqueLogoUri,
            backgroundMainUri = if (disp != null) mediaUri(media?.backgroundMainId) ?: "" else current.backgroundMainUri,
            backgroundPrayerUri = if (disp != null) mediaUri(media?.backgroundPrayerId) ?: "" else current.backgroundPrayerUri,
            backgroundIqamahUri = if (disp != null) mediaUri(media?.backgroundIqamahId) ?: "" else current.backgroundIqamahUri,
            layout = disp?.layout?.takeIf { it == "BAR_BAWAH" || it == "GALERI" } ?: current.layout,
            illustrationSilhouette =
                disp?.illustration?.silhouette?.takeIf { it in setOf("KUBAH", "TUMPANG", "MUSHOLLA") } ?: current.illustrationSilhouette,
            mutiaraHaditsEnabled = mutiara?.enabled ?: current.mutiaraHaditsEnabled,
            mutiaraEveryMinutes = mutiara?.everyMinutes.inRange(5, 60, current.mutiaraEveryMinutes),
            mutiaraShowSeconds = mutiara?.showSeconds.inRange(15, 120, current.mutiaraShowSeconds),
            runningTextMessages = runningTextMessages(bundle.runningTexts, today),
        )
    }

    /**
     * Teks berjalan yang berlaku hari ini, digabung dengan pemisah `||` (format v1.x).
     * Dipanggil ulang setiap hari karena bundle bisa dipakai berhari-hari saat offline.
     */
    fun runningTextMessages(
        texts: List<RunningTextDto>?,
        today: String,
    ): String =
        (texts ?: emptyList())
            .filter { t ->
                val text = t.text?.trim().orEmpty()
                text.isNotEmpty() &&
                    (t.startsOn == null || t.startsOn <= today) &&
                    (t.endsOn == null || t.endsOn >= today)
            }.joinToString("||") { it.text!!.trim().replace("||", "|") }

    /** Apakah perubahan ini mengharuskan jadwal shalat dihitung ulang. */
    fun affectsPrayerTimes(
        a: AppSettings,
        b: AppSettings,
    ): Boolean =
        a.latitude != b.latitude ||
            a.longitude != b.longitude ||
            a.prayerServiceType != b.prayerServiceType ||
            a.prayerOffsetEnabled != b.prayerOffsetEnabled ||
            listOf(a.imsakOffset, a.fajrOffset, a.sunriseOffset, a.dhuhrOffset, a.asrOffset, a.maghribOffset, a.ishaOffset) !=
            listOf(b.imsakOffset, b.fajrOffset, b.sunriseOffset, b.dhuhrOffset, b.asrOffset, b.maghribOffset, b.ishaOffset) ||
            listOf(a.manualImsak, a.manualFajrAzan, a.manualSunrise, a.manualDhuhrAzan, a.manualAsrAzan, a.manualMaghribAzan, a.manualIshaAzan) !=
            listOf(b.manualImsak, b.manualFajrAzan, b.manualSunrise, b.manualDhuhrAzan, b.manualAsrAzan, b.manualMaghribAzan, b.manualIshaAzan)
}
