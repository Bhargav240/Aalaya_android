package com.aalaya.pg.feature.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.core.domain.model.ComplaintState
import com.aalaya.pg.core.domain.model.Floors
import com.aalaya.pg.core.network.dto.NoticeDto
import com.aalaya.pg.core.util.DateFmt
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.navigation.Routes
import com.aalaya.pg.ui.components.AutoSizeText
import com.aalaya.pg.ui.components.Avatar
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.IconTone
import com.aalaya.pg.ui.components.ListRow
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.NoteBanner
import com.aalaya.pg.ui.components.QuickActionTile
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.pressScale
import com.aalaya.pg.ui.components.toneForComplaint
import com.aalaya.pg.ui.components.toneForStatus
import com.aalaya.pg.ui.theme.HamsaTheme
import com.aalaya.pg.ui.theme.HeroAmountTextStyle
import com.aalaya.pg.ui.theme.OverlineTextStyle

/**
 * Resident home, ordered by what the resident opens the app to find out:
 *
 *   1. Needs attention — identity verification, when it is still outstanding
 *   2. Balance         — what they owe, as the one headline figure
 *   3. Your stay       — room, bed and branch: reference, not headline
 *   4. Notices         — the latest thing the property said
 *
 * An earlier version put stay and balance in a single card and ranked them
 * backwards: the room number took `titleLarge` while the amount owed sat in the
 * card's lower half at `bodySmall`, so the fact that never changes outshouted
 * the one that does. The two are separate cards now and the balance carries the
 * same 42sp treatment the staff dashboard gives its outstanding figure, so both
 * homes read as the same product.
 *
 * The balance card leads to Receipts rather than offering a pay action: rent is
 * recorded by staff, so the app cannot take a payment and must not imply it can.
 */
@Composable
fun TenantHomeScreen(
    tenantName: String,
    identityVerified: Boolean = true,
    onOpen: (String) -> Unit = {},
    viewModel: TenantHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors
    LaunchedEffect(Unit) {
        viewModel.refresh(includeNotices = true, includeReceipts = true, includeComplaints = true)
    }

    val resident = state.ledger
    val displayName = resident?.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: tenantName

    // "Room 203 · Floor 2 · HSR Layout" — the three facts that answer "where am
    // I staying?" without opening anything. Floor is read off the room number,
    // the same fallback the rest of the app uses.
    val stayLine = resident?.let { stay ->
        listOfNotNull(
            stay.roomNumber?.let { "Room $it" },
            stay.roomNumber?.let { Floors.labelOf(Floors.keyOf(it)) },
            stay.branchName,
        ).joinToString(" · ").takeIf { it.isNotBlank() }
    }

    HamsaScreen(
        title = "",
        showTopBar = false,
        refreshing = state.loading,
        onRefresh = {
            viewModel.refresh(
                includeNotices = true,
                includeReceipts = true,
                includeComplaints = true,
                forceRefresh = true,
            )
        },
    ) {
        item("greeting") {
            // A fixed greeting rather than a time-of-day one: the device clock
            // could disagree with the resident's sense of time (travel, an
            // overnight session), and "Good night" is an odd thing to read
            // while checking a rent balance.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HamsaTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hello, $displayName",
                        style = HamsaTheme.type.headlineSmall,
                        color = colors.textPrimary,
                    )
                    stayLine?.let {
                        Text(
                            text = it,
                            style = HamsaTheme.type.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
                Avatar(
                    name = displayName,
                    size = 44.dp,
                    modifier = Modifier.pressScale { onOpen(Routes.PROFILE) },
                )
            }
        }

        if (resident == null && state.error == null && state.loading) {
            item("loading") { LoadingState() }
        }
        if (resident == null && state.error != null) {
            item("error") {
                ErrorState(state.error!!, onRetry = { viewModel.refresh(forceRefresh = true) })
            }
        }

        resident?.let { stay ->
            // ── 1. Needs attention ──────────────────────────────────────
            // Promoted above the balance: until this is done, onboarding is
            // incomplete and nothing else on the screen can proceed.
            if (!identityVerified) {
                item("attention") {
                    NoteBanner(
                        text = "Verify your identity to finish onboarding",
                        onClick = { onOpen(Routes.INVITATIONS) },
                    )
                }
            }

            // ── 2. Balance ──────────────────────────────────────────────
            item("balance") {
                BalanceCard(
                    outstandingPaise = stay.outstandingPaise,
                    paidTotalPaise = stay.paidTotalPaise,
                    rentPaise = stay.rentPaise,
                    plan = stay.planKind.replaceFirstChar { it.uppercase() },
                    lastPaidOn = state.lastReceipt?.let { DateFmt.epochSeconds(it.issuedEpoch) },
                    onClick = { onOpen(Routes.RECEIPTS) },
                )
            }

            // ── 3. Your stay ────────────────────────────────────────────
            item("stay") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    SectionHead("Your stay")
                    ListRow(
                        title = listOfNotNull(stay.roomNumber, stay.bedLabel)
                            .joinToString(" · ")
                            .ifBlank { "Room not assigned" },
                        subtitle = stay.branchName ?: "Branch not assigned",
                        showTopDivider = false,
                        onClick = { onOpen(Routes.ACCOMMODATION_DETAILS) },
                        leading = { IconTile(icon = Icons.Outlined.Apartment) },
                        trailing = {
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = "View stay details",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }
            }

            // ── 4. Anything open ────────────────────────────────────────
            // Only shown when something is actually being worked on; a
            // resident with nothing outstanding should not see an empty card
            // telling them so.
            val open = state.openComplaints
            if (open.isNotEmpty()) {
                item("complaints") {
                    HamsaCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpen(Routes.COMPLAINTS) },
                    ) {
                        SectionHead("Your complaints")
                        open.take(2).forEachIndexed { index, complaint ->
                            val current = ComplaintState.fromWire(complaint.state)
                            ListRow(
                                title = complaint.title,
                                subtitle = "Raised with the property",
                                showTopDivider = index > 0,
                                leading = {
                                    IconTile(
                                        icon = Icons.Outlined.ReportProblem,
                                        tone = IconTone.Warning,
                                    )
                                },
                                trailing = {
                                    StatusChip(
                                        label = current.label,
                                        tone = toneForComplaint(current),
                                    )
                                },
                            )
                        }
                        if (open.size > 2) {
                            Text(
                                text = "+${open.size - 2} more",
                                style = HamsaTheme.type.labelMedium,
                                color = colors.textTertiary,
                                modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
                            )
                        }
                    }
                }
            }

            // ── 5. Quick Actions ────────────────────────────────────────
            // Only destinations that already exist. There is deliberately no
            // "Pay rent": rent is recorded by staff in cash, so the app cannot
            // take a payment and must not imply that it can.
            item("quick-actions") {
                Column(verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm)) {
                    SectionHead("Quick Actions")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
                    ) {
                        QuickActionTile(
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            label = "Receipts",
                            onClick = { onOpen(Routes.RECEIPTS) },
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionTile(
                            icon = Icons.Outlined.ReportProblem,
                            label = "Complaints",
                            onClick = { onOpen(Routes.COMPLAINTS) },
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionTile(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Away days",
                            onClick = { onOpen(Routes.AWAY) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── 6. Notices ──────────────────────────────────────────────
            item("notices") {
                NoticesCard(
                    notices = state.notices,
                    loading = state.loading,
                    onSeeAll = { onOpen(Routes.NOTICES) },
                )
            }
        }
    }
}

/**
 * The figure a resident opens the app for. Outstanding takes the same 42sp
 * treatment as the staff dashboard's headline so the two homes match; paid-so-far
 * and rent sit under a hairline as context.
 *
 * Rent shows the plan once ("Monthly · ₹12,500"). The old layout stacked
 * "Monthly", the amount and "per month" in a narrow end column, stating the
 * period twice.
 */
@Composable
private fun BalanceCard(
    outstandingPaise: Long,
    paidTotalPaise: Long,
    rentPaise: Long,
    plan: String,
    lastPaidOn: String?,
    onClick: () -> Unit,
) {
    val colors = HamsaTheme.colors
    val owes = outstandingPaise > 0
    HamsaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            // Centred, not top-aligned: against a short figure like ₹0 a
            // top-aligned chevron sits level with the overline and reads as
            // part of the label rather than as the card's affordance.
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (owes) "YOU OWE" else "BALANCE",
                    style = OverlineTextStyle,
                    color = colors.textSecondary,
                )
                AutoSizeText(
                    text = Money.format(outstandingPaise),
                    style = HeroAmountTextStyle,
                    color = if (owes) colors.warningFg else colors.textPrimary,
                    minFontScale = 0.6f,
                    modifier = Modifier.padding(top = HamsaTheme.spacing.xs),
                )
                Text(
                    text = if (owes) "Due now" else "You're all paid up",
                    style = HamsaTheme.type.bodySmall,
                    color = if (owes) colors.textSecondary else colors.successFg,
                    modifier = Modifier.padding(top = HamsaTheme.spacing.xxs),
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "View your receipts",
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }

        HorizontalDivider(
            color = colors.border,
            modifier = Modifier.padding(vertical = HamsaTheme.spacing.lg),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
        ) {
            SupportingFigure(
                label = "Paid so far",
                value = Money.format(paidTotalPaise),
                modifier = Modifier.weight(1f),
            )
            SupportingFigure(
                label = "Rent",
                value = "$plan · ${Money.format(rentPaise)}",
                modifier = Modifier.weight(1f),
            )
        }

        // Only shown once there is a receipt to point at, so a resident who has
        // never paid isn't told about a payment that never happened.
        lastPaidOn?.let { on ->
            Text(
                text = "Last payment $on",
                style = HamsaTheme.type.labelMedium,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = HamsaTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun SupportingFigure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = HamsaTheme.type.labelMedium,
            color = HamsaTheme.colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AutoSizeText(
            text = value,
            style = HamsaTheme.type.titleMedium,
            color = HamsaTheme.colors.textPrimary,
            minFontScale = 0.7f,
        )
    }
}

/**
 * The latest notice. Header lives inside the card, matching how every section
 * on the staff dashboard is built — it used to sit outside, which made the two
 * homes look like different apps.
 */
@Composable
private fun NoticesCard(
    notices: List<NoticeDto>,
    loading: Boolean,
    onSeeAll: () -> Unit,
) {
    val colors = HamsaTheme.colors
    val notice = notices.firstOrNull()
    if (notice == null && loading) return

    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        SectionHead(
            title = "Notices",
            trailing = if (notices.size > 1) {
                { TextButton(onClick = onSeeAll) { Text("See all", color = colors.brand) } }
            } else {
                null
            },
        )
        if (notice == null) {
            Text(
                text = "No notices yet. Anything the property announces shows up here.",
                style = HamsaTheme.type.bodySmall,
                color = colors.textTertiary,
            )
        } else {
            ListRow(
                title = notice.title,
                // The date, not a body preview. ListRow's subtitle has no line
                // cap and is shared by half the app, so a 120-character excerpt
                // wrapped to three lines here — a block, not a scannable row.
                // The title carries the meaning and the full text is one tap away.
                subtitle = DateFmt.epochSeconds(notice.createdEpoch),
                showTopDivider = false,
                onClick = onSeeAll,
                leading = { IconTile(icon = Icons.Outlined.Campaign) },
                trailing = {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "See all notices",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}
