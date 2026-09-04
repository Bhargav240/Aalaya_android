package com.aalaya.pg.feature.rooms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.ResidentDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.feature.branches.BranchRepository
import com.aalaya.pg.feature.residents.ResidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomsUiState(
    val loading: Boolean = true,
    val rooms: List<RoomDto> = emptyList(),
    val bedsByRoom: Map<String, List<BedDto>> = emptyMap(),
    val expandedRoomId: String? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    /** Active residents of this branch, for the bed-assignment picker. */
    val residents: List<ResidentDto> = emptyList(),
)

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val repo: BranchRepository,
    private val residentRepo: ResidentRepository,
    private val snackbar: SnackbarManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val branchId: String = checkNotNull(savedStateHandle["branchId"])

    private val _state = MutableStateFlow(RoomsUiState())
    val state: StateFlow<RoomsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.rooms(branchId)) {
                is ApiResult.Success -> {
                    // Drop the cached beds too, or a bed reassigned elsewhere
                    // keeps showing its old occupant here until the ViewModel
                    // is recreated. Reload immediately for whichever room is
                    // expanded; others reload lazily next time they're opened.
                    _state.update { it.copy(loading = false, rooms = r.data, bedsByRoom = emptyMap()) }
                    _state.value.expandedRoomId?.let { loadBeds(it) }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun toggleRoom(roomId: String) {
        val expanding = _state.value.expandedRoomId != roomId
        _state.update { it.copy(expandedRoomId = if (expanding) roomId else null) }
        if (expanding && _state.value.bedsByRoom[roomId] == null) loadBeds(roomId)
    }

    private fun loadBeds(roomId: String) {
        viewModelScope.launch {
            when (val r = repo.beds(roomId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(bedsByRoom = it.bedsByRoom + (roomId to r.data))
                }
                is ApiResult.Failure -> snackbar.show(r.error.message)
            }
        }
    }

    fun addRoom(number: String, coolingType: String, onDone: () -> Unit) {
        if (number.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.createRoom(branchId, number, coolingType)) {
                is ApiResult.Success -> { _state.update { it.copy(saving = false) }; onDone(); refresh() }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    fun addBed(roomId: String, label: String, onDone: () -> Unit) {
        if (label.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.createBed(roomId, label)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false, bedsByRoom = it.bedsByRoom - roomId) }
                    onDone()
                    loadBeds(roomId)
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    /** Maker-checker room rename. */
    fun renameRoom(roomId: String, number: String, coolingType: String, onDone: () -> Unit) {
        if (number.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.updateRoom(roomId, number, coolingType)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    snackbar.show(
                        if (r.data.status == "pending") "Change sent for owner approval."
                        else "Room updated.",
                    )
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    /** Maker-checker room delete. */
    fun deleteRoom(roomId: String) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val r = repo.deleteRoom(roomId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    snackbar.show(
                        if (r.data.status == "pending") "Deletion sent for owner approval."
                        else "Room deleted.",
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

    /** Lazily loads the branch's residents for the bed-assignment picker. */
    fun ensureResidents() {
        if (_state.value.residents.isNotEmpty()) return
        viewModelScope.launch {
            when (val r = residentRepo.residents(branchId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(residents = r.data.filter { res -> res.status != "moved_out" })
                }
                is ApiResult.Failure -> snackbar.show(r.error.message)
            }
        }
    }

    /**
     * Loads every bed in one branch-scoped request before an assignment. A
     * resident's bed assignment is a single column server-side, so the picker
     * needs the complete placement map to warn before it moves someone. This
     * replaces the previous one-network-request-per-room N+1 pattern.
     */
    fun ensureAllBedsLoaded() {
        val rooms = _state.value.rooms
        if (rooms.isEmpty() || rooms.all { _state.value.bedsByRoom.containsKey(it.id) }) return
        viewModelScope.launch {
            when (val result = repo.branchBeds(branchId)) {
                is ApiResult.Success -> {
                    // Include empty rooms too, so a zero-bed room is cached
                    // and does not trigger another network call on each tap.
                    val grouped = rooms.associate { it.id to emptyList<BedDto>() } +
                        result.data.groupBy { it.roomId }
                    _state.update { it.copy(bedsByRoom = it.bedsByRoom + grouped) }
                }
                // Best-effort: preserve the established behavior where an
                // unavailable placement map never blocks the picker.
                is ApiResult.Failure -> Unit
            }
        }
    }

    /** Assigns (or, with null resident, clears) a bed. Refreshes the room's beds. */
    fun assignBed(roomId: String, bedId: String?, residentId: String, onDone: () -> Unit) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = residentRepo.assignBed(residentId, bedId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false, bedsByRoom = it.bedsByRoom - roomId) }
                    snackbar.show(if (bedId == null) "Bed unassigned." else "Bed assigned.")
                    onDone()
                    loadBeds(roomId)
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }
}
