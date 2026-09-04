package com.aalaya.pg.feature.tenant

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.domain.model.ComplaintState
import com.aalaya.pg.core.network.dto.ComplaintDto
import com.aalaya.pg.core.network.dto.LedgerDto
import com.aalaya.pg.core.network.dto.NoticeDto
import com.aalaya.pg.core.network.dto.ReceiptDto
import com.aalaya.pg.feature.complaints.ComplaintRepository
import com.aalaya.pg.feature.notices.NoticeRepository
import com.aalaya.pg.feature.receipts.ReceiptDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class TenantHomeUiState(
    val loading: Boolean = true,
    val ledger: LedgerDto? = null,
    val receipts: List<ReceiptDto> = emptyList(),
    val receiptsLoading: Boolean = false,
    val error: String? = null,
    val downloadingId: String? = null,
    val openUri: Uri? = null,
    val notices: List<NoticeDto> = emptyList(),
    val complaints: List<ComplaintDto> = emptyList(),
) {
    /**
     * Complaints still being worked on. Resolved and closed ones are the
     * resident's history, not something home needs to shout about.
     */
    val openComplaints: List<ComplaintDto>
        get() = complaints.filter {
            ComplaintState.fromWire(it.state) !in
                listOf(ComplaintState.Resolved, ComplaintState.Closed)
        }

    /** Most recent receipt, used for the "last payment" line. */
    val lastReceipt: ReceiptDto? get() = receipts.maxByOrNull { it.issuedEpoch }
}

@HiltViewModel
class TenantHomeViewModel @Inject constructor(
    private val repo: TenantRepository,
    private val downloader: ReceiptDownloader,
    private val noticeRepo: NoticeRepository,
    private val complaintRepo: ComplaintRepository,
    private val snackbar: SnackbarManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TenantHomeUiState(
            loading = repo.cachedLedger() == null,
            ledger = repo.cachedLedger(),
            receipts = repo.cachedReceipts().orEmpty(),
            receiptsLoading = repo.cachedReceipts() == null,
            notices = noticeRepo.cachedItems().orEmpty(),
        ),
    )
    val state: StateFlow<TenantHomeUiState> = _state.asStateFlow()

    fun refresh(
        includeReceipts: Boolean = false,
        includeNotices: Boolean = false,
        includeComplaints: Boolean = false,
        includeLedger: Boolean = true,
        forceRefresh: Boolean = false,
    ) {
        _state.update {
            it.copy(
                loading = includeLedger && it.ledger == null,
                receiptsLoading = includeReceipts && repo.cachedReceipts() == null,
                error = null,
            )
        }
        viewModelScope.launch {
            val ledgerDeferred = if (includeLedger) async { repo.myLedger(forceRefresh = forceRefresh) } else null
            val noticesDeferred = if (includeNotices) async { noticeRepo.list(forceRefresh = forceRefresh) } else null
            val receiptsDeferred = if (includeReceipts) async { repo.myReceipts(forceRefresh = forceRefresh) } else null
            // Complaints only decorate a card; a failure must never blank the
            // screen, so the result is taken if it arrives and ignored if not.
            val complaintsDeferred = if (includeComplaints) async { complaintRepo.list() } else null
            val ledger = ledgerDeferred?.await()
            val receipts = receiptsDeferred?.await()
            val notices = noticesDeferred?.await()
            val complaints = complaintsDeferred?.await()
            _state.update {
                it.copy(
                    loading = false,
                    ledger = (ledger as? ApiResult.Success)?.data ?: it.ledger,
                    receipts = (receipts as? ApiResult.Success)?.data ?: it.receipts,
                    receiptsLoading = false,
                    notices = (notices as? ApiResult.Success)?.data ?: it.notices,
                    complaints = (complaints as? ApiResult.Success)?.data ?: it.complaints,
                    error = (ledger as? ApiResult.Failure)?.error?.message
                        ?: (receipts as? ApiResult.Failure)?.error?.message,
                )
            }
        }
    }

    fun downloadReceipt(receipt: ReceiptDto) {
        if (_state.value.downloadingId != null) return
        _state.update { it.copy(downloadingId = receipt.id) }
        viewModelScope.launch {
            when (val r = downloader.download(receipt.id, receipt.receiptNumber, mine = true)) {
                is ApiResult.Success -> _state.update { it.copy(downloadingId = null, openUri = r.data) }
                is ApiResult.Failure -> {
                    _state.update { it.copy(downloadingId = null) }
                    snackbar.show(r.error.message)
                }
            }
        }
    }

    fun consumeOpenUri() = _state.update { it.copy(openUri = null) }
}
