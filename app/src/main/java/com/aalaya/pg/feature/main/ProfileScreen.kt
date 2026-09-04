package com.aalaya.pg.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.aalaya.pg.core.data.ThemeMode
import com.aalaya.pg.core.data.UserSession
import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.navigation.Routes
import com.aalaya.pg.ui.components.DestructiveButton
import com.aalaya.pg.ui.components.GradientIdentityCard
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.components.ListRow
import com.aalaya.pg.ui.components.SectionHead
import com.aalaya.pg.ui.components.SelectableChip
import com.aalaya.pg.ui.components.StatusChip
import com.aalaya.pg.ui.components.StatusTone
import com.aalaya.pg.ui.theme.HamsaTheme

@Composable
fun ProfileScreen(
    session: UserSession,
    onOpen: (String) -> Unit,
    onSignOut: () -> Unit,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    val colors = HamsaTheme.colors
    val isTenant = session.role == Role.Tenant
    val displayName = session.fullName?.takeIf { it.isNotBlank() }
        ?: session.role.label

    HamsaScreen(
        title = "Profile",
        onBack = if (showBack) onBack else null,
    ) {
        item("identity") {
            GradientIdentityCard(
                name = displayName,
                subtitle = session.role.label,
                trailing = {
                    if (isTenant && session.verified) {
                        StatusChip(label = "Verified", tone = StatusTone.Success)
                    }
                },
            )
        }

        if (isTenant) {
            item("stay") { TenantStayCard(onOpen) }
        }

        // Discovery is a resident/prospect concern. Staff run properties rather
        // than shop for them, and this screen is not on their navigation at all
        // (their equivalent surface is the More tab).
        if (!session.role.isStaff) {
            item("explore") {
                HamsaCard(modifier = Modifier.fillMaxWidth()) {
                    ListRow(
                        title = "Explore PGs",
                        subtitle = "Discover available rooms and stays",
                        showTopDivider = false,
                        onClick = { onOpen(Routes.EXPLORE) },
                        leading = { ProfileIcon(Icons.Outlined.Explore) },
                        trailing = { Icon(Icons.Outlined.ChevronRight, null, tint = colors.brand) },
                    )
                }
            }
        }

        // A resident cares about their stay, not about identifiers. The stay
        // card is only composed for tenants so its ledger call is never made
        // by staff, who would be refused it.
        if (isTenant) {
            item("help") {
                ProfileLink(
                    title = "Help & FAQs",
                    subtitle = "Rent, deposits and complaints",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = { onOpen(Routes.HELP) },
                )
            }
        }

        item("contact") {
            ProfileLink(
                title = "Contact us",
                subtitle = "Get help from the Aalaya team",
                icon = Icons.AutoMirrored.Outlined.ContactSupport,
                onClick = { onOpen(Routes.CONTACT_US) },
            )
        }

        item("privacy") {
            ProfileLink(
                title = "Privacy policy",
                subtitle = "How we protect your information",
                icon = Icons.Outlined.PrivacyTip,
                onClick = { onOpen(Routes.PRIVACY_POLICY) },
            )
        }

        item("appearance") { AppearanceCard() }

        // No Administration block here: staff no longer reach this screen (the
        // More tab's identity card is not a link any more), and More already
        // owns Receipt template and Audit log under "Business". Keeping a
        // second copy would be unreachable code.

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

@Composable
private fun ProfileAccordion(
    title: String,
    subtitle: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    var expanded by remember { mutableStateOf(false) }
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        ListRow(
            title = title,
            subtitle = subtitle,
            showTopDivider = false,
            onClick = { expanded = !expanded },
            leading = { ProfileIcon(icon) },
            trailing = {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = HamsaTheme.colors.textSecondary,
                )
            },
        )
        if (expanded) {
            Text(
                content,
                style = HamsaTheme.type.bodySmall,
                color = HamsaTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = HamsaTheme.spacing.sm, vertical = HamsaTheme.spacing.sm),
            )
        }
    }
}

@Composable
fun ProfileIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    IconTile(icon = icon, size = 42.dp, radius = HamsaTheme.radii.md, iconSize = 20.dp)
}

@Composable
fun ProfileLink(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        ListRow(
            title = title,
            subtitle = subtitle,
            showTopDivider = false,
            onClick = onClick,
            leading = { ProfileIcon(icon) },
            trailing = { Icon(Icons.Outlined.ChevronRight, null, tint = HamsaTheme.colors.brand) },
        )
    }
}

/** Light/dark/system — a device-local preference, same for every role. */
@Composable
fun AppearanceCard(viewModel: ThemeViewModel = hiltViewModel()) {
    val mode by viewModel.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
    val colors = HamsaTheme.colors

    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        SectionHead("Appearance")
        Row(
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
            modifier = Modifier.fillMaxWidth().padding(top = HamsaTheme.spacing.xs),
        ) {
            listOf(
                ThemeMode.System to "System",
                ThemeMode.Light to "Light",
                ThemeMode.Dark to "Dark",
            ).forEach { (value, label) ->
                SelectableChip(
                    label = label,
                    selected = mode == value,
                    onClick = { viewModel.setMode(value) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            "System matches your device's setting.",
            style = HamsaTheme.type.labelMedium,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = HamsaTheme.spacing.xs),
        )
    }
}

/**
 * "Your stay" — the resident's plan and money, read from their own ledger.
 *
 * The design also lists branch, room/bed, joined date, phone, emergency contact
 * and registered Aadhaar name. None of those reach the client: `/me` returns
 * only ids plus a name, and `LedgerDto` carries no placement or join date, so
 * they're omitted rather than shown blank.
 */
@Composable
private fun TenantStayCard(onOpen: (String) -> Unit) {
    HamsaCard(modifier = Modifier.fillMaxWidth()) {
        ListRow(
            title = "Your stay",
            subtitle = "Accommodation details",
            showTopDivider = false,
            onClick = { onOpen(Routes.ACCOMMODATION_DETAILS) },
            leading = { ProfileIcon(Icons.Outlined.Apartment) },
            trailing = { Text("View", style = HamsaTheme.type.labelMedium, color = HamsaTheme.colors.brand) },
        )
    }
}
