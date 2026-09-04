package com.aalaya.pg.feature.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.domain.model.ComplaintState
import com.aalaya.pg.core.network.dto.ComplaintDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComplaintsUiState(
    val loading: Boolean = true,
    val items: List<ComplaintDto> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val transitioningId: String? = null,
)

@HiltViewModel
class ComplaintsViewModel @Inject constructor(
    private val repo: ComplaintRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ComplaintsUiState(
            loading = repo.cachedItems() == null,
            items = repo.cachedItems().orEmpty(),
        ),
    )
    val state: StateFlow<ComplaintsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.warmCache()
            repo.cachedItems()?.let { cached ->
                _state.update { it.copy(loading = false, items = cached) }
            }
            refresh()
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.list()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun raise(title: String, description: String, businessLevel: Boolean, onDone: () -> Unit) {
        if (title.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.raise(title, description, businessLevel, null)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    onDone(); refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    fun transition(id: String, to: ComplaintState) {
        if (_state.value.transitioningId != null) return
        _state.update { it.copy(transitioningId = id) }
        viewModelScope.launch {
            when (val r = repo.transition(id, to)) {
                is ApiResult.Success -> {
                    snackbar.show("Moved to ${to.label}")
                    _state.update { it.copy(transitioningId = null) }
                    refresh()
                }
                is ApiResult.Failure -> {
                    snackbar.show(r.error.message)
                    _state.update { it.copy(transitioningId = null) }
                }
            }
        }
    }
}
