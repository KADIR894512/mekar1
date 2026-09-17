package com.example.data.network.dto

import com.squareup.moshi.Json

/**
 * Standard structured API error response envelope returned by production endpoints.
 */
data class ApiErrorResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "path") val path: String? = null,
    @Json(name = "field_errors") val fieldErrors: Map<String, List<String>>? = null
)

/**
 * Standard generic response envelope for all successful production API responses.
 */
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "status") val status: String? = "OK",
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "meta") val meta: PaginationMetaDto? = null,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * Pagination metadata for collection endpoints.
 */
data class PaginationMetaDto(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "per_page") val perPage: Int = 20,
    @Json(name = "total_items") val totalItems: Long = 0,
    @Json(name = "total_pages") val totalPages: Int = 1
)

// =========================================================================
// Authentication & User DTOs
// =========================================================================

data class LoginRequestDto(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "password") val password: String,
    @Json(name = "device_id") val deviceId: String? = null,
    @Json(name = "device_name") val deviceName: String? = null,
    @Json(name = "fcm_token") val fcmToken: String? = null
)

data class LoginResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "token_type") val tokenType: String = "Bearer",
    @Json(name = "expires_in") val expiresInSeconds: Long = 86400,
    @Json(name = "user") val user: UserProfileDto
)

data class RefreshTokenRequestDto(
    @Json(name = "refresh_token") val refreshToken: String
)

data class TokenRefreshResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresInSeconds: Long = 86400
)

data class UserProfileDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "role") val role: String = "CUSTOMER",
    @Json(name = "permissions") val permissions: String = "",
    @Json(name = "status") val status: String = "ACTIVE"
)

// =========================================================================
// Orders & Fulfillment DTOs
// =========================================================================

data class CreateOrderRequestDto(
    @Json(name = "order_number") val orderNumber: String,
    @Json(name = "customer_id") val customerId: Long = 0L,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_phone") val customerPhone: String = "",
    @Json(name = "customer_email") val customerEmail: String = "",
    @Json(name = "service_id") val serviceId: Long = 0L,
    @Json(name = "service_name") val serviceName: String = "",
    @Json(name = "paper_size") val paperSize: String = "A4",
    @Json(name = "is_color") val isColor: Boolean = false,
    @Json(name = "is_duplex") val isDuplex: Boolean = false,
    @Json(name = "copies") val copies: Int = 1,
    @Json(name = "page_range") val pageRange: String = "All",
    @Json(name = "page_count") val pageCount: Int = 1,
    @Json(name = "paper_type") val paperType: String = "Plain",
    @Json(name = "binding") val binding: String = "None",
    @Json(name = "lamination") val lamination: String = "None",
    @Json(name = "scanning") val scanning: Boolean = false,
    @Json(name = "thermal") val thermal: Boolean = false,
    @Json(name = "document_name") val documentName: String = "",
    @Json(name = "document_size_bytes") val documentSizeBytes: Long = 0L,
    @Json(name = "calculated_price") val calculatedPrice: Double = 0.0,
    @Json(name = "tax_amount") val taxAmount: Double = 0.0,
    @Json(name = "discount_amount") val discountAmount: Double = 0.0,
    @Json(name = "final_amount") val finalAmount: Double = 0.0,
    @Json(name = "coupon_code") val couponCode: String? = null,
    @Json(name = "payment_method") val paymentMethod: String = "CASH",
    @Json(name = "payment_status") val paymentStatus: String = "PENDING",
    @Json(name = "transaction_id") val transactionId: String? = null,
    @Json(name = "notes") val notes: String? = null
)

data class OrderResponseDto(
    @Json(name = "id") val id: Long,
    @Json(name = "order_number") val orderNumber: String,
    @Json(name = "customer_id") val customerId: Long = 0L,
    @Json(name = "customer_name") val customerName: String = "",
    @Json(name = "customer_phone") val customerPhone: String = "",
    @Json(name = "customer_email") val customerEmail: String = "",
    @Json(name = "service_id") val serviceId: Long = 0L,
    @Json(name = "service_name") val serviceName: String = "",
    @Json(name = "paper_size") val paperSize: String = "A4",
    @Json(name = "is_color") val isColor: Boolean = false,
    @Json(name = "is_duplex") val isDuplex: Boolean = false,
    @Json(name = "copies") val copies: Int = 1,
    @Json(name = "page_range") val pageRange: String = "All",
    @Json(name = "page_count") val pageCount: Int = 1,
    @Json(name = "paper_type") val paperType: String = "Plain",
    @Json(name = "binding") val binding: String = "None",
    @Json(name = "lamination") val lamination: String = "None",
    @Json(name = "document_name") val documentName: String = "",
    @Json(name = "calculated_price") val calculatedPrice: Double = 0.0,
    @Json(name = "tax_amount") val taxAmount: Double = 0.0,
    @Json(name = "discount_amount") val discountAmount: Double = 0.0,
    @Json(name = "final_amount") val finalAmount: Double = 0.0,
    @Json(name = "payment_status") val paymentStatus: String = "PENDING",
    @Json(name = "payment_method") val paymentMethod: String = "CASH",
    @Json(name = "transaction_id") val transactionId: String? = null,
    @Json(name = "print_status") val printStatus: String = "QUEUED",
    @Json(name = "assigned_printer_id") val assignedPrinterId: Long? = null,
    @Json(name = "assigned_staff_id") val assignedStaffId: Long? = null,
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @Json(name = "notes") val notes: String? = null
)

data class UpdateOrderStatusRequestDto(
    @Json(name = "print_status") val printStatus: String,
    @Json(name = "payment_status") val paymentStatus: String? = null,
    @Json(name = "assigned_printer_id") val assignedPrinterId: Long? = null,
    @Json(name = "assigned_staff_id") val assignedStaffId: Long? = null,
    @Json(name = "cancel_reason") val cancelReason: String? = null,
    @Json(name = "notes") val notes: String? = null
)

data class SyncOrdersBatchRequestDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "orders") val orders: List<CreateOrderRequestDto>
)

data class SyncOrdersBatchResponseDto(
    @Json(name = "synced_count") val syncedCount: Int,
    @Json(name = "failed_count") val failedCount: Int,
    @Json(name = "order_ids") val orderIds: List<Long> = emptyList(),
    @Json(name = "server_time") val serverTime: Long = System.currentTimeMillis()
)

// =========================================================================
// Printers & Print Job DTOs
// =========================================================================

data class PrinterStatusDto(
    @Json(name = "id") val id: Long,
    @Json(name = "printer_identifier") val printerIdentifier: String,
    @Json(name = "name") val name: String,
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "model") val model: String,
    @Json(name = "connection_type") val connectionType: String,
    @Json(name = "ip_address") val ipAddress: String,
    @Json(name = "port") val port: Int,
    @Json(name = "location") val location: String,
    @Json(name = "status") val status: String,
    @Json(name = "is_enabled") val isEnabled: Boolean = true,
    @Json(name = "is_default") val isDefault: Boolean = false,
    @Json(name = "color_support") val colorSupport: Boolean = true,
    @Json(name = "duplex_support") val duplexSupport: Boolean = true,
    @Json(name = "paper_level_percent") val paperLevelPercent: Int = 100,
    @Json(name = "toner_level_percent") val tonerLevelPercent: Int = 100,
    @Json(name = "active_jobs_count") val activeJobsCount: Int = 0,
    @Json(name = "last_seen") val lastSeen: Long = System.currentTimeMillis()
)

data class SubmitPrintJobRequestDto(
    @Json(name = "order_id") val orderId: Long,
    @Json(name = "printer_id") val printerId: Long,
    @Json(name = "document_name") val documentName: String,
    @Json(name = "document_url") val documentUrl: String? = null,
    @Json(name = "document_type") val documentType: String = "PDF",
    @Json(name = "page_count") val pageCount: Int,
    @Json(name = "copies") val copies: Int = 1,
    @Json(name = "paper_size") val paperSize: String = "A4",
    @Json(name = "is_color") val isColor: Boolean = false,
    @Json(name = "is_duplex") val isDuplex: Boolean = false,
    @Json(name = "priority") val priority: Int = 5
)

data class PrintJobResponseDto(
    @Json(name = "id") val id: Long,
    @Json(name = "order_id") val orderId: Long,
    @Json(name = "printer_id") val printerId: Long,
    @Json(name = "document_name") val documentName: String,
    @Json(name = "status") val status: String,
    @Json(name = "priority") val priority: Int,
    @Json(name = "progress_percent") val progressPercent: Int,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "created_at") val createdAt: Long,
    @Json(name = "completed_at") val completedAt: Long? = null
)

// =========================================================================
// Catalog, Pricing & Telemetry DTOs
// =========================================================================

data class ServiceItemDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "icon_name") val iconName: String,
    @Json(name = "base_price") val basePrice: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "category") val category: String,
    @Json(name = "is_available") val isAvailable: Boolean = true
)

data class PricingRuleDto(
    @Json(name = "id") val id: Long,
    @Json(name = "service_name") val serviceName: String,
    @Json(name = "paper_size") val paperSize: String,
    @Json(name = "is_color") val isColor: Boolean,
    @Json(name = "is_duplex") val isDuplex: Boolean,
    @Json(name = "min_pages") val minPages: Int,
    @Json(name = "max_pages") val maxPages: Int,
    @Json(name = "price_per_page") val pricePerPage: Double
)

data class PlanItemDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Double,
    @Json(name = "billing_period") val billingPeriod: String,
    @Json(name = "features") val features: String,
    @Json(name = "storage_limit_gb") val storageLimitGb: Int,
    @Json(name = "print_limit_monthly") val printLimitMonthly: Int,
    @Json(name = "is_active") val isActive: Boolean = true
)

data class PricingCatalogResponseDto(
    @Json(name = "services") val services: List<ServiceItemDto> = emptyList(),
    @Json(name = "pricing_rules") val pricingRules: List<PricingRuleDto> = emptyList(),
    @Json(name = "plans") val plans: List<PlanItemDto> = emptyList(),
    @Json(name = "currency") val currency: String = "INR",
    @Json(name = "catalog_version") val catalogVersion: String = "1.0.0"
)

data class TelemetryHeartbeatRequestDto(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "os_version") val osVersion: String,
    @Json(name = "device_model") val deviceModel: String,
    @Json(name = "user_role") val userRole: String,
    @Json(name = "active_printers") val activePrinters: Int,
    @Json(name = "queued_jobs") val queuedJobs: Int,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class TelemetryHeartbeatResponseDto(
    @Json(name = "acknowledged") val acknowledged: Boolean = true,
    @Json(name = "server_time") val serverTime: Long = System.currentTimeMillis(),
    @Json(name = "sync_required") val syncRequired: Boolean = false,
    @Json(name = "active_alerts") val activeAlerts: List<String> = emptyList()
)

data class ApiHealthResponseDto(
    @Json(name = "status") val status: String = "healthy",
    @Json(name = "environment") val environment: String = "production",
    @Json(name = "version") val version: String = "1.0.0",
    @Json(name = "database") val database: String = "connected",
    @Json(name = "redis") val redis: String = "connected",
    @Json(name = "uptime_seconds") val uptimeSeconds: Long = 0,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
