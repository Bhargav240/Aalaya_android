package com.aalaya.pg.core.network

import com.aalaya.pg.core.data.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the bearer access token to outgoing requests. Anonymous endpoints
 * (login, refresh, provision, discovery) simply carry an unused header — the
 * backend ignores it when a route needs no auth.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Never overwrite an explicitly-set auth header (e.g. refresh flow).
        if (request.header("Authorization") != null) return chain.proceed(request)

        // Non-blocking snapshot: avoids runBlocking on the OkHttp dispatcher for
        // every request. Primed at bootstrap; a token saved after this intercept
        // arrives on the retry that the authenticator triggers on 401.
        val token = tokenStore.peekAccessToken() ?: runBlocking { tokenStore.accessToken() }
        val out = if (token.isNullOrEmpty()) {
            request
        } else {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(out)
    }
}
