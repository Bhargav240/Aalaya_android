package com.aalaya.pg.feature.floors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.network.dto.NewRoomRequest
import com.aalaya.pg.feature.branches.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One room being described on the add-floor form. */
data class RoomDraft(
    val number: String = "",
    val coolingType: String = "non_ac",
    val beds: Int = 2,
)

data class AddFloorUiState(
    val floorName: String = "",
    val rooms: List<RoomDraft> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
) {
    val totalBeds: Int get() = rooms.sumOf { it.beds }

    /**
     * Every room needs a number, and no two may share one. The duplicate check
     * lives here rather than only on the server so the person sees it while
     * they are still looking at the field they mistyped.
     */
    val duplicateNumbers: Set<String>
        get() = rooms.map { it.number.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    val canSubmit: Boolean
        get() = floorName.isNotBlank() &&
            !saving &&
            rooms.all { it.number.isNotBlank() } &&
            duplicateNumbers.isEmpty()
}

/**
 * Builds a floor and the rooms on it as one form, submitted as one request.
 *
 * Creating the floor and then each room separately would mean a manager's
 * single action produced a floor request plus one request per room, and an
 * owner approving them one at a time. The backend takes the whole floor in a
 * single call and creates it atomically instead.
 */
@HiltViewModel
class AddFloorViewModel @Inject constructor(
    private val repo: BranchRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AddFloorUiState())
    val state: StateFlow<AddFloorUiState> = _state.asStateFlow()

    fun onFloorName(value: String) = _state.update {
        // Renumbering follows the floor name so the suggested room numbers stay
        // sensible if the person renames "Floor 2" to "Floor 3" mid-form.
        val renumbered = it.rooms.mapIndexed { index, room ->
            val suggested = suggestNumber(value, index)
            // Only replace a suggestion, never something typed by hand.
            if (room.number.isBlank() || room.number == suggestNumber(it.floorName, index)) {
                room.copy(number = suggested)
            } else {
                room
            }
        }
        it.copy(floorName = value.take(60), rooms = renumbered, error = null)
    }

    fun addRoom() = _state.update {
        it.copy(
            rooms = it.rooms + RoomDraft(number = suggestNumber(it.floorName, it.rooms.size)),
            error = null,
        )
    }

    fun removeRoom(index: Int) = _state.update {
        it.copy(rooms = it.rooms.filterIndexed { i, _ -> i != index })
    }

    /** Grow or shrink the list to [count], keeping what is already typed. */
    fun setRoomCount(count: Int) = _state.update { st ->
        val target = count.coerceIn(0, 30)
        val rooms = when {
            target <= st.rooms.size -> st.rooms.take(target)
            else -> st.rooms + (st.rooms.size until target).map {
                RoomDraft(number = suggestNumber(st.floorName, it))
            }
        }
        st.copy(rooms = rooms, error = null)
    }

    fun onRoomNumber(index: Int, value: String) = updateRoom(index) { it.copy(number = value.take(20)) }
    fun onRoomCooling(index: Int, value: String) = updateRoom(index) { it.copy(coolingType = value) }
    fun onRoomBeds(index: Int, value: Int) = updateRoom(index) { it.copy(beds = value.coerceIn(1, 26)) }

    fun clearError() = _state.update { it.copy(error = null) }

    fun submit(branchId: String, sortOrder: Int, onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val rooms = s.rooms.map {
                NewRoomRequest(number = it.number.trim(), coolingType = it.coolingType, beds = it.beds)
            }
            when (val result = repo.createFloor(branchId, s.floorName, sortOrder, rooms)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    snackbar.show(
                        if (result.data.applied) {
                            val n = rooms.size
                            when (n) {
                                0 -> "${s.floorName} added"
                                1 -> "${s.floorName} added with 1 room"
                                else -> "${s.floorName} added with $n rooms"
                            }
                        } else {
                            // A manager's floor does not exist yet, so the
                            // message must not claim it was created.
                            "Sent for approval"
                        },
                    )
                    onDone()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(saving = false, error = result.error.message)
                }
            }
        }
    }

    private fun updateRoom(index: Int, transform: (RoomDraft) -> RoomDraft) = _state.update { st ->
        st.copy(
            rooms = st.rooms.mapIndexed { i, room -> if (i == index) transform(room) else room },
            error = null,
        )
    }
}

/**
 * "Floor 3" + the 2nd room becomes "302". The convention is the same one the
 * rest of the app reads floors back out of, so suggested numbers stay
 * consistent with how Occupancy and Payments group them.
 */
internal fun suggestNumber(floorName: String, index: Int): String {
    val digits = floorName.filter { it.isDigit() }
    return if (digits.isEmpty()) "" else "$digits${"%02d".format(index + 1)}"
}
