package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.PrinterEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Authenticated Firebase user profile representation.
 */
data class FirebaseUserInfo(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean,
    val isEmailVerified: Boolean = false
)

/**
 * Publicly shared document or print-ready file hosted on Master Printer Cloud / Firebase.
 */
data class CloudPublicFile(
    val id: String,
    val title: String,
    val fileName: String,
    val fileType: String, // PDF, IMAGE, DOC, BLUEPRINT, VISITING_CARD
    val fileSizeKb: Long,
    val pageCount: Int,
    val isPublic: Boolean = true,
    val publicUrl: String,
    val publicPin: String, // Express 6-character kiosk code e.g. "MP-8834"
    val qrCodePayload: String,
    val uploaderName: String,
    val uploaderRole: String = "Admin",
    val createdAt: Long = System.currentTimeMillis(),
    val downloadCount: Int = 0,
    val printCount: Int = 0,
    val description: String = ""
)

/**
 * Connection states for Firebase Cloud Services.
 */
sealed class FirebaseConnectionState {
    data object Disconnected : FirebaseConnectionState()
    data object Connecting : FirebaseConnectionState()
    data class Connected(
        val projectId: String,
        val appName: String,
        val authUserUid: String? = null,
        val authUserEmail: String? = null
    ) : FirebaseConnectionState()
    data class Error(val message: String) : FirebaseConnectionState()
}

/**
 * Enterprise Firebase Integration Service for MASTER PRINTER.
 *
 * Provides:
 * 1. Automatic & Safe programmatic Firebase App initialization (with custom project fallback).
 * 2. Real-time Cloud Firestore synchronization for Orders and Printer Fleet.
 * 3. Firebase Auth session management and anonymous kiosk authentication.
 * 4. Active connectivity diagnostics and telemetry sync.
 */
class FirebaseService private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("master_printer_firebase_settings", Context.MODE_PRIVATE)

    private val _connectionState = MutableStateFlow<FirebaseConnectionState>(FirebaseConnectionState.Disconnected)
    val connectionState: StateFlow<FirebaseConnectionState> = _connectionState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(prefs.getLong(KEY_LAST_SYNC_TIME, 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _currentUserFlow = MutableStateFlow<FirebaseUserInfo?>(null)
    val currentUserFlow: StateFlow<FirebaseUserInfo?> = _currentUserFlow.asStateFlow()

    private val _publicFilesFlow = MutableStateFlow<List<CloudPublicFile>>(createDefaultPublicFiles())
    val publicFilesFlow: StateFlow<List<CloudPublicFile>> = _publicFilesFlow.asStateFlow()

    private var firebaseApp: FirebaseApp? = null
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var ordersListenerRegistration: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        initializeFirebase()
    }

    /**
     * Initializes Firebase App. Attempts default configuration first (google-services.json),
     * and if not found, falls back to stored or default programmatic FirebaseOptions.
     */
    @Synchronized
    fun initializeFirebase(): Boolean {
        _connectionState.value = FirebaseConnectionState.Connecting
        try {
            // Check if default FirebaseApp already exists
            val existingApps = FirebaseApp.getApps(context)
            firebaseApp = if (existingApps.isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                try {
                    // Try default initialization if google-services.json was provided
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    Log.w(TAG, "Default Firebase initialization not found. Initializing with programmatic options: ${e.message}")
                    null
                }
            }

            // Fallback to custom/programmatic Firebase App if default isn't configured
            if (firebaseApp == null) {
                val projectId = getProjectId()
                val apiKey = getApiKey()
                val appId = getAppId()

                val options = FirebaseOptions.Builder()
                    .setProjectId(projectId)
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .build()

                firebaseApp = FirebaseApp.initializeApp(context, options, APP_NAME)
            }

            val app = firebaseApp ?: throw IllegalStateException("FirebaseApp could not be initialized")

            // Initialize Firestore & Auth
            firestore = FirebaseFirestore.getInstance(app)
            auth = FirebaseAuth.getInstance(app)

            // Setup Auth State Listener
            setupAuthListener()

            val currentUser = auth?.currentUser
            _currentUserFlow.value = currentUser?.let {
                FirebaseUserInfo(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    isAnonymous = it.isAnonymous,
                    isEmailVerified = it.isEmailVerified
                )
            }

            _connectionState.value = FirebaseConnectionState.Connected(
                projectId = app.options.projectId ?: getProjectId(),
                appName = app.name,
                authUserUid = currentUser?.uid,
                authUserEmail = currentUser?.email
            )
            Log.i(TAG, "Firebase initialized successfully on project: ${app.options.projectId}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
            _connectionState.value = FirebaseConnectionState.Error(e.localizedMessage ?: "Unknown Firebase error")
            return false
        }
    }

    private fun setupAuthListener() {
        val currentAuth = auth ?: return
        authStateListener?.let { currentAuth.removeAuthStateListener(it) }
        authStateListener = FirebaseAuth.AuthStateListener { fbAuth ->
            val user = fbAuth.currentUser
            _currentUserFlow.value = user?.let {
                FirebaseUserInfo(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    isAnonymous = it.isAnonymous,
                    isEmailVerified = it.isEmailVerified
                )
            }
            val current = _connectionState.value
            if (current is FirebaseConnectionState.Connected) {
                _connectionState.value = current.copy(
                    authUserUid = user?.uid,
                    authUserEmail = user?.email
                )
            }
        }
        currentAuth.addAuthStateListener(authStateListener!!)
    }

    /**
     * Reconfigures Firebase credentials programmatically (e.g. from Admin Settings dialog).
     */
    suspend fun updateFirebaseConfig(
        projectId: String,
        apiKey: String,
        appId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            prefs.edit()
                .putString(KEY_PROJECT_ID, projectId.trim())
                .putString(KEY_API_KEY, apiKey.trim())
                .putString(KEY_APP_ID, appId.trim())
                .apply()

            // Delete old named app if exists
            try {
                FirebaseApp.getInstance(APP_NAME).delete()
            } catch (_: Exception) {}

            val options = FirebaseOptions.Builder()
                .setProjectId(projectId.trim())
                .setApiKey(apiKey.trim())
                .setApplicationId(appId.trim())
                .build()

            firebaseApp = FirebaseApp.initializeApp(context, options, APP_NAME)
            val app = firebaseApp ?: throw IllegalStateException("Failed to initialize Firebase with updated config")
            firestore = FirebaseFirestore.getInstance(app)
            auth = FirebaseAuth.getInstance(app)
            setupAuthListener()

            val currentUser = auth?.currentUser
            _currentUserFlow.value = currentUser?.let {
                FirebaseUserInfo(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    isAnonymous = it.isAnonymous,
                    isEmailVerified = it.isEmailVerified
                )
            }

            _connectionState.value = FirebaseConnectionState.Connected(
                projectId = projectId.trim(),
                appName = app.name,
                authUserUid = currentUser?.uid,
                authUserEmail = currentUser?.email
            )
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconfigure Firebase", e)
            _connectionState.value = FirebaseConnectionState.Error(e.localizedMessage ?: "Configuration failed")
            Result.failure(e)
        }
    }

    /**
     * Authenticates using Firebase Auth email & password.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUserInfo> = withContext(Dispatchers.IO) {
        try {
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth is not initialized")
            val authResult = authInstance.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw IllegalStateException("No user returned from Firebase Authentication")

            val info = FirebaseUserInfo(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                isAnonymous = user.isAnonymous,
                isEmailVerified = user.isEmailVerified
            )
            _currentUserFlow.value = info
            val current = _connectionState.value
            if (current is FirebaseConnectionState.Connected) {
                _connectionState.value = current.copy(
                    authUserUid = user.uid,
                    authUserEmail = user.email
                )
            }
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signInWithEmailAndPassword failed: ${e.message}", e)
            val cleanMessage = when {
                e.message?.contains("user-not-found", ignoreCase = true) == true -> "No Firebase account found with this email"
                e.message?.contains("wrong-password", ignoreCase = true) == true -> "Incorrect password"
                e.message?.contains("invalid-email", ignoreCase = true) == true -> "Invalid email address format"
                e.message?.contains("user-disabled", ignoreCase = true) == true -> "This Firebase user account has been disabled"
                e.message?.contains("too-many-requests", ignoreCase = true) == true -> "Too many attempts. Please try again later."
                e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true -> "Invalid email or password"
                e.message?.contains("network", ignoreCase = true) == true -> "Network error connecting to Firebase servers"
                else -> e.localizedMessage ?: "Firebase login failed"
            }
            Result.failure(Exception(cleanMessage))
        }
    }

    /**
     * Creates a new user in Firebase Auth with email & password.
     */
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUserInfo> = withContext(Dispatchers.IO) {
        try {
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth is not initialized")
            val authResult = authInstance.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw IllegalStateException("User creation failed in Firebase")

            if (!displayName.isNullOrBlank()) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                try {
                    user.updateProfile(profileUpdates).await()
                } catch (pe: Exception) {
                    Log.w(TAG, "Could not update user profile display name: ${pe.message}")
                }
            }

            val info = FirebaseUserInfo(
                uid = user.uid,
                email = user.email,
                displayName = displayName ?: user.displayName,
                isAnonymous = user.isAnonymous,
                isEmailVerified = user.isEmailVerified
            )
            _currentUserFlow.value = info
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase createUserWithEmailAndPassword failed: ${e.message}", e)
            val cleanMessage = when {
                e.message?.contains("email-already-in-use", ignoreCase = true) == true -> "This email is already registered in Firebase"
                e.message?.contains("weak-password", ignoreCase = true) == true -> "Password is too weak. Must be at least 6 characters."
                e.message?.contains("invalid-email", ignoreCase = true) == true -> "Invalid email address format"
                else -> e.localizedMessage ?: "Firebase user registration failed"
            }
            Result.failure(Exception(cleanMessage))
        }
    }

    /**
     * Sends password reset email through Firebase Auth.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth is not initialized")
            authInstance.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sendPasswordResetEmail failed: ${e.message}", e)
            val cleanMessage = when {
                e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found for this email"
                e.message?.contains("invalid-email", ignoreCase = true) == true -> "Invalid email address format"
                else -> e.localizedMessage ?: "Failed to send password reset email"
            }
            Result.failure(Exception(cleanMessage))
        }
    }

    /**
     * Signs out the current Firebase user.
     */
    fun signOut(): Result<Unit> {
        return try {
            auth?.signOut()
            _currentUserFlow.value = null
            val current = _connectionState.value
            if (current is FirebaseConnectionState.Connected) {
                _connectionState.value = current.copy(authUserUid = null, authUserEmail = null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signOut error", e)
            Result.failure(e)
        }
    }

    fun getCurrentFirebaseUser(): FirebaseUserInfo? = _currentUserFlow.value

    /**
     * Authenticates the kiosk anonymously with Firebase Auth for secure Firestore rules enforcement.
     */
    suspend fun authenticateAnonymously(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authInstance = auth ?: throw IllegalStateException("Firebase Auth not initialized")
            val result = authInstance.signInAnonymously().await()
            val user = result.user
            val uid = user?.uid ?: "anonymous"
            val info = FirebaseUserInfo(
                uid = uid,
                email = null,
                displayName = "Guest Kiosk User",
                isAnonymous = true,
                isEmailVerified = false
            )
            _currentUserFlow.value = info
            val current = _connectionState.value
            if (current is FirebaseConnectionState.Connected) {
                _connectionState.value = current.copy(authUserUid = uid, authUserEmail = null)
            }
            Result.success(uid)
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous auth failed or offline: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Tests Firestore connectivity by reading/writing a lightweight heartbeat document.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: throw IllegalStateException("Cloud Firestore not initialized")
            val heartbeat = hashMapOf(
                "device_id" to android.os.Build.MODEL,
                "timestamp" to System.currentTimeMillis(),
                "status" to "ONLINE",
                "app_version" to "2.4.0",
                "platform" to "Android"
            )

            db.collection(COLLECTION_TELEMETRY)
                .document("heartbeat_${android.os.Build.MODEL.replace("\\s+".toRegex(), "_")}")
                .set(heartbeat, SetOptions.merge())
                .await()

            val now = System.currentTimeMillis()
            _lastSyncTime.value = now
            prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()

            Result.success("Firestore connected successfully. Telemetry heartbeat written.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase connection test failed", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizes a single order to Cloud Firestore collection "orders".
     */
    suspend fun syncOrderToFirestore(order: OrderEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: throw IllegalStateException("Cloud Firestore not initialized")
            val orderMap = hashMapOf(
                "id" to order.id,
                "order_number" to order.orderNumber,
                "customer_name" to order.customerName,
                "customer_phone" to order.customerPhone,
                "service_name" to order.serviceName,
                "paper_size" to order.paperSize,
                "is_color" to order.isColor,
                "is_duplex" to order.isDuplex,
                "copies" to order.copies,
                "page_count" to order.pageCount,
                "calculated_price" to order.calculatedPrice,
                "final_amount" to order.finalAmount,
                "payment_status" to order.paymentStatus,
                "print_status" to order.printStatus,
                "created_at" to order.createdAt,
                "updated_at" to System.currentTimeMillis(),
                "synced_from" to "Kiosk Android Tablet"
            )

            db.collection(COLLECTION_ORDERS)
                .document(order.orderNumber)
                .set(orderMap, SetOptions.merge())
                .await()

            Result.success(order.orderNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync order ${order.orderNumber} to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizes a list of orders in batch to Cloud Firestore.
     */
    suspend fun syncOrdersBatchToFirestore(orders: List<OrderEntity>): Result<Int> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val db = firestore ?: throw IllegalStateException("Cloud Firestore not initialized")
            var count = 0
            val batch = db.batch()

            for (order in orders) {
                val docRef = db.collection(COLLECTION_ORDERS).document(order.orderNumber)
                val orderMap = hashMapOf(
                    "id" to order.id,
                    "order_number" to order.orderNumber,
                    "customer_name" to order.customerName,
                    "customer_phone" to order.customerPhone,
                    "service_name" to order.serviceName,
                    "paper_size" to order.paperSize,
                    "is_color" to order.isColor,
                    "is_duplex" to order.isDuplex,
                    "copies" to order.copies,
                    "page_count" to order.pageCount,
                    "calculated_price" to order.calculatedPrice,
                    "final_amount" to order.finalAmount,
                    "payment_status" to order.paymentStatus,
                    "print_status" to order.printStatus,
                    "created_at" to order.createdAt,
                    "updated_at" to System.currentTimeMillis(),
                    "synced_from" to "Kiosk Android Tablet"
                )
                batch.set(docRef, orderMap, SetOptions.merge())
                count++
            }

            batch.commit().await()

            val now = System.currentTimeMillis()
            _lastSyncTime.value = now
            prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()

            Log.i(TAG, "Successfully synced $count orders to Firestore")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Batch sync to Firestore failed", e)
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Synchronizes printer hardware status to Cloud Firestore collection "printers".
     */
    suspend fun syncPrinterFleetToFirestore(printers: List<PrinterEntity>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: throw IllegalStateException("Cloud Firestore not initialized")
            val batch = db.batch()
            for (printer in printers) {
                val docRef = db.collection(COLLECTION_PRINTERS).document(printer.id.toString())
                val map = hashMapOf(
                    "id" to printer.id,
                    "printer_identifier" to printer.printerIdentifier,
                    "name" to printer.name,
                    "manufacturer" to printer.manufacturer,
                    "model" to printer.model,
                    "connection_type" to printer.connectionType,
                    "ip_address" to printer.ipAddress,
                    "port" to printer.port,
                    "location" to printer.location,
                    "status" to printer.status,
                    "is_enabled" to printer.isEnabled,
                    "is_default" to printer.isDefault,
                    "last_seen" to printer.lastSeen,
                    "updated_at" to System.currentTimeMillis()
                )
                batch.set(docRef, map, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(printers.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync printer fleet to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Listens to real-time Cloud Firestore updates in the "orders" collection.
     */
    fun startRealtimeOrdersListener(
        onOrdersChanged: (List<Map<String, Any>>) -> Unit
    ) {
        stopRealtimeOrdersListener()
        val db = firestore ?: return
        ordersListenerRegistration = db.collection(COLLECTION_ORDERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore snapshot listener encountered error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orderDocs = snapshot.documents.mapNotNull { it.data }
                    onOrdersChanged(orderDocs)
                }
            }
    }

    fun stopRealtimeOrdersListener() {
        ordersListenerRegistration?.remove()
        ordersListenerRegistration = null
    }

    /**
     * Publishes a document or print file to Master Printer Cloud & Firestore.
     */
    suspend fun publishPublicFile(file: CloudPublicFile): Result<CloudPublicFile> = withContext(Dispatchers.IO) {
        try {
            // Update in-memory reactive flow
            val updated = _publicFilesFlow.value.toMutableList()
            val existingIndex = updated.indexOfFirst { it.id == file.id }
            if (existingIndex >= 0) {
                updated[existingIndex] = file
            } else {
                updated.add(0, file)
            }
            _publicFilesFlow.value = updated

            // Sync to Firestore collection "public_files" if connected
            firestore?.let { db ->
                val fileMap = hashMapOf(
                    "id" to file.id,
                    "title" to file.title,
                    "file_name" to file.fileName,
                    "file_type" to file.fileType,
                    "file_size_kb" to file.fileSizeKb,
                    "page_count" to file.pageCount,
                    "is_public" to file.isPublic,
                    "public_url" to file.publicUrl,
                    "public_pin" to file.publicPin,
                    "qr_code_payload" to file.qrCodePayload,
                    "uploader_name" to file.uploaderName,
                    "uploader_role" to file.uploaderRole,
                    "created_at" to file.createdAt,
                    "download_count" to file.downloadCount,
                    "print_count" to file.printCount,
                    "description" to file.description
                )
                db.collection(COLLECTION_PUBLIC_FILES)
                    .document(file.id)
                    .set(fileMap, SetOptions.merge())
                    .await()
            }

            Result.success(file)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync for public file ${file.id} skipped: ${e.message}")
            // Return success even if offline so the file is stored locally in cloud cache
            Result.success(file)
        }
    }

    /**
     * Deletes a public file from Cloud Firestore and local state.
     */
    suspend fun deletePublicFile(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _publicFilesFlow.value = _publicFilesFlow.value.filterNot { it.id == id }
            firestore?.collection(COLLECTION_PUBLIC_FILES)?.document(id)?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete public file $id from Firestore", e)
            Result.success(Unit)
        }
    }

    /**
     * Toggles whether a published document is publicly accessible or private.
     */
    suspend fun togglePublicFileVisibility(id: String, isPublic: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _publicFilesFlow.value = _publicFilesFlow.value.map {
                if (it.id == id) it.copy(isPublic = isPublic) else it
            }
            firestore?.collection(COLLECTION_PUBLIC_FILES)?.document(id)
                ?.update("is_public", isPublic)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore update visibility error for $id: ${e.message}")
            Result.success(Unit)
        }
    }

    fun recordFilePrint(id: String) {
        _publicFilesFlow.value = _publicFilesFlow.value.map {
            if (it.id == id) it.copy(printCount = it.printCount + 1) else it
        }
    }

    fun recordFileDownload(id: String) {
        _publicFilesFlow.value = _publicFilesFlow.value.map {
            if (it.id == id) it.copy(downloadCount = it.downloadCount + 1) else it
        }
    }

    private fun createDefaultPublicFiles(): List<CloudPublicFile> {
        return listOf(
            CloudPublicFile(
                id = "pub_catalog_2026",
                title = "Master Printer Corporate Catalog 2026",
                fileName = "Master_Printer_Corporate_Catalog_2026.pdf",
                fileType = "PDF",
                fileSizeKb = 4200,
                pageCount = 12,
                isPublic = true,
                publicUrl = "https://masterprinter.cloud/p/pub_catalog_2026",
                publicPin = "MP-2026",
                qrCodePayload = "MP-CLOUD:pub_catalog_2026",
                uploaderName = "Abdul Kadir (Super Admin)",
                uploaderRole = "SUPER_ADMIN",
                downloadCount = 84,
                printCount = 31,
                description = "Official 2026 service menu, bulk volume pricing tiers, and paper specimen catalog."
            ),
            CloudPublicFile(
                id = "pub_blueprint_spec",
                title = "Architectural Elevation Plan A1 Drawing",
                fileName = "Architectural_Elevation_Plan_A1.pdf",
                fileType = "BLUEPRINT",
                fileSizeKb = 8600,
                pageCount = 2,
                isPublic = true,
                publicUrl = "https://masterprinter.cloud/p/pub_blueprint_spec",
                publicPin = "MP-4912",
                qrCodePayload = "MP-CLOUD:pub_blueprint_spec",
                uploaderName = "Abdul Kadir (Super Admin)",
                uploaderRole = "SUPER_ADMIN",
                downloadCount = 42,
                printCount = 18,
                description = "Ultra high-resolution engineering blueprint specimen formatted for A1/A0 Plotters."
            ),
            CloudPublicFile(
                id = "pub_visiting_card",
                title = "Executive Visiting Card Embossed Specimen",
                fileName = "Executive_Visiting_Card_Embossed.pdf",
                fileType = "VISITING_CARD",
                fileSizeKb = 1250,
                pageCount = 1,
                isPublic = true,
                publicUrl = "https://masterprinter.cloud/p/pub_visiting_card",
                publicPin = "MP-7814",
                qrCodePayload = "MP-CLOUD:pub_visiting_card",
                uploaderName = "Kadir Admin",
                uploaderRole = "ADMIN",
                downloadCount = 115,
                printCount = 76,
                description = "Vector 350 GSM matte finish corporate business card layout with spot UV layers."
            )
        )
    }

    fun getProjectId(): String = prefs.getString(KEY_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
    fun getApiKey(): String = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    fun getAppId(): String = prefs.getString(KEY_APP_ID, DEFAULT_APP_ID) ?: DEFAULT_APP_ID

    companion object {
        private const val TAG = "FirebaseService"
        private const val APP_NAME = "MasterPrinterApp"

        const val COLLECTION_ORDERS = "orders"
        const val COLLECTION_PRINTERS = "printers"
        const val COLLECTION_TELEMETRY = "telemetry"
        const val COLLECTION_PUBLIC_FILES = "public_files"

        private const val KEY_PROJECT_ID = "firebase_project_id"
        private const val KEY_API_KEY = "firebase_api_key"
        private const val KEY_APP_ID = "firebase_app_id"
        private const val KEY_LAST_SYNC_TIME = "firebase_last_sync_time"

        // Default project configuration for Master Printer Cloud
        const val DEFAULT_PROJECT_ID = "master-printer-cloud"
        const val DEFAULT_API_KEY = "AIzaSyB_MasterPrinterLiveKey98213"
        const val DEFAULT_APP_ID = "1:449753653790:android:masterprinter"

        @Volatile
        private var INSTANCE: FirebaseService? = null

        fun getInstance(context: Context): FirebaseService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
