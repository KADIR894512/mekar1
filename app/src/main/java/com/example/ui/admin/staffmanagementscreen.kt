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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.domain.model.UserRole
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.RoleBadge
import com.example.ui.security.BiometricSecuredBadge
import com.example.ui.security.rememberBiometricSecurityController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StaffManagementScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val staffUsers = users.filter { it.role != "CUSTOMER" }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    val (biometricGuard, BiometricFallbackDialog) = rememberBiometricSecurityController(viewModel)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Staff & Role Permissions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        BiometricSecuredBadge(text = "Admin Access")
                    }
                    Text(
                        text = "Manage Administrators, Managers, Counter Staff, and Printer Operators.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddStaffDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Staff")
                }
            }
        }

        items(staffUsers) { user ->
            val created = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.createdAt))
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
                            Text(user.fullName.ifBlank { user.username }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Username: ${user.username} • ${user.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RoleBadge(role = user.role)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Phone: ${user.phone}", style = MaterialTheme.typography.labelSmall)
                            Text("Status: ${user.status} • Joined: $created", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (user.status == "ACTIVE") "Account Enabled" else "Account Suspended",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.status == "ACTIVE") Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Switch(
                            checked = user.status == "ACTIVE",
                            onCheckedChange = { checked ->
                                val nextStatus = if (checked) "ACTIVE" else "DISABLED"
                                biometricGuard.authenticate(
                                    title = "Staff Account Privilege",
                                    subtitle = "Authorize setting ${user.username} to $nextStatus status",
                                    description = "Admin biometric authorization required to adjust account access."
                                ) {
                                    viewModel.updateUserStatus(user.id, nextStatus)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddStaffDialog) {
        var username by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(UserRole.STAFF) }

        Dialog(onDismissRequest = { showAddStaffDialog = false }) {
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
                    Text("Provision New Staff Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("System User ID (e.g. STAFF02)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Official Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Temporary Password") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assign Functional Role", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(UserRole.STAFF, UserRole.PRINTER_OPERATOR, UserRole.MANAGER, UserRole.ADMIN).forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { showAddStaffDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (username.isNotBlank() && email.isNotBlank() && pass.isNotBlank()) {
                                    biometricGuard.authenticate(
                                        title = "Provision Staff Account",
                                        subtitle = "Authorize creating ${role.displayName} account for $username",
                                        description = "Admin biometric authentication required to provision new staff credentials."
                                    ) {
                                        viewModel.createStaffUser(
                                            username = username,
                                            email = email,
                                            phone = phone,
                                            pass = pass,
                                            fullName = fullName,
                                            role = role
                                        ) { success, _ ->
                                            if (success) showAddStaffDialog = false
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Create Account")
                        }
                    }
                }
            }
        }
    }

    BiometricFallbackDialog()
}
