package com.example.domain.model

enum class UserRole(val displayName: String, val level: Int) {
    SUPER_ADMIN("Super Admin", 100),
    ADMIN("Administrator", 80),
    MANAGER("Manager", 60),
    STAFF("Staff", 40),
    PRINTER_OPERATOR("Printer Operator", 30),
    CUSTOMER("Customer", 10);

    companion object {
        fun fromString(value: String): UserRole {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CUSTOMER
        }
    }
}

enum class Permission(val code: String, val description: String) {
    VIEW_ORDERS("view_orders", "View all customer orders"),
    PROCESS_ORDERS("process_orders", "Process, update and fulfill orders"),
    CANCEL_ORDERS("cancel_orders", "Cancel customer orders"),
    REFUND_ORDERS("refund_orders", "Initiate and process payment refunds"),
    MANAGE_PRINTERS("manage_printers", "Register, edit, configure hardware printers"),
    MANAGE_QUEUE("manage_queue", "Prioritize, pause, resume and cancel print queue jobs"),
    PRINT_DOCUMENTS("print_documents", "Execute physical printing jobs"),
    REPRINT_DOCUMENTS("reprint_documents", "Authorize and trigger reprints"),
    MANAGE_CUSTOMERS("manage_customers", "View customer records, notes and account status"),
    MANAGE_STAFF("manage_staff", "Create and manage staff accounts and roles"),
    MANAGE_SERVICES("manage_services", "Create, edit, toggle print catalog services"),
    MANAGE_PRICING("manage_pricing", "Configure per-page, paper, binding and coupon pricing"),
    MANAGE_SETTINGS("manage_settings", "Update general system settings"),
    MANAGE_BRANDING("manage_branding", "Modify app brand name, logos, colors and banners"),
    MANAGE_INTEGRATIONS("manage_integrations", "Configure payment gateways, webhooks, APIs"),
    VIEW_REPORTS("view_reports", "View sales, financial, printer and customer reports"),
    VIEW_AUDIT_LOGS("view_audit_logs", "Inspect security audit logs"),
    MANAGE_PLANS("manage_plans", "Configure subscription and pricing tiers");

    companion object {
        val ALL_ADMIN_PERMISSIONS = entries.toSet()
        val MANAGER_PERMISSIONS = setOf(
            VIEW_ORDERS, PROCESS_ORDERS, CANCEL_ORDERS,
            MANAGE_PRINTERS, MANAGE_QUEUE, PRINT_DOCUMENTS, REPRINT_DOCUMENTS,
            MANAGE_CUSTOMERS, VIEW_REPORTS
        )
        val STAFF_PERMISSIONS = setOf(
            VIEW_ORDERS, PROCESS_ORDERS, MANAGE_QUEUE, PRINT_DOCUMENTS,
            MANAGE_CUSTOMERS
        )
        val OPERATOR_PERMISSIONS = setOf(
            VIEW_ORDERS, MANAGE_QUEUE, PRINT_DOCUMENTS, REPRINT_DOCUMENTS
        )
        val CUSTOMER_PERMISSIONS = emptySet<Permission>()
    }
}

enum class OrderStatus(val displayName: String, val colorHex: Long) {
    PENDING("Pending", 0xFF64748B),
    PAYMENT_PENDING("Payment Pending", 0xFFEAB308),
    PAYMENT_VERIFIED("Payment Verified", 0xFF0284C7),
    QUEUED("Queued in Printer", 0xFF6366F1),
    PRINTING("Printing Now", 0xFF3B82F6),
    READY("Ready for Pickup", 0xFF10B981),
    COMPLETED("Completed", 0xFF059669),
    FAILED("Printing Failed", 0xFFEF4444),
    CANCELLED("Cancelled", 0xFF94A3B8),
    REFUND_PENDING("Refund Pending", 0xFFF97316),
    REFUNDED("Refunded", 0xFF8B5CF6);

    companion object {
        fun fromString(value: String): OrderStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

enum class PaymentStatus(val displayName: String) {
    INITIATED("Initiated"),
    PENDING("Pending Verification"),
    VERIFIED("Verified"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    PARTIALLY_REFUNDED("Partially Refunded");

    companion object {
        fun fromString(value: String): PaymentStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: INITIATED
        }
    }
}

enum class PrinterStatus(val displayName: String, val colorHex: Long) {
    ONLINE("Online", 0xFF10B981),
    OFFLINE("Offline", 0xFF64748B),
    BUSY("Busy", 0xFFF59E0B),
    PRINTING("Printing", 0xFF3B82F6),
    PAPER_LOW("Paper Low", 0xFFF97316),
    PAPER_OUT("Paper Out", 0xFFEF4444),
    ERROR("Hardware Error", 0xFFDC2626),
    MAINTENANCE("In Maintenance", 0xFF8B5CF6);

    companion object {
        fun fromString(value: String): PrinterStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OFFLINE
        }
    }
}

enum class PrintJobStatus(val displayName: String) {
    QUEUED("Queued"),
    PRINTING("Printing"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): PrintJobStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: QUEUED
        }
    }
}
