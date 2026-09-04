package com.aalaya.pg.feature.residents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.FormDialog
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.HamsaTopBar
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.Refreshable
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.toneForStatus
import com.aalaya.pg.ui.theme.HamsaTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BranchDetailScreen(
    branchName: String,
    canManage: Boolean,
    onOpenResident: (residentId: String, name: String) -> Unit,
    onOpenRooms: () -> Unit,
    onOpenFloors: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: ResidentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf(NewResidentForm()) }
    var showPublish by remember { mutableStateOf(false) }
    var listingForm by remember { mutableStateOf(ListingForm()) }

    Scaffold(
        containerColor = HamsaTheme.colors.canvas,
        topBar = { HamsaTopBar(title = branchName.ifBlank { "Branch" }, onBack = onBack, actions = {
            if (canManage) {
                PrimaryButton(
                    text = "Add resident",
                    onClick = { form = NewResidentForm(); showAdd = true },
                    size = ButtonSize.Small,
                )
            }
        }) },
    ) { padding ->
        Refreshable(
            refreshing = state.loading && state.residents.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.loading && state.residents.isEmpty() -> LoadingState(Modifier.padding(horizontal = HamsaTheme.spacing.screenH, vertical = HamsaTheme.spacing.screenTop))
                state.error != null && state.residents.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel::refresh)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(HamsaTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text("Residents", style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textTertiary)
                            Row {
                                if (canManage) {
                                    androidx.compose.material3.TextButton(onClick = { listingForm = ListingForm(); showPublish = true }) { Text("Publish") }
                                }
                                androidx.compose.material3.TextButton(onClick = onOpenFloors) { Text("Floors") }
                                androidx.compose.material3.TextButton(onClick = onOpenRooms) { Text("Rooms & beds") }
                            }
                        }
                    }
                    if (state.residents.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No residents yet",
                                detail = if (canManage) {
                                    "Add a resident, or place one into a free bed from Occupancy."
                                } else {
                                    "Nobody has been placed in this branch yet."
                                },
                                icon = Icons.Outlined.Person,
                            )
                        }
                    } else {
                        items(state.residents, key = { it.id }) { r ->
                            HamsaCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenResident(r.id, r.fullName) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
                                ) {
                                    IconTile(icon = Icons.Outlined.Person, size = 32.dp, iconSize = 17.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(r.fullName, style = HamsaTheme.type.titleMedium, color = HamsaTheme.colors.textPrimary)
                                        Text(
                                            "${r.planKind.replaceFirstChar { it.uppercase() }} plan",
                                            style = HamsaTheme.type.bodySmall,
                                            color = HamsaTheme.colors.textTertiary,
                                        )
                                    }
                                    StatusChip(label = r.status.replaceFirstChar { it.uppercase() }, tone = toneForStatus(r.status))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        FormDialog(
            title = "Add resident",
            confirmText = "Create",
            confirmEnabled = form.fullName.isNotBlank(),
            saving = state.saving,
            error = state.saveError,
            onConfirm = { viewModel.addResident(form) { showAdd = false } },
            onDismiss = { showAdd = false },
        ) {
            HamsaTextField(value = form.fullName, onValueChange = { form = form.copy(fullName = it) }, label = "Full name")
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = form.phone, onValueChange = { form = form.copy(phone = it) }, label = "Phone (optional)", keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            Text("Plan", style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.xs)) {
                PlanKind.entries.forEach { plan ->
                    SelectableChip(
                        label = plan.label,
                        selected = form.plan == plan,
                        onClick = { form = form.copy(plan = plan) },
                    )
                }
            }
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = form.rentRupees, onValueChange = { form = form.copy(rentRupees = it) }, label = "Rent (₹)", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = form.depositRupees, onValueChange = { form = form.copy(depositRupees = it) }, label = "Deposit (₹)", keyboardType = KeyboardType.Number)
        }
    }

    if (showPublish) {
        FormDialog(
            title = "Publish to Explore",
            confirmText = "Publish",
            confirmEnabled = listingForm.title.isNotBlank(),
            saving = state.publishing,
            error = state.publishError,
            onConfirm = { viewModel.publishListing(listingForm) { showPublish = false } },
            onDismiss = { showPublish = false },
        ) {
            Text(
                "Lists this branch publicly so seekers can discover it.",
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.textTertiary,
            )
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = listingForm.title, onValueChange = { listingForm = listingForm.copy(title = it) }, label = "Listing title")
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = listingForm.area, onValueChange = { listingForm = listingForm.copy(area = it) }, label = "Area (optional)")
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = listingForm.city, onValueChange = { listingForm = listingForm.copy(city = it) }, label = "City (optional)")
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = listingForm.description, onValueChange = { listingForm = listingForm.copy(description = it) }, label = "Description (optional)", singleLine = false)
            Spacer(Modifier.height(HamsaTheme.spacing.xs))
            HamsaTextField(value = listingForm.amenities, onValueChange = { listingForm = listingForm.copy(amenities = it) }, label = "Amenities (optional)")
        }
    }
}
