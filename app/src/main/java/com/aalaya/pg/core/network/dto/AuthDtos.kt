package com.aalaya.pg.core.network.dto

import kotlinx.serialization.Serializable

// Request/response bodies for the /auth/* and /orgs endpoints.
// Property names are camelCase; the Json instance applies snake_case globally.

@Serializable
data class ProvisionRequest(
    val name: String,
    val ownerEmail: String,
    val ownerPhone: String? = null,
    val ownerName: String,
)

@Serializable
data class RequestEmailOtpRequest(
    val email: String,
)

@Serializable
data class VerifyEmailOtpRequest(
    val email: String,
    val code: String,
)

@Serializable
data class VerifyResponse(
    val accessToken: String,
    val refreshToken: String,
    val isVerified: Boolean = true,
    val isNewDevice: Boolean = false,
)

@Serializable
data class TokensResponse(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class AccessResponse(
    val accessToken: String,
    // Rotating refresh: the backend now returns a NEW refresh token on every
    // refresh; the old one is revoked server-side. Optional for compatibility.
    val refreshToken: String? = null,
)

@Serializable
data class VerifyIdentityRequest(
    val method: String? = null,
    val providerName: String? = null,
    val providerRef: String,
    val phone: String? = null,
    val emergencyContact: String? = null,
    val country: String? = null,
)

@Serializable
data class VerifyIdentityResponse(
    val status: String,
    val reason: String? = null,
)

@Serializable
data class MeResponse(
    val userId: String,
    val orgId: String,
    val role: String,
    val branchId: String? = null,
    val fullName: String? = null,
    val verified: Boolean = true,
)

@Serializable
data class MembershipDto(
    val membershipId: String,
    val organizationId: String,
    val organizationName: String,
    val role: String,
    val branchId: String? = null,
)

@Serializable
data class SwitchOrganizationRequest(
    val organizationId: String,
    val role: String,
    val refreshToken: String,
)
