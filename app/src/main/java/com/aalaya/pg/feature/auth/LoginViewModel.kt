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
    val fullName: String = "",
    val phone: String = "",
    val otpCode: String = "",
    val otpSent: Boolean = false,
    val resendSecondsLeft: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val isNewDevice: Boolean = false,
    val isVerified: Boolean = true,
) {
    val canSubmit: Boolean
        get() {
            if (otpSent) return otpCode.length == 6
            val emailValid = email.contains('@') && email.substringAfter('@').contains('.')
            if (intent == AuthIntent.SignUp) {
                val phoneDigits = phone.filter { it.isDigit() }
                return emailValid && (phone.isBlank() || phoneDigits.length >= 10)
            }
            return emailValid
        }
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    private var submitJob: Job? = null

    fun onEmail(v: String) = _state.update { it.copy(email = v.take(254), error = null) }
    fun onFullName(v: String) = _state.update { it.copy(fullName = v.take(100), error = null) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v.take(20), error = null) }
    fun onOtpCode(v: String) = _state.update { it.copy(otpCode = v, error = null) }

    fun startSignUp() = _state.update {
        it.copy(step = AuthStep.Email, intent = AuthIntent.SignUp, error = null)
    }

    fun startLogIn() = _state.update {
        it.copy(step = AuthStep.Email, intent = AuthIntent.LogIn, error = null)
    }

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
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        submitJob = viewModelScope.launch {
            when (val result = repo.requestOtp(
                email = s.email,
                fullName = s.fullName.takeIf { it.isNotBlank() },
                phone = s.phone.takeIf { it.isNotBlank() },
            )) {
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
            when (val result = repo.verifyOtp(
                email = s.email,
                code = s.otpCode,
                fullName = s.fullName.takeIf { it.isNotBlank() },
                phone = s.phone.takeIf { it.isNotBlank() },
            )) {
                is ApiResult.Success -> {
                    val outcome = result.data
                    cooldownJob?.cancel()
                    _state.update {
                        LoginUiState(
                            email = it.email,
                            fullName = it.fullName,
                            phone = it.phone,
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
