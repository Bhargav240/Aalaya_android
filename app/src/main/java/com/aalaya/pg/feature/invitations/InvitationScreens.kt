package com.aalaya.pg.feature.invitations

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.core.network.dto.TenantInvitationDto
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.DateField
import com.aalaya.pg.ui.components.DestructiveButton
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FieldLabel
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaDropdown
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.KeyValueRow
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.ModalBackdrop
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.components.toneForStatus
import com.aalaya.pg.ui.theme.HamsaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun InvitationListScreen(
    onOpenInvitation: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: MyInvitationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InvitationList(
        title = "Your invitations",
        lead = "Accept an invitation to join a property. Your verified login email must match.",
        state = state,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onOpenInvitation = onOpenInvitation,
    )
}

@Composable
fun InvitationManagementScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenInvitation: (String) -> Unit,
    viewModel: InvitationManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InvitationList(
        title = "Tenant invitations",
        lead = "Invite tenants, follow onboarding progress, and activate approved memberships.",
        state = state,
        onRefresh = viewModel::refresh,
        onBack = onBack,
        onOpenInvitation = onOpenInvitation,
        onCreate = onCreate,
    )
}

@Composable
private fun InvitationList(
    title: String,
    lead: String,
    state: InvitationListState,
    onRefresh: () -> Unit,
    onOpenInvitation: (String) -> Unit,
    onBack: (() -> Unit)?,
    onCreate: (() -> Unit)? = null,
) {
    HamsaScreen(
        title = title,
        lead = lead,
        onBack = onBack,
        refreshing = state.loading,
        onRefresh = onRefresh,
        actions = onCreate?.let { create ->
            {
                PrimaryButton(
                    text = "New",
                    onClick = create,
                    size = ButtonSize.Small,
                )
            }
        },
    ) {
        when {
            state.loading && state.invitations.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.invitations.isEmpty() -> item("error") {
                ErrorState(state.error, onRetry = onRefresh)
            }
            state.invitations.isEmpty() -> item("empty") {
                EmptyState(
                    title = "No invitations found",
                    detail = if (onCreate == null) {
                        "Invitations sent to your verified email will appear here."
                    } else {
                        "Create an invitation when you are ready to onboard a tenant."
                    },
                    icon = Icons.Outlined.MailOutline,
                    action = onCreate?.let { create ->
                        { PrimaryButton("Create invitation", create) }
                    },
                )
            }
            else -> state.invitations.forEach { invitation ->
                item(invitation.id) {
                    InvitationRow(invitation, onClick = { onOpenInvitation(invitation.id) })
                }
            }
        }
    }
}

@Composable
private fun InvitationRow(invitation: TenantInvitationDto, onClick: () -> Unit) {
    HamsaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Icons.Outlined.MarkEmailRead,
                // Amber while the invitation is still waiting on someone.
                tone = if (toneForStatus(invitation.status) == StatusTone.Warning) {
                    IconTone.Warning
                } else {
                    IconTone.Brand
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(invitation.fullName, style = HamsaTheme.type.titleSmall, color = HamsaTheme.colors.textPrimary)
                Text(invitation.email, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textSecondary)
            }
            StatusChip(invitationStatusLabel(invitation.status), toneForStatus(invitation.status))
        }
        invitation.plannedMoveInDate?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.sm))
            Text("Move-in $it", style = HamsaTheme.type.labelMedium, color = HamsaTheme.colors.textTertiary)
        }
    }
}

@Composable
fun InvitationDetailScreen(
    onBack: () -> Unit,
    onOpenDigiLocker: (String) -> Unit,
    viewModel: InvitationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScreen(
        title = "Invitation",
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    ) {
        val status = state.onboarding?.status ?: state.invitation?.status
        if (status == "invited") {
            PrimaryButton("Accept invitation", viewModel::accept, Modifier.fillMaxWidth(), loading = state.actionInProgress == "accept")
            SecondaryButton("Decline", viewModel::decline, Modifier.fillMaxWidth(), enabled = state.actionInProgress == null)
        }
        if (status == "kyc_pending" || status == "kyc_failed") {
            PrimaryButton(
                "Verify with DigiLocker",
                onClick = { state.invitation?.id?.let(onOpenDigiLocker) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun TenantOnboardingStatusScreen(
    onBack: () -> Unit,
    onOpenDigiLocker: (String) -> Unit,
    viewModel: InvitationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        viewModel.startPolling()
        onDispose(viewModel::stopPolling)
    }
    DetailScreen(
        title = "Onboarding status",
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    ) {
        val status = state.onboarding?.status
        if (status == "kyc_pending" || status == "kyc_failed") {
            PrimaryButton(
                "Continue identity verification",
                onClick = { state.invitation?.id?.let(onOpenDigiLocker) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun DigiLockerVerificationScreen(
    onBack: () -> Unit,
    viewModel: InvitationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(viewModel) {
        viewModel.startPolling()
        onDispose(viewModel::stopPolling)
    }
    LaunchedEffect(state.authorizationUrl) {
        state.authorizationUrl?.let { url ->
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
            viewModel.consumeAuthorizationUrl()
        }
    }

    HamsaScreen(
        title = "Identity verification",
        lead = "DigiLocker opens securely in your browser. Aalaya stores only the verification result, not your Aadhaar document.",
        onBack = onBack,
        refreshing = state.loading,
        onRefresh = viewModel::refresh,
    ) {
        item("status") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                Text("Verification status", style = HamsaTheme.type.titleSmall, color = HamsaTheme.colors.textPrimary)
                Spacer(Modifier.height(HamsaTheme.spacing.sm))
                StatusChip(
                    invitationStatusLabel(state.verification?.status ?: state.onboarding?.status ?: "not_started"),
                    toneForStatus(state.verification?.status ?: state.onboarding?.status ?: "pending"),
                )
                state.verification?.safeFailureReason?.let {
                    Spacer(Modifier.height(HamsaTheme.spacing.sm))
                    Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.errorFg)
                }
            }
        }
        item("action") {
            PrimaryButton(
                "Open DigiLocker",
                viewModel::startDigiLocker,
                Modifier.fillMaxWidth(),
                loading = state.actionInProgress == "digilocker",
            )
        }
    }
}

@Composable
fun InvitationReviewScreen(
    canApprove: Boolean,
    onBack: () -> Unit,
    viewModel: InvitationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    varReviewScreen(state, canApprove, onBack, viewModel)
}

@Composable
private fun varReviewScreen(
    state: InvitationDetailState,
    canApprove: Boolean,
    onBack: () -> Unit,
    viewModel: InvitationDetailViewModel,
) {
    var reviewReason by remember { mutableStateOf("") }
    DetailScreen("Review invitation", state, onBack, viewModel::refresh) {
        val status = state.onboarding?.status ?: state.invitation?.status.orEmpty()
        if (status in setOf("kyc_pending", "kyc_failed")) {
            HamsaTextField(
                value = reviewReason,
                onValueChange = { reviewReason = it.take(500) },
                label = "Manual identity review reason",
                placeholder = "Record what was checked",
                singleLine = false,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
                PrimaryButton(
                    "Approve KYC",
                    onClick = { viewModel.manualReview(true, reviewReason) },
                    modifier = Modifier.weight(1f),
                    enabled = reviewReason.isNotBlank(),
                    loading = state.actionInProgress == "manual_approve",
                )
                SecondaryButton(
                    "Fail KYC",
                    onClick = { viewModel.manualReview(false, reviewReason) },
                    modifier = Modifier.weight(1f),
                    enabled = reviewReason.isNotBlank() && state.actionInProgress == null,
                )
            }
        }
        if (status == "approval_pending" && canApprove) {
            PrimaryButton("Approve & activate", viewModel::approve, Modifier.fillMaxWidth(), loading = state.actionInProgress == "approve")
            DestructiveButton("Reject", viewModel::reject, Modifier.fillMaxWidth(), enabled = state.actionInProgress == null)
        }
        if (status !in setOf("cancelled", "rejected", "active", "approved")) {
            SecondaryButton("Resend email", viewModel::resend, Modifier.fillMaxWidth(), enabled = state.actionInProgress == null)
            DestructiveButton("Cancel invitation", viewModel::cancel, Modifier.fillMaxWidth(), enabled = state.actionInProgress == null)
        }
    }
}

@Composable
private fun DetailScreen(
    title: String,
    state: InvitationDetailState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    actions: @Composable () -> Unit,
) {
    HamsaScreen(title, onBack = onBack, refreshing = state.loading, onRefresh = onRefresh) {
        when {
            state.loading && state.invitation == null -> item("loading") { LoadingState() }
            state.error != null && state.invitation == null -> item("error") { ErrorState(state.error, onRetry = onRefresh) }
            state.invitation != null -> {
                val invitation = state.invitation
                item("summary") {
                    HamsaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(invitation.fullName, style = HamsaTheme.type.titleMedium, color = HamsaTheme.colors.textPrimary)
                            StatusChip(invitationStatusLabel(state.onboarding?.status ?: invitation.status), toneForStatus(state.onboarding?.status ?: invitation.status))
                        }
                        Spacer(Modifier.height(HamsaTheme.spacing.sm))
                        KeyValueRow("Email", invitation.email)
                        invitation.monthlyRentPaise?.let { KeyValueRow("Monthly rent", Money.format(it)) }
                        invitation.depositPaise?.let { KeyValueRow("Deposit", Money.format(it)) }
                        invitation.plannedMoveInDate?.let { KeyValueRow("Move-in", it) }
                        KeyValueRow("Identity verification", if (invitation.requireIdentityVerification) "Required" else "Not required")
                    }
                }
                state.onboarding?.let { onboarding ->
                    item("next") {
                        HamsaCard(modifier = Modifier.fillMaxWidth(), borderColor = HamsaTheme.colors.brandMuted) {
                            Text("Next step", style = HamsaTheme.type.titleSmall, color = HamsaTheme.colors.textPrimary)
                            Text(onboardingNextActionLabel(onboarding.nextAction), style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textSecondary)
                        }
                    }
                }
                item("actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) { actions() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvitationScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateInvitationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showMoveInDatePicker by remember { mutableStateOf(false) }
    LaunchedEffect(state.createdInvitationId) {
        state.createdInvitationId?.let {
            viewModel.consumeCreated()
            onCreated(it)
        }
    }
    HamsaScreen(
        title = "Invite tenant",
        lead = "Select a room for the tenant. Bed assignment is optional; only available beds from your authorized branches are shown.",
        onBack = onBack,
    ) {
        if (state.loading) {
            item("loading") { LoadingState() }
        } else {
            item("form") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md)) {
                        HamsaTextField(state.email, viewModel::setEmail, "Email", keyboardType = KeyboardType.Email)
                        HamsaTextField(state.fullName, viewModel::setFullName, "Full name")
                        FieldLabel("Tenant type")
                        Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
                            listOf(PlanKind.Daily, PlanKind.Weekly, PlanKind.Monthly).forEach { plan ->
                                SelectableChip(
                                    label = plan.label,
                                    selected = state.planKind == plan,
                                    onClick = { viewModel.setPlanKind(plan) },
                                )
                            }
                        }
                        HamsaDropdown("Branch", state.branches, state.branch, { it.name }, viewModel::selectBranch)
                        if (state.branch != null) {
                            HamsaDropdown("Room", state.rooms, state.room, { "Room ${it.number} · ${it.coolingType.replace('_', ' ').uppercase()}" }, viewModel::selectRoom, placeholder = if (state.rooms.isEmpty()) "No rooms found" else "Select a room")
                        }
                        if (state.room != null) {
                            HamsaDropdown("Bed (optional)", state.beds, state.bed, { it.label }, viewModel::selectBed, placeholder = if (state.beds.isEmpty()) "No available beds" else "Select a bed")
                        }
                        HamsaTextField(state.rentRupees, viewModel::setRent, "Monthly rent (₹)", keyboardType = KeyboardType.Decimal)
                        HamsaTextField(state.depositRupees, viewModel::setDeposit, "Deposit (₹)", keyboardType = KeyboardType.Decimal)
                        DateField(
                            label = "Planned move-in date",
                            value = state.moveInDate.ifBlank { null },
                            placeholder = "Select a date",
                            onClick = { showMoveInDatePicker = true },
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("DigiLocker verification", style = HamsaTheme.type.bodyLarge, color = HamsaTheme.colors.textPrimary)
                                Text("Require identity verification before approval", style = HamsaTheme.type.labelMedium, color = HamsaTheme.colors.textSecondary)
                            }
                            Switch(checked = state.requireKyc, onCheckedChange = viewModel::setRequireKyc)
                        }
                        state.error?.let { Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.errorFg) }
                        PrimaryButton("Send invitation", viewModel::submit, Modifier.fillMaxWidth(), loading = state.saving)
                    }
                }
            }
        }
    }

    if (showMoveInDatePicker) {
        val initialDate = state.moveInDate.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showMoveInDatePicker = false },
            confirmButton = {
                ModalBackdrop()
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setMoveInDate(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString(),
                        )
                    }
                    showMoveInDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showMoveInDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = pickerState) }
    }
}

internal fun invitationStatusLabel(status: String): String = status
    .replace('_', ' ')
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

internal fun onboardingNextActionLabel(action: String): String = when (action) {
    "accept_or_decline" -> "Review and accept or decline this invitation."
    "continue_onboarding" -> "Continue the tenant onboarding steps."
    "complete_identity_verification" -> "Complete DigiLocker identity verification."
    "contact_property" -> "Identity verification needs review. Contact the property team."
    "wait_for_approval" -> "Your property team is reviewing the invitation."
    "activation_in_progress" -> "Your tenant access is being activated."
    "open_tenant_home" -> "Activation is complete. Switch to this property to continue."
    "none" -> "No action is currently required."
    else -> invitationStatusLabel(action)
}
