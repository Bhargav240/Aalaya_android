package com.aalaya.pg.feature.rooms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.ResidentDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.ui.components.BedChip
import com.aalaya.pg.ui.components.BedState
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaAlertDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaDropdown
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.HamsaTopBar
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.Refreshable
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.theme.HamsaTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomsScreen(
    branchName: String,
    canManage: Boolean,
    onBack: () -> Unit = {},
    viewModel: RoomsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddRoom by remember { mutableStateOf(false) }
    var roomNumber by remember { mutableStateOf("") }
    var roomCoolingType by remember { mutableStateOf("non_ac") }
    var addBedFor by remember { mutableStateOf<String?>(null) }
    var bedLabel by remember { mutableStateOf("") }
    var renameRoom by remember { mutableStateOf<RoomDto?>(null) }
    var renameNumber by remember { mutableStateOf("") }
    var renameCoolingType by remember { mutableStateOf("non_ac") }
    var deleteRoomId by remember { mutableStateOf<String?>(null) }
    var assignBedTarget by remember { mutableStateOf<Pair<String, BedDto>?>(null) } // roomId to bed
    var pendingMove by remember { mutableStateOf<PendingMove?>(null) }
    var coolingFilter by remember { mutableStateOf("all") }
    val visibleRooms = state.rooms.filter { coolingFilter == "all" || it.coolingType == coolingFilter }

    Scaffold(
        containerColor = HamsaTheme.colors.canvas,
        topBar = { HamsaTopBar(title = "Rooms & beds", onBack = onBack, actions = {
            if (canManage) {
                PrimaryButton(
                    text = "Add room",
                    onClick = { roomNumber = ""; roomCoolingType = "non_ac"; showAddRoom = true },
                    size = ButtonSize.Small,
                )
            }
        }) },
    ) { padding ->
        Refreshable(
            refreshing = state.loading && state.rooms.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.loading && state.rooms.isEmpty() -> LoadingState(Modifier.padding(horizontal = HamsaTheme.spacing.screenH, vertical = HamsaTheme.spacing.screenTop))
                state.error != null && state.rooms.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel::refresh)
                state.rooms.isEmpty() -> HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No rooms yet",
                        detail = if (canManage) {
                            "Add a room, then give it beds to place residents in."
                        } else {
                            "This branch has no rooms yet."
                        },
                        icon = Icons.Outlined.GridView,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(HamsaTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                ) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
                            SelectableChip("Any", coolingFilter == "all", onClick = { coolingFilter = "all" })
                            SelectableChip("AC", coolingFilter == "ac", onClick = { coolingFilter = "ac" })
                            SelectableChip("Non-AC", coolingFilter == "non_ac", onClick = { coolingFilter = "non_ac" })
                        }
                    }
                    items(visibleRooms, key = { it.id }) { room ->
                        RoomCard(
                            room = room,
                            expanded = state.expandedRoomId == room.id,
                            beds = state.bedsByRoom[room.id],
                            canManage = canManage,
                            onToggle = { viewModel.toggleRoom(room.id) },
                            onAddBed = { addBedFor = room.id; bedLabel = "" },
                            onRename = { renameNumber = room.number; renameCoolingType = room.coolingType; renameRoom = room },
                            onDelete = { deleteRoomId = room.id },
                            onBedClick = { bed ->
                                if (canManage) {
                                    viewModel.ensureResidents()
                                    viewModel.ensureAllBedsLoaded()
                                    assignBedTarget = room.id to bed
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddRoom) {
        FormDialog(
            title = "Add room",
            confirmText = "Create",
            confirmEnabled = roomNumber.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.addRoom(roomNumber, roomCoolingType) { showAddRoom = false } },
            onDismiss = { showAddRoom = false },
        ) {
            HamsaTextField(value = roomNumber, onValueChange = { roomNumber = it }, label = "Room number")
            Row {
                TextButton(onClick = { roomCoolingType = "ac" }) { Text(if (roomCoolingType == "ac") "✓ AC" else "AC") }
                TextButton(onClick = { roomCoolingType = "non_ac" }) { Text(if (roomCoolingType == "non_ac") "✓ Non-AC" else "Non-AC") }
            }
        }
    }

    addBedFor?.let { roomId ->
        FormDialog(
            title = "Add bed",
            confirmText = "Create",
            confirmEnabled = bedLabel.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.addBed(roomId, bedLabel) { addBedFor = null } },
            onDismiss = { addBedFor = null },
        ) {
            HamsaTextField(value = bedLabel, onValueChange = { bedLabel = it }, label = "Bed label (e.g. A, B)")
        }
    }

    renameRoom?.let { room ->
        FormDialog(
            title = "Rename room",
            confirmText = "Save",
            confirmEnabled = renameNumber.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.renameRoom(room.id, renameNumber, renameCoolingType) { renameRoom = null } },
            onDismiss = { renameRoom = null },
        ) {
            HamsaTextField(value = renameNumber, onValueChange = { renameNumber = it }, label = "Room number")
            Row {
                TextButton(onClick = { renameCoolingType = "ac" }) { Text(if (renameCoolingType == "ac") "✓ AC" else "AC") }
                TextButton(onClick = { renameCoolingType = "non_ac" }) { Text(if (renameCoolingType == "non_ac") "✓ Non-AC" else "Non-AC") }
            }
        }
    }

    deleteRoomId?.let { id ->
        HamsaAlertDialog(
            onDismissRequest = { deleteRoomId = null },
            title = { Text("Delete room?", style = HamsaTheme.type.titleMedium) },
            text = {
                Text(
                    "The room is hidden, never wiped. Manager deletions wait for the owner's approval.",
                    style = HamsaTheme.type.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteRoomId = null; viewModel.deleteRoom(id) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteRoomId = null }) { Text("Cancel") } },
        )
    }

    assignBedTarget?.let { (roomId, bed) ->
        HamsaAlertDialog(
            onDismissRequest = { assignBedTarget = null },
            title = { Text("Bed ${bed.label}", style = HamsaTheme.type.titleMedium) },
            text = {
                Column {
                    if (bed.occupantName != null) {
                        Text(
                            "Occupied by ${bed.occupantName}.",
                            style = HamsaTheme.type.bodySmall,
                            color = HamsaTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(HamsaTheme.spacing.xs))
                        TextButton(onClick = {
                            val occupant = bed.occupantResidentId
                            if (occupant != null) {
                                viewModel.assignBed(roomId, null, occupant) { assignBedTarget = null }
                            }
                        }) { Text("Unassign") }
                    } else {
                        Text(
                            "Assign a resident to this bed:",
                            style = HamsaTheme.type.bodySmall,
                            color = HamsaTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(HamsaTheme.spacing.xs))
                        if (state.residents.isEmpty()) {
                            Text(
                                "No active residents in this branch.",
                                style = HamsaTheme.type.bodySmall,
                                color = HamsaTheme.colors.textTertiary,
                            )
                        } else {
                            state.residents.forEach { res ->
                                val elsewhere = currentBedOf(state, res.id)
                                TextButton(onClick = {
                                    if (elsewhere != null) {
                                        pendingMove = PendingMove(roomId, bed, res, elsewhere)
                                    } else {
                                        viewModel.assignBed(roomId, bed.id, res.id) { assignBedTarget = null }
                                    }
                                }) {
                                    Column {
                                        Text(res.fullName)
                                        if (elsewhere != null) {
                                            Text(
                                                "Currently Room ${elsewhere.first} · Bed ${elsewhere.second}",
                                                style = HamsaTheme.type.labelMedium,
                                                color = HamsaTheme.colors.textTertiary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    state.saveError?.let {
                        Spacer(Modifier.height(HamsaTheme.spacing.xs))
                        Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.errorFg)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { assignBedTarget = null }) { Text("Close") } },
        )
    }

    pendingMove?.let { move ->
        HamsaAlertDialog(
            onDismissRequest = { pendingMove = null },
            title = { Text("Move ${move.resident.fullName}?", style = HamsaTheme.type.titleMedium) },
            text = {
                Text(
                    "They're currently in Room ${move.elsewhere.first} · Bed ${move.elsewhere.second}. " +
                        "Assigning them to Bed ${move.bed.label} here will free that bed.",
                    style = HamsaTheme.type.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.assignBed(move.roomId, move.bed.id, move.resident.id) {
                        pendingMove = null
                        assignBedTarget = null
                    }
                }) { Text("Move") }
            },
            dismissButton = { TextButton(onClick = { pendingMove = null }) { Text("Cancel") } },
        )
    }
}

private data class PendingMove(
    val roomId: String,
    val bed: BedDto,
    val resident: ResidentDto,
    /** (room number, bed label) the resident is currently occupying. */
    val elsewhere: Pair<String, String>,
)

/** Which room+bed a resident currently occupies, if any bed data is loaded for it. */
private fun currentBedOf(state: RoomsUiState, residentId: String): Pair<String, String>? {
    for (room in state.rooms) {
        val bed = state.bedsByRoom[room.id]?.firstOrNull { it.occupantResidentId == residentId }
        if (bed != null) return room.number to bed.label
    }
    return null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomCard(
    room: RoomDto,
    expanded: Boolean,
    beds: List<BedDto>?,
    canManage: Boolean,
    onToggle: () -> Unit,
    onAddBed: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBedClick: (BedDto) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    HamsaCard(modifier = Modifier.fillMaxWidth().clickable { onToggle() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Outlined.Bed)
            Text(
                "Room ${room.number} · ${if (room.coolingType == "ac") "AC" else "Non-AC"}",
                style = HamsaTheme.type.titleMedium,
                color = HamsaTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                beds?.let {
                    Text(
                        "${it.size} beds · ${it.count { b -> b.occupantResidentId != null }} occupied",
                        style = HamsaTheme.type.bodySmall,
                        color = HamsaTheme.colors.textTertiary,
                    )
                }
                if (canManage) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = "Room actions",
                                tint = HamsaTheme.colors.textTertiary,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { menuOpen = false; onRename() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            when (beds) {
                null -> Text("Loading beds…", style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textTertiary)
                else -> {
                    if (beds.isEmpty()) {
                        Text("No beds yet.", style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textTertiary)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.xs)) {
                            beds.forEach { bed ->
                                BedChip(
                                    bedId = bed.label,
                                    state = if (bed.occupantResidentId != null) BedState.Occupied else BedState.Free,
                                    occupantName = bed.occupantName,
                                    onClick = { onBedClick(bed) },
                                )
                            }
                        }
                        if (canManage) {
                            Text(
                                "Tap a bed to assign or free it.",
                                style = HamsaTheme.type.bodySmall,
                                color = HamsaTheme.colors.textTertiary,
                            )
                        }
                    }
                    if (canManage) {
                        TextButton(onClick = onAddBed) { Text("+ Add bed") }
                    }
                }
            }
        }
    }
}
