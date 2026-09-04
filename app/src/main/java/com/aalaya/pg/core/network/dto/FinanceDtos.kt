package com.aalaya.pg.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Payments, receipts, ledgers, finance reports, change-requests.

@Serializable
data class PaymentRequest(val amountPaise: Long)

@Serializable
data class PaymentResponse(
    val paymentId: String,
    val receiptId: String,
    val receiptNumber: String,
    val invoiceNumber: String? = null,
)

/** What a reversal undid, so the UI can name the amount it put back. */
@Serializable
data class VoidPaymentResponse(
    val paymentId: String,
    val amountPaise: Long,
    val invoiceNumber: String? = null,
)

@Serializable
data class ReceiptDto(
    val id: String,
    val receiptNumber: String,
    val invoiceNumber: String? = null,
    val amountPaise: Long,
    val status: String,
    val issuedEpoch: Long,
)

@Serializable
data class LedgerDto(
    val fullName: String,
    val branchId: String? = null,
    val branchName: String? = null,
    val roomNumber: String? = null,
    val bedLabel: String? = null,
    val planKind: String,
    val status: String? = null,
    val rentPaise: Long,
    val depositPaise: Long,
    val paidTotalPaise: Long,
    val accruedPaise: Long,
    val outstandingPaise: Long,
)

@Serializable
data class FinanceReportDto(
    val collectedPaise: Long,
    val depositsPaise: Long,
    val accruedPaise: Long,
    val outstandingPaise: Long,
    val residentsActive: Long,
    val residentsTotal: Long,
    // Keep these wire names explicit: occupancy must be based on beds, not
    // resident counts, and this response is also consumed outside the global
    // Json configuration in tests and future clients.
    @SerialName("beds_total") val bedsTotal: Int = 0,
    @SerialName("beds_occupied") val bedsOccupied: Int = 0,
)

@Serializable
data class FinanceBranchDto(
    val branchId: String,
    val branchName: String,
    // The backend flattens the report fields onto this object.
    val collectedPaise: Long,
    val depositsPaise: Long,
    val accruedPaise: Long,
    val outstandingPaise: Long,
    val residentsActive: Long,
    val residentsTotal: Long,
    @SerialName("beds_total") val bedsTotal: Int = 0,
    @SerialName("beds_occupied") val bedsOccupied: Int = 0,
)

@Serializable
data class FinanceOrgDto(
    val total: FinanceReportDto,
    val branches: List<FinanceBranchDto>,
)

@Serializable
data class ChangeRequestDto(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val status: String,
    val requestedBy: String,
)

/** One resident's rollup row from GET /branches/{id}/dues (A7 batch endpoint). */
@Serializable
data class ResidentDueDto(
    val resident_id: String,
    val full_name: String,
    val plan_kind: String,
    val status: String,
    val rent_paise: Long,
    val deposit_paise: Long,
    val paid_total_paise: Long,
    val accrued_paise: Long,
    val outstanding_paise: Long,
)
