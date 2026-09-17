package com.example.data.network.api

import com.example.data.network.dto.ApiHealthResponseDto
import com.example.data.network.dto.ApiResponse
import com.example.data.network.dto.CreateOrderRequestDto
import com.example.data.network.dto.LoginRequestDto
import com.example.data.network.dto.LoginResponseDto
import com.example.data.network.dto.OrderResponseDto
import com.example.data.network.dto.PricingCatalogResponseDto
import com.example.data.network.dto.PrintJobResponseDto
import com.example.data.network.dto.PrinterStatusDto
import com.example.data.network.dto.RefreshTokenRequestDto
import com.example.data.network.dto.SubmitPrintJobRequestDto
import com.example.data.network.dto.SyncOrdersBatchRequestDto
import com.example.data.network.dto.SyncOrdersBatchResponseDto
import com.example.data.network.dto.TelemetryHeartbeatRequestDto
import com.example.data.network.dto.TelemetryHeartbeatResponseDto
import com.example.data.network.dto.TokenRefreshResponseDto
import com.example.data.network.dto.UpdateOrderStatusRequestDto
import com.example.data.network.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Production Retrofit Service definition for Master Printer.
 * Supports authentication, customer order fulfillment, cloud print fleet orchestration,
 * telemetry heartbeats, and real-time catalog synchronization.
 */
interface MasterPrinterApiService {

    // =========================================================================
    // System & Health
    // =========================================================================

    @Headers("X-No-Auth: true")
    @GET("api/v1/health")
    suspend fun checkHealth(): ApiResponse<ApiHealthResponseDto>

    // =========================================================================
    // Authentication & Profile
    // =========================================================================

    @Headers("X-No-Auth: true")
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): ApiResponse<LoginResponseDto>

    @Headers("X-No-Auth: true")
    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestDto
    ): ApiResponse<TokenRefreshResponseDto>

    @GET("api/v1/auth/profile")
    suspend fun getProfile(): ApiResponse<UserProfileDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    // =========================================================================
    // Orders & Fulfillment
    // =========================================================================

    @GET("api/v1/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("customer_id") customerId: Long? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): ApiResponse<List<OrderResponseDto>>

    @GET("api/v1/orders/{orderId}")
    suspend fun getOrderById(
        @Path("orderId") orderId: Long
    ): ApiResponse<OrderResponseDto>

    @GET("api/v1/orders/by-number/{orderNumber}")
    suspend fun getOrderByNumber(
        @Path("orderNumber") orderNumber: String
    ): ApiResponse<OrderResponseDto>

    @POST("api/v1/orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequestDto
    ): ApiResponse<OrderResponseDto>

    @PATCH("api/v1/orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Long,
        @Body request: UpdateOrderStatusRequestDto
    ): ApiResponse<OrderResponseDto>

    @POST("api/v1/orders/sync-batch")
    suspend fun syncOrdersBatch(
        @Body request: SyncOrdersBatchRequestDto
    ): ApiResponse<SyncOrdersBatchResponseDto>

    // =========================================================================
    // Printers & Hardware Queue
    // =========================================================================

    @GET("api/v1/printers")
    suspend fun getPrinters(): ApiResponse<List<PrinterStatusDto>>

    @GET("api/v1/printers/{printerId}/status")
    suspend fun getPrinterStatus(
        @Path("printerId") printerId: Long
    ): ApiResponse<PrinterStatusDto>

    @POST("api/v1/printers/{printerId}/jobs")
    suspend fun submitPrintJob(
        @Path("printerId") printerId: Long,
        @Body request: SubmitPrintJobRequestDto
    ): ApiResponse<PrintJobResponseDto>

    @GET("api/v1/jobs/{jobId}/status")
    suspend fun getJobStatus(
        @Path("jobId") jobId: Long
    ): ApiResponse<PrintJobResponseDto>

    @POST("api/v1/jobs/{jobId}/cancel")
    suspend fun cancelPrintJob(
        @Path("jobId") jobId: Long
    ): ApiResponse<PrintJobResponseDto>

    // =========================================================================
    // Catalog & Pricing
    // =========================================================================

    @Headers("X-No-Auth: true")
    @GET("api/v1/catalog/pricing")
    suspend fun getPricingCatalog(): ApiResponse<PricingCatalogResponseDto>

    // =========================================================================
    // Telemetry & Diagnostic Heartbeat
    // =========================================================================

    @POST("api/v1/telemetry/heartbeat")
    suspend fun sendTelemetryHeartbeat(
        @Body request: TelemetryHeartbeatRequestDto
    ): ApiResponse<TelemetryHeartbeatResponseDto>
}
