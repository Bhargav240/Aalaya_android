package com.aalaya.pg.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * The canonical surface (`.card`): 16dp radius, 18dp padding, soft azure shadow
 * in light mode and a hairline border in dark. Everything that groups content
 * on the canvas should use this rather than a bare Material Card, so radius and
 * depth stay identical across screens.
 */
@Composable
fun HamsaCard(
    modifier: Modifier = Modifier,
    padding: Dp = HamsaTheme.spacing.card,
    color: Color = HamsaTheme.colors.surface,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            // The scale has to come before the surface so the card's
            // background and border dip with its contents, not around them.
            .let { if (onClick != null) it.pressScale(onClick = onClick) else it }
            .hamsaSurface(
                radius = HamsaTheme.radii.lg,
                color = color,
                borderColor = borderColor,
            )
            .padding(padding),
        content = content,
    )
}
