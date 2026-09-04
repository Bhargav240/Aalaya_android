package com.aalaya.pg.feature.explore

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

data class MyListingsUiState(
    val loading: Boolean = true,
    val listings: List<Listing> = emptyList(),
    val error: String? = null,
    val submittingId: String? = null,
)

/** Owner/manager view of their own listings and their publish status. */
@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val repo: DiscoveryRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MyListingsUiState())
    val state: StateFlow<MyListingsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.myListings()) {
                is ApiResult.Success ->
                    _state.update { it.copy(loading = false, listings = r.data) }
                is ApiResult.Failure ->
                    _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun submit(listing: Listing) {
        if (_state.value.submittingId != null) return
        _state.update { it.copy(submittingId = listing.branchId) }
        viewModelScope.launch {
            when (val r = repo.submitListing(listing.branchId)) {
                is ApiResult.Success -> {
                    snackbar.show("Sent for approval.")
                    _state.update { it.copy(submittingId = null) }
                    refresh()
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(submittingId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }
}
