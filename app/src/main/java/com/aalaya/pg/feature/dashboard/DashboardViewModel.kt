package com.aalaya.pg.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.data.AuthState
import com.aalaya.pg.core.data.BranchContext
import com.aalaya.pg.core.data.SessionManager
import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.core.network.dto.BranchDto
import com.aalaya.pg.feature.approvals.ApprovalRepository
import com.aalaya.pg.feature.branches.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val canApprove: Boolean = false,
    /** Change requests awaiting the owner's yes — tints the Approvals tile. */
    val pendingApprovals: Int = 0,
    /**
     * Owner-only branch scope. `null` means nothing has been picked yet; a
     * branch id scopes the room figures to that branch.
     */
    val branches: List<BranchDto> = emptyList(),
    val scopedBranchId: String? = null,
    /** Free rooms per sharing type, for the branches currently in scope. */
    val roomSummary: List<RoomSummaryRow> = emptyList(),
    val roomSummaryLoading: Boolean = true,
    val roomSummaryError: String? = null,
    val roomScopeBranchIds: List<String> = emptyList(),
    val roomScopeLabel: String = "Branch",
) {
    val totalVacantRooms: Int get() = roomSummary.sumOf { it.vacantCount }
}

/**
 * Staff home.
 *
 * Home used to lead with money — outstanding, collected, occupancy, complaints.
 * Those numbers each have a screen of their own (Payments, Occupancy,
 * Complaints), so repeating them here made the first screen a summary of other
 * screens. It now answers the one question that has no home elsewhere: *what
 * can I offer someone who walks in today?* — free rooms by sharing type.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val branchRepo: BranchRepository,
    private val approvalRepo: ApprovalRepository,
    private val roomAvailability: RoomAvailabilityRepository,
    private val sessionManager: SessionManager,
    private val branchContext: BranchContext,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init { refresh(); observeScope() }

    /**
     * Owner scope switch. Writes through to the shared context so Occupancy
     * and Payments follow the same active branch.
     */
    fun scopeToBranch(branchId: String) {
        if (_state.value.scopedBranchId == branchId) return
        val name = branchContext.branches.value
            .firstOrNull { it.id == branchId }?.name.orEmpty()
        branchContext.select(branchId, name)
    }

    /** Mirrors the shared context into dashboard state. */
    private fun observeScope() {
        viewModelScope.launch {
            branchContext.selected.collect { selection ->
                if (_state.value.scopedBranchId == selection?.id) return@collect
                _state.update { it.copy(scopedBranchId = selection?.id) }
                refresh()
            }
        }
    }

    fun refresh() {
        _state.update {
            it.copy(
                loading = true,
                error = null,
                roomSummaryLoading = it.roomSummary.isEmpty(),
                roomSummaryError = null,
            )
        }
        viewModelScope.launch {
            val session = (sessionManager.state.value as? AuthState.Authenticated)?.session
            val isOwner = session?.role?.canApprove == true
            _state.update { it.copy(canApprove = isOwner) }

            // Branches and the approval badge do not depend on one another.
            // Starting both now removes a complete network round trip from the
            // owner Home screen's first paint.
            val branchesDeferred = if (isOwner && _state.value.branches.isEmpty()) {
                async { branchRepo.branches() }
            } else {
                null
            }
            val approvalsDeferred = if (isOwner) async { approvalRepo.pending() } else null

            // Owners get the branch list so Home can scope itself; managers are
            // already pinned to their own branch.
            branchesDeferred?.await()?.let { result ->
                (result as? ApiResult.Success)?.let { r ->
                    branchContext.setBranches(r.data)
                    _state.update {
                        it.copy(
                            branches = r.data,
                            scopedBranchId = branchContext.selected.value?.id,
                        )
                    }
                }
            }

            // The approvals count only tints a tile, so a failure must never
            // blank the screen — it simply leaves the count at zero.
            if (approvalsDeferred != null) {
                (approvalsDeferred.await() as? ApiResult.Success)?.let { res ->
                    _state.update { it.copy(pendingApprovals = res.data.size) }
                }
            } else {
                _state.update { it.copy(pendingApprovals = 0) }
            }

            loadRoomSummary(session?.role, session?.branchId)
        }
    }

    private suspend fun loadRoomSummary(role: Role?, sessionBranchId: String?) {
        val scope = resolveScope(role, sessionBranchId)
        _state.update {
            it.copy(roomScopeBranchIds = scope.branchIds, roomScopeLabel = scope.label)
        }

        when (val result = roomAvailability.summary(scope.branchIds)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    loading = false,
                    roomSummary = result.data,
                    roomSummaryLoading = false,
                    roomSummaryError = null,
                )
            }
            is ApiResult.Failure -> _state.update {
                it.copy(
                    loading = false,
                    roomSummary = emptyList(),
                    roomSummaryLoading = false,
                    roomSummaryError = result.error.message,
                )
            }
        }
    }

    /**
     * Which branches the room figures cover.
     *
     * A manager only ever sees their own branch. An owner sees whichever branch
     * they picked; before they pick, the first branch stands in so Home opens
     * with real numbers rather than an empty prompt.
     */
    private fun resolveScope(role: Role?, sessionBranchId: String?): RoomScope {
        val state = _state.value
        if (role == Role.Manager && sessionBranchId != null) {
            return RoomScope(listOf(sessionBranchId), "Your branch")
        }
        state.scopedBranchId?.let { id ->
            val name = state.branches.firstOrNull { it.id == id }?.name ?: "Selected branch"
            return RoomScope(listOf(id), name)
        }
        if (role?.canApprove != true) return RoomScope(emptyList(), "No branches")
        val first = state.branches.firstOrNull()
            ?: return RoomScope(emptyList(), "No branches")
        return RoomScope(listOf(first.id), first.name)
    }
}
