package com.mosque.prayerclock.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorPrimaryAccent
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * SIWAK — Layar Jumat (khutbah berlangsung).
 * Sesuai permintaan: TANPA hitung mundur. Menampilkan imbauan silent HP,
 * ajakan mendengarkan khutbah, dan JAM BERJALAN saat ini.
 * Parameter sisa waktu dipertahankan demi kompatibilitas pemanggil, namun tidak dipakai.
 */
@Composable
fun JummahInProgress(
    @Suppress("UNUSED_PARAMETER") hoursRemaining: Long,
    @Suppress("UNUSED_PARAMETER") minutesRemaining: Long,
    @Suppress("UNUSED_PARAMETER") secondsRemaining: Long,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(1000)
        }
    }
    val t = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val clockText = "%02d:%02d:%02d".format(t.hour, t.minute, t.second)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                                ),
                        ),
                ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val h = maxHeight

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.Mosque,
                    contentDescription = null,
                    tint = com.mosque.prayerclock.ui.theme.ColorPrimaryAccent,
                    modifier = Modifier.size((h.value * 0.09f).coerceIn(44f, 110f).dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Judul: Khutbah Berlangsung
                Text(
                    text = localizedStringResource(R.string.jummah_in_progress_title),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = (h.value * 0.065f).coerceIn(26f, 72f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Jam berjalan
                Text(
                    text = clockText,
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = (h.value * 0.15f).coerceIn(52f, 190f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Imbauan silent HP
                Text(
                    text = localizedStringResource(R.string.silent_your_phone),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = (h.value * 0.055f).coerceIn(22f, 56f).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Ajakan dengarkan khutbah
                Text(
                    text = localizedStringResource(R.string.jummah_prayer_reminder),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = (h.value * 0.038f).coerceIn(16f, 40f).sp,
                        ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Salawat (Arab)
                Text(
                    text = localizedStringResource(R.string.salawat_arabic),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = (h.value * 0.05f).coerceIn(22f, 52f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
