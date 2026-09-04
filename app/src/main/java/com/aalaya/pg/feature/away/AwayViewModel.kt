package com.aalaya.pg.feature.away

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.common.onFailure
import com.aalaya.pg.core.common.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AwayUiState(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val periods: List<AwayPeriod> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    /** Id of the period being corrected; null means the form reports a new one. */
    val editingId: String? = null,
) {
    /** Both dates chosen, and the return date isn't before departure. */
    val canSubmit: Boolean get() = from != null && to != null && !to.isBefore(from)

    val isEditing: Boolean get() = editingId != null
}

@HiltViewModel
class AwayViewModel @Inject constructor(
    private val repository: AwayRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AwayUiState())
    val state: StateFlow<AwayUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repository.list()
                .onSuccess { periods -> _state.update { it.copy(periods = periods, loading = false, error = null) } }
                .onFailure { f -> _state.update { it.copy(loading = false, error = f.message) } }
        }
    }

    fun setFrom(date: LocalDate) {
        _state.update {
            // Keep the range coherent: pushing departure past the return date
            // clears the return rather than leaving an impossible span.
            val to = it.to?.takeIf { t -> !t.isBefore(date) }
            it.copy(from = date, to = to, error = null)
        }
    }

    fun setTo(date: LocalDate) {
        val from = _state.value.from
        if (from != null && date.isBefore(from)) {
            _state.update { it.copy(error = "You can't come back before you leave.") }
            return
        }
        _state.update { it.copy(to = date, error = null) }
    }

    /** Loads an existing period into the form so its dates can be corrected. */
    fun startEdit(period: AwayPeriod) {
        _state.update {
            it.copy(editingId = period.id, from = period.from, to = period.to, error = null)
        }
    }

    /** Leaves edit mode without saving, clearing the form back to empty. */
    fun cancelEdit() {
        _state.update { it.copy(editingId = null, from = null, to = null, error = null) }
    }

    fun submit(note: String?) {
        val s = _state.value
        if (!s.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            // Same form, two destinations: correcting keeps the original row
            // (and its audit trail) instead of cancelling and re-reporting.
            val editingId = s.editingId
            val result = if (editingId != null) {
                repository.update(editingId, s.from!!, s.to!!, note)
            } else {
                repository.report(s.from!!, s.to!!, note)
            }
            result
                .onSuccess {
                    _state.update {
                        it.copy(from = null, to = null, saving = false, error = null, editingId = null)
                    }
                    snackbar.show(
                        if (editingId != null) {
                            "Away days updated — the PG has the new dates"
                        } else {
                            "Away days reported — the PG knows your bed will be empty"
                        },
                    )
                    refresh()
                }
                .onFailure { f ->
                    _state.update { it.copy(saving = false, error = f.message) }
                }
        }
    }

    fun remove(period: AwayPeriod) {
        viewModelScope.launch {
            repository.cancel(period.id)
                .onSuccess {
                    // Cancelling the row currently loaded in the form would
                    // otherwise leave the form pointed at a dead id, so the
                    // next save came back 404.
                    _state.update {
                        if (it.editingId == period.id) {
                            it.copy(editingId = null, from = null, to = null, error = null)
                        } else {
                            it
                        }
                    }
                    snackbar.show("Away days cancelled")
                    refresh()
                }
                .onFailure { f -> snackbar.show(f.message) }
        }
    }
}
