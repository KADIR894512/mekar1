package com.example.data.session

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Biometric authentication capability status.
 */
sealed class BiometricStatus {
    data object Available : BiometricStatus()
    data class Unavailable(val reason: String) : BiometricStatus()
}

/**
 * BiometricAuthManager
 *
 * Wraps AndroidX BiometricPrompt to provide modern fingerprint, face recognition,
 * and device credential (PIN/pattern/password) authentication.
 */
class BiometricAuthManager(private val context: Context) {

    private val biometricManager: BiometricManager = BiometricManager.from(context)

    /**
     * Checks if biometric hardware and enrollment are available on this device.
     */
    fun checkBiometricAvailability(): BiometricStatus {
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricStatus.Available
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricStatus.Unavailable("No biometric hardware (fingerprint/face) detected on device")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricStatus.Unavailable("Biometric sensors are currently unavailable")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricStatus.Unavailable("No fingerprint or face unlock enrolled on device. Set one in Settings.")
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricStatus.Unavailable("Security update required for biometric authentication")
            }
            else -> {
                BiometricStatus.Unavailable("Biometric authentication unsupported or disabled")
            }
        }
    }

    /**
     * Checks whether fingerprint or face authentication is actively enrolled and usable.
     */
    fun isBiometricReady(): Boolean {
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        val result = biometricManager.canAuthenticate(authenticators)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows standard Android BiometricPrompt for fingerprint or face authentication.
     * Supports device credential fallback (PIN/pattern/password).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Verify fingerprint or face to open Master Printer",
        description: String = "Place your finger on sensor or look at the camera",
        allowDeviceCredentials: Boolean = true,
        negativeButtonText: String = "Use App Password",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i(TAG, "Biometric authentication succeeded")
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "Biometric authentication error [$errorCode]: $errString")
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric verification failed (not recognized)")
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        if (allowDeviceCredentials && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ supports combining BIOMETRIC with DEVICE_CREDENTIAL
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else if (allowDeviceCredentials && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            promptInfoBuilder.setNegativeButtonText(negativeButtonText)
        }

        val promptInfo = promptInfoBuilder.build()
        prompt.authenticate(promptInfo)
    }

    companion object {
        private const val TAG = "BiometricAuthManager"

        @Volatile
        private var INSTANCE: BiometricAuthManager? = null

        fun getInstance(context: Context): BiometricAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BiometricAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
