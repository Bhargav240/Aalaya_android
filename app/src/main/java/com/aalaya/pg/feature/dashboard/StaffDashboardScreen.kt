package com.aalaya.pg.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.navigation.Routes
import com.aalaya.pg.navigation.roomAvailabilityRoute
import com.aalaya.pg.ui.components.AutoSizeText
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.QuickActionTile
import com.aalaya.pg.ui.components.ScopeChip
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.components.SkeletonList
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.AmountTextStyle
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * Staff home.
 *
 * The screen answers one question — *what can I offer someone who walks in
 * today?* — and then gets out of the way:
 *
 *   1. Greeting      — who is looking
 *   2. Quick Actions — the destinations that are not bottom-nav tabs
 *   3. Room summary  — free rooms by sharing type, tappable for the room list
 *
 * Money used to lead this screen (outstanding, collected, occupancy, open
 * complaints). Every one of those figures already owns a screen of its own, so
 * Home was a summary of other summaries; vacancy is the fact that had nowhere
 * else to live.
 */
@Composable
fun StaffDashboardScreen(
    isOwner: Boolean,
    staffName: String,
    onOpen: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    HamsaScreen(
        // No app-bar title: the greeting is this screen's heading. A bar
        // reading "Overview" above "Good Day, ..." stacked two headers, and a
        // bar reading "Home" would only repeat the highlighted bottom-nav tab.
        // Tenant Home already leads with its greeting the same way.
        title = "",
        showTopBar = false,
        refreshing = state.refreshing,
        onRefresh = viewModel::refresh,
    ) {
        item("greeting") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HamsaTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
            ) {
                Text(
                    text = "Good Day, $staffName!",
                    style = HamsaTheme.type.headlineSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )

                // Owners pick which branch every figure below refers to;
                // managers are pinned to their own and get no chip at all.
                if (isOwner && state.branches.isNotEmpty()) {
                    val scopedName = state.scopedBranchId
                        ?.let { id -> state.branches.firstOrNull { it.id == id }?.name }
                    ScopeChip(
                        label = scopedName ?: state.branches.first().name,
                        options = state.branches.map { it.id },
                        optionLabel = { id -> state.branches.firstOrNull { it.id == id }?.name.orEmpty() },
                        onSelect = viewModel::scopeToBranch,
                    )
                }
            }
        }

        item("quick-actions") {
            QuickActions(isOwner = isOwner, pendingApprovals = state.pendingApprovals, onOpen = onOpen)
        }

        item("room-summary-head") {
            SectionHead("Room Summary")
        }

        item("room-summary") {
            when {
                state.roomSummaryLoading && state.roomSummary.isEmpty() ->
                    SkeletonList(count = 2)

                state.roomSummaryError != null && state.roomSummary.isEmpty() ->
                    HamsaCard(modifier = Modifier.fillMaxWidth()) {
                        ErrorState(state.roomSummaryError!!, onRetry = viewModel::refresh)
                    }

                state.roomSummary.isEmpty() ->
                    HamsaCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            title = "No rooms available",
                            detail = "Every room is currently occupied right now.",
                            icon = Icons.Outlined.Bed,
                        )
                    }

                else -> RoomSummaryCard(
                    rows = state.roomSummary,
                    totalVacant = state.totalVacantRooms,
                    onOpenType = { row ->
                        onOpen(roomAvailabilityRoute(row.sharingCount, row.title, state.roomScopeLabel))
                    },
                )
            }
        }
    }
}

/**
 * The three-up grid of destinations that have no bottom-nav tab. Approvals
 * turns amber while anything is waiting, so the owner sees it without reading.
 */
@Composable
private fun QuickActions(
    isOwner: Boolean,
    pendingApprovals: Int,
    onOpen: (String) -> Unit,
) {
    data class Action(
        val icon: ImageVector,
        val label: String,
        val route: String,
        val tone: IconTone = IconTone.Brand,
    )

    val actions = buildList {
        add(Action(Icons.Outlined.Apartment, "Branches", Routes.BRANCHES))
        add(Action(Icons.Outlined.Campaign, "Notices", Routes.NOTICES))
        add(Action(Icons.Outlined.PersonAdd, "Invitations", Routes.INVITATION_MANAGEMENT))
        if (isOwner) {
            add(
                Action(
                    Icons.Outlined.VerifiedUser,
                    "Approvals",
                    Routes.APPROVALS,
                    if (pendingApprovals > 0) IconTone.Warning else IconTone.Brand,
                ),
            )
            add(Action(Icons.Outlined.QuestionAnswer, "Enquiries", Routes.ENQUIRIES))
            add(Action(Icons.Outlined.Map, "Listings", Routes.MY_LISTINGS))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
        SectionHead("Quick Actions")
        actions.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
            ) {
                row.forEach { action ->
                    QuickActionTile(
                        icon = action.icon,
                        label = action.label,
                        tone = action.tone,
                        onClick = { onOpen(action.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep a short final row's tiles at column width rather than
                // letting two tiles stretch across three columns.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * The headline vacancy figure, then one tappable row per sharing type.
 */
@Composable
private fun RoomSummaryCard(
    rows: List<RoomSummaryRow>,
    totalVacant: Int,
    onOpenType: (RoomSummaryRow) -> Unit,
) {
    val colors = HamsaTheme.colors
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AutoSizeText(
                    text = totalVacant.toString(),
                    style = AmountTextStyle,
                    color = colors.brand,
                    minFontScale = 0.7f,
                )
                Text(
                    text = "vacant rooms",
                    style = HamsaTheme.type.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            StatusChip(label = "Available now", tone = StatusTone.Success)
        }

        HorizontalDivider(
            color = colors.border,
            modifier = Modifier.padding(vertical = HamsaTheme.spacing.md),
        )

        Column(verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
            rows.forEach { row ->
                HamsaCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = HamsaTheme.spacing.md,
                    onClick = { onOpenType(row) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.title,
                                style = HamsaTheme.type.titleSmall,
                                color = colors.textPrimary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = row.vacantCount.toString(),
                                style = HamsaTheme.type.titleLarge,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = if (row.vacantCount == 1) "room" else "rooms",
                                style = HamsaTheme.type.labelSmall,
                                color = colors.textSecondary,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
