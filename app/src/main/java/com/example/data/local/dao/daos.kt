package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.BannerEntity
import com.example.data.local.entities.CouponEntity
import com.example.data.local.entities.IntegrationEntity
import com.example.data.local.entities.NotificationTemplateEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.PricingRuleEntity
import com.example.data.local.entities.PrintJobEntity
import com.example.data.local.entities.PrinterEntity
import com.example.data.local.entities.ServiceEntity
import com.example.data.local.entities.SettingEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY createdAt DESC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email OR phone = :phone LIMIT 1")
    suspend fun getUserByEmailOrPhone(email: String, phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = :userId")
    suspend fun updatePassword(userId: Long, passwordHash: String)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: String)

    @Query("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'")
    fun getCustomerCount(): Flow<Int>
}

@Dao
interface PrinterDao {
    @Query("SELECT * FROM printers ORDER BY isDefault DESC, name ASC")
    fun getAllPrinters(): Flow<List<PrinterEntity>>

    @Query("SELECT * FROM printers WHERE isEnabled = 1")
    fun getEnabledPrinters(): Flow<List<PrinterEntity>>

    @Query("SELECT * FROM printers WHERE id = :id LIMIT 1")
    suspend fun getPrinterById(id: Long): PrinterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrinter(printer: PrinterEntity): Long

    @Update
    suspend fun updatePrinter(printer: PrinterEntity)

    @Query("UPDATE printers SET status = :status, lastSeen = :lastSeen WHERE id = :id")
    suspend fun updatePrinterStatus(id: Long, status: String, lastSeen: Long = System.currentTimeMillis())

    @Query("DELETE FROM printers WHERE id = :id")
    suspend fun deletePrinter(id: Long)

    @Query("SELECT COUNT(*) FROM printers WHERE status = 'ONLINE' AND isEnabled = 1")
    fun getActivePrinterCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM printers WHERE status != 'ONLINE' OR isEnabled = 0")
    fun getOfflinePrinterCount(): Flow<Int>
}

@Dao
interface PrintJobDao {
    @Query("SELECT * FROM print_jobs ORDER BY priority DESC, createdAt ASC")
    fun getAllPrintJobs(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE status IN ('QUEUED', 'PRINTING', 'PAUSED') ORDER BY priority DESC, createdAt ASC")
    fun getActiveQueue(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE id = :id LIMIT 1")
    suspend fun getPrintJobById(id: Long): PrintJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrintJob(job: PrintJobEntity): Long

    @Update
    suspend fun updatePrintJob(job: PrintJobEntity)

    @Query("UPDATE print_jobs SET status = :status, progressPercent = :progress, completedAt = :completedAt WHERE id = :id")
    suspend fun updateJobStatus(id: Long, status: String, progress: Int, completedAt: Long?)

    @Query("UPDATE print_jobs SET printerId = :newPrinterId WHERE id = :id")
    suspend fun reassignPrinter(id: Long, newPrinterId: Long)

    @Query("UPDATE print_jobs SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: Int)

    @Query("DELETE FROM print_jobs WHERE id = :id")
    suspend fun deletePrintJob(id: Long)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getOrdersByCustomer(customerId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber LIMIT 1")
    suspend fun getOrderByOrderNumber(orderNumber: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET printStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrderStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET paymentStatus = :paymentStatus, transactionId = :txnId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, paymentStatus: String, txnId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET assignedPrinterId = :printerId WHERE id = :id")
    suspend fun assignPrinter(id: Long, printerId: Long?)

    @Query("UPDATE orders SET assignedStaffId = :staffId WHERE id = :id")
    suspend fun assignStaff(id: Long, staffId: Long?)

    @Query("SELECT COUNT(*) FROM orders")
    fun getTotalOrdersCount(): Flow<Int>

    @Query("SELECT SUM(finalAmount) FROM orders WHERE paymentStatus = 'VERIFIED'")
    fun getTotalRevenue(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM orders WHERE printStatus IN ('PENDING', 'PAYMENT_PENDING', 'QUEUED')")
    fun getPendingOrdersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE printStatus = 'PRINTING'")
    fun getPrintingOrdersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE printStatus = 'COMPLETED'")
    fun getCompletedOrdersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE printStatus = 'FAILED'")
    fun getFailedOrdersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders WHERE paymentStatus IN ('REFUNDED', 'PARTIALLY_REFUNDED') OR printStatus = 'REFUNDED'")
    fun getRefundOrdersCount(): Flow<Int>
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY isAvailable DESC, id ASC")
    fun getAllServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE isVisibleToCustomer = 1 AND isAvailable = 1 ORDER BY id ASC")
    fun getCustomerServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    suspend fun getServiceById(id: Long): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity): Long

    @Update
    suspend fun updateService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteService(id: Long)
}

@Dao
interface PricingRuleDao {
    @Query("SELECT * FROM pricing_rules ORDER BY category ASC, label ASC")
    fun getAllPricingRules(): Flow<List<PricingRuleEntity>>

    @Query("SELECT * FROM pricing_rules WHERE ruleKey = :key LIMIT 1")
    suspend fun getRuleByKey(key: String): PricingRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: PricingRuleEntity): Long

    @Update
    suspend fun updateRule(rule: PricingRuleEntity)

    @Query("UPDATE pricing_rules SET value = :value WHERE ruleKey = :key")
    suspend fun updateValueByKey(key: String, value: Double)
}

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons ORDER BY id DESC")
    fun getAllCoupons(): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE code = :code AND isActive = 1 LIMIT 1")
    suspend fun getActiveCoupon(code: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: CouponEntity): Long

    @Update
    suspend fun updateCoupon(coupon: CouponEntity)

    @Query("DELETE FROM coupons WHERE id = :id")
    suspend fun deleteCoupon(id: Long)
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners ORDER BY displayOrder ASC, id ASC")
    fun getAllBanners(): Flow<List<BannerEntity>>

    @Query("SELECT * FROM banners WHERE isActive = 1 ORDER BY displayOrder ASC")
    fun getActiveBanners(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: BannerEntity): Long

    @Update
    suspend fun updateBanner(banner: BannerEntity)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBanner(id: Long)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}

@Dao
interface IntegrationDao {
    @Query("SELECT * FROM integrations ORDER BY id ASC")
    fun getAllIntegrations(): Flow<List<IntegrationEntity>>

    @Query("SELECT * FROM integrations WHERE serviceKey = :key LIMIT 1")
    suspend fun getIntegrationByKey(key: String): IntegrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegration(integration: IntegrationEntity): Long

    @Update
    suspend fun updateIntegration(integration: IntegrationEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}

@Dao
interface NotificationTemplateDao {
    @Query("SELECT * FROM notification_templates ORDER BY id ASC")
    fun getAllTemplates(): Flow<List<NotificationTemplateEntity>>

    @Query("SELECT * FROM notification_templates WHERE eventType = :eventType LIMIT 1")
    suspend fun getTemplateByEvent(eventType: String): NotificationTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: NotificationTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: NotificationTemplateEntity)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY price ASC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)
}
