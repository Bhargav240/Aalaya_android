package com.aalaya.pg.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Shrinks [text] until it fits on one line, down to [minFontScale] of the
 * style's size. This is the Compose equivalent of the design system's
 * `font-size: clamp(17px, 4.2vw, 24px)` on `.stat .v`: a rupee figure like
 * ₹22,35,300 has to stay on a single line inside a half-width stat tile
 * instead of wrapping or being clipped.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minFontScale: Float = 0.7f,
) {
    var scale by remember(text) { mutableStateOf(1f) }
    Text(
        text = text,
        style = style.copy(
            fontSize = style.fontSize * scale,
            lineHeight = style.lineHeight * scale,
        ),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.didOverflowWidth && scale > minFontScale) {
                scale = (scale - 0.05f).coerceAtLeast(minFontScale)
            }
        },
    )
}
