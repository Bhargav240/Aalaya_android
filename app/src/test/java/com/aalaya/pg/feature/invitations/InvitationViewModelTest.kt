package com.aalaya.pg.feature.invitations

import androidx.lifecycle.SavedStateHandle
import com.aalaya.pg.MainDispatcherRule
import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.SnackbarManager
import com.aalaya.pg.core.network.dto.OnboardingStatusDto
import com.aalaya.pg.core.network.dto.TenantInvitationDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvitationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `accept action refreshes onboarding status`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = mockk<InvitationRepository>()
        val invitation = invitation(status = "invited")
        coEvery { repository.get(INVITATION_ID) } returns ApiResult.Success(invitation)
        coEvery { repository.onboardingStatus(INVITATION_ID) } returns ApiResult.Success(
            OnboardingStatusDto(INVITATION_ID, "approval_pending", false, "wait_for_approval"),
        )
        coEvery { repository.verification(INVITATION_ID) } returns ApiResult.Success(null)
        coEvery { repository.accept(INVITATION_ID) } returns ApiResult.Success(Unit)
        val viewModel = InvitationDetailViewModel(
            SavedStateHandle(mapOf("invitationId" to INVITATION_ID)),
            repository,
            SnackbarManager(),
        )
        advanceUntilIdle()

        viewModel.accept()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.accept(INVITATION_ID) }
        coVerify(atLeast = 2) { repository.onboardingStatus(INVITATION_ID) }
        assertEquals("approval_pending", viewModel.state.value.onboarding?.status)
        assertEquals(null, viewModel.state.value.actionInProgress)
    }

    private fun invitation(status: String) = TenantInvitationDto(
        id = INVITATION_ID,
        orgId = "org-a",
        branchId = "branch-a",
        email = "tenant@example.com",
        fullName = "Tenant Person",
        requireIdentityVerification = false,
        status = status,
        expiresAt = "2026-08-20T00:00:00Z",
        lastSentAt = "2026-08-12T00:00:00Z",
    )

    private companion object {
        const val INVITATION_ID = "10000000-0000-0000-0000-000000000001"
    }
}
