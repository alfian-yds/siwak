package com.mosque.prayerclock.domain

import com.mosque.prayerclock.server.ContentDto
import com.mosque.prayerclock.server.SlideDto

/** Layar yang tampil, urut dari prioritas tertinggi (App Flow §3.1). */
enum class TvScreen { JUMAT, SHALAT, IQAMAH_COUNTDOWN, ADZAN_COUNTDOWN, MUTIARA_HADITS, UTAMA }

data class ScreenInputs(
    val jumatActive: Boolean,
    val prayerScreenActive: Boolean,
    val iqamahCountdownActive: Boolean,
    val adzanCountdownActive: Boolean,
    /** Jadwal Mutiara Hadits jatuh tempo dan fitur aktif. */
    val mutiaraDue: Boolean,
    /** Detik sampai layar prioritas 1–4 berikutnya dimulai (null bila tidak diketahui). */
    val secondsUntilPriorityScreen: Long?,
    val nightHours: Boolean,
)

data class ScreenDecision(
    val screen: TvScreen,
    /** Redup mode malam hanya di atas tampilan utama (App Flow §3.2). */
    val dimForNight: Boolean,
    /** Panel slide berhenti berganti selama prioritas 1–4 aktif (App Flow §3.3). */
    val slidesPaused: Boolean,
)

/** `ScreenPriorityResolver`: dievaluasi tiap detik; prioritas tertinggi yang syaratnya terpenuhi yang tampil. */
object ScreenPriorityResolver {
    /** Mutiara Hadits tidak dimulai bila layar prioritas 1–4 akan muncul dalam 2 menit. */
    const val MUTIARA_GUARD_SECONDS = 120L

    fun resolve(i: ScreenInputs): ScreenDecision {
        val screen =
            when {
                i.jumatActive -> TvScreen.JUMAT
                i.prayerScreenActive -> TvScreen.SHALAT
                i.iqamahCountdownActive -> TvScreen.IQAMAH_COUNTDOWN
                i.adzanCountdownActive -> TvScreen.ADZAN_COUNTDOWN
                i.mutiaraDue && !i.nightHours && (i.secondsUntilPriorityScreen == null || i.secondsUntilPriorityScreen > MUTIARA_GUARD_SECONDS) ->
                    TvScreen.MUTIARA_HADITS
                else -> TvScreen.UTAMA
            }
        val priority = screen.ordinal <= TvScreen.ADZAN_COUNTDOWN.ordinal
        return ScreenDecision(screen = screen, dimForNight = screen == TvScreen.UTAMA && i.nightHours, slidesPaused = priority)
    }
}

/** Periode ilustrasi (App Flow §3.4). Semua nilai dalam menit sejak tengah malam. */
enum class SkyPeriod { FAJAR, SIANG, SENJA, MALAM }

object Periods {
    fun at(
        now: Int,
        subuh: Int,
        terbit: Int,
        maghrib: Int,
        isya: Int,
    ): SkyPeriod =
        when {
            now in subuh until terbit -> SkyPeriod.FAJAR
            now in terbit until (maghrib - 30) -> SkyPeriod.SIANG
            now in (maghrib - 30) until isya -> SkyPeriod.SENJA
            else -> SkyPeriod.MALAM
        }

    /** "HH:MM" → menit sejak tengah malam, atau null bila format salah. */
    fun minutes(hhmm: String?): Int? {
        val p = hhmm?.split(":") ?: return null
        val h = p.getOrNull(0)?.toIntOrNull() ?: return null
        val m = p.getOrNull(1)?.take(2)?.toIntOrNull() ?: return null
        return if (h in 0..23 && m in 0..59) h * 60 + m else null
    }
}

/**
 * Fase bulan dari tanggal Hijriah (1–30): 0 = bulan baru, 0,5 = purnama (tanggal 14–15), 1 = kembali baru.
 * Bulan Hijriah mengikuti siklus sinodik ±29,53 hari, jadi tanggal 1 ≈ hilal, 7 ≈ separuh, 14 ≈ purnama.
 */
object MoonPhase {
    private const val SYNODIC = 29.53

    /**
     * Bulan Hijriah dimulai saat hilal terlihat, ±1 hari setelah bulan baru, sehingga umur bulan pada
     * pertengahan tanggal Hijriah ≈ tanggal + 0,5 hari.
     */
    fun fraction(hijriDay: Int): Double = ((hijriDay.coerceIn(1, 30) + 0.5) / SYNODIC).coerceIn(0.0, 1.0)

    /** Bagian bulan yang terang, 0..1. */
    fun illumination(hijriDay: Int): Double = (1 - kotlin.math.cos(2 * Math.PI * fraction(hijriDay))) / 2

    /** Separuh pertama bulan: sisi kanan yang terang (bulan sabit menuju purnama). */
    fun waxing(hijriDay: Int): Boolean = fraction(hijriDay) < 0.5
}

/** Satu item di daftar putar panel Galeri. */
data class PlaylistItem(val content: ContentDto, val durationSec: Int, val auto: Boolean)

/**
 * Daftar putar panel slide (App Flow §3.3).
 * @param dayIndex 0 = Senin … 6 = Minggu; @param minutes menit sejak tengah malam.
 */
object SlidePlaylist {
    const val DEFAULT_SECONDS = 20
    private val infoTypes = setOf("info_kas", "info_jumat", "info_agenda", "info_hari_besar", "image")

    fun matches(
        s: SlideDto,
        dayIndex: Int,
        minutes: Int,
    ): Boolean {
        val mask = s.daysMask ?: 127
        if (mask and (1 shl dayIndex) == 0) return false
        val from = Periods.minutes(s.timeFrom)
        val to = Periods.minutes(s.timeTo)
        if (from == null || to == null) return true
        // Rentang boleh melewati tengah malam, mis. 21:00–04:00.
        return if (from <= to) minutes in from until to else minutes >= from || minutes < to
    }

    /** Kamis mulai 15:00 sampai Jumat 12:00 (indeks 3 = Kamis, 4 = Jumat). */
    fun isJumatWindow(
        dayIndex: Int,
        minutes: Int,
    ): Boolean = (dayIndex == 3 && minutes >= 15 * 60) || (dayIndex == 4 && minutes < 12 * 60)

    fun build(
        slides: List<SlideDto>,
        content: List<ContentDto>,
        dayIndex: Int,
        minutes: Int,
        autoHadiths: List<ContentDto>,
        autoSeconds: Int = DEFAULT_SECONDS,
    ): List<PlaylistItem> {
        val byId = content.associateBy { it.id }
        val scheduled =
            slides
                .filter { matches(it, dayIndex, minutes) }
                .mapNotNull { s -> byId[s.contentId]?.let { PlaylistItem(it, (s.durationSec ?: DEFAULT_SECONDS).coerceIn(10, 60), auto = false) } }
                .toMutableList()
        if (isJumatWindow(dayIndex, minutes)) {
            // Petugas Jumat didahulukan menjelang Jumat.
            val (jumat, rest) = scheduled.partition { it.content.type == "info_jumat" }
            scheduled.clear()
            scheduled.addAll(jumat + rest)
        }
        val slideIds = scheduled.map { it.content.id }.toSet()
        val autos = autoHadiths.filter { it.id !in slideIds }.map { PlaylistItem(it, autoSeconds.coerceIn(10, 60), auto = true) }
        if (scheduled.isEmpty()) return autos
        if (autos.isEmpty()) return scheduled

        // Hadits otomatis disisipkan setiap 2 slide.
        val out = mutableListOf<PlaylistItem>()
        var a = 0
        scheduled.forEachIndexed { i, item ->
            out += item
            // Hadits disisipkan tiap 2 slide; bila slide hanya satu, tiap slide (agar panel tetap berganti).
            val every = if (scheduled.size >= 2) 2 else 1
            if ((i + 1) % every == 0) out += autos[a++ % autos.size]
        }
        return out
    }

    fun isInfo(c: ContentDto) = c.type in infoTypes
}

/** Banner pengumuman segera (App Flow §3.2, F-TV-11). */
object AnnouncementRules {
    /** Lebih dari satu pengumuman aktif ditampilkan bergantian tiap 10 detik. */
    const val ROTATE_MS = 10_000L

    /** Banner tampil di atas prioritas 4–6, tidak saat shalat, hitung mundur iqamah, dan khutbah Jumat. */
    fun visibleOn(screen: TvScreen): Boolean =
        screen == TvScreen.ADZAN_COUNTDOWN || screen == TvScreen.MUTIARA_HADITS || screen == TvScreen.UTAMA

    /** Pengumuman yang sedang dalam masa tayang menurut jam TV (tetap berlaku saat offline). */
    fun active(
        list: List<com.mosque.prayerclock.server.AnnouncementDto>,
        nowMs: Long,
    ): List<com.mosque.prayerclock.server.AnnouncementDto> =
        list.filter { a ->
            val text = a.text?.trim().orEmpty()
            val start = parseMs(a.startsAt) ?: Long.MIN_VALUE
            val end = parseMs(a.expiresAt) ?: return@filter false
            text.isNotEmpty() && nowMs >= start && nowMs < end
        }

    /** Pengumuman yang tampil sekarang, bergiliran tiap [ROTATE_MS]. */
    fun current(
        active: List<com.mosque.prayerclock.server.AnnouncementDto>,
        nowMs: Long,
    ): com.mosque.prayerclock.server.AnnouncementDto? =
        if (active.isEmpty()) null else active[((nowMs / ROTATE_MS) % active.size).toInt()]

    private fun parseMs(iso: String?): Long? =
        iso?.let { runCatching { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
}
