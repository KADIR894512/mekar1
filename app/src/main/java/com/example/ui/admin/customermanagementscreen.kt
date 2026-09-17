package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.MasterPrinterViewModel
import com.example.ui.security.BiometricSecuredBadge
import com.example.ui.security.rememberBiometricSecurityController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerManagementScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val allUsers by viewModel.allUsers.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val customers = allUsers.filter { it.role == "CUSTOMER" }
    var search by remember { mutableStateOf("") }
    val (biometricGuard, BiometricFallbackDialog) = rememberBiometricSecurityController(viewModel)

    val filtered = customers.filter { c ->
        if (search.isBlank()) true
        else c.fullName.contains(search, ignoreCase = true) ||
                c.username.contains(search, ignoreCase = true) ||
                c.email.contains(search, ignoreCase = true) ||
                c.phone.contains(search)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Registry (${customers.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    BiometricSecuredBadge(text = "Admin Access")
                }
                Text(
                    text = "Directory of walk-in and registered digital print clients.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search customers by name, email or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        items(filtered) { customer ->
            val customerOrders = allOrders.filter { it.customerId == customer.id }
            val totalSpend = customerOrders.sumOf { it.finalAmount }
            val regDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(customer.createdAt))

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
                            Text(customer.fullName.ifBlank { customer.username }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${customer.email} • ${customer.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "${customerOrders.size} Orders",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
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
                            Text("Total Spend: ₹${String.format(Locale.US, "%.2f", totalSpend)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("Joined: $regDate", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (customer.status == "ACTIVE") "Active Customer" else "Account Blocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (customer.status == "ACTIVE") Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Switch(
                            checked = customer.status == "ACTIVE",
                            onCheckedChange = { checked ->
                                val next = if (checked) "ACTIVE" else "DISABLED"
                                biometricGuard.authenticate(
                                    title = "Customer Access Control",
                                    subtitle = "Authorize ${if (checked) "unblocking" else "blocking"} account for ${customer.username}",
                                    description = "Admin biometric authorization required to modify customer status."
                                ) {
                                    viewModel.updateUserStatus(customer.id, next)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    BiometricFallbackDialog()
}
