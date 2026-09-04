package com.aalaya.pg.feature.approvals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        lead = "Anything a manager adds, edits or deletes waits here for your yes.",
        onBack = onBack,
        refreshing = state.loading && state.items.isNotEmpty(),
        onRefresh = viewModel::refresh,
    ) {
        when {
            state.loading && state.items.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.items.isEmpty() -> item("error") {
                ErrorState(state.error!!, onRetry = viewModel::refresh)
            }
            state.items.isEmpty() -> item("empty") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "Queue is clear",
                        detail = "Manager edits and deletes will wait here for your approval.",
                        icon = Icons.Outlined.CheckCircle,
                    )
                }
            }
            else -> items(state.items, key = { it.id }) { cr ->
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    val destructive = cr.operation.equals("delete", true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(
                            icon = Icons.Outlined.VerifiedUser,
                            tone = if (destructive) IconTone.Error else IconTone.Warning,
                        )
                        StatusChip(
                            label = "${cr.operation.replaceFirstChar { it.uppercase() }} · ${cr.entityType}",
                            tone = if (destructive) StatusTone.Error else StatusTone.Warning,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            "by ${cr.requestedBy.take(8)}…",
                            style = HamsaTheme.type.labelMedium,
                            color = colors.textTertiary,
                        )
                    }
                    Text(
                        text = "${cr.entityType.replaceFirstChar { it.uppercase() }} ${cr.entityId.take(8)}…",
                        style = HamsaTheme.type.titleMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = HamsaTheme.spacing.md),
                    )
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
