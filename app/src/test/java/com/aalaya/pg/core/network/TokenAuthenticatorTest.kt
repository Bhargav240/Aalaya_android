package com.aalaya.pg.core.network

import com.aalaya.pg.core.data.TokenStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TokenAuthenticatorTest {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `concurrent 401 responses perform one rotating refresh`() = runBlocking {
        val access = AtomicReference("stale-access")
        val refresh = AtomicReference("refresh-one")
        val tokenStore = mockk<TokenStore>()
        coEvery { tokenStore.accessToken() } answers { access.get() }
        coEvery { tokenStore.refreshToken() } answers { refresh.get() }
        coEvery { tokenStore.save(any(), any()) } coAnswers {
            access.set(firstArg())
            refresh.set(secondArg())
        }
        val refreshCalls = AtomicInteger()
        val bareClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                refreshCalls.incrementAndGet()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """{"access_token":"fresh-access","refresh_token":"refresh-two"}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()
        val invalidator = mockk<SessionInvalidator>(relaxed = true)
        val authenticator = TokenAuthenticator(
            tokenStore,
            Json { namingStrategy = JsonNamingStrategy.SnakeCase },
            BaseUrl("http://localhost/"),
            bareClient,
            invalidator,
        )

        val retries = List(8) {
            async(Dispatchers.Default) { authenticator.authenticate(null, unauthorizedResponse()) }
        }.awaitAll()

        assertEquals(1, refreshCalls.get())
        retries.forEach { retry ->
            assertNotNull(retry)
            assertEquals("Bearer fresh-access", retry?.header("Authorization"))
        }
        coVerify(exactly = 1) { tokenStore.save("fresh-access", "refresh-two") }
        verify(exactly = 0) { invalidator.invalidate() }
    }

    private fun unauthorizedResponse(): Response {
        val request = Request.Builder()
            .url("http://localhost/private")
            .header("Authorization", "Bearer stale-access")
            .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }
}
