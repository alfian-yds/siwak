package com.mosque.prayerclock.domain

import com.mosque.prayerclock.server.ContentDto
import com.mosque.prayerclock.server.SlideDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji satu baris per aturan di App Flow §3 (tugas 2.10). */
class ScreenRulesTest {
    private val base = ScreenInputs(false, false, false, false, mutiaraDue = false, secondsUntilPriorityScreen = 3600, nightHours = false)
    private fun r(i: ScreenInputs) = ScreenPriorityResolver.resolve(i)

    @Test fun p1JumatMengalahkanSemua() =
        assertEquals(TvScreen.JUMAT, r(base.copy(jumatActive = true, prayerScreenActive = true, adzanCountdownActive = true, mutiaraDue = true)).screen)

    @Test fun p2LayarShalat() = assertEquals(TvScreen.SHALAT, r(base.copy(prayerScreenActive = true, iqamahCountdownActive = true)).screen)

    @Test fun p3HitungMundurIqamah() = assertEquals(TvScreen.IQAMAH_COUNTDOWN, r(base.copy(iqamahCountdownActive = true, adzanCountdownActive = true)).screen)

    @Test fun p4HitungMundurAdzan() = assertEquals(TvScreen.ADZAN_COUNTDOWN, r(base.copy(adzanCountdownActive = true, mutiaraDue = true)).screen)

    @Test fun p5MutiaraHadits() = assertEquals(TvScreen.MUTIARA_HADITS, r(base.copy(mutiaraDue = true)).screen)

    @Test fun p5TidakDalamDuaMenitSebelumLayarPrioritas() {
        assertEquals(TvScreen.UTAMA, r(base.copy(mutiaraDue = true, secondsUntilPriorityScreen = 100)).screen)
        assertEquals(TvScreen.MUTIARA_HADITS, r(base.copy(mutiaraDue = true, secondsUntilPriorityScreen = 121)).screen)
    }

    @Test fun p5TidakSaatModeMalam() = assertEquals(TvScreen.UTAMA, r(base.copy(mutiaraDue = true, nightHours = true)).screen)

    @Test fun p6TampilanUtama() = assertEquals(TvScreen.UTAMA, r(base).screen)

    @Test fun redupMalamHanyaDiTampilanUtama() {
        assertTrue(r(base.copy(nightHours = true)).dimForNight)
        assertFalse(r(base.copy(nightHours = true, adzanCountdownActive = true)).dimForNight)
    }

    @Test fun slideBerhentiSaatPrioritas1Sampai4() {
        assertTrue(r(base.copy(iqamahCountdownActive = true)).slidesPaused)
        assertFalse(r(base.copy(mutiaraDue = true)).slidesPaused)
        assertFalse(r(base).slidesPaused)
    }

    @Test fun periodeIlustrasi() {
        val (subuh, terbit, maghrib, isya) = listOf(4 * 60, 5 * 60 + 20, 17 * 60 + 50, 19 * 60)
        assertEquals(SkyPeriod.FAJAR, Periods.at(4 * 60 + 30, subuh, terbit, maghrib, isya))
        assertEquals(SkyPeriod.SIANG, Periods.at(12 * 60, subuh, terbit, maghrib, isya))
        assertEquals(SkyPeriod.SENJA, Periods.at(17 * 60 + 25, subuh, terbit, maghrib, isya)) // 30 menit sebelum Maghrib
        assertEquals(SkyPeriod.MALAM, Periods.at(21 * 60, subuh, terbit, maghrib, isya))
        assertEquals(SkyPeriod.MALAM, Periods.at(2 * 60, subuh, terbit, maghrib, isya))
    }

    @Test fun faseBulanSesuaiTanggalHijriah() {
        // Kriteria tugas 2.8: tanggal 1, 7, 14, 21, 27.
        assertTrue(MoonPhase.illumination(1) < 0.05) // hilal
        assertEquals(0.5, MoonPhase.illumination(7), 0.12) // separuh, sedang membesar
        assertTrue(MoonPhase.waxing(7))
        assertTrue(MoonPhase.illumination(14) > 0.95) // purnama
        assertEquals(0.5, MoonPhase.illumination(21), 0.12) // separuh, mengecil
        assertFalse(MoonPhase.waxing(21))
        assertTrue(MoonPhase.illumination(27) < 0.2) // sabit tua
    }

    private fun c(id: String, type: String = "info_kas") = ContentDto(id, type, null, null, "t", null, null, null)

    @Test fun jadwalSlideHariDanJam() {
        val s = SlideDto("s", "a", 20, 0b0000001, "21:00", "04:00") // hanya Senin, lintas tengah malam
        assertTrue(SlidePlaylist.matches(s, 0, 22 * 60))
        assertTrue(SlidePlaylist.matches(s, 0, 3 * 60))
        assertFalse(SlidePlaylist.matches(s, 0, 12 * 60))
        assertFalse(SlidePlaylist.matches(s, 1, 22 * 60))
    }

    @Test fun haditsOtomatisDisisipkanSetiapDuaSlide() {
        val slides = listOf("a", "b", "c", "d").map { SlideDto("s$it", it, null, null, null, null) }
        val content = listOf("a", "b", "c", "d").map { c(it) }
        val list = SlidePlaylist.build(slides, content, 0, 600, listOf(c("h1", "hadith"), c("h2", "hadith")))
        assertEquals(listOf("a", "b", "h1", "c", "d", "h2"), list.map { it.content.id })
        assertEquals(20, list.first().durationSec)
    }

    @Test fun petugasJumatDidahulukanKamisSoreSampaiJumatSiang() {
        val slides = listOf(SlideDto("1", "kas", 15, null, null, null), SlideDto("2", "jumat", 15, null, null, null))
        val content = listOf(c("kas"), c("jumat", "info_jumat"))
        assertEquals("kas", SlidePlaylist.build(slides, content, 2, 600, emptyList()).first().content.id)
        assertEquals("jumat", SlidePlaylist.build(slides, content, 3, 16 * 60, emptyList()).first().content.id)
        assertEquals("jumat", SlidePlaylist.build(slides, content, 4, 9 * 60, emptyList()).first().content.id)
        assertEquals("kas", SlidePlaylist.build(slides, content, 4, 13 * 60, emptyList()).first().content.id)
    }

    @Test fun tanpaSlidePakaiHaditsOtomatis() {
        assertEquals(listOf("h"), SlidePlaylist.build(emptyList(), emptyList(), 0, 0, listOf(c("h", "hadith"))).map { it.content.id })
        assertTrue(SlidePlaylist.build(emptyList(), emptyList(), 0, 0, emptyList()).isEmpty())
    }
}
