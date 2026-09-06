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

enum class ApprovalFilter(val label: String) {
    TO_BE_APPROVED("To be approved"),
    APPROVED("Approved"),
    REJECTED("Rejected");
}

enum class ApprovalSort(val label: String) {
    LATEST_FIRST("Latest first"),
    OLDEST_FIRST("Oldest first");
}

data class ApprovalsUiState(
    val loading: Boolean = true,
    val allItems: List<ChangeRequestDto> = emptyList(),
    val filter: ApprovalFilter = ApprovalFilter.TO_BE_APPROVED,
    val sort: ApprovalSort = ApprovalSort.LATEST_FIRST,
    val error: String? = null,
    val actioningId: String? = null,
) {
    val pendingCount: Int
        get() = allItems.count { it.status.equals("pending", ignoreCase = true) }

    val approvedCount: Int
        get() = allItems.count { it.status.equals("approved", ignoreCase = true) }

    val rejectedCount: Int
        get() = allItems.count { it.status.equals("rejected", ignoreCase = true) }

    val filteredItems: List<ChangeRequestDto>
        get() {
            val statusTarget = when (filter) {
                ApprovalFilter.TO_BE_APPROVED -> "pending"
                ApprovalFilter.APPROVED -> "approved"
                ApprovalFilter.REJECTED -> "rejected"
            }
            val list = allItems.filter { it.status.equals(statusTarget, ignoreCase = true) }
            return when (sort) {
                ApprovalSort.LATEST_FIRST -> list.sortedByDescending { it.createdAt ?: "" }
                ApprovalSort.OLDEST_FIRST -> list.sortedBy { it.createdAt ?: "" }
            }
        }

    val items: List<ChangeRequestDto>
        get() = filteredItems
}

@HiltViewModel
class ApprovalsViewModel @Inject constructor(
    private val repo: ApprovalRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ApprovalsUiState())
    val state: StateFlow<ApprovalsUiState> = _state.asStateFlow()

    init { refresh() }

    fun setFilter(filter: ApprovalFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun setSort(sort: ApprovalSort) {
        _state.update { it.copy(sort = sort) }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.listChanges("all")) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, allItems = r.data) }
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
                is ApiResult.Success -> {
                    _state.update { it.copy(actioningId = null) }
                    snackbar.show(successMessage)
                    refresh()
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(actioningId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }
}
