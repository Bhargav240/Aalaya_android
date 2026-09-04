package com.aalaya.pg.navigation

import android.net.Uri

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.aalaya.pg.core.domain.model.Role

/** Route keys used by the NavHost. Kept as plain strings for simplicity. */
object Routes {
    const val HOME = "home"
    const val OCCUPANCY = "occupancy"
    const val PAYMENTS = "payments"
    const val RECEIPTS = "receipts"
    const val EXPLORE = "explore"
    const val PROFILE = "profile"
    const val ACCOMMODATION_DETAILS = "accommodation-details"
    const val MORE = "more"
    const val AWAY = "away"

    // Detail routes (arguments appended at navigation time).
    const val BRANCHES = "branches"
    const val CREATE_BRANCH = "create-branch"
    const val BRANCH_DETAIL = "branch"          // branch/{branchId}
    const val ROOMS = "rooms"                    // rooms/{branchId}
    const val FLOORS = "floors"                  // floors/{branchId}
    const val ADD_FLOOR = "add-floor"            // add-floor/{branchId}
    const val RESIDENT_DETAIL = "resident"      // resident/{residentId}
    const val COMPLAINTS = "complaints"
    const val NOTICES = "notices"
    const val APPROVALS = "approvals"
    const val AUDIT = "audit"
    const val TEMPLATE = "receipt-template"
    const val HELP = "help"
    const val CONTACT_US = "contact-us"
    const val PRIVACY_POLICY = "privacy-policy"
    const val ENQUIRIES = "enquiries"
    const val MY_LISTINGS = "my-listings"
    const val INVITATIONS = "invitations"
    const val INVITATION_MANAGEMENT = "invitation-management"
    const val CREATE_INVITATION = "create-invitation"
    const val INVITATION_DETAIL = "invitation"
    const val INVITATION_REVIEW = "invitation-review"
    const val INVITATION_STATUS = "invitation-status"
    const val DIGILOCKER = "digilocker"
    /** room-availability/{sharingCount}?title=..&scope=.. */
    const val ROOM_AVAILABILITY = "room-availability"
}

data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Bottom-nav tabs, following the design's Android navigation.
 *
 * Staff  — Home · Occupancy · Payments · Complaints · More
 * Tenant — Home · Receipts · Complaints · Away · Profile
 *
 * The mockup's tenant nav lists six destinations and shows the first five in
 * the bottom bar (`items.slice(0, 5)`), with **Help** as the sixth — reachable
 * from Profile and from the resident's home screen rather than the bar.
 *
 * Explore remains available from the tenant Profile page so a resident of one
 * property can continue discovering other properties without crowding the bar.
 */
fun tabsForRole(role: Role): List<TabItem> = when {
    role.isStaff -> listOf(
        TabItem(Routes.HOME, "Home", Icons.Outlined.Home),
        TabItem(Routes.OCCUPANCY, "Occupancy", Icons.Outlined.GridView),
        TabItem(Routes.PAYMENTS, "Payments", Icons.Outlined.Payments),
        TabItem(Routes.COMPLAINTS, "Complaints", Icons.Outlined.Build),
        TabItem(Routes.MORE, "More", Icons.Outlined.MoreHoriz),
    )
    role == Role.Tenant -> listOf(
        TabItem(Routes.HOME, "Home", Icons.Outlined.Home),
        TabItem(Routes.RECEIPTS, "Receipts", Icons.AutoMirrored.Outlined.ReceiptLong),
        TabItem(Routes.COMPLAINTS, "Complaints", Icons.Outlined.Build),
        TabItem(Routes.AWAY, "Away", Icons.Outlined.CalendarToday),
        TabItem(Routes.PROFILE, "Profile", Icons.Outlined.Person),
    )
    else -> listOf(
        TabItem(Routes.HOME, "Explore", Icons.Outlined.Search),
        TabItem(Routes.INVITATIONS, "Invites", Icons.Outlined.MailOutline),
        TabItem(Routes.PROFILE, "Profile", Icons.Outlined.Person),
    )
}

fun invitationDetailRoute(invitationId: String): String =
    "${Routes.INVITATION_DETAIL}/$invitationId"

fun invitationReviewRoute(invitationId: String): String =
    "${Routes.INVITATION_REVIEW}/$invitationId"

fun digilockerRoute(invitationId: String): String =
    "${Routes.DIGILOCKER}/$invitationId"

/**
 * Home's room-summary row → the rooms behind it. Only the sharing count and
 * the labels travel; the branch scope is re-read from BranchContext so the two
 * screens cannot drift apart.
 */
fun roomAvailabilityRoute(sharingCount: Int, title: String, scopeLabel: String): String =
    "${Routes.ROOM_AVAILABILITY}/$sharingCount" +
        "?title=${Uri.encode(title)}&scope=${Uri.encode(scopeLabel)}"

/** The add-floor form for a branch. `floors` seeds the new floor's position. */
fun addFloorRoute(branchId: String, branchName: String, existingFloors: Int): String =
    "${Routes.ADD_FLOOR}/$branchId?name=${Uri.encode(branchName)}&floors=$existingFloors"
