package com.aalaya.pg.feature.enquiries

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        lead = "Active leads — contacted enquiries stay here for follow-ups. Close to dismiss.",
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
                    title = "No active enquiries",
                    detail = "When someone enquires about one of your listings it shows up here. Closed enquiries are automatically removed.",
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
    val context = LocalContext.current
    val isContacted = enquiry.status == "contacted"
    val displayName = enquiry.prospectName?.takeIf { it.isNotBlank() } ?: "Prospective Resident"
    val hasValidPhone = enquiry.phone.isNotBlank() && enquiry.phone != "email-authenticated" && enquiry.phone != "No contact"

    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Outlined.Person)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    style = HamsaTheme.type.titleSmall,
                    color = HamsaTheme.colors.textPrimary,
                )
                Text(
                    DateFmt.epochSeconds(enquiry.createdAtEpoch),
                    style = HamsaTheme.type.labelMedium,
                    color = HamsaTheme.colors.textTertiary,
                )
            }
            StatusChip(label = enquiry.status, tone = toneFor(enquiry.status))
        }

        Spacer(Modifier.height(HamsaTheme.spacing.xs))

        // Contact Info (Phone & Email)
        if (hasValidPhone) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${enquiry.phone.trim()}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
            ) {
                Icon(
                    Icons.Outlined.Phone,
                    contentDescription = "Call",
                    modifier = Modifier.size(16.dp),
                    tint = HamsaTheme.colors.brand,
                )
                Text(
                    enquiry.phone,
                    style = HamsaTheme.type.bodyMedium,
                    color = HamsaTheme.colors.brand,
                )
            }
        }

        enquiry.email?.takeIf { it.isNotBlank() }?.let { email ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.Email,
                    contentDescription = "Email",
                    modifier = Modifier.size(16.dp),
                    tint = HamsaTheme.colors.textSecondary,
                )
                Text(
                    email,
                    style = HamsaTheme.type.bodySmall,
                    color = HamsaTheme.colors.textSecondary,
                )
            }
        }

        enquiry.note?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            Text(
                "Note: $it",
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.textSecondary,
            )
        }

        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
            SecondaryButton(
                text = if (isContacted) "Follow-up" else "Contacted",
                onClick = onContacted,
                enabled = !busy,
                size = ButtonSize.Small,
            )
            GhostButton(
                text = "Close",
                onClick = onClosed,
                enabled = !busy,
            )
        }
    }
}

private fun toneFor(status: String): StatusTone = when (status) {
    "contacted" -> StatusTone.Info
    "closed" -> StatusTone.Neutral
    else -> StatusTone.Warning
}
