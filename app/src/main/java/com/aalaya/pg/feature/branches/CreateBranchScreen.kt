package com.aalaya.pg.feature.branches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.HamsaTopBar
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.theme.HamsaTheme

/** Initial setup page for a new property. Later edits still use branch/floor/room screens. */
@Composable
fun CreateBranchScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: BranchesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var floorCount by remember { mutableStateOf("1") }
    var roomsPerFloor by remember { mutableStateOf("1") }
    var bedsPerRoom by remember { mutableStateOf("1") }
    val floors = floorCount.toIntOrNull()
    val rooms = roomsPerFloor.toIntOrNull()
    val beds = bedsPerRoom.toIntOrNull()
    val valid = name.isNotBlank() && floors in 1..100 && rooms in 1..100 && beds in 1..26

    Column(
        modifier = Modifier.fillMaxSize().background(HamsaTheme.colors.canvas),
    ) {
        HamsaTopBar(title = "Create branch", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxWidth().padding(HamsaTheme.spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
        ) {
            Text(
                "Set up the initial building layout",
                style = HamsaTheme.type.titleMedium,
                color = HamsaTheme.colors.textPrimary,
            )
            Text(
                "Rooms are numbered 101, 102 … on Floor 1, then 201 … on Floor 2. Beds are created as A, B, C …",
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.textSecondary,
            )
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md)) {
                    HamsaTextField(name, { name = it }, label = "Branch name")
                    HamsaTextField(floorCount, { floorCount = it }, label = "Number of floors (1–100)")
                    HamsaTextField(roomsPerFloor, { roomsPerFloor = it }, label = "Rooms per floor (1–100)")
                    HamsaTextField(bedsPerRoom, { bedsPerRoom = it }, label = "Beds per room (1–26)")
                }
            }
            Text(
                floors?.let { f -> rooms?.let { r -> beds?.let { b -> "This will create $f floors, ${f * r} rooms and ${f * r * b} beds." } } } ?: "Enter valid layout values.",
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.textSecondary,
            )
            state.saveError?.let {
                Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.errorFg)
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "Create branch",
                onClick = {
                    viewModel.addBranchWithLayout(name, floors!!, rooms!!, beds!!) { onCreated() }
                },
                enabled = valid,
                loading = state.saving,
                size = ButtonSize.Default,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
