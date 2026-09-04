package com.aalaya.pg.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TenantInvitationDto(
    val id: String,
    val orgId: String,
    val branchId: String,
    val intendedRoomId: String? = null,
    val intendedBedId: String? = null,
    val email: String,
    val fullName: String,
    val planKind: String = "monthly",
    val monthlyRentPaise: Long? = null,
    val depositPaise: Long? = null,
    val plannedMoveInDate: String? = null,
    val requireIdentityVerification: Boolean,
    val status: String,
    val expiresAt: String,
    val lastSentAt: String,
    val acceptedByUserId: String? = null,
    val acceptedAt: String? = null,
    val approvedAt: String? = null,
    val approvedByUserId: String? = null,
)

@Serializable
data class CreateTenantInvitationRequest(
    val email: String,
    val fullName: String,
    val planKind: String,
    val branchId: String,
    val intendedRoomId: String,
    val intendedBedId: String? = null,
    val monthlyRentPaise: Long? = null,
    val depositPaise: Long? = null,
    val plannedMoveInDate: String? = null,
    val requireIdentityVerification: Boolean? = null,
    val expiresInDays: Long? = null,
)

@Serializable
data class OnboardingStatusDto(
    val invitationId: String,
    val status: String,
    val requireIdentityVerification: Boolean,
    val nextAction: String,
)

@Serializable
data class DigiLockerStartDto(val authorizationUrl: String)

@Serializable
data class InvitationVerificationDto(
    val provider: String,
    val providerReference: String? = null,
    val documentType: String? = null,
    val status: String,
    val verifiedAt: String? = null,
    val safeFailureReason: String? = null,
)

@Serializable
data class ManualKycReviewRequest(
    val decision: String,
    val reason: String,
)
