package com.aalaya.pg.feature.occupancy

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.data.AuthState
import com.aalaya.pg.core.data.BranchContext
import com.aalaya.pg.core.data.SessionManager
import com.aalaya.pg.core.domain.model.Floors
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.BranchDto
import com.aalaya.pg.core.network.dto.CreateTenantInvitationRequest
import com.aalaya.pg.core.network.dto.FloorDto
import com.aalaya.pg.core.network.dto.LedgerDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.feature.branches.BranchRepository
import com.aalaya.pg.feature.branches.OccupancyCache
import com.aalaya.pg.feature.invitations.InvitationRepository
import com.aalaya.pg.feature.residents.ResidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A new resident's details, captured when registering them into a specific free bed. */
data class RegisterForm(
    val email: String = "",
    val fullName: String = "",
    val planKind: PlanKind = PlanKind.Monthly,
    val rentRupees: String = "",
    val depositRupees: String = "",
    val moveInDate: String = "",
    val requireKyc: Boolean = true,
)

/** The resident summary shown in the tap-a-bed sheet. */
data class ResidentSheetState(
    val residentId: String,
    val name: String,
    val bedLabel: String,
    val loading: Boolean = true,
    val ledger: LedgerDto? = null,
)

@Immutable
data class OccupancyUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val isOwner: Boolean = false,
    val branches: List<BranchDto> = emptyList(),
    val selectedBranchId: String? = null,
    val selectedBranchName: String = "",
    val rooms: List<RoomDto> = emptyList(),
    val bedsByRoom: Map<String, List<BedDto>> = emptyMap(),
    /** Real floor rows for this branch, used to file a new room correctly. */
    val floors: List<FloorDto> = emptyList(),
    val expandedRoomId: String? = null,
    /** Floor/room cascade — floor keys are derived from room numbers. */
    val selectedFloor: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val sheet: ResidentSheetState? = null,
) {
    /** Distinct floors currently represented, used to position a new one. */
    val floorCount: Int get() = Floors.keysOf(rooms.map { it.number }).size
}

@HiltViewModel
class OccupancyViewModel @Inject constructor(
    private val branchRepo: BranchRepository,
    private val residentRepo: ResidentRepository,
    private val invitationRepo: InvitationRepository,
    private val sessionManager: SessionManager,
    private val branchContext: BranchContext,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(OccupancyUiState())
    val state: StateFlow<OccupancyUiState> = _state.asStateFlow()

    init { start() }

    private fun start() {
        val session = (sessionManager.state.value as? AuthState.Authenticated)?.session
        val isManager = session?.role == Role.Manager && session.branchId != null
        _state.update { it.copy(isOwner = !isManager) }

        // The branch list is shared, so only fetch it if nobody has yet.
        viewModelScope.launch {
            if (!branchContext.loaded.value) {
                // Disk cache makes the branch scope available before the
                // background network refresh finishes after an app restart.
                branchRepo.cachedBranches()?.let(branchContext::setBranches)
                when (val r = branchRepo.branches()) {
                    is ApiResult.Success -> branchContext.setBranches(r.data)
                    is ApiResult.Failure ->
                        _state.update { it.copy(loading = false, error = r.error.message) }
                }
            }
            // A manager is pinned to their own branch — never a choice.
            if (isManager) {
                val name = branchContext.branches.value
                    .firstOrNull { it.id == session!!.branchId }?.name.orEmpty()
                branchContext.select(session!!.branchId!!, name)
            }
        }

        // React to whatever branch the user picked — here or on any other screen.
        viewModelScope.launch {
            branchContext.selected.collect { selection ->
                _state.update {
                    it.copy(
                        selectedBranchId = selection?.id,
                        selectedBranchName = selection?.name.orEmpty(),
                        rooms = emptyList(),
                        bedsByRoom = emptyMap(),
                        expandedRoomId = null,
                        selectedFloor = null,
                        loading = selection != null,
                        error = null,
                    )
                }
                if (selection != null) {
                    // Paint the last known layout immediately. refresh() keeps
                    // it visible and silently replaces it with server data.
                    branchRepo.cachedOccupancy(selection.id)?.let { cached ->
                        _state.update {
                            it.copy(
                                loading = false,
                                rooms = cached.rooms,
                                bedsByRoom = cached.beds.groupBy { bed -> bed.roomId },
                                floors = cached.floors,
                            )
                        }
                    }
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            branchContext.branches.collect { list ->
                _state.update { it.copy(branches = list) }
            }
        }
    }

    /** Writes to the shared context, so every screen follows. */
    fun selectBranch(branchId: String, branchName: String) {
        branchContext.select(
            branchId,
            branchName.ifBlank {
                branchContext.branches.value.firstOrNull { it.id == branchId }?.name.orEmpty()
            },
        )
    }

    fun refresh() {
        val branchId = _state.value.selectedBranchId ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // These are three independent reads. Starting beds/floors only
            // after rooms made the first Occupancy paint cost the sum of all
            // three request times. Fetch them together and publish one
            // coherent screen state when the responses arrive.
            coroutineScope {
                val roomsDeferred = async { branchRepo.rooms(branchId) }
                val bedsDeferred = async { branchRepo.branchBeds(branchId) }
                val floorsDeferred = async { branchRepo.floors(branchId) }

                val rooms = roomsDeferred.await()
                val beds = bedsDeferred.await()
                val floors = floorsDeferred.await()

                when (rooms) {
                is ApiResult.Success -> {
                    val cachedBeds = (beds as? ApiResult.Success)?.data.orEmpty()
                    val cachedFloors = (floors as? ApiResult.Success)?.data.orEmpty()
                    branchRepo.cacheOccupancy(
                        branchId,
                        OccupancyCache(rooms.data, cachedBeds, cachedFloors),
                    )
                    _state.update {
                        it.copy(
                            loading = false,
                            rooms = rooms.data,
                            // The room list remains useful if either optional
                            // supporting request fails.
                            bedsByRoom = cachedBeds.groupBy { bed -> bed.roomId },
                            floors = cachedFloors,
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = rooms.error.message)
                }
                }
            }
        }
    }

    fun selectFloor(floorKey: String?) {
        _state.update { it.copy(selectedFloor = floorKey) }
    }

    /** Opens the resident sheet for an occupied bed and loads their balance. */
    fun openResidentSheet(residentId: String, name: String, bedLabel: String) {
        _state.update {
            it.copy(sheet = ResidentSheetState(residentId, name, bedLabel, loading = true))
        }
        viewModelScope.launch {
            val ledger = (residentRepo.ledger(residentId) as? ApiResult.Success)?.data
            _state.update { s ->
                s.copy(
                    sheet = s.sheet
                        ?.takeIf { it.residentId == residentId }
                        ?.copy(loading = false, ledger = ledger),
                )
            }
        }
    }

    fun closeResidentSheet() {
        _state.update { it.copy(sheet = null) }
    }

    fun toggleRoom(roomId: String) {
        val expanding = _state.value.expandedRoomId != roomId
        _state.update { it.copy(expandedRoomId = if (expanding) roomId else null) }
        if (expanding && _state.value.bedsByRoom[roomId] == null) loadBeds(roomId)
    }

    private fun loadBeds(roomId: String) {
        viewModelScope.launch {
            when (val r = branchRepo.beds(roomId)) {
                is ApiResult.Success -> _state.update { it.copy(bedsByRoom = it.bedsByRoom + (roomId to r.data)) }
                is ApiResult.Failure -> snackbar.show(r.error.message)
            }
        }
    }

    /**
     * Adds a room to the floor currently selected on this screen.
     *
     * Occupancy groups by a floor *key* derived from room numbers ("2"), not by
     * a floor row, so the key is matched back to a real floor by its label
     * ("Floor 2") — the same name the seeder and the add-floor form produce.
     * If no such row exists the room is still created, just unfiled, exactly as
     * every room did before floors were introduced.
     */
    fun addRoom(number: String, coolingType: String, beds: Int, onDone: () -> Unit) {
        val branchId = _state.value.selectedBranchId ?: return
        if (number.isBlank() || _state.value.saving) return
        val floorKey = _state.value.selectedFloor
        val floorId = floorKey
            ?.takeIf { it != Floors.ALL }
            ?.let { key -> _state.value.floors.firstOrNull { it.name == Floors.labelOf(key) }?.id }

        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = branchRepo.createRoom(branchId, number, coolingType, floorId, beds)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    onDone()
                    snackbar.show(
                        if (r.data.applied) "Room $number added" else "Sent for approval",
                    )
                    refresh()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(saving = false, saveError = r.error.message)
                }
            }
        }
    }

    fun clearSaveError() = _state.update { it.copy(saveError = null) }

    fun addBed(roomId: String, label: String, onDone: () -> Unit) {
        if (label.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = branchRepo.createBed(roomId, label)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false, bedsByRoom = it.bedsByRoom - roomId) }
                    onDone()
                    loadBeds(roomId)
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }

    /** Sends the same tenant invitation used by the invitation-management screen, prefilled for this bed. */
    fun registerResident(roomId: String, bedId: String, form: RegisterForm, onDone: () -> Unit) {
        val branchId = _state.value.selectedBranchId ?: return
        if (form.email.isBlank() || !form.email.contains('@') || form.fullName.isBlank() || _state.value.saving) return
        val rentPaise = Money.parseRupeesToPaise(form.rentRupees) ?: 0L
        val depositPaise = Money.parseRupeesToPaise(form.depositRupees) ?: 0L
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = invitationRepo.create(
                CreateTenantInvitationRequest(
                    email = form.email.trim(),
                    fullName = form.fullName.trim(),
                    planKind = form.planKind.wire,
                    branchId = branchId,
                    intendedRoomId = roomId,
                    intendedBedId = bedId,
                    monthlyRentPaise = rentPaise.takeIf { form.rentRupees.isNotBlank() },
                    depositPaise = depositPaise.takeIf { form.depositRupees.isNotBlank() },
                    plannedMoveInDate = form.moveInDate.trim().ifBlank { null },
                    requireIdentityVerification = form.requireKyc,
                ),
            )) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    snackbar.show("Invitation sent to ${form.email.trim()}.")
                    onDone()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = result.error.message) }
            }
        }
    }
}
