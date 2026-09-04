package com.aalaya.pg.feature.residents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aalaya.pg.core.network.dto.LedgerDto
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.GhostButton
import com.aalaya.pg.ui.components.HamsaSheet
import com.aalaya.pg.ui.components.KeyValueRow
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `openBedDetail` — the resident summary the mockup opens as a centred sheet
 * rather than a full screen, so the list underneath keeps its place.
 *
 * [onOpenFull] is offered as a secondary action for the things the sheet
 * deliberately doesn't carry (receipts, ledger history, move-out settlement).
 */
@Composable
fun ResidentSheet(
    residentName: String,
    bedLabel: String?,
    ledger: LedgerDto?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onOpenFull: () -> Unit,
    onMarkPaid: (() -> Unit)? = null,
    markingPaid: Boolean = false,
) {
    val colors = HamsaTheme.colors
    val outstanding = ledger?.outstandingPaise ?: 0L
    val owing = outstanding > 0

    HamsaSheet(
        title = bedLabel?.let { "Bed $it" } ?: residentName,
        onDismiss = onDismiss,
        trailing = {
            if (ledger != null) {
                StatusChip(
                    label = if (owing) "Due" else "Paid up",
                    tone = if (owing) StatusTone.Warning else StatusTone.Success,
                )
            }
        },
    ) {
        if (loading && ledger == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = HamsaTheme.spacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = colors.brand,
                )
            }
            return@HamsaSheet
        }

        KeyValueRow("Resident", ledger?.fullName ?: residentName)
        bedLabel?.let { KeyValueRow("Bed", it) }
        if (ledger != null) {
            KeyValueRow("Plan", ledger.planKind.replaceFirstChar { it.uppercase() })
            KeyValueRow("Rent", "${Money.format(ledger.rentPaise)} / month")
            KeyValueRow("Deposit held", Money.format(ledger.depositPaise))
            KeyValueRow("Paid to date", Money.format(ledger.paidTotalPaise))
            HorizontalDivider(
                color = colors.border,
                modifier = Modifier.padding(vertical = HamsaTheme.spacing.sm),
            )
            KeyValueRow(
                label = "Outstanding",
                value = Money.format(outstanding),
                valueColor = if (owing) colors.errorFg else colors.successFg,
            )
        } else {
            Text(
                "Couldn't load this resident's balance.",
                style = HamsaTheme.type.bodySmall,
                color = colors.textTertiary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = HamsaTheme.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onMarkPaid != null && owing) {
                PrimaryButton(
                    text = "Mark paid",
                    onClick = onMarkPaid,
                    loading = markingPaid,
                    size = ButtonSize.Small,
                )
            }
            GhostButton(text = "Full record →", onClick = onOpenFull)
        }
    }
}
