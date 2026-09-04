package com.aalaya.pg.feature.audit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.ListRow
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.theme.HamsaTheme

@Composable
fun AuditScreen(
    onBack: () -> Unit,
    viewModel: AuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HamsaScreen(
        title = "Audit log",
        lead = "Who changed what, and when. Nothing is ever truly deleted.",
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
                        title = "Nothing logged yet",
                        detail = "Changes to branches, rooms and residents will appear here.",
                        icon = Icons.Outlined.History,
                    )
                }
            }
            else -> item("entries") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    state.items.forEachIndexed { index, e ->
                        ListRow(
                            title = e.action,
                            subtitle = buildString {
                                e.actorId?.let { append("by ${it.take(8)}… · ") }
                                append(DateFmt.epochSeconds(e.atEpoch))
                            },
                            showTopDivider = index > 0,
                            leading = {
                                IconTile(
                                    icon = Icons.Outlined.History,
                                    size = 30.dp,
                                    iconSize = 16.dp,
                                )
                            },
                            trailing = e.entityType?.let { type ->
                                {
                                    Text(
                                        type,
                                        style = HamsaTheme.type.labelMedium,
                                        color = HamsaTheme.colors.textTertiary,
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
