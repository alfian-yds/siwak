package com.mosque.prayerclock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Teks berjalan SIWAK: selalu bergulir dari tepi kanan sampai habis di kiri, baik teks panjang maupun
 * pendek (marquee bawaan Compose diam bila teksnya muat). [onCycle] dipanggil setiap kali teks selesai
 * lewat satu putaran penuh, dipakai pengumuman untuk berganti ke pengumuman berikutnya.
 */
@Composable
fun SiwakMarquee(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    speedDpPerSecond: Float = 60f,
    onCycle: () -> Unit = {},
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val onCycleNow by rememberUpdatedState(onCycle)
    BoxWithConstraints(modifier.clipToBounds()) {
        val containerPx = constraints.maxWidth.toFloat()
        val textPx = remember(text, style) { measurer.measure(text, style, softWrap = false, maxLines = 1).size.width.toFloat() }
        val offset = remember(text) { Animatable(containerPx) }
        LaunchedEffect(text, containerPx, textPx) {
            if (text.isBlank() || containerPx <= 0f) return@LaunchedEffect
            val speedPx = with(density) { speedDpPerSecond.dp.toPx() }.coerceAtLeast(1f)
            while (true) {
                offset.snapTo(containerPx)
                val distance = containerPx + textPx
                offset.animateTo(-textPx, tween(durationMillis = (distance / speedPx * 1000).roundToInt(), easing = LinearEasing))
                onCycleNow()
            }
        }
        Text(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier =
                Modifier
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .offset { IntOffset(offset.value.roundToInt(), 0) },
        )
    }
}
