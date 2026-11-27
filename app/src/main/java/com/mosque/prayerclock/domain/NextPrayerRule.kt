package com.mosque.prayerclock.domain

import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.utils.TimeUtils

/**
 * Menentukan waktu shalat yang sedang berlangsung atau berikutnya, dari jam sekarang ("HH:mm").
 *
 * - Dari adzan sampai iqamah + lama layar shalat: waktu itu masih dianggap "sekarang" (hitung mundur iqamah).
 * - Jumat: Dzuhur tanpa iqamah, dianggap berlangsung sampai adzan + lama khutbah.
 * - Selain itu: waktu pertama hari ini yang adzannya belum tiba; bila sudah lewat Isya, Subuh (besok).
 *
 * Aturan ini murni (tanpa jam sistem) agar bisa diuji, dan dipanggil berkala dari MainViewModel.
 */
object NextPrayerRule {
    data class Slot(
        val type: PrayerType,
        val azan: String,
        val iqamah: String?,
    )

    fun calculate(
        slots: List<Slot>,
        now: String,
        isFriday: Boolean,
        prayerScreenMinutes: Int,
        jummahMinutes: Int,
    ): PrayerType {
        for (s in slots) {
            val iqamah = if (isFriday && s.type == PrayerType.DHUHR) null else s.iqamah
            val end =
                when {
                    iqamah != null -> TimeUtils.addMinutesToTime(iqamah, prayerScreenMinutes)
                    isFriday && s.type == PrayerType.DHUHR -> TimeUtils.addMinutesToTime(s.azan, jummahMinutes)
                    else -> continue
                }
            if (TimeUtils.compareTimeStrings(s.azan, now) <= 0 && TimeUtils.compareTimeStrings(now, end) < 0) return s.type
        }
        return slots.firstOrNull { TimeUtils.compareTimeStrings(now, it.azan) < 0 }?.type ?: PrayerType.FAJR
    }

    /** Isi kartu "Berikutnya" di Galeri. */
    enum class Phase { ADZAN, IQAMAH, BERLANGSUNG }

    data class Label(
        val phase: Phase,
        /** Menit menuju adzan/iqamah; null saat shalat/khutbah berlangsung. */
        val minutesLeft: Int?,
    )

    /**
     * Semua nilai dalam menit sejak tengah malam. Antara adzan dan iqamah kartu menghitung mundur ke iqamah;
     * setelah iqamah (layar shalat) atau selama Jumat tanpa iqamah, waktu itu sedang berlangsung.
     * Di luar itu menghitung mundur ke adzan (melewati tengah malam bila perlu, mis. Subuh besok).
     */
    fun label(
        azan: Int,
        iqamah: Int?,
        now: Int,
        prayerScreenMinutes: Int,
        /** Lama Jumat (khutbah dan shalat) bila ini Dzuhur hari Jumat; null untuk waktu lain. */
        jummahMinutes: Int?,
    ): Label {
        val end = iqamah?.let { it + prayerScreenMinutes } ?: jummahMinutes?.let { azan + it }
        val inPeriod = end != null && azan <= now && now < end
        return when {
            inPeriod && iqamah != null && now < iqamah -> Label(Phase.IQAMAH, iqamah - now)
            inPeriod -> Label(Phase.BERLANGSUNG, null)
            else -> Label(Phase.ADZAN, ((azan - now) % 1440 + 1440) % 1440)
        }
    }
}
