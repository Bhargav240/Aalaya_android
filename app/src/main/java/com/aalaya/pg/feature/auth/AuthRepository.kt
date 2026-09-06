package com.aalaya.pg.feature.auth

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.AppFailure
import com.aalaya.pg.core.data.SessionManager
import com.aalaya.pg.core.data.TokenStore
import com.aalaya.pg.core.data.UserSession
import com.aalaya.pg.core.data.ApiCache
import com.aalaya.pg.core.domain.model.Role
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.RequestEmailOtpRequest
import com.aalaya.pg.core.network.dto.MembershipDto
import com.aalaya.pg.core.network.dto.SwitchOrganizationRequest
import com.aalaya.pg.core.network.dto.VerifyEmailOtpRequest
import com.aalaya.pg.core.network.dto.VerifyResponse
import com.aalaya.pg.core.network.dto.VerifyIdentityRequest
import com.aalaya.pg.core.network.dto.VerifyIdentityResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the credential exchange. After obtaining tokens it saves them, resolves
 * the caller's identity via /me, and promotes the SessionManager to Authenticated.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
    private val tokenStore: TokenStore,
    private val session: SessionManager,
    private val cache: ApiCache,
) {
    /** Outcome of a successful OTP verify — tokens plus the account/device flags. */
    data class AuthOutcome(
        val isVerified: Boolean,
        val isNewDevice: Boolean,
    )

    // Send exactly the digits `canSubmit` validated -- trimming whitespace
    // alone would still forward stray punctuation/pasted garbage the UI never
    // actually checked.
    private fun normalizedEmail(email: String) = email.trim().lowercase()

    suspend fun requestOtp(
        email: String,
        fullName: String? = null,
        phone: String? = null,
    ): ApiResult<Unit> =
        when (val r = executor.execute {
            api.requestEmailOtp(
                RequestEmailOtpRequest(
                    email = normalizedEmail(email),
                    fullName = fullName?.trim()?.takeIf { it.isNotEmpty() },
                    phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> r
        }

    /**
     * Verify the one-time code. Along with the tokens the backend reports
     * whether the account is [AuthOutcome.isVerified] and whether this sign-in
     * is on a previously-unseen device ([AuthOutcome.isNewDevice]).
     */
    suspend fun verifyOtp(
        email: String,
        code: String,
        fullName: String? = null,
        phone: String? = null,
    ): ApiResult<AuthOutcome> =
        when (
            val r = executor.execute {
                api.verifyEmailOtp(
                    VerifyEmailOtpRequest(
                        email = normalizedEmail(email),
                        code = code.trim(),
                        fullName = fullName?.trim()?.takeIf { it.isNotEmpty() },
                        phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
            }
        ) {
            is ApiResult.Success -> when (val done = completeSignIn(r.data)) {
                is ApiResult.Success -> ApiResult.Success(
                    AuthOutcome(isVerified = r.data.isVerified, isNewDevice = r.data.isNewDevice),
                )
                is ApiResult.Failure -> done
            }
            is ApiResult.Failure -> r.rejectingCredentialsAs("Incorrect or expired code.")
        }

    /**
     * A 401 here means the submitted credentials were rejected, not that a
     * prior session expired — NetworkExecutor can't tell the two apart, so
     * the credential-exchange calls remap it themselves.
     */
    private fun <T> ApiResult.Failure.rejectingCredentialsAs(message: String): ApiResult<T> =
        if (error is AppFailure.Unauthorized) ApiResult.Failure(AppFailure.Validation(message)) else this

    /**
     * Dev/bootstrap: create an org + owner. Requires the server provisioning
     * key. The app never calls this in production — the owner signs in with
     * phone+OTP afterward — so this is a no-op stub keeping the surface safe.
     */
    suspend fun provisionOrg(
        name: String,
        ownerEmail: String,
        ownerName: String,
        provisioningKey: String,
        ownerPhone: String? = null,
    ): ApiResult<Unit> =
        ApiResult.Failure(AppFailure.Validation("Org provisioning is a backend bootstrap step — not available in-app."))

    private suspend fun completeSignIn(tokens: VerifyResponse): ApiResult<Unit> {
        // Cached private data must never survive a switch to a different user.
        cache.clear()
        // Persist tokens first so the interceptor can authorize the /me call.
        tokenStore.save(tokens.accessToken, tokens.refreshToken)
        return when (val me = executor.execute { api.me() }) {
            is ApiResult.Success -> {
                val role = Role.fromWire(me.data.role) ?: Role.Tenant
                session.onAuthenticated(
                    tokens.accessToken,
                    tokens.refreshToken,
                    UserSession(
                        userId = me.data.userId,
                        orgId = me.data.orgId,
                        role = role,
                        branchId = me.data.branchId,
                        fullName = me.data.fullName,
                        // Staff never verify; tenants must complete it once.
                        verified = if (role == Role.Tenant) me.data.verified else true,
                    ),
                )
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                // Roll back everything we persisted so no stale branch/user
                // scope leaks into the next login attempt (A5).
                session.signOut()
                me
            }
        }
    }

    /**
     * First-login identity verification (PRD flow 1). On a "verified" outcome
     * the session is promoted so the gate in AppRoot unlocks the main app.
     */
    suspend fun verifyIdentity(
        providerRef: String,
        phone: String?,
        emergencyContact: String?,
    ): ApiResult<VerifyIdentityResponse> {
        val result = executor.execute {
            api.verifyIdentity(
                VerifyIdentityRequest(
                    providerRef = providerRef.trim(),
                    phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                    emergencyContact = emergencyContact?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
        if (result is ApiResult.Success && result.data.status == "verified") {
            session.onVerified()
        }
        return result
    }

    suspend fun signOut() {
        // Best-effort server-side revocation of all refresh tokens; local
        // sign-out always proceeds even if the network call fails.
        runCatching { executor.execute { api.logout() } }
        cache.clear()
        session.signOut()
    }

    suspend fun memberships(): ApiResult<List<MembershipDto>> =
        executor.execute { api.memberships() }

    suspend fun switchOrganization(membership: MembershipDto): ApiResult<Unit> {
        val refreshToken = tokenStore.refreshToken()
            ?: return ApiResult.Failure(AppFailure.Unauthorized())
        return when (
            val switched = executor.execute {
                api.switchOrganization(
                    SwitchOrganizationRequest(
                        organizationId = membership.organizationId,
                        role = membership.role,
                        refreshToken = refreshToken,
                    ),
                )
            }
        ) {
            is ApiResult.Failure -> switched
            is ApiResult.Success -> {
                tokenStore.save(switched.data.accessToken, switched.data.refreshToken)
                when (val me = executor.execute { api.me() }) {
                    is ApiResult.Failure -> {
                        session.signOut()
                        me
                    }
                    is ApiResult.Success -> {
                        val role = Role.fromWire(me.data.role)
                            ?: return ApiResult.Failure(AppFailure.Validation("Unsupported account role."))
                        session.onAuthenticated(
                            switched.data.accessToken,
                            switched.data.refreshToken,
                            UserSession(
                                userId = me.data.userId,
                                orgId = me.data.orgId,
                                role = role,
                                branchId = me.data.branchId,
                                fullName = me.data.fullName,
                                verified = me.data.verified,
                            ),
                        )
                        ApiResult.Success(Unit)
                    }
                }
            }
        }
    }
}
