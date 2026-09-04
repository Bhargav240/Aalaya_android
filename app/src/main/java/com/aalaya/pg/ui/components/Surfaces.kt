package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme
import com.aalaya.pg.ui.theme.ShadowTint

/**
 * The design system expresses depth two different ways depending on mode, and
 * every raised surface in the app has to follow the same rule:
 *
 *  - **Light** — a soft azure-tinted drop shadow plus a subtle border.
 *  - **Dark**  — shadows are switched off entirely (`--sh-card: none`) and a
 *    1px hairline border stands in (`body.dark .card { border: 1px solid }`).
 *
 * Centralising it here keeps cards, stat tiles, sheets and the nav bar from
 * drifting apart, which is what produces the "some cards have borders, some
 * have shadows" look.
 */
@Composable
fun Modifier.hamsaSurface(
    shape: Shape,
    elevation: Dp = HamsaTheme.elevation.card,
    color: Color = HamsaTheme.colors.surface,
    borderColor: Color? = null,
): Modifier {
    val colors = HamsaTheme.colors
    return if (colors.isDark) {
        this
            .background(color, shape)
            .border(1.dp, borderColor ?: colors.border, shape)
    } else {
        val shadowed = this.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = ShadowTint,
            spotColor = ShadowTint,
        ).background(color, shape)
        // A hairline border gives light surfaces the crisp, premium structure
        // that shadows alone cannot provide on bright screens.
        shadowed.border(1.dp, borderColor ?: colors.border, shape)
    }
}

/** Convenience overload for the common rounded-rect case. */
@Composable
fun Modifier.hamsaSurface(
    radius: Dp = HamsaTheme.radii.lg,
    elevation: Dp = HamsaTheme.elevation.card,
    color: Color = HamsaTheme.colors.surface,
    borderColor: Color? = null,
): Modifier = hamsaSurface(RoundedCornerShape(radius), elevation, color, borderColor)
