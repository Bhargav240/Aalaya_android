package com.aalaya.pg.feature.notices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.HorizontalDivider
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
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.theme.HamsaTheme

@Composable
fun NoticesScreen(
    canManage: Boolean,
    onBack: () -> Unit,
    viewModel: NoticesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    HamsaScreen(
        title = "Notices",
        lead = "Broadcast to your branch — residents see it instantly.",
        onBack = onBack,
        refreshing = state.loading && state.items.isNotEmpty(),
        onRefresh = viewModel::refresh,
        actions = if (canManage) {
            {
                PrimaryButton(
                    text = "New",
                    onClick = { title = ""; body = ""; showAdd = true },
                    size = ButtonSize.Small,
                )
            }
        } else {
            null
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
                        title = "No notices yet",
                        detail = "Anything you publish shows up on every resident's home screen.",
                        icon = Icons.Outlined.Campaign,
                    )
                }
            }
            else -> item("list") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    state.items.forEachIndexed { index, n ->
                        NoticeRow(
                            title = n.title,
                            body = n.body,
                            meta = DateFmt.epochSeconds(n.createdEpoch),
                            showTopDivider = index > 0,
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        FormDialog(
            title = "New notice",
            confirmText = "Send to branch",
            confirmEnabled = title.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.create(title, body) { showAdd = false } },
            onDismiss = { showAdd = false },
        ) {
            HamsaTextField(
                value = title,
                onValueChange = { title = it },
                label = "Title",
                placeholder = "e.g. Rent due on the 5th",
            )
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            HamsaTextField(
                value = body,
                onValueChange = { body = it },
                label = "Message",
                placeholder = "Keep it short and clear",
                singleLine = false,
            )
        }
    }
}

/**
 * A notice as the design draws it: an unread dot, a bold title, the body in
 * muted text and a faint author/time line underneath.
 */
@Composable
private fun NoticeRow(
    title: String,
    body: String?,
    meta: String,
    showTopDivider: Boolean,
) {
    val colors = HamsaTheme.colors
    Column {
        if (showTopDivider) HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = HamsaTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
        ) {
            IconTile(icon = Icons.Outlined.Campaign, modifier = Modifier.padding(top = 2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = HamsaTheme.type.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                if (!body.isNullOrBlank()) {
                    Text(
                        text = body,
                        style = HamsaTheme.type.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Text(
                    text = meta,
                    style = HamsaTheme.type.labelMedium,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
