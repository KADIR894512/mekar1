package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val fullName: String,
    val role: String, // SUPER_ADMIN, ADMIN, MANAGER, STAFF, PRINTER_OPERATOR, CUSTOMER
    val permissions: String = "",
    val status: String = "ACTIVE", // ACTIVE, DISABLED
    val createdAt: Long = System.currentTimeMillis(),
    val internalNotes: String = ""
)

@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val printerIdentifier: String,
    val name: String,
    val manufacturer: String,
    val model: String,
    val connectionType: String, // Wi-Fi/LAN, Bluetooth, USB/OTG, Android Print Framework, Direct IP
    val ipAddress: String,
    val port: Int,
    val location: String,
    val paperSizesSupported: String = "A4, A3, A5, Letter, Legal",
    val colorSupport: Boolean = true,
    val duplexSupport: Boolean = true,
    val status: String = "ONLINE", // ONLINE, OFFLINE, BUSY, PRINTING, PAPER_LOW, PAPER_OUT, ERROR, MAINTENANCE
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val assignedServices: String = "ALL",
    val lastSeen: Long = System.currentTimeMillis(),
    val maintenanceNotes: String = ""
)

@Entity(
    tableName = "print_jobs",
    indices = [Index(value = ["orderId"]), Index(value = ["printerId"])]
)
data class PrintJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val printerId: Long,
    val documentName: String,
    val documentType: String,
    val pageCount: Int,
    val copies: Int,
    val paperSize: String,
    val isColor: Boolean,
    val isDuplex: Boolean,
    val status: String = "QUEUED", // QUEUED, PRINTING, PAUSED, COMPLETED, FAILED, CANCELLED
    val priority: Int = 5, // 1 to 10
    val progressPercent: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(
    tableName = "orders",
    indices = [Index(value = ["orderNumber"], unique = true), Index(value = ["customerId"])]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val serviceId: Long,
    val serviceName: String,
    val paperSize: String,
    val isColor: Boolean,
    val isDuplex: Boolean,
    val copies: Int,
    val pageRange: String,
    val pageCount: Int,
    val paperType: String,
    val binding: String,
    val lamination: String,
    val scanning: Boolean = false,
    val thermal: Boolean = false,
    val documentName: String,
    val documentSizeBytes: Long,
    val calculatedPrice: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val finalAmount: Double,
    val couponCode: String? = null,
    val paymentStatus: String, // INITIATED, PENDING, VERIFIED, FAILED, REFUNDED, PARTIALLY_REFUNDED
    val paymentMethod: String, // UPI, Card, Net Banking, Store POS
    val transactionId: String? = null,
    val printStatus: String, // PENDING, PAYMENT_PENDING, PAYMENT_VERIFIED, QUEUED, PRINTING, READY, COMPLETED, FAILED, CANCELLED, REFUND_PENDING, REFUNDED
    val assignedPrinterId: Long? = null,
    val assignedStaffId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val cancelReason: String? = null,
    val notes: String? = null
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val iconName: String,
    val basePrice: Double,
    val unit: String = "per page",
    val minQty: Int = 1,
    val maxQty: Int = 1000,
    val isAvailable: Boolean = true,
    val taxPercent: Double = 18.0,
    val extraCharges: Double = 0.0,
    val printerAssignment: String = "ALL",
    val isVisibleToCustomer: Boolean = true
)

@Entity(
    tableName = "pricing_rules",
    indices = [Index(value = ["ruleKey"], unique = true)]
)
data class PricingRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleKey: String,
    val label: String,
    val value: Double,
    val category: String // PAGE, PAPER, FINISHING, FEES, TAX
)

@Entity(
    tableName = "coupons",
    indices = [Index(value = ["code"], unique = true)]
)
data class CouponEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val discountPercent: Double,
    val discountAmount: Double,
    val minOrderValue: Double,
    val isActive: Boolean = true,
    val maxUses: Int = 100,
    val usedCount: Int = 0
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val buttonText: String = "Order Now",
    val actionDestination: String = "order",
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val category: String = "GENERAL"
)

@Entity(
    tableName = "integrations",
    indices = [Index(value = ["serviceKey"], unique = true)]
)
data class IntegrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceKey: String,
    val name: String,
    val isEnabled: Boolean = false,
    val baseUrl: String = "",
    val apiKeyMasked: String = "",
    val secretKeyMasked: String = "",
    val webhookUrl: String = "",
    val connectionStatus: String = "NOT_CONFIGURED", // NOT_CONFIGURED, CONNECTED, ERROR
    val lastSyncTime: Long = 0,
    val errorLog: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val role: String,
    val action: String,
    val target: String,
    val status: String = "SUCCESS",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_templates")
data class NotificationTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val title: String,
    val messageTemplate: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val billingPeriod: String = "Monthly",
    val features: String,
    val storageLimitGb: Int = 5,
    val printLimitMonthly: Int = 500,
    val staffLimit: Int = 2,
    val printerLimit: Int = 2,
    val isActive: Boolean = true
)
