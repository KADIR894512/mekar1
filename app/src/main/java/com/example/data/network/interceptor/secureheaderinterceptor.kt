package com.example.data.network.interceptor

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.session.UserSessionManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.UUID

/**
 * OkHttp Interceptor that injects enterprise-grade security headers, authorization tokens,
 * device fingerprinting, and distributed tracing headers into every outgoing request.
 */
class SecureHeaderInterceptor(
    private val tokenProvider: () -> String?,
    private val apiKeyProvider: () -> String? = { null },
    private val deviceIdProvider: () -> String = { "android-device-" + UUID.randomUUID().toString().take(8) },
    private val appVersion: String = "1.0.0"
) : Interceptor {

    constructor(
        context: Context,
        userSessionManager: UserSessionManager = UserSessionManager.getInstance(context),
        apiKeyProvider: () -> String? = { null }
    ) : this(
        tokenProvider = { userSessionManager.getAuthToken() },
        apiKeyProvider = apiKeyProvider,
        deviceIdProvider = { extractDeviceId(context) },
        appVersion = extractAppVersion(context)
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 1. Check for No-Auth bypass flag
        val isNoAuth = originalRequest.header(HEADER_NO_AUTH)?.equals("true", ignoreCase = true) == true
        if (isNoAuth) {
            requestBuilder.removeHeader(HEADER_NO_AUTH)
        }

        // 2. Authorization Header Injection
        if (!isNoAuth && originalRequest.header(HEADER_AUTHORIZATION).isNullOrBlank()) {
            val authToken = tokenProvider()
            if (!authToken.isNullOrBlank()) {
                val formattedToken = if (authToken.startsWith("Bearer ", ignoreCase = true)) {
                    authToken
                } else {
                    "Bearer $authToken"
                }
                requestBuilder.header(HEADER_AUTHORIZATION, formattedToken)
            }
        }

        // 3. API Key Injection (if configured for tenant or gateway security)
        val apiKey = apiKeyProvider()
        if (!apiKey.isNullOrBlank() && originalRequest.header(HEADER_API_KEY).isNullOrBlank()) {
            requestBuilder.header(HEADER_API_KEY, apiKey)
        }

        // 4. Standard Content Negotiation Headers
        if (originalRequest.header(HEADER_ACCEPT).isNullOrBlank()) {
            requestBuilder.header(HEADER_ACCEPT, MIME_APPLICATION_JSON)
        }

        // Add Content-Type if request has a body and not already present
        if (originalRequest.body != null && originalRequest.header(HEADER_CONTENT_TYPE).isNullOrBlank()) {
            requestBuilder.header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        }

        // 5. Distributed Tracing & Idempotency Headers
        val requestId = originalRequest.header(HEADER_REQUEST_ID) ?: UUID.randomUUID().toString()
        requestBuilder.header(HEADER_REQUEST_ID, requestId)
        requestBuilder.header(HEADER_CORRELATION_ID, requestId)
        requestBuilder.header(HEADER_CLIENT_TIMESTAMP, System.currentTimeMillis().toString())

        // 6. Device & Client Telemetry Headers
        requestBuilder.header(HEADER_APP_PLATFORM, "Android")
        requestBuilder.header(HEADER_APP_VERSION, appVersion)
        requestBuilder.header(HEADER_DEVICE_ID, deviceIdProvider())
        requestBuilder.header(HEADER_DEVICE_MODEL, "${Build.MANUFACTURER} ${Build.MODEL}")
        requestBuilder.header(HEADER_OS_VERSION, "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")

        val modifiedRequest = requestBuilder.build()
        return chain.proceed(modifiedRequest)
    }

    companion object {
        private const val TAG = "SecureHeaderInterceptor"

        // Standard HTTP Headers
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val MIME_APPLICATION_JSON = "application/json"

        // Custom Security & Telemetry Headers
        const val HEADER_NO_AUTH = "X-No-Auth"
        const val HEADER_API_KEY = "X-Api-Key"
        const val HEADER_REQUEST_ID = "X-Request-ID"
        const val HEADER_CORRELATION_ID = "X-Correlation-ID"
        const val HEADER_CLIENT_TIMESTAMP = "X-Client-Timestamp"
        const val HEADER_APP_PLATFORM = "X-App-Platform"
        const val HEADER_APP_VERSION = "X-App-Version"
        const val HEADER_DEVICE_ID = "X-Device-ID"
        const val HEADER_DEVICE_MODEL = "X-Device-Model"
        const val HEADER_OS_VERSION = "X-OS-Version"
    }
}

private fun extractDeviceId(context: Context): String {
    return try {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            ?: ("dev-" + Build.MODEL.hashCode().toString(16))
    } catch (e: Throwable) {
        "dev-" + Build.MODEL.hashCode().toString(16)
    }
}

private fun extractAppVersion(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (e: Throwable) {
        "1.0.0"
    }
}
