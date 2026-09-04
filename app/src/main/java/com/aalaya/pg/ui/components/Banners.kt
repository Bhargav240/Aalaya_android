package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `.note` — a soft brand-tinted advisory strip. Used for "2 changes waiting for
 * approval →" on the dashboard and inline explanations elsewhere. When
 * [onClick] is supplied a chevron is appended, matching the tappable variant.
 */
@Composable
fun NoteBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTone: IconTone = IconTone.Brand,
    onClick: (() -> Unit)? = null,
) {
    val colors = HamsaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.brandSubtle, RoundedCornerShape(HamsaTheme.radii.md))
            .border(1.dp, colors.brandMuted, RoundedCornerShape(HamsaTheme.radii.md))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            IconTile(icon = icon, tone = iconTone, size = 28.dp, iconSize = 15.dp)
        }
        Text(
            text = text,
            style = HamsaTheme.type.bodySmall,
            color = if (colors.isDark) colors.textSecondary else colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** `.callout-ok` / `.callout-bad` — a settled/blocked result strip. */
@Composable
fun Callout(
    text: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = HamsaTheme.colors
    Text(
        text = text,
        style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = if (positive) colors.successFg else colors.errorFg,
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (positive) colors.successBg else colors.errorBg,
                RoundedCornerShape(HamsaTheme.radii.md),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
