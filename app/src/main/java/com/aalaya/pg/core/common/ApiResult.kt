package com.aalaya.pg.core.common

/** A tiny Result type so ViewModels never touch Retrofit/OkHttp types directly. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppFailure) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(block: (AppFailure) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) block(error)
    return this
}

/** Normalised failure taxonomy, decoupled from transport. */
sealed class AppFailure(open val message: String) {
    data class Network(override val message: String = "No connection. Check your network.") : AppFailure(message)
    data class Unauthorized(override val message: String = "Your session expired. Please sign in again.") : AppFailure(message)
    data class Forbidden(override val message: String = "You don't have permission to do that.") : AppFailure(message)
    data class NotFound(override val message: String = "Not found.") : AppFailure(message)
    data class Validation(override val message: String) : AppFailure(message)
    data class Conflict(override val message: String) : AppFailure(message)
    data class Server(override val message: String = "Something went wrong on the server.") : AppFailure(message)
    data class Unknown(override val message: String = "Unexpected error.") : AppFailure(message)
}
