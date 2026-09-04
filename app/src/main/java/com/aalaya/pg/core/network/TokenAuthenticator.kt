package com.aalaya.pg.core.network

import com.aalaya.pg.core.data.TokenStore
import com.aalaya.pg.core.network.dto.AccessResponse
import com.aalaya.pg.core.network.dto.RefreshRequest
import com.aalaya.pg.di.BareClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On a 401, transparently exchanges the refresh token for a fresh access token
 * and retries the original request once. If refresh fails, the session is cleared
 * and null is returned so the UI falls back to the sign-in gate.
 *
 * The refresh request goes through a *bare* client (no authenticator), so a
 * persistently-unauthorized refresh can never recurse into itself.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val json: Json,
    private val baseUrl: BaseUrl,
    @BareClient private val bareClient: OkHttpClient,
    private val sessionInvalidator: SessionInvalidator,
) : okhttp3.Authenticator {

    /**
     * Serialises refreshes. Several screens can 401 at the same moment — the
     * dashboard, occupancy and payments all reload when the active branch
     * changes. Because the backend *rotates* refresh tokens, two concurrent
     * refreshes mean the second one presents a token the server just revoked,
     * gets a 401, and signs the user out mid-session. One at a time, then.
     */
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Give up after one retry to avoid infinite 401 loops.
        if (responseCount(response) >= 2) return null

        val staleAccess = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(refreshLock) {
            // Another thread may have refreshed while this one waited on the
            // lock. If so the stored token is already good — retry with it
            // rather than burning (and revoking) the refresh token again.
            val current = runBlocking { tokenStore.accessToken() }
            if (!current.isNullOrEmpty() && current != staleAccess) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val refresh = runBlocking { tokenStore.refreshToken() } ?: return null

            val refreshed = runCatching { refreshAccess(refresh) }.getOrNull()
            if (refreshed == null) {
                runBlocking { tokenStore.clear() }
                sessionInvalidator.invalidate()
                return null
            }
            runBlocking {
                // Rotating refresh: persist the fresh refresh token too, or the
                // next refresh would present an already-revoked token.
                val newRefresh = refreshed.refreshToken
                if (newRefresh != null) {
                    tokenStore.save(refreshed.accessToken, newRefresh)
                } else {
                    tokenStore.updateAccess(refreshed.accessToken)
                }
            }
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun refreshAccess(refreshToken: String): AccessResponse {
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(baseUrl.base + "auth/refresh")
            .post(body)
            .build()
        bareClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("refresh failed: ${resp.code}")
            val payload = resp.body?.string().orEmpty()
            return json.decodeFromString(AccessResponse.serializer(), payload)
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

/** Wraps the API base URL so the authenticator can build the refresh request. */
class BaseUrl(val base: String)

/** Lets the authenticator signal the app to drop to the sign-in screen. */
interface SessionInvalidator {
    fun invalidate()
}
