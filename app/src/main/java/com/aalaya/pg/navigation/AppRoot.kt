package com.aalaya.pg.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.feature.auth.PinScreen
import com.aalaya.pg.core.data.AuthState
import com.aalaya.pg.feature.auth.LoginScreen
import com.aalaya.pg.feature.main.AppViewModel
import com.aalaya.pg.feature.main.MainScaffold

/**
 * Top-level gate. Watches the session and swaps the entire UI between the
 * sign-in screen and the authenticated app. The app-wide Snackbar host lives in
 * [MainScaffold] so it floats above the bottom navigation.
 */
@Composable
fun AppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val sessionEpoch by viewModel.sessionEpoch.collectAsStateWithLifecycle()

    when (val s = authState) {
        is AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is AuthState.Unauthenticated -> Box(Modifier.fillMaxSize().systemBarsPadding()) {
            LoginScreen()
        }
        is AuthState.Authenticated -> {
            // Identity verification belongs to a specific tenant invitation,
            // not to the user's global account. Prospect sessions therefore
            // remain usable for Explore while onboarding is pending.
            key(sessionEpoch, s.session.userId) {
                StaffPinGate(session = s.session, viewModel = viewModel)
            }
        }
    }
}

/**
 * The second factor for staff (Revision 3 §12): a 4-digit app PIN set once,
 * entered on every cold start before the main app opens. It sits behind the
 * emailed OTP, so a stolen phone alone opens nothing. Residents and prospects
 * are not gated, so this composable hands straight through for them.
 */
@Composable
private fun StaffPinGate(
    session: com.aalaya.pg.core.data.UserSession,
    viewModel: AppViewModel,
) {
    val pinStore = viewModel.pinStore
    if (!pinStore.requiresPin(session.role)) {
        MainScaffold(
            session = session,
            snackbarMessages = viewModel.snackbarMessages,
            onSignOut = viewModel::signOut,
        )
        return
    }

    val unlocked by viewModel.pinUnlocked.collectAsStateWithLifecycle()
    var hasPin by remember { mutableStateOf<Boolean?>(null) }

    androidx.compose.runtime.LaunchedEffect(session.userId) {
        hasPin = pinStore.hasPinNow()
    }

    when {
        unlocked -> MainScaffold(
            session = session,
            snackbarMessages = viewModel.snackbarMessages,
            pinStore = pinStore,
            onSignOut = viewModel::signOut,
        )
        hasPin == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        hasPin == false -> PinScreen(
            pinStore = pinStore,
            title = "Set your app PIN",
            onUnlocked = viewModel::unlockPin,
            onSubmitPin = viewModel::setPin,
        )
        else -> PinScreen(
            pinStore = pinStore,
            title = "Enter your app PIN",
            onUnlocked = viewModel::unlockPin,
        )
    }
}
