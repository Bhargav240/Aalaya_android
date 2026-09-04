package com.aalaya.pg.core.data

import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.core.network.SessionInvalidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class UserSession(
    val userId: String,
    val orgId: String,
    val role: Role,
    val branchId: String? = null,
    val fullName: String? = null,
    // Identity verification (PRD flow 1). Staff default true; tenants must
    // verify once at first login before the main app unlocks.
    val verified: Boolean = true,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val session: UserSession) : AuthState
}

/**
 * Single source of truth for "who is signed in". Restores identity from disk on
 * cold start (so we don't need a network round-trip to know the role), and
 * implements [SessionInvalidator] so a failed token refresh drops us to sign-in.
 */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore,
    private val branchContext: BranchContext,
    private val pinStore: PinStore,
    private val snackbar: com.aalaya.pg.core.common.SnackbarManager,
    @com.aalaya.pg.di.AppScope private val appScope: CoroutineScope,
) : SessionInvalidator {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * Increments on every *new* sign-in (not on a verification update).
     *
     * The authenticated shell keys itself on this. Compose's saveable state
     * survives the sign-out → sign-in round trip because the shell reoccupies
     * the same composition slot, so without a changing key the NavController
     * restores the previous user's back stack — a resident could land on the
     * tab a manager left open. Bumping the epoch forces a fresh shell.
     */
    private val _sessionEpoch = MutableStateFlow(0)
    val sessionEpoch: StateFlow<Int> = _sessionEpoch.asStateFlow()

    /** Called once at startup to hydrate state from persisted tokens/identity. */
    suspend fun bootstrap() {
        try {
            bootstrapInternal()
        } catch (e: Exception) {
            // Corrupt DataStore or IO failure must not pin the app on the
            // Loading spinner — drop to sign-in instead.
            _state.value = AuthState.Unauthenticated
        }
    }

    private suspend fun bootstrapInternal() {
        tokenStore.primeAccessToken()
        val identity = tokenStore.identity()
        _state.value = if (identity != null && tokenStore.refreshToken() != null) {
            AuthState.Authenticated(identity)
        } else {
            AuthState.Unauthenticated
        }
    }

    suspend fun onAuthenticated(access: String, refresh: String, session: UserSession) {
        // Tokens were already persisted by the caller (it needed them in the
        // store before calling /me); only write identity + bump the session.
        // Re-saving tokens here would double the DataStore write per login (A15).
        if (tokenStore.peekAccessToken() != access) {
            tokenStore.save(access, refresh)
        }
        tokenStore.saveIdentity(session)
        _sessionEpoch.value += 1
        _state.value = AuthState.Authenticated(session)
    }

    suspend fun signOut() {
        tokenStore.clear()
        // BranchContext is a separate process-lifetime singleton, not covered
        // by sessionEpoch's NavController reset -- left uncleared, the next
        // person to sign in on this device (a different manager, or an owner)
        // could silently inherit the previous user's branch scope.
        branchContext.clear()
        // Same argument for the on-device PIN: leave it behind and the next
        // owner on this device is asked to enter (and checked against) the
        // previous owner's hash.
        pinStore.clear()
        _state.value = AuthState.Unauthenticated
    }

    /** Marks the signed-in user as identity-verified and persists it. */
    suspend fun onVerified() {
        val current = (_state.value as? AuthState.Authenticated)?.session ?: return
        val updated = current.copy(verified = true)
        tokenStore.saveIdentity(updated)
        _state.value = AuthState.Authenticated(updated)
    }

    /** From [SessionInvalidator]: token refresh failed on a background thread. */
    override fun invalidate() {
        appScope.launch {
            // Surface the drop — without this the user just sees a silent bounce
            // back to sign-in with no explanation.
            snackbar.show("Session expired — please sign in again")
            signOut()
        }
    }
}
