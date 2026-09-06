package com.aalaya.pg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `.section-head` — an h2 with optional trailing badge/link, placed above a
 * card's inner list. The trailing slot is where the design puts running totals
 * ("₹22,35,300 total") and jump links ("Occupancy →").
 */
@Composable
fun SectionHead(
    title: String,
    modifier: Modifier = Modifier,
    style: TextStyle = HamsaTheme.type.titleMedium,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = HamsaTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = style,
            color = HamsaTheme.colors.textPrimary,
        )
        trailing?.invoke()
    }
}
