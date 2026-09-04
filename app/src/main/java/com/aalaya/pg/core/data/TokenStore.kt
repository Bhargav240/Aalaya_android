package com.aalaya.pg.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aalaya.pg.core.domain.model.Role
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "hamsa_auth")

data class AuthBootstrap(
    val accessToken: String?,
    val refreshToken: String?,
    val identity: UserSession?,
)

/**
 * Persists the access + refresh tokens. Backed by DataStore (async, corruption-safe).
 * The tokens are opaque JWTs the backend issues; we never decode them client-side
 * beyond what the /me endpoint returns.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val orgIdKey = stringPreferencesKey("org_id")
    private val roleKey = stringPreferencesKey("role")
    private val branchIdKey = stringPreferencesKey("branch_id")
    private val fullNameKey = stringPreferencesKey("full_name")
    private val verifiedKey = stringPreferencesKey("verified")
    private val deviceIdKey = stringPreferencesKey("device_id")

    // In-memory snapshot of the access token so the synchronous OkHttp
    // interceptor never has to runBlocking a DataStore read on request hot
    // paths. Kept in step by every writer below and primed once per process.
    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var cachePrimed = false

    /** Synchronous snapshot for the OkHttp interceptor. May be stale-by-one only
     * during the initial prime or a concurrent rotation — safe because requests
     * issued before priming simply go out unauthenticated and get retried. */
    fun peekAccessToken(): String? = cachedAccessToken

    /** Hydrate access, refresh token, and identity in a single DataStore read for fast startup. */
    suspend fun hydrateBootstrap(): AuthBootstrap {
        val prefs = context.authDataStore.data.first()
        val access = prefs[accessKey]
        val refresh = prefs[refreshKey]
        cachedAccessToken = access
        cachePrimed = true
        val userId = prefs[userIdKey]
        val orgId = prefs[orgIdKey]
        val roleWire = prefs[roleKey]
        val identity = if (userId != null && orgId != null && roleWire != null) {
            Role.fromWire(roleWire)?.let { role ->
                UserSession(
                    userId = userId,
                    orgId = orgId,
                    role = role,
                    branchId = prefs[branchIdKey],
                    fullName = prefs[fullNameKey],
                    verified = prefs[verifiedKey] != "false",
                )
            }
        } else null
        return AuthBootstrap(
            accessToken = access,
            refreshToken = refresh,
            identity = identity,
        )
    }

    /** Prime the in-memory snapshot from disk once per process. Idempotent. */
    suspend fun primeAccessToken() {
        if (cachePrimed) return
        cachedAccessToken = context.authDataStore.data.first()[accessKey]
        cachePrimed = true
    }

    suspend fun accessToken(): String? =
        cachedAccessToken?.takeIf { cachePrimed }
            ?: context.authDataStore.data.first()[accessKey].also { cachedAccessToken = it; cachePrimed = true }

    suspend fun refreshToken(): String? = context.authDataStore.data.first()[refreshKey]

    suspend fun save(access: String, refresh: String) {
        // A fresh login implies the old one is gone — reset the snapshot to the
        // brand-new token rather than trusting whatever was primed for the old user.
        cachedAccessToken = access
        cachePrimed = true
        context.authDataStore.edit {
            it[accessKey] = access
            it[refreshKey] = refresh
        }
    }

    suspend fun updateAccess(access: String) {
        cachedAccessToken = access
        cachePrimed = true
        context.authDataStore.edit { it[accessKey] = access }
    }

    suspend fun saveIdentity(session: UserSession) {
        context.authDataStore.edit {
            it[userIdKey] = session.userId
            it[orgIdKey] = session.orgId
            it[roleKey] = session.role.wire
            session.branchId?.let { branchId -> it[branchIdKey] = branchId }
                ?: it.remove(branchIdKey)
            session.fullName?.let { name -> it[fullNameKey] = name }
                ?: it.remove(fullNameKey)
            it[verifiedKey] = if (session.verified) "true" else "false"
        }
    }

    /** Restore the cached identity (used for offline-friendly cold start). */
    suspend fun identity(): UserSession? {
        val prefs = context.authDataStore.data.first()
        val userId = prefs[userIdKey] ?: return null
        val orgId = prefs[orgIdKey] ?: return null
        val role = Role.fromWire(prefs[roleKey]) ?: return null
        return UserSession(
            userId = userId,
            orgId = orgId,
            role = role,
            branchId = prefs[branchIdKey],
            fullName = prefs[fullNameKey],
            verified = prefs[verifiedKey] != "false",
        )
    }

    suspend fun clear() {
        cachedAccessToken = null
        cachePrimed = true // "primed to empty" — a later accessToken() won't resurrect the old value from disk mid-clear
        context.authDataStore.edit { it.clear() }
    }

    /**
     * Stable install-level UUID sent with OTP verify so the backend can flag
     * sign-ins from a new device. Generated once and persisted; clearing auth
     * data keeps it (it identifies the device, not the session).
     */
    suspend fun deviceId(): String {
        context.authDataStore.data.first()[deviceIdKey]?.let { return it }
        val id = java.util.UUID.randomUUID().toString()
        context.authDataStore.edit { it[deviceIdKey] = id }
        return id
    }
}
