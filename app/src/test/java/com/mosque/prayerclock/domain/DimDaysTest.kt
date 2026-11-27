package com.mosque.prayerclock.domain

import com.mosque.prayerclock.domain.DimDays.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DimDaysTest {
    private fun dim(
        date: String,
        dow: Int,
        weekdays: Set<Int> = emptySet(),
        national: Boolean = true,
        cuti: Boolean = false,
        extra: Set<String> = emptySet(),
        server: Map<String, Kind> = emptyMap(),
    ) = DimDays.isDimDay(date, dow, weekdays, national, cuti, extra, server)

    @Test fun jumlahSesuaiSkb() {
        fun count(year: String, kind: Kind) = DimDays.BUILT_IN.count { (d, k) -> d.startsWith(year) && k == kind }
        assertEquals(17, count("2026", Kind.LIBUR))
        assertEquals(8, count("2026", Kind.CUTI))
        assertEquals(18, count("2027", Kind.LIBUR))
        assertEquals(8, count("2027", Kind.CUTI))
    }

    @Test fun akhirPekan() {
        assertTrue(dim("2026-11-28", 6, weekdays = setOf(6, 7))) // Sabtu
        assertFalse(dim("2026-11-27", 5, weekdays = setOf(6, 7))) // Jumat
    }

    @Test fun tanggalMerahDanCutiBersama() {
        assertTrue(dim("2026-12-25", 5)) // Natal, libur nasional
        assertFalse(dim("2026-12-24", 4)) // cuti bersama, belum dicentang
        assertTrue(dim("2026-12-24", 4, cuti = true))
        assertFalse(dim("2026-12-25", 5, national = false))
    }

    @Test fun tanggalTambahan() = assertTrue(dim("2027-07-01", 4, national = false, extra = setOf("2027-07-01")))

    @Test fun daftarServerDipakaiUntukTahunBerikutnya() {
        val server = DimDays.parseServer("2028-01-01:L,2028-02-25:C")
        assertTrue(dim("2028-01-01", 6, server = server))
        assertFalse(dim("2028-02-25", 5, server = server))
        assertTrue(dim("2028-02-25", 5, cuti = true, server = server))
    }

    @Test fun simpanBaca() {
        val enc = DimDays.encode(listOf("2027-01-01" to Kind.LIBUR, "2027-02-05" to Kind.CUTI))
        assertEquals(mapOf("2027-01-01" to Kind.LIBUR, "2027-02-05" to Kind.CUTI), DimDays.parseServer(enc))
        assertEquals(setOf(6, 7), DimDays.parseWeekdays("6,7,9,x"))
    }

    @Test fun redupDiSemuaLayarBilaTidakTerangSaatShalat() {
        val base = ScreenInputs(false, prayerScreenActive = true, false, false, mutiaraDue = false, secondsUntilPriorityScreen = 0, nightHours = true)
        assertFalse(ScreenPriorityResolver.resolve(base).dimForNight)
        assertTrue(ScreenPriorityResolver.resolve(base.copy(dimAllScreens = true)).dimForNight)
    }
}
