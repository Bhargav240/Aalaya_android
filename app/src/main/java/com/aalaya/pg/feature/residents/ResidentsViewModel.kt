package com.aalaya.pg.feature.residents

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.core.network.dto.PublishListingRequest
import com.aalaya.pg.core.network.dto.ResidentDto
import com.aalaya.pg.feature.branches.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewResidentForm(
    val fullName: String = "",
    val phone: String = "",
    val plan: PlanKind = PlanKind.Monthly,
    val rentRupees: String = "",
    val depositRupees: String = "",
)

data class ListingForm(
    val title: String = "",
    val area: String = "",
    val city: String = "",
    val description: String = "",
    val amenities: String = "",
)

data class ResidentsUiState(
    val loading: Boolean = true,
    val residents: List<ResidentDto> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val publishing: Boolean = false,
    val publishError: String? = null,
)

@HiltViewModel
class ResidentsViewModel @Inject constructor(
    private val repo: ResidentRepository,
    private val branchRepo: BranchRepository,
    private val snackbar: SnackbarManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val branchId: String = checkNotNull(savedStateHandle["branchId"])

    private val _state = MutableStateFlow(ResidentsUiState())
    val state: StateFlow<ResidentsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.residents(branchId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, residents = r.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun addResident(form: NewResidentForm, onDone: () -> Unit) {
        if (form.fullName.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val rentPaise = rupeesToPaise(form.rentRupees)
            val depositPaise = rupeesToPaise(form.depositRupees)
            when (val r = repo.create(branchId, form.fullName, form.phone, form.plan, rentPaise, depositPaise)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    private fun rupeesToPaise(rupees: String): Long =
        com.aalaya.pg.core.util.Money.parseRupeesToPaise(rupees) ?: 0L

    fun publishListing(form: ListingForm, onDone: () -> Unit) {
        if (form.title.isBlank() || _state.value.publishing) return
        _state.update { it.copy(publishing = true, publishError = null) }
        viewModelScope.launch {
            val req = PublishListingRequest(
                title = form.title.trim(),
                area = form.area.trim().ifBlank { null },
                city = form.city.trim().ifBlank { null },
                description = form.description.trim().ifBlank { null },
                amenities = form.amenities.trim().ifBlank { null },
            )
            when (val r = branchRepo.publishListing(branchId, req)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(publishing = false) }
                    snackbar.show("Published to Explore.")
                    onDone()
                }
                is ApiResult.Failure -> _state.update { it.copy(publishing = false, publishError = r.error.message) }
            }
        }
    }
}
