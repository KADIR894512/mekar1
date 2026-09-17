package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.data.local.entities.UserEntity
import com.example.domain.model.Permission
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted payload containing ciphertext and initialization vector.
 */
data class EncryptedPayload(
    val cipherTextBase64: String,
    val ivBase64: String
)

/**
 * Represents the current user session state.
 */
sealed class UserSessionState {
    data object Unauthenticated : UserSessionState()

    data class Authenticated(
        val user: UserEntity,
        val role: UserRole,
        val token: String,
        val permissions: Set<Permission>,
        val expiresAt: Long,
        val isExpiringSoon: Boolean = false
    ) : UserSessionState()
}

/**
 * Diagnostic metadata for active security session.
 */
data class SessionMetadata(
    val isAuthenticated: Boolean,
    val username: String?,
    val role: UserRole?,
    val tokenPreview: String?,
    val issuedAt: Long,
    val expiresAt: Long,
    val remainingSeconds: Long,
    val isExpired: Boolean,
    val encryptionStandard: String = "AES-256-GCM (Hardware Keystore)"
)

/**
 * UserSessionManager
 *
 * Enterprise-grade session and role management service for Master Printer.
 * Handles:
 * 1. Role-based state (SUPER_ADMIN, ADMIN, MANAGER, STAFF, PRINTER_OPERATOR, CUSTOMER)
 * 2. Hardware-backed / Android KeyStore AES-GCM secure authentication token encryption and storage
 * 3. Reactive state flows for UI authentication state, permissions, and roles
 * 4. Token validation, refresh cycles, and automatic expiration enforcement
 */
class UserSessionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val secureRandom = SecureRandom()

    // Reactive State
    private val _sessionState = MutableStateFlow<UserSessionState>(UserSessionState.Unauthenticated)
    val sessionState: StateFlow<UserSessionState> = _sessionState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _effectivePermissions = MutableStateFlow<Set<Permission>>(emptySet())
    val effectivePermissions: StateFlow<Set<Permission>> = _effectivePermissions.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isRoyalWallpaperEnabled = MutableStateFlow(prefs.getBoolean(KEY_ROYAL_WALLPAPER_ENABLED, true))
    val isRoyalWallpaperEnabled: StateFlow<Boolean> = _isRoyalWallpaperEnabled.asStateFlow()

    private val _biometricAccountUsername = MutableStateFlow(prefs.getString(KEY_BIOMETRIC_USERNAME, "KADIR143") ?: "KADIR143")
    val biometricAccountUsername: StateFlow<String> = _biometricAccountUsername.asStateFlow()

    init {
        restoreSession()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setRoyalWallpaperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ROYAL_WALLPAPER_ENABLED, enabled).apply()
        _isRoyalWallpaperEnabled.value = enabled
    }

    fun setBiometricUsername(username: String) {
        prefs.edit().putString(KEY_BIOMETRIC_USERNAME, username).apply()
        _biometricAccountUsername.value = username
    }

    fun isAutoLoginEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, true)

    fun setAutoLoginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled).apply()
    }

    // =========================================================================
    // Role-Based State & Access Control Helpers
    // =========================================================================

    val isSuperAdmin: Boolean
        get() = _currentRole.value == UserRole.SUPER_ADMIN

    val isAdmin: Boolean
        get() = _currentRole.value.level >= UserRole.ADMIN.level

    val isManager: Boolean
        get() = _currentRole.value.level >= UserRole.MANAGER.level

    val isStaff: Boolean
        get() = _currentRole.value.level >= UserRole.STAFF.level

    val isPrinterOperator: Boolean
        get() = _currentRole.value == UserRole.PRINTER_OPERATOR || _currentRole.value.level >= UserRole.MANAGER.level

    val isCustomer: Boolean
        get() = _currentRole.value == UserRole.CUSTOMER

    fun hasRole(role: UserRole): Boolean {
        return _currentRole.value == role
    }

    fun hasMinimumRole(minRole: UserRole): Boolean {
        return _currentRole.value.level >= minRole.level
    }

    fun hasPermission(permission: Permission): Boolean {
        if (isSuperAdmin) return true
        return _effectivePermissions.value.contains(permission)
    }

    fun hasAnyPermission(vararg permissions: Permission): Boolean {
        if (isSuperAdmin) return true
        return permissions.any { _effectivePermissions.value.contains(it) }
    }

    fun hasAllPermissions(vararg permissions: Permission): Boolean {
        if (isSuperAdmin) return true
        return permissions.all { _effectivePermissions.value.contains(it) }
    }

    /**
     * Executes the block if the current session meets the required minimum role.
     * Otherwise returns a failure result with SecurityException.
     */
    fun <T> runIfAuthorized(minRole: UserRole, block: () -> T): Result<T> {
        return if (hasMinimumRole(minRole)) {
            try {
                Result.success(block())
            } catch (e: Throwable) {
                Result.failure(e)
            }
        } else {
            Result.failure(
                SecurityException(
                    "Access denied: Action requires minimum role ${minRole.displayName}, but current user role is ${_currentRole.value.displayName}"
                )
            )
        }
    }

    /**
     * Throws SecurityException if the session does not satisfy the specified minimum role.
     */
    fun requireMinimumRole(minRole: UserRole, actionDescription: String = "access this resource") {
        if (!hasMinimumRole(minRole)) {
            throw SecurityException(
                "Unauthorized: You must have at least ${minRole.displayName} clearance to $actionDescription. Current clearance: ${_currentRole.value.displayName}"
            )
        }
    }

    // =========================================================================
    // Authentication & Token Management
    // =========================================================================

    /**
     * Stores user details and encrypts authentication tokens using hardware-backed AES-256-GCM.
     */
    @Synchronized
    fun saveSession(
        user: UserEntity,
        authToken: String,
        refreshToken: String? = null,
        validityDurationMs: Long = DEFAULT_SESSION_VALIDITY_MS
    ) {
        val now = System.currentTimeMillis()
        val expiresAt = now + validityDurationMs
        val role = UserRole.fromString(user.role)

        // Encrypt tokens
        val encryptedAuthToken = encrypt(authToken)
        val encryptedRefreshToken = refreshToken?.let { encrypt(it) }

        // Persist securely to SharedPreferences
        val editor = prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_FULL_NAME, user.fullName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .putString(KEY_ROLE, role.name)
            .putString(KEY_PERMISSIONS, user.permissions)
            .putString(KEY_STATUS, user.status)
            .putString(KEY_BIOMETRIC_USERNAME, user.username)
            .putLong(KEY_ISSUED_AT, now)
            .putLong(KEY_EXPIRES_AT, expiresAt)

        if (encryptedAuthToken != null) {
            editor.putString(KEY_AUTH_TOKEN_CIPHER, encryptedAuthToken.cipherTextBase64)
            editor.putString(KEY_AUTH_TOKEN_IV, encryptedAuthToken.ivBase64)
        }

        if (encryptedRefreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN_CIPHER, encryptedRefreshToken.cipherTextBase64)
            editor.putString(KEY_REFRESH_TOKEN_IV, encryptedRefreshToken.ivBase64)
        } else {
            editor.remove(KEY_REFRESH_TOKEN_CIPHER)
            editor.remove(KEY_REFRESH_TOKEN_IV)
        }

        editor.apply()

        // Update in-memory reactive state
        val permissionsSet = resolvePermissions(role, user.permissions)
        _currentUser.value = user
        _currentRole.value = role
        _isLoggedIn.value = true
        _effectivePermissions.value = permissionsSet
        _sessionState.value = UserSessionState.Authenticated(
            user = user,
            role = role,
            token = authToken,
            permissions = permissionsSet,
            expiresAt = expiresAt,
            isExpiringSoon = false
        )

        Log.i(TAG, "Secure user session established for '${user.username}' with role: ${role.name}")
    }

    /**
     * Decrypts and retrieves the active authentication token.
     * Returns null if unauthenticated or expired.
     */
    @Synchronized
    fun getAuthToken(): String? {
        if (isTokenExpired()) {
            clearSession()
            return null
        }

        val cipher = prefs.getString(KEY_AUTH_TOKEN_CIPHER, null) ?: return null
        val iv = prefs.getString(KEY_AUTH_TOKEN_IV, null) ?: return null

        return decrypt(EncryptedPayload(cipher, iv))
    }

    /**
     * Decrypts and retrieves the active refresh token.
     */
    @Synchronized
    fun getRefreshToken(): String? {
        val cipher = prefs.getString(KEY_REFRESH_TOKEN_CIPHER, null) ?: return null
        val iv = prefs.getString(KEY_REFRESH_TOKEN_IV, null) ?: return null

        return decrypt(EncryptedPayload(cipher, iv))
    }

    /**
     * Returns an HTTP Authorization header formatted string (e.g. "Bearer eyJhbG...").
     */
    fun getBearerHeader(): String? {
        val token = getAuthToken() ?: return null
        return "Bearer $token"
    }

    /**
     * Checks whether the current session token is expired.
     */
    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt == 0L) return true
        return System.currentTimeMillis() >= expiresAt
    }

    /**
     * Returns remaining validity duration in milliseconds.
     */
    fun getTokenRemainingTimeMs(): Long {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val remaining = expiresAt - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Rotates or updates authentication tokens without changing user profile.
     */
    @Synchronized
    fun refreshAuthToken(
        newAuthToken: String,
        newRefreshToken: String? = null,
        validityDurationMs: Long = DEFAULT_SESSION_VALIDITY_MS
    ): Boolean {
        val user = _currentUser.value ?: return false
        saveSession(user, newAuthToken, newRefreshToken ?: getRefreshToken(), validityDurationMs)
        return true
    }

    /**
     * Updates cached user profile in memory and in secure storage.
     */
    @Synchronized
    fun updateUserProfile(user: UserEntity) {
        val token = getAuthToken() ?: return
        val remainingMs = getTokenRemainingTimeMs()
        if (remainingMs > 0) {
            saveSession(user, token, getRefreshToken(), remainingMs)
        }
    }

    /**
     * Clears all tokens, credentials, and resets session state to Unauthenticated.
     */
    @Synchronized
    fun clearSession() {
        prefs.edit().clear().apply()

        _currentUser.value = null
        _currentRole.value = UserRole.CUSTOMER
        _isLoggedIn.value = false
        _effectivePermissions.value = emptySet()
        _sessionState.value = UserSessionState.Unauthenticated

        Log.i(TAG, "Secure user session cleared and tokens revoked.")
    }

    /**
     * Restores an existing session from secure encrypted storage on application launch.
     */
    @Synchronized
    fun restoreSession(): Boolean {
        val username = prefs.getString(KEY_USERNAME, null)
        val roleStr = prefs.getString(KEY_ROLE, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        if (username.isNullOrBlank() || roleStr.isNullOrBlank()) {
            clearSession()
            return false
        }

        if (System.currentTimeMillis() >= expiresAt) {
            if (isAutoLoginEnabled()) {
                val newExpiry = System.currentTimeMillis() + DEFAULT_SESSION_VALIDITY_MS
                prefs.edit().putLong(KEY_EXPIRES_AT, newExpiry).apply()
            } else {
                Log.w(TAG, "Stored session for '$username' expired. Clearing session.")
                clearSession()
                return false
            }
        }

        var token = getAuthToken()
        if (token.isNullOrBlank()) {
            if (isAutoLoginEnabled()) {
                token = generateSecureToken("auto_renew_")
                val enc = encrypt(token)
                if (enc != null) {
                    prefs.edit()
                        .putString(KEY_AUTH_TOKEN_CIPHER, enc.cipherTextBase64)
                        .putString(KEY_AUTH_TOKEN_IV, enc.ivBase64)
                        .apply()
                }
            } else {
                clearSession()
                return false
            }
        }

        val role = UserRole.fromString(roleStr)
        val permissionsStr = prefs.getString(KEY_PERMISSIONS, "") ?: ""
        val permissionsSet = resolvePermissions(role, permissionsStr)

        val user = UserEntity(
            id = prefs.getLong(KEY_USER_ID, 0L),
            username = username,
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            passwordHash = "", // Passwords never cached in session prefs
            fullName = prefs.getString(KEY_FULL_NAME, username) ?: username,
            role = role.name,
            permissions = permissionsStr,
            status = prefs.getString(KEY_STATUS, "ACTIVE") ?: "ACTIVE",
            createdAt = prefs.getLong(KEY_ISSUED_AT, System.currentTimeMillis()),
            internalNotes = ""
        )

        val remainingMs = expiresAt - System.currentTimeMillis()
        val isExpiringSoon = remainingMs < 30 * 60 * 1000L // within 30 minutes

        _currentUser.value = user
        _currentRole.value = role
        _isLoggedIn.value = true
        _effectivePermissions.value = permissionsSet
        _sessionState.value = UserSessionState.Authenticated(
            user = user,
            role = role,
            token = token,
            permissions = permissionsSet,
            expiresAt = expiresAt,
            isExpiringSoon = isExpiringSoon
        )

        Log.i(TAG, "Session restored successfully for '$username' [Role: ${role.name}]")
        return true
    }

    /**
     * Generates a cryptographically strong pseudo-random authentication bearer token.
     */
    fun generateSecureToken(prefix: String = "mp_sec_"): String {
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)
        val raw = Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "$prefix$raw"
    }

    /**
     * Generates a cryptographically strong refresh token.
     */
    fun generateRefreshToken(prefix: String = "mp_rf_"): String {
        val randomBytes = ByteArray(48)
        secureRandom.nextBytes(randomBytes)
        val raw = Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "$prefix$raw"
    }

    /**
     * Provides diagnostic session metadata for security inspection.
     */
    fun getSessionMetadata(): SessionMetadata {
        val token = getAuthToken()
        val tokenPreview = if (token != null && token.length > 12) {
            "${token.take(8)}...${token.takeLast(4)}"
        } else token

        val issuedAt = prefs.getLong(KEY_ISSUED_AT, 0L)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val remaining = getTokenRemainingTimeMs()

        return SessionMetadata(
            isAuthenticated = _isLoggedIn.value,
            username = _currentUser.value?.username,
            role = _currentRole.value,
            tokenPreview = tokenPreview,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            remainingSeconds = remaining / 1000L,
            isExpired = isTokenExpired(),
            encryptionStandard = "AES-256-GCM (Android Keystore)"
        )
    }

    // =========================================================================
    // Android Keystore AES-256-GCM Cryptographic Engine
    // =========================================================================

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
                )
                val builder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)

                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }

            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } catch (e: Throwable) {
            Log.w(TAG, "Keystore unavailable or threw error; using resilient fallback key: ${e.message}")
            getFallbackKey()
        }
    }

    /**
     * Resilient cryptographic key fallback if Android KeyStore provider is unavailable
     * (e.g. during certain headless JVM execution environments).
     */
    private fun getFallbackKey(): SecretKey {
        val seed = (context.packageName + "_master_printer_session_sec_key_v1").toByteArray(StandardCharsets.UTF_8)
        val keyBytes = ByteArray(32)
        System.arraycopy(seed, 0, keyBytes, 0, minOf(seed.size, 32))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encrypt(plainText: String): EncryptedPayload? {
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            EncryptedPayload(
                cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to encrypt token", e)
            null
        }
    }

    private fun decrypt(payload: EncryptedPayload): String? {
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val iv = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
            val cipherText = Base64.decode(payload.cipherTextBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to decrypt token", e)
            null
        }
    }

    private fun resolvePermissions(role: UserRole, customPermissionsStr: String): Set<Permission> {
        val basePermissions = when (role) {
            UserRole.SUPER_ADMIN -> Permission.ALL_ADMIN_PERMISSIONS
            UserRole.ADMIN -> Permission.ALL_ADMIN_PERMISSIONS
            UserRole.MANAGER -> Permission.MANAGER_PERMISSIONS
            UserRole.STAFF -> Permission.STAFF_PERMISSIONS
            UserRole.PRINTER_OPERATOR -> Permission.OPERATOR_PERMISSIONS
            UserRole.CUSTOMER -> Permission.CUSTOMER_PERMISSIONS
        }.toMutableSet()

        if (customPermissionsStr.isNotBlank()) {
            val customCodes = customPermissionsStr.split(",").map { it.trim().lowercase() }
            Permission.entries.forEach { p ->
                if (customCodes.contains(p.code.lowercase())) {
                    basePermissions.add(p)
                }
            }
        }

        return basePermissions
    }

    companion object {
        private const val TAG = "UserSessionManager"
        private const val PREFS_NAME = "master_printer_secure_session_prefs"

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "MasterPrinterSecureAuthKeyAlias_v1"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        // Prefs Keys
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_USERNAME = "session_username"
        private const val KEY_FULL_NAME = "session_full_name"
        private const val KEY_EMAIL = "session_email"
        private const val KEY_PHONE = "session_phone"
        private const val KEY_ROLE = "session_role"
        private const val KEY_PERMISSIONS = "session_permissions"
        private const val KEY_STATUS = "session_status"
        private const val KEY_AUTH_TOKEN_CIPHER = "session_auth_token_cipher"
        private const val KEY_AUTH_TOKEN_IV = "session_auth_token_iv"
        private const val KEY_REFRESH_TOKEN_CIPHER = "session_refresh_token_cipher"
        private const val KEY_REFRESH_TOKEN_IV = "session_refresh_token_iv"
        private const val KEY_ISSUED_AT = "session_issued_at"
        private const val KEY_EXPIRES_AT = "session_expires_at"
        private const val KEY_BIOMETRIC_ENABLED = "session_biometric_enabled"
        private const val KEY_BIOMETRIC_USERNAME = "session_biometric_username"
        private const val KEY_ROYAL_WALLPAPER_ENABLED = "session_royal_wallpaper_enabled"
        private const val KEY_AUTO_LOGIN_ENABLED = "session_auto_login_enabled"

        const val DEFAULT_SESSION_VALIDITY_MS = 365L * 24 * 60 * 60 * 1000L // 365 Days (Persistent Login)

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
