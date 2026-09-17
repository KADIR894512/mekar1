package com.example.ui.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.local.entities.OrderEntity
import com.example.domain.model.OrderStatus
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.InvoiceDialog
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.StatusBadge
import com.example.ui.security.BiometricSecuredBadge
import com.example.ui.security.rememberBiometricSecurityController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StaffPortalScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val settings by viewModel.settingsMap.collectAsState()
    val storeName = settings["store_name"] ?: "MASTER PRINTER Hub"
    val storeAddress = settings["business_address"] ?: "Digital Print Avenue, Central Tech Park"

    var searchQuery by remember { mutableStateOf("") }
    var viewingInvoiceOrder by remember { mutableStateOf<OrderEntity?>(null) }
    val (biometricGuard, BiometricFallbackDialog) = rememberBiometricSecurityController(viewModel)

    val filteredOrders = allOrders.filter { order ->
        if (searchQuery.isBlank()) true
        else order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.customerPhone.contains(searchQuery)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Counter Staff Order Fulfillment",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    BiometricSecuredBadge(text = "Staff Station")
                }
                Text(
                    text = "Verify counter cash/UPI receipts, hand over finished jobs, and print invoices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Order #, Customer Name, or Phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(filteredOrders) { order ->
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
                            Text("ORDER #${order.orderNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${order.customerName} • ${order.customerPhone}", style = MaterialTheme.typography.bodySmall)
                        }
                        OrderStatusBadge(status = order.printStatus)
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
                            Text("${order.serviceName} (${order.documentName})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("₹${String.format(Locale.US, "%.2f", order.finalAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (order.paymentStatus != "VERIFIED") {
                            Button(
                                onClick = {
                                    biometricGuard.authenticate(
                                        title = "Accept Counter Payment",
                                        subtitle = "Authorize receiving ₹${String.format(Locale.US, "%.2f", order.finalAmount)} for Order #${order.orderNumber}",
                                        description = "Staff biometric verification required to confirm payment collection."
                                    ) {
                                        viewModel.verifyPayment(order.id, "Store POS Counter", "POS-CASH-${System.currentTimeMillis() % 100000}") { _, _ -> }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept Payment")
                            }
                        }

                        if (order.printStatus == "READY" || order.printStatus == "READY_FOR_PICKUP") {
                            Button(
                                onClick = {
                                    biometricGuard.authenticate(
                                        title = "Authorize Order Handover",
                                        subtitle = "Confirm print package delivery to ${order.customerName}",
                                        description = "Staff biometric verification required to close fulfilled order."
                                    ) {
                                        viewModel.updateOrderStatus(order.id, OrderStatus.COMPLETED)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hand Over")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewingInvoiceOrder = order },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receipt")
                        }
                    }
                }
            }
        }
    }

    viewingInvoiceOrder?.let { order ->
        InvoiceDialog(
            order = order,
            storeName = storeName,
            storeAddress = storeAddress,
            onDismiss = { viewingInvoiceOrder = null }
        )
    }

    BiometricFallbackDialog()
}
