package com.example.data.network

import android.content.Context
import android.util.Log
import com.example.data.network.api.MasterPrinterApiService
import com.example.data.network.interceptor.ErrorHandlingInterceptor
import com.example.data.network.interceptor.SecureHeaderInterceptor
import com.example.data.session.UserSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Enterprise Retrofit & OkHttp client manager for Master Printer.
 *
 * Configures:
 * 1. [SecureHeaderInterceptor] for automated Bearer token injection, device fingerprinting,
 *    distributed tracing IDs, and API key management.
 * 2. [ErrorHandlingInterceptor] for transport timeout management, structured error envelope parsing,
 *    and HTTP 401 unauthorized session eviction.
 * 3. [HttpLoggingInterceptor] for audit logging in diagnostic/development builds.
 * 4. High-performance connection pooling, JSON parsing via Moshi Kotlin, and dynamic Base URL switching.
 */
class RetrofitClient private constructor(
    private val context: Context,
    initialBaseUrl: String = DEFAULT_BASE_URL,
    private val userSessionManager: UserSessionManager = UserSessionManager.getInstance(context)
) {

    @Volatile
    private var currentBaseUrl: String = sanitizeUrl(initialBaseUrl)

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG_HTTP, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val secureHeaderInterceptor = SecureHeaderInterceptor(
        context = context,
        userSessionManager = userSessionManager,
        apiKeyProvider = {
            // Read active tenant API key if stored in session or settings
            val prefs = context.getSharedPreferences("master_printer_api_settings", Context.MODE_PRIVATE)
            prefs.getString("custom_api_key", null)
        }
    )

    val errorHandlingInterceptor = ErrorHandlingInterceptor(
        moshi = moshi,
        onUnauthorized = {
            Log.w(TAG, "Server returned HTTP 401 Unauthorized: Evicting stale authentication credentials.")
            // Mark session as expired in user session manager
            userSessionManager.clearSession()
        }
    )

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(secureHeaderInterceptor)
        .addInterceptor(errorHandlingInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Volatile
    private var retrofit: Retrofit = buildRetrofit(currentBaseUrl)

    @Volatile
    var apiService: MasterPrinterApiService = retrofit.create(MasterPrinterApiService::class.java)
        private set

    /**
     * Updates the base URL dynamically (e.g. for staging, cloud backend, or custom self-hosted kiosk server).
     */
    @Synchronized
    fun updateBaseUrl(newBaseUrl: String) {
        val sanitized = sanitizeUrl(newBaseUrl)
        if (sanitized != currentBaseUrl) {
            currentBaseUrl = sanitized
            retrofit = buildRetrofit(currentBaseUrl)
            apiService = retrofit.create(MasterPrinterApiService::class.java)
            Log.i(TAG, "Production API Base URL updated to: $currentBaseUrl")
        }
    }

    fun getBaseUrl(): String = currentBaseUrl

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    companion object {
        private const val TAG = "RetrofitClient"
        private const val TAG_HTTP = "MasterPrinterApi"

        const val DEFAULT_BASE_URL = "https://api.masterprinter.express/"
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 30L

        @Volatile
        private var INSTANCE: RetrofitClient? = null

        fun getInstance(context: Context): RetrofitClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RetrofitClient(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun sanitizeUrl(url: String): String {
            val trimmed = url.trim()
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }
}
