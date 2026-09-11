package com.aalaya.pg.feature.dashboard

import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DashboardUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
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
 * Answers what can be offered to walk-in tenants today: free rooms by sharing type.
 * Optimized with immediate in-memory/disk cache seeding (0ms initial render) and
 * resilient background revalidation.
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

    private var refreshJob: Job? = null

    init {
        val initialSession = (sessionManager.state.value as? AuthState.Authenticated)?.session
        val initialIsOwner = initialSession?.role?.canApprove == true
        _state.update { it.copy(canApprove = initialIsOwner) }

        viewModelScope.launch {
            seedFromCache(initialSession?.role, initialSession?.branchId)
            refresh()
        }
        observeScope()
    }

    /**
     * Seed state from cache for instantaneous (0ms) first frame render.
     */
    private suspend fun seedFromCache(role: Role?, sessionBranchId: String?) {
        val cachedBranches = branchRepo.cachedBranches()
        if (!cachedBranches.isNullOrEmpty()) {
            branchContext.setBranches(cachedBranches)
            val selectedId = branchContext.selected.value?.id ?: cachedBranches.firstOrNull()?.id
            _state.update {
                it.copy(
                    branches = cachedBranches,
                    scopedBranchId = selectedId,
                )
            }
        }
        val scope = resolveScope(role, sessionBranchId)
        if (scope.branchIds.isNotEmpty()) {
            val cachedSummary = roomAvailability.cachedSummary(scope.branchIds)
            if (cachedSummary != null) {
                _state.update {
                    it.copy(
                        loading = false,
                        roomSummary = cachedSummary,
                        roomSummaryLoading = false,
                        roomScopeBranchIds = scope.branchIds,
                        roomScopeLabel = scope.label,
                    )
                }
            }
        }
    }

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
                val currentScope = _state.value.scopedBranchId
                if (currentScope == selection?.id) return@collect
                _state.update { it.copy(scopedBranchId = selection?.id) }
                val session = (sessionManager.state.value as? AuthState.Authenticated)?.session
                loadRoomSummary(session?.role, session?.branchId)
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val session = (sessionManager.state.value as? AuthState.Authenticated)?.session
            val isOwner = session?.role?.canApprove == true
            _state.update {
                it.copy(
                    canApprove = isOwner,
                    refreshing = true,
                    loading = it.roomSummary.isEmpty() && it.branches.isEmpty(),
                    error = null,
                    roomSummaryLoading = it.roomSummary.isEmpty(),
                    roomSummaryError = null,
                )
            }

            try {
                val branchesDeferred = async { branchRepo.branches() }

                val approvalsDeferred = if (isOwner) {
                    async { approvalRepo.pending() }
                } else null

                // Stream branches as soon as they complete
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

                // Stream approvals as soon as they complete
                if (approvalsDeferred != null) {
                    (approvalsDeferred.await() as? ApiResult.Success)?.let { res ->
                        _state.update { it.copy(pendingApprovals = res.data.size) }
                    }
                } else {
                    _state.update { it.copy(pendingApprovals = 0) }
                }

                loadRoomSummary(session?.role, session?.branchId)
                // Brief yield so Compose PullToRefresh indicator registers active refresh state cleanly
                delay(200)
            } finally {
                _state.update {
                    it.copy(
                        refreshing = false,
                        loading = false,
                        roomSummaryLoading = false,
                    )
                }
            }
        }
    }

    private suspend fun loadRoomSummary(role: Role?, sessionBranchId: String?) {
        val scope = resolveScope(role, sessionBranchId)
        _state.update {
            it.copy(
                roomScopeBranchIds = scope.branchIds,
                roomScopeLabel = scope.label,
                roomSummaryLoading = it.roomSummary.isEmpty(),
            )
        }

        if (_state.value.roomSummary.isEmpty() && scope.branchIds.isNotEmpty()) {
            val cached = roomAvailability.cachedSummary(scope.branchIds)
            if (cached != null) {
                _state.update {
                    it.copy(
                        roomSummary = cached,
                        roomSummaryLoading = false,
                    )
                }
            }
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
                    roomSummaryLoading = false,
                    roomSummaryError = if (it.roomSummary.isEmpty()) result.error.message else null,
                )
            }
        }
    }

    /**
     * Which branches the room figures cover.
     */
    private fun resolveScope(role: Role?, sessionBranchId: String?): RoomScope {
        val state = _state.value
        state.scopedBranchId?.let { id ->
            val name = state.branches.firstOrNull { it.id == id }?.name ?: "Selected branch"
            return RoomScope(listOf(id), name)
        }
        if (role == Role.Manager && sessionBranchId != null) {
            return RoomScope(listOf(sessionBranchId), "Your branch")
        }
        val first = state.branches.firstOrNull()
            ?: return RoomScope(emptyList(), "No branches")
        return RoomScope(listOf(first.id), first.name)
    }
}
