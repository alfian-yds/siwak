package com.mosque.prayerclock.ui.announcement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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

/** Pengumuman di bawah tidak transparan agar teks berjalan di bawahnya tidak terlihat. */
private val BannerSolid = Color(0xFFA83A2C)

/**
 * Banner pengumuman segera (Design Brief §6.4): lebar penuh di dalam area aman, menempel di bawah
 * (menutupi teks berjalan),
 * label "PENGUMUMAN" di kiri, teks maks. 2 baris. Masuk geser naik 300 ms, keluar pudar 300 ms.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
        enter = slideInVertically(tween(300)) { it },
        exit = fadeOut(tween(300)),
    ) {
        // rc.3: setinggi dan seposisi bar teks berjalan (menutupinya), satu baris yang bergulir bila panjang.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BannerSolid)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "PENGUMUMAN",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
            )
            Crossfade(targetState = last, animationSpec = tween(300), label = "pengumuman", modifier = Modifier.weight(1f)) { a ->
                Text(
                    a?.text.orEmpty(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
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
