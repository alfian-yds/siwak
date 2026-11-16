package com.mosque.prayerclock.ui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mosque.prayerclock.domain.MoonPhase
import com.mosque.prayerclock.domain.SkyPeriod
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Palet langit per periode (Design Brief §7). */
private data class Sky(val top: Color, val mid: Color, val bottom: Color, val silhouette: Color)

private val SKIES =
    mapOf(
        SkyPeriod.FAJAR to Sky(Color(0xFF1B2A55), Color(0xFFA9587D), Color(0xFFF2B47C), Color(0xFF130F24)),
        SkyPeriod.SIANG to Sky(Color(0xFF2F6F9F), Color(0xFF8FC0DB), Color(0xFFEFE1BD), Color(0xFF123A2B)),
        SkyPeriod.SENJA to Sky(Color(0xFF2B1F4F), Color(0xFFB9566C), Color(0xFFF0A75E), Color(0xFF1A0F1D)),
        SkyPeriod.MALAM to Sky(Color(0xFF06162A), Color(0xFF0D3550), Color(0xFF124A3C), Color(0xFF04100B)),
    )

private val WINDOW_LIGHT = Color(0xFFF6C66B)

/**
 * Ilustrasi vektor yang digambar kode (Design Brief §7). Tidak memakai foto pihak lain dan tidak
 * menampilkan makhluk bernyawa. Koordinat dirancang di bidang 400×225 lalu diskalakan.
 *
 * @param dayProgress posisi matahari 0 (terbit) … 1 (terbenam), dipakai periode Siang/Senja.
 * @param hijriDay tanggal Hijriah (1–30) untuk bentuk bulan di periode Malam.
 * @param silhouette KUBAH | TUMPANG | MUSHOLLA.
 */
@Composable
fun IllustrationRenderer(
    period: SkyPeriod,
    dayProgress: Float,
    hijriDay: Int,
    silhouette: String,
    modifier: Modifier = Modifier,
) {
    val stars = remember { List(70) { Random(it * 7919 + 17).let { r -> Offset(r.nextFloat() * 400f, r.nextFloat() * 120f) to (0.5f + r.nextFloat()) } } }
    Canvas(modifier) {
        val sky = SKIES.getValue(period)
        drawRect(Brush.verticalGradient(0f to sky.top, 0.55f to sky.mid, 1f to sky.bottom))

        // Lapisan digambar di bidang 400×225, "cover" agar penuh tanpa terdistorsi.
        val scale = maxOf(size.width / 400f, size.height / 225f)
        val dx = (size.width - 400f * scale) / 2f
        val dy = (size.height - 225f * scale) / 2f
        withTransform({
            translate(dx, dy)
            scale(scale, scale, Offset.Zero)
        }) {
            if (period == SkyPeriod.FAJAR || period == SkyPeriod.MALAM) {
                val alpha = if (period == SkyPeriod.FAJAR) 0.35f else 0.8f
                stars.forEach { (p, r) -> drawCircle(Color.White.copy(alpha = alpha * (r / 1.5f)), radius = r * 0.7f, center = p) }
            }
            when (period) {
                SkyPeriod.SIANG, SkyPeriod.SENJA -> drawSun(dayProgress.coerceIn(0f, 1f), period == SkyPeriod.SENJA)
                SkyPeriod.MALAM -> drawMoon(Offset(318f, 52f), 15f, hijriDay)
                SkyPeriod.FAJAR -> drawCircle(Color(0xFFFFE2B0).copy(alpha = 0.25f), radius = 60f, center = Offset(330f, 230f))
            }
            drawKhatamPattern(sky.top)
            val lit = period == SkyPeriod.SENJA || period == SkyPeriod.MALAM
            when (silhouette) {
                "TUMPANG" -> drawTumpang(sky.silhouette, lit)
                "MUSHOLLA" -> drawMusholla(sky.silhouette, lit)
                else -> drawKubah(sky.silhouette, lit)
            }
        }
    }
}

private fun DrawScope.drawSun(
    progress: Float,
    dusk: Boolean,
) {
    // Busur dari timur (kiri) ke barat (kanan); senja rendah dan lebih jingga.
    val x = 40f + progress * 320f
    val y = 160f - sin(PI * progress).toFloat() * 120f
    val core = if (dusk) Color(0xFFFFC27A) else Color(0xFFFFF6D8)
    drawCircle(core.copy(alpha = 0.30f), radius = 28f, center = Offset(x, y))
    drawCircle(core, radius = 15f, center = Offset(x, y))
}

/** Bulan dengan bagian terang sesuai fase: separuh pertama terang di kanan (membesar). */
private fun DrawScope.drawMoon(
    c: Offset,
    r: Float,
    hijriDay: Int,
) {
    val illum = MoonPhase.illumination(hijriDay).toFloat()
    val waxing = MoonPhase.waxing(hijriDay)
    drawCircle(Color(0xFFFFF3C9).copy(alpha = 0.10f), radius = r * 2.2f, center = c)
    drawCircle(Color(0xFF8A93A6).copy(alpha = 0.25f), radius = r, center = c) // sisi gelap samar
    if (illum < 0.01f) return
    val disk = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    val rx = r * abs(1f - 2f * illum)
    val ellipse = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
    val gibbous = illum > 0.5f
    val path =
        Path().apply {
            moveTo(c.x, c.y - r)
            if (waxing) {
                arcTo(disk, -90f, 180f, false) // tepi kanan, atas → bawah
                arcTo(ellipse, 90f, if (gibbous) 180f else -180f, false) // garis terminator kembali ke atas
            } else {
                arcTo(disk, -90f, -180f, false) // tepi kiri
                arcTo(ellipse, 90f, if (gibbous) -180f else 180f, false)
            }
            close()
        }
    drawPath(path, Color(0xFFFFF6D8))
}

/** Motif bintang delapan (khatam) tipis, opasitas ±7%. */
private fun DrawScope.drawKhatamPattern(base: Color) {
    val color = Color.White.copy(alpha = 0.07f)
    val step = 50f
    var y = 18f
    var row = 0
    while (y < 140f) {
        var x = if (row % 2 == 0) 20f else 45f
        while (x < 420f) {
            val s = 9f
            drawRect(color, topLeft = Offset(x - s, y - s), size = Size(2 * s, 2 * s), style = Stroke(0.8f))
            withTransform({ rotate(45f, Offset(x, y)) }) {
                drawRect(color, topLeft = Offset(x - s, y - s), size = Size(2 * s, 2 * s), style = Stroke(0.8f))
            }
            x += step
        }
        y += 40f
        row++
    }
}

private fun DrawScope.windows(
    lit: Boolean,
    xs: List<Float>,
    y: Float,
    w: Float,
    h: Float,
) {
    if (!lit) return
    xs.forEach { x ->
        val p =
            Path().apply {
                moveTo(x, y + h)
                lineTo(x, y + w / 2)
                arcTo(Rect(x, y, x + w, y + w), 180f, 180f, false)
                lineTo(x + w, y + h)
                close()
            }
        drawPath(p, WINDOW_LIGHT.copy(alpha = 0.85f))
    }
}

/** Masjid kubah dengan dua menara (bawaan). */
private fun DrawScope.drawKubah(
    c: Color,
    lit: Boolean,
) {
    drawRect(c, Offset(0f, 205f), Size(400f, 20f)) // tanah
    // Menara kiri & kanan
    for (mx in listOf(118f, 270f)) {
        drawRect(c, Offset(mx, 95f), Size(12f, 112f))
        drawRect(c, Offset(mx - 3f, 120f), Size(18f, 4f))
        drawPath(Path().apply { moveTo(mx - 1f, 95f); quadraticBezierTo(mx + 6f, 72f, mx + 13f, 95f); close() }, c)
        drawRect(c, Offset(mx + 5.5f, 64f), Size(1f, 10f))
    }
    // Badan & kubah
    drawRect(c, Offset(140f, 150f), Size(120f, 57f))
    drawRect(c, Offset(160f, 128f), Size(80f, 24f))
    drawPath(Path().apply { moveTo(158f, 130f); cubicTo(158f, 78f, 242f, 78f, 242f, 130f); close() }, c)
    drawRect(c, Offset(199.3f, 78f), Size(1.4f, 14f))
    drawCircle(c, radius = 4f, center = Offset(200f, 76f))
    // Serambi rendah
    drawRect(c, Offset(70f, 175f), Size(70f, 32f))
    drawRect(c, Offset(260f, 175f), Size(70f, 32f))
    windows(lit, listOf(152f, 172f, 216f, 236f), 163f, 10f, 22f)
    windows(lit, listOf(186f), 158f, 28f, 49f)
    windows(lit, listOf(84f, 108f, 276f, 300f), 184f, 8f, 14f)
}

/** Masjid beratap tumpang tiga khas Jawa. */
private fun DrawScope.drawTumpang(
    c: Color,
    lit: Boolean,
) {
    drawRect(c, Offset(0f, 205f), Size(400f, 20f))
    fun roof(
        y: Float,
        halfTop: Float,
        halfBottom: Float,
        h: Float,
    ) = drawPath(
        Path().apply {
            moveTo(200f - halfTop, y)
            lineTo(200f + halfTop, y)
            lineTo(200f + halfBottom, y + h)
            lineTo(200f - halfBottom, y + h)
            close()
        },
        c,
    )
    roof(150f, 70f, 120f, 22f)
    drawRect(c, Offset(140f, 166f), Size(120f, 6f))
    roof(118f, 40f, 78f, 22f)
    drawRect(c, Offset(160f, 134f), Size(80f, 18f))
    roof(90f, 12f, 46f, 22f)
    drawRect(c, Offset(176f, 106f), Size(48f, 14f))
    // Mustaka di puncak
    drawPath(Path().apply { moveTo(193f, 90f); quadraticBezierTo(200f, 66f, 207f, 90f); close() }, c)
    drawRect(c, Offset(98f, 172f), Size(204f, 35f))
    windows(lit, listOf(118f, 142f, 166f, 222f, 246f, 270f), 180f, 10f, 20f)
    windows(lit, listOf(190f), 178f, 20f, 29f)
}

/** Musholla sederhana dengan atap pelana dan kubah kecil. */
private fun DrawScope.drawMusholla(
    c: Color,
    lit: Boolean,
) {
    drawRect(c, Offset(0f, 205f), Size(400f, 20f))
    drawRect(c, Offset(140f, 160f), Size(120f, 47f))
    drawPath(Path().apply { moveTo(128f, 162f); lineTo(200f, 128f); lineTo(272f, 162f); close() }, c)
    drawPath(Path().apply { moveTo(188f, 134f); cubicTo(188f, 112f, 212f, 112f, 212f, 134f); close() }, c)
    drawRect(c, Offset(199.4f, 104f), Size(1.2f, 10f))
    drawCircle(c, radius = 2.5f, center = Offset(200f, 103f))
    windows(lit, listOf(156f, 228f), 172f, 12f, 22f)
    windows(lit, listOf(190f), 170f, 20f, 37f)
}

/** Posisi matahari 0..1 di antara terbit dan terbenam. */
fun sunProgress(
    nowMinutes: Int,
    sunrise: Int,
    sunset: Int,
): Float = if (sunset <= sunrise) 0.5f else ((nowMinutes - sunrise).toFloat() / (sunset - sunrise)).coerceIn(0f, 1f)
