package com.aalaya.pg.feature.occupancy

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.Floors
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.BranchDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.feature.residents.ResidentSheet
import com.aalaya.pg.ui.components.BedChip
import com.aalaya.pg.ui.components.BedState
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.DateField
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FieldLabel
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaDropdown
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.ModalBackdrop
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.ScopeChip
import com.aalaya.pg.ui.components.SearchField
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SegmentedControl
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.components.Tag
import com.aalaya.pg.ui.theme.HamsaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The occupancy explorer: pick a branch, then browse rooms and expand one to
 * see its beds. Free beds are tappable to register a resident straight into
 * them; occupied beds open the resident.
 *
 * The design also groups rooms by floor and filters by sharing/AC with a rent
 * shown on every bed. The backend's RoomDto carries only `id/branchId/number`
 * and BedDto has no price, so those controls are omitted rather than faked —
 * sharing is the one attribute that can be derived (from the bed count).
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OccupancyScreen(
    canManage: Boolean,
    onOpenResident: (id: String, name: String) -> Unit,
    onAddFloor: (branchId: String, branchName: String, existingFloors: Int) -> Unit = { _, _, _ -> },
    viewModel: OccupancyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    var query by remember { mutableStateOf("") }
    var showAddRoom by remember { mutableStateOf(false) }
    var newRoomNumber by remember { mutableStateOf("") }
    var newRoomCooling by remember { mutableStateOf("non_ac") }
    var newRoomBeds by remember { mutableStateOf(2) }
    var freeOnly by remember { mutableStateOf(false) }
    var sharingFilter by remember { mutableStateOf(0) }
    var coolingFilter by remember { mutableStateOf("all") }
    var registerTarget by remember { mutableStateOf<Triple<String, String, RegisterForm>?>(null) }
    /** Room id currently showing the "Add bed" dialog, if any. */
    var addBedTarget by remember { mutableStateOf<String?>(null) }
    var bedLabel by remember { mutableStateOf("") }

    val branchPicked = state.selectedBranchId != null

    // Search matches a room number, or the name of anyone in one of its beds.
    val normalized = query.trim().lowercase()
    fun roomMatches(room: RoomDto): Boolean {
        if (normalized.isEmpty()) return true
        if (room.number.lowercase().contains(normalized)) return true
        return state.bedsByRoom[room.id].orEmpty().any { bed ->
            bed.occupantName?.lowercase()?.contains(normalized) == true ||
                bed.label.lowercase().contains(normalized)
        }
    }
    fun sharingMatches(room: RoomDto): Boolean {
        if (sharingFilter == 0) return true
        // Unknown (not yet expanded) must NOT pass a specific filter -- showing
        // it optimistically then having it vanish once beds load (because the
        // real count didn't match) reads as "No rooms match" appearing out of
        // nowhere. Excluding it here is the honest answer: "not confirmed to match."
        val beds = state.bedsByRoom[room.id] ?: return false
        return beds.size == sharingFilter
    }

    // Branch → Floor → Room, revealed one step at a time: each dropdown only
    // appears once the one before it has been answered.
    val searching = normalized.isNotEmpty()
    val floorChosen = state.selectedFloor != null
    val floorKeys = Floors.keysOf(state.rooms.map { it.number })
    val roomsOnFloor = state.rooms.filter {
        state.selectedFloor == null ||
            state.selectedFloor == Floors.ALL ||
            Floors.keyOf(it.number) == state.selectedFloor
    }

    val visibleRooms = roomsOnFloor
        .filter { coolingFilter == "all" || it.coolingType == coolingFilter }
        .filter { roomMatches(it) && sharingMatches(it) }

    // Occupancy counts only cover rooms whose beds have been loaded.
    val knownBeds = state.bedsByRoom.values.flatten()
    val occupied = knownBeds.count { it.occupantResidentId != null }

    HamsaScreen(
        title = "Occupancy",
        // Dialog-window blur can be disabled by an Android device/emulator.
        // Blur the underlying explorer as a reliable visual fallback whenever
        // an occupied-bed detail sheet is open.
        modifier = Modifier.blur(
            if (state.sheet != null || showAddRoom) 8.dp else 0.dp,
        ),
        lead = when {
            state.isOwner && state.branches.isEmpty() ->
                "No branches found. Add a branch before tracking occupancy."
            !branchPicked -> "No branch is available."
            !floorChosen && !searching -> "Choose a floor to view rooms and beds."
            else -> "Expand a room to see beds and register a resident."
        },
        refreshing = state.loading && state.rooms.isNotEmpty(),
        onRefresh = viewModel::refresh,
        // Adding a floor is the one structural action that belongs here:
        // Occupancy is where someone notices a storey is missing. Rooms are
        // still created from a floor, on the add-floor form or the Floors
        // screen, so there is one place that owns each level.
        actions = if (canManage && branchPicked) {
            {
                PrimaryButton(
                    text = "Add floor",
                    onClick = {
                        onAddFloor(
                            state.selectedBranchId.orEmpty(),
                            state.selectedBranchName,
                            state.floorCount,
                        )
                    },
                    size = ButtonSize.Small,
                )
            }
        } else {
            null
        },
    ) {
        item("search") {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = if (branchPicked) {
                    "Search name, room or bed"
                } else "No branch available",
                enabled = branchPicked,
            )
        }

        // Branch is app-wide context, not a per-screen filter — the same chip
        // appears on Home, and changing it here changes it everywhere.
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

        // Filters and the bed list only make sense once the scope is settled —
        // a free-text search bypasses the floor step, as in the mockup.
        if (branchPicked && (floorChosen || searching)) {
            item("sharing") {
                Column {
                    FieldLabel("Sharing")
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    ) {
                        SelectableChip("Any", sharingFilter == 0, onClick = { sharingFilter = 0 })
                        (1..4).forEach { n ->
                            SelectableChip("$n-share", sharingFilter == n, onClick = { sharingFilter = n })
                        }
                    }
                }
            }

            item("cooling") {
                Column {
                    FieldLabel("Room type")
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    ) {
                        SelectableChip("Any", coolingFilter == "all", onClick = { coolingFilter = "all" })
                        SelectableChip("AC", coolingFilter == "ac", onClick = { coolingFilter = "ac" })
                        SelectableChip("Non-AC", coolingFilter == "non_ac", onClick = { coolingFilter = "non_ac" })
                    }
                }
            }

            item("mode") {
                SegmentedControl(
                    options = listOf("Rooms & beds", "Free beds only"),
                    selectedIndex = if (freeOnly) 1 else 0,
                    onSelect = { freeOnly = it == 1 },
                )
            }

            // Adding a room belongs to a specific floor, so this only appears
            // once one is selected — never under "All floors", where there
            // would be no floor to file the room onto.
            if (canManage && floorChosen && state.selectedFloor != Floors.ALL) {
                item("add-room") {
                    SecondaryButton(
                        text = "Add room to ${Floors.labelOf(state.selectedFloor.orEmpty())}",
                        onClick = {
                            newRoomNumber = suggestRoomNumber(
                                state.selectedFloor.orEmpty(),
                                roomsOnFloor.map { it.number },
                            )
                            newRoomCooling = "non_ac"
                            newRoomBeds = 2
                            showAddRoom = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            if (knownBeds.isNotEmpty()) {
                item("counts") {
                    val pct = occupied * 100 / knownBeds.size
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    ) {
                        StatusChip("$pct% occupied", StatusTone.Brand)
                        StatusChip("$occupied / ${knownBeds.size} beds", StatusTone.Neutral)
                        StatusChip("${knownBeds.size - occupied} vacant", StatusTone.Success)
                    }
                }
            }
        }

        when {
            state.isOwner && state.branches.isEmpty() -> item("no-branches") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No branches found",
                        detail = "Add a branch before tracking rooms, floors and beds.",
                        icon = Icons.Outlined.GridView,
                    )
                }
            }
            !branchPicked -> item("pick") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "Select a branch",
                        detail = "Then browse its rooms and beds.",
                        icon = Icons.Outlined.GridView,
                    )
                }
            }
            state.loading && state.rooms.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.rooms.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }
            // A floor selector is only useful once there is at least one
            // recognisable floor. Without this branch the screen says
            // "Select a floor" while deliberately rendering no dropdown.
            floorKeys.isEmpty() -> item("no-floors") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = if (state.rooms.isEmpty()) "No floors found" else "No usable floors found",
                        detail = if (canManage) {
                            "Add a room with a floor-style number (for example, 101) to get started."
                        } else {
                            "Your manager hasn't added any rooms to this branch yet."
                        },
                        icon = Icons.Outlined.GridView,
                    )
                }
            }
            !floorChosen && !searching -> item("pick-floor") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "Select a floor",
                        detail = "${state.selectedBranchName} — pick a floor above to continue.",
                        icon = Icons.Outlined.GridView,
                    )
                }
            }
            visibleRooms.isEmpty() -> item("empty") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = if (state.rooms.isEmpty()) "No rooms yet" else "No rooms match",
                        detail = when {
                            state.rooms.isNotEmpty() -> "Try a different name, room or filter."
                            canManage -> "Add the first room to start tracking beds."
                            else -> "Your manager hasn't added rooms yet."
                        },
                        icon = Icons.Outlined.GridView,
                    )
                }
            }
            else -> items(
                items = visibleRooms,
                key = { it.id },
            ) { room ->
                RoomCard(
                    room = room,
                    expanded = state.expandedRoomId == room.id,
                    beds = state.bedsByRoom[room.id],
                    freeOnly = freeOnly,
                    canManage = canManage,
                    onAddBed = { bedLabel = ""; addBedTarget = room.id },
                    onToggle = { viewModel.toggleRoom(room.id) },
                    onBedClick = { bed ->
                        val residentId = bed.occupantResidentId
                        if (residentId != null) {
                            // A sheet, not a new screen — the mockup's
                            // openBedDetail keeps the explorer underneath.
                            viewModel.openResidentSheet(
                                residentId = residentId,
                                name = bed.occupantName.orEmpty(),
                                bedLabel = "${room.number}-${bed.label}",
                            )
                        } else if (canManage) {
                            registerTarget = Triple(room.id, bed.id, RegisterForm())
                        }
                    },
                )
            }
        }
    }


    if (showAddRoom) {
        FormDialog(
            title = "Add room to ${Floors.labelOf(state.selectedFloor.orEmpty())}",
            confirmText = "Add room",
            confirmEnabled = newRoomNumber.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = {
                viewModel.addRoom(newRoomNumber, newRoomCooling, newRoomBeds) {
                    showAddRoom = false
                }
            },
            onDismiss = { showAddRoom = false; viewModel.clearSaveError() },
        ) {
            HamsaTextField(
                value = newRoomNumber,
                onValueChange = { newRoomNumber = it.take(20) },
                label = "Room number",
                placeholder = "e.g. 204",
            )
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            Text("Room type", style = HamsaTheme.type.labelMedium, color = colors.textSecondary)
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
                SelectableChip(
                    label = "AC",
                    selected = newRoomCooling == "ac",
                    onClick = { newRoomCooling = "ac" },
                    modifier = Modifier.weight(1f),
                )
                SelectableChip(
                    label = "Non-AC",
                    selected = newRoomCooling == "non_ac",
                    onClick = { newRoomCooling = "non_ac" },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sharing", style = HamsaTheme.type.labelMedium, color = colors.textSecondary)
                    Text(
                        text = if (newRoomBeds == 1) "Single room" else "$newRoomBeds people share",
                        style = HamsaTheme.type.bodySmall,
                        color = colors.textTertiary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                ) {
                    SecondaryButton(
                        text = "",
                        onClick = { newRoomBeds = (newRoomBeds - 1).coerceAtLeast(1) },
                        enabled = newRoomBeds > 1 && !state.saving,
                        size = ButtonSize.Small,
                        leading = {
                            Icon(
                                Icons.Outlined.Remove,
                                contentDescription = "One fewer bed",
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Text(
                        newRoomBeds.toString(),
                        style = HamsaTheme.type.titleMedium,
                        color = colors.textPrimary,
                    )
                    SecondaryButton(
                        text = "",
                        onClick = { newRoomBeds = (newRoomBeds + 1).coerceAtMost(26) },
                        enabled = newRoomBeds < 26 && !state.saving,
                        size = ButtonSize.Small,
                        leading = {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "One more bed",
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
    }

    addBedTarget?.let { roomId ->
        FormDialog(
            title = "Add bed",
            confirmText = "Create",
            confirmEnabled = bedLabel.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = {
                viewModel.addBed(roomId, bedLabel) { addBedTarget = null; bedLabel = "" }
            },
            onDismiss = { addBedTarget = null; bedLabel = "" },
        ) {
            HamsaTextField(
                value = bedLabel,
                onValueChange = { bedLabel = it },
                label = "Bed label",
                placeholder = "e.g. C",
            )
        }
    }

    state.sheet?.let { sheet ->
        ResidentSheet(
            residentName = sheet.name,
            bedLabel = sheet.bedLabel,
            ledger = sheet.ledger,
            loading = sheet.loading,
            onDismiss = viewModel::closeResidentSheet,
            onOpenFull = {
                viewModel.closeResidentSheet()
                onOpenResident(sheet.residentId, sheet.name)
            },
        )
    }

    registerTarget?.let { (roomId, bedId, form) ->
        RegisterResidentDialog(
            form = form,
            saving = state.saving,
            error = state.saveError,
            onFormChange = { registerTarget = Triple(roomId, bedId, it) },
            onConfirm = { viewModel.registerResident(roomId, bedId, form) { registerTarget = null } },
            onDismiss = { registerTarget = null },
        )
    }
}

/** `.room` — a tappable summary that expands to reveal the bed grid. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomCard(
    room: RoomDto,
    expanded: Boolean,
    beds: List<BedDto>?,
    freeOnly: Boolean,
    canManage: Boolean,
    onAddBed: () -> Unit,
    onToggle: () -> Unit,
    onBedClick: (BedDto) -> Unit,
) {
    val colors = HamsaTheme.colors
    val free = beds?.count { it.occupantResidentId == null }
    val shown = if (freeOnly) beds?.filter { it.occupantResidentId == null } else beds

    HamsaCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
        ) {
            IconTile(
                icon = Icons.Outlined.Bed,
                tone = if (beds != null && beds.isNotEmpty() && free == 0) {
                    IconTone.Warning
                } else {
                    IconTone.Brand
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Room ${room.number}",
                        style = HamsaTheme.type.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (beds != null) {
                        val noBeds = beds.isEmpty()
                        Text(
                            text = when {
                                noBeds -> "No beds"
                                free == 0 -> "Full"
                                else -> "$free free"
                            },
                            style = HamsaTheme.type.labelMedium.copy(
                                fontWeight = if (!noBeds && free == 0) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = if (!noBeds && free == 0) colors.warningFg else colors.textTertiary,
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Tag(if (room.coolingType == "ac") "AC" else "Non-AC")
                }
                if (beds != null) {
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Tag("${beds.size}-sharing")
                        Tag("${beds.size - (free ?: 0)}/${beds.size} occupied")
                    }
                }
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = colors.brand,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }

        if (expanded) {
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            when {
                beds == null -> Text(
                    "Loading beds…",
                    style = HamsaTheme.type.bodySmall,
                    color = colors.textTertiary,
                )
                shown.isNullOrEmpty() -> Text(
                    if (freeOnly) "No free beds in this room." else "No beds yet.",
                    style = HamsaTheme.type.bodySmall,
                    color = colors.textTertiary,
                )
                else -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                ) {
                    shown.forEach { bed ->
                        BedChip(
                            bedId = "${room.number}-${bed.label}",
                            state = if (bed.occupantResidentId != null) {
                                BedState.Occupied
                            } else {
                                BedState.Free
                            },
                            occupantName = bed.occupantName,
                            onClick = { onBedClick(bed) },
                        )
                    }
                }
            }
            if (canManage && beds != null) {
                Spacer(Modifier.height(HamsaTheme.spacing.sm))
                Text(
                    "+ Add bed",
                    style = HamsaTheme.type.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.brand,
                    modifier = Modifier.clickable(onClick = onAddBed),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterResidentDialog(
    form: RegisterForm,
    saving: Boolean,
    error: String?,
    onFormChange: (RegisterForm) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    FormDialog(
        title = "Invite tenant",
        confirmText = "Send invitation",
        confirmEnabled = form.email.contains('@') && form.fullName.isNotBlank(),
        saving = saving,
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        HamsaTextField(
            value = form.email,
            onValueChange = { onFormChange(form.copy(email = it)) },
            label = "Email",
            placeholder = "tenant@example.com",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(HamsaTheme.spacing.stack))
        HamsaTextField(
            value = form.fullName,
            onValueChange = { onFormChange(form.copy(fullName = it)) },
            label = "Full name (exactly as on Aadhaar)",
            placeholder = "e.g. RAVI KUMAR B",
            helpText = "Records always match the real person.",
        )
        Spacer(Modifier.height(HamsaTheme.spacing.stack))
        FieldLabel("Tenant type")
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
        ) {
            listOf(
                com.aalaya.pg.core.domain.model.PlanKind.Daily,
                com.aalaya.pg.core.domain.model.PlanKind.Weekly,
                com.aalaya.pg.core.domain.model.PlanKind.Monthly,
            ).forEach { plan ->
                SelectableChip(
                    label = plan.label,
                    selected = form.planKind == plan,
                    onClick = { onFormChange(form.copy(planKind = plan)) },
                )
            }
        }
        Spacer(Modifier.height(HamsaTheme.spacing.stack))
        HamsaTextField(
            value = form.rentRupees,
            onValueChange = { onFormChange(form.copy(rentRupees = it)) },
            label = "Rent per bed (₹)",
            keyboardType = KeyboardType.Number,
        )
        Spacer(Modifier.height(HamsaTheme.spacing.stack))
        HamsaTextField(
            value = form.depositRupees,
            onValueChange = { onFormChange(form.copy(depositRupees = it)) },
            label = "Security deposit (₹)",
            keyboardType = KeyboardType.Number,
            helpText = "Recorded separately from rent.",
        )
        Spacer(Modifier.height(HamsaTheme.spacing.stack))
        DateField(
            label = "Planned move-in date",
            value = form.moveInDate.ifBlank { null },
            placeholder = "Select a date",
            onClick = { showDatePicker = true },
        )
        Spacer(Modifier.height(HamsaTheme.spacing.stack))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("DigiLocker verification", style = HamsaTheme.type.bodyLarge, color = HamsaTheme.colors.textPrimary)
                Text("Require identity verification before approval", style = HamsaTheme.type.labelMedium, color = HamsaTheme.colors.textSecondary)
            }
            Switch(checked = form.requireKyc, onCheckedChange = { onFormChange(form.copy(requireKyc = it)) })
        }
    }

    if (showDatePicker) {
        val initialDate = form.moveInDate.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                ModalBackdrop()
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onFormChange(form.copy(moveInDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()))
                    }
                    showDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

/**
 * The next unused room number on a floor: with 201 and 202 present, suggests
 * 203. Falls back to empty when the floor key is not numeric (a block, say),
 * where no convention can be assumed.
 */
private fun suggestRoomNumber(floorKey: String, existing: List<String>): String {
    if (floorKey.isEmpty() || !floorKey.all { it.isDigit() }) return ""
    val taken = existing.toSet()
    for (n in 1..99) {
        val candidate = "$floorKey${"%02d".format(n)}"
        if (candidate !in taken) return candidate
    }
    return ""
}
