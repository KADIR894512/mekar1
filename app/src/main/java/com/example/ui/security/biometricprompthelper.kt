package com.example.ui.security

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
import java.util.concurrent.Executor

/**
 * Result state for biometric authentication operations.
 */
sealed class BiometricAuthResult {
    /**
     * User successfully verified via biometric (fingerprint/face) or device credentials.
     */
    data class Success(val result: BiometricPrompt.AuthenticationResult) : BiometricAuthResult()

    /**
     * An error occurred (e.g. timeout, sensor lock, user canceled, or no biometrics enrolled).
     */
    data class Error(
        val errorCode: Int,
        val errorMessage: CharSequence,
        val isUserCanceled: Boolean
    ) : BiometricAuthResult()

    /**
     * Biometric was presented but not recognized (e.g. unknown fingerprint).
     */
    data object Failed : BiometricAuthResult()
}

/**
 * Encapsulates the logic for triggering and handling Android BiometricPrompt
 * authentication results for sensitive operations such as admin privilege changes,
 * payment verifications, hardware fleet control, and job routing.
 */
class BiometricPromptHelper(
    private val activity: FragmentActivity,
    private val executor: Executor = ContextCompat.getMainExecutor(activity)
) {
    private val biometricManager = BiometricManager.from(activity)

    /**
     * Checks if biometric hardware is present, enabled, and has enrolled credentials.
     */
    fun canAuthenticate(allowDeviceCredentials: Boolean = true): Boolean {
        val authenticators = if (allowDeviceCredentials) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK
        }
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Returns detailed availability status or specific reason if unavailable.
     */
    fun checkBiometricStatus(allowDeviceCredentials: Boolean = true): BiometricStatusResult {
        val authenticators = if (allowDeviceCredentials) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK
        }
        return when (val status = biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatusResult.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricStatusResult.Unavailable("No biometric hardware present on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricStatusResult.Unavailable("Biometric sensors are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricStatusResult.Unavailable("No fingerprints or credentials enrolled. Please enroll in device Settings.")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricStatusResult.Unavailable("Security update required to use biometric sensors.")
            else -> BiometricStatusResult.Unavailable("Biometric authentication is not supported (status code $status).")
        }
    }

    /**
     * Triggers a BiometricPrompt tailored for sensitive operations with unified result callback.
     *
     * @param title Title displayed at the top of the system prompt dialog.
     * @param subtitle Subtitle specifying the context of the sensitive action.
     * @param description Informational description detailing the verification reason.
     * @param negativeButtonText Label for cancel button (used if device credentials not allowed).
     * @param allowDeviceCredentials Whether PIN, pattern, or device password can fallback.
     * @param cryptoObject Optional Cryptographic object for signature/cipher operations.
     * @param onResult Callback delivering the final [BiometricAuthResult].
     */
    fun authenticateSensitiveOperation(
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel",
        allowDeviceCredentials: Boolean = true,
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded for operation: $title")
                onResult(BiometricAuthResult.Success(result))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val isCanceled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                Log.w(TAG, "Biometric authentication error [$errorCode]: $errString (canceled: $isCanceled)")
                onResult(
                    BiometricAuthResult.Error(
                        errorCode = errorCode,
                        errorMessage = errString,
                        isUserCanceled = isCanceled
                    )
                )
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed: credential not recognized")
                onResult(BiometricAuthResult.Failed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)

        subtitle?.let { promptInfoBuilder.setSubtitle(it) }
        description?.let { promptInfoBuilder.setDescription(it) }

        if (allowDeviceCredentials && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else if (allowDeviceCredentials && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            promptInfoBuilder.setNegativeButtonText(negativeButtonText)
        }

        val promptInfo = promptInfoBuilder.build()

        if (cryptoObject != null) {
            prompt.authenticate(promptInfo, cryptoObject)
        } else {
            prompt.authenticate(promptInfo)
        }
    }

    /**
     * Convenience method for sensitive actions with separate lambdas for verified, error, and failed.
     */
    fun authenticate(
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel",
        allowDeviceCredentials: Boolean = true,
        onVerified: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errorMessage: CharSequence, isUserCanceled: Boolean) -> Unit = { _, _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        authenticateSensitiveOperation(
            title = title,
            subtitle = subtitle,
            description = description,
            negativeButtonText = negativeButtonText,
            allowDeviceCredentials = allowDeviceCredentials
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> onVerified(result.result)
                is BiometricAuthResult.Error -> onError(result.errorCode, result.errorMessage, result.isUserCanceled)
                is BiometricAuthResult.Failed -> onFailed()
            }
        }
    }

    companion object {
        private const val TAG = "BiometricPromptHelper"

        /**
         * Creates an instance of BiometricPromptHelper for the given FragmentActivity.
         */
        fun create(activity: FragmentActivity): BiometricPromptHelper {
            return BiometricPromptHelper(activity)
        }
    }
}

/**
 * Availability state representation for BiometricPromptHelper status checks.
 */
sealed class BiometricStatusResult {
    data object Available : BiometricStatusResult()
    data class Unavailable(val message: String) : BiometricStatusResult()
}
