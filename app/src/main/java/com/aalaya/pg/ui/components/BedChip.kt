package com.aalaya.pg.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme
import com.aalaya.pg.ui.theme.Palette

/** `.bed.occ` / `.bed.free` / `.bed.locked`. */
enum class BedState { Occupied, Free, Locked }

/**
 * A dashed rounded-rect outline. `Modifier.border` can only stroke solid lines,
 * and the design draws free/locked beds with `1.5px dashed`.
 */
private fun Modifier.dashedBorder(
    color: Color,
    radius: Dp,
    strokeWidth: Dp = 1.5.dp,
    dash: Dp = 5.dp,
    gap: Dp = 4.dp,
): Modifier = drawBehind {
    val stroke = strokeWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
        ),
    )
}

/**
 * A single bed slot in the occupancy explorer (80×66 in the design).
 *
 *  - **Occupied** — filled brand gradient, occupant initials, bed id, rent.
 *  - **Free**     — dashed strong border, "· free", rent in brand.
 *  - **Locked**   — dashed faint border, not tappable (other branch, or a bed
 *    the current manager may only see counts for).
 *
 * Dark mode swaps the occupied gradient for a flat brand-muted fill, per
 * `body.dark .bed.occ`.
 */
@Composable
fun BedChip(
    bedId: String,
    state: BedState,
    modifier: Modifier = Modifier,
    occupantName: String? = null,
    priceLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(10.dp)

    val headline = when (state) {
        BedState.Occupied -> occupantName?.let(::initialsOf) ?: "—"
        BedState.Free -> "${bedId.substringAfterLast('-')} · free"
        BedState.Locked -> bedId.substringAfterLast('-')
    }
    val headlineColor = when (state) {
        BedState.Occupied -> Color.White.takeIf { !colors.isDark } ?: colors.textPrimary
        BedState.Free -> colors.textSecondary
        BedState.Locked -> colors.textTertiary
    }
    val idColor = when (state) {
        BedState.Occupied ->
            if (colors.isDark) colors.textSecondary else Color.White.copy(alpha = 0.75f)
        else -> colors.textTertiary
    }
    val priceColor = when (state) {
        BedState.Occupied ->
            if (colors.isDark) colors.textSecondary else Color.White.copy(alpha = 0.95f)
        BedState.Free -> colors.brand
        BedState.Locked -> colors.textTertiary
    }

    var box = modifier.width(80.dp).height(66.dp).clip(shape)
    box = when (state) {
        BedState.Occupied -> box.background(
            if (colors.isDark) {
                Brush.linearGradient(listOf(colors.brandMuted, colors.brandMuted))
            } else {
                Brush.linearGradient(listOf(Palette.BrandGradFrom, Palette.BrandGradTo))
            },
        )
        // `.bed.free` / `.bed.locked` are dashed outlines. Modifier.border only
        // draws solid strokes, so the dash is drawn directly.
        BedState.Free -> box.dashedBorder(colors.borderStrong, 10.dp)
        BedState.Locked -> box.dashedBorder(colors.border, 10.dp)
    }
    if (onClick != null && state != BedState.Locked) {
        box = box.clickable(onClick = onClick)
    }

    Column(
        modifier = box,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline,
            style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = headlineColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = bedId,
            style = HamsaTheme.type.labelSmall.copy(fontSize = HamsaTheme.type.labelSmall.fontSize),
            color = idColor,
            maxLines = 1,
        )
        if (priceLabel != null) {
            Text(
                text = priceLabel,
                style = HamsaTheme.type.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = priceColor,
                maxLines = 1,
            )
        }
    }
}
