package com.aalaya.pg.feature.invitations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.BranchDto
import com.aalaya.pg.core.network.dto.CreateTenantInvitationRequest
import com.aalaya.pg.core.network.dto.InvitationVerificationDto
import com.aalaya.pg.core.network.dto.OnboardingStatusDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.core.network.dto.TenantInvitationDto
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.feature.branches.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class InvitationListState(
    val loading: Boolean = true,
    val invitations: List<TenantInvitationDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MyInvitationsViewModel @Inject constructor(
    private val repository: InvitationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(InvitationListState())
    val state: StateFlow<InvitationListState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.mine()) {
            is ApiResult.Success -> _state.value = InvitationListState(false, result.data)
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
        }
    }
}

@HiltViewModel
class InvitationManagementViewModel @Inject constructor(
    private val repository: InvitationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(InvitationListState())
    val state: StateFlow<InvitationListState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.managed()) {
            is ApiResult.Success -> _state.value = InvitationListState(false, result.data)
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
        }
    }
}

data class InvitationDetailState(
    val loading: Boolean = true,
    val invitation: TenantInvitationDto? = null,
    val onboarding: OnboardingStatusDto? = null,
    val verification: InvitationVerificationDto? = null,
    val error: String? = null,
    val actionInProgress: String? = null,
    val authorizationUrl: String? = null,
)

@HiltViewModel
class InvitationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: InvitationRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {
    private val invitationId: String = checkNotNull(savedStateHandle["invitationId"])
    private val _state = MutableStateFlow(InvitationDetailState())
    val state: StateFlow<InvitationDetailState> = _state.asStateFlow()
    private var pollJob: Job? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.get(invitationId)) {
            is ApiResult.Success -> {
                _state.update { it.copy(loading = false, invitation = result.data) }
                refreshStatus(showLoading = false)
            }
            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
        }
    }

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                refreshStatus(showLoading = false)
                delay(5_000)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun refreshStatus(showLoading: Boolean) {
        if (showLoading) _state.update { it.copy(loading = true) }
        when (val status = repository.onboardingStatus(invitationId)) {
            is ApiResult.Success -> {
                val verification = repository.verification(invitationId)
                _state.update {
                    it.copy(
                        loading = false,
                        onboarding = status.data,
                        verification = (verification as? ApiResult.Success)?.data,
                    )
                }
            }
            is ApiResult.Failure -> if (_state.value.invitation == null) {
                _state.update { it.copy(loading = false, error = status.error.message) }
            }
        }
    }

    fun consumeAuthorizationUrl() = _state.update { it.copy(authorizationUrl = null) }

    fun accept() = perform("accept", repository::accept)
    fun decline() = perform("decline", repository::decline)
    fun approve() = perform("approve", repository::approve)
    fun reject() = perform("reject", repository::reject)
    fun cancel() = perform("cancel", repository::cancel)

    fun resend() {
        if (_state.value.actionInProgress != null) return
        _state.update { it.copy(actionInProgress = "resend") }
        viewModelScope.launch {
            when (val result = repository.resend(invitationId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(invitation = result.data, actionInProgress = null) }
                    snackbar.show("Invitation sent again.")
                }
                is ApiResult.Failure -> finishFailure(result.error.message)
            }
        }
    }

    fun startDigiLocker() {
        if (_state.value.actionInProgress != null) return
        _state.update { it.copy(actionInProgress = "digilocker") }
        viewModelScope.launch {
            when (val result = repository.startDigiLocker(invitationId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(actionInProgress = null, authorizationUrl = result.data.authorizationUrl)
                }
                is ApiResult.Failure -> finishFailure(result.error.message)
            }
        }
    }

    fun manualReview(approved: Boolean, reason: String) {
        if (_state.value.actionInProgress != null || reason.isBlank()) return
        val action = if (approved) "manual_approve" else "manual_reject"
        _state.update { it.copy(actionInProgress = action) }
        viewModelScope.launch {
            when (val result = repository.manualReview(invitationId, approved, reason)) {
                is ApiResult.Success -> {
                    snackbar.show("Identity review recorded.")
                    _state.update { it.copy(actionInProgress = null) }
                    refresh()
                }
                is ApiResult.Failure -> finishFailure(result.error.message)
            }
        }
    }

    private fun perform(name: String, operation: suspend (String) -> ApiResult<Unit>) {
        if (_state.value.actionInProgress != null) return
        _state.update { it.copy(actionInProgress = name) }
        viewModelScope.launch {
            when (val result = operation(invitationId)) {
                is ApiResult.Success -> {
                    snackbar.show("Invitation updated.")
                    _state.update { it.copy(actionInProgress = null) }
                    refresh()
                }
                is ApiResult.Failure -> finishFailure(result.error.message)
            }
        }
    }

    private fun finishFailure(message: String) {
        _state.update { it.copy(actionInProgress = null) }
        snackbar.show(message)
    }
}

data class CreateInvitationState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val branches: List<BranchDto> = emptyList(),
    val rooms: List<RoomDto> = emptyList(),
    val beds: List<BedDto> = emptyList(),
    val email: String = "",
    val fullName: String = "",
    val planKind: PlanKind = PlanKind.Monthly,
    val branch: BranchDto? = null,
    val room: RoomDto? = null,
    val bed: BedDto? = null,
    val rentRupees: String = "",
    val depositRupees: String = "",
    val moveInDate: String = "",
    val requireKyc: Boolean = true,
    val error: String? = null,
    val createdInvitationId: String? = null,
)

@HiltViewModel
class CreateInvitationViewModel @Inject constructor(
    private val invitations: InvitationRepository,
    private val branches: BranchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateInvitationState())
    val state: StateFlow<CreateInvitationState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = branches.branches()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, branches = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
            }
        }
    }

    fun setEmail(value: String) = _state.update { it.copy(email = value.take(254), error = null) }
    fun setFullName(value: String) = _state.update { it.copy(fullName = value.take(120), error = null) }
    fun setPlanKind(value: PlanKind) = _state.update { it.copy(planKind = value, error = null) }
    fun setRent(value: String) = _state.update { it.copy(rentRupees = moneyInput(value), error = null) }
    fun setDeposit(value: String) = _state.update { it.copy(depositRupees = moneyInput(value), error = null) }
    fun setMoveInDate(value: String) = _state.update { it.copy(moveInDate = value.take(10), error = null) }
    fun setRequireKyc(value: Boolean) = _state.update { it.copy(requireKyc = value) }

    fun selectBranch(branch: BranchDto) {
        _state.update { it.copy(branch = branch, room = null, bed = null, rooms = emptyList(), beds = emptyList()) }
        viewModelScope.launch {
            when (val result = branches.rooms(branch.id)) {
                is ApiResult.Success -> _state.update { it.copy(rooms = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
        }
    }

    fun selectRoom(room: RoomDto) {
        _state.update { it.copy(room = room, bed = null, beds = emptyList()) }
        viewModelScope.launch {
            when (val result = branches.beds(room.id)) {
                is ApiResult.Success -> _state.update {
                    it.copy(beds = result.data.filter { bed -> bed.occupantResidentId == null })
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
        }
    }

    fun selectBed(bed: BedDto) = _state.update { it.copy(bed = bed) }

    fun submit() {
        val current = _state.value
        val validation = validate(current)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val request = CreateTenantInvitationRequest(
                email = current.email.trim(),
                fullName = current.fullName.trim(),
                planKind = current.planKind.wire,
                branchId = checkNotNull(current.branch).id,
                intendedRoomId = checkNotNull(current.room).id,
                intendedBedId = current.bed?.id,
                monthlyRentPaise = current.rentRupees.toPaiseOrNull(),
                depositPaise = current.depositRupees.toPaiseOrNull(),
                plannedMoveInDate = current.moveInDate.trim().ifBlank { null },
                requireIdentityVerification = current.requireKyc,
            )
            when (val result = invitations.create(request)) {
                is ApiResult.Success -> _state.update {
                    it.copy(saving = false, createdInvitationId = result.data.id)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(saving = false, error = result.error.message)
                }
            }
        }
    }

    fun consumeCreated() = _state.update { it.copy(createdInvitationId = null) }

    private fun validate(state: CreateInvitationState): String? = when {
        !state.email.contains('@') -> "Enter a valid email address."
        state.fullName.trim().length < 2 -> "Enter the tenant's full name."
        state.branch == null -> "Select a branch."
        state.room == null -> "Select a room."
        state.rentRupees.isNotBlank() && state.rentRupees.toPaiseOrNull() == null -> "Enter a valid rent amount."
        state.depositRupees.isNotBlank() && state.depositRupees.toPaiseOrNull() == null -> "Enter a valid deposit amount."
        state.moveInDate.isNotBlank() && !DATE_PATTERN.matches(state.moveInDate) -> "Use YYYY-MM-DD for move-in date."
        else -> null
    }
}

private val DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")

private fun moneyInput(value: String): String = value.filter { it.isDigit() || it == '.' }.take(12)

private fun String.toPaiseOrNull(): Long? {
    if (isBlank()) return null
    return runCatching {
        BigDecimal(this).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
    }.getOrNull()?.takeIf { it >= 0 }
}
