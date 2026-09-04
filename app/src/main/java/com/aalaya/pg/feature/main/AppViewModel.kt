package com.aalaya.pg.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.data.AuthState
import com.aalaya.pg.core.data.PinStore
import com.aalaya.pg.core.data.SessionManager
import com.aalaya.pg.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives the top-level session gate: bootstraps identity, exposes auth state. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val session: SessionManager,
    private val authRepository: AuthRepository,
    internal val pinStore: PinStore,
    snackbarManager: SnackbarManager,
) : ViewModel() {

    val authState: StateFlow<AuthState> = session.state

    /** Changes on each new sign-in; the shell keys on it to reset navigation. */
    val sessionEpoch: StateFlow<Int> = session.sessionEpoch
    val snackbarMessages: SharedFlow<String> = snackbarManager.messages

    /** Owners must re-enter their app PIN on each cold start (SIM-swap defence).
     *  Flips true for the lifetime of this process once the PIN validates. */
    private val _pinUnlocked = MutableStateFlow(false)
    val pinUnlocked: StateFlow<Boolean> = _pinUnlocked.asStateFlow()

    init {
        viewModelScope.launch { session.bootstrap() }
        // Session expiry can sign out through SessionManager directly (from
        // OkHttp's authenticator), without passing through this ViewModel's
        // signOut(). Do not carry an owner PIN unlock into the next login in
        // the same process.
        viewModelScope.launch {
            session.state.collect { state ->
                if (state is AuthState.Unauthenticated) _pinUnlocked.value = false
            }
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            pinStore.setPin(pin)
            _pinUnlocked.value = true
        }
    }

    fun unlockPin() {
        _pinUnlocked.value = true
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
