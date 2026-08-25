package com.mosque.prayerclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
 * SIWAK — Layar "Telah Masuk Waktu Shalat".
 * Menggantikan layar Dua for Joining Saff. Menampilkan imbauan silent HP + jam berjalan,
 * tanpa teks doa Arab. Tampil selama durasi yang diatur (duaDisplayDurationMinutes).
 */
@Composable
fun DuaForJoiningSaff(
    modifier: Modifier = Modifier,
    backgroundUri: String = "",
) {
    // Jam berjalan
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(1000)
        }
    }
    val t = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val clockText =
        "%02d:%02d:%02d".format(t.hour, t.minute, t.second)

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
        // SIWAK: background kustom layar shalat (jika diunggah)
        if (backgroundUri.isNotEmpty()) {
            coil.compose.AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alpha = 0.5f,
            )
        }
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
                Text(
                    text = "🕌",
                    fontSize = (h.value * 0.10f).coerceIn(48f, 120f).sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Judul: "Telah Masuk Waktu Shalat"
                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_title),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = (h.value * 0.07f).coerceIn(28f, 84f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = ColorPrimaryAccent,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Jam berjalan
                Text(
                    text = clockText,
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = (h.value * 0.16f).coerceIn(56f, 200f).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Imbauan silent HP
                Text(
                    text = localizedStringResource(R.string.silent_your_phone),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = (h.value * 0.06f).coerceIn(24f, 64f).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = localizedStringResource(R.string.dua_joining_saff_meaning),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = (h.value * 0.04f).coerceIn(18f, 44f).sp,
                        ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
