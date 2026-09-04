package com.aalaya.pg.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `.list-row` — the standard row inside a card: optional leading (avatar/dot),
 * a title+subtitle column that grows, and trailing content (badge/button).
 *
 * Pass `showTopDivider = false` for the first row in a group, mirroring
 * `.list-row:first-child { border-top: none }`.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showTopDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        if (showTopDivider) {
            HorizontalDivider(color = HamsaTheme.colors.border)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(vertical = HamsaTheme.spacing.rowV),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    // The design's row titles are `<b class="small">` — 13px
                    // semibold, which is what separates them from the subtitle.
                    style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = HamsaTheme.colors.textPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = HamsaTheme.type.labelMedium,
                        color = HamsaTheme.colors.textSecondary,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/**
 * `.rrow` — a label/value pair used inside receipts, "Your PG", settlement
 * breakdowns and bed detail sheets. Label is muted left, value is emphasised
 * right (`.rrow .v { font-weight: 500 }`).
 */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = HamsaTheme.colors.textPrimary,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = HamsaTheme.type.bodySmall,
            color = HamsaTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
        )
    }
}
