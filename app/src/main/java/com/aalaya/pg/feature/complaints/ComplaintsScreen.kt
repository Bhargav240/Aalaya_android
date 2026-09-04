package com.aalaya.pg.feature.complaints

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.ComplaintState
import com.aalaya.pg.core.network.dto.ComplaintDto
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.toneForComplaint
import com.aalaya.pg.ui.components.toneForStatus
import com.aalaya.pg.ui.theme.HamsaTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComplaintsScreen(
    canManage: Boolean,
    viewModel: ComplaintsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRaise by remember { mutableStateOf(false) }
    // Which complaint is open, if any. Held here rather than per card so that
    // opening one closes the last — the list stays a list, and you never
    // scroll past two half-read steppers looking for the third.
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var business by remember { mutableStateOf(false) }

    HamsaScreen(
        title = "Complaints",
        lead = if (canManage) {
            "The resident sees each step live as you move it along."
        } else {
            "Only you and your branch staff can see these."
        },
        refreshing = state.loading && state.items.isNotEmpty(),
        onRefresh = viewModel::refresh,
        // Only residents raise complaints. Staff triage what comes in and move
        // it through the state machine — the mockup's staff view has no raise
        // affordance at all.
        actions = if (canManage) {
            null
        } else {
            {
                PrimaryButton(
                    text = "Raise",
                    onClick = { title = ""; desc = ""; business = false; showRaise = true },
                    size = ButtonSize.Small,
                )
            }
        },
    ) {
        when {
            state.loading && state.items.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.items.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }
            state.items.isEmpty() -> item("empty") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "All clear",
                        detail = if (canManage) {
                            "Nothing outstanding. Complaints your residents raise " +
                                "arrive here for you to triage."
                        } else {
                            "Nothing outstanding. Raise one and you'll see it move " +
                                "stage by stage."
                        },
                        icon = Icons.Outlined.Build,
                    )
                }
            }
            else -> items(state.items, key = { it.id }) { c ->
                ComplaintCard(
                    c = c,
                    canManage = canManage,
                    transitioning = state.transitioningId == c.id,
                    expanded = expandedId == c.id,
                    onToggle = { expandedId = if (expandedId == c.id) null else c.id },
                    onTransition = { to -> viewModel.transition(c.id, to) },
                )
            }
        }
    }

    if (showRaise) {
        FormDialog(
            title = "Raise a complaint",
            confirmText = "Submit",
            confirmEnabled = title.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.raise(title, desc, business) { showRaise = false } },
            onDismiss = { showRaise = false },
        ) {
            HamsaTextField(
                value = title,
                onValueChange = { title = it },
                label = "What's wrong?",
                placeholder = "e.g. Fan not working",
            )
            Spacer(Modifier.height(HamsaTheme.spacing.stack))
            HamsaTextField(
                value = desc,
                onValueChange = { desc = it },
                label = "Description (optional)",
                singleLine = false,
            )
            if (canManage) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
                ) {
                    Checkbox(
                        checked = business,
                        onCheckedChange = { business = it },
                        colors = CheckboxDefaults.colors(checkedColor = HamsaTheme.colors.brand),
                    )
                    Text(
                        "Business-level (org-wide)",
                        style = HamsaTheme.type.bodySmall,
                        color = HamsaTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComplaintCard(
    c: ComplaintDto,
    canManage: Boolean,
    transitioning: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onTransition: (ComplaintState) -> Unit,
) {
    val current = ComplaintState.fromWire(c.state)
    // Collapsed by default, and only one card is ever open. A full stepper per
    // complaint turned a list of five into a page of scrolling, and the status
    // chip already answers the question most of the time — the stages only
    // matter for the one complaint you are actually chasing.
    val chevronTurn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "complaintChevron",
    )

    HamsaCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Icons.Outlined.ReportProblem,
                tone = when (current) {
                    ComplaintState.Resolved, ComplaintState.Closed -> IconTone.Success
                    else -> IconTone.Warning
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    c.title,
                    style = HamsaTheme.type.titleMedium,
                    color = HamsaTheme.colors.textPrimary,
                )
                if (c.isBusinessLevel) {
                    Text(
                        "Business-level",
                        style = HamsaTheme.type.labelMedium,
                        color = HamsaTheme.colors.textTertiary,
                    )
                }
            }
            StatusChip(label = current.label, tone = toneForComplaint(current))
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Hide progress" else "Show progress",
                tint = HamsaTheme.colors.textTertiary,
                modifier = Modifier.size(20.dp).rotate(chevronTurn),
            )
        }

        if (expanded) {
            if (!c.description.isNullOrBlank()) {
                Spacer(Modifier.height(HamsaTheme.spacing.md))
                Text(
                    c.description,
                    style = HamsaTheme.type.bodySmall,
                    color = HamsaTheme.colors.textSecondary,
                )
            }
            if (c.createdEpoch > 0) {
                Spacer(Modifier.height(HamsaTheme.spacing.sm))
                Text(
                    text = buildString {
                        append("Raised ${DateFmt.epochSeconds(c.createdEpoch)}")
                        // Only worth saying once the state has actually moved.
                        if (c.updatedEpoch > c.createdEpoch) {
                            append(" · Updated ${DateFmt.epochSeconds(c.updatedEpoch)}")
                        }
                    },
                    style = HamsaTheme.type.labelMedium,
                    color = HamsaTheme.colors.textTertiary,
                )
            }

            if (canManage && current.allowedTransitions().isNotEmpty()) {
                Spacer(Modifier.height(HamsaTheme.spacing.md))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                ) {
                    current.allowedTransitions().forEach { to ->
                        SecondaryButton(
                            text = "Move to ${to.label}",
                            onClick = { onTransition(to) },
                            enabled = !transitioning,
                            size = ButtonSize.Small,
                        )
                    }
                }
            }
        }
    }
}
