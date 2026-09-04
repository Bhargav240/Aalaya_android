package com.aalaya.pg.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifyUiState(
    val providerRef: String = "",
    val phone: String = "",
    val emergencyContact: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = providerRef.isNotBlank() && !submitting
}

@HiltViewModel
class VerifyIdentityViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(VerifyUiState())
    val state: StateFlow<VerifyUiState> = _state.asStateFlow()

    fun onProviderRef(v: String) = _state.update { it.copy(providerRef = v, error = null) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun onEmergencyContact(v: String) = _state.update { it.copy(emergencyContact = v, error = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.verifyIdentity(s.providerRef, s.phone, s.emergencyContact)) {
                is ApiResult.Success -> {
                    if (r.data.status == "verified") {
                        // SessionManager flips verified=true; AppRoot unlocks the app.
                        snackbar.show("Identity verified. Welcome!")
                    } else {
                        _state.update {
                            it.copy(
                                submitting = false,
                                error = "Verification failed" +
                                    (r.data.reason?.let { reason -> ": $reason" } ?: "") + ".",
                            )
                        }
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = r.error.message)
                }
            }
        }
    }
}
