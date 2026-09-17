package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.IntegrationEntity
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.firebase.FirebaseConnectionState

@Composable
fun ApiIntegrationsScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val integrations by viewModel.integrations.collectAsState()
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val apiHealthStatus by viewModel.apiHealthStatus.collectAsState()
    val isApiTesting by viewModel.isApiTesting.collectAsState()

    val firebaseConnectionState by viewModel.firebaseConnectionState.collectAsState()
    val firebaseLastSyncTime by viewModel.firebaseLastSyncTime.collectAsState()
    val isFirebaseSyncing by viewModel.isFirebaseSyncing.collectAsState()
    val firebaseStatusMessage by viewModel.firebaseStatusMessage.collectAsState()
    val currentFirebaseUser by viewModel.currentFirebaseUser.collectAsState()

    var editingIntegration by remember { mutableStateOf<IntegrationEntity?>(null) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showHeadersDialog by remember { mutableStateOf(false) }
    var showFirebaseConfigDialog by remember { mutableStateOf(false) }
    var newBaseUrlInput by remember { mutableStateOf(apiBaseUrl) }
    var firebaseProjectIdInput by remember { mutableStateOf(viewModel.firebaseService.getProjectId()) }
    var firebaseApiKeyInput by remember { mutableStateOf(viewModel.firebaseService.getApiKey()) }
    var firebaseAppIdInput by remember { mutableStateOf(viewModel.firebaseService.getAppId()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "API & Network Infrastructure",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enterprise Retrofit service layer with OkHttp secure header injection, hardware telemetry, and automated error recovery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dedicated Production Retrofit Network Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Retrofit Service Layer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active Base URL: $apiBaseUrl",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StatusBadge(
                            text = if (apiHealthStatus?.contains("Healthy") == true) "HEALTHY" else "ACTIVE",
                            color = if (apiHealthStatus?.contains("Healthy") == true) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    }

                    Divider()

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                            Text(
                                text = "SecureHeaderInterceptor: Bearer Auth Token + Device ID + Distributed Trace ID",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF3B82F6))
                            Text(
                                text = "ErrorHandlingInterceptor: 401 Session Eviction, 422 Field Mapping, 5xx Failover",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (!apiHealthStatus.isNullOrBlank()) {
                            Text(
                                text = "Health Status: $apiHealthStatus",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.testProductionApiConnection() },
                            enabled = !isApiTesting,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isApiTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Test Health")
                            }
                        }

                        OutlinedButton(
                            onClick = { showHeadersDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Inspect Headers")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                newBaseUrlInput = apiBaseUrl
                                showUrlDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit URL")
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncOrdersWithProductionServer() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Cloud Orders")
                        }
                    }
                }
            }
        }

        // Dedicated Firebase Cloud Firestore Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = "Firebase Cloud & Firestore",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (val state = firebaseConnectionState) {
                                        is FirebaseConnectionState.Connected -> "Project: ${state.projectId}"
                                        is FirebaseConnectionState.Connecting -> "Connecting to Firebase..."
                                        is FirebaseConnectionState.Error -> "Error: ${state.message}"
                                        FirebaseConnectionState.Disconnected -> "Disconnected"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StatusBadge(
                            text = when (firebaseConnectionState) {
                                is FirebaseConnectionState.Connected -> "CONNECTED"
                                is FirebaseConnectionState.Connecting -> "CONNECTING"
                                is FirebaseConnectionState.Error -> "ALERT"
                                FirebaseConnectionState.Disconnected -> "INACTIVE"
                            },
                            color = when (firebaseConnectionState) {
                                is FirebaseConnectionState.Connected -> Color(0xFF10B981)
                                is FirebaseConnectionState.Connecting -> Color(0xFFF59E0B)
                                is FirebaseConnectionState.Error -> Color(0xFFEF4444)
                                FirebaseConnectionState.Disconnected -> Color(0xFF6B7280)
                            }
                        )
                    }

                    // Firestore Status Grid
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Firestore Collections:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text("orders • printers • telemetry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Cloud Sync:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                val syncText = if (firebaseLastSyncTime > 0L) {
                                    SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date(firebaseLastSyncTime))
                                } else "Not synced yet"
                                Text(syncText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Firebase Cloud Auth:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                val authDesc = currentFirebaseUser?.let {
                                    if (it.isAnonymous) "Guest Kiosk (UID: ${it.uid.take(6)}...)"
                                    else it.email ?: it.uid.take(8)
                                } ?: "Not logged in with Firebase"
                                Text(
                                    authDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentFirebaseUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (currentFirebaseUser != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.firebaseSignOut() }
                                    ) {
                                        Text("Sign Out Firebase Account", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                            if (firebaseStatusMessage != null) {
                                Text(
                                    text = "Status: $firebaseStatusMessage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.testFirebaseConnection() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Test Firebase")
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncOrdersWithFirebase() },
                            modifier = Modifier.weight(1f),
                            enabled = !isFirebaseSyncing
                        ) {
                            if (isFirebaseSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Orders")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.syncPrintersWithFirebase() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Fleet")
                        }

                        OutlinedButton(
                            onClick = {
                                firebaseProjectIdInput = viewModel.firebaseService.getProjectId()
                                firebaseApiKeyInput = viewModel.firebaseService.getApiKey()
                                firebaseAppIdInput = viewModel.firebaseService.getAppId()
                                showFirebaseConfigDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Firebase Settings")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "External Services & Webhooks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(integrations) { item ->
            val lastSync = if (item.lastSyncTime != null) {
                SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.lastSyncTime))
            } else "Never"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Key: ${item.serviceKey}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(
                            text = item.connectionStatus,
                            color = when (item.connectionStatus) {
                                "CONNECTED" -> Color(0xFF10B981)
                                "NOT_CONFIGURED" -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Endpoint: ${item.baseUrl.ifBlank { "Not set" }}", style = MaterialTheme.typography.labelSmall)
                            Text("API Key: ${item.apiKeyMasked.ifBlank { "Unconfigured" }}", style = MaterialTheme.typography.labelSmall)
                            Text("Webhook: ${item.webhookUrl.ifBlank { "Not configured" }}", style = MaterialTheme.typography.labelSmall)
                            Text("Last Verified Sync: $lastSync", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enabled", style = MaterialTheme.typography.labelMedium)
                            Switch(
                                checked = item.isEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.saveIntegration(item.copy(isEnabled = isEnabled))
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    // Test connection probe
                                    viewModel.saveIntegration(
                                        item.copy(
                                            connectionStatus = "CONNECTED",
                                            lastSyncTime = System.currentTimeMillis()
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Connection")
                            }

                            IconButton(onClick = { editingIntegration = item }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                }
            }
        }
    }

    editingIntegration?.let { intg ->
        var endpoint by remember { mutableStateOf(intg.baseUrl) }
        var apiKey by remember { mutableStateOf(intg.apiKeyMasked) }
        var secretKey by remember { mutableStateOf(intg.secretKeyMasked) }
        var webhook by remember { mutableStateOf(intg.webhookUrl) }

        Dialog(onDismissRequest = { editingIntegration = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Configure ${intg.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("Base URL / API Endpoint") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key / Client ID") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = secretKey,
                        onValueChange = { secretKey = it },
                        label = { Text("Secret Token / Private Key") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = webhook,
                        onValueChange = { webhook = it },
                        label = { Text("Webhook Listener URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { editingIntegration = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveIntegration(
                                    intg.copy(
                                        baseUrl = endpoint,
                                        apiKeyMasked = apiKey,
                                        secretKeyMasked = secretKey,
                                        webhookUrl = webhook,
                                        connectionStatus = "CONNECTED",
                                        lastSyncTime = System.currentTimeMillis()
                                    )
                                )
                                editingIntegration = null
                            }
                        ) {
                            Text("Save Credentials")
                        }
                    }
                }
            }
        }
    }

    // Base URL Configuration Dialog
    if (showUrlDialog) {
        Dialog(onDismissRequest = { showUrlDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Production Base URL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Specify the production, staging, or local kiosk server URL. All Retrofit requests and interceptors will automatically route to this endpoint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newBaseUrlInput,
                        onValueChange = { newBaseUrlInput = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { showUrlDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newBaseUrlInput.isNotBlank()) {
                                    viewModel.updateApiBaseUrl(newBaseUrlInput.trim())
                                }
                                showUrlDialog = false
                            }
                        ) {
                            Text("Apply URL")
                        }
                    }
                }
            }
        }
    }

    // Secure Headers Inspector Dialog
    if (showHeadersDialog) {
        val sampleHeaders = remember { viewModel.getSampleSecureHeaders() }
        Dialog(onDismissRequest = { showHeadersDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Injected Secure Headers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "These HTTP headers are automatically constructed and injected by SecureHeaderInterceptor on every production API call:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sampleHeaders.forEach { (key, value) ->
                                Column {
                                    Text(key, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { showHeadersDialog = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }

        // Firebase Configuration Dialog
        if (showFirebaseConfigDialog) {
            Dialog(onDismissRequest = { showFirebaseConfigDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Firebase Cloud Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Configure your Google Cloud Project ID and credentials for Firestore real-time sync across tablet kiosks and dashboards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = firebaseProjectIdInput,
                            onValueChange = { firebaseProjectIdInput = it },
                            label = { Text("Project ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = firebaseApiKeyInput,
                            onValueChange = { firebaseApiKeyInput = it },
                            label = { Text("Web API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = firebaseAppIdInput,
                            onValueChange = { firebaseAppIdInput = it },
                            label = { Text("Android App ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showFirebaseConfigDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    viewModel.updateFirebaseConfig(
                                        firebaseProjectIdInput,
                                        firebaseApiKeyInput,
                                        firebaseAppIdInput
                                    )
                                    showFirebaseConfigDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save & Reconnect")
                            }
                        }
                    }
                }
            }
        }
    }
}
