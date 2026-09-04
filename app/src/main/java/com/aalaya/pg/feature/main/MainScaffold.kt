package com.aalaya.pg.feature.main

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.aalaya.pg.core.data.UserSession
import com.aalaya.pg.feature.approvals.ApprovalsScreen
import com.aalaya.pg.feature.audit.AuditScreen
import com.aalaya.pg.feature.away.AwayScreen
import com.aalaya.pg.feature.branches.BranchesScreen
import com.aalaya.pg.feature.branches.CreateBranchScreen
import com.aalaya.pg.feature.complaints.ComplaintsScreen
import com.aalaya.pg.feature.dashboard.RoomAvailabilityScreen
import com.aalaya.pg.feature.dashboard.StaffDashboardScreen
import com.aalaya.pg.feature.enquiries.EnquiriesScreen
import com.aalaya.pg.feature.explore.ExploreScreen
import com.aalaya.pg.feature.explore.MyListingsScreen
import com.aalaya.pg.feature.floors.AddFloorScreen
import com.aalaya.pg.feature.floors.FloorsScreen
import com.aalaya.pg.feature.help.ContactUsScreen
import com.aalaya.pg.feature.help.HelpScreen
import com.aalaya.pg.feature.help.PrivacyPolicyScreen
import com.aalaya.pg.feature.help.helpAudienceFor
import com.aalaya.pg.feature.invitations.CreateInvitationScreen
import com.aalaya.pg.feature.invitations.DigiLockerVerificationScreen
import com.aalaya.pg.feature.invitations.InvitationDetailScreen
import com.aalaya.pg.feature.invitations.InvitationListScreen
import com.aalaya.pg.feature.invitations.InvitationManagementScreen
import com.aalaya.pg.feature.invitations.InvitationReviewScreen
import com.aalaya.pg.feature.invitations.TenantOnboardingStatusScreen
import com.aalaya.pg.feature.notices.NoticesScreen
import com.aalaya.pg.feature.occupancy.OccupancyScreen
import com.aalaya.pg.feature.payments.PaymentsScreen
import com.aalaya.pg.feature.residents.BranchDetailScreen
import com.aalaya.pg.feature.residents.ResidentDetailScreen
import com.aalaya.pg.feature.rooms.RoomsScreen
import com.aalaya.pg.feature.template.TemplateScreen
import com.aalaya.pg.feature.tenant.AccommodationDetailsScreen
import com.aalaya.pg.feature.tenant.TenantHomeScreen
import com.aalaya.pg.feature.tenant.TenantReceiptsScreen
import com.aalaya.pg.navigation.Routes
import com.aalaya.pg.navigation.addFloorRoute
import com.aalaya.pg.navigation.digilockerRoute
import com.aalaya.pg.navigation.invitationDetailRoute
import com.aalaya.pg.navigation.invitationReviewRoute
import com.aalaya.pg.navigation.tabsForRole
import com.aalaya.pg.ui.theme.HamsaTheme
import kotlinx.coroutines.flow.SharedFlow

/**
 * Authenticated shell: role-aware bottom navigation over a NavHost.
 *
 * Screens own their own top app bar (via `HamsaScreen`), so this scaffold only
 * supplies the nav bar and the app-wide snackbar. That keeps the large-title
 * bar and the screen's scrolling content in one composable per screen, which is
 * how the design lays them out.
 */
@Composable
fun MainScaffold(
    session: UserSession,
    snackbarMessages: SharedFlow<String>,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val tabs = tabsForRole(session.role)
    val canManage = session.role.canManage
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = HamsaTheme.colors

    LaunchedEffect(Unit) {
        snackbarMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isTab = tabs.any { it.route == currentRoute }
    val back: () -> Unit = { navController.popBackStack() }

    // Plain forward push, guarded with launchSingleTop so double-tapping the
    // same list row/card doesn't push two copies of the destination (a second
    // Back press would otherwise land on a duplicate instead of the list).
    val push: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }

    // Home is the graph's own start destination, and AndroidX Navigation has a
    // long-standing bug where navigate()-ing to the start destination via
    // popUpTo(startId){saveState=true}+launchSingleTop+restoreState silently
    // no-ops (confirmed: the system back button pops to Home fine, but this
    // navigate() call does not). Home is never removed from the bottom of the
    // stack, so a plain pop always reaches it.
    //
    // Tabs deliberately do NOT save/restore their back-stack entry. Doing so
    // preserved the entry's ViewModelStore and rememberSaveable bundle, so
    // opening Payments, filtering to a floor, leaving for Home and coming back
    // showed the old filter instead of a fresh screen — the same for every
    // tab's local selections.
    //
    // Nothing real is lost by dropping it: what should outlive a tab switch
    // already lives above the screen (session in SessionManager, branch scope
    // in BranchContext, data in the repositories), while what resets here is
    // exactly the transient layer — chosen floor and room, search text,
    // filter chips, expanded cards, half-open sheets.
    //
    // Pushing a detail screen and coming back is untouched: that entry is never
    // popped, so returning from a resident or a receipt keeps its context.
    val navigateToTab: (String) -> Unit = { route ->
        if (route != currentRoute) {
            if (route == Routes.HOME) {
                navController.popBackStack(Routes.HOME, inclusive = false)
            } else {
                navController.navigate(route) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                }
            }
        }
    }

    // In-screen quick actions (Home/More/Profile cards) can link straight to a
    // tab route -- route those through the same tab-navigation logic as the
    // bottom bar. Everything else is a plain forward push, guarded with
    // launchSingleTop so repeatedly tapping the same card doesn't pile up
    // duplicate back-stack entries.
    val openRoute: (String) -> Unit = { route ->
        if (tabs.any { it.route == route }) {
            navigateToTab(route)
        } else {
            push(route)
        }
    }

    Scaffold(
        containerColor = colors.canvas,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                // `.toast` — near-black pill with a light label, not Material's
                // default inverseSurface treatment.
                Snackbar(
                    snackbarData = data,
                    containerColor = colors.toast,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    actionColor = colors.toastTick,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(HamsaTheme.radii.md),
                )
            }
        },
        bottomBar = {
            if (isTab) {
                Column {
                    HorizontalDivider(color = colors.border)
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                    ) {
                        val current = backStack?.destination
                        tabs.forEach { tab ->
                            val selected = current?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = HamsaTheme.type.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.brand,
                                    selectedTextColor = colors.brand,
                                    indicatorColor = colors.brandSubtle,
                                    unselectedIconColor = colors.textTertiary,
                                    unselectedTextColor = colors.textTertiary,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                Routes.HOME,
                deepLinks = listOf(navDeepLink { uriPattern = "hamsavilas://app/${Routes.HOME}" }),
            ) {
                if (session.role.isStaff) {
                    StaffDashboardScreen(
                        isOwner = session.role.canApprove,
                        staffName = session.fullName?.trim()?.takeIf { it.isNotEmpty() }
                            ?: session.role.label,
                        onOpen = openRoute,
                    )
                } else if (session.role == com.aalaya.pg.core.domain.model.Role.Tenant) {
                    TenantHomeScreen(
                        tenantName = session.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: "there",
                        identityVerified = session.verified,
                        onOpen = openRoute,
                    )
                } else {
                    ExploreScreen()
                }
            }

            composable(
                Routes.OCCUPANCY,
                deepLinks = listOf(navDeepLink { uriPattern = "hamsavilas://app/${Routes.OCCUPANCY}" }),
            ) {
                OccupancyScreen(
                    canManage = canManage,
                    onOpenResident = { id, name ->
                        push("${Routes.RESIDENT_DETAIL}/$id?name=${Uri.encode(name)}")
                    },
                    onAddFloor = { branchId, branchName, floors ->
                        push(addFloorRoute(branchId, branchName, floors))
                    },
                )
            }

            composable(
                Routes.MORE,
                deepLinks = listOf(navDeepLink { uriPattern = "hamsavilas://app/${Routes.MORE}" }),
            ) {
                MoreScreen(
                    session = session,
                    onOpen = openRoute,
                    onSignOut = onSignOut,
                )
            }

            composable(Routes.BRANCHES) {
                BranchesScreen(
                    canManage = session.role.canApprove,
                    canEdit = canManage,
                    onBack = back,
                    onOpenBranch = { id, name ->
                        push("${Routes.BRANCH_DETAIL}/$id?name=${Uri.encode(name)}")
                    },
                    onAddBranch = { push(Routes.CREATE_BRANCH) },
                )
            }

            composable(Routes.CREATE_BRANCH) {
                CreateBranchScreen(
                    onBack = back,
                    onCreated = back,
                )
            }

            composable(
                route = "${Routes.BRANCH_DETAIL}/{branchId}?name={name}",
                arguments = listOf(
                    navArgument("branchId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val branchId = entry.arguments?.getString("branchId").orEmpty()
                val branchName = entry.arguments?.getString("name").orEmpty()
                BranchDetailScreen(
                    branchName = branchName,
                    canManage = canManage,
                    onBack = back,
                    onOpenResident = { id, name ->
                        push("${Routes.RESIDENT_DETAIL}/$id?name=${Uri.encode(name)}")
                    },
                    onOpenRooms = {
                        push("${Routes.ROOMS}/$branchId?name=${Uri.encode(branchName)}")
                    },
                    onOpenFloors = {
                        push("${Routes.FLOORS}/$branchId?name=${Uri.encode(branchName)}")
                    },
                )
            }

            composable(
                route = "${Routes.FLOORS}/{branchId}?name={name}",
                arguments = listOf(
                    navArgument("branchId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                FloorsScreen(
                    branchId = entry.arguments?.getString("branchId").orEmpty(),
                    branchName = entry.arguments?.getString("name").orEmpty(),
                    onBack = back,
                )
            }

            composable(
                route = "${Routes.ROOMS}/{branchId}?name={name}",
                arguments = listOf(
                    navArgument("branchId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                RoomsScreen(
                    branchName = entry.arguments?.getString("name").orEmpty(),
                    canManage = canManage,
                    onBack = back,
                )
            }

            composable(
                route = "${Routes.RESIDENT_DETAIL}/{residentId}?name={name}",
                arguments = listOf(
                    navArgument("residentId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                ResidentDetailScreen(
                    residentName = entry.arguments?.getString("name").orEmpty(),
                    canManage = canManage,
                    onBack = back,
                    onDeleted = { navController.popBackStack() },
                )
            }

            composable(
                Routes.PAYMENTS,
                deepLinks = listOf(navDeepLink { uriPattern = "hamsavilas://app/${Routes.PAYMENTS}" }),
            ) {
                PaymentsScreen(
                    onOpenResident = { id, name ->
                        push("${Routes.RESIDENT_DETAIL}/$id?name=${Uri.encode(name)}")
                    },
                )
            }

            composable(
                Routes.RECEIPTS,
                deepLinks = listOf(navDeepLink { uriPattern = "hamsavilas://app/${Routes.RECEIPTS}" }),
            ) { TenantReceiptsScreen() }
            composable(
                Routes.AWAY,
                deepLinks = listOf(navDeepLink { uriPattern = "hamsavilas://app/${Routes.AWAY}" }),
            ) { AwayScreen() }
            composable(Routes.EXPLORE) {
                ExploreScreen(onBack = if (tabs.any { it.route == Routes.EXPLORE }) null else back)
            }
            composable(
                Routes.PROFILE,
                deepLinks = listOf(navDeepLink { uriPattern = "hamsavilas://app/${Routes.PROFILE}" }),
            ) {
                ProfileScreen(
                    session = session,
                    showBack = session.role.isStaff,
                    onBack = back,
                    onOpen = openRoute,
                    onSignOut = onSignOut,
                )
            }

            composable(Routes.COMPLAINTS) { ComplaintsScreen(canManage = canManage) }
            composable(Routes.ACCOMMODATION_DETAILS) { AccommodationDetailsScreen(onBack = back) }
            composable(Routes.APPROVALS) { ApprovalsScreen(onBack = back) }
            composable(Routes.NOTICES) { NoticesScreen(canManage = canManage, onBack = back) }
            composable(Routes.ENQUIRIES) { EnquiriesScreen(onBack = back) }
            composable(Routes.MY_LISTINGS) { MyListingsScreen(onBack = back) }
            composable(Routes.INVITATIONS) {
                InvitationListScreen(
                    onBack = back,
                    onOpenInvitation = { push(invitationDetailRoute(it)) },
                )
            }
            composable(Routes.INVITATION_MANAGEMENT) {
                InvitationManagementScreen(
                    onBack = back,
                    onCreate = { push(Routes.CREATE_INVITATION) },
                    onOpenInvitation = { push(invitationReviewRoute(it)) },
                )
            }
            composable(Routes.CREATE_INVITATION) {
                CreateInvitationScreen(
                    onBack = back,
                    onCreated = { id ->
                        navController.navigate(invitationReviewRoute(id)) {
                            popUpTo(Routes.CREATE_INVITATION) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = "${Routes.INVITATION_DETAIL}/{invitationId}",
                arguments = listOf(navArgument("invitationId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "hamsavilas://onboarding/{invitationId}" },
                ),
            ) {
                InvitationDetailScreen(
                    onBack = back,
                    onOpenDigiLocker = { push(digilockerRoute(it)) },
                )
            }
            composable(
                route = "${Routes.INVITATION_STATUS}/{invitationId}",
                arguments = listOf(navArgument("invitationId") { type = NavType.StringType }),
            ) {
                TenantOnboardingStatusScreen(
                    onBack = back,
                    onOpenDigiLocker = { push(digilockerRoute(it)) },
                )
            }
            composable(
                route = "${Routes.DIGILOCKER}/{invitationId}",
                arguments = listOf(navArgument("invitationId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "hamsavilas://onboarding?invitation_id={invitationId}" },
                ),
            ) { DigiLockerVerificationScreen(onBack = back) }
            composable(
                route = "${Routes.INVITATION_REVIEW}/{invitationId}",
                arguments = listOf(navArgument("invitationId") { type = NavType.StringType }),
            ) {
                InvitationReviewScreen(
                    canApprove = session.role.isStaff,
                    onBack = back,
                )
            }
            composable(
                route = "${Routes.ROOM_AVAILABILITY}/{sharingCount}?title={title}&scope={scope}",
                arguments = listOf(
                    navArgument("sharingCount") { type = NavType.IntType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "Rooms" },
                    navArgument("scope") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                RoomAvailabilityScreen(
                    title = entry.arguments?.getString("title") ?: "Rooms",
                    sharingCount = entry.arguments?.getInt("sharingCount") ?: 0,
                    scopeLabel = entry.arguments?.getString("scope").orEmpty(),
                    onBack = back,
                )
            }
            composable(
                route = "${Routes.ADD_FLOOR}/{branchId}?name={name}&floors={floors}",
                arguments = listOf(
                    navArgument("branchId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("floors") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                AddFloorScreen(
                    branchId = entry.arguments?.getString("branchId").orEmpty(),
                    branchName = entry.arguments?.getString("name").orEmpty(),
                    existingFloors = entry.arguments?.getInt("floors") ?: 0,
                    onBack = back,
                )
            }
            composable(Routes.AUDIT) { AuditScreen(onBack = back) }
            composable(Routes.TEMPLATE) { TemplateScreen(onBack = back) }
            composable(Routes.HELP) {
                HelpScreen(audience = helpAudienceFor(session.role), onBack = back)
            }
            composable(Routes.CONTACT_US) { ContactUsScreen(onBack = back) }
            composable(Routes.PRIVACY_POLICY) { PrivacyPolicyScreen(onBack = back) }
        }
    }
}
