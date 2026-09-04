package com.aalaya.pg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * A square icon-over-label tile for the home screen's quick-action grid.
 *
 * These replace a stack of full-width link rows: the same destinations read far
 * faster as a 3-up grid, and the icon does the work the row's chevron used to.
 * Labels wrap to two lines so a longer destination name doesn't truncate.
 */
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tone: IconTone = IconTone.Brand,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.pressScale(onClick = onClick) else it }
            .hamsaSurface(radius = HamsaTheme.radii.lg)
            .padding(vertical = HamsaTheme.spacing.md, horizontal = HamsaTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
    ) {
        IconTile(
            icon = icon,
            tone = tone,
            size = 46.dp,
            radius = HamsaTheme.radii.md,
            iconSize = 22.dp,
        )
        Text(
            text = label,
            style = HamsaTheme.type.labelMedium,
            color = HamsaTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
