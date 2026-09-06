package com.aalaya.pg.feature.residents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.core.network.dto.LedgerDto
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.feature.receipts.ReceiptRow
import com.aalaya.pg.feature.receipts.openPdf
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaAlertDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.HamsaTopBar
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.ModalBackdrop
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.Refreshable
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.theme.HamsaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentDetailScreen(
    residentName: String,
    canManage: Boolean,
    onDeleted: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: ResidentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPay by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var showMoveOut by remember { mutableStateOf(false) }
    var lastDay by remember { mutableStateOf("") }
    var noticeDays by remember { mutableStateOf("30") }
    var deductions by remember { mutableStateOf(listOf(DeductionInput())) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editPlan by remember { mutableStateOf<PlanKind?>(null) }
    var editRent by remember { mutableStateOf("") }
    var editDeposit by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.openUri) {
        state.openUri?.let { openPdf(context, it); viewModel.consumeOpenUri() }
    }

    Scaffold(
        containerColor = HamsaTheme.colors.canvas,
        topBar = { HamsaTopBar(title = residentName.ifBlank { "Resident" }, onBack = onBack, actions = {
            if (canManage) {
                PrimaryButton(
                    text = "Record payment",
                    onClick = { amount = ""; showPay = true },
                    size = ButtonSize.Small,
                )
            }
        }) },
    ) { padding ->
        when {
            state.loading && state.ledger == null -> LoadingState(
                Modifier.padding(padding).padding(horizontal = HamsaTheme.spacing.screenH, vertical = HamsaTheme.spacing.screenTop),
            )
            state.error != null && state.ledger == null -> ErrorState(state.error!!, Modifier.padding(padding), onRetry = viewModel::refresh)
            else -> Refreshable(
                refreshing = state.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding),
            ) {
                LazyColumn(
                modifier = Modifier.fillMaxSize().padding(HamsaTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
            ) {
                item {
                }
                state.ledger?.let { item { LedgerSummary(it) } }
                if (canManage) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                        ) {
                            SecondaryButton(
                                text = "Edit",
                                onClick = {
                                    editName = state.ledger?.fullName.orEmpty()
                                    editPhone = ""
                                    editPlan = null
                                    editRent = ""
                                    editDeposit = ""
                                    showEdit = true
                                },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = "Delete",
                                onClick = { showDelete = true },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        SecondaryButton(
                            text = "Move out & settle",
                            onClick = {
                                lastDay = ""; noticeDays = "30"; deductions = listOf(DeductionInput()); showMoveOut = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    SectionHead(
                        title = "Receipts",
                        modifier = Modifier.padding(top = HamsaTheme.spacing.xs),
                    )
                }
                if (state.receipts.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No receipts yet",
                            detail = "Recording a payment issues a receipt here.",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        )
                    }
                } else {
                    items(state.receipts, key = { it.id }) { r ->
                        ReceiptRow(
                            receipt = r,
                            downloading = state.downloadingId == r.id,
                            onDownload = { viewModel.downloadReceipt(r) },
                        )
                    }
                }
                }
            }
        }
    }

    if (showPay) {
        FormDialog(
            title = "Record payment",
            confirmText = "Record",
            confirmEnabled = amount.isNotBlank(),
            saving = state.saving,
            error = state.payError,
            onConfirm = { viewModel.recordPayment(amount) { showPay = false } },
            onDismiss = { showPay = false },
        ) {
            Text("A receipt is generated and stored automatically.", style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textTertiary)
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = amount, onValueChange = { amount = it }, label = "Amount (₹)", keyboardType = KeyboardType.Number)
        }
    }

    if (showMoveOut) {
        FormDialog(
            title = "Move out & settle",
            confirmText = "Settle",
            confirmEnabled = lastDay.isNotBlank(),
            saving = state.movingOut,
            error = state.moveOutError,
            onConfirm = { viewModel.moveOut(lastDay, noticeDays, deductions) { showMoveOut = false } },
            onDismiss = { showMoveOut = false },
        ) {
            HamsaTextField(value = lastDay, onValueChange = { lastDay = it }, label = "Last day (YYYY-MM-DD)")
            TextButton(onClick = { showDatePicker = true }) { Text("Pick a date…") }
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = noticeDays, onValueChange = { noticeDays = it }, label = "Notice days", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            Text("Deductions (optional)", style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.textSecondary)
            deductions.forEachIndexed { i, d ->
                HamsaTextField(
                    value = d.reason,
                    onValueChange = { v -> deductions = deductions.toMutableList().also { it[i] = it[i].copy(reason = v) } },
                    label = "Reason",
                )
                Spacer(Modifier.height(HamsaTheme.spacing.xxs))
                HamsaTextField(
                    value = d.amountRupees,
                    onValueChange = { v -> deductions = deductions.toMutableList().also { it[i] = it[i].copy(amountRupees = v) } },
                    label = "Amount (₹)",
                    keyboardType = KeyboardType.Number,
                )
                Spacer(Modifier.height(HamsaTheme.spacing.xs))
            }
            TextButton(onClick = { deductions = deductions + DeductionInput() }) { Text("+ Add deduction") }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                ModalBackdrop()
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                        lastDay = fmt.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dateState) }
    }

    if (showEdit) {
        FormDialog(
            title = "Edit resident",
            confirmText = "Save",
            confirmEnabled = editName.isNotBlank() || editPhone.isNotBlank() || editPlan != null
                || editRent.isNotBlank() || editDeposit.isNotBlank(),
            saving = state.editing,
            error = state.editError,
            onConfirm = {
                viewModel.editResident(
                    editName, editPhone, editPlan, editRent, editDeposit,
                ) { showEdit = false }
            },
            onDismiss = { showEdit = false },
        ) {
            Text(
                "Owners apply instantly; manager changes go to the owner for approval.",
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.textTertiary,
            )
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = editName, onValueChange = { editName = it }, label = "Full name")
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(
                value = editPhone,
                onValueChange = { editPhone = it },
                label = "Phone (leave blank to keep)",
                keyboardType = KeyboardType.Phone,
            )
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(
                value = editRent,
                onValueChange = { editRent = it },
                label = "Monthly rent ₹ (leave blank to keep)",
                keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(
                value = editDeposit,
                onValueChange = { editDeposit = it },
                label = "Deposit ₹ (leave blank to keep)",
                keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            Text("Plan", style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.xs)) {
                PlanKind.entries.forEach { p ->
                    SelectableChip(
                        label = p.label,
                        selected = editPlan == p,
                        onClick = { editPlan = if (editPlan == p) null else p },
                    )
                }
            }
        }
    }

    if (showDelete) {
        HamsaAlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete resident?", style = HamsaTheme.type.titleMedium) },
            text = {
                Text(
                    "The record is hidden, never wiped, and can be recovered. " +
                        "Manager deletions wait for the owner's approval.",
                    style = HamsaTheme.type.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteResident(onDeleted)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    state.settlement?.let { s ->
        HamsaAlertDialog(
            onDismissRequest = { viewModel.clearSettlement() },
            title = { Text(if (s.forfeited) "Deposit forfeited" else "Settlement", style = HamsaTheme.type.titleMedium) },
            text = {
                Column {
                    SettlementLine("Rent to date", Money.format(s.rentToDatePaise))
                    SettlementLine("Paid", Money.format(s.paidTotalPaise))
                    SettlementLine("Outstanding", Money.format(s.outstandingPaise))
                    SettlementLine("Deposit held", Money.format(s.depositHeldPaise))
                    SettlementLine("Deductions", Money.format(s.deductionsTotalPaise))
                    SettlementLine("Refund", Money.format(s.refundPaise))
                    if (s.note.isNotBlank()) {
                        Spacer(Modifier.height(HamsaTheme.spacing.xs))
                        Text(s.note, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textTertiary)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.clearSettlement() }) { Text("Done") } },
        )
    }
}

@Composable
private fun SettlementLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textSecondary)
        Text(value, style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.textPrimary)
    }
}

@Composable
private fun LedgerSummary(l: LedgerDto) {
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Icons.Outlined.Person,
                tone = if (l.outstandingPaise > 0) IconTone.Warning else IconTone.Success,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Outstanding", style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.textTertiary)
                Text(
                    Money.format(l.outstandingPaise),
                    style = HamsaTheme.type.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (l.outstandingPaise > 0) HamsaTheme.colors.warningFg else HamsaTheme.colors.successFg,
                )
            }
        }
        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Cell("Rent", Money.format(l.rentPaise))
            Cell("Accrued", Money.format(l.accruedPaise))
            Cell("Paid", Money.format(l.paidTotalPaise))
            Cell("Deposit", Money.format(l.depositPaise))
        }
    }
}

@Composable
private fun Cell(label: String, value: String) {
    Column {
        Text(label, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textTertiary)
        Text(value, style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.textPrimary)
    }
}

