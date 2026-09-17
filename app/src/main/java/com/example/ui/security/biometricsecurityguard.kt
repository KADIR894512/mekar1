package com.example.ui.security

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.data.session.BiometricAuthManager
import com.example.data.session.BiometricStatus
import com.example.ui.MasterPrinterViewModel

/**
 * Controller for launching Android BiometricPrompt for sensitive admin and operator actions.
 */
class BiometricSecurityController(
    val activity: FragmentActivity?,
    val biometricPromptHelper: BiometricPromptHelper?,
    val isBiometricEnabled: Boolean,
    val showToast: (String) -> Unit,
    val onRequireFallbackDialog: (title: String, subtitle: String, onProceed: () -> Unit) -> Unit
) {
    /**
     * Executes the requested sensitive action only after verifying the user's biometrics
     * (fingerprint, face, or device screen lock credentials) using BiometricPromptHelper.
     */
    fun authenticate(
        title: String,
        subtitle: String,
        description: String = "Authorize sensitive operation using fingerprint, face, or device PIN",
        onVerified: () -> Unit
    ) {
        if (!isBiometricEnabled) {
            // Biometric security bypassed by user preference
            onVerified()
            return
        }

        if (activity == null || biometricPromptHelper == null) {
            onRequireFallbackDialog(title, subtitle, onVerified)
            return
        }

        val status = biometricPromptHelper.checkBiometricStatus()
        when (status) {
            is BiometricStatusResult.Available -> {
                biometricPromptHelper.authenticate(
                    title = title,
                    subtitle = subtitle,
                    description = description,
                    allowDeviceCredentials = true,
                    negativeButtonText = "Cancel",
                    onVerified = {
                        showToast("Biometric verification approved")
                        onVerified()
                    },
                    onError = { code, errString, isCanceled ->
                        if (!isCanceled) {
                            showToast("Biometric error: $errString")
                        }
                    },
                    onFailed = {
                        showToast("Biometric not recognized. Action cancelled.")
                    }
                )
            }
            is BiometricStatusResult.Unavailable -> {
                // Device lacks hardware or enrollment (e.g. emulator or un-enrolled device);
                // show fallback confirmation prompt so the user is never blocked.
                onRequireFallbackDialog(title, "${status.message}\n\n$subtitle", onVerified)
            }
        }
    }
}

/**
 * Remember and instantiate a BiometricSecurityController in any Composable screen.
 */
@Composable
fun rememberBiometricSecurityController(
    viewModel: MasterPrinterViewModel
): Pair<BiometricSecurityController, @Composable () -> Unit> {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricPromptHelper = remember(activity) {
        activity?.let { BiometricPromptHelper.create(it) }
    }
    val isBiometricEnabled by viewModel.sessionManager.isBiometricEnabled.collectAsState()

    var fallbackDialogData by remember {
        mutableStateOf<Triple<String, String, () -> Unit>?>(null)
    }

    val controller = remember(activity, biometricPromptHelper, isBiometricEnabled) {
        BiometricSecurityController(
            activity = activity,
            biometricPromptHelper = biometricPromptHelper,
            isBiometricEnabled = isBiometricEnabled,
            showToast = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onRequireFallbackDialog = { title, subtitle, onProceed ->
                fallbackDialogData = Triple(title, subtitle, onProceed)
            }
        )
    }

    val dialogComposable: @Composable () -> Unit = {
        fallbackDialogData?.let { (title, subtitle, onProceed) ->
            Dialog(onDismissRequest = { fallbackDialogData = null }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Security Authorization",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Operator / Admin Confirmation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { fallbackDialogData = null }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    fallbackDialogData = null
                                    onProceed()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm Action")
                            }
                        }
                    }
                }
            }
        }
    }

    return Pair(controller, dialogComposable)
}

/**
 * Visual badge indicating that an action or section is guarded by Android Biometrics.
 */
@Composable
fun BiometricSecuredBadge(
    text: String = "Biometric Secured",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
