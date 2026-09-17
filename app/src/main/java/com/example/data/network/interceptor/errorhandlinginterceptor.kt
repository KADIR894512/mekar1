package com.example.data.network.interceptor

import android.util.Log
import com.example.data.network.dto.ApiErrorResponse
import com.example.data.network.exception.ApiException
import com.example.data.network.exception.ConflictException
import com.example.data.network.exception.ForbiddenException
import com.example.data.network.exception.NetworkTimeoutException
import com.example.data.network.exception.NetworkUnavailableException
import com.example.data.network.exception.NotFoundException
import com.example.data.network.exception.RateLimitExceededException
import com.example.data.network.exception.ServerException
import com.example.data.network.exception.UnauthorizedException
import com.example.data.network.exception.UnknownApiException
import com.example.data.network.exception.ValidationException
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * OkHttp Interceptor that traps low-level transport/socket exceptions and HTTP error responses,
 * transforms them into strongly-typed domain exceptions [ApiException], and triggers security
 * callbacks upon authentication revocation (HTTP 401).
 */
class ErrorHandlingInterceptor(
    private val moshi: Moshi = Moshi.Builder().build(),
    private val onUnauthorized: (() -> Unit)? = null
) : Interceptor {

    private val errorAdapter = moshi.adapter(ApiErrorResponse::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val response: Response = try {
            chain.proceed(request)
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "HTTP connection/read timeout for: ${request.url}", e)
            throw NetworkTimeoutException("Request to ${request.url.encodedPath} timed out. Please check your connection and try again.", e)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS lookup failed for: ${request.url.host}", e)
            throw NetworkUnavailableException("Unable to reach production server (${request.url.host}). Verify network connectivity.", e)
        } catch (e: ConnectException) {
            Log.e(TAG, "Failed to establish TCP connection to: ${request.url}", e)
            throw NetworkUnavailableException("Failed to connect to production server at ${request.url.host}:${request.url.port}.", e)
        } catch (e: NoRouteToHostException) {
            Log.e(TAG, "No route to host for: ${request.url}", e)
            throw NetworkUnavailableException("No route to production server. Please check your network gateway.", e)
        } catch (e: IOException) {
            if (e is ApiException) throw e
            Log.e(TAG, "Transport IO failure on ${request.url}", e)
            throw NetworkUnavailableException("Network error occurred: ${e.localizedMessage ?: "I/O failure"}", e)
        }

        // Return immediately if HTTP status is 2xx Successful
        if (response.isSuccessful) {
            return response
        }

        // Inspect and parse structured error response
        val statusCode = response.code
        val rawBody: String? = try {
            response.peekBody(MAX_ERROR_PEEK_BYTES).string()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to peek response body", e)
            null
        }

        val parsedError: ApiErrorResponse? = rawBody?.let { json ->
            try {
                errorAdapter.fromJson(json)
            } catch (e: Throwable) {
                null
            }
        }

        val errorMessage = parsedError?.message
            ?.takeIf { it.isNotBlank() }
            ?: (response.message.takeIf { it.isNotBlank() } ?: "HTTP $statusCode error occurred on ${request.url.encodedPath}")
        val errorCode = parsedError?.code

        Log.w(TAG, "HTTP $statusCode from ${request.url.encodedPath}: $errorMessage (code: $errorCode)")

        when (statusCode) {
            401 -> {
                // Trigger token revocation or logout action
                try {
                    onUnauthorized?.invoke()
                } catch (e: Throwable) {
                    Log.e(TAG, "Error in onUnauthorized callback", e)
                }
                throw UnauthorizedException(
                    message = errorMessage,
                    errorCode = errorCode ?: "ERR_UNAUTHORIZED",
                    errorBody = rawBody
                )
            }

            403 -> {
                throw ForbiddenException(
                    message = errorMessage,
                    errorCode = errorCode ?: "ERR_FORBIDDEN",
                    errorBody = rawBody
                )
            }

            404 -> {
                throw NotFoundException(
                    message = errorMessage,
                    errorCode = errorCode ?: "ERR_NOT_FOUND",
                    errorBody = rawBody
                )
            }

            409 -> {
                throw ConflictException(
                    message = errorMessage,
                    errorCode = errorCode ?: "ERR_CONFLICT",
                    errorBody = rawBody
                )
            }

            422 -> {
                throw ValidationException(
                    message = errorMessage,
                    errorCode = errorCode ?: "ERR_VALIDATION_FAILED",
                    fieldErrors = parsedError?.fieldErrors ?: emptyMap(),
                    errorBody = rawBody
                )
            }

            429 -> {
                val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
                throw RateLimitExceededException(
                    message = errorMessage,
                    retryAfterSeconds = retryAfterSeconds,
                    errorBody = rawBody
                )
            }

            in 500..599 -> {
                throw ServerException(
                    message = errorMessage,
                    statusCode = statusCode,
                    errorCode = errorCode ?: "ERR_SERVER_ERROR",
                    errorBody = rawBody
                )
            }

            else -> {
                throw UnknownApiException(
                    message = errorMessage,
                    statusCode = statusCode,
                    errorCode = errorCode,
                    errorBody = rawBody
                )
            }
        }
    }

    companion object {
        private const val TAG = "ErrorHandlingInterceptor"
        private const val MAX_ERROR_PEEK_BYTES = 512 * 1024L // 512 KB
    }
}
