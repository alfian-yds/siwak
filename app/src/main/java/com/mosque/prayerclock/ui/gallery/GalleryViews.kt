package com.mosque.prayerclock.ui.gallery

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxWidth as fw
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.mosque.prayerclock.R
import com.mosque.prayerclock.domain.PlaylistItem
import com.mosque.prayerclock.domain.SkyPeriod
import com.mosque.prayerclock.server.ContentDto
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

/** Teks Arab memakai Amiri yang dibundel (Design Brief §4). */
val Amiri = FontFamily(Font(R.font.amiri_regular, FontWeight.Normal), Font(R.font.amiri_bold, FontWeight.Bold))

/** Ukuran huruf dalam persen tinggi layar (Design Brief §5), sama di semua resolusi. */
@Composable
fun screenPct(p: Float): TextUnit {
    val h = LocalConfiguration.current.screenHeightDp
    return with(LocalDensity.current) { (h * p * LocalTextScale.current).dp.toSp() }
}

/**
 * Pembesar tulisan konten. Panel di samping jam memakai 1; slide layar penuh memakai [FULLSCREEN_TEXT_SCALE]
 * agar hadits dan info terbaca dari belakang ruangan.
 */
val LocalTextScale = androidx.compose.runtime.staticCompositionLocalOf { 1f }
const val FULLSCREEN_TEXT_SCALE = 1.8f

/** Lapisan latar untuk semua slide dan Mutiara Hadits. */
data class SceneState(val period: SkyPeriod, val dayProgress: Float, val hijriDay: Int, val silhouette: String)

/** Warna aksen mengikuti tema warna yang dipilih (sama dengan jam di layar utama). */
private val Accent: Color
    @Composable
    @androidx.compose.runtime.ReadOnlyComposable
    get() = com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
private val GlassBg = Color(0xCC06130D)
private val CATEGORY_LABEL =
    mapOf(
        "adab_masjid" to "Adab masuk masjid",
        "shaf_berjamaah" to "Shaf & berjamaah",
        "keutamaan" to "Keutamaan",
        "kebersihan" to "Kebersihan & adab",
        "larangan_masjid" to "Larangan di masjid",
        "jumat" to "Shalat Jumat",
        "dzikir_doa" to "Dzikir & doa",
    )

// ---- Panel slide Galeri (Design Brief §6.2, App Flow §3.3) -----------------------------

@Composable
fun GalleryPanel(
    playlist: List<PlaylistItem>,
    paused: Boolean,
    scene: SceneState,
    mosqueName: String,
    mediaUri: (String?) -> String?,
    hijriToday: Triple<Int, Int, Int>?,
    modifier: Modifier = Modifier,
) {
    var index by remember(playlist) { mutableIntStateOf(0) }
    val item = playlist.getOrNull(index % playlist.size.coerceAtLeast(1))
    LaunchedEffect(index, paused, playlist) {
        if (!paused && item != null && playlist.size > 1) {
            delay(item.durationSec * 1000L)
            index = (index + 1) % playlist.size
        }
    }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
        Crossfade(targetState = item, animationSpec = tween(800), label = "slide") { current ->
            Box(Modifier.fillMaxSize()) {
                when {
                    current == null -> EmptySlide(scene, mosqueName)
                    current.content.type == "hadith" -> HadithSlide(current.content, scene)
                    current.content.type == "image" -> ImageSlide(current.content, mediaUri, scene)
                    else -> InfoSlide(current.content, scene, hijriToday)
                }
            }
        }
        if (playlist.size > 1) {
            Row(
                Modifier.align(Alignment.BottomEnd).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                playlist.indices.take(12).forEach { i ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (i == index % playlist.size) Accent else Color.White.copy(alpha = 0.35f)))
                }
            }
        }
    }
}

@Composable
private fun SceneBackground(scene: SceneState) {
    IllustrationRenderer(scene.period, scene.dayProgress, scene.hijriDay, scene.silhouette, Modifier.fillMaxSize())
}

/** Gradien gelap 0% → 82% di bawah teks (kontras Design Brief §5). */
@Composable
private fun BoxScope.BottomShade() {
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f))))
}

@Composable
private fun EmptySlide(
    scene: SceneState,
    mosqueName: String,
) {
    Box(Modifier.fillMaxSize()) {
        SceneBackground(scene)
        BottomShade()
        Text(
            mosqueName,
            Modifier.align(Alignment.BottomStart).padding(24.dp),
            color = Color.White,
            fontSize = screenPct(0.05f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun CategoryChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 14.dp, vertical = 6.dp),
        color = Accent,
        fontSize = screenPct(0.022f),
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Isi hadits: teks Arab maks. 2 baris, terjemah maks. 4 baris, sumber (Design Brief §5). */
@Composable
fun HadithText(
    c: ContentDto,
    align: TextAlign = TextAlign.Start,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!c.arabicText.isNullOrBlank()) {
            Text(
                c.arabicText,
                Modifier.fw(),
                color = Color.White,
                fontFamily = Amiri,
                fontSize = screenPct(0.045f),
                lineHeight = screenPct(0.075f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (align == TextAlign.Center) TextAlign.Center else TextAlign.End,
                style = TextStyle(textDirection = TextDirection.Rtl),
            )
        }
        Text(
            "“${c.translation.orEmpty()}”",
            Modifier.fw(),
            color = Color.White,
            fontSize = screenPct(0.033f),
            lineHeight = screenPct(0.045f),
            // Layar penuh tanpa teks Arab: terjemah boleh 6 baris; dengan teks Arab tetap 4 agar kartu tidak menabrak jam.
            maxLines = if (LocalTextScale.current > 1f && c.arabicText.isNullOrBlank()) 6 else 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
        )
        Text(c.sourceRef.orEmpty(), Modifier.fw(), color = Accent, fontSize = screenPct(0.022f), textAlign = align, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HadithSlide(
    c: ContentDto,
    scene: SceneState,
) {
    Box(Modifier.fillMaxSize()) {
        SceneBackground(scene)
        BottomShade()
        CategoryChip(CATEGORY_LABEL[c.category] ?: "Hadits", Modifier.align(Alignment.TopStart).padding(18.dp))
        Box(Modifier.align(Alignment.BottomStart).padding(start = 28.dp, end = 28.dp, bottom = 38.dp)) { HadithText(c) }
    }
}

@Composable
private fun ImageSlide(
    c: ContentDto,
    mediaUri: (String?) -> String?,
    scene: SceneState,
) {
    val uri = mediaUri(c.mediaId)
    Box(Modifier.fillMaxSize()) {
        if (uri == null) SceneBackground(scene) else AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        val caption = c.bodyObject?.str("caption")
        if (!caption.isNullOrBlank()) {
            BottomShade()
            Text(caption, Modifier.align(Alignment.BottomStart).padding(24.dp), color = Color.White, fontSize = screenPct(0.03f), maxLines = 2)
        }
    }
}

// ---- Panel informasi (Design Brief §6.5) -------------------------------------------------

private fun JsonObject.str(k: String): String? = get(k)?.takeIf { !it.isJsonNull }?.asString

private fun JsonObject.long(k: String): Long = get(k)?.takeIf { !it.isJsonNull }?.asLong ?: 0L

private val rupiah: NumberFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

private fun formatDate(iso: String?): String {
    val p = iso?.split("-") ?: return ""
    val months = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    return if (p.size == 3) "${p[2].toIntOrNull() ?: p[2]} ${months.getOrNull((p[1].toIntOrNull() ?: 1) - 1) ?: p[1]} ${p[0]}" else iso
}

/** Tanggal Hijriah tetap untuk tiap hari besar (bulan, tanggal). */
private val HARI_BESAR =
    mapOf(
        "TAHUN_BARU_HIJRIAH" to Triple("Tahun Baru Hijriah", 1, 1),
        "ASYURA" to Triple("Hari Asyura", 1, 10),
        "MAULID_NABI" to Triple("Maulid Nabi Muhammad ﷺ", 3, 12),
        "ISRA_MIRAJ" to Triple("Isra Mi'raj", 7, 27),
        "AWAL_RAMADAN" to Triple("Awal Ramadhan", 9, 1),
        "NUZULUL_QURAN" to Triple("Nuzulul Qur'an", 9, 17),
        "IDUL_FITRI" to Triple("Idul Fitri", 10, 1),
        "ARAFAH" to Triple("Hari Arafah", 12, 9),
        "IDUL_ADHA" to Triple("Idul Adha", 12, 10),
    )

/**
 * Perkiraan sisa hari ke tanggal Hijriah tertentu (bulan rata-rata 29,53 hari).
 * Ditampilkan sebagai perkiraan karena awal bulan Hijriah ditetapkan lewat rukyat/sidang isbat.
 */
fun daysUntilHijri(
    today: Triple<Int, Int, Int>,
    month: Int,
    day: Int,
): Int {
    val (d, m, _) = today
    val now = (m - 1) * 29.53 + d
    var target = (month - 1) * 29.53 + day
    if (target < now - 0.5) target += 354.37
    return Math.round(target - now).toInt()
}

@Composable
private fun InfoSlide(
    c: ContentDto,
    scene: SceneState,
    hijriToday: Triple<Int, Int, Int>?,
) {
    val b = c.bodyObject ?: JsonObject()
    Box(Modifier.fillMaxSize()) {
        SceneBackground(scene)
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))
        Column(
            Modifier
                .align(Alignment.Center)
                .widthIn(max = (640 * LocalTextScale.current).dp)
                .fillMaxWidth(0.84f)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassBg)
                .padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (c.type) {
                "info_kas" -> {
                    InfoTitle("Laporan kas", "Per ${formatDate(b.str("reportDate"))}")
                    val open = b.long("openingBalance")
                    val income = b.long("income")
                    val expense = b.long("expense")
                    InfoRow("Saldo awal", "Rp ${rupiah.format(open)}")
                    InfoRow("Pemasukan", "Rp ${rupiah.format(income)}")
                    InfoRow("Pengeluaran", "Rp ${rupiah.format(expense)}")
                    InfoRow("Saldo akhir", "Rp ${rupiah.format(open + income - expense)}", highlight = true)
                }
                "info_jumat" -> {
                    InfoTitle("Petugas Jumat", formatDate(b.str("date")))
                    InfoRow("Khatib", b.str("khatib").orEmpty())
                    InfoRow("Imam", b.str("imam").orEmpty())
                    b.str("muadzin")?.takeIf { it.isNotBlank() }?.let { InfoRow("Muadzin", it) }
                    b.str("theme")?.takeIf { it.isNotBlank() }?.let { InfoRow("Tema", it) }
                }
                "info_agenda" -> {
                    InfoTitle("Agenda kegiatan", "")
                    b.getAsJsonArray("items")?.take(8)?.forEach { el ->
                        val o = el.asJsonObject
                        val right = listOfNotNull(o.str("title"), o.str("speaker")?.takeIf { it.isNotBlank() }).joinToString(" · ")
                        InfoRow("${o.str("day")} ${o.str("time")}", right)
                    }
                }
                "info_teks" -> {
                    // rc.5: informasi bebas (judul + isi teks).
                    InfoTitle("Informasi", b.str("title").orEmpty())
                    Text(
                        b.str("text").orEmpty(),
                        color = Color.White,
                        fontSize = screenPct(0.03f),
                        lineHeight = screenPct(0.044f),
                        maxLines = if (LocalTextScale.current > 1f) 7 else 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                "info_hari_besar" -> {
                    val (name, month, day) = HARI_BESAR[b.str("event")] ?: Triple("Hari besar Islam", 1, 1)
                    InfoTitle("Hari besar Islam", name)
                    val days = hijriToday?.let { daysUntilHijri(it, month, day) }
                    Text(
                        when {
                            days == null -> ""
                            days <= 0 -> "Hari ini"
                            else -> "± $days hari lagi"
                        },
                        color = Accent,
                        fontSize = screenPct(0.07f),
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Perkiraan; mengikuti penetapan resmi.", color = Color.White.copy(alpha = 0.7f), fontSize = screenPct(0.022f))
                }
            }
        }
    }
}

@Composable
private fun InfoTitle(
    label: String,
    title: String,
) {
    Text(label.uppercase(), color = Accent, fontSize = screenPct(0.022f), letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
    if (title.isNotBlank()) Text(title, color = Color.White, fontSize = screenPct(0.04f), fontWeight = FontWeight.Bold)
    Spacer(Modifier.size(4.dp))
}

@Composable
private fun InfoRow(
    name: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = Color.White.copy(alpha = 0.85f), fontSize = screenPct(0.03f), modifier = Modifier.width((220 * LocalTextScale.current).dp), maxLines = 1)
        Text(
            value,
            Modifier.weight(1f),
            color = if (highlight) Accent else Color.White,
            fontSize = screenPct(0.03f),
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---- Mutiara Hadits (Design Brief §6.3) --------------------------------------------------

/** Slide atau panel informasi layar penuh untuk layar penuh berkala (rc.3), dengan jam dan waktu shalat berikutnya. */
@Composable
fun ContentFullscreen(
    item: PlaylistItem,
    scene: SceneState,
    mediaUri: (String?) -> String?,
    hijriToday: Triple<Int, Int, Int>?,
    clockText: String,
    nextPrayerText: String?,
    /** Jarak info waktu shalat dari bawah: lebih tinggi hanya bila banner pengumuman sedang tampil. */
    bottomInset: androidx.compose.ui.unit.Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalTextScale provides FULLSCREEN_TEXT_SCALE) {
            when (item.content.type) {
                "image" -> ImageSlide(item.content, mediaUri, scene)
                else -> InfoSlide(item.content, scene, hijriToday)
            }
        }
        Text(
            clockText,
            Modifier.align(Alignment.TopEnd).padding(36.dp).clip(RoundedCornerShape(50)).background(GlassBg).padding(horizontal = 18.dp, vertical = 6.dp),
            color = Accent,
            fontSize = screenPct(0.05f),
            fontWeight = FontWeight.Bold,
        )
        if (nextPrayerText != null) {
            Text(
                nextPrayerText,
                Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = bottomInset).clip(RoundedCornerShape(50)).background(GlassBg).padding(horizontal = 18.dp, vertical = 8.dp),
                color = Accent,
                fontSize = screenPct(0.03f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun MutiaraHaditsScreen(
    hadith: ContentDto,
    scene: SceneState,
    clockText: String,
    nextPrayerText: String?,
    /** Jarak info waktu shalat dari bawah: lebih tinggi hanya bila banner pengumuman sedang tampil. */
    bottomInset: androidx.compose.ui.unit.Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        SceneBackground(scene)
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.30f)))
        Text(clockText, Modifier.align(Alignment.TopEnd).padding(36.dp), color = Accent, fontSize = screenPct(0.05f), fontWeight = FontWeight.Bold)
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x8C06130D))
                .padding(horizontal = 40.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalTextScale provides FULLSCREEN_TEXT_SCALE) {
                CategoryChip("Mutiara Hadits · ${CATEGORY_LABEL[hadith.category] ?: "Hadits"}")
                HadithText(hadith, TextAlign.Center)
            }
        }
        if (nextPrayerText != null) {
            Text(
                nextPrayerText,
                Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = bottomInset).clip(RoundedCornerShape(50)).background(GlassBg).padding(horizontal = 18.dp, vertical = 8.dp),
                color = Accent,
                fontSize = screenPct(0.03f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ---- Kolom kanan Galeri: jam ringkas -------------------------------------------------------

private val HIJRI_MONTHS =
    listOf("Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir", "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban", "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah")
private val DAY_NAMES = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Ahad")
private val MONTH_NAMES = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

/**
 * Jam dan tanggal untuk kolom kanan Galeri (42% lebar). Ukuran mengikuti tinggi layar agar
 * jam, tanggal Masehi, dan tanggal Hijriah muat tanpa terpotong.
 */
@Composable
fun GalleryClock(
    hour: Int,
    minute: Int,
    second: Int,
    dayIndex: Int,
    day: Int,
    month: Int,
    year: Int,
    hijri: Triple<Int, Int, Int>?,
    showSeconds: Boolean,
    use24h: Boolean,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val h = if (use24h) hour else ((hour + 11) % 12 + 1)
    val time = buildString {
        append("%02d:%02d".format(h, minute))
        if (showSeconds) append(":%02d".format(second))
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(time, color = color, fontSize = screenPct(if (showSeconds) 0.13f else 0.17f), fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            "${DAY_NAMES.getOrElse(dayIndex) { "" }}, $day ${MONTH_NAMES.getOrElse(month - 1) { "" }} $year",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = screenPct(0.03f),
            maxLines = 1,
        )
        if (hijri != null) {
            Text(
                "${hijri.first} ${HIJRI_MONTHS.getOrElse(hijri.second - 1) { "" }} ${hijri.third} H",
                color = accent,
                fontSize = screenPct(0.03f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
