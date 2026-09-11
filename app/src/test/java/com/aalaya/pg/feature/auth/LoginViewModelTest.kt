package com.aalaya.pg.feature.auth

import com.aalaya.pg.MainDispatcherRule
import com.aalaya.pg.core.common.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    @Test
    fun `email OTP login sends then verifies the exact account`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.requestOtp("Tenant@Example.com", null, null, false) } returns ApiResult.Success(Unit)
        coEvery { repository.verifyOtp("Tenant@Example.com", "123456") } returns
            ApiResult.Success(AuthRepository.AuthOutcome(isVerified = true, isNewDevice = false))
        val viewModel = LoginViewModel(repository)

        viewModel.onEmail("Tenant@Example.com")
        assertTrue(viewModel.state.value.canSubmit)
        viewModel.submit()
        runCurrent()

        assertTrue(viewModel.state.value.otpSent)
        assertEquals(30, viewModel.state.value.resendSecondsLeft)
        viewModel.onOtpCode("123456")
        viewModel.submit()
        runCurrent()

        coVerify(exactly = 1) { repository.requestOtp("Tenant@Example.com", null, null, false) }
        coVerify(exactly = 1) { repository.verifyOtp("Tenant@Example.com", "123456") }
        assertFalse(viewModel.state.value.loading)
        assertFalse(viewModel.state.value.otpSent)
        assertEquals("Tenant@Example.com", viewModel.state.value.email)
    }
}
