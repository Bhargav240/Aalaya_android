package com.aalaya.pg.feature.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.data.BranchContext
import com.aalaya.pg.core.network.dto.BranchDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BranchesUiState(
    val loading: Boolean = true,
    val branches: List<BranchDto> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
class BranchesViewModel @Inject constructor(
    private val repo: BranchRepository,
    private val branchContext: BranchContext,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(BranchesUiState())
    val state: StateFlow<BranchesUiState> = _state.asStateFlow()

    init {
        // The Create Branch page has its own navigation-scoped ViewModel. When
        // it updates the shared branch context, this existing list screen is
        // still alive underneath it. Observe that context so returning here
        // shows the new branch immediately, without requiring pull-to-refresh.
        viewModelScope.launch {
            branchContext.branches.collect { branches ->
                if (branchContext.loaded.value) {
                    _state.update { it.copy(loading = false, branches = branches, error = null) }
                }
            }
        }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.branches()) {
                // The API applies the manager's branch scope. Rendering the
                // response directly avoids fetching and filtering hidden
                // organization branches on the device.
                is ApiResult.Success -> {
                    branchContext.setBranches(r.data)
                    _state.update { it.copy(loading = false, branches = r.data) }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    /** Returns true if the branch was created (so the screen can close the dialog). */
    fun addBranch(name: String, onDone: () -> Unit) {
        if (name.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.createBranch(name)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    /** Creates the branch and all its initial floors, rooms and beds. */
    fun addBranchWithLayout(
        name: String,
        floorCount: Int,
        roomsPerFloor: Int,
        bedsPerRoom: Int,
        onDone: () -> Unit,
    ) {
        if (name.isBlank() || floorCount !in 1..100 || roomsPerFloor !in 1..100 ||
            bedsPerRoom !in 1..26 || _state.value.saving
        ) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.createBranchWithLayout(name, floorCount, roomsPerFloor, bedsPerRoom)) {
                is ApiResult.Success -> {
                    // Update the singleton context immediately. All mounted branch
                    // dropdowns observe it, so the new branch appears without an app restart.
                    val list = when (val branches = repo.branches()) {
                        is ApiResult.Success -> branches.data
                        is ApiResult.Failure -> _state.value.branches + BranchDto(r.data.id, r.data.name)
                    }
                    branchContext.setBranches(list)
                    branchContext.select(r.data.id, r.data.name)
                    _state.update { it.copy(saving = false, branches = list) }
                    snackbar.show("Branch and layout created.")
                    onDone()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    /** Maker-checker rename: applied (owner) or queued for approval (manager). */
    fun renameBranch(id: String, name: String, address: String, onDone: () -> Unit) {
        if ((name.isBlank() && address.isBlank()) || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.updateBranch(id, name, address)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    snackbar.show(
                        if (r.data.status == "pending") "Change sent for owner approval."
                        else "Branch updated.",
                    )
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    /** Maker-checker delete (soft). */
    fun deleteBranch(id: String) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.deleteBranch(id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    snackbar.show(
                        if (r.data.status == "pending") "Deletion sent for owner approval."
                        else "Branch deleted.",
                    )
                    refresh()
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(saving = false) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }
}
