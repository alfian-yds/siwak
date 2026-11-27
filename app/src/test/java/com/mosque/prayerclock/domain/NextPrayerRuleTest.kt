package com.mosque.prayerclock.domain

import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.domain.NextPrayerRule.Slot
import org.junit.Assert.assertEquals
import org.junit.Test

/** Jadwal dari laporan lapangan 27 Nov 2026 (Ashar 14.55); iqamah 10 menit, layar shalat 10 menit. */
class NextPrayerRuleTest {
    private val slots =
        listOf(
            Slot(PrayerType.FAJR, "03:50", "04:00"),
            Slot(PrayerType.SUNRISE, "05:09", null),
            Slot(PrayerType.DHUHR, "11:30", "11:40"),
            Slot(PrayerType.ASR, "14:55", "15:05"),
            Slot(PrayerType.MAGHRIB, "17:46", "17:56"),
            Slot(PrayerType.ISHA, "19:01", "19:11"),
        )

    private fun at(now: String, friday: Boolean = false) =
        NextPrayerRule.calculate(slots, now, isFriday = friday, prayerScreenMinutes = 10, jummahMinutes = 30)

    @Test fun sebelumAshar() = assertEquals(PrayerType.ASR, at("14:30"))

    @Test fun antaraAdzanDanIqamahAsharTetapAshar() = assertEquals(PrayerType.ASR, at("14:59"))

    @Test fun layarShalatAsharMasihAshar() = assertEquals(PrayerType.ASR, at("15:10"))

    @Test fun setelahLayarShalatAsharKeMaghrib() = assertEquals(PrayerType.MAGHRIB, at("15:15"))

    @Test fun setelahLayarShalatMaghribKeIsya() = assertEquals(PrayerType.ISHA, at("18:06"))

    @Test fun setelahIsyaKeSubuh() = assertEquals(PrayerType.FAJR, at("19:30"))

    @Test fun lewatTengahMalamSubuh() = assertEquals(PrayerType.FAJR, at("01:00"))

    @Test fun setelahSubuhKeTerbitLaluDzuhur() {
        assertEquals(PrayerType.SUNRISE, at("04:30"))
        assertEquals(PrayerType.DHUHR, at("05:09"))
    }

    private fun hm(s: String) = s.substring(0, 2).toInt() * 60 + s.substring(3).toInt()

    private fun label(now: String, azan: String, iqamah: String?, jumat: Int? = null) =
        NextPrayerRule.label(hm(azan), iqamah?.let(::hm), hm(now), prayerScreenMinutes = 10, jummahMinutes = jumat)

    @Test fun kartuGaleri() {
        assertEquals(NextPrayerRule.Label(NextPrayerRule.Phase.ADZAN, 25), label("14:30", "14:55", "15:05"))
        assertEquals(NextPrayerRule.Label(NextPrayerRule.Phase.IQAMAH, 6), label("14:59", "14:55", "15:05")) // kasus laporan
        assertEquals(NextPrayerRule.Label(NextPrayerRule.Phase.BERLANGSUNG, null), label("15:10", "14:55", "15:05")) // layar shalat
        assertEquals(NextPrayerRule.Label(NextPrayerRule.Phase.ADZAN, 500), label("19:30", "03:50", "04:00")) // Subuh besok
        assertEquals(NextPrayerRule.Label(NextPrayerRule.Phase.BERLANGSUNG, null), label("11:50", "11:30", null, jumat = 30))
        assertEquals(NextPrayerRule.Label(NextPrayerRule.Phase.ADZAN, 1439), label("05:10", "05:09", null)) // Terbit lewat: bukan "berlangsung"
    }

    @Test fun jumatDzuhurBerlangsungSelamaKhutbah() {
        assertEquals(PrayerType.DHUHR, at("11:55", friday = true))
        assertEquals(PrayerType.ASR, at("12:00", friday = true))
    }
}
