package com.aalaya.pg.feature.enquiries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.network.dto.EnquiryDto
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.GhostButton
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/** The manager's inbox of listing enquiries. */
@Composable
fun EnquiriesScreen(
    onBack: () -> Unit,
    viewModel: EnquiriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HamsaScreen(
        title = "Enquiries",
        lead = "People who asked about your listings. Mark them as you follow up.",
        onBack = onBack,
        refreshing = state.loading,
        onRefresh = viewModel::refresh,
    ) {
        when {
            state.loading && state.enquiries.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.enquiries.isEmpty() ->
                item("error") { ErrorState(state.error!!, onRetry = viewModel::refresh) }
            state.enquiries.isEmpty() -> item("empty") {
                EmptyState(
                    title = "No enquiries yet",
                    detail = "When someone enquires about one of your listings it shows up here.",
                    icon = Icons.Outlined.Inbox,
                )
            }
            else -> state.enquiries.forEach { e ->
                item(e.id) {
                    EnquiryRow(
                        enquiry = e,
                        busy = state.updatingId == e.id,
                        onContacted = { viewModel.setStatus(e, "contacted") },
                        onClosed = { viewModel.setStatus(e, "closed") },
                    )
                }
            }
        }
    }
}

@Composable
private fun EnquiryRow(
    enquiry: EnquiryDto,
    busy: Boolean,
    onContacted: () -> Unit,
    onClosed: () -> Unit,
) {
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Outlined.RecordVoiceOver)
            Text(
                enquiry.phone,
                style = HamsaTheme.type.titleSmall,
                color = HamsaTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            StatusChip(label = enquiry.status, tone = toneFor(enquiry.status))
        }
        Text(
            DateFmt.epochSeconds(enquiry.createdAtEpoch),
            style = HamsaTheme.type.labelMedium,
            color = HamsaTheme.colors.textTertiary,
        )
        enquiry.note?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textSecondary)
        }
        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
            SecondaryButton(text = "Contacted", onClick = onContacted, enabled = !busy, size = ButtonSize.Small)
            GhostButton(text = "Closed", onClick = onClosed, enabled = !busy)
        }
    }
}

private fun toneFor(status: String): StatusTone = when (status) {
    "contacted" -> StatusTone.Info
    "closed" -> StatusTone.Neutral
    else -> StatusTone.Warning
}
