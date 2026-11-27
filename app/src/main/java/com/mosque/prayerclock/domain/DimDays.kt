package com.mosque.prayerclock.domain

/**
 * Redup sepanjang hari pada hari tertentu (2.1): hari dalam pekan, tanggal merah, dan tanggal tambahan.
 *
 * Tanggal merah bawaan: SKB 3 Menteri tentang Hari Libur Nasional dan Cuti Bersama
 * (2026: SKB 1497/2/5 Tahun 2025; 2027: SKB 1205, 3, dan 2 Tahun 2026). Salinan yang sama ada di server
 * (api/src/holidays.ts); TV terhubung juga menerima daftar terbaru dari bundle sehingga tahun berikutnya
 * tidak menunggu pembaruan aplikasi.
 */
object DimDays {
    enum class Kind { LIBUR, CUTI }

    private val L = Kind.LIBUR
    private val C = Kind.CUTI

    val BUILT_IN: Map<String, Kind> =
        mapOf(
            // 2026
            "2026-01-01" to L, "2026-01-16" to L, "2026-02-16" to C, "2026-02-17" to L,
            "2026-03-18" to C, "2026-03-19" to L, "2026-03-20" to C, "2026-03-21" to L,
            "2026-03-22" to L, "2026-03-23" to C, "2026-03-24" to C, "2026-04-03" to L,
            "2026-04-05" to L, "2026-05-01" to L, "2026-05-14" to L, "2026-05-15" to C,
            "2026-05-27" to L, "2026-05-28" to C, "2026-05-31" to L, "2026-06-01" to L,
            "2026-06-16" to L, "2026-08-17" to L, "2026-08-25" to L, "2026-12-24" to C,
            "2026-12-25" to L,
            // 2027
            "2027-01-01" to L, "2027-01-05" to L, "2027-02-05" to C, "2027-02-06" to L,
            "2027-03-08" to L, "2027-03-09" to C, "2027-03-10" to L, "2027-03-11" to L,
            "2027-03-12" to C, "2027-03-15" to C, "2027-03-25" to C, "2027-03-26" to L,
            "2027-03-28" to L, "2027-05-01" to L, "2027-05-06" to L, "2027-05-17" to L,
            "2027-05-18" to C, "2027-05-19" to C, "2027-05-20" to L, "2027-06-01" to L,
            "2027-06-06" to L, "2027-08-15" to L, "2027-08-17" to L, "2027-12-24" to C,
            "2027-12-25" to L, "2027-12-26" to L,
        )

    /** "2027-01-01:L,2027-02-05:C" (disimpan di DataStore dari bundle) → peta tanggal. */
    fun parseServer(encoded: String): Map<String, Kind> =
        encoded
            .split(',')
            .mapNotNull { part ->
                val (date, k) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                val kind =
                    when (k) {
                        "L" -> L
                        "C" -> C
                        else -> return@mapNotNull null
                    }
                date.trim() to kind
            }.toMap()

    fun encode(list: List<Pair<String, Kind>>): String = list.joinToString(",") { (d, k) -> "$d:${if (k == L) "L" else "C"}" }

    /** "6,7" → {6, 7}; nilai di luar 1..7 diabaikan. */
    fun parseWeekdays(encoded: String): Set<Int> = encoded.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 } }.toSet()

    /**
     * Apakah [date] (YYYY-MM-DD) termasuk hari redup.
     * @param isoDayOfWeek 1 = Senin … 7 = Ahad.
     * @param serverHolidays daftar dari bundle (diutamakan bila tanggal yang sama ada di kedua daftar).
     */
    fun isDimDay(
        date: String,
        isoDayOfWeek: Int,
        weekdays: Set<Int>,
        nationalHolidays: Boolean,
        cutiBersama: Boolean,
        extraDates: Set<String>,
        serverHolidays: Map<String, Kind> = emptyMap(),
    ): Boolean {
        if (isoDayOfWeek in weekdays || date in extraDates) return true
        return when (serverHolidays[date] ?: BUILT_IN[date]) {
            Kind.LIBUR -> nationalHolidays
            Kind.CUTI -> cutiBersama
            null -> false
        }
    }
}
