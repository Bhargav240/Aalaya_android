package com.aalaya.pg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * A modal with a caller-supplied form body — "add branch", "record payment",
 * "raise complaint", and so on.
 *
 * Built on [HamsaSheet] rather than Material's AlertDialog so its buttons are
 * the design's Secondary/Primary pair instead of bare text links. This is the
 * most frequently seen dialog in the app, so it was the most visible place the
 * old Material styling still showed through.
 */
@Composable
fun FormDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    saving: Boolean = false,
    error: String? = null,
    body: @Composable () -> Unit,
) {
    HamsaSheet(
        title = title,
        onDismiss = { if (!saving) onDismiss() },
    ) {
        body()

        error?.let {
            Text(
                text = it,
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.errorFg,
                modifier = Modifier.padding(top = HamsaTheme.spacing.md),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HamsaTheme.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !saving,
                size = ButtonSize.Small,
            )
            PrimaryButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = saving,
                size = ButtonSize.Small,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
