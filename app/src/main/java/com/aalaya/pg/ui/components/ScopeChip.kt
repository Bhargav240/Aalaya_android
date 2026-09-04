package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * A compact context switcher: a brand-tinted pill showing the current scope,
 * tapping it opens the options. The design's topbar carries a context line
 * ("Owner · All branches", "Manager · Koramangala"); this makes that line
 * interactive so Home can be narrowed to one branch without leaving the screen.
 */
@Composable
fun <T> ScopeChip(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    /** Renders muted, so an unchosen scope doesn't look like a live selection. */
    placeholder: Boolean = false,
) {
    val colors = HamsaTheme.colors
    var open by remember { mutableStateOf(false) }
    val content = if (placeholder) colors.textSecondary else colors.brand

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                // The placeholder sits on the canvas, so it needs a surface and
                // a hairline to read as tappable — tinting it canvas-on-canvas
                // made the affordance invisible until pressed.
                .background(
                    if (placeholder) colors.surface else colors.brandSubtle,
                    RoundedCornerShape(HamsaTheme.radii.pill),
                )
                .then(
                    if (placeholder) {
                        Modifier.border(
                            1.dp,
                            colors.border,
                            RoundedCornerShape(HamsaTheme.radii.pill),
                        )
                    } else {
                        Modifier
                    },
                )
                .clickable { open = true }
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(
                text = label,
                style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = content,
            )
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(colors.surface),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            optionLabel(option),
                            style = HamsaTheme.type.bodyLarge,
                            color = colors.textPrimary,
                        )
                    },
                    onClick = { onSelect(option); open = false },
                )
            }
        }
    }
}
