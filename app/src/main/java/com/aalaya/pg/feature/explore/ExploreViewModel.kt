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

data class ExploreUiState(
    val query: String = "",
    val loading: Boolean = true,
    val listings: List<Listing> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    internal val repo: DiscoveryRepository,
    internal val enquiries: com.aalaya.pg.feature.enquiries.EnquiryRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    init { refresh() }

    fun onQuery(v: String) = _state.update { it.copy(query = v) }

    fun submitSearch() {
        val q = _state.value.query.trim().ifBlank { null }
        load { if (q == null) repo.list() else repo.search(q, null) }
    }

    fun refresh() = load { repo.list() }

    fun nearby(lat: Double, lng: Double) = load { repo.nearby(lat, lng) }

    fun onLocationUnavailable() {
        snackbar.show("Couldn't get your location. Showing all PGs instead.")
        // The message promises the unfiltered list -- actually fetch it,
        // rather than leaving whatever (possibly nearby-filtered) results
        // were already on screen.
        refresh()
    }

    private fun load(block: suspend () -> ApiResult<List<Listing>>) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = block()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, listings = r.data, error = null) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }
}
