package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `.sheet` — the design's centred modal card: 20dp radius, a title row with a
 * circular close button, and scrollable content capped at 88% of the viewport.
 *
 * The mockup uses this for bed/resident detail, payment confirmation and
 * receipts, rather than pushing a whole new screen. Keeping those as modals
 * preserves the user's place in the list underneath.
 */
@Composable
fun HamsaSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HamsaTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ModalBackdrop()
        Column(
            modifier = modifier
                .padding(HamsaTheme.spacing.lg)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(HamsaTheme.radii.xl))
                .border(1.dp, colors.borderStrong, RoundedCornerShape(HamsaTheme.radii.xl))
                .padding(horizontal = HamsaTheme.spacing.xl, vertical = HamsaTheme.spacing.xxl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
            ) {
                Text(
                    text = title,
                    style = HamsaTheme.type.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
                // `sheetCloseBtn()` — a 36dp circular button on the canvas tint.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.canvas, CircleShape)
                        .border(1.dp, colors.border, CircleShape),
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = HamsaTheme.spacing.md)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

// The backdrop now lives in Overlay.kt as ModalBackdrop() so sheets, form
// dialogs, confirmations and date pickers all share one implementation.
