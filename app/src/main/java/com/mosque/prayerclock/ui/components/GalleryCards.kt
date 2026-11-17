package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mosque.prayerclock.data.model.WeatherInfo
import kotlin.math.min
import kotlin.math.roundToInt

/** Kotak bergaya sama dengan kotak jam (tata letak Galeri, rc.3). */
@Composable
fun GalleryBox(
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Card(
            modifier = (if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth()).padding(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp),
        ) { content() }
    }
}

/**
 * Waktu shalat berikutnya untuk Galeri: label kecil, nama waktu, jam besar, dan sisa waktu.
 * Ukuran huruf mengikuti tinggi dan lebar kotak agar tidak terpotong.
 */
@Composable
fun GalleryNextPrayerCard(
    title: String,
    time: String,
    remaining: String?,
    modifier: Modifier = Modifier,
) {
    // Ukuran teks tetap (sama dengan versi kotak tinggi); tinggi kotak mengikuti isi tanpa ruang kosong.
    GalleryBox(modifier, fill = false) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("Berikutnya", fontSize = 13.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
                Text(title, fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                if (remaining != null) {
                    Text(remaining, fontSize = 13.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), maxLines = 1)
                }
            }
            Text(time, fontSize = 46.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        }
    }
}

/** Cuaca ringkas untuk Galeri: ikon dan suhu besar, keterangan, kelembapan, dan angin. */
@Composable
fun GalleryWeatherCard(
    info: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    val wx = weatherIconFor(info.icon)
    GalleryBox(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            val d = LocalDensity.current
            val h = with(d) { maxHeight.toPx() }
            val w = with(d) { maxWidth.toPx() }
            val iconDp = with(d) { min(h * 0.36f, w * 0.30f).toDp() }
            val tempSize = with(d) { min(h * 0.32f, w * 0.20f).toSp() }
            val smallSize = with(d) { min(h * 0.12f, w / 16f).toSp() }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(wx.vector, contentDescription = null, tint = wx.tint, modifier = Modifier.size(iconDp))
                    Text("${info.temperature.roundToInt()}°C", fontSize = tempSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                }
                Text(
                    info.description.replaceFirstChar { it.uppercase() },
                    fontSize = smallSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Lembap ${info.humidity}%" + (info.windSpeed?.let { " · Angin ${it.roundToInt()} km/j" } ?: ""),
                    fontSize = smallSize,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Cuaca satu baris kecil di bawah panel Galeri: ikon, suhu, keterangan, terasa, kelembapan, angin, UV. */
@Composable
fun GalleryWeatherStrip(
    info: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    val wx = weatherIconFor(info.icon)
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val iconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    androidx.compose.foundation.layout.Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(wx.vector, contentDescription = null, tint = wx.tint, modifier = Modifier.size(20.dp))
            Text("${info.temperature.roundToInt()}°C", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            Text(info.description.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = muted, maxLines = 1, modifier = Modifier.weight(1f))
            StripItem(androidx.compose.material.icons.Icons.Filled.Thermostat, "${info.feelsLike.roundToInt()}°", iconTint, muted)
            StripItem(androidx.compose.material.icons.Icons.Filled.WaterDrop, "${info.humidity}%", iconTint, muted)
            info.windSpeed?.let { StripItem(androidx.compose.material.icons.Icons.Filled.Air, "${it.roundToInt()} km/j", iconTint, muted) }
            info.uvIndex?.let { StripItem(androidx.compose.material.icons.Icons.Filled.WbSunny, "UV ${it.roundToInt()}", iconTint, muted) }
        }
    }
}

@Composable
private fun StripItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 13.sp, color = color, maxLines = 1)
    }
}
