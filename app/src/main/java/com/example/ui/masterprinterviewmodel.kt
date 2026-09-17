package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
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
import com.example.data.repository.MasterPrinterRepository
import com.example.data.firebase.FirebaseService
import com.example.data.firebase.FirebaseConnectionState
import com.example.data.firebase.FirebaseUserInfo
import com.example.data.firebase.CloudPublicFile
import com.example.data.session.UserSessionManager
import com.example.data.session.UserSessionState
import com.example.domain.model.OrderStatus
import com.example.domain.model.Permission
import com.example.domain.model.PrintJobStatus
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppPortal(val title: String, val minRole: UserRole) {
    CUSTOMER("Customer Portal", UserRole.CUSTOMER),
    PRINTER_OPERATOR("Printer Operator Terminal", UserRole.PRINTER_OPERATOR),
    STAFF("Staff Portal", UserRole.STAFF),
    ADMIN("Admin Portal", UserRole.ADMIN),
    SUPER_ADMIN("Super Admin System Control", UserRole.SUPER_ADMIN)
}

enum class CustomerNavScreen {
    HOME,
    SERVICES_CATALOG,
    ORDER_BUILDER,
    CLOUD_FILES,
    QR_FLOW,
    ORDER_TRACKING,
    ORDERS_HISTORY,
    PLANS,
    PROFILE,
    SUPPORT
}

enum class AdminNavScreen(val title: String) {
    DASHBOARD("Dashboard"),
    ORDERS("Order Management"),
    CLOUD_FILES("Cloud Public Files & Publish"),
    PRINTERS("MASTER PRINTER Systems"),
    QUEUE("Print Queue Center"),
    APP_CONTROL("App Control Center"),
    SERVICES("Service Catalog"),
    PRICING("Pricing Manager"),
    PAYMENTS("Payments & Billing"),
    INTEGRATIONS("API & Integrations"),
    NOTIFICATIONS("Notification Templates"),
    CUSTOMERS("Customer Management"),
    STAFF("Staff & Permissions"),
    REPORTS("Reports & Analytics"),
    AUDIT_LOGS("Audit Security Logs"),
    PLANS("Plans & Upgrades"),
    SETTINGS("System Settings")
}

class MasterPrinterViewModel(application: Application) : AndroidViewModel(application) {

    val sessionManager = UserSessionManager.getInstance(application)
    val retrofitClient = com.example.data.network.RetrofitClient.getInstance(application)
    private val repository: MasterPrinterRepository

    // Session State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentPortal = MutableStateFlow(AppPortal.CUSTOMER)
    val currentPortal: StateFlow<AppPortal> = _currentPortal.asStateFlow()

    private val _customerScreen = MutableStateFlow(CustomerNavScreen.HOME)
    val customerScreen: StateFlow<CustomerNavScreen> = _customerScreen.asStateFlow()

    private val _adminScreen = MutableStateFlow(AdminNavScreen.DASHBOARD)
    val adminScreen: StateFlow<AdminNavScreen> = _adminScreen.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<Long?>(null)
    val selectedOrderId: StateFlow<Long?> = _selectedOrderId.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Production Network Service State
    private val _apiBaseUrl = MutableStateFlow(retrofitClient.getBaseUrl())
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    private val _apiHealthStatus = MutableStateFlow<String?>("Ready")
    val apiHealthStatus: StateFlow<String?> = _apiHealthStatus.asStateFlow()

    private val _isApiTesting = MutableStateFlow(false)
    val isApiTesting: StateFlow<Boolean> = _isApiTesting.asStateFlow()

    // Firebase Cloud Integration State
    val firebaseService: FirebaseService = FirebaseService.getInstance(application)
    val firebaseConnectionState: StateFlow<FirebaseConnectionState> = firebaseService.connectionState
    val firebaseLastSyncTime: StateFlow<Long> = firebaseService.lastSyncTime
    val isFirebaseSyncing: StateFlow<Boolean> = firebaseService.isSyncing
    val currentFirebaseUser: StateFlow<FirebaseUserInfo?> = firebaseService.currentUserFlow
    val publicFiles: StateFlow<List<CloudPublicFile>> = firebaseService.publicFilesFlow

    private val _firebaseStatusMessage = MutableStateFlow<String?>("Connected")
    val firebaseStatusMessage: StateFlow<String?> = _firebaseStatusMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MasterPrinterRepository(db)
        repository.setApiService(retrofitClient.apiService)
        repository.setFirebaseService(firebaseService)

        // Restore active encrypted session on startup if valid
        val restoredUser = sessionManager.currentUser.value
        if (restoredUser != null && sessionManager.isLoggedIn.value) {
            _currentUser.value = restoredUser
            when (sessionManager.currentRole.value) {
                UserRole.SUPER_ADMIN -> {
                    _currentPortal.value = AppPortal.SUPER_ADMIN
                    _adminScreen.value = AdminNavScreen.DASHBOARD
                }
                UserRole.ADMIN -> {
                    _currentPortal.value = AppPortal.ADMIN
                    _adminScreen.value = AdminNavScreen.DASHBOARD
                }
                UserRole.MANAGER, UserRole.STAFF -> {
                    _currentPortal.value = AppPortal.STAFF
                }
                UserRole.PRINTER_OPERATOR -> {
                    _currentPortal.value = AppPortal.PRINTER_OPERATOR
                }
                UserRole.CUSTOMER -> {
                    _currentPortal.value = AppPortal.CUSTOMER
                    _customerScreen.value = CustomerNavScreen.HOME
                }
            }
        } else if (sessionManager.isAutoLoginEnabled()) {
            // Auto connect to data so login options are not repeatedly required
            autoConnectToData()
        }
    }

    // Reactive Data Flows
    val allOrders = repository.getAllOrders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPrinters = repository.getAllPrinters().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeQueue = repository.getActiveQueue().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allServices = repository.getAllServices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customerServices = repository.getCustomerServices().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pricingRules = repository.getAllPricingRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val coupons = repository.getAllCoupons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val banners = repository.getActiveBanners().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allBanners = repository.getAllBanners().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUsers = repository.getAllUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val integrations = repository.getAllIntegrations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notificationTemplates = repository.getAllNotificationTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val plans = repository.getAllPlans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.getRecentAuditLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsMap: StateFlow<Map<String, String>> = repository.getAllSettings()
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // KPI Summary Flows
    val totalOrdersCount = repository.getTotalOrdersCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalRevenue = repository.getTotalRevenue().map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val pendingOrdersCount = repository.getPendingOrdersCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val printingOrdersCount = repository.getPrintingOrdersCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val completedOrdersCount = repository.getCompletedOrdersCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val failedOrdersCount = repository.getFailedOrdersCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val refundOrdersCount = repository.getRefundOrdersCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val activePrinterCount = repository.getActivePrinterCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val offlinePrinterCount = repository.getOfflinePrinterCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val customerCount = repository.getCustomerCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val customerOrders: StateFlow<List<OrderEntity>> = combine(allOrders, _currentUser) { orders, user ->
        if (user == null) emptyList()
        else orders.filter { it.customerId == user.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun login(identifier: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.authenticateUser(identifier, pass)
            if (user != null) {
                val authToken = sessionManager.generateSecureToken("mp_auth_")
                val refreshToken = sessionManager.generateRefreshToken("mp_rf_")
                sessionManager.saveSession(user, authToken, refreshToken)
                _currentUser.value = user

                // Direct to appropriate portal based on role
                when (UserRole.fromString(user.role)) {
                    UserRole.SUPER_ADMIN -> {
                        _currentPortal.value = AppPortal.SUPER_ADMIN
                        _adminScreen.value = AdminNavScreen.DASHBOARD
                    }
                    UserRole.ADMIN -> {
                        _currentPortal.value = AppPortal.ADMIN
                        _adminScreen.value = AdminNavScreen.DASHBOARD
                    }
                    UserRole.MANAGER, UserRole.STAFF -> {
                        _currentPortal.value = AppPortal.STAFF
                    }
                    UserRole.PRINTER_OPERATOR -> {
                        _currentPortal.value = AppPortal.PRINTER_OPERATOR
                    }
                    UserRole.CUSTOMER -> {
                        _currentPortal.value = AppPortal.CUSTOMER
                        _customerScreen.value = CustomerNavScreen.HOME
                    }
                }
                onResult(true, null)
            } else {
                onResult(false, "Invalid credentials or account disabled")
            }
        }
    }

    fun loginWithBiometric(
        preferredUsername: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val targetUserIdentifier = preferredUsername
                ?: sessionManager.biometricAccountUsername.value
                ?: "KADIR143"

            var user = repository.getUserByIdentifier(targetUserIdentifier)
            if (user == null && targetUserIdentifier.equals("KADIR143", ignoreCase = true)) {
                // Ensure Super Admin is found
                user = repository.getUserByUsername("KADIR143")
            }

            if (user != null) {
                val authToken = sessionManager.generateSecureToken("mp_bio_auth_")
                val refreshToken = sessionManager.generateRefreshToken("mp_bio_rf_")
                sessionManager.saveSession(user, authToken, refreshToken)
                _currentUser.value = user

                // Route to appropriate portal
                when (UserRole.fromString(user.role)) {
                    UserRole.SUPER_ADMIN -> {
                        _currentPortal.value = AppPortal.SUPER_ADMIN
                        _adminScreen.value = AdminNavScreen.DASHBOARD
                    }
                    UserRole.ADMIN -> {
                        _currentPortal.value = AppPortal.ADMIN
                        _adminScreen.value = AdminNavScreen.DASHBOARD
                    }
                    UserRole.MANAGER, UserRole.STAFF -> {
                        _currentPortal.value = AppPortal.STAFF
                    }
                    UserRole.PRINTER_OPERATOR -> {
                        _currentPortal.value = AppPortal.PRINTER_OPERATOR
                    }
                    UserRole.CUSTOMER -> {
                        _currentPortal.value = AppPortal.CUSTOMER
                        _customerScreen.value = CustomerNavScreen.HOME
                    }
                }
                onResult(true, null)
            } else {
                onResult(false, "No account linked for biometric unlock. Please sign in with password first.")
            }
        }
    }

    fun register(
        user: String,
        email: String,
        phone: String,
        pass: String,
        fullName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.registerUser(user, email, phone, pass, fullName, UserRole.CUSTOMER)
            if (res.isSuccess) {
                val registeredUser = res.getOrNull()
                if (registeredUser != null) {
                    val authToken = sessionManager.generateSecureToken("mp_auth_")
                    val refreshToken = sessionManager.generateRefreshToken("mp_rf_")
                    sessionManager.saveSession(registeredUser, authToken, refreshToken)
                    _currentUser.value = registeredUser
                }
                _currentPortal.value = AppPortal.CUSTOMER
                _customerScreen.value = CustomerNavScreen.HOME
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _currentUser.value = null
        _currentPortal.value = AppPortal.CUSTOMER
        _customerScreen.value = CustomerNavScreen.HOME
        _selectedOrderId.value = null
    }

    fun switchPortal(portal: AppPortal) {
        // Strict Role Authorization Check using UserSessionManager
        if (!sessionManager.hasMinimumRole(portal.minRole)) {
            viewModelScope.launch {
                _toastMessage.emit("Unauthorized: ${portal.title} requires ${portal.minRole.displayName} access")
            }
            return
        }

        _currentPortal.value = portal
    }

    fun setCustomerScreen(screen: CustomerNavScreen) {
        _customerScreen.value = screen
    }

    fun setAdminScreen(screen: AdminNavScreen) {
        _adminScreen.value = screen
    }

    fun setSelectedOrderId(id: Long?) {
        _selectedOrderId.value = id
    }

    fun placeCustomerOrder(
        service: ServiceEntity,
        paperSize: String,
        isColor: Boolean,
        isDuplex: Boolean,
        copies: Int,
        pageRange: String,
        pageCount: Int,
        paperType: String,
        binding: String,
        lamination: String,
        scanning: Boolean,
        thermal: Boolean,
        documentName: String,
        documentSizeBytes: Long,
        couponCode: String?,
        onSuccess: (OrderEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("Please login or create an account to submit print jobs")
            return
        }
        viewModelScope.launch {
            try {
                val order = repository.createOrder(
                    customer = user,
                    service = service,
                    paperSize = paperSize,
                    isColor = isColor,
                    isDuplex = isDuplex,
                    copies = copies,
                    pageRange = pageRange,
                    pageCount = pageCount,
                    paperType = paperType,
                    binding = binding,
                    lamination = lamination,
                    scanning = scanning,
                    thermal = thermal,
                    documentName = documentName,
                    documentSizeBytes = documentSizeBytes,
                    couponCode = couponCode
                )
                _selectedOrderId.value = order.id
                onSuccess(order)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to place order")
            }
        }
    }

    fun verifyPayment(
        orderId: Long,
        method: String,
        txnId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.verifyAndProcessPayment(
                orderId = orderId,
                paymentMethod = method,
                txnId = txnId,
                username = user.username,
                role = user.role
            )
            if (result.isSuccess) {
                _toastMessage.emit("Payment Verified! Job entered MASTER PRINTER queue.")
                onComplete(true, null)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Payment verification failed"
                _toastMessage.emit(msg)
                onComplete(false, msg)
            }
        }
    }

    fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status, user.username, user.role)
            _toastMessage.emit("Order #$orderId updated to ${status.displayName}")
        }
    }

    fun cancelOrder(orderId: Long, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.cancelOrder(orderId, reason, user.username, user.role)
            _toastMessage.emit("Order #$orderId cancelled")
        }
    }

    fun refundOrder(orderId: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.refundOrder(orderId, user.username, user.role)
            _toastMessage.emit("Refund processed for Order #$orderId")
        }
    }

    // Printer & Queue controls
    fun updateJobStatus(jobId: Long, status: PrintJobStatus, progress: Int = 0) {
        viewModelScope.launch {
            repository.updateJobStatus(jobId, status, progress)
        }
    }

    fun reassignJob(jobId: Long, newPrinterId: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.reassignJobPrinter(jobId, newPrinterId, user.username, user.role)
            _toastMessage.emit("Job reassigned to printer #$newPrinterId")
        }
    }

    fun updateJobPriority(jobId: Long, priority: Int) {
        viewModelScope.launch {
            repository.updateJobPriority(jobId, priority)
        }
    }

    fun savePrinter(printer: PrinterEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.savePrinter(printer, user.username, user.role)
            _toastMessage.emit("Printer saved: ${printer.name}")
        }
    }

    fun updatePrinterStatus(id: Long, status: String) {
        viewModelScope.launch {
            repository.updatePrinterStatus(id, status)
        }
    }

    fun deletePrinter(id: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deletePrinter(id, user.username, user.role)
            _toastMessage.emit("Printer deleted")
        }
    }

    // App Control Center, Pricing, Services
    fun saveService(service: ServiceEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveService(service, user.username, user.role)
            _toastMessage.emit("Service saved: ${service.name}")
        }
    }

    fun deleteService(id: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteService(id, user.username, user.role)
            _toastMessage.emit("Service removed")
        }
    }

    fun updatePricingRule(key: String, value: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updatePricingRule(key, value, user.username, user.role)
            _toastMessage.emit("Updated pricing for $key to ₹$value")
        }
    }

    fun saveCoupon(coupon: CouponEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveCoupon(coupon, user.username, user.role)
            _toastMessage.emit("Coupon saved: ${coupon.code}")
        }
    }

    fun deleteCoupon(id: Long) {
        viewModelScope.launch {
            repository.deleteCoupon(id)
            _toastMessage.emit("Coupon deleted")
        }
    }

    fun saveBanner(banner: BannerEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveBanner(banner, user.username, user.role)
            _toastMessage.emit("Banner updated")
        }
    }

    fun deleteBanner(id: Long) {
        viewModelScope.launch {
            repository.deleteBanner(id)
            _toastMessage.emit("Banner deleted")
        }
    }

    fun updateSetting(key: String, value: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.setSetting(key, value, username = user.username, role = user.role)
            _toastMessage.emit("Setting '$key' saved")
        }
    }

    fun saveIntegration(integration: IntegrationEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveIntegration(integration, user.username, user.role)
            _toastMessage.emit("Integration updated: ${integration.name}")
        }
    }

    fun saveNotificationTemplate(template: NotificationTemplateEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveNotificationTemplate(template, user.username, user.role)
            _toastMessage.emit("Template updated: ${template.eventType}")
        }
    }

    fun savePlan(plan: PlanEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.savePlan(plan, user.username, user.role)
            _toastMessage.emit("Plan saved: ${plan.name}")
        }
    }

    fun updateUserStatus(userId: Long, status: String) {
        viewModelScope.launch {
            repository.updateUserStatus(userId, status)
            _toastMessage.emit("User status updated to $status")
        }
    }

    fun createStaffUser(
        username: String,
        email: String,
        phone: String,
        pass: String,
        fullName: String,
        role: UserRole,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.registerUser(username, email, phone, pass, fullName, role)
            if (res.isSuccess) {
                _toastMessage.emit("Staff member created: $fullName (${role.displayName})")
                onComplete(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to create staff"
                _toastMessage.emit(err)
                onComplete(false, err)
            }
        }
    }

    // =========================================================================
    // Network Service & Interceptor Control
    // =========================================================================

    fun updateApiBaseUrl(newUrl: String) {
        retrofitClient.updateBaseUrl(newUrl)
        _apiBaseUrl.value = retrofitClient.getBaseUrl()
        repository.setApiService(retrofitClient.apiService)
        viewModelScope.launch {
            _toastMessage.emit("Production Base URL updated to: ${_apiBaseUrl.value}")
        }
    }

    fun testProductionApiConnection(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isApiTesting.value = true
            _apiHealthStatus.value = "Connecting to ${_apiBaseUrl.value}..."
            when (val result = repository.checkApiHealth()) {
                is com.example.data.network.NetworkResult.Success -> {
                    val health = result.data
                    val statusText = "Healthy (v${health.version}, env: ${health.environment})"
                    _apiHealthStatus.value = statusText
                    _toastMessage.emit("API Connection Successful: $statusText")
                    onResult(true, statusText)
                }
                is com.example.data.network.NetworkResult.Error -> {
                    val errorMsg = result.message
                    _apiHealthStatus.value = "Failed: $errorMsg"
                    _toastMessage.emit("API Test Failed: $errorMsg")
                    onResult(false, errorMsg)
                }
                else -> {
                    _apiHealthStatus.value = "Unknown state"
                }
            }
            _isApiTesting.value = false
        }
    }

    fun syncOrdersWithProductionServer() {
        viewModelScope.launch {
            val deviceId = android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "device-kiosk-01"

            _toastMessage.emit("Syncing orders with production server...")
            when (val result = repository.syncOrdersToServer(deviceId)) {
                is com.example.data.network.NetworkResult.Success -> {
                    val syncData = result.data
                    _toastMessage.emit("Synced ${syncData.syncedCount} orders to production server!")
                }
                is com.example.data.network.NetworkResult.Error -> {
                    _toastMessage.emit("Sync failed: ${result.message}")
                }
                else -> Unit
            }
        }
    }

    fun getSampleSecureHeaders(): Map<String, String> {
        val token = sessionManager.getAuthToken()
        return linkedMapOf(
            "Authorization" to if (token.isNullOrBlank()) "Bearer [Pending Login / Guest]" else "Bearer ${token.take(12)}...${token.takeLast(6)}",
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "X-App-Platform" to "Android",
            "X-App-Version" to "1.0.0",
            "X-Device-ID" to "android-kiosk-${android.os.Build.MODEL.hashCode().toString(16)}",
            "X-Request-ID" to java.util.UUID.randomUUID().toString(),
            "X-Client-Timestamp" to System.currentTimeMillis().toString()
        )
    }

    // --- Firebase Actions ---

    fun testFirebaseConnection() {
        viewModelScope.launch {
            _firebaseStatusMessage.value = "Testing Firestore connection..."
            _toastMessage.emit("Pinging Cloud Firestore...")
            val result = repository.testFirebaseConnection()
            if (result.isSuccess) {
                _firebaseStatusMessage.value = "Connected & Verified"
                _toastMessage.emit("Firebase Connected! Heartbeat recorded in Firestore.")
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown Firebase error"
                _firebaseStatusMessage.value = "Failed: $errorMsg"
                _toastMessage.emit("Firebase connection note: $errorMsg")
            }
        }
    }

    fun syncOrdersWithFirebase() {
        viewModelScope.launch {
            _toastMessage.emit("Syncing print orders to Cloud Firestore...")
            val result = repository.syncOrdersToFirebase()
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _toastMessage.emit("Successfully synced $count orders to Firebase Firestore!")
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Sync failed"
                _toastMessage.emit("Firebase sync error: $errorMsg")
            }
        }
    }

    fun syncPrintersWithFirebase() {
        viewModelScope.launch {
            _toastMessage.emit("Syncing printer fleet status to Firebase...")
            val result = repository.syncPrintersToFirebase()
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _toastMessage.emit("Synced $count printer telemetry nodes to Firestore!")
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Fleet sync failed"
                _toastMessage.emit("Firebase fleet sync note: $errorMsg")
            }
        }
    }

    fun updateFirebaseConfig(projectId: String, apiKey: String, appId: String) {
        viewModelScope.launch {
            _toastMessage.emit("Updating Firebase configuration...")
            val result = firebaseService.updateFirebaseConfig(projectId, apiKey, appId)
            if (result.isSuccess) {
                _toastMessage.emit("Firebase reconfigured with Project: $projectId")
            } else {
                _toastMessage.emit("Failed to update Firebase: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Authenticates with Firebase Auth and synchronizes the session with the local enterprise database.
     */
    fun firebaseLogin(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val authResult = firebaseService.signInWithEmailAndPassword(email, pass)
            if (authResult.isSuccess) {
                val fbUser = authResult.getOrThrow()
                // Find or create matching local user
                val allUsersList = repository.getAllUsers().first()
                var user = allUsersList.find {
                    it.email.equals(fbUser.email, ignoreCase = true) ||
                    it.username.equals(fbUser.email?.substringBefore("@"), ignoreCase = true)
                }

                if (user == null) {
                    val safeUsername = (fbUser.email?.substringBefore("@") ?: "firebase_user_${fbUser.uid.take(5)}").lowercase()
                    val isSuperAdmin = fbUser.email?.contains("kadir", ignoreCase = true) == true ||
                                       fbUser.email?.contains("admin", ignoreCase = true) == true
                    val role = if (isSuperAdmin) UserRole.SUPER_ADMIN.name else UserRole.CUSTOMER.name
                    val newUser = UserEntity(
                        username = safeUsername,
                        fullName = fbUser.displayName ?: safeUsername.replaceFirstChar { it.uppercase() },
                        email = fbUser.email ?: "$safeUsername@masterprinter.cloud",
                        phone = "",
                        role = role,
                        passwordHash = "FIREBASE_AUTH_PROVIDER",
                        status = "ACTIVE"
                    )
                    val insertedId = repository.insertUser(newUser)
                    user = newUser.copy(id = insertedId)
                }

                val authToken = sessionManager.generateSecureToken("firebase_auth_")
                val refreshToken = sessionManager.generateRefreshToken("firebase_rf_")
                sessionManager.saveSession(user, authToken, refreshToken)
                _currentUser.value = user

                // Route to appropriate portal
                when (UserRole.fromString(user.role)) {
                    UserRole.SUPER_ADMIN -> {
                        _currentPortal.value = AppPortal.SUPER_ADMIN
                        _adminScreen.value = AdminNavScreen.DASHBOARD
                    }
                    UserRole.ADMIN -> {
                        _currentPortal.value = AppPortal.ADMIN
                        _adminScreen.value = AdminNavScreen.DASHBOARD
                    }
                    UserRole.MANAGER, UserRole.STAFF -> {
                        _currentPortal.value = AppPortal.STAFF
                    }
                    UserRole.PRINTER_OPERATOR -> {
                        _currentPortal.value = AppPortal.PRINTER_OPERATOR
                    }
                    UserRole.CUSTOMER -> {
                        _currentPortal.value = AppPortal.CUSTOMER
                        _customerScreen.value = CustomerNavScreen.HOME
                    }
                }
                _toastMessage.emit("Firebase login successful! Welcome, ${user.fullName}")
                onResult(true, null)
            } else {
                val error = authResult.exceptionOrNull()?.message ?: "Firebase authentication failed"
                onResult(false, error)
            }
        }
    }

    /**
     * Registers a new user with Firebase Auth and creates their local database record.
     */
    fun firebaseRegister(email: String, pass: String, fullName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val authResult = firebaseService.createUserWithEmailAndPassword(email, pass, fullName)
            if (authResult.isSuccess) {
                val fbUser = authResult.getOrThrow()
                val safeUsername = (fbUser.email?.substringBefore("@") ?: "user_${fbUser.uid.take(5)}").lowercase()
                val newUser = UserEntity(
                    username = safeUsername,
                    fullName = fullName.ifBlank { safeUsername.replaceFirstChar { it.uppercase() } },
                    email = fbUser.email ?: email,
                    phone = "",
                    role = UserRole.CUSTOMER.name,
                    passwordHash = "FIREBASE_AUTH_PROVIDER",
                    status = "ACTIVE"
                )
                val insertedId = repository.insertUser(newUser)
                val user = newUser.copy(id = insertedId)

                val authToken = sessionManager.generateSecureToken("firebase_auth_")
                val refreshToken = sessionManager.generateRefreshToken("firebase_rf_")
                sessionManager.saveSession(user, authToken, refreshToken)
                _currentUser.value = user
                _currentPortal.value = AppPortal.CUSTOMER
                _customerScreen.value = CustomerNavScreen.HOME
                _toastMessage.emit("Firebase account created! Welcome to Master Printer.")
                onResult(true, null)
            } else {
                val error = authResult.exceptionOrNull()?.message ?: "Registration failed"
                onResult(false, error)
            }
        }
    }

    /**
     * Anonymous Firebase Login for Instant Kiosk / Guest Print Access.
     */
    fun firebaseAnonymousLogin(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val authResult = firebaseService.authenticateAnonymously()
            if (authResult.isSuccess) {
                val uid = authResult.getOrThrow()
                val safeUsername = "guest_${uid.take(6).lowercase()}"
                val guestUser = UserEntity(
                    username = safeUsername,
                    fullName = "Guest Kiosk User",
                    email = "$safeUsername@masterprinter.cloud",
                    phone = "",
                    role = UserRole.CUSTOMER.name,
                    passwordHash = "ANONYMOUS_FIREBASE",
                    status = "ACTIVE"
                )
                val insertedId = repository.insertUser(guestUser)
                val user = guestUser.copy(id = insertedId)

                val authToken = sessionManager.generateSecureToken("fb_guest_")
                sessionManager.saveSession(user, authToken, null)
                _currentUser.value = user
                _currentPortal.value = AppPortal.CUSTOMER
                _customerScreen.value = CustomerNavScreen.HOME
                _toastMessage.emit("Connected as Firebase Kiosk Guest!")
                onResult(true, null)
            } else {
                val error = authResult.exceptionOrNull()?.message ?: "Guest login failed"
                onResult(false, error)
            }
        }
    }

    /**
     * Sends a password reset email using Firebase Auth.
     */
    fun sendFirebasePasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = firebaseService.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _toastMessage.emit("Password reset email sent to $email via Firebase")
                onResult(true, null)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                onResult(false, error)
            }
        }
    }

    /**
     * Signs out of Firebase Auth and current session.
     */
    fun firebaseSignOut() {
        viewModelScope.launch {
            firebaseService.signOut()
            logout()
            _toastMessage.emit("Signed out from Firebase Cloud")
        }
    }

    /**
     * Connects directly to Data (Database & Cloud Firestore) and signs in automatically,
     * so user does not need to repeatedly fill credentials.
     */
    fun autoConnectToData(targetScreen: AdminNavScreen = AdminNavScreen.CLOUD_FILES, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // Ensure Firebase connection
                firebaseService.initializeFirebase()

                // Persist auto-login preference so user is not prompted again
                sessionManager.setAutoLoginEnabled(true)

                // Authenticate Super Admin account
                var adminUser = repository.authenticateUser("KADIR143", "ABDUL123")
                if (adminUser == null) {
                    val regResult = repository.registerUser(
                        username = "KADIR143",
                        email = "kadir@masterprinter.cloud",
                        phone = "9876543210",
                        password = "ABDUL123",
                        fullName = "Abdul Kadir (Super Admin)",
                        role = UserRole.SUPER_ADMIN
                    )
                    adminUser = regResult.getOrNull()
                }

                if (adminUser != null) {
                    val token = sessionManager.generateSecureToken("mp_auto_")
                    val refreshToken = sessionManager.generateRefreshToken("mp_auto_rf_")
                    sessionManager.saveSession(adminUser, token, refreshToken)
                    _currentUser.value = adminUser
                    _currentPortal.value = AppPortal.SUPER_ADMIN
                    _adminScreen.value = targetScreen
                    _toastMessage.emit("Connected to Data & Cloud! Logged in as ${adminUser.fullName}")
                }
                onComplete?.invoke()
            } catch (e: Exception) {
                android.util.Log.e("MasterPrinterVM", "Auto connect error: ${e.message}", e)
                onComplete?.invoke()
            }
        }
    }

    /**
     * Publishes a file to Master Printer Cloud & Firebase with public URL and Kiosk QR code.
     */
    fun publishCloudFile(
        title: String,
        fileName: String,
        fileType: String,
        fileSizeKb: Long,
        pageCount: Int,
        isPublic: Boolean = true,
        description: String = "",
        onComplete: (CloudPublicFile) -> Unit
    ) {
        viewModelScope.launch {
            val randomSuffix = (1000..9999).random()
            val fileId = "pub_${System.currentTimeMillis().toString().takeLast(6)}_$randomSuffix"
            val publicPin = "MP-$randomSuffix"
            val publicUrl = "https://masterprinter.cloud/p/$fileId"
            val qrPayload = "MP-CLOUD:$fileId:${fileName.replace(" ", "_")}"
            val user = _currentUser.value

            val newFile = CloudPublicFile(
                id = fileId,
                title = title.ifBlank { fileName },
                fileName = fileName,
                fileType = fileType,
                fileSizeKb = fileSizeKb,
                pageCount = pageCount,
                isPublic = isPublic,
                publicUrl = publicUrl,
                publicPin = publicPin,
                qrCodePayload = qrPayload,
                uploaderName = user?.fullName ?: "Abdul Kadir",
                uploaderRole = user?.role ?: "SUPER_ADMIN",
                description = description
            )

            val result = firebaseService.publishPublicFile(newFile)
            if (result.isSuccess) {
                _toastMessage.emit("File published to cloud! Public Link & Kiosk QR generated.")
                onComplete(newFile)
            }
        }
    }

    fun deleteCloudFile(id: String) {
        viewModelScope.launch {
            firebaseService.deletePublicFile(id)
            _toastMessage.emit("File removed from public cloud.")
        }
    }

    fun togglePublicFileVisibility(id: String, isPublic: Boolean) {
        viewModelScope.launch {
            firebaseService.togglePublicFileVisibility(id, isPublic)
            val msg = if (isPublic) "File is now PUBLIC to all kiosks & customers" else "File is now PRIVATE"
            _toastMessage.emit(msg)
        }
    }

    fun recordFilePrint(id: String) {
        firebaseService.recordFilePrint(id)
    }

    fun recordFileDownload(id: String) {
        firebaseService.recordFileDownload(id)
    }
}
