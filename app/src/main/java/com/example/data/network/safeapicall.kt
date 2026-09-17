package com.example.data.network

import android.util.Log
import com.example.data.network.exception.ApiException
import com.example.data.network.exception.UnknownApiException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sealed result wrapper for network and repository operations.
 */
sealed class NetworkResult<out T> {

    data class Success<out T>(
        val data: T,
        val message: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : NetworkResult<T>()

    data class Error(
        val exception: ApiException,
        val message: String = exception.message ?: "An unexpected network error occurred",
        val statusCode: Int? = exception.statusCode,
        val errorCode: String? = exception.errorCode
    ) : NetworkResult<Nothing>()

    data object Loading : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = (this as? Success)?.data ?: defaultValue

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Error) -> Unit): NetworkResult<T> {
        if (this is Error) action(this)
        return this
    }
}

/**
 * Safely executes a suspendable network API call within the designated CoroutineDispatcher,
 * translating all thrown [ApiException] or unexpected exceptions into [NetworkResult].
 */
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> T
): NetworkResult<T> {
    return withContext(dispatcher) {
        try {
            val result = apiCall()
            NetworkResult.Success(result)
        } catch (e: ApiException) {
            Log.w("SafeApiCall", "API call failed with ApiException: ${e.message} (code: ${e.errorCode})")
            NetworkResult.Error(
                exception = e,
                message = e.message ?: "Network API error",
                statusCode = e.statusCode,
                errorCode = e.errorCode
            )
        } catch (e: Throwable) {
            Log.e("SafeApiCall", "Unhandled exception during API call", e)
            val wrapped = UnknownApiException(
                message = e.localizedMessage ?: "An unexpected system error occurred during network operation.",
                statusCode = -1,
                errorCode = "ERR_UNEXPECTED"
            )
            NetworkResult.Error(
                exception = wrapped,
                message = wrapped.message ?: "An unexpected system error occurred.",
                statusCode = null,
                errorCode = wrapped.errorCode
            )
        }
    }
}
