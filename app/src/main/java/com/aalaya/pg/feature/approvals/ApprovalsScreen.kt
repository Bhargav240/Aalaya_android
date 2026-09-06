package com.aalaya.pg.feature.approvals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * Maker-checker queue. Every manager edit or delete waits here for the owner's
 * yes; creates never do.
 */
@Composable
fun ApprovalsScreen(
    onBack: () -> Unit,
    viewModel: ApprovalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    HamsaScreen(
        title = "Approvals",
        lead = "Review and manage change requests raised by managers.",
        onBack = onBack,
        refreshing = state.loading && state.allItems.isNotEmpty(),
        onRefresh = viewModel::refresh,
    ) {
        when {
            state.loading && state.allItems.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.allItems.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }
            else -> {
                item("filters_and_sort") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = HamsaTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Filter tabs with live count badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SelectableChip(
                                label = "To be approved (${state.pendingCount})",
                                selected = state.filter == ApprovalFilter.TO_BE_APPROVED,
                                onClick = { viewModel.setFilter(ApprovalFilter.TO_BE_APPROVED) },
                            )
                            SelectableChip(
                                label = "Approved (${state.approvedCount})",
                                selected = state.filter == ApprovalFilter.APPROVED,
                                onClick = { viewModel.setFilter(ApprovalFilter.APPROVED) },
                            )
                            SelectableChip(
                                label = "Rejected (${state.rejectedCount})",
                                selected = state.filter == ApprovalFilter.REJECTED,
                                onClick = { viewModel.setFilter(ApprovalFilter.REJECTED) },
                            )
                        }

                        // Showing count & Sort dropdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${state.filteredItems.size} ${state.filter.label.lowercase()} items",
                                style = HamsaTheme.type.labelMedium,
                                color = colors.textTertiary,
                            )

                            var sortMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clickable { sortMenuExpanded = true }
                                        .background(colors.surface, RoundedCornerShape(8.dp))
                                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = colors.textSecondary,
                                    )
                                    Text(
                                        text = state.sort.label,
                                        style = HamsaTheme.type.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textSecondary,
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = colors.textSecondary,
                                    )
                                }

                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Latest first", style = HamsaTheme.type.labelMedium) },
                                        onClick = {
                                            viewModel.setSort(ApprovalSort.LATEST_FIRST)
                                            sortMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Oldest first", style = HamsaTheme.type.labelMedium) },
                                        onClick = {
                                            viewModel.setSort(ApprovalSort.OLDEST_FIRST)
                                            sortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.filteredItems.isEmpty()) {
                    item("empty") {
                        HamsaCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                title = when (state.filter) {
                                    ApprovalFilter.TO_BE_APPROVED -> "Queue is clear"
                                    ApprovalFilter.APPROVED -> "No approved tickets"
                                    ApprovalFilter.REJECTED -> "No rejected tickets"
                                },
                                detail = when (state.filter) {
                                    ApprovalFilter.TO_BE_APPROVED -> "Manager edits and deletes will wait here for your approval."
                                    ApprovalFilter.APPROVED -> "Approved change requests will appear here."
                                    ApprovalFilter.REJECTED -> "Rejected change requests will appear here."
                                },
                                icon = Icons.Outlined.CheckCircle,
                            )
                        }
                    }
                } else {
                    items(state.filteredItems, key = { it.id }) { cr ->
                        HamsaCard(modifier = Modifier.fillMaxWidth()) {
                            val destructive = cr.operation.equals("delete", true)
                            val isPending = cr.status.equals("pending", true)
                            val isApproved = cr.status.equals("approved", true)
                            val isRejected = cr.status.equals("rejected", true)

                            val statusTone = when {
                                isApproved -> StatusTone.Success
                                isRejected -> StatusTone.Error
                                destructive -> StatusTone.Error
                                else -> StatusTone.Warning
                            }

                            val titleText = when (cr.entityType.lowercase()) {
                                "resident" -> cr.entityName ?: "Resident ${cr.entityId.take(8)}…"
                                "floor" -> cr.entityName ?: "Floor ${cr.entityId.take(8)}…"
                                "room" -> if (cr.entityName != null) "Room ${cr.entityName}" else "Room ${cr.entityId.take(8)}…"
                                "bed" -> if (cr.entityName != null) "Bed ${cr.entityName}" else "Bed ${cr.entityId.take(8)}…"
                                "branch" -> cr.entityName ?: "Branch ${cr.entityId.take(8)}…"
                                else -> "${cr.entityType.replaceFirstChar { it.uppercase() }} ${cr.entityName ?: cr.entityId.take(8)}"
                            }

                            val actionSummary = cr.description ?: when (cr.operation.lowercase()) {
                                "delete" -> "Delete ${cr.entityType}"
                                "create" -> "Create new ${cr.entityType}"
                                else -> "Update ${cr.entityType}"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconTile(
                                        icon = when {
                                            isApproved -> Icons.Outlined.CheckCircle
                                            isRejected -> Icons.Outlined.Cancel
                                            else -> Icons.Outlined.VerifiedUser
                                        },
                                        tone = when {
                                            isApproved -> IconTone.Success
                                            isRejected -> IconTone.Error
                                            destructive -> IconTone.Error
                                            else -> IconTone.Warning
                                        },
                                    )
                                    StatusChip(
                                        label = "${cr.operation.replaceFirstChar { it.uppercase() }} · ${cr.entityType.replaceFirstChar { it.uppercase() }}",
                                        tone = statusTone,
                                    )
                                }

                                StatusChip(
                                    label = cr.status.uppercase(),
                                    tone = when {
                                        isApproved -> StatusTone.Success
                                        isRejected -> StatusTone.Error
                                        else -> StatusTone.Warning
                                    },
                                )
                            }

                            Text(
                                text = titleText,
                                style = HamsaTheme.type.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
                            )

                            // Prominent proposed change summary
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = HamsaTheme.spacing.sm),
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "PROPOSED CHANGE",
                                        style = HamsaTheme.type.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = colors.brand,
                                    )
                                    Text(
                                        text = actionSummary,
                                        style = HamsaTheme.type.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }

                            // Audit metadata: Raised by & when
                            val requesterName = cr.requestedByName ?: "User ${cr.requestedBy.take(8)}"
                            val createdDate = DateFmt.isoDateTime(cr.createdAt)
                            Text(
                                text = "Raised by $requesterName${if (createdDate.isNotEmpty()) " on $createdDate" else ""}",
                                style = HamsaTheme.type.labelSmall,
                                color = colors.textTertiary,
                            )

                            // Decided by & when (if approved or rejected)
                            if (!isPending) {
                                val deciderName = cr.decidedByName ?: "Owner"
                                val decidedDate = DateFmt.isoDateTime(cr.decidedAt)
                                val actionVerb = if (isApproved) "Approved" else "Rejected"
                                Text(
                                    text = "$actionVerb by $deciderName${if (decidedDate.isNotEmpty()) " on $decidedDate" else ""}",
                                    style = HamsaTheme.type.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isApproved) colors.brand else colors.errorFg,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            // Approve & Reject buttons only for pending tickets
                            if (isPending) {
                                Row(
                                    modifier = Modifier.padding(top = HamsaTheme.spacing.md),
                                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                                ) {
                                    PrimaryButton(
                                        text = "Approve",
                                        onClick = { viewModel.approve(cr.id) },
                                        enabled = state.actioningId == null,
                                        size = ButtonSize.Small,
                                        leading = {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                    )
                                    SecondaryButton(
                                        text = "Reject",
                                        onClick = { viewModel.reject(cr.id) },
                                        enabled = state.actioningId == null,
                                        size = ButtonSize.Small,
                                        leading = {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
