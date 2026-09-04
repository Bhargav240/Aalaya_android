package com.aalaya.pg.feature.branches

import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.HamsaAlertDialog
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.pressScale
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.Refreshable
import com.aalaya.pg.ui.theme.HamsaTheme
import com.aalaya.pg.ui.components.HamsaTopBar

@Composable
fun BranchesScreen(
    canManage: Boolean,
    // Managers may REQUEST edits/deletes (maker-checker); creating is owner-only.
    canEdit: Boolean = canManage,
    onOpenBranch: (branchId: String, name: String) -> Unit,
    onAddBranch: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: BranchesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuForId by remember { mutableStateOf<String?>(null) }
    var editBranch by remember { mutableStateOf<Pair<String, String>?>(null) } // id to current name
    var editName by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(HamsaTheme.colors.canvas)) {
    HamsaTopBar(
        title = "Branches",
        onBack = onBack,
        actions = {
            if (canManage) {
                PrimaryButton(
                    text = "Add branch",
                    onClick = onAddBranch,
                    size = ButtonSize.Small,
                )
            }
        },
    )
    Scaffold(
        containerColor = HamsaTheme.colors.canvas,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.branches.isEmpty() -> LoadingState(Modifier.padding(horizontal = HamsaTheme.spacing.screenH, vertical = HamsaTheme.spacing.screenTop))
                state.error != null && state.branches.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel::refresh)
                state.branches.isEmpty() -> HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No branches yet",
                        detail = if (canManage) {
                            "Add your first branch to start tracking rooms and residents."
                        } else {
                            "Your owner hasn't added any branches yet."
                        },
                        icon = Icons.Outlined.Apartment,
                    )
                }
                else -> Refreshable(refreshing = state.loading, onRefresh = viewModel::refresh) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(HamsaTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    ) {
                    items(state.branches, key = { it.id }) { branch ->
                        HamsaCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale { onOpenBranch(branch.id, branch.name) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                            ) {
                                IconTile(icon = Icons.Outlined.Apartment)
                                Text(
                                    branch.name,
                                    style = HamsaTheme.type.titleMedium,
                                    color = HamsaTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (canEdit) {
                                    Box {
                                        IconButton(onClick = { menuForId = branch.id }) {
                                            Icon(
                                                Icons.Outlined.MoreVert,
                                                contentDescription = "Branch actions",
                                                tint = HamsaTheme.colors.textTertiary,
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuForId == branch.id,
                                            onDismissRequest = { menuForId = null },
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Rename / address") },
                                                onClick = {
                                                    menuForId = null
                                                    editName = branch.name
                                                    editAddress = ""
                                                    editBranch = branch.id to branch.name
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = { menuForId = null; deleteId = branch.id },
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = HamsaTheme.colors.textTertiary,
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

    editBranch?.let { (id, _) ->
        FormDialog(
            title = "Edit branch",
            confirmText = "Save",
            confirmEnabled = editName.isNotBlank() || editAddress.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.renameBranch(id, editName, editAddress) { editBranch = null } },
            onDismiss = { editBranch = null },
        ) {
            HamsaTextField(value = editName, onValueChange = { editName = it }, label = "Branch name")
            androidx.compose.foundation.layout.Spacer(
                Modifier.padding(top = HamsaTheme.spacing.xs),
            )
            HamsaTextField(
                value = editAddress,
                onValueChange = { editAddress = it },
                label = "Address (leave blank to keep)",
            )
        }
    }

    deleteId?.let { id ->
        HamsaAlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Delete branch?", style = HamsaTheme.type.titleMedium) },
            text = {
                Text(
                    "The branch is hidden, never wiped. Manager deletions wait for the owner's approval.",
                    style = HamsaTheme.type.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteId = null; viewModel.deleteBranch(id) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } },
        )
    }
}
