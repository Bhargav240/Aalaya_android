package com.aalaya.pg.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RESEND_COOLDOWN_SECS = 30

/** Which panel of the sign-in flow is on screen. */
enum class AuthStep { Welcome, Email, Otp }

/**
 * Why the person is here. The backend makes no distinction — an unknown email
 * that asks for a code is auto-provisioned as a prospect, a known one just
 * signs in — but the wording has to match what the person believes they are
 * doing, so "Sign up" must not greet them with "Welcome back".
 */
enum class AuthIntent { SignUp, LogIn }

data class LoginUiState(
    val step: AuthStep = AuthStep.Welcome,
    val intent: AuthIntent = AuthIntent.LogIn,
    val email: String = "",
    val otpCode: String = "",
    val otpSent: Boolean = false,
    val resendSecondsLeft: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val isNewDevice: Boolean = false,
    val isVerified: Boolean = true,
) {
    val canSubmit: Boolean
        get() = if (!otpSent) email.contains('@') && email.substringAfter('@').contains('.') else otpCode.length == 6
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    // The in-flight sendOtp()/verifyOtp() call, if any. Without tracking this,
    // tapping "Change email" while a request is still in flight leaves it
    // running: it resolves later and snaps the UI back to the OTP-entry step
    // (with a fresh cooldown) right after the user had already backed out of it.
    private var submitJob: Job? = null

    // Cap length at the RFC 5321 maximum so a pasted block of text can't sail
    // past canSubmit's "contains @ and a dot" check unbounded.
    fun onEmail(v: String) = _state.update { it.copy(email = v.take(254), error = null) }
    fun onOtpCode(v: String) = _state.update { it.copy(otpCode = v, error = null) }

    fun startSignUp() = _state.update {
        it.copy(step = AuthStep.Email, intent = AuthIntent.SignUp, error = null)
    }

    fun startLogIn() = _state.update {
        it.copy(step = AuthStep.Email, intent = AuthIntent.LogIn, error = null)
    }

    /**
     * Google sign-in. The backend route exists (`POST /auth/google`) but stays
     * disabled until GOOGLE_CLIENT_ID is configured, so the button that calls
     * this is hidden behind BuildConfig.GOOGLE_SIGN_IN_ENABLED. When the OAuth
     * client is issued, this is where the ID token from the Credential Manager
     * flow gets exchanged; until then it must never be reachable.
     */
    fun continueWithGoogle() = _state.update {
        it.copy(error = "Google sign-in isn't available yet. Use your email for now.")
    }

    /** Back out of the email panel to the welcome panel. */
    fun backToWelcome() {
        cooldownJob?.cancel()
        submitJob?.cancel()
        _state.update {
            it.copy(step = AuthStep.Welcome, otpSent = false, otpCode = "", loading = false, error = null)
        }
    }

    fun changeEmail() {
        cooldownJob?.cancel()
        submitJob?.cancel()
        _state.update {
            it.copy(
                step = AuthStep.Email,
                otpSent = false,
                otpCode = "",
                resendSecondsLeft = 0,
                loading = false,
                error = null,
            )
        }
    }

    fun resend() {
        if (_state.value.resendSecondsLeft > 0) return
        sendOtp()
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit || s.loading) return
        if (!s.otpSent) sendOtp() else verifyOtp()
    }

    private fun sendOtp() {
        val email = _state.value.email
        _state.update { it.copy(loading = true, error = null) }
        submitJob = viewModelScope.launch {
            when (val result = repo.requestOtp(email)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, otpSent = true, step = AuthStep.Otp) }
                    startCooldown()
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
            }
        }
    }

    private fun verifyOtp() {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        submitJob = viewModelScope.launch {
            when (val result = repo.verifyOtp(s.email, s.otpCode)) {
                // SessionManager flips to Authenticated and the root graph swaps to
                // the app — but this ViewModel instance can resurface after a later
                // sign-out, so reset it fully rather than leaving loading=true and
                // the submitted state stuck forever. We still capture the new-device
                // / verified flags before resetting so the banner/gate can read them.
                is ApiResult.Success -> {
                    val outcome = result.data
                    cooldownJob?.cancel()
                    _state.update {
                        // Keep the email so a post-sign-out login doesn't make the
                        // same person re-type it (A13).
                        LoginUiState(
                            email = it.email,
                            isNewDevice = outcome.isNewDevice,
                            isVerified = outcome.isVerified,
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _state.update { it.copy(resendSecondsLeft = RESEND_COOLDOWN_SECS) }
            while (_state.value.resendSecondsLeft > 0) {
                delay(1_000)
                _state.update { it.copy(resendSecondsLeft = it.resendSecondsLeft - 1) }
            }
        }
    }
}
