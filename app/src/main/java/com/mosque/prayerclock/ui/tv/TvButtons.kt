package com.mosque.prayerclock.ui.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Tombol untuk remote TV: saat terpilih (fokus D-pad) tombol diberi bingkai terang dan sedikit membesar,
 * karena lapisan fokus bawaan Material nyaris tak terlihat di layar TV. Tanda tangan sama dengan Material 3
 * sehingga cukup mengganti import.
 */
val TvFocusColor = Color(0xFFFFD54F)

fun Modifier.tvFocusRing(focused: Boolean, shape: Shape): Modifier =
    if (focused) {
        this.graphicsLayer {
            scaleX = 1.06f
            scaleY = 1.06f
        }.border(3.dp, TvFocusColor, shape)
    } else {
        this
    }

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    androidx.compose.material3.Button(
        onClick, modifier.tvFocusRing(focused, shape), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    androidx.compose.material3.OutlinedButton(
        onClick, modifier.tvFocusRing(focused, shape), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content,
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    androidx.compose.material3.TextButton(
        onClick, modifier.tvFocusRing(focused, shape), enabled, shape, colors, elevation, border, contentPadding, interactionSource, content,
    )
}

/** Bingkai fokus untuk baris/kartu yang bisa diklik (pasang sebelum clickable/selectable). */
fun Modifier.tvFocusHighlight(shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)): Modifier =
    composed {
        var focused by remember { mutableStateOf(false) }
        this
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .then(if (focused) Modifier.border(3.dp, TvFocusColor, shape) else Modifier)
    }

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.SwitchColors = androidx.compose.material3.SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.tvFocusRing(focused, androidx.compose.foundation.shape.RoundedCornerShape(50)),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}
