package com.aalaya.pg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * A shimmering placeholder bar.
 *
 * Lists in this app fetch over a phone connection, and a centred spinner tells
 * the user nothing about what is coming. Drawing the shape of the content
 * instead makes the wait feel shorter and stops the layout jumping when data
 * lands.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    radius: Dp = 6.dp,
) {
    val colors = HamsaTheme.colors
    val base = colors.canvas
    val highlight = colors.border.copy(alpha = 0.7f)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonSweep",
    )
    Column(
        modifier = modifier
            .height(height)
            .drawBehind {
                // The highlight band is one component-width wide and travels
                // from just off the left edge to just past the right one.
                val start = -size.width + (size.width * 2f) * progress
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(start, 0f),
                        end = Offset(start + size.width, 0f),
                    ),
                    cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
                )
            },
    ) {}
}

/** A card-shaped placeholder: a title bar and two shorter detail bars. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hamsaSurface(radius = HamsaTheme.radii.lg)
            .padding(HamsaTheme.spacing.card),
        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
    ) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 14.dp)
        SkeletonBox(modifier = Modifier.width(220.dp), height = 12.dp)
        SkeletonBox(modifier = Modifier.width(140.dp), height = 10.dp)
    }
}

/**
 * The standard loading body for a list screen — [count] stacked [SkeletonCard]s
 * matching the spacing the real rows will use.
 */
@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 3,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
    ) {
        repeat(count) { SkeletonCard() }
    }
}
