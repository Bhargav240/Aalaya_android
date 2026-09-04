package com.aalaya.pg.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.AmountTextStyle
import com.aalaya.pg.ui.theme.HamsaTheme
import com.aalaya.pg.ui.theme.OverlineTextStyle

/**
 * `.stat` — a metric card: tiny uppercase overline, a big Manrope value, and an
 * optional faint caption. Used by the dashboard 2x2 grid, the payments 3-up
 * summary and tenant receipts.
 *
 * The value auto-shrinks rather than wrapping, matching the design's
 * `font-size: clamp(17px, 4.2vw, 24px)` — long rupee figures like ₹22,35,300
 * must stay on one line inside a half-width tile.
 */
@Composable
fun StatTile(
    overline: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueColor: Color = HamsaTheme.colors.textPrimary,
    accentColor: Color = HamsaTheme.colors.brand,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hamsaSurface(radius = HamsaTheme.radii.lg, borderColor = accentColor.copy(alpha = 0.32f))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(HamsaTheme.spacing.stat),
    ) {
        Text(
            text = overline.uppercase(),
            style = OverlineTextStyle,
            color = HamsaTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AutoSizeText(
            text = value,
            style = AmountTextStyle,
            color = valueColor,
            minFontScale = 0.7f,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )
        if (caption != null) {
            Text(
                text = caption,
                style = HamsaTheme.type.labelMedium,
                color = HamsaTheme.colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
