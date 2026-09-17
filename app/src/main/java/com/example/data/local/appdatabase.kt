package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.BannerDao
import com.example.data.local.dao.CouponDao
import com.example.data.local.dao.IntegrationDao
import com.example.data.local.dao.NotificationTemplateDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.PlanDao
import com.example.data.local.dao.PricingRuleDao
import com.example.data.local.dao.PrintJobDao
import com.example.data.local.dao.PrinterDao
import com.example.data.local.dao.ServiceDao
import com.example.data.local.dao.SettingDao
import com.example.data.local.dao.UserDao
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

@Database(
    entities = [
        UserEntity::class,
        PrinterEntity::class,
        PrintJobEntity::class,
        OrderEntity::class,
        ServiceEntity::class,
        PricingRuleEntity::class,
        CouponEntity::class,
        BannerEntity::class,
        SettingEntity::class,
        IntegrationEntity::class,
        AuditLogEntity::class,
        NotificationTemplateEntity::class,
        PlanEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun printerDao(): PrinterDao
    abstract fun printJobDao(): PrintJobDao
    abstract fun orderDao(): OrderDao
    abstract fun serviceDao(): ServiceDao
    abstract fun pricingRuleDao(): PricingRuleDao
    abstract fun couponDao(): CouponDao
    abstract fun bannerDao(): BannerDao
    abstract fun settingDao(): SettingDao
    abstract fun integrationDao(): IntegrationDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationTemplateDao(): NotificationTemplateDao
    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "master_printer.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
