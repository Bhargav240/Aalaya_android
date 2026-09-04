package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * The semantic tints an [IconTile] can carry. Each maps to a foreground/background
 * pair already in the palette, so a row can signal "overdue" or "rejected"
 * without a screen hand-picking colours.
 */
enum class IconTone { Brand, Warning, Error, Success, Gold, Neutral }

/**
 * A small rounded badge holding a single icon — the motif that runs through
 * every list in the app: a room card, a payment row, a notice, a receipt.
 *
 * Giving each row a tinted 36dp tile is what turns a wall of text into
 * something scannable, and the [tone] keeps that signal consistent (amber for
 * anything owed or pending, red for failures, brand blue for the ordinary case).
 *
 * Sizes follow the design's ladder: 36dp for a card header or list row, 30–32dp
 * for a denser secondary list, 24–28dp for rows nested inside a card.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: IconTone = IconTone.Brand,
    size: Dp = 36.dp,
    radius: Dp = HamsaTheme.radii.sm,
    tint: Color? = null,
    background: Color? = null,
    iconSize: Dp = size * 0.5f,
    contentDescription: String? = null,
) {
    val colors = HamsaTheme.colors
    val fg = tint ?: when (tone) {
        IconTone.Brand -> colors.brand
        IconTone.Warning -> colors.warningFg
        IconTone.Error -> colors.errorFg
        IconTone.Success -> colors.successFg
        IconTone.Gold -> colors.gold
        IconTone.Neutral -> colors.textSecondary
    }
    val bg = background ?: when (tone) {
        IconTone.Brand -> colors.brandSubtle
        IconTone.Warning -> colors.warningBg
        IconTone.Error -> colors.errorBg
        IconTone.Success -> colors.successBg
        IconTone.Gold -> colors.goldBg
        IconTone.Neutral -> colors.canvas
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(bg, RoundedCornerShape(radius)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(iconSize),
        )
    }
}
