package com.aalaya.pg.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

/** Owner/manager view of their own listings, with a "Send for approval" action. */
@Composable
fun MyListingsScreen(
    onBack: () -> Unit,
    viewModel: MyListingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HamsaScreen(
        title = "My listings",
        lead = "Your listings and where they are in the publish pipeline.",
        onBack = onBack,
        refreshing = state.loading,
        onRefresh = viewModel::refresh,
    ) {
        when {
            state.loading && state.listings.isEmpty() -> item("loading") { LoadingState() }
            state.error != null && state.listings.isEmpty() ->
                item("error") { ErrorState(state.error!!, onRetry = viewModel::refresh) }
            state.listings.isEmpty() -> item("empty") {
                EmptyState(
                    title = "No listings yet",
                    detail = "Publish a listing from a branch to see it here.",
                    icon = Icons.Outlined.HomeWork,
                )
            }
            else -> state.listings.forEach { l ->
                item(l.branchId) {
                    MyListingRow(
                        listing = l,
                        busy = state.submittingId == l.branchId,
                        onSubmit = { viewModel.submit(l) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListingRow(
    listing: Listing,
    busy: Boolean,
    onSubmit: () -> Unit,
) {
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Outlined.Home)
            Text(
                listing.title,
                style = HamsaTheme.type.titleSmall,
                color = HamsaTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            StatusChip(label = ListingStatus.fromWire(listing.status).label, tone = ListingStatus.fromWire(listing.status).tone)
        }
        val location = listOfNotNull(listing.area, listing.city).joinToString(" · ")
        if (location.isNotBlank()) {
            Text(location, style = HamsaTheme.type.labelMedium, color = HamsaTheme.colors.textSecondary)
        }
        if (ListingStatus.fromWire(listing.status).canSubmit) {
            Spacer(Modifier.height(HamsaTheme.spacing.sm))
            PrimaryButton(
                text = "Send for approval",
                onClick = onSubmit,
                loading = busy,
                size = ButtonSize.Small,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Mirrors the backend `public_listings.status` state machine (A10). */
enum class ListingStatus(val wire: String, val label: String, val tone: StatusTone, val canSubmit: Boolean) {
    Draft("draft", "Draft", StatusTone.Neutral, true),
    Pending("pending", "Pending review", StatusTone.Warning, false),
    Live("live", "Live", StatusTone.Success, false),
    Rejected("rejected", "Rejected", StatusTone.Error, true),
    Unlisted("unlisted", "Unlisted", StatusTone.Neutral, false),
    Unknown("", "Unknown", StatusTone.Neutral, false);

    companion object {
        fun fromWire(s: String): ListingStatus = entries.firstOrNull { it.wire == s } ?: Unknown
    }
}
