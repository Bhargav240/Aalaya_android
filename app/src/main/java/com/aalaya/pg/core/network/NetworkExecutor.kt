package com.aalaya.pg.core.network

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.AppFailure
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The backend's RFC-7807 problem+json error body. Internal test visibility
 * so ProblemDtoTest can round-trip it. */
@Serializable
data class ProblemDto(
    val title: String? = null,
    val detail: String? = null,
    val status: Int? = null,
)

/**
 * Wraps every network call: turns Retrofit exceptions into a typed [AppFailure],
 * parsing the backend's problem+json so the user sees the real validation
 * message ("email already in use") rather than a raw HTTP code.
 */
@Singleton
class NetworkExecutor @Inject constructor(private val json: Json) {

    suspend fun <T> execute(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        ApiResult.Failure(mapHttp(e))
    } catch (e: IOException) {
        ApiResult.Failure(AppFailure.Network())
    } catch (e: CancellationException) {
        // Must propagate, not be reported as a failure -- swallowing it here
        // breaks structured concurrency (the parent job never learns this
        // coroutine was cancelled, e.g. because the screen that requested it
        // is already gone).
        throw e
    } catch (e: Exception) {
        ApiResult.Failure(AppFailure.Unknown(e.message ?: "Unexpected error."))
    }

    private fun mapHttp(e: HttpException): AppFailure {
        val problem = runCatching {
            e.response()?.errorBody()?.string()?.let { json.decodeFromString<ProblemDto>(it) }
        }.getOrNull()
        // Prefer the RFC-7807 `detail` (specific validation messages like
        // "phone number already registered") over the generic `title`.
        val message = problem?.detail ?: problem?.title
        return when (e.code()) {
            400 -> AppFailure.Validation(message ?: "Invalid request.")
            401 -> AppFailure.Unauthorized()
            403 -> AppFailure.Forbidden()
            404 -> AppFailure.NotFound()
            409 -> AppFailure.Conflict(message ?: "Conflict.")
            in 500..599 -> AppFailure.Server()
            else -> AppFailure.Unknown(message ?: "Error ${e.code()}.")
        }
    }
}
