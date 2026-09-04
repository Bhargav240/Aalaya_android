package com.aalaya.pg.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.data.AuthState
import com.aalaya.pg.core.data.ApiCache
import com.aalaya.pg.core.data.BranchContext
import com.aalaya.pg.core.data.SessionManager
import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.core.network.dto.BranchDto
import com.aalaya.pg.core.network.dto.ResidentDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.feature.branches.BranchRepository
import com.aalaya.pg.feature.residents.ResidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * A resident paired with their current balance and, where known, the bed they
 * occupy. [roomNumber] is resolved by walking the branch's rooms and beds —
 * `ResidentDto` itself carries no placement — and drives the floor/room filters.
 */
@Serializable
data class ResidentDue(
    val resident: ResidentDto,
    val outstandingPaise: Long,
    val rentPaise: Long = 0L,
    /**
     * Money already received. "Paid" is derived, not stored: a resident reads
     * as settled when nothing is outstanding. This is what tells the UI whether
     * there is actually a payment available to reverse, since someone who has
     * simply not accrued rent yet also shows nothing outstanding.
     */
    val paidTotalPaise: Long = 0L,
    val roomId: String? = null,
    val roomNumber: String? = null,
    val bedLabel: String? = null,
)

data class PaymentsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    /** Only populated for an Owner, who must pick which branch to view. */
    val branches: List<BranchDto> = emptyList(),
    val isOwner: Boolean = false,
    val selectedBranchId: String? = null,
    val selectedBranchName: String = "",
    val dues: List<ResidentDue> = emptyList(),
    val rooms: List<RoomDto> = emptyList(),
    val selectedFloor: String? = null,
    val selectedRoomId: String? = null,
    /** Resident id currently being marked paid, so only that row spins. */
    val markingPaidId: String? = null,
    /** Resident whose payment is currently being reversed, for the spinner. */
    val markingUnpaidId: String? = null,
)

@Serializable
private data class PaymentsCache(
    val dues: List<ResidentDue>,
    val rooms: List<RoomDto>,
)

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val residentRepo: ResidentRepository,
    private val branchRepo: BranchRepository,
    private val sessionManager: SessionManager,
    private val branchContext: BranchContext,
    private val snackbar: SnackbarManager,
    private val cache: ApiCache,
) : ViewModel() {

    /** In-flight collection keys, so a retry replays rather than duplicates. */
    private val idempotencyKeys = mutableMapOf<String, String>()

    private val _state = MutableStateFlow(PaymentsUiState())
    val state: StateFlow<PaymentsUiState> = _state.asStateFlow()

    init { start() }

    private fun start() {
        val session = (sessionManager.state.value as? AuthState.Authenticated)?.session
        val isManager = session?.role == Role.Manager && session.branchId != null
        _state.update { it.copy(isOwner = !isManager) }

        viewModelScope.launch {
            if (!branchContext.loaded.value) {
                when (val r = branchRepo.branches()) {
                    is ApiResult.Success -> branchContext.setBranches(r.data)
                    is ApiResult.Failure ->
                        _state.update { it.copy(loading = false, error = r.error.message) }
                }
            }
            if (isManager) {
                val name = branchContext.branches.value
                    .firstOrNull { it.id == session!!.branchId }?.name.orEmpty()
                branchContext.select(session!!.branchId!!, name)
            }
        }

        // Follows whichever branch is active app-wide, so the dues list can
        // never disagree with what Occupancy is showing.
        viewModelScope.launch {
            branchContext.selected.collect { selection ->
                _state.update {
                    it.copy(
                        selectedBranchId = selection?.id,
                        selectedBranchName = selection?.name.orEmpty(),
                        dues = emptyList(),
                        rooms = emptyList(),
                        selectedFloor = null,
                        selectedRoomId = null,
                        loading = selection != null,
                        error = null,
                    )
                }
                if (selection != null) {
                    cache.read(paymentCacheKey(selection.id), PaymentsCache.serializer(), CACHE_MAX_AGE_MS)
                        ?.let { cached ->
                            _state.update {
                                it.copy(loading = false, dues = cached.dues, rooms = cached.rooms)
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

    fun selectFloor(floorKey: String?) {
        _state.update { it.copy(selectedFloor = floorKey, selectedRoomId = null) }
    }

    fun selectRoom(roomId: String?) {
        _state.update { it.copy(selectedRoomId = roomId) }
    }

    fun refresh() {
        val branchId = _state.value.selectedBranchId ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // A7: fetch the roster and the per-resident dues rollup in parallel —
            // one /dues call replaces the previous N ledger fan-out.
            coroutineScope {
                val residentsDeferred = async { residentRepo.residents(branchId) }
                val duesDeferred = async { residentRepo.branchDues(branchId) }
                // Room/floor filters must not depend on the branch having
                // residents yet. A newly configured PG has rooms before its
                // first resident or payment, and should still offer filters.
                val roomsDeferred = async { branchRepo.rooms(branchId) }
                val bedsDeferred = async { branchRepo.branchBeds(branchId) }
                val r = residentsDeferred.await()
                val rooms = when (val rr = roomsDeferred.await()) {
                    is ApiResult.Success -> rr.data
                    is ApiResult.Failure -> emptyList()
                }
                val duesByResident = when (val dr = duesDeferred.await()) {
                    is ApiResult.Success -> dr.data.associateBy { it.resident_id }
                    is ApiResult.Failure -> emptyMap()
                }
                when (r) {
                    is ApiResult.Success -> {

                    // Build resident → bed placement from ONE batch call
                    // (was: a room->bed fan-out — M rooms, M+1 requests).
                    // A resident record has no room reference; this fills the
                    // floor/room filters.
                    val roomNumberById = rooms.associate { it.id to it.number }
                    val branchBeds = when (val br = bedsDeferred.await()) {
                        is ApiResult.Success -> br.data
                        is ApiResult.Failure -> emptyList()
                    }
                    val placement: Map<String, Triple<String, String, String>> = branchBeds
                        .mapNotNull { bed ->
                            val rid = bed.occupantResidentId ?: return@mapNotNull null
                            val roomNumber = roomNumberById[bed.roomId].orEmpty()
                            rid to Triple(bed.roomId, roomNumber, bed.label)
                        }
                        .toMap()

                    val dues = r.data.map { resident ->
                        val due = duesByResident[resident.id]
                        val where = placement[resident.id]
                        ResidentDue(
                            resident = resident,
                            outstandingPaise = due?.outstanding_paise ?: 0L,
                            rentPaise = due?.rent_paise ?: 0L,
                            paidTotalPaise = due?.paid_total_paise ?: 0L,
                            roomId = where?.first,
                            roomNumber = where?.second,
                            bedLabel = where?.third,
                        )
                    }.sortedByDescending { it.outstandingPaise }

                    cache.write(
                        paymentCacheKey(branchId),
                        PaymentsCache.serializer(),
                        PaymentsCache(dues = dues, rooms = rooms),
                    )
                    _state.update { it.copy(loading = false, dues = dues, rooms = rooms) }
                }
                    is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
                }
            }
        }
    }

    /**
     * Records a cash payment against a resident. The backend generates the
     * receipt, so the list is refreshed afterwards to pick up the new balance.
     */
    fun markPaid(residentId: String, amountPaise: Long, onDone: () -> Unit = {}) {
        if (_state.value.markingPaidId != null || amountPaise <= 0L) return
        _state.update { it.copy(markingPaidId = residentId) }
        // One key per (resident, amount) collection attempt. A retry after a
        // timeout reuses it and gets the original receipt back; collecting from
        // the same resident again later is a different amount-or-balance and
        // mints a new one, so genuine second payments still go through.
        val key = idempotencyKeys.getOrPut("$residentId:$amountPaise") {
            UUID.randomUUID().toString()
        }
        viewModelScope.launch {
            when (val r = residentRepo.recordPayment(residentId, amountPaise, key)) {
                is ApiResult.Success -> {
                    // Collected: the key has done its job and must not be
                    // reused, or a later legitimate payment of the same amount
                    // would silently replay this receipt.
                    idempotencyKeys.remove("$residentId:$amountPaise")
                    _state.update { it.copy(markingPaidId = null) }
                    snackbar.show("Payment recorded — receipt issued")
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(markingPaidId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }

    /**
     * Reverses the resident's most recent payment — the undo for a mistaken
     * [markPaid]. The backend voids the payment and its receipt, so the list is
     * refreshed to pick up the restored balance rather than being patched
     * locally: that keeps the screen honest if anything else changed too.
     */
    fun markUnpaid(residentId: String, onDone: () -> Unit = {}) {
        if (_state.value.markingUnpaidId != null) return
        _state.update { it.copy(markingUnpaidId = residentId) }
        viewModelScope.launch {
            when (val r = residentRepo.voidLastPayment(residentId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(markingUnpaidId = null) }
                    snackbar.show("Payment reversed — receipt withdrawn")
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(markingUnpaidId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }
}

private const val CACHE_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
private fun paymentCacheKey(branchId: String) = "payments_$branchId"
