package com.aalaya.pg.feature.residents

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.core.network.dto.DeductionDto
import com.aalaya.pg.core.network.dto.LedgerDto
import com.aalaya.pg.core.network.dto.ReceiptDto
import com.aalaya.pg.core.network.dto.SettlementDto
import com.aalaya.pg.core.util.Money
import com.aalaya.pg.feature.receipts.ReceiptDownloader
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

data class ResidentDetailUiState(
    val loading: Boolean = true,
    val ledger: LedgerDto? = null,
    val receipts: List<ReceiptDto> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val payError: String? = null,
    val downloadingId: String? = null,
    val openUri: Uri? = null,
    val movingOut: Boolean = false,
    val moveOutError: String? = null,
    val settlement: SettlementDto? = null,
    val editing: Boolean = false,
    val editError: String? = null,
)

/** One editable deduction row in the move-out form. */
data class DeductionInput(val reason: String = "", val amountRupees: String = "")

@HiltViewModel
class ResidentDetailViewModel @Inject constructor(
    private val repo: ResidentRepository,
    private val downloader: ReceiptDownloader,
    private val snackbar: SnackbarManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val residentId: String = checkNotNull(savedStateHandle["residentId"])

    private val _state = MutableStateFlow(ResidentDetailUiState())
    val state: StateFlow<ResidentDetailUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // A resident's balance and receipt history do not depend on one
            // another. Fetch them together; the screen state and error
            // behavior remain unchanged.
            val (ledger, receipts) = coroutineScope {
                val ledgerDeferred = async { repo.ledger(residentId) }
                val receiptsDeferred = async { repo.receipts(residentId) }
                ledgerDeferred.await() to receiptsDeferred.await()
            }
            _state.update {
                it.copy(
                    loading = false,
                    ledger = (ledger as? ApiResult.Success)?.data,
                    receipts = (receipts as? ApiResult.Success)?.data ?: emptyList(),
                    error = (ledger as? ApiResult.Failure)?.error?.message,
                )
            }
        }
    }

    /** In-flight collection keys, so a retry replays rather than duplicates. */
    private val idempotencyKeys = mutableMapOf<String, String>()

    fun recordPayment(amountRupees: String, onDone: () -> Unit) {
        val paise = Money.parseRupeesToPaise(amountRupees) ?: 0L
        if (paise <= 0 || _state.value.saving) {
            _state.update { it.copy(payError = "Enter a valid amount.") }
            return
        }
        _state.update { it.copy(saving = true, payError = null) }
        // Same rule as the Payments list: one key per collection attempt, so a
        // retry replays the original receipt instead of issuing a second one.
        val key = idempotencyKeys.getOrPut(paise.toString()) { UUID.randomUUID().toString() }
        viewModelScope.launch {
            when (val r = repo.recordPayment(residentId, paise, key)) {
                is ApiResult.Success -> {
                    idempotencyKeys.remove(paise.toString())
                    _state.update { it.copy(saving = false) }
                    snackbar.show("Payment recorded — receipt generated.")
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(saving = false, payError = r.error.message) }
            }
        }
    }

    fun downloadReceipt(receipt: ReceiptDto) {
        if (_state.value.downloadingId != null) return
        _state.update { it.copy(downloadingId = receipt.id) }
        viewModelScope.launch {
            when (val r = downloader.download(receipt.id, receipt.receiptNumber)) {
                is ApiResult.Success -> _state.update { it.copy(downloadingId = null, openUri = r.data) }
                is ApiResult.Failure -> {
                    _state.update { it.copy(downloadingId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }

    fun consumeOpenUri() = _state.update { it.copy(openUri = null) }

    fun moveOut(lastDay: String, noticeDays: String, deductions: List<DeductionInput>, onDone: () -> Unit) {
        val days = noticeDays.trim().toLongOrNull()
        if (!lastDay.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            _state.update { it.copy(moveOutError = "Last day must be YYYY-MM-DD.") }
            return
        }
        if (days == null || days < 0) {
            _state.update { it.copy(moveOutError = "Notice days must be a number.") }
            return
        }
        if (_state.value.movingOut) return
        _state.update { it.copy(movingOut = true, moveOutError = null) }
        viewModelScope.launch {
            val dtos = deductions
                .filter { it.reason.isNotBlank() }
                .map { DeductionDto(it.reason.trim(), Money.parseRupeesToPaise(it.amountRupees) ?: 0L) }
            when (val r = repo.moveOut(residentId, lastDay, days, dtos)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(movingOut = false, settlement = r.data) }
                    snackbar.show("Move-out settled.")
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(movingOut = false, moveOutError = r.error.message) }
            }
        }
    }

    fun clearSettlement() = _state.update { it.copy(settlement = null) }

    /** Maker-checker edit: owners apply instantly; managers queue for approval. */
    fun editResident(fullName: String, phone: String, plan: PlanKind?, onDone: () -> Unit) {
        if (_state.value.editing) return
        if (fullName.isBlank() && phone.isBlank() && plan == null) {
            _state.update { it.copy(editError = "Nothing to change.") }
            return
        }
        _state.update { it.copy(editing = true, editError = null) }
        viewModelScope.launch {
            when (val r = repo.update(residentId, fullName, phone, plan)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(editing = false) }
                    snackbar.show(
                        if (r.data.status == "pending") "Change sent for owner approval."
                        else "Resident updated.",
                    )
                    onDone()
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(editing = false, editError = r.error.message) }
            }
        }
    }

    /** Maker-checker delete (soft). */
    fun deleteResident(onDeleted: () -> Unit) {
        if (_state.value.editing) return
        _state.update { it.copy(editing = true, editError = null) }
        viewModelScope.launch {
            when (val r = repo.delete(residentId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(editing = false) }
                    if (r.data.status == "pending") {
                        snackbar.show("Deletion sent for owner approval.")
                    } else {
                        snackbar.show("Resident deleted.")
                        onDeleted()
                    }
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(editing = false) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }

}
