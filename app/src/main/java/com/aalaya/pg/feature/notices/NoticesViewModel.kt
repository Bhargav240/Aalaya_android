package com.aalaya.pg.feature.notices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.dto.NoticeDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoticesUiState(
    val loading: Boolean = true,
    val items: List<NoticeDto> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val repo: NoticeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NoticesUiState())
    val state: StateFlow<NoticesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.warmCache()
            repo.cachedItems()?.let { cached ->
                _state.update { it.copy(loading = false, items = cached) }
            }
            refresh()
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.list(forceRefresh = true)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun create(title: String, body: String, onDone: () -> Unit) {
        if (title.isBlank() || _state.value.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val r = repo.create(title, body, null)) {
                is ApiResult.Success -> { _state.update { it.copy(saving = false) }; onDone(); refresh() }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }
}
