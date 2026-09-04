package com.aalaya.pg.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.data.AuthState
import com.aalaya.pg.core.data.BranchContext
import com.aalaya.pg.core.data.SessionManager
import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.feature.branches.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomAvailabilityUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rooms: List<VacantRoomItem> = emptyList(),
) {
    val totalVacancies: Int get() = rooms.sumOf { it.vacancyCount }
    val totalLabel: String
        get() = if (totalVacancies == 1) "1 vacancy" else "$totalVacancies vacancies"
    /** Branch name is only worth printing when more than one is in play. */
    val showsBranchNames: Boolean get() = rooms.map { it.branchName }.distinct().size > 1
}

/**
 * Every free room of one sharing type.
 *
 * The branch scope is re-derived here from the shared [BranchContext] rather
 * than passed through the route, so this screen and Home can never disagree
 * about which branches they are counting.
 */
@HiltViewModel
class RoomAvailabilityViewModel @Inject constructor(
    private val roomAvailability: RoomAvailabilityRepository,
    private val branchRepo: BranchRepository,
    private val sessionManager: SessionManager,
    private val branchContext: BranchContext,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomAvailabilityUiState())
    val state: StateFlow<RoomAvailabilityUiState> = _state.asStateFlow()

    private var sharingCount: Int = 0

    fun load(sharingCount: Int) {
        this.sharingCount = sharingCount
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = it.rooms.isEmpty(), error = null) }
        viewModelScope.launch {
            val session = (sessionManager.state.value as? AuthState.Authenticated)?.session

            val branches = branchContext.branches.value.ifEmpty {
                (branchRepo.branches() as? ApiResult.Success)?.data.orEmpty()
            }
            val names = branches.associate { it.id to it.name }

            val branchIds = when {
                session?.role == Role.Manager && session.branchId != null -> listOf(session.branchId)
                branchContext.selected.value != null -> listOf(branchContext.selected.value!!.id)
                else -> listOfNotNull(branches.firstOrNull()?.id)
            }

            when (val result = roomAvailability.vacantRooms(branchIds, sharingCount, names)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, rooms = result.data, error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, rooms = emptyList(), error = result.error.message)
                }
            }
        }
    }
}
