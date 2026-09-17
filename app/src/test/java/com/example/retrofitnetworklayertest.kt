package com.example

import com.example.data.network.NetworkResult
import com.example.data.network.dto.ApiErrorResponse
import com.example.data.network.dto.ApiResponse
import com.example.data.network.dto.LoginResponseDto
import com.example.data.network.dto.OrderResponseDto
import com.example.data.network.exception.ConflictException
import com.example.data.network.exception.ForbiddenException
import com.example.data.network.exception.NotFoundException
import com.example.data.network.exception.RateLimitExceededException
import com.example.data.network.exception.ServerException
import com.example.data.network.exception.UnauthorizedException
import com.example.data.network.exception.ValidationException
import com.example.data.network.interceptor.ErrorHandlingInterceptor
import com.example.data.network.interceptor.SecureHeaderInterceptor
import com.example.data.network.safeApiCall
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RetrofitNetworkLayerTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Helper fake interceptor chain
    private fun createFakeChain(
        request: Request,
        responseBuilder: (Request) -> Response = { req ->
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }
    ): Interceptor.Chain {
        return object : Interceptor.Chain {
            private var currentRequest = request
            override fun request(): Request = currentRequest
            override fun proceed(request: Request): Response {
                currentRequest = request
                return responseBuilder(request)
            }
            override fun connection() = null
            override fun call(): okhttp3.Call = throw NotImplementedError()
            override fun connectTimeoutMillis() = 30000
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 30000
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 30000
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
    }

    // =========================================================================
    // 1. SecureHeaderInterceptor Tests
    // =========================================================================

    @Test
    fun testSecureHeaderInterceptor_injectsBearerAuthAndTelemetry() {
        val testToken = "test-jwt-token-xyz-123"
        val testApiKey = "kiosk-tenant-api-key"
        val testDeviceId = "hw-device-kiosk-99"

        val interceptor = SecureHeaderInterceptor(
            tokenProvider = { testToken },
            apiKeyProvider = { testApiKey },
            deviceIdProvider = { testDeviceId },
            appVersion = "2.4.0"
        )

        val originalRequest = Request.Builder()
            .url("https://api.masterprinter.express/api/v1/orders")
            .build()

        var processedRequest: Request? = null
        val chain = createFakeChain(originalRequest) { req ->
            processedRequest = req
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        interceptor.intercept(chain)

        assertNotNull(processedRequest)
        assertEquals("Bearer $testToken", processedRequest?.header("Authorization"))
        assertEquals(testApiKey, processedRequest?.header("X-Api-Key"))
        assertEquals(testDeviceId, processedRequest?.header("X-Device-ID"))
        assertEquals("Android", processedRequest?.header("X-App-Platform"))
        assertEquals("2.4.0", processedRequest?.header("X-App-Version"))
        assertEquals("application/json", processedRequest?.header("Accept"))
        assertNotNull(processedRequest?.header("X-Request-ID"))
        assertNotNull(processedRequest?.header("X-Client-Timestamp"))
    }

    @Test
    fun testSecureHeaderInterceptor_respectsNoAuthBypass() {
        val testToken = "secret-token"
        val interceptor = SecureHeaderInterceptor(
            tokenProvider = { testToken },
            apiKeyProvider = { null },
            deviceIdProvider = { "dev-1" },
            appVersion = "1.0.0"
        )

        val originalRequest = Request.Builder()
            .url("https://api.masterprinter.express/api/v1/auth/login")
            .header("X-No-Auth", "true")
            .build()

        var processedRequest: Request? = null
        val chain = createFakeChain(originalRequest) { req ->
            processedRequest = req
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        interceptor.intercept(chain)

        assertNotNull(processedRequest)
        assertNull("Authorization should NOT be injected when X-No-Auth is present", processedRequest?.header("Authorization"))
        assertNull("X-No-Auth should be stripped prior to transmitting over network", processedRequest?.header("X-No-Auth"))
    }

    // =========================================================================
    // 2. ErrorHandlingInterceptor Tests
    // =========================================================================

    @Test
    fun testErrorHandlingInterceptor_handles401AndInvokesCallback() {
        val unauthorizedCalled = AtomicBoolean(false)
        val interceptor = ErrorHandlingInterceptor(
            moshi = moshi,
            onUnauthorized = { unauthorizedCalled.set(true) }
        )

        val errorJson = """
            {
                "status": "error",
                "code": "AUTH_TOKEN_EXPIRED",
                "message": "The session token has expired. Please authenticate again."
            }
        """.trimIndent()

        val chain = createFakeChain(Request.Builder().url("https://api.masterprinter.express/api/v1/profile").build()) { req ->
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body(errorJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        try {
            interceptor.intercept(chain)
            fail("Expected UnauthorizedException was not thrown")
        } catch (e: UnauthorizedException) {
            assertTrue("onUnauthorized callback should be executed on 401", unauthorizedCalled.get())
            assertEquals(401, e.statusCode)
            assertEquals("AUTH_TOKEN_EXPIRED", e.errorCode)
            assertTrue(e.message?.contains("expired") == true)
        }
    }

    @Test
    fun testErrorHandlingInterceptor_handles422ValidationWithFieldErrors() {
        val interceptor = ErrorHandlingInterceptor(moshi = moshi)

        val validationJson = """
            {
                "status": "error",
                "code": "VALIDATION_FAILED",
                "message": "Invalid order parameters provided.",
                "field_errors": {
                    "copies": ["Copies must be greater than 0"],
                    "paper_size": ["Unsupported paper size"]
                }
            }
        """.trimIndent()

        val chain = createFakeChain(Request.Builder().url("https://api.masterprinter.express/api/v1/orders").build()) { req ->
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(422)
                .message("Unprocessable Entity")
                .body(validationJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        try {
            interceptor.intercept(chain)
            fail("Expected ValidationException was not thrown")
        } catch (e: ValidationException) {
            assertEquals(422, e.statusCode)
            assertEquals("VALIDATION_FAILED", e.errorCode)
            assertEquals(listOf("Copies must be greater than 0"), e.fieldErrors["copies"])
            assertEquals(listOf("Unsupported paper size"), e.fieldErrors["paper_size"])
        }
    }

    @Test
    fun testErrorHandlingInterceptor_handles429RateLimit() {
        val interceptor = ErrorHandlingInterceptor(moshi = moshi)

        val chain = createFakeChain(Request.Builder().url("https://api.masterprinter.express/api/v1/orders").build()) { req ->
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(429)
                .header("Retry-After", "45")
                .message("Too Many Requests")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        try {
            interceptor.intercept(chain)
            fail("Expected RateLimitExceededException was not thrown")
        } catch (e: RateLimitExceededException) {
            assertEquals(429, e.statusCode)
            assertEquals(45L, e.retryAfterSeconds)
        }
    }

    @Test
    fun testErrorHandlingInterceptor_handles500ServerError() {
        val interceptor = ErrorHandlingInterceptor(moshi = moshi)

        val chain = createFakeChain(Request.Builder().url("https://api.masterprinter.express/api/v1/health").build()) { req ->
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body("""{"message":"Database connection pool exhausted"}""".toResponseBody("application/json".toMediaType()))
                .build()
        }

        try {
            interceptor.intercept(chain)
            fail("Expected ServerException was not thrown")
        } catch (e: ServerException) {
            assertEquals(500, e.statusCode)
            assertTrue(e.message?.contains("Database") == true)
        }
    }

    // =========================================================================
    // 3. SafeApiCall Tests
    // =========================================================================

    @Test
    fun testSafeApiCall_successWrapper() = runBlocking {
        val result = safeApiCall {
            "API_DATA_OK"
        }

        assertTrue(result is NetworkResult.Success)
        assertEquals("API_DATA_OK", (result as NetworkResult.Success).data)
        assertEquals("API_DATA_OK", result.getOrNull())
    }

    @Test
    fun testSafeApiCall_errorWrapper() = runBlocking {
        val result = safeApiCall {
            throw NotFoundException("Order with id 9999 was not found")
        }

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(404, error.statusCode)
        assertTrue(error.message.contains("9999"))
        assertNull(result.getOrNull())
    }

    // =========================================================================
    // 4. Moshi DTO Serialization Tests
    // =========================================================================

    @Test
    fun testOrderResponseDto_serialization() {
        val json = """
            {
                "id": 101,
                "order_number": "MP-2026-9876",
                "customer_name": "Aarav Sharma",
                "calculated_price": 250.0,
                "final_amount": 280.0,
                "payment_status": "COMPLETED",
                "order_status": "QUEUED"
            }
        """.trimIndent()

        val adapter = moshi.adapter(OrderResponseDto::class.java)
        val order = adapter.fromJson(json)

        assertNotNull(order)
        assertEquals(101L, order?.id)
        assertEquals("MP-2026-9876", order?.orderNumber)
        assertEquals("Aarav Sharma", order?.customerName)
        assertEquals(280.0, order?.finalAmount ?: 0.0, 0.001)
        assertEquals("COMPLETED", order?.paymentStatus)
    }

    @Test
    fun testLoginResponseDto_serialization() {
        val json = """
            {
                "access_token": "mock-access-token-abc",
                "token_type": "Bearer",
                "expires_in": 86400,
                "refresh_token": "mock-refresh-token-xyz",
                "user": {
                    "id": 1,
                    "username": "superadmin",
                    "role": "SUPER_ADMIN"
                }
            }
        """.trimIndent()

        val adapter = moshi.adapter(LoginResponseDto::class.java)
        val loginResponse = adapter.fromJson(json)

        assertNotNull(loginResponse)
        assertEquals("mock-access-token-abc", loginResponse?.accessToken)
        assertEquals(86400L, loginResponse?.expiresInSeconds)
        assertEquals("superadmin", loginResponse?.user?.username)
        assertEquals("SUPER_ADMIN", loginResponse?.user?.role)
    }
}
