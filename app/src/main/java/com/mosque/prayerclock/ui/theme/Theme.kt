package com.mosque.prayerclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color

@Composable
fun MosqueClockTheme(
    colorTheme: ColorTheme = AppColorThemes.ClassicMosque,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalColorTheme provides colorTheme) {
        // Create dynamic color scheme based on selected theme
        val dynamicColorScheme =
            darkColorScheme(
                primary = colorTheme.uiPrimary, // Primary UI color from theme
                secondary = colorTheme.uiSecondary, // Secondary UI from theme
                tertiary = colorTheme.uiTertiary, // Tertiary UI from theme
                background = colorTheme.backgroundMain, // Pure black for maximum contrast
                surface = colorTheme.surfaceCard, // Card surface from theme
                onPrimary = colorTheme.textPrimary, // Text on primary
                onSecondary = colorTheme.textPrimary, // Text on secondary
                onTertiary = colorTheme.textPrimary, // Text on tertiary
                onBackground = colorTheme.textPrimary, // Text on background
                onSurface = colorTheme.textPrimary, // Text on surface
                primaryContainer = colorTheme.uiPrimary.copy(alpha = 0.3f), // Container colors
                secondaryContainer = colorTheme.uiSecondary.copy(alpha = 0.3f),
                tertiaryContainer = colorTheme.uiTertiary.copy(alpha = 0.3f),
            )

        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = Typography,
        ) {
            // Sorotan fokus remote yang tegas: bawaan Material hanya 12%, nyaris tak terlihat di TV.
            CompositionLocalProvider(LocalRippleTheme provides TvFocusRippleTheme, content = content)
        }
    }
}

/** Lapisan fokus/tekan yang jelas untuk navigasi D-pad. */
private object TvFocusRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color = LocalContentColor.current

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(draggedAlpha = 0.24f, focusedAlpha = 0.2f, hoveredAlpha = 0.2f, pressedAlpha = 0.34f)
}
