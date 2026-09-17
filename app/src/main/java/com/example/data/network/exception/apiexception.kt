package com.example.data.network.exception

import java.io.IOException

/**
 * Base sealed exception for all network, API, and HTTP communication failures
 * across the Master Printer application.
 */
sealed class ApiException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null,
    val errorCode: String? = null,
    val errorBody: String? = null
) : IOException(message, cause) {

    val isClientError: Boolean
        get() = statusCode != null && statusCode in 400..499

    val isServerError: Boolean
        get() = statusCode != null && statusCode in 500..599
}

/**
 * Thrown when the physical device has no internet connection, DNS resolution fails,
 * or host is unreachable.
 */
class NetworkUnavailableException(
    message: String = "No internet connection available. Please check your Wi-Fi or cellular network.",
    cause: Throwable? = null
) : ApiException(
    message = message,
    cause = cause,
    statusCode = null,
    errorCode = "ERR_NETWORK_UNAVAILABLE"
)

/**
 * Thrown when an HTTP request times out (connect, read, or write timeout).
 */
class NetworkTimeoutException(
    message: String = "Network connection timed out while contacting production server.",
    cause: Throwable? = null
) : ApiException(
    message = message,
    cause = cause,
    statusCode = 408,
    errorCode = "ERR_REQUEST_TIMEOUT"
)

/**
 * Thrown on HTTP 401 Unauthorized (expired JWT token, invalid credentials, or session terminated).
 */
class UnauthorizedException(
    message: String = "Authentication session expired or invalid credentials. Please log in again.",
    errorCode: String? = "ERR_UNAUTHORIZED",
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = 401,
    errorCode = errorCode,
    errorBody = errorBody
)

/**
 * Thrown on HTTP 403 Forbidden (authenticated user lacks necessary role or ACL permissions).
 */
class ForbiddenException(
    message: String = "Access denied: You do not have sufficient permissions to access this resource.",
    errorCode: String? = "ERR_FORBIDDEN",
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = 403,
    errorCode = errorCode,
    errorBody = errorBody
)

/**
 * Thrown on HTTP 404 Resource Not Found.
 */
class NotFoundException(
    message: String = "The requested resource was not found on the server.",
    errorCode: String? = "ERR_NOT_FOUND",
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = 404,
    errorCode = errorCode,
    errorBody = errorBody
)

/**
 * Thrown on HTTP 409 Conflict (e.g. duplicate username, stale order concurrency, printer busy).
 */
class ConflictException(
    message: String = "A resource conflict occurred on the server.",
    errorCode: String? = "ERR_CONFLICT",
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = 409,
    errorCode = errorCode,
    errorBody = errorBody
)

/**
 * Thrown on HTTP 422 Unprocessable Entity or HTTP 400 Bad Request with field-level validation errors.
 */
class ValidationException(
    message: String = "Validation failed for the submitted payload.",
    errorCode: String? = "ERR_VALIDATION_FAILED",
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = 422,
    errorCode = errorCode,
    errorBody = errorBody
)

/**
 * Thrown on HTTP 429 Too Many Requests (Rate limit exceeded).
 */
class RateLimitExceededException(
    message: String = "Rate limit exceeded. Please wait before retrying.",
    val retryAfterSeconds: Long? = null,
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = 429,
    errorCode = "ERR_RATE_LIMIT_EXCEEDED",
    errorBody = errorBody
)

/**
 * Thrown on HTTP 500, 502, 503, 504 server errors.
 */
class ServerException(
    message: String = "Production server encountered an internal error. Please try again later.",
    statusCode: Int = 500,
    errorCode: String? = "ERR_SERVER_ERROR",
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = statusCode,
    errorCode = errorCode,
    errorBody = errorBody
)

/**
 * Thrown when an unmapped HTTP status code or payload is received.
 */
class UnknownApiException(
    message: String,
    statusCode: Int,
    errorCode: String? = null,
    errorBody: String? = null
) : ApiException(
    message = message,
    statusCode = statusCode,
    errorCode = errorCode,
    errorBody = errorBody
)
