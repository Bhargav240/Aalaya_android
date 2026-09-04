package com.aalaya.pg.feature.floors

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * Add a floor, and the rooms on it, in one pass.
 *
 * A floor is rarely added alone — it is added because a storey exists with
 * rooms on it. Making the person create the floor, find it, then add each room
 * one dialog at a time turns one real-world act into a dozen interactions. So
 * the form asks how many rooms and expands to collect each one, and submits
 * the lot as a single request.
 *
 * Room numbers are suggested from the floor name ("Floor 3" → 301, 302…) and
 * stay editable, because numbering conventions differ between properties.
 */
@Composable
fun AddFloorScreen(
    branchId: String,
    branchName: String,
    existingFloors: Int,
    onBack: () -> Unit,
    viewModel: AddFloorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    HamsaScreen(
        title = "Add floor",
        lead = branchName,
        onBack = onBack,
    ) {
        item("floor") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(icon = Icons.Outlined.Layers)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Floor details",
                            style = HamsaTheme.type.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            "Name it the way your residents refer to it.",
                            style = HamsaTheme.type.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(HamsaTheme.spacing.md))
                HamsaTextField(
                    value = state.floorName,
                    onValueChange = viewModel::onFloorName,
                    label = "Floor name",
                    placeholder = "e.g. Floor 3 or Ground",
                )
            }
        }

        item("count") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                SectionHead("How many rooms?")
                Text(
                    "Add them now, or leave it at zero and add rooms later.",
                    style = HamsaTheme.type.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(HamsaTheme.spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Counter(
                        value = state.rooms.size,
                        onChange = viewModel::setRoomCount,
                        enabled = !state.saving,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.rooms.isNotEmpty()) {
                        StatusChip(
                            label = "${state.totalBeds} beds total",
                            tone = StatusTone.Info,
                        )
                    }
                }
            }
        }

        if (state.rooms.isNotEmpty()) {
            item("rooms-head") { SectionHead("Rooms on this floor") }
        }

        itemsIndexed(state.rooms) { index, room ->
            RoomDraftCard(
                index = index,
                room = room,
                duplicate = room.number.trim().lowercase() in state.duplicateNumbers &&
                    room.number.isNotBlank(),
                enabled = !state.saving,
                onNumber = { viewModel.onRoomNumber(index, it) },
                onCooling = { viewModel.onRoomCooling(index, it) },
                onBeds = { viewModel.onRoomBeds(index, it) },
                onRemove = { viewModel.removeRoom(index) },
            )
        }

        if (state.rooms.isNotEmpty()) {
            item("add-another") {
                SecondaryButton(
                    text = "Add another room",
                    onClick = viewModel::addRoom,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }

        state.error?.let { message ->
            item("error") {
                Text(message, style = HamsaTheme.type.bodySmall, color = colors.errorFg)
            }
        }

        item("submit") {
            PrimaryButton(
                text = if (state.rooms.isEmpty()) {
                    "Create floor"
                } else {
                    "Create floor with ${state.rooms.size} room${if (state.rooms.size == 1) "" else "s"}"
                },
                onClick = {
                    // New floors sort after the ones that already exist.
                    viewModel.submit(branchId, existingFloors + 1, onBack)
                },
                enabled = state.canSubmit,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item("note") {
            Text(
                text = "A manager's new floor waits for the owner's approval; " +
                    "an owner's is added straight away.",
                style = HamsaTheme.type.labelMedium,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun RoomDraftCard(
    index: Int,
    room: RoomDraft,
    duplicate: Boolean,
    enabled: Boolean,
    onNumber: (String) -> Unit,
    onCooling: (String) -> Unit,
    onBeds: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = HamsaTheme.colors
    HamsaCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Icons.Outlined.Bed,
                tone = if (duplicate) IconTone.Error else IconTone.Brand,
                size = 32.dp,
                iconSize = 17.dp,
            )
            Text(
                text = "Room ${index + 1}",
                style = HamsaTheme.type.titleSmall,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove this room",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        HamsaTextField(
            value = room.number,
            onValueChange = onNumber,
            label = "Room number",
            placeholder = "e.g. 301",
            keyboardType = KeyboardType.Text,
        )
        if (duplicate) {
            Text(
                text = "Another room on this floor already uses this number.",
                style = HamsaTheme.type.bodySmall,
                color = colors.errorFg,
                modifier = Modifier.padding(top = HamsaTheme.spacing.xs),
            )
        }

        Spacer(Modifier.height(HamsaTheme.spacing.md))
        Text("Room type", style = HamsaTheme.type.labelMedium, color = colors.textSecondary)
        Spacer(Modifier.height(HamsaTheme.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
            SelectableChip(
                label = "AC",
                selected = room.coolingType == "ac",
                onClick = { onCooling("ac") },
                modifier = Modifier.weight(1f),
            )
            SelectableChip(
                label = "Non-AC",
                selected = room.coolingType == "non_ac",
                onClick = { onCooling("non_ac") },
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
                    // Says what the number means, so "3" is not ambiguous.
                    text = if (room.beds == 1) "Single room" else "${room.beds} people share",
                    style = HamsaTheme.type.bodySmall,
                    color = colors.textTertiary,
                )
            }
            Counter(value = room.beds, onChange = onBeds, enabled = enabled, min = 1, max = 26)
        }
    }
}

/** A plain minus/value/plus control — used for room count and beds per room. */
@Composable
private fun Counter(
    value: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean,
    min: Int = 0,
    max: Int = 30,
) {
    val colors = HamsaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
    ) {
        SecondaryButton(
            text = "",
            onClick = { onChange(value - 1) },
            enabled = enabled && value > min,
            size = ButtonSize.Small,
            leading = {
                Icon(Icons.Outlined.Remove, contentDescription = "One fewer", modifier = Modifier.size(18.dp))
            },
        )
        Text(
            text = value.toString(),
            style = HamsaTheme.type.titleMedium,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = HamsaTheme.spacing.sm),
        )
        SecondaryButton(
            text = "",
            onClick = { onChange(value + 1) },
            enabled = enabled && value < max,
            size = ButtonSize.Small,
            leading = {
                Icon(Icons.Outlined.Add, contentDescription = "One more", modifier = Modifier.size(18.dp))
            },
        )
    }
}
