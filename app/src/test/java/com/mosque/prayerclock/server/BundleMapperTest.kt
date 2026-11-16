package com.mosque.prayerclock.server

import com.google.gson.Gson
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.ClockType
import com.mosque.prayerclock.data.model.HijriProvider
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.PrayerServiceType
import com.mosque.prayerclock.data.model.SoundType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji pemetaan bundle server ke AppSettings (Backend Schema §4.1, tugas 1.14). */
class BundleMapperTest {
    private val gson = Gson()

    /** Bentuk persis jawaban GET /api/v1/device/bundle dari server SIWAK. */
    private val bundleJson =
        """
        {
          "bundleSchema": 1, "generatedAt": "2026-11-16T14:00:00.000Z", "configVersion": 7, "contentVersion": 1,
          "mosque": { "id": "m1", "name": "Masjid Al-Hidayah", "regionLabel": "Kota Bandung", "latitude": -6.914744, "longitude": 107.60981, "timezone": "Asia/Jakarta" },
          "device": { "id": "d1", "name": "Serambi", "pinSalt": "aa", "pinHash": "bb" },
          "settings": {
            "schedule": {
              "source": "MANUAL",
              "manualTimes": { "imsak": "04:10", "subuh": "04:20", "terbit": "05:35", "dzuhur": "11:50", "ashar": "15:05", "maghrib": "17:55", "isya": "19:05" },
              "offsetsEnabled": true,
              "offsets": { "imsak": -1, "subuh": 2, "terbit": 0, "dzuhur": 1, "ashar": 3, "maghrib": -2, "isya": 4 },
              "showImsak": true, "showTerbit": false,
              "hijri": { "source": "MANUAL", "dayOffset": 1, "manual": { "day": 15, "month": 5, "year": 1448, "setOn": "2026-11-16" } }
            },
            "prayerCycle": {
              "iqamahGapMinutes": { "subuh": 25, "dzuhur": 12, "ashar": 11, "maghrib": 7, "isya": 9 },
              "showIqamahCycle": false,
              "fullScreenCountdown": { "enabled": false, "minutes": 7 },
              "prayerScreenMinutes": 6,
              "jumat": { "screenEnabled": false, "durationMinutes": 75 },
              "jumatNightBayan": { "enabled": true, "minutes": 40 }
            },
            "sound": { "enabled": false, "adzan": { "enabled": false, "type": "TICK", "mediaId": null }, "iqamah": { "enabled": true, "type": "BEEP", "mediaId": null } },
            "display": {
              "layout": "BAR_BAWAH", "colorTheme": "emerald_garden", "language": "en",
              "clock": { "type": "ANALOG", "showSeconds": false, "use24h": false, "sizePercent": 130 },
              "nightMode": { "enabled": true, "startHour": 21, "endHour": 3, "dimPercent": 70 },
              "media": { "logoId": null, "backgroundMainId": null, "backgroundPrayerId": null, "backgroundIqamahId": null },
              "showWeather": false,
              "illustration": { "silhouette": "KUBAH", "useMosquePhoto": false },
              "mutiaraHadits": { "enabled": true, "everyMinutes": 15, "showSeconds": 30 },
              "hadithCategories": ["jumat"]
            },
            "kiosk": { "autoReturn": { "enabled": true, "minutes": 15 } }
          },
          "runningTexts": [
            { "id": "t1", "text": "Kajian Selasa", "startsOn": null, "endsOn": null },
            { "id": "t2", "text": "Belum mulai", "startsOn": "2026-12-01", "endsOn": null },
            { "id": "t3", "text": "Sudah lewat", "startsOn": null, "endsOn": "2026-11-15" },
            { "id": "t4", "text": "Hari ini saja", "startsOn": "2026-11-16", "endsOn": "2026-11-16" }
          ],
          "slides": [], "content": [], "announcements": [], "media": [],
          "fieldBaruDariServer": { "diabaikan": true }
        }
        """.trimIndent()

    private val bundle = gson.fromJson(bundleJson, BundleDto::class.java)
    private val result = BundleMapper.toAppSettings(AppSettings(), bundle, today = "2026-11-16")

    @Test
    fun masjidDanLokasi() {
        assertEquals("Masjid Al-Hidayah", result.mosqueName)
        assertEquals("Kota Bandung", result.city)
        assertEquals(-6.914744, result.latitude, 1e-9)
        assertEquals(107.60981, result.longitude, 1e-9)
    }

    @Test
    fun jadwalManualDanKoreksiMenit() {
        assertEquals(PrayerServiceType.MANUAL, result.prayerServiceType)
        assertEquals(listOf("04:10", "04:20", "05:35", "11:50", "15:05", "17:55", "19:05"),
            listOf(result.manualImsak, result.manualFajrAzan, result.manualSunrise, result.manualDhuhrAzan, result.manualAsrAzan, result.manualMaghribAzan, result.manualIshaAzan))
        assertTrue(result.prayerOffsetEnabled)
        assertEquals(listOf(-1, 2, 0, 1, 3, -2, 4),
            listOf(result.imsakOffset, result.fajrOffset, result.sunriseOffset, result.dhuhrOffset, result.asrOffset, result.maghribOffset, result.ishaOffset))
        assertTrue(result.showImsak)
        assertFalse(result.showSunrise)
    }

    @Test
    fun tanggalHijriahManual() {
        assertEquals(HijriProvider.MANUAL, result.hijriProvider)
        assertEquals(1, result.hijriDayOffset)
        assertEquals(15, result.manualHijriDay)
        assertEquals(5, result.manualHijriMonth)
        assertEquals(1448, result.manualHijriYear)
        assertEquals("2026-11-16", result.lastUpdatedGregorianDate)
    }

    @Test
    fun siklusAdzanIqamahDanJumat() {
        assertEquals(listOf(25, 12, 11, 7, 9),
            listOf(result.fajrIqamahGap, result.dhuhrIqamahGap, result.asrIqamahGap, result.maghribIqamahGap, result.ishaIqamahGap))
        assertFalse(result.showIqamahCycle)
        assertFalse(result.fullScreenCountdownEnabled)
        assertEquals(7, result.fullScreenCountdownMinutes)
        assertEquals(6, result.duaDisplayDurationMinutes)
        assertFalse(result.showJummahScreen)
        assertEquals(75, result.jummahDurationMinutes)
        assertTrue(result.jummaNightBayanEnabled)
        assertEquals(40, result.jummaNightBayanMinutes)
    }

    @Test
    fun suara() {
        assertFalse(result.soundEnabled)
        assertFalse(result.azanSoundEnabled)
        assertEquals(SoundType.COUNTDOWN_TICKING, result.azanSoundType)
        assertTrue(result.iqamahSoundEnabled)
        assertEquals(SoundType.TRADITIONAL_BEEP, result.iqamahSoundType)
    }

    @Test
    fun tampilan() {
        assertEquals("emerald_garden", result.colorTheme)
        assertEquals(Language.ENGLISH, result.language)
        assertEquals(ClockType.ANALOG, result.clockType)
        assertFalse(result.showSeconds)
        assertFalse(result.show24HourFormat)
        assertEquals(130, result.clockSizePercent)
        assertTrue(result.nightModeEnabled)
        assertEquals(21, result.nightStartHour)
        assertEquals(3, result.nightEndHour)
        assertEquals(70, result.nightDimLevel)
        assertFalse(result.showWeather)
    }

    @Test
    fun teksBerjalanSesuaiTanggalHariIni() {
        assertEquals("Kajian Selasa||Hari ini saja", result.runningTextMessages)
        assertEquals("Kajian Selasa||Belum mulai", BundleMapper.runningTextMessages(bundle.runningTexts, "2026-12-01"))
    }

    @Test
    fun fieldLokalTidakDisentuh() {
        val local = AppSettings(openWeatherMapApiKey = "kunci-lokal", autoUpdateCheckEnabled = false, mosqueLogoUri = "content://logo")
        val r = BundleMapper.toAppSettings(local, bundle, "2026-11-16")
        assertEquals("kunci-lokal", r.openWeatherMapApiKey)
        assertFalse(r.autoUpdateCheckEnabled)
        // TV terhubung: logo diatur dari admin web; server tidak memilih logo → memakai bawaan.
        assertEquals("", r.mosqueLogoUri)
    }

    @Test
    fun mediaDanTampilanGaleri() {
        val json =
            """{"settings":{"sound":{"enabled":true,"adzan":{"enabled":true,"type":"CUSTOM","mediaId":"a1"},"iqamah":{"enabled":true,"type":"CUSTOM","mediaId":"belum"}},
              "display":{"layout":"GALERI","media":{"logoId":"l1","backgroundMainId":null},"illustration":{"silhouette":"TUMPANG"},"mutiaraHadits":{"enabled":true,"everyMinutes":20,"showSeconds":45}}}}"""
        val files = mapOf("a1" to "file:///media/aaa", "l1" to "file:///media/lll")
        val r = BundleMapper.toAppSettings(AppSettings(), gson.fromJson(json, BundleDto::class.java), "2026-11-16") { files[it] }
        assertEquals(SoundType.CUSTOM, r.azanSoundType)
        assertEquals("file:///media/aaa", r.azanSoundUri)
        // Audio iqamah belum terunduh → tetap bip agar tidak diam.
        assertEquals(SoundType.TRADITIONAL_BEEP, r.iqamahSoundType)
        assertEquals("file:///media/lll", r.mosqueLogoUri)
        assertEquals("", r.backgroundMainUri)
        assertEquals("GALERI", r.layout)
        assertEquals("TUMPANG", r.illustrationSilhouette)
        assertTrue(r.mutiaraHaditsEnabled)
        assertEquals(20, r.mutiaraEveryMinutes)
        assertEquals(45, r.mutiaraShowSeconds)
    }

    @Test
    fun nilaiDiLuarBatasMemakaiNilaiLama() {
        val bad =
            gson.fromJson(
                """{"mosque":{"latitude":200,"longitude":5},"settings":{"schedule":{"offsets":{"subuh":99},"manualTimes":{"subuh":"25:99"}},"display":{"clock":{"sizePercent":120}}}}""",
                BundleDto::class.java,
            )
        val current = AppSettings(latitude = -7.0, fajrOffset = 3, manualFajrAzan = "04:30", clockSizePercent = 115)
        val r = BundleMapper.toAppSettings(current, bad, "2026-11-16")
        assertEquals(-7.0, r.latitude, 0.0)
        assertEquals(5.0, r.longitude, 0.0)
        assertEquals(3, r.fajrOffset)
        assertEquals("04:30", r.manualFajrAzan)
        assertEquals(115, r.clockSizePercent)
    }

    @Test
    fun bundleKosongTidakMengubahPengaturanKecualiTeks() {
        val empty = gson.fromJson("{}", BundleDto::class.java)
        val current = AppSettings(mosqueName = "Lama", fajrIqamahGap = 30)
        val r = BundleMapper.toAppSettings(current, empty, "2026-11-16")
        assertEquals("Lama", r.mosqueName)
        assertEquals(30, r.fajrIqamahGap)
    }

    @Test
    fun perubahanJadwalTerdeteksi() {
        assertTrue(BundleMapper.affectsPrayerTimes(AppSettings(), result))
        assertFalse(BundleMapper.affectsPrayerTimes(result, result.copy(colorTheme = "amber_sunset")))
    }
}
