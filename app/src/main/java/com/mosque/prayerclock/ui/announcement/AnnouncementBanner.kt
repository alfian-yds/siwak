package com.mosque.prayerclock.ui.announcement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.server.AnnouncementDto
import com.mosque.prayerclock.ui.gallery.screenPct

/** Warna `pengumuman` (Design Brief §3): #A83A2C, opasitas 92%. */
private val BannerBg = Color(0xEBA83A2C)

/**
 * Banner pengumuman segera (Design Brief §6.4): lebar penuh di dalam area aman, menempel di atas,
 * label "PENGUMUMAN" di kiri, teks maks. 2 baris. Masuk geser turun 300 ms, keluar pudar 300 ms.
 */
@Composable
fun AnnouncementBanner(
    announcement: AnnouncementDto?,
    modifier: Modifier = Modifier,
) {
    // Teks terakhir tetap tampil selama banner memudar keluar.
    var last by remember { mutableStateOf(announcement) }
    if (announcement != null) last = announcement
    AnimatedVisibility(
        visible = announcement != null,
        modifier = modifier,
        enter = slideInVertically(tween(300)) { -it },
        exit = fadeOut(tween(300)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp) // area aman TV
                .clip(RoundedCornerShape(14.dp))
                .background(BannerBg)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "PENGUMUMAN",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = screenPct(0.026f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
            )
            Crossfade(targetState = last, animationSpec = tween(300), label = "pengumuman") { a ->
                val text = a?.text.orEmpty()
                val size = bannerTextSize(text.length)
                Text(
                    text,
                    color = Color.White,
                    fontSize = screenPct(size),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = screenPct(size * 1.24f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Ukuran huruf (bagian tinggi layar) agar pengumuman sampai 200 karakter muat dalam 2 baris:
 * teks pendek tetap besar, teks panjang mengecil bertahap. Elipsis hanya sebagai cadangan.
 */
internal fun bannerTextSize(length: Int): Float =
    when {
        length <= 100 -> 0.042f
        length <= 140 -> 0.034f
        length <= 170 -> 0.030f
        else -> 0.026f
    }
