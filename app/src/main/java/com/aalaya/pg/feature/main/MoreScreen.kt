package com.aalaya.pg.feature.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aalaya.pg.core.data.UserSession
import com.aalaya.pg.navigation.Routes
import com.aalaya.pg.ui.components.DestructiveButton
import com.aalaya.pg.ui.components.GradientIdentityCard
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.ListRow
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.theme.HamsaTheme

private data class MoreLink(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
)

/**
 * The staff "More" tab — everything that doesn't fit the five bottom-nav slots.
 * The design's desktop sidebar lists these; on mobile they collapse to here.
 */
@Composable
fun MoreScreen(
    session: UserSession,
    onOpen: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val colors = HamsaTheme.colors
    val displayName = session.fullName?.takeIf { it.isNotBlank() } ?: session.role.label
    val manage = buildList {
        add(MoreLink("Branches", "Rooms, beds and residents", Routes.BRANCHES, Icons.Outlined.Apartment))
        add(MoreLink("Tenant invitations", "Invite and onboard tenants", Routes.INVITATION_MANAGEMENT, Icons.Outlined.PersonAdd))
        add(MoreLink("Notices", "Broadcast to residents", Routes.NOTICES, Icons.Outlined.Campaign))
        add(MoreLink("Enquiries", "People asking about your PGs", Routes.ENQUIRIES, Icons.Outlined.QuestionAnswer))
        add(MoreLink("My listings", "Publish status & approval", Routes.MY_LISTINGS, Icons.Outlined.Map))
        if (session.role.canApprove) {
            add(MoreLink("Approvals", "Pending change requests", Routes.APPROVALS, Icons.Outlined.VerifiedUser))
        }
    }
    val support = listOf(
        MoreLink("Contact us", "Get help from the Aalaya team", Routes.CONTACT_US, Icons.Outlined.MailOutline),
        MoreLink("Privacy policy", "How we protect your information", Routes.PRIVACY_POLICY, Icons.Outlined.Shield),
        MoreLink("Help & FAQs", "Running your PG day to day", Routes.HELP, Icons.Outlined.HelpOutline),
    )
    val business = buildList {
        // Both require is_owner server-side (set_receipt_template, list_audit)
        // -- a Manager tapping either always gets a Forbidden error, so they're
        // hidden rather than offered and left to fail.
        if (session.role.canApprove) {
            add(MoreLink("Receipt template", "Branding on every receipt", Routes.TEMPLATE, Icons.Outlined.Receipt))
            add(MoreLink("Audit log", "Who changed what, and when", Routes.AUDIT, Icons.Outlined.History))
        }
    }

    HamsaScreen(title = "More") {
        // Identity is display-only for staff. It used to open ProfileScreen,
        // which offered Explore PGs and duplicated the account options now
        // listed directly below — so the tap led somewhere staff never needed.
        // No onClick, no chevron: nothing here suggests it is actionable.
        item("account") {
            GradientIdentityCard(name = displayName, subtitle = session.role.label)
        }

        item("manage") { MoreLinkCard("Manage", manage, onOpen) }

        if (business.isNotEmpty()) {
            item("business") { MoreLinkCard("Business", business, onOpen) }
        }

        // Account options, previously reachable only by tapping the identity
        // card. Grouped and styled exactly like Manage and Business above —
        // same card, same rows, same chevron — so the page reads as one list
        // rather than switching to the resident Profile screen's row style.
        item("support") { MoreLinkCard("Support", support, onOpen) }

        item("appearance") { AppearanceCard() }

        item("signout") {
            DestructiveButton(
                text = "Log out",
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

/**
 * One titled card of navigable rows — the More tab's only list shape.
 *
 * Manage, Business and Support all render through this, so a row added to any
 * group inherits the same divider, chevron and spacing instead of drifting.
 */
@Composable
private fun MoreLinkCard(
    title: String,
    links: List<MoreLink>,
    onOpen: (String) -> Unit,
) {
    val colors = HamsaTheme.colors
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        SectionHead(title)
        links.forEachIndexed { index, link ->
            ListRow(
                title = link.title,
                subtitle = link.subtitle,
                showTopDivider = index > 0,
                onClick = { onOpen(link.route) },
                leading = { IconTile(icon = link.icon) },
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}
