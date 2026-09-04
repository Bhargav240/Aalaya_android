package com.aalaya.pg.feature.floors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.ChangeResponse
import com.aalaya.pg.core.network.dto.FloorDto
import com.aalaya.pg.core.network.dto.MutationResponse
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.feature.branches.BranchRepository
import com.aalaya.pg.core.common.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FloorsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val floors: List<FloorDto> = emptyList(),
    val rooms: List<RoomDto> = emptyList(),
    val saving: Boolean = false,
    val saveError: String? = null,
    val bedsByRoom: Map<String, List<BedDto>> = emptyMap(),
    val expandedRoomId: String? = null,
) {
    /** Rooms explicitly filed on a floor. */
    fun roomsOn(floorId: String): List<RoomDto> = rooms.filter { it.floorId == floorId }

    /**
     * Rooms that predate floors. They still appear on the screen — grouped
     * under their derived floor elsewhere in the app — so an owner can see what
     * is left to file rather than wondering where a room went.
     */
    val unassignedRooms: List<RoomDto> get() = rooms.filter { it.floorId == null }

    /** Beds already loaded for a room, keyed by room id. Absent = not fetched. */
    fun bedsOf(roomId: String): List<BedDto>? = bedsByRoom[roomId]
}

@HiltViewModel
class FloorsViewModel @Inject constructor(
    private val repo: BranchRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FloorsUiState())
    val state: StateFlow<FloorsUiState> = _state.asStateFlow()

    private var branchId: String = ""

    fun start(branchId: String) {
        if (this.branchId == branchId) return
        this.branchId = branchId
        refresh()
    }

    fun refresh() {
        if (branchId.isEmpty()) return
        _state.update { it.copy(loading = it.floors.isEmpty(), error = null) }
        viewModelScope.launch {
            // Rooms come along so each floor can show what is on it, and so the
            // owner can move a stray room onto a floor from the same screen.
            val (floors, rooms) = coroutineScope {
                val floorsDeferred = async { repo.floors(branchId) }
                val roomsDeferred = async { repo.rooms(branchId) }
                floorsDeferred.await() to roomsDeferred.await()
            }
            when (floors) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        floors = floors.data,
                        rooms = (rooms as? ApiResult.Success)?.data ?: it.rooms,
                        error = null,
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = floors.error.message)
                }
            }
        }
    }

    fun createFloor(name: String, onDone: () -> Unit) = mutate(onDone) {
        // New floors land after the existing ones rather than at position 0.
        repo.createFloor(branchId, name, _state.value.floors.size + 1)
    }

    fun renameFloor(floor: FloorDto, name: String, onDone: () -> Unit) = mutate(onDone) {
        repo.updateFloor(floor.id, name, floor.sortOrder)
    }

    fun deleteFloor(floor: FloorDto, onDone: () -> Unit) = mutate(onDone) {
        repo.deleteFloor(floor.id)
    }

    /** Add a room directly onto a floor — the floor comes from context. */
    fun addRoom(floorId: String, number: String, coolingType: String, onDone: () -> Unit) =
        mutate(onDone) { repo.createRoom(branchId, number, coolingType, floorId).asChange() }

    /** Add a bed to a room already on a floor. */
    fun addBed(roomId: String, label: String, onDone: () -> Unit) =
        mutate(onDone) { repo.createBed(roomId, label).asChange() }

    fun toggleRoom(roomId: String) {
        val expanding = _state.value.expandedRoomId != roomId
        _state.update { it.copy(expandedRoomId = if (expanding) roomId else null) }
        if (expanding && _state.value.bedsByRoom[roomId] == null) {
            viewModelScope.launch {
                (repo.beds(roomId) as? ApiResult.Success)?.let { beds ->
                    _state.update { it.copy(bedsByRoom = it.bedsByRoom + (roomId to beds.data)) }
                }
            }
        }
    }

    fun assignRoom(roomId: String, floorId: String?, onDone: () -> Unit) = mutate(onDone) {
        when (val r = repo.setRoomFloor(roomId, floorId)) {
            is ApiResult.Success -> ApiResult.Success(
                ChangeResponse(r.data.status, r.data.changeRequestId),
            )
            is ApiResult.Failure -> r
        }
    }

    fun clearSaveError() = _state.update { it.copy(saveError = null) }

    /**
     * Every write here goes through maker-checker: an owner's change lands, a
     * manager's queues. The caller cannot tell which from the request alone, so
     * the response decides what the user is told.
     */
    private fun mutate(onDone: () -> Unit, block: suspend () -> ApiResult<ChangeResponse>) {
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    onDone()
                    if (!result.data.applied) {
                        snackbar.show("Sent for approval")
                    }
                    refresh()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(saving = false, saveError = result.error.message)
                }
            }
        }
    }

}

/** A create response reads as a change: applied outright, or queued. */
private fun ApiResult<MutationResponse>.asChange(): ApiResult<ChangeResponse> = when (this) {
    is ApiResult.Success -> ApiResult.Success(ChangeResponse(data.status, data.changeRequestId))
    is ApiResult.Failure -> this
}
