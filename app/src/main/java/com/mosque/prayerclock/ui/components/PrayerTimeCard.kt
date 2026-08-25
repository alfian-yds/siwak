package com.mosque.prayerclock.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.PrayerInfo
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.ui.LocalEffectiveLanguage
import com.mosque.prayerclock.ui.localizedStringResource
import com.mosque.prayerclock.ui.theme.ColorAzanTime
import com.mosque.prayerclock.ui.theme.ColorIqamahTime
import com.mosque.prayerclock.ui.theme.ColorNextAzanTime
import com.mosque.prayerclock.ui.theme.ColorNextIqamahTime
import com.mosque.prayerclock.ui.theme.ColorNextSunriseTime
import com.mosque.prayerclock.ui.theme.ColorSunriseTime
import com.mosque.prayerclock.utils.TimeUtils
import kotlin.math.min

/**
 * Compact Prayer Time Card Component
 *
 * Displays prayer name and time with dynamic font scaling.
 * Used in the prayer times row at the bottom of the main screen.
 *
 * Features:
 * - Dynamic font sizing based on available space
 * - Color-coded prayer times (Azan/Iqamah/Sunrise)
 * - Animated transitions between Azan and Iqamah times
 * - Highlighted border for the next prayer
 * - Shows "Azan" or "Iqamah" label for prayers with iqamah times (excludes Sunrise and Jummah)
 */
@Composable
fun PrayerTimeCard(
    prayerInfo: PrayerInfo,
    isNext: Boolean = false,
    show24Hour: Boolean = false,
    globalShowAzan: Boolean = true,
    showCycleLabel: Boolean = true, // SIWAK: tampilkan label Adzan/Iqamah hanya bila pergantian aktif
    modifier: Modifier = Modifier,
) {
    val cardPadding = 1.dp
    // Use consistent content padding for all cards to prevent size variations
    val contentPadding = 2.dp
    val cornerRadius = 8.dp

    // Use global synchronized animation state
    // All cards will show Azan/Iqamah at the same time
    val isShowingAzan = globalShowAzan || prayerInfo.iqamahTime == null

    // Get the prayer color based on current state
    // Use theme-specific "next" colors when isNext is true for better emphasis
    // SIWAK: Terbit tidak lagi beraksen oranye — samakan dengan waktu lain (warna azan)
    val prayerColor =
        if (isShowingAzan) {
            if (isNext) ColorNextAzanTime else ColorAzanTime
        } else {
            if (isNext) ColorNextIqamahTime else ColorIqamahTime
        }

    Card(
        modifier =
            modifier
                .padding(cardPadding)
                .let { cardModifier ->
                    if (isNext) {
                        cardModifier.border(
                            width = 2.dp,
                            color = prayerColor,
                            shape = RoundedCornerShape(cornerRadius),
                        )
                    } else {
                        cardModifier
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 6.dp,
            ),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isNext) {
                            prayerColor.copy(alpha = 0.05f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        },
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(cornerRadius - 2.dp),
        ) {
            // Use BoxWithConstraints to dynamically calculate font sizes based on card width
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
            ) {
                val density = LocalDensity.current

                // Get effective language and font scaling
                val effectiveLanguage = LocalEffectiveLanguage.current
                val fontScale = getLanguageFontScale(effectiveLanguage)

                // Calculate available space for this prayer card
                val availableWidthPx = with(density) { maxWidth.toPx() }
                val availableHeightPx = with(density) { maxHeight.toPx() }

                // Calculate dynamic font sizes based on available space
                // Each card gets equal space in the row, so we optimize for that
                val calculatedTimeFontSize =
                    with(density) {
                        // Time takes about 60% of card height
                        val heightBasedSize = (availableHeightPx * 0.60f).toSp()

                        // Time string is typically 7-8 chars ("12:45 AM" or "12:45")
                        val estimatedTimeChars = if (show24Hour) 5f else 8f
                        val widthBasedSize = (availableWidthPx / estimatedTimeChars * 1.8f).toSp()

                        // Use the smaller to ensure it fits
                        min(heightBasedSize.value, widthBasedSize.value).sp
                    }

                // Prayer name text scales with language
                // It should be smaller than time but maximize available space
                // Sunrise can use 2 lines since it doesn't have Azan/Iqamah label
                val calculatedNameFontSize =
                    with(density) {
                        // Name takes about 25% of card height
                        val heightBasedSize = (availableHeightPx * 0.25f * fontScale).toSp()

                        // All prayers use same character estimation
                        // Sunrise can wrap to 2 lines, so it doesn't need special handling
                        val estimatedNameChars = 10f
                        val widthMultiplier = 2.0f
                        val widthBasedSize = (availableWidthPx / estimatedNameChars * widthMultiplier * fontScale).toSp()

                        // Use the smaller to ensure it fits, but cap at a percentage of time size
                        // All prayers get same cap since Sunrise can wrap to 2 lines
                        val dynamicSize = min(heightBasedSize.value, widthBasedSize.value)
                        val maxAllowed = calculatedTimeFontSize.value * 0.80f

                        min(dynamicSize, maxAllowed).sp
                    }

                val spacingSize = 1.dp

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // SIWAK: label Adzan/Iqamah hanya untuk waktu yang PUNYA iqamah
                    // (Imsak, Terbit, Jumat tidak punya -> tanpa label). Ruang label tetap
                    // dicadangkan agar semua kartu tinggi seragam.
                    val hasIqamah = prayerInfo.iqamahTime != null
                    val calculatedLabelFontSize =
                        with(LocalDensity.current) {
                            (calculatedNameFontSize.value * 0.75f).sp
                        }

                    // Nama waktu (selalu 1 baris)
                    Text(
                        text = prayerInfo.name,
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontSize = calculatedNameFontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )

                    // Baris label (Adzan/Iqamah) — HANYA saat pergantian aktif.
                    // Bila pergantian mati (hanya Adzan), baris label tidak dirender sama sekali
                    // sehingga tak ada gap kosong.
                    if (showCycleLabel) {
                        if (hasIqamah) {
                            AnimatedContent(
                                targetState = globalShowAzan,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(800)) togetherWith
                                        fadeOut(animationSpec = tween(800))
                                },
                            ) { isShowingAzan ->
                                Text(
                                    text =
                                        if (isShowingAzan) {
                                            localizedStringResource(R.string.azan_letter)
                                        } else {
                                            localizedStringResource(R.string.iqamah_letter)
                                        },
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontSize = calculatedLabelFontSize,
                                            fontWeight = FontWeight.Normal,
                                        ),
                                    color = prayerColor.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            // Cadangkan ruang setinggi label agar kartu seragam (saat pergantian aktif)
                            Text(
                                text = " ",
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = calculatedLabelFontSize,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacingSize))

                    // Simplified time display without abbreviation text for better readability
                    AnimatedContent(
                        targetState = globalShowAzan,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(800)) togetherWith
                                fadeOut(animationSpec = tween(800))
                        },
                    ) { isShowingAzan ->
                        Text(
                            text =
                                if (isShowingAzan || prayerInfo.iqamahTime == null) {
                                    TimeUtils.formatTimeBasedOnPreference(prayerInfo.azanTime, show24Hour)
                                } else {
                                    TimeUtils.formatTimeBasedOnPreference(
                                        prayerInfo.iqamahTime,
                                        show24Hour,
                                    )
                                },
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = calculatedTimeFontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                            maxLines = 1,
                            softWrap = false,
                            color = prayerColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
