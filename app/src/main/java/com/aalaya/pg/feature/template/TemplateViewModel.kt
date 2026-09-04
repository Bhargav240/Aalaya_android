package com.aalaya.pg.feature.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.ReceiptTemplateDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val displayName: String = "",
    val address: String = "",
    val footer: String = "",
    val accent: String = "",
    val saving: Boolean = false,
    val saveError: String? = null,
)

/** Editor for the org's receipt branding (name, address, footer, accent colour). */
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TemplateUiState())
    val state: StateFlow<TemplateUiState> = _state.asStateFlow()

    private var logoKey: String? = null

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = executor.execute { api.receiptTemplate() }) {
                is ApiResult.Success -> {
                    logoKey = r.data.logoKey
                    _state.update {
                        it.copy(
                            loading = false,
                            displayName = r.data.displayName,
                            address = r.data.address.orEmpty(),
                            footer = r.data.footer.orEmpty(),
                            accent = r.data.accent.orEmpty(),
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.error.message) }
            }
        }
    }

    fun onDisplayName(v: String) = _state.update { it.copy(displayName = v) }
    fun onAddress(v: String) = _state.update { it.copy(address = v) }
    fun onFooter(v: String) = _state.update { it.copy(footer = v) }
    fun onAccent(v: String) = _state.update { it.copy(accent = v) }

    fun save() {
        val s = _state.value
        if (s.displayName.isBlank() || s.saving) {
            _state.update { it.copy(saveError = "Display name is required.") }
            return
        }
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            val body = ReceiptTemplateDto(
                displayName = s.displayName.trim(),
                address = s.address.trim().ifBlank { null },
                footer = s.footer.trim().ifBlank { null },
                accent = s.accent.trim().ifBlank { null },
                logoKey = logoKey,
            )
            when (val r = executor.execute { api.setReceiptTemplate(body) }) {
                is ApiResult.Success -> { _state.update { it.copy(saving = false) }; snackbar.show("Receipt branding saved.") }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, saveError = r.error.message) }
            }
        }
    }
}
