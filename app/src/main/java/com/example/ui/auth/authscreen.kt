package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FirebaseConnectionState
import com.example.data.session.BiometricAuthManager
import com.example.data.session.BiometricStatus
import com.example.ui.AdminNavScreen
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.RoyalKCrestGraphic
import com.example.ui.components.RoyalWallpaperBackground

@Composable
fun AuthScreen(
    viewModel: MasterPrinterViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val biometricAuthManager = remember { BiometricAuthManager.getInstance(context) }
    val isBiometricEnabled by viewModel.sessionManager.isBiometricEnabled.collectAsState()
    val isRoyalWallpaperEnabled by viewModel.sessionManager.isRoyalWallpaperEnabled.collectAsState()
    val biometricUsername by viewModel.sessionManager.biometricAccountUsername.collectAsState()
    val firebaseConnState by viewModel.firebaseConnectionState.collectAsState()
    val currentFirebaseUser by viewModel.currentFirebaseUser.collectAsState()

    var showWallpaperPreview by remember { mutableStateOf(false) }
    var isAutoLoginChecked by remember { mutableStateOf(true) }
    var isRegistering by remember { mutableStateOf(false) }
    var identifier by remember { mutableStateOf("KADIR143") }
    var password by remember { mutableStateOf("ABDUL123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Firebase Auth fields
    var firebaseEmail by remember { mutableStateOf("") }
    var firebasePassword by remember { mutableStateOf("") }
    var firebasePasswordVisible by remember { mutableStateOf(false) }

    // Register fields
    var regFullName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var registerWithFirebase by remember { mutableStateOf(true) }

    // Forgot password dialog
    var showForgotPassword by remember { mutableStateOf(false) }

    fun triggerBiometricUnlock() {
        if (fragmentActivity == null) {
            errorMessage = "Biometric prompt requires an active activity context."
            return
        }
        val availability = biometricAuthManager.checkBiometricAvailability()
        if (availability is BiometricStatus.Unavailable) {
            Toast.makeText(context, availability.reason, Toast.LENGTH_SHORT).show()
        }

        isLoading = true
        errorMessage = null
        biometricAuthManager.authenticate(
            activity = fragmentActivity,
            title = "Master Printer Biometric Access",
            subtitle = "Verify fingerprint, face, or device lock to open account ($biometricUsername)",
            description = "Touch fingerprint sensor or glance at camera to unlock",
            allowDeviceCredentials = true,
            negativeButtonText = "Enter Password",
            onSuccess = {
                isLoading = false
                viewModel.loginWithBiometric(biometricUsername) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Biometric authentication verified!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        errorMessage = err ?: "Biometric login failed"
                    }
                }
            },
            onError = { errorCode, errString ->
                isLoading = false
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    errorMessage = "Biometric: $errString"
                }
            },
            onFailed = {
                isLoading = false
                errorMessage = "Fingerprint or face not recognized. Try again or enter password."
            }
        )
    }

    // Main Content Box
    val mainContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRoyalWallpaperEnabled) 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f) 
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Controls Row: Wallpaper Toggle & Full-Screen Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isRoyalWallpaperEnabled) Color(0xFFD4AF37).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = if (isRoyalWallpaperEnabled) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isRoyalWallpaperEnabled) "Wallpaper ON" else "Wallpaper OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRoyalWallpaperEnabled) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { viewModel.sessionManager.setRoyalWallpaperEnabled(!isRoyalWallpaperEnabled) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRoyalWallpaperEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Wallpaper",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showWallpaperPreview = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Full Screen View", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brand Header
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0066FF), Color(0xFFD4AF37))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "MASTER PRINTER",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "MASTER PRINTER",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enterprise Print Platform & Cloud Kiosks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!isRegistering) {
                    // ⚡ Primary 1-Tap Data Connect Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "⚡ डाटा से कनेक्ट करें (Direct Data Connect)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Firebase Cloud & Local Database Synchronized",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.autoConnectToData(AdminNavScreen.CLOUD_FILES) {
                                        isLoading = false
                                        onLoginSuccess()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLoading) "Connecting to Data..." else "⚡ डाटा से कनेक्ट करें और फाइल पब्लिक करें",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAutoLoginChecked,
                                    onCheckedChange = {
                                        isAutoLoginChecked = it
                                        viewModel.sessionManager.setAutoLoginEnabled(it)
                                    }
                                )
                                Text(
                                    text = "हमेशा कनेक्टेड रखें (बार-बार लॉगिन न पूछे)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(
                            text = "या आईडी से लॉगिन करें (Or Sign In)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sign In Form
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("Email, Mobile or Admin ID") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showForgotPassword = true }) {
                            Text("Forgot password?", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (identifier.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both identifier and password"
                                return@Button
                            }
                            isLoading = true
                            viewModel.login(identifier, password) { success, err ->
                                isLoading = false
                                if (success) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = err ?: "Login failed"
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (isLoading) "Verifying..." else "Sign In to Master Printer", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometric Unlock Card (Fingerprint / Face Unlock / PIN)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E202A).copy(alpha = 0.9f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometrics",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Instant Unlock",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFF8DC)
                                    )
                                    Text(
                                        text = "Fingerprint, Face Unlock or Device PIN ($biometricUsername)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB0BEC5),
                                        maxLines = 1
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Face Unlock",
                                    tint = Color(0xFFFFD54F).copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Button(
                                onClick = { triggerBiometricUnlock() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD4AF37),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Unlock with Fingerprint / Face",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = { isRegistering = true; errorMessage = null },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("New Customer? Register an Account", style = MaterialTheme.typography.bodyMedium)
                    }

                } else {
                    // Registration Form
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Register Customer Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { isRegistering = false }) {
                            Text("Back to Login", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))


                    // Registration Form
                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text("Mobile Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Choose Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cloud Firebase Registration Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = registerWithFirebase,
                            onCheckedChange = { registerWithFirebase = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Firebase Cloud Account",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enables cloud synchronization and multi-device access",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (regUsername.isBlank() || regEmail.isBlank() || regPhone.isBlank() || regPassword.isBlank()) {
                                errorMessage = "Please fill in all registration fields"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null

                            if (registerWithFirebase) {
                                viewModel.firebaseRegister(
                                    email = regEmail,
                                    pass = regPassword,
                                    fullName = regFullName.ifBlank { regUsername }
                                ) { success, err ->
                                    isLoading = false
                                    if (success) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = err ?: "Firebase registration failed"
                                    }
                                }
                            } else {
                                viewModel.register(
                                    user = regUsername,
                                    email = regEmail,
                                    phone = regPhone,
                                    pass = regPassword,
                                    fullName = regFullName
                                ) { success, err ->
                                    isLoading = false
                                    if (success) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = err ?: "Registration failed"
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (isLoading) "Creating Account..." else "Register Customer Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    }

    // Render Wallpaper or Standard Background
    if (isRoyalWallpaperEnabled) {
        RoyalWallpaperBackground {
            mainContent()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            mainContent()
        }
    }

    if (showWallpaperPreview) {
        Dialog(
            onDismissRequest = { showWallpaperPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                RoyalWallpaperBackground(dimOverlayAlpha = 0.1f) {}
                IconButton(
                    onClick = { showWallpaperPreview = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Preview", tint = Color.White)
                }
            }
        }
    }

    if (showForgotPassword) {
        var resetEmail by remember { mutableStateOf("") }
        var resetSent by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showForgotPassword = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (resetSent) {
                        Text(
                            text = "A secure verification reset link has been dispatched to $resetEmail. Please check your inbox.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF10B981)
                        )
                        Button(
                            onClick = { showForgotPassword = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Return to Login")
                        }
                    } else {
                        Text(
                            text = "Enter your registered email address or mobile number to receive a secure password recovery code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email or Mobile Number") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showForgotPassword = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (resetEmail.isNotBlank()) resetSent = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Send Reset Code")
                            }
                        }
                    }
                }
            }
        }
    }
}
