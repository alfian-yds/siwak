package com.mosque.prayerclock.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.data.model.WeatherInfo
import com.mosque.prayerclock.ui.AnimatedLocalizedText
import com.mosque.prayerclock.ui.localizedStringResource
import kotlin.math.min

@Composable
fun WeatherCard(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxSize() // Remove fixed height to allow auto-scaling
                .padding(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(3.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(9.dp),
        ) {
            // Use BoxWithConstraints for dynamic sizing
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val density = LocalDensity.current
                val availableHeightPx = with(density) { maxHeight.toPx() }
                val availableWidthPx = with(density) { maxWidth.toPx() }

                // Calculate dynamic sizes based on available space
                val calculatedTemperatureFontSize =
                    with(density) {
                        // SIWAK: ukuran berbasis LEBAR agar STABIL walau kotak dipipihkan
                        // (dulu berbasis tinggi -> ikut mengecil saat kotak dibuat pipih).
                        // Guard tinggi (0.55H) hanya membatasi bila kotak jadi sangat pendek (anti-terpotong).
                        val widthBasedSize = (availableWidthPx / 10f)
                        val heightGuard = (availableHeightPx * 0.55f)
                        min(widthBasedSize, heightGuard).toSp()
                    }

                // Details font is 50% of temperature size
                val calculatedDetailsFontSize = calculatedTemperatureFontSize * 0.50f

                // Icon size is proportional to temperature font
                val calculatedIconSize =
                    with(density) {
                        (calculatedTemperatureFontSize.toPx() * 1.3f).toDp()
                    }

                // Detail icon size is proportional to details font
                val calculatedDetailIconSize =
                    with(density) {
                        (calculatedDetailsFontSize.toPx() * 1.1f).toDp()
                    }

                val cardPadding = 6.dp // Balanced padding for proper spacing

                LaunchedEffect(weatherInfo.icon, weatherInfo.temperature) {
                    Log.d("WeatherCard", "Weather: ${weatherInfo.temperature}°C icon='${weatherInfo.icon}'")
                }
                // SIWAK: suhu di kiri, 4 detail dalam grid 2x2 di kanan
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(cardPadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // KIRI: ikon + suhu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // SIWAK: ikon lokal (vector) berdasarkan kode cuaca — semua jenis pasti tampil, jalan offline
                        val wx = weatherIconFor(weatherInfo.icon)
                        Icon(
                            imageVector = wx.vector,
                            contentDescription = weatherInfo.description,
                            modifier = Modifier.size(calculatedIconSize),
                            tint = wx.tint,
                        )
                        Text(
                            text = "${weatherInfo.temperature.toInt()}°C",
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = calculatedTemperatureFontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    // KANAN: 4 detail dalam grid 2x2
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            WeatherDetailRow(
                                icon = Icons.Filled.Thermostat,
                                value = "${weatherInfo.feelsLike.toInt()}°",
                                fontSize = calculatedDetailsFontSize,
                                iconSize = calculatedDetailIconSize,
                            )
                            WeatherDetailRow(
                                icon = Icons.Filled.WaterDrop,
                                value = "${weatherInfo.humidity}%",
                                fontSize = calculatedDetailsFontSize,
                                iconSize = calculatedDetailIconSize,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            WeatherDetailRow(
                                icon = Icons.Filled.Air,
                                value = "${weatherInfo.windSpeed?.toInt() ?: 0} km/h",
                                fontSize = calculatedDetailsFontSize,
                                iconSize = calculatedDetailIconSize,
                            )
                            WeatherDetailRow(
                                icon = Icons.Filled.WbSunny,
                                value = "UV ${weatherInfo.uvIndex?.toInt() ?: 0}",
                                fontSize = calculatedDetailsFontSize,
                                iconSize = calculatedDetailIconSize,
                            )
                        }
                    }
                }
            }
        }
    }
}

// SIWAK: ikon cuaca lokal (vector) + warna, dipetakan dari kode cuaca.
internal data class WxIcon(val vector: ImageVector, val tint: Color)

/**
 * Petakan kode cuaca ke ikon lokal agar SEMUA jenis cuaca tampil & jalan offline.
 * Menerima kode OpenWeatherMap (mis. "10d") maupun URL yang memuat kode tsb
 * (mis. ".../img/wn/10d@2x.png"). Fallback ke ikon awan bila tak dikenali.
 */
internal fun weatherIconFor(rawIcon: String): WxIcon {
    // Ekstrak kode OWM 2-digit + d/n dari URL/string (mis. "10d"); bila tak ada, pakai apa adanya
    val code = Regex("([0-9]{2}[dn])").find(rawIcon)?.value ?: rawIcon
    val base = code.take(2)
    val isNight = code.endsWith("n")
    return when (base) {
        "01" -> // cerah
            if (isNight) WxIcon(Icons.Filled.NightsStay, Color(0xFFB0BEC5))
            else WxIcon(Icons.Filled.WbSunny, Color(0xFFFFC107))
        "02" -> // sedikit berawan
            if (isNight) WxIcon(Icons.Filled.NightsStay, Color(0xFF90A4AE))
            else WxIcon(Icons.Filled.FilterDrama, Color(0xFFFFD54F))
        "03" -> WxIcon(Icons.Filled.Cloud, Color(0xFFB0BEC5)) // berawan
        "04" -> WxIcon(Icons.Filled.Cloud, Color(0xFF90A4AE)) // mendung
        "09" -> WxIcon(Icons.Filled.Grain, Color(0xFF4FC3F7)) // gerimis/hujan lokal
        "10" -> WxIcon(Icons.Filled.Grain, Color(0xFF29B6F6)) // hujan
        "11" -> WxIcon(Icons.Filled.FlashOn, Color(0xFF9575CD)) // badai petir
        "13" -> WxIcon(Icons.Filled.AcUnit, Color(0xFF81D4FA)) // salju
        "50" -> WxIcon(Icons.Filled.BlurOn, Color(0xFFB0BEC5)) // kabut
        else -> WxIcon(Icons.Filled.Cloud, Color(0xFF90A4AE))
    }
}

@Composable
private fun WeatherDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium, // Added medium weight for better readability
                ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), // Increased alpha from 0.7f to 0.8f
            maxLines = 1,
            softWrap = false, // SIWAK: cegah "UV 0" pecah jadi vertikal (nabrak)
        )
    }
}
