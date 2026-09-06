package com.aalaya.pg.feature.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.network.dto.AuditEntryDto
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * Audit log showing only manager-originated changes.
 * Each entry is tappable to reveal the details of what was changed.
 */
@Composable
fun AuditScreen(
    onBack: () -> Unit,
    viewModel: AuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Only show entries that represent manager changes (edits, deletes, creates
    // that went through maker-checker). System/owner-only entries are excluded.
    val managerEntries = state.items.filter { entry ->
        // Include entries that have an actor (someone made the change) and
        // represent meaningful business mutations.
        entry.actorId != null && entry.action in listOf(
            "create", "update", "delete", "approve", "reject",
            "move_out", "record_payment", "assign_bed",
        )
    }

    HamsaScreen(
        title = "Audit log",
        lead = "Manager changes — who changed what, and when.",
        onBack = onBack,
        refreshing = state.loading && state.items.isNotEmpty(),
        onRefresh = viewModel::refresh,
    ) {
        when {
            state.loading && state.items.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.items.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }
            managerEntries.isEmpty() -> item("empty") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No manager changes yet",
                        detail = "Edits, approvals, and other manager actions will appear here.",
                        icon = Icons.Outlined.History,
                    )
                }
            }
            else -> item("entries") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    managerEntries.forEachIndexed { index, e ->
                        AuditEntryRow(
                            entry = e,
                            showTopDivider = index > 0,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditEntryRow(
    entry: AuditEntryDto,
    showTopDivider: Boolean,
) {
    val colors = HamsaTheme.colors
    var expanded by remember { mutableStateOf(false) }

    // Friendly label for the action
    val actionLabel = when (entry.action) {
        "create" -> "Created"
        "update" -> "Updated"
        "delete" -> "Deleted"
        "approve" -> "Approved"
        "reject" -> "Rejected"
        "move_out" -> "Moved out"
        "record_payment" -> "Payment recorded"
        "assign_bed" -> "Bed assigned"
        else -> entry.action.replaceFirstChar { it.uppercase() }
    }

    // Show entity type as readable
    val entityLabel = entry.entityType?.replaceFirstChar { it.uppercase() }

    // Actor display: prefer real name, fall back to short UUID
    val actorShort = entry.actorName
        ?: entry.actorId?.take(8)?.let { "$it…" }
        ?: "Unknown"

    if (showTopDivider) {
        HorizontalDivider(color = colors.border)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = HamsaTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Icons.Outlined.History,
                size = 32.dp,
                iconSize = 16.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = HamsaTheme.spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actionLabel,
                        style = HamsaTheme.type.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    entityLabel?.let {
                        Text(
                            text = it,
                            style = HamsaTheme.type.labelMedium,
                            color = colors.textTertiary,
                        )
                    }
                }
                Text(
                    text = "by $actorShort",
                    style = HamsaTheme.type.bodySmall,
                    color = colors.textSecondary,
                )
                Text(
                    text = DateFmt.epochSeconds(entry.atEpoch),
                    style = HamsaTheme.type.bodySmall,
                    color = colors.textTertiary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }

        // Expandable details section
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 44.dp, // align with text after icon
                        top = HamsaTheme.spacing.xs,
                        end = HamsaTheme.spacing.xs,
                    ),
            ) {
                entry.entityId?.let { id ->
                    DetailRow(label = "Entity ID", value = id)
                }
                entry.actorName?.let { name ->
                    DetailRow(label = "Changed by", value = name)
                } ?: entry.actorId?.let { id ->
                    DetailRow(label = "Changed by (ID)", value = id)
                }
                DetailRow(label = "Action", value = entry.action)
                entry.entityType?.let { t ->
                    DetailRow(label = "Record type", value = t.replaceFirstChar { it.uppercase() })
                }
                DetailRow(label = "When", value = DateFmt.epochSeconds(entry.atEpoch))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = HamsaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        Text(
            text = "$label: ",
            style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary,
        )
        Text(
            text = value,
            style = HamsaTheme.type.bodySmall,
            color = colors.textPrimary,
        )
    }
}

