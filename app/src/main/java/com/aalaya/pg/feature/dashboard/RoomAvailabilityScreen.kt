package com.aalaya.pg.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.Floors
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * The rooms behind one row of Home's room summary, grouped by branch and floor.
 *
 * Floors are not a backend concept — they are read off the room number by
 * [Floors] — so this is the same grouping Occupancy and Payments use, which is
 * what lets a manager say "101 and 103 are free" without cross-referencing.
 */
@Composable
fun RoomAvailabilityScreen(
    title: String,
    sharingCount: Int,
    scopeLabel: String,
    onBack: () -> Unit,
    viewModel: RoomAvailabilityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    LaunchedEffect(sharingCount) { viewModel.load(sharingCount) }

    HamsaScreen(
        title = title,
        onBack = onBack,
        refreshing = state.loading && state.rooms.isNotEmpty(),
        onRefresh = viewModel::refresh,
    ) {
        when {
            state.loading && state.rooms.isEmpty() -> item("loading") { LoadingState() }

            state.error != null && state.rooms.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }

            state.rooms.isEmpty() -> item("empty") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No rooms available",
                        detail = "There are no vacant rooms in this room type right now.",
                        icon = Icons.Outlined.Bed,
                    )
                }
            }

            else -> {
                item("header") {
                    HamsaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scopeLabel,
                                    style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary,
                                )
                            }
                            StatusChip(label = state.totalLabel, tone = StatusTone.Success)
                        }
                    }
                }

                val byBranch = state.rooms.groupBy { it.branchName }
                byBranch.keys.sorted().forEach { branchName ->
                    val branchRooms = byBranch[branchName].orEmpty()
                    item("branch-$branchName") {
                        val branchVacancies = branchRooms.sumOf { it.vacancyCount }
                        HamsaCard(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.brandSubtle,
                            borderColor = colors.brand.copy(alpha = 0.25f),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconTile(icon = Icons.Outlined.Bed)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = branchName,
                                        style = HamsaTheme.type.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = "${branchRooms.size} rooms",
                                        style = HamsaTheme.type.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                                StatusChip(
                                    label = if (branchVacancies == 1) "1 vacancy" else "$branchVacancies vacancies",
                                    tone = StatusTone.Success,
                                )
                            }
                        }
                    }
                    Floors.keysOf(branchRooms.map { it.roomNumber }).forEach { floorKey ->
                        val floorRooms = branchRooms.filter { it.floorKey == floorKey }
                        if (floorRooms.isNotEmpty()) {
                            item("floor-$branchName-$floorKey") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = HamsaTheme.spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                                ) {
                                    Text(
                                        text = Floors.labelOf(floorKey),
                                        style = HamsaTheme.type.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary,
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = colors.border,
                                    )
                                    Text(
                                        text = if (floorRooms.sumOf { it.vacancyCount } == 1) {
                                            "1 vacancy"
                                        } else {
                                            "${floorRooms.sumOf { it.vacancyCount }} vacancies"
                                        },
                                        style = HamsaTheme.type.labelSmall,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                            items(floorRooms, key = { it.roomId }) { room ->
                                RoomCard(room)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: VacantRoomItem) {
    val colors = HamsaTheme.colors
    HamsaCard(modifier = Modifier.fillMaxWidth(), padding = HamsaTheme.spacing.md) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Outlined.Bed)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.roomLabel,
                    style = HamsaTheme.type.titleSmall,
                    color = colors.textPrimary,
                )
            }
            StatusChip(
                label = if (room.vacancyCount == 1) "1 vacancy" else "${room.vacancyCount} vacancies",
                tone = StatusTone.Success,
            )
        }
    }
}
