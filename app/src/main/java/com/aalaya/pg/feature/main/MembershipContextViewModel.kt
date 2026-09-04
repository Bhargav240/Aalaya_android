package com.aalaya.pg.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.dto.MembershipDto
import com.aalaya.pg.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MembershipContextState(
    val loading: Boolean = true,
    val switchingId: String? = null,
    val memberships: List<MembershipDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MembershipContextViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MembershipContextState())
    val state: StateFlow<MembershipContextState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = authRepository.memberships()) {
            is ApiResult.Success -> _state.value = MembershipContextState(
                loading = false,
                memberships = result.data,
            )
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
        }
    }

    fun switchTo(membership: MembershipDto) {
        if (_state.value.switchingId != null) return
        _state.update { it.copy(switchingId = membership.membershipId, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.switchOrganization(membership)) {
                is ApiResult.Success -> Unit // The authenticated shell restarts via sessionEpoch.
                is ApiResult.Failure -> _state.update {
                    it.copy(switchingId = null, error = result.error.message)
                }
            }
        }
    }
}
