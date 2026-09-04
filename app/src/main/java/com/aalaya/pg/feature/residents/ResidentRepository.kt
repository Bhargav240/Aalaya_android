package com.aalaya.pg.feature.residents

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.AssignBedRequest
import com.aalaya.pg.core.network.dto.ChangeResponse
import com.aalaya.pg.core.network.dto.CreateResidentRequest
import com.aalaya.pg.core.network.dto.DeductionDto
import com.aalaya.pg.core.network.dto.LedgerDto
import com.aalaya.pg.core.network.dto.MoveOutRequest
import com.aalaya.pg.core.network.dto.PaymentRequest
import com.aalaya.pg.core.network.dto.ReceiptDto
import com.aalaya.pg.core.network.dto.ResidentDto
import com.aalaya.pg.core.network.dto.ResidentDueDto
import com.aalaya.pg.core.network.dto.SettlementDto
import com.aalaya.pg.core.network.dto.UpdateResidentRequest
import com.aalaya.pg.core.domain.model.PlanKind
import com.aalaya.pg.feature.branches.toUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResidentRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    suspend fun residents(branchId: String): ApiResult<List<ResidentDto>> =
        executor.execute { api.residents(branchId) }

    /** Batch dues (A7): one call replaces the per-resident ledger fan-out. */
    suspend fun branchDues(branchId: String): ApiResult<List<ResidentDueDto>> =
        executor.execute { api.branchDues(branchId) }

    suspend fun create(
        branchId: String,
        fullName: String,
        phone: String?,
        plan: PlanKind,
        rentPaise: Long,
        depositPaise: Long,
    ): ApiResult<Unit> = createAndGetId(branchId, fullName, phone, plan, rentPaise, depositPaise).toUnit()

    /** Same as [create], but returns the new resident's id (e.g. to assign a bed right after). */
    suspend fun createAndGetId(
        branchId: String,
        fullName: String,
        phone: String?,
        plan: PlanKind,
        rentPaise: Long,
        depositPaise: Long,
    ): ApiResult<String> = when (
        val r = executor.execute {
            api.createResident(
                branchId,
                CreateResidentRequest(
                    fullName = fullName.trim(),
                    phone = phone?.trim()?.ifBlank { null },
                    planKind = plan.wire,
                    rentPaise = rentPaise,
                    depositPaise = depositPaise,
                ),
            )
        }
    ) {
        is ApiResult.Success -> ApiResult.Success(r.data.id)
        is ApiResult.Failure -> r
    }

    /** Maker-checker edit: applied instantly for owners, queued for managers. */
    suspend fun update(
        residentId: String,
        fullName: String?,
        phone: String?,
        plan: PlanKind?,
    ): ApiResult<ChangeResponse> = executor.execute {
        api.updateResident(
            residentId,
            UpdateResidentRequest(
                fullName = fullName?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                planKind = plan?.wire,
            ),
        )
    }

    /** Maker-checker soft delete. */
    suspend fun delete(residentId: String): ApiResult<ChangeResponse> =
        executor.execute { api.deleteResident(residentId) }

    /** Assign (bedId) or clear (null) the resident's bed. */
    suspend fun assignBed(residentId: String, bedId: String?): ApiResult<Unit> =
        executor.execute { api.assignBed(residentId, AssignBedRequest(bedId)) }.toUnit()

    suspend fun ledger(residentId: String): ApiResult<LedgerDto> =
        executor.execute { api.residentLedger(residentId) }

    suspend fun receipts(residentId: String): ApiResult<List<ReceiptDto>> =
        executor.execute { api.residentReceipts(residentId) }

    /** Records a payment; the backend generates + stores the receipt and returns its id. */
    /**
     * Records a payment. [idempotencyKey] must stay the same across retries of
     * the *same* collection and differ for a genuinely new one — the server
     * replays the original receipt for a key it has already seen, which is what
     * stops a double tap producing two invoices for one payment.
     */
    suspend fun recordPayment(
        residentId: String,
        amountPaise: Long,
        idempotencyKey: String,
    ): ApiResult<String> =
        when (
            val r = executor.execute {
                api.recordPayment(residentId, idempotencyKey, PaymentRequest(amountPaise))
            }
        ) {
            is ApiResult.Success -> ApiResult.Success(r.data.receiptId)
            is ApiResult.Failure -> r
        }

    /**
     * Reverses the resident's latest payment. The backend soft-deletes it and
     * its receipt, so the outstanding balance recomputes on the next read.
     */
    suspend fun voidLastPayment(residentId: String): ApiResult<Long> =
        when (val r = executor.execute { api.voidLastPayment(residentId) }) {
            is ApiResult.Success -> ApiResult.Success(r.data.amountPaise)
            is ApiResult.Failure -> r
        }

    suspend fun moveOut(
        residentId: String,
        lastDay: String,
        noticeDays: Long,
        deductions: List<DeductionDto>,
    ): ApiResult<SettlementDto> = executor.execute {
        api.moveOut(
            residentId,
            MoveOutRequest(
                lastDay = lastDay,
                noticeDays = noticeDays,
                deductions = deductions.ifEmpty { null },
            ),
        )
    }
}
