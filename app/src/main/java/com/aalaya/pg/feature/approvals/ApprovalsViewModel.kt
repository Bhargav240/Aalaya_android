package com.aalaya.pg.feature.approvals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.network.dto.ChangeRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApprovalsUiState(
    val loading: Boolean = true,
    val items: List<ChangeRequestDto> = emptyList(),
    val error: String? = null,
    val actioningId: String? = null,
)

@HiltViewModel
class ApprovalsViewModel @Inject constructor(
    private val repo: ApprovalRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ApprovalsUiState())
    val state: StateFlow<ApprovalsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.pending()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun approve(id: String) = act(id, successMessage = "Change applied") { repo.approve(id) }
    fun reject(id: String) = act(id, successMessage = "Change rejected") { repo.reject(id) }

    private fun act(id: String, successMessage: String, block: suspend () -> ApiResult<Unit>) {
        if (_state.value.actioningId != null) return
        _state.update { it.copy(actioningId = id) }
        viewModelScope.launch {
            when (val r = block()) {
                is ApiResult.Success -> { _state.update { it.copy(actioningId = null) }; snackbar.show(successMessage); refresh() }
                is ApiResult.Failure -> {
                    _state.update { it.copy(actioningId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }
}
