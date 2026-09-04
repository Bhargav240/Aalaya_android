package com.aalaya.pg.feature.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.network.dto.ReceiptDto
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.feature.receipts.ReceiptRow
import com.aalaya.pg.feature.receipts.openPdf
import com.aalaya.pg.feature.receipts.PdfPreviewScreen
import com.aalaya.pg.ui.components.EmptyState
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaDropdown
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.components.StatGrid
import com.aalaya.pg.ui.components.StatTile
import com.aalaya.pg.ui.theme.HamsaTheme
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** The year-month a receipt was issued in, in the device's zone. */
private fun ReceiptDto.yearMonth(): YearMonth =
    Instant.ofEpochSecond(issuedEpoch).atZone(ZoneId.systemDefault()).let {
        YearMonth.of(it.year, it.monthValue)
    }

private fun monthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())

/**
 * The resident's receipts, filtered by year then month — the mockup's "Pick a
 * month to view or download that receipt — no endless scrolling."
 *
 * The month dropdown only offers months that actually have a receipt in the
 * chosen year, so you can never land on an empty selection by accident.
 */
@Composable
fun TenantReceiptsScreen(viewModel: TenantHomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = HamsaTheme.colors
    var previewUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh(includeReceipts = true, includeLedger = false) }

    LaunchedEffect(state.openUri) {
        state.openUri?.let { previewUri = it; viewModel.consumeOpenUri() }
    }

    previewUri?.let { uri ->
        PdfPreviewScreen(uri = uri, onBack = { previewUri = null })
        return
    }

    val now = remember { YearMonth.now() }
    val earliest = remember(state.receipts) {
        state.receipts.minByOrNull { it.issuedEpoch }?.yearMonth()
    }

    // The selectable window runs from the resident's first month through the
    // current one — every month in between is offered even when it holds no
    // receipt, so a gap reads as "nothing that month" rather than vanishing.
    // Months before the start and after today are never offered.
    val start = earliest ?: now
    val years = remember(start, now) { (start.year..now.year).toList().asReversed() }
    var selectedYear by remember(years) { mutableStateOf(years.firstOrNull()) }

    val months = remember(start, now, selectedYear) {
        val year = selectedYear ?: now.year
        val first = if (year == start.year) start.monthValue else 1
        val last = if (year == now.year) now.monthValue else 12
        if (first > last) emptyList() else (first..last).toList().asReversed()
    }
    // Prefer the newest month that actually has a receipt; otherwise the newest
    // month in range, so the screen always opens on something meaningful.
    var selectedMonth by remember(months, state.receipts) {
        val withReceipts = state.receipts
            .map { it.yearMonth() }
            .filter { it.year == selectedYear }
            .map { it.monthValue }
        mutableStateOf(months.firstOrNull { it in withReceipts } ?: months.firstOrNull())
    }

    val shown = state.receipts.filter {
        val ym = it.yearMonth()
        ym.year == selectedYear && ym.monthValue == selectedMonth
    }

    HamsaScreen(
        title = "Receipts",
        lead = "Pick a month to view or download that receipt — no endless scrolling.",
        refreshing = state.loading && state.receipts.isNotEmpty(),
        onRefresh = { viewModel.refresh(includeReceipts = true, includeLedger = false, forceRefresh = true) },
    ) {
        if (state.receiptsLoading && state.receipts.isEmpty()) {
            item("loading") { LoadingState() }
            return@HamsaScreen
        }
        if (!state.receiptsLoading && state.error != null && state.receipts.isEmpty()) {
            item("error") { ErrorState(state.error!!, onRetry = viewModel::refresh) }
            return@HamsaScreen
        }
        if (state.receipts.isEmpty()) {
            item("empty") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No receipts yet",
                        detail = "They appear here the moment a payment is recorded.",
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    )
                }
            }
            return@HamsaScreen
        }

        item("stats") {
            StatGrid(
                tiles = listOf(
                    {
                        StatTile(
                            overline = "Receipts on record",
                            value = state.receipts.size.toString(),
                            caption = "every payment, saved",
                        )
                    },
                    {
                        StatTile(
                            overline = "Records from",
                            value = earliest?.let {
                                "${monthName(it.monthValue).take(3)} ${it.year}"
                            } ?: "—",
                            caption = "your first receipt",
                        )
                    },
                ),
            )
        }

        item("filters") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                SectionHead("Find a receipt")
                Row(horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md)) {
                    HamsaDropdown(
                        label = "Year",
                        options = years,
                        selected = selectedYear,
                        optionLabel = { it.toString() },
                        onSelect = { selectedYear = it; selectedMonth = null },
                        modifier = Modifier.weight(1f),
                    )
                    HamsaDropdown(
                        label = "Month",
                        options = months,
                        selected = selectedMonth,
                        optionLabel = ::monthName,
                        onSelect = { selectedMonth = it },
                        placeholder = "—",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item("none") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No receipt for that month",
                        detail = buildString {
                            append("Nothing was recorded in ")
                            selectedMonth?.let { append("${monthName(it)} ") }
                            append(selectedYear?.toString().orEmpty())
                            append(".")
                        },
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    )
                }
            }
        } else {
            items(shown, key = { it.id }) { r ->
                ReceiptRow(
                    receipt = r,
                    downloading = state.downloadingId == r.id,
                    onDownload = { viewModel.downloadReceipt(r) },
                    onOpen = { viewModel.downloadReceipt(r) },
                )
            }
        }

        item("footnote") {
            Text(
                text = earliest?.let {
                    "Records start from ${monthName(it.monthValue)} ${it.year} — " +
                        "${Money.format(state.receipts.sumOf { r -> r.amountPaise })} paid in total."
                }.orEmpty(),
                style = HamsaTheme.type.labelMedium,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = HamsaTheme.spacing.xs),
            )
        }
    }
}
