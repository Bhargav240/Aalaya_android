package com.aalaya.pg.feature.tenant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.Floors
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.KeyValueRow
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.theme.HamsaTheme

@Composable
fun AccommodationDetailsScreen(
    onBack: () -> Unit,
    viewModel: TenantHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ledger = state.ledger
    LaunchedEffect(Unit) { viewModel.refresh() }
    HamsaScreen(title = "Accommodation Info", onBack = onBack, refreshing = state.loading, onRefresh = { viewModel.refresh(forceRefresh = true) }) {
        when {
            state.loading && ledger == null -> item("loading") { LoadingState() }
            state.error != null && ledger == null -> item("error") { ErrorState(state.error!!, onRetry = viewModel::refresh) }
            ledger != null -> {
                item("property") {
                    HamsaCard(modifier = Modifier.fillMaxWidth(), color = HamsaTheme.colors.brandSubtle, borderColor = HamsaTheme.colors.brand.copy(alpha = 0.22f)) {
                        IconTile(
                            icon = Icons.Outlined.Apartment,
                            size = 32.dp,
                            iconSize = 17.dp,
                        )
                        Text("Your accommodation", style = HamsaTheme.type.labelMedium, color = HamsaTheme.colors.textSecondary, modifier = Modifier.padding(top = HamsaTheme.spacing.sm))
                        Text(ledger.branchName ?: "Branch not assigned", style = HamsaTheme.type.headlineSmall, color = HamsaTheme.colors.textPrimary)
                    }
                }
                item("room") {
                    HamsaCard(modifier = Modifier.fillMaxWidth()) {
                        IconTile(
                            icon = Icons.Outlined.Bed,
                            size = 32.dp,
                            iconSize = 17.dp,
                            modifier = Modifier.padding(bottom = HamsaTheme.spacing.sm),
                        )
                        SectionHead("Room & bed")
                        KeyValueRow("Room", ledger.roomNumber ?: "Not assigned")
                        // The resident's ledger carries a room number, not a
                        // floor id, so the floor is read off the number the
                        // same way Occupancy and Payments do. No extra call,
                        // and it stays right for rooms with no floor filed.
                        ledger.roomNumber?.let { number ->
                            KeyValueRow("Floor", Floors.labelOf(Floors.keyOf(number)))
                        }
                        KeyValueRow("Bed", ledger.bedLabel ?: "Not assigned")
                        KeyValueRow("Plan", ledger.planKind.replaceFirstChar { it.uppercase() })
                    }
                }
                item("financial") {
                    HamsaCard(modifier = Modifier.fillMaxWidth()) {
                        IconTile(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            tone = IconTone.Success,
                            size = 32.dp,
                            iconSize = 17.dp,
                            modifier = Modifier.padding(bottom = HamsaTheme.spacing.sm),
                        )
                        SectionHead("Financial details")
                        KeyValueRow("Monthly rent", Money.format(ledger.rentPaise))
                        KeyValueRow("Deposit held", Money.format(ledger.depositPaise))
                        KeyValueRow("Paid to date", Money.format(ledger.paidTotalPaise))
                        KeyValueRow("Outstanding", Money.format(ledger.outstandingPaise), valueColor = if (ledger.outstandingPaise > 0) HamsaTheme.colors.errorFg else HamsaTheme.colors.successFg)
                    }
                }
            }
        }
    }
}
