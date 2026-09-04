package com.aalaya.pg.feature.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.Floors
import com.aalaya.pg.core.network.dto.BranchDto
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.DestructiveButton
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaDropdown
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.HamsaSheet
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.KeyValueRow
import com.aalaya.pg.ui.components.ListRow
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.ScopeChip
import com.aalaya.pg.ui.components.SearchField
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.components.SegmentedControl
import com.aalaya.pg.ui.components.StatGrid
import com.aalaya.pg.ui.components.StatTile
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * Who's paid up and who owes. Cash is confirmed at the desk, then marked paid
 * from the resident's detail screen — this is the branch-wide view onto that.
 */
private const val ALL_ROOMS = "__all_rooms__"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentsScreen(
    onOpenResident: (id: String, name: String) -> Unit,
    viewModel: PaymentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) } // 0 = all, 1 = due, 2 = paid
    var confirmFor by remember { mutableStateOf<ResidentDue?>(null) }
    var unpaidFor by remember { mutableStateOf<ResidentDue?>(null) }
    var sheetFor by remember { mutableStateOf<ResidentDue?>(null) }

    val branchPicked = state.selectedBranchId != null
    val normalized = query.trim().lowercase()

    // Branch → Floor → Room, revealed one step at a time, same as Occupancy.
    val searching = normalized.isNotEmpty()
    val floorChosen = state.selectedFloor != null
    val floorKeys = Floors.keysOf(state.rooms.map { it.number })
    val roomsOnFloor = state.rooms.filter {
        state.selectedFloor == null ||
            state.selectedFloor == Floors.ALL ||
            Floors.keyOf(it.number) == state.selectedFloor
    }

    val matching = state.dues
        .filter { due ->
            state.selectedFloor == null ||
                state.selectedFloor == Floors.ALL ||
                (due.roomNumber != null && Floors.keyOf(due.roomNumber) == state.selectedFloor)
        }
        .filter { due -> state.selectedRoomId == null || due.roomId == state.selectedRoomId }
        .filter {
            normalized.isEmpty() ||
                it.resident.fullName.lowercase().contains(normalized) ||
                it.roomNumber?.lowercase()?.contains(normalized) == true
        }
    val due = matching.filter { it.outstandingPaise > 0 }
    val paid = matching.filter { it.outstandingPaise <= 0 }
    val listed = when (filter) {
        1 -> due
        2 -> paid
        else -> due + paid
    }
    val totalOutstanding = due.sumOf { it.outstandingPaise }

    HamsaScreen(
        title = "Payments",
        lead = when {
            state.isOwner && state.branches.isEmpty() ->
                "No branches found. Add a branch before tracking payments."
            !branchPicked -> "No branch is available."
            !floorChosen && !searching -> "Choose a floor to view payments."
            else -> "Confirm cash, then mark paid."
        },
        refreshing = state.loading && state.dues.isNotEmpty(),
        onRefresh = viewModel::refresh,
    ) {
        item("search") {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = if (branchPicked) "Search resident" else "No branch available",
                enabled = branchPicked,
            )
        }

        // Same shared branch context as Home and Occupancy.
        if (state.isOwner && state.branches.isNotEmpty()) {
            item("branch") {
                ScopeChip(
                    label = state.selectedBranchName.ifBlank { state.branches.first().name },
                    options = state.branches,
                    optionLabel = BranchDto::name,
                    onSelect = { viewModel.selectBranch(it.id, it.name) },
                )
            }
        }

        // Step 2 — only once a branch is chosen.
        if (branchPicked && floorKeys.isNotEmpty()) {
            item("floor") {
                HamsaDropdown(
                    label = "Floor",
                    options = floorKeys + Floors.ALL,
                    selected = state.selectedFloor,
                    placeholder = "Select floor…",
                    optionLabel = { key ->
                        if (key == Floors.ALL) "All floors" else Floors.labelOf(key)
                    },
                    onSelect = { viewModel.selectFloor(it) },
                )
            }
        }

        // Step 3 — only once a floor is chosen.
        if (branchPicked && floorChosen) {
            item("room") {
                HamsaDropdown(
                    label = "Room",
                    options = listOf(ALL_ROOMS) + roomsOnFloor.map { it.id },
                    selected = state.selectedRoomId ?: ALL_ROOMS,
                    optionLabel = { id ->
                        if (id == ALL_ROOMS) {
                            "All rooms (${roomsOnFloor.size})"
                        } else {
                            "Room ${roomsOnFloor.firstOrNull { r -> r.id == id }?.number.orEmpty()}"
                        }
                    },
                    onSelect = { viewModel.selectRoom(it.takeIf { id -> id != ALL_ROOMS }) },
                )
            }
        }

        when {
            state.isOwner && state.branches.isEmpty() -> item("no-branches") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No branches found",
                        detail = "Add a branch before tracking residents and payments.",
                        icon = Icons.Outlined.Payments,
                    )
                }
            }
            !branchPicked -> item("pick") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "Select a branch",
                        detail = "Then see who's paid up and who owes.",
                        icon = Icons.Outlined.Payments,
                    )
                }
            }
            state.loading && state.dues.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.dues.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }
            // The floor picker is hidden when the branch has no rooms, so
            // never ask the user to select a floor in that state.
            floorKeys.isEmpty() && !searching -> item("no-floors") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = if (state.rooms.isEmpty()) "No floors found" else "No usable floors found",
                        detail = "Add rooms with floor-style numbers (for example, 101) to organise payments by floor.",
                        icon = Icons.Outlined.Payments,
                    )
                }
            }
            !floorChosen && !searching -> item("pick-floor") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "Select a floor",
                        detail = "${state.selectedBranchName} — pick a floor above to continue.",
                        icon = Icons.Outlined.Payments,
                    )
                }
            }
            else -> {
                item("counts") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    ) {
                        StatusChip("${due.size} due", StatusTone.Warning)
                        StatusChip("${paid.size} paid up", StatusTone.Success)
                    }
                }

                item("stats") {
                    StatGrid(
                        tiles = listOf(
                            {
                                StatTile(
                                    overline = "Still owed",
                                    value = Money.format(totalOutstanding),
                                    caption = "${due.size} due",
                                    valueColor = colors.warningFg,
                                )
                            },
                            {
                                StatTile(
                                    overline = "Residents",
                                    value = matching.size.toString(),
                                    caption = "${paid.size} paid up",
                                )
                            },
                        ),
                    )
                }

                item("filter") {
                    SegmentedControl(
                        options = listOf(
                            "All (${matching.size})",
                            "Due (${due.size})",
                            "Paid (${paid.size})",
                        ),
                        selectedIndex = filter,
                        onSelect = { filter = it },
                    )
                }

                item("list") {
                    SectionHead(
                        title = state.selectedBranchName.ifBlank { "Residents" },
                    )
                }

                if (listed.isEmpty()) {
                    item("none") {
                        HamsaCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                title = if (state.dues.isEmpty()) {
                                    "No payment records found"
                                } else {
                                    "No residents match"
                                },
                                detail = if (state.dues.isEmpty()) {
                                    "This branch has no residents with payment records yet."
                                } else {
                                    "Try a different search or filter."
                                },
                                icon = Icons.Outlined.Payments,
                            )
                        }
                    }
                } else {
                    item("rows") {
                        HamsaCard(modifier = Modifier.fillMaxWidth()) {
                            listed.forEachIndexed { index, item ->
                                val owing = item.outstandingPaise > 0
                                val bed = listOfNotNull(item.roomNumber, item.bedLabel)
                                    .takeIf { it.isNotEmpty() }?.joinToString("-")
                                ListRow(
                                    title = listOfNotNull(item.resident.fullName, bed)
                                        .joinToString(" · "),
                                    subtitle = if (owing) {
                                        "${Money.format(item.outstandingPaise)} due"
                                    } else {
                                        "Paid up"
                                    },
                                    showTopDivider = index > 0,
                                    onClick = { sheetFor = item },
                                    // The tile's tone is the status: amber when
                                    // money is owed, brand blue when settled.
                                    leading = {
                                        IconTile(
                                            icon = Icons.Outlined.CurrencyRupee,
                                            tone = if (owing) IconTone.Warning else IconTone.Brand,
                                        )
                                    },
                                    trailing = {
                                        // "Mark paid" sits in the row itself, as
                                        // in the mockup — collecting cash is the
                                        // whole point of this screen. Once paid,
                                        // the same slot offers the reversal, so a
                                        // mis-tap is correctable from where it
                                        // happened.
                                        when {
                                            owing -> PrimaryButton(
                                                text = "Mark paid",
                                                onClick = { confirmFor = item },
                                                size = ButtonSize.Small,
                                                loading = state.markingPaidId == item.resident.id,
                                            )
                                            item.paidTotalPaise > 0 -> SecondaryButton(
                                                text = "Mark unpaid",
                                                onClick = { unpaidFor = item },
                                                size = ButtonSize.Small,
                                                enabled = state.markingUnpaidId != item.resident.id,
                                            )
                                            // Settled with nothing ever received
                                            // (no rent accrued yet): there is no
                                            // payment to reverse, so only status.
                                            else -> StatusChip(
                                                label = "Paid",
                                                tone = StatusTone.Success,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm before recording cash, mirroring the mockup's receipt preview.
    confirmFor?.let { target ->
        HamsaSheet(title = "Confirm payment", onDismiss = { confirmFor = null }) {
            Text(
                "Cash received at the desk? Confirm to mark it paid and issue a " +
                    "receipt to the resident.",
                style = HamsaTheme.type.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = HamsaTheme.spacing.md),
            )
            KeyValueRow("From", target.resident.fullName)
            listOfNotNull(target.roomNumber, target.bedLabel).takeIf { it.isNotEmpty() }?.let {
                KeyValueRow("Bed", it.joinToString("-"))
            }
            KeyValueRow("Mode", "Cash")
            KeyValueRow(
                label = "Amount",
                value = Money.format(target.outstandingPaise),
                valueColor = colors.brand,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = HamsaTheme.spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = { confirmFor = null },
                    size = ButtonSize.Small,
                )
                PrimaryButton(
                    text = "Confirm & mark paid",
                    onClick = {
                        viewModel.markPaid(target.resident.id, target.outstandingPaise) {
                            confirmFor = null
                        }
                    },
                    loading = state.markingPaidId == target.resident.id,
                    size = ButtonSize.Small,
                )
            }
        }
    }

    // Reversing money is confirmed too: it withdraws a receipt the resident may
    // already have seen, so it should never happen on a single stray tap.
    unpaidFor?.let { target ->
        HamsaSheet(title = "Reverse payment", onDismiss = { unpaidFor = null }) {
            Text(
                "This undoes the most recent payment for this resident and " +
                    "withdraws its receipt. The balance returns to what it was. " +
                    "The reversal is recorded in the audit log.",
                style = HamsaTheme.type.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = HamsaTheme.spacing.md),
            )
            KeyValueRow("Resident", target.resident.fullName)
            KeyValueRow(
                label = "Received so far",
                value = Money.format(target.paidTotalPaise),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = HamsaTheme.spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = { unpaidFor = null },
                    size = ButtonSize.Small,
                )
                DestructiveButton(
                    text = "Mark unpaid",
                    onClick = {
                        viewModel.markUnpaid(target.resident.id) { unpaidFor = null }
                    },
                    enabled = state.markingUnpaidId != target.resident.id,
                    size = ButtonSize.Small,
                )
            }
        }
    }

    // Tapping the row opens a summary sheet rather than pushing a new screen.
    sheetFor?.let { target ->
        HamsaSheet(
            title = target.resident.fullName,
            onDismiss = { sheetFor = null },
            trailing = {
                StatusChip(
                    label = if (target.outstandingPaise > 0) "Due" else "Paid up",
                    tone = if (target.outstandingPaise > 0) StatusTone.Warning else StatusTone.Success,
                )
            },
        ) {
            listOfNotNull(target.roomNumber, target.bedLabel).takeIf { it.isNotEmpty() }?.let {
                KeyValueRow("Bed", it.joinToString("-"))
            }
            KeyValueRow("Plan", target.resident.planKind.replaceFirstChar { c -> c.uppercase() })
            if (target.rentPaise > 0) KeyValueRow("Rent", Money.format(target.rentPaise) + " / month")
            KeyValueRow(
                label = "Outstanding",
                value = Money.format(target.outstandingPaise),
                valueColor = if (target.outstandingPaise > 0) colors.errorFg else colors.successFg,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = HamsaTheme.spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            ) {
                if (target.outstandingPaise > 0) {
                    PrimaryButton(
                        text = "Mark paid",
                        onClick = { sheetFor = null; confirmFor = target },
                        size = ButtonSize.Small,
                    )
                } else if (target.paidTotalPaise > 0) {
                    SecondaryButton(
                        text = "Mark unpaid",
                        onClick = { sheetFor = null; unpaidFor = target },
                        size = ButtonSize.Small,
                    )
                }
                SecondaryButton(
                    text = "Full record",
                    onClick = {
                        sheetFor = null
                        onOpenResident(target.resident.id, target.resident.fullName)
                    },
                    size = ButtonSize.Small,
                )
            }
        }
    }
}
