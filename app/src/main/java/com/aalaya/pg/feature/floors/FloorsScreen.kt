package com.aalaya.pg.feature.floors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.FloorDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaAlertDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.ListRow
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * Floors of one branch, with the rooms filed on each.
 *
 * Only an owner reaches this screen — the server refuses floor writes from
 * anyone else, and offering buttons that always 403 would be worse than not
 * showing them.
 *
 * Rooms created before floors existed appear under "Not on a floor yet". They
 * are not broken: the rest of the app still locates them by room number. This
 * screen simply gives the owner somewhere to finish the job.
 */
@Composable
fun FloorsScreen(
    branchId: String,
    branchName: String,
    onBack: () -> Unit,
    viewModel: FloorsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    LaunchedEffect(branchId) { viewModel.start(branchId) }

    var showAdd by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<FloorDto?>(null) }
    var deleting by remember { mutableStateOf<FloorDto?>(null) }
    var name by remember { mutableStateOf("") }
    // Adding a room or bed is scoped to whatever the user tapped, so the
    // parent is carried rather than picked from a list.
    var addRoomOn by remember { mutableStateOf<FloorDto?>(null) }
    var addBedIn by remember { mutableStateOf<RoomDto?>(null) }
    var roomNumber by remember { mutableStateOf("") }
    var roomCooling by remember { mutableStateOf("non_ac") }
    var bedLabel by remember { mutableStateOf("") }

    HamsaScreen(
        title = "Floors",
        // Keep the underlying floor list visually de-emphasised while the
        // add-room form is open, matching the tenant-details modal treatment.
        modifier = Modifier.blur(if (addRoomOn != null) 8.dp else 0.dp),
        lead = branchName,
        onBack = onBack,
        refreshing = state.loading && state.floors.isNotEmpty(),
        onRefresh = viewModel::refresh,
        actions = {
            PrimaryButton(
                text = "Add floor",
                onClick = { name = ""; showAdd = true },
                size = ButtonSize.Small,
            )
        },
    ) {
        when {
            state.loading && state.floors.isEmpty() -> item("loading") { LoadingState() }

            state.error != null && state.floors.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }

            state.floors.isEmpty() && state.unassignedRooms.isEmpty() -> item("empty") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No floors yet",
                        detail = "Add a floor, then file this branch's rooms onto it.",
                        icon = Icons.Outlined.Layers,
                    )
                }
            }

            else -> {
                items(state.floors, key = { it.id }) { floor ->
                    FloorCard(
                        floor = floor,
                        rooms = state.roomsOn(floor.id),
                        expandedRoomId = state.expandedRoomId,
                        bedsOf = state::bedsOf,
                        onRename = { renaming = floor; name = floor.name },
                        onDelete = { deleting = floor },
                        onAddRoom = { addRoomOn = floor; roomNumber = ""; roomCooling = "non_ac" },
                        onToggleRoom = viewModel::toggleRoom,
                        onAddBed = { room -> addBedIn = room; bedLabel = "" },
                    )
                }

                if (state.unassignedRooms.isNotEmpty()) {
                    item("unassigned") {
                        HamsaCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconTile(icon = Icons.Outlined.Bed, tone = com.aalaya.pg.ui.components.IconTone.Warning)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Not on a floor yet",
                                        style = HamsaTheme.type.titleMedium,
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        "Located by room number until you file them.",
                                        style = HamsaTheme.type.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            state.unassignedRooms.forEachIndexed { index, room ->
                                ListRow(
                                    title = "Room ${room.number}",
                                    subtitle = if (room.coolingType == "ac") "AC" else "Non-AC",
                                    showTopDivider = true.takeIf { index >= 0 } ?: false,
                                    trailing = {
                                        if (state.floors.isNotEmpty()) {
                                            SecondaryButton(
                                                text = "File",
                                                onClick = {
                                                    viewModel.assignRoom(
                                                        room.id,
                                                        state.floors.first().id,
                                                    ) {}
                                                },
                                                enabled = !state.saving,
                                                size = ButtonSize.Small,
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

        state.saveError?.let { message ->
            item("save-error") {
                Text(message, style = HamsaTheme.type.bodySmall, color = colors.errorFg)
            }
        }
    }

    if (showAdd || renaming != null) {
        val editing = renaming
        FormDialog(
            title = if (editing == null) "Add floor" else "Rename floor",
            confirmText = if (editing == null) "Add" else "Save",
            confirmEnabled = name.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = {
                if (editing == null) {
                    viewModel.createFloor(name) { showAdd = false }
                } else {
                    viewModel.renameFloor(editing, name) { renaming = null }
                }
            },
            onDismiss = { showAdd = false; renaming = null; viewModel.clearSaveError() },
        ) {
            HamsaTextField(
                value = name,
                onValueChange = { name = it },
                label = "Floor name",
                placeholder = "e.g. Floor 2 or Ground",
            )
        }
    }

    addRoomOn?.let { floor ->
        FormDialog(
            title = "Add room to ${floor.name}",
            confirmText = "Add",
            confirmEnabled = roomNumber.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = {
                viewModel.addRoom(floor.id, roomNumber, roomCooling) { addRoomOn = null }
            },
            onDismiss = { addRoomOn = null; viewModel.clearSaveError() },
        ) {
            HamsaTextField(
                value = roomNumber,
                onValueChange = { roomNumber = it },
                label = "Room number",
                placeholder = "e.g. 201",
            )
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
                SelectableChip(
                    label = "AC",
                    selected = roomCooling == "ac",
                    onClick = { roomCooling = "ac" },
                    modifier = Modifier.weight(1f),
                )
                SelectableChip(
                    label = "Non-AC",
                    selected = roomCooling == "non_ac",
                    onClick = { roomCooling = "non_ac" },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    addBedIn?.let { room ->
        FormDialog(
            title = "Add bed to Room ${room.number}",
            confirmText = "Add",
            confirmEnabled = bedLabel.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.addBed(room.id, bedLabel) { addBedIn = null } },
            onDismiss = { addBedIn = null; viewModel.clearSaveError() },
        ) {
            HamsaTextField(
                value = bedLabel,
                onValueChange = { bedLabel = it },
                label = "Bed label",
                placeholder = "e.g. A",
            )
        }
    }

    deleting?.let { floor ->
        HamsaAlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${floor.name}?") },
            text = {
                Text(
                    if (floor.roomCount > 0) {
                        "This floor still has ${floor.roomCount} room(s) on it. " +
                            "Move them onto another floor first."
                    } else {
                        "This floor has no rooms on it and can be removed."
                    },
                )
            },
            confirmButton = {
                SecondaryButton(
                    text = "Delete",
                    onClick = { viewModel.deleteFloor(floor) { deleting = null } },
                    enabled = !state.saving,
                    size = ButtonSize.Small,
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = "Cancel",
                    onClick = { deleting = null },
                    size = ButtonSize.Small,
                )
            },
        )
    }
}

@Composable
private fun FloorCard(
    floor: FloorDto,
    rooms: List<RoomDto>,
    expandedRoomId: String?,
    bedsOf: (String) -> List<BedDto>?,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddRoom: () -> Unit,
    onToggleRoom: (String) -> Unit,
    onAddBed: (RoomDto) -> Unit,
) {
    val colors = HamsaTheme.colors
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Outlined.Layers)
            Column(modifier = Modifier.weight(1f)) {
                Text(floor.name, style = HamsaTheme.type.titleMedium, color = colors.textPrimary)
                Text(
                    if (floor.roomCount == 1) "1 room" else "${floor.roomCount} rooms",
                    style = HamsaTheme.type.bodySmall,
                    color = colors.textSecondary,
                )
            }
            StatusChip(
                label = if (floor.roomCount == 0) "Empty" else "In use",
                tone = if (floor.roomCount == 0) StatusTone.Warning else StatusTone.Success,
            )
        }

        if (rooms.isEmpty()) {
            Text(
                text = "No rooms on this floor yet.",
                style = HamsaTheme.type.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = HamsaTheme.spacing.md),
            )
        }

        // Each room expands to its beds, so the whole hierarchy is reachable
        // without leaving the floor being worked on.
        rooms.forEach { room ->
            val expanded = expandedRoomId == room.id
            ListRow(
                title = "Room ${room.number}",
                subtitle = if (room.coolingType == "ac") "AC" else "Non-AC",
                showTopDivider = true,
                onClick = { onToggleRoom(room.id) },
                leading = { IconTile(icon = Icons.Outlined.Bed, size = 30.dp, iconSize = 16.dp) },
                trailing = {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Hide beds" else "Show beds",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
            if (expanded) {
                val beds = bedsOf(room.id)
                Column(modifier = Modifier.padding(start = HamsaTheme.spacing.xxl)) {
                    when {
                        beds == null -> Text(
                            "Loading beds…",
                            style = HamsaTheme.type.bodySmall,
                            color = colors.textTertiary,
                        )
                        beds.isEmpty() -> Text(
                            "No beds yet.",
                            style = HamsaTheme.type.bodySmall,
                            color = colors.textTertiary,
                        )
                        else -> beds.forEach { bed ->
                            Text(
                                text = "Bed ${bed.label}" +
                                    (bed.occupantName?.let { " · $it" } ?: " · Free"),
                                style = HamsaTheme.type.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                    SecondaryButton(
                        text = "Add bed",
                        onClick = { onAddBed(room) },
                        size = ButtonSize.Small,
                        modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
                    )
                }
            }
        }

        Spacer(Modifier.height(HamsaTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
            PrimaryButton(text = "Add room", onClick = onAddRoom, size = ButtonSize.Small)
            SecondaryButton(text = "Rename", onClick = onRename, size = ButtonSize.Small)
            SecondaryButton(text = "Delete", onClick = onDelete, size = ButtonSize.Small)
        }
    }
}
