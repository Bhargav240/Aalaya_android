package com.aalaya.pg.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * Springy press feedback for tappable surfaces — the shared counterpart of the
 * iOS `ScaleButtonStyle`.
 *
 * Cards and tiles in this app are large targets where a ripple alone reads as
 * flat; dipping the whole surface to 97% on press is what makes a tap feel
 * physical. The ripple is kept on top of it so the interaction still looks
 * native on Android rather than like a transplanted iOS control.
 *
 * Pass [onClick] to make the surface clickable, or omit it to animate a surface
 * whose click is handled further in (a row inside an already-clickable card).
 */
@Composable
fun Modifier.pressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (() -> Unit)? = null,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    return this
        .scale(scale)
        .let { base ->
            if (onClick == null) {
                base
            } else {
                base.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onClick = onClick,
                )
            }
        }
}
