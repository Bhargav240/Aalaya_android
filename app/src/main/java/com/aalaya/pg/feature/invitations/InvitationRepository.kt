package com.aalaya.pg.feature.invitations

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.CreateTenantInvitationRequest
import com.aalaya.pg.core.network.dto.DigiLockerStartDto
import com.aalaya.pg.core.network.dto.InvitationVerificationDto
import com.aalaya.pg.core.network.dto.ManualKycReviewRequest
import com.aalaya.pg.core.network.dto.OnboardingStatusDto
import com.aalaya.pg.core.network.dto.TenantInvitationDto
import com.aalaya.pg.feature.branches.toUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvitationRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    suspend fun mine(): ApiResult<List<TenantInvitationDto>> =
        executor.execute { api.myTenantInvitations() }

    suspend fun managed(): ApiResult<List<TenantInvitationDto>> =
        executor.execute { api.tenantInvitations() }

    suspend fun get(id: String): ApiResult<TenantInvitationDto> =
        executor.execute { api.tenantInvitation(id) }

    suspend fun create(request: CreateTenantInvitationRequest): ApiResult<TenantInvitationDto> =
        executor.execute { api.createTenantInvitation(request) }

    suspend fun accept(id: String): ApiResult<Unit> =
        executor.execute { api.acceptTenantInvitation(id) }.toUnit()

    suspend fun decline(id: String): ApiResult<Unit> =
        executor.execute { api.declineTenantInvitation(id) }.toUnit()

    suspend fun approve(id: String): ApiResult<Unit> =
        executor.execute { api.approveTenantInvitation(id) }.toUnit()

    suspend fun reject(id: String): ApiResult<Unit> =
        executor.execute { api.rejectTenantInvitation(id) }.toUnit()

    suspend fun cancel(id: String): ApiResult<Unit> =
        executor.execute { api.cancelTenantInvitation(id) }.toUnit()

    suspend fun resend(id: String): ApiResult<TenantInvitationDto> =
        executor.execute { api.resendTenantInvitation(id) }

    suspend fun onboardingStatus(id: String): ApiResult<OnboardingStatusDto> =
        executor.execute { api.tenantOnboardingStatus(id) }

    suspend fun startDigiLocker(id: String): ApiResult<DigiLockerStartDto> =
        executor.execute { api.startDigiLocker(id) }

    suspend fun verification(id: String): ApiResult<InvitationVerificationDto?> =
        executor.execute { api.invitationVerification(id) }

    suspend fun manualReview(id: String, approved: Boolean, reason: String): ApiResult<Unit> =
        executor.execute {
            api.manualKycReview(
                id,
                ManualKycReviewRequest(
                    decision = if (approved) "approved" else "rejected",
                    reason = reason.trim(),
                ),
            )
        }.toUnit()
}
