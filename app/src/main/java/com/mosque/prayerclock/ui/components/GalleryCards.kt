package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(5.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(15.dp),
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
    GalleryBox(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            val d = LocalDensity.current
            val h = with(d) { maxHeight.toPx() }
            val w = with(d) { maxWidth.toPx() }
            val timeSize = with(d) { min(h * 0.33f, w / (time.length * 0.62f)).toSp() }
            val labelSize = with(d) { min(h * 0.12f, w / 15f).toSp() }
            val nameSize = with(d) { min(h * 0.15f, w / (title.length * 0.6f + 1)).toSp() }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Berikutnya", fontSize = labelSize, lineHeight = labelSize * 1.1f, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
                Text(title, fontSize = nameSize, lineHeight = nameSize * 1.1f, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, textAlign = TextAlign.Center)
                Text(time, fontSize = timeSize, lineHeight = timeSize * 1.05f, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                if (remaining != null) {
                    Text(remaining, fontSize = labelSize, lineHeight = labelSize * 1.1f, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), maxLines = 1)
                }
            }
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
