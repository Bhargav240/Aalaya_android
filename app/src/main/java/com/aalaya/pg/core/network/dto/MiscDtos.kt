package com.aalaya.pg.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProvisionResponse(
    val orgId: String,
    val ownerId: String,
)

// ─── Move-out settlement ─────────────────────────────────────────────────────

@Serializable
data class DeductionDto(val reason: String, val amountPaise: Long)

@Serializable
data class MoveOutRequest(
    val lastDay: String, // YYYY-MM-DD
    val noticeDays: Long,
    val deductions: List<DeductionDto>? = null,
)

@Serializable
data class SettlementDto(
    val rentToDatePaise: Long,
    val paidTotalPaise: Long,
    val outstandingPaise: Long,
    val depositHeldPaise: Long,
    val deductionsTotalPaise: Long,
    val refundPaise: Long,
    val forfeited: Boolean,
    val note: String,
)

// ─── Receipt template (org branding on receipts) ─────────────────────────────

@Serializable
data class ReceiptTemplateDto(
    val displayName: String,
    val address: String? = null,
    val footer: String? = null,
    val accent: String? = null,
    val logoKey: String? = null,
)

// ─── Audit log ───────────────────────────────────────────────────────────────

@Serializable
data class AuditEntryDto(
    val id: Long,
    val action: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val actorId: String? = null,
    val atEpoch: Long,
)

// ─── Admin ───────────────────────────────────────────────────────────────────

@Serializable
data class ArchiveRequest(val olderThanDays: Long? = null)

// ─── Health ──────────────────────────────────────────────────────────────────

@Serializable
data class HealthDto(
    val status: String,
    val dbReady: Boolean,
    val requestId: String? = null,
)
