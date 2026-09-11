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
 * Why the person is here.
 */
enum class AuthIntent { SignUp, LogIn }

object AuthValidators {
    // Name contains only alphabets (with optional spaces between words) with a minimum of 3 characters
    private val NAME_REGEX = Regex("^[a-zA-Z]+(?:\\s[a-zA-Z]+)*$")
    // Valid 10-digit Indian mobile number starting with 6-9
    private val PHONE_REGEX = Regex("^[6-9]\\d{9}$")
    // Valid general email format (not restricted to Gmail)
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length >= 3 && trimmed.matches(NAME_REGEX)
    }

    fun isValidPhone(phone: String): Boolean {
        val digits = phone.trim().filter { it.isDigit() }
        return digits.matches(PHONE_REGEX)
    }

    fun isValidEmail(email: String): Boolean {
        return email.trim().matches(EMAIL_REGEX)
    }

    fun nameError(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Full name is required."
        if (trimmed.length < 3) return "Name must contain only alphabets with a minimum of 3 characters."
        if (!trimmed.all { it.isLetter() || it.isWhitespace() }) return "Name must contain only alphabets."
        if (!trimmed.matches(NAME_REGEX)) return "Name must contain only alphabets."
        return null
    }

    fun phoneError(phone: String): String? {
        val trimmed = phone.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (trimmed.isEmpty()) return "Phone number is required."
        if (!digits.matches(PHONE_REGEX)) {
            return "Enter a valid 10-digit Indian mobile number starting with 6–9."
        }
        return null
    }

    fun emailError(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return "Email address is required."
        if (!isValidEmail(trimmed)) {
            return "Enter a valid email address (e.g. name@example.com)."
        }
        return null
    }
}

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
    val fullNameTouched: Boolean = false,
    val phoneTouched: Boolean = false,
    val emailTouched: Boolean = false,
    val attemptedSubmit: Boolean = false,
) {
    val nameError: String? get() = AuthValidators.nameError(fullName)
    val phoneError: String? get() = AuthValidators.phoneError(phone)
    val emailError: String? get() = AuthValidators.emailError(email)

    val showNameError: Boolean
        get() = intent == AuthIntent.SignUp && (attemptedSubmit || (fullNameTouched && fullName.isNotEmpty())) && nameError != null

    val showPhoneError: Boolean
        get() = intent == AuthIntent.SignUp && (attemptedSubmit || (phoneTouched && phone.isNotEmpty())) && phoneError != null

    val showEmailError: Boolean
        get() = (attemptedSubmit || (emailTouched && email.isNotEmpty())) && emailError != null

    val isFormValid: Boolean
        get() = if (intent == AuthIntent.SignUp) {
            nameError == null && phoneError == null && emailError == null
        } else {
            emailError == null
        }

    val canSubmit: Boolean
        get() {
            if (otpSent) return otpCode.length == 6
            return isFormValid
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

    fun onEmail(v: String) = _state.update {
        it.copy(email = v.take(254), emailTouched = true, error = null)
    }

    fun onFullName(v: String) = _state.update {
        it.copy(fullName = v.take(100), fullNameTouched = true, error = null)
    }

    fun onPhone(v: String) = _state.update {
        it.copy(phone = v.take(20), phoneTouched = true, error = null)
    }

    fun onOtpCode(v: String) = _state.update {
        it.copy(otpCode = v, error = null)
    }

    fun startSignUp() = _state.update {
        it.copy(
            step = AuthStep.Email,
            intent = AuthIntent.SignUp,
            fullNameTouched = false,
            phoneTouched = false,
            emailTouched = false,
            attemptedSubmit = false,
            error = null,
        )
    }

    fun startLogIn() = _state.update {
        it.copy(
            step = AuthStep.Email,
            intent = AuthIntent.LogIn,
            fullNameTouched = false,
            phoneTouched = false,
            emailTouched = false,
            attemptedSubmit = false,
            error = null,
        )
    }

    fun continueWithGoogle() = _state.update {
        it.copy(error = "Google sign-in isn't available yet. Use your email for now.")
    }

    /** Back out of the email panel to the welcome panel. */
    fun backToWelcome() {
        cooldownJob?.cancel()
        submitJob?.cancel()
        _state.update {
            it.copy(
                step = AuthStep.Welcome,
                otpSent = false,
                otpCode = "",
                loading = false,
                error = null,
                fullNameTouched = false,
                phoneTouched = false,
                emailTouched = false,
                attemptedSubmit = false,
            )
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
        if (s.loading) return
        if (s.otpSent) {
            if (s.otpCode.length == 6) verifyOtp()
            return
        }
        if (!s.isFormValid) {
            _state.update { it.copy(attemptedSubmit = true) }
            return
        }
        sendOtp()
    }

    private fun sendOtp() {
        val s = _state.value
        _state.update { it.copy(loading = true, error = null) }
        submitJob = viewModelScope.launch {
            when (val result = repo.requestOtp(
                email = s.email.trim(),
                fullName = s.fullName.trim().takeIf { it.isNotBlank() },
                phone = s.phone.trim().takeIf { it.isNotBlank() },
                isSignup = s.intent == AuthIntent.SignUp,
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
                email = s.email.trim(),
                code = s.otpCode.trim(),
                fullName = s.fullName.trim().takeIf { it.isNotBlank() },
                phone = s.phone.trim().takeIf { it.isNotBlank() },
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
