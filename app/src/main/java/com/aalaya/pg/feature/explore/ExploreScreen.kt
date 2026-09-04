package com.aalaya.pg.feature.explore

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaSheet
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.HamsaTopBar
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.KeyValueRow
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.Refreshable
import com.aalaya.pg.ui.components.SearchField
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme
import java.util.Locale

@Composable
fun ExploreScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun findNearby() {
        val loc = LocationProvider.lastKnownLatLng(context)
        if (loc != null) viewModel.nearby(loc.first, loc.second) else viewModel.onLocationUnavailable()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) findNearby() else viewModel.onLocationUnavailable() }

    Column(modifier = Modifier.fillMaxSize().background(HamsaTheme.colors.canvas)) {
        HamsaTopBar(title = "Explore PGs", onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().padding(HamsaTheme.spacing.lg)) {
        SearchField(
            value = state.query,
            onValueChange = viewModel::onQuery,
            placeholder = "Search by name or area",
        )
        Spacer(Modifier.height(HamsaTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
            SecondaryButton(text = "Search", onClick = viewModel::submitSearch, modifier = Modifier.weight(1f))
            SecondaryButton(
                text = "Near me",
                onClick = {
                    if (LocationProvider.hasPermission(context)) findNearby()
                    else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(HamsaTheme.spacing.sm))

        var detail by remember { mutableStateOf<Listing?>(null) }

        when {
            state.loading && state.listings.isEmpty() -> LoadingState(Modifier.padding(horizontal = HamsaTheme.spacing.screenH, vertical = HamsaTheme.spacing.screenTop))
            state.error != null && state.listings.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel::refresh)
            state.listings.isEmpty() -> HamsaCard(modifier = Modifier.fillMaxWidth()) {
                EmptyState(
                    title = "No PGs listed yet",
                    detail = "Published listings show up here. Try \"Near me\" or a different search.",
                    icon = Icons.Outlined.Explore,
                )
            }
            else -> Refreshable(refreshing = state.loading, onRefresh = viewModel::refresh) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
                    items(state.listings, key = { it.branchId }) { listing ->
                        ListingCard(listing, onClick = { detail = listing })
                    }
                }
            }
        }

        detail?.let { l ->
            HamsaSheet(title = l.title, onDismiss = { detail = null }) {
                val location = listOfNotNull(l.area, l.city).joinToString(" · ")
                if (location.isNotBlank()) KeyValueRow("Location", location)
                l.distanceKm?.let {
                    KeyValueRow("Distance", String.format(Locale.US, "%.1f km away", it))
                }
                l.description?.let {
                    Text(
                        it,
                        style = HamsaTheme.type.bodySmall,
                        color = HamsaTheme.colors.textPrimary,
                        modifier = Modifier.padding(top = HamsaTheme.spacing.md),
                    )
                }
                l.amenities?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        "Amenities: $it",
                        style = HamsaTheme.type.bodySmall,
                        color = HamsaTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
                    )
                }
                // Live vacancy — derived from real beds, never typed by the owner.
                if (l.bedsAvailable != null) {
                    Spacer(Modifier.height(HamsaTheme.spacing.sm))
                    val price = l.startingPricePaise?.let { " from ₹${it / 100}" } ?: ""
                    StatusChip(
                        label = "${l.bedsAvailable} bed${if (l.bedsAvailable == 1) "" else "s"} free$price",
                        tone = StatusTone.Success,
                    )
                }
                Spacer(Modifier.height(HamsaTheme.spacing.md))
                EnquireSheet(listing = l, enquiries = viewModel.enquiries)
            }
        }
        }
    }
}

@Composable
private fun ListingCard(listing: Listing, onClick: () -> Unit) {
    HamsaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Outlined.Home)
            Text(
                listing.title,
                style = HamsaTheme.type.titleMedium,
                color = HamsaTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            listing.distanceKm?.let {
                StatusChip(label = String.format(Locale.US, "%.1f km", it), tone = StatusTone.Info)
            }
        }
        val location = listOfNotNull(listing.area, listing.city).joinToString(" · ")
        if (location.isNotBlank()) {
            Text(location, style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.textSecondary)
        }
        listing.description?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.xxs))
            Text(
                it,
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        listing.amenities?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.xxs))
            Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.textTertiary)
        }
        // Live vacancy badge — real beds, not an owner's marketing claim.
        if (listing.bedsAvailable != null) {
            Spacer(Modifier.height(HamsaTheme.spacing.xxs))
            val price = listing.startingPricePaise?.let { " · from ₹${it / 100}" } ?: ""
            Text(
                "${listing.bedsAvailable} bed${if (listing.bedsAvailable == 1) "" else "s"} free$price",
                style = HamsaTheme.type.labelLarge,
                color = HamsaTheme.colors.brand,
            )
        }
    }
}
