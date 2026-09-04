package com.aalaya.pg.feature.enquiries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.network.dto.EnquiryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnquiriesUiState(
    val loading: Boolean = true,
    val enquiries: List<EnquiryDto> = emptyList(),
    val error: String? = null,
    val updatingId: String? = null,
)

/** The manager's enquiry inbox — people who asked about a listing. */
@HiltViewModel
class EnquiriesViewModel @Inject constructor(
    private val repo: EnquiryRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(EnquiriesUiState())
    val state: StateFlow<EnquiriesUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.list()) {
                is ApiResult.Success ->
                    _state.update { it.copy(loading = false, enquiries = r.data) }
                is ApiResult.Failure ->
                    _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun setStatus(enquiry: EnquiryDto, status: String) {
        if (_state.value.updatingId != null) return
        _state.update { it.copy(updatingId = enquiry.id) }
        viewModelScope.launch {
            when (val r = repo.setStatus(enquiry.id, status)) {
                is ApiResult.Success -> {
                    _state.update { s ->
                        s.copy(
                            updatingId = null,
                            enquiries = s.enquiries.map { if (it.id == enquiry.id) it.copy(status = status) else it },
                        )
                    }
                    snackbar.show(if (status == "contacted") "Marked as contacted." else "Enquiry closed.")
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(updatingId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }
}
