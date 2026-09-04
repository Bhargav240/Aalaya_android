package com.aalaya.pg.feature.help

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaScreen
import com.aalaya.pg.ui.components.IconTile
import com.aalaya.pg.ui.theme.HamsaTheme

private data class Faq(val question: String, val answer: String)

/**
 * Who the help content is written for.
 *
 * Help used to serve the resident FAQ to everyone, so an owner asking "how do
 * I get my deposit back?" was reading a tenant's answer. Roles map to an
 * audience here, in one place, rather than through role checks scattered across
 * the screen — [Role.Prospect] and [Role.PlatformOperator] read the resident
 * content because that is what describes the app they can actually see.
 */
enum class HelpAudience { Resident, Owner, Manager }

fun helpAudienceFor(role: Role): HelpAudience = when (role) {
    Role.Owner -> HelpAudience.Owner
    Role.Manager -> HelpAudience.Manager
    Role.Tenant, Role.Prospect, Role.PlatformOperator -> HelpAudience.Resident
}

private val HelpAudience.screenLead: String
    get() = when (this) {
        HelpAudience.Resident -> "How rent, deposits and complaints work at Aalaya."
        HelpAudience.Owner -> "Running your PG: money, staff and approvals."
        HelpAudience.Manager -> "Running your branch day to day."
    }

private val HelpAudience.footerTitle: String
    get() = if (this == HelpAudience.Resident) "Still stuck?" else "Need a hand?"

private val HelpAudience.footerBody: String
    get() = when (this) {
        HelpAudience.Resident ->
            "Your branch manager is the fastest route — they can update your " +
                "phone, emergency contact or room straight away."
        HelpAudience.Owner ->
            "Contact us from the More tab for anything about billing, listings " +
                "or your account."
        HelpAudience.Manager ->
            "Your owner can approve pending changes and update branch settings. " +
                "For anything else, use Contact us on the More tab."
    }

private fun faqsFor(audience: HelpAudience): List<Faq> = when (audience) {
    HelpAudience.Resident -> RESIDENT_FAQS
    HelpAudience.Owner -> OWNER_FAQS
    HelpAudience.Manager -> MANAGER_FAQS
}

// Wording taken from the product blueprint's plain-language rules so the app
// explains dues, deposits and complaints exactly as the business does.
private val RESIDENT_FAQS = listOf(
    Faq(
        "When does my rent show as due?",
        "Dues for a month appear from the 30th of the previous month — so August rent " +
            "shows from 30 July. Pay cash at the desk; once the manager marks it paid, " +
            "your receipt appears in the app.",
    ),
    Faq(
        "How do I get my deposit back?",
        "Give at least 30 days' notice before leaving. Your deposit is returned in cash " +
            "minus fair, itemised deductions — each one with a reason you can see.",
    ),
    Faq(
        "My complaint was marked resolved but isn't fixed",
        "Decline to close it and it moves straight back to \"In progress\". Your manager " +
            "sees it immediately.",
    ),
    Faq(
        "Can I pay online?",
        "Payments are cash for now. When the manager marks your cash payment as paid, a " +
            "downloadable receipt appears in your app.",
    ),
    Faq(
        "Who can see my details?",
        "Only you and your branch staff. Other residents never see your dues or " +
            "complaints, and managers of other branches can't see you at all.",
    ),
)

// Shared by both staff audiences: the parts of the job an owner and a manager
// do identically. Each list below adds what is specific to that role.
private val STAFF_COMMON_FAQS = listOf(
    Faq(
        "How do I record a rent payment?",
        "Open Payments, tap the resident and confirm the amount. A receipt is " +
            "issued and appears in the resident's app straight away.",
    ),
    Faq(
        "I marked the wrong person as paid",
        "Open that resident on the Payments tab and choose \"Mark as unpaid\". " +
            "The payment is voided, the receipt is withdrawn and the balance " +
            "returns to what it was. Every reversal is recorded in the audit log.",
    ),
    Faq(
        "How do I onboard a new tenant?",
        "More → Tenant invitations → invite by email. They sign in with a " +
            "one-time code, verify their identity, and you approve the request. " +
            "The bed is assigned only once you approve.",
    ),
    Faq(
        "A complaint was resolved but the resident reopened it",
        "Residents can decline to close a complaint, which moves it back to " +
            "\"In progress\". Reassign it or update the resident from Complaints.",
    ),
)

private val OWNER_FAQS = STAFF_COMMON_FAQS + listOf(
    Faq(
        "What needs my approval?",
        "Managers cannot edit or delete records directly. Their changes queue " +
            "under More → Approvals for you to approve or reject, so nothing " +
            "changes without an owner signing off.",
    ),
    Faq(
        "Can I change how receipts look?",
        "Yes — More → Receipt template sets the name, address, footer and accent " +
            "colour printed on every receipt for your organisation.",
    ),
    Faq(
        "How do I get my PG listed publicly?",
        "More → My listings, fill in the details and submit for approval. The " +
            "Aalaya team reviews it before it goes live in Explore.",
    ),
    Faq(
        "Who can see what?",
        "Managers are limited to their own branch and see only its residents. " +
            "You see every branch, plus the audit log of who changed what.",
    ),
)

private val MANAGER_FAQS = STAFF_COMMON_FAQS + listOf(
    Faq(
        "Why do my edits say \"pending approval\"?",
        "Edits and deletions are reviewed by the owner before they take effect. " +
            "Your request is saved immediately and applies once approved.",
    ),
    Faq(
        "Why can't I see other branches?",
        "You are assigned to one branch, and residents, rooms and enquiries are " +
            "scoped to it. Ask your owner if you need access to another branch.",
    ),
    Faq(
        "Why can't I open the receipt template or audit log?",
        "Those are owner-only. They change organisation-wide settings and " +
            "records, so they are not part of a branch manager's access.",
    ),
)

/**
 * `.faq` — an accordion of expandable questions. Static content, so it needs no
 * backend; the answers mirror the rules in the product blueprint.
 *
 * The question set is chosen by [audience], so an owner never reads a
 * resident's answers and a manager is never told about owner-only screens.
 */
@Composable
fun HelpScreen(
    audience: HelpAudience,
    onBack: () -> Unit,
) {
    // Reset the open row when switching audience so the accordion never
    // restores an index that the new, shorter list does not have.
    var openIndex by remember(audience) { mutableIntStateOf(0) }
    val faqs = faqsFor(audience)

    HamsaScreen(
        title = "Help & support",
        lead = audience.screenLead,
        onBack = onBack,
    ) {
        item("faqs") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                faqs.forEachIndexed { index, faq ->
                    FaqRow(
                        faq = faq,
                        expanded = openIndex == index,
                        showTopDivider = index > 0,
                        onToggle = { openIndex = if (openIndex == index) -1 else index },
                    )
                }
            }
        }

        item("contact") {
            HamsaCard(modifier = Modifier.fillMaxWidth()) {
                IconTile(
                    icon = Icons.Outlined.MailOutline,
                    modifier = Modifier.padding(bottom = HamsaTheme.spacing.sm),
                )
                Text(
                    audience.footerTitle,
                    style = HamsaTheme.type.titleSmall,
                    color = HamsaTheme.colors.textPrimary,
                )
                Text(
                    audience.footerBody,
                    style = HamsaTheme.type.bodySmall,
                    color = HamsaTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = HamsaTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun FaqRow(
    faq: Faq,
    expanded: Boolean,
    showTopDivider: Boolean,
    onToggle: () -> Unit,
) {
    val colors = HamsaTheme.colors
    Column(modifier = Modifier.animateContentSize()) {
        if (showTopDivider) HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
        ) {
            IconTile(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                size = 32.dp,
                iconSize = 17.dp,
            )
            Text(
                text = faq.question,
                style = HamsaTheme.type.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            // `.faq summary::after` — a "+" that becomes "–" when open.
            Text(
                text = if (expanded) "–" else "+",
                style = HamsaTheme.type.titleMedium,
                color = colors.brand,
            )
        }
        if (expanded) {
            Text(
                text = faq.answer,
                style = HamsaTheme.type.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = HamsaTheme.spacing.md),
            )
        }
    }
}
