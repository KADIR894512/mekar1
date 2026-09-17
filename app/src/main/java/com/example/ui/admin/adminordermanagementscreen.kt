package com.example.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.OrderEntity
import com.example.domain.model.OrderStatus
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.InvoiceDialog
import com.example.ui.components.OrderStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminOrderManagementScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val allPrinters by viewModel.allPrinters.collectAsState()
    val settings by viewModel.settingsMap.collectAsState()
    val storeName = settings["store_name"] ?: "MASTER PRINTER Hub"
    val storeAddress = settings["business_address"] ?: "Digital Print Avenue, Central Tech Park"

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var viewingInvoiceOrder by remember { mutableStateOf<OrderEntity?>(null) }
    var cancelOrderTarget by remember { mutableStateOf<OrderEntity?>(null) }

    val filterOptions = listOf(
        "ALL",
        "PAYMENT_PENDING",
        "QUEUED",
        "PRINTING",
        "READY_FOR_PICKUP",
        "COMPLETED",
        "CANCELLED"
    )

    val currentFilter = filterOptions[selectedTabIndex]
    val filteredOrders = allOrders.filter { order ->
        val matchesFilter = if (currentFilter == "ALL") true else order.printStatus == currentFilter
        val matchesSearch = if (searchQuery.isBlank()) true else {
            order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    order.customerName.contains(searchQuery, ignoreCase = true) ||
                    order.customerPhone.contains(searchQuery)
        }
        matchesFilter && matchesSearch
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Master Order Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control orders across all stations, verify payments, and process customer fulfillment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Order #, Customer Name, or Mobile...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                filterOptions.forEachIndexed { index, opt ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = opt.replace("_", " "),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
        }

        items(filteredOrders) { order ->
            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
            val assignedPrinter = allPrinters.find { it.id == order.assignedPrinterId }

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
                            Text(order.orderNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${order.customerName} (${order.customerPhone}) • $dateStr", style = MaterialTheme.typography.bodySmall)
                        }
                        OrderStatusBadge(status = order.printStatus)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${order.serviceName} • ${order.documentName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("₹${String.format(Locale.US, "%.2f", order.finalAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Specs: ${order.pageCount} pgs × ${order.copies} • ${order.paperSize} • ${if (order.isColor) "Colour" else "B&W"} • ${if (order.isDuplex) "Duplex" else "Single"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (assignedPrinter != null) {
                                Text("Hardware: ${assignedPrinter.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Admin Action Buttons based on status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (order.paymentStatus != "VERIFIED") {
                            Button(
                                onClick = {
                                    viewModel.verifyPayment(order.id, "Admin Verification", "ADMIN-TXN-${System.currentTimeMillis() % 100000}") { _, _ -> }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify Payment")
                            }
                        } else if (order.printStatus == "QUEUED") {
                            Button(
                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.PRINTING) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send to Print")
                            }
                        } else if (order.printStatus == "PRINTING") {
                            Button(
                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.READY) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Ready")
                            }
                        } else if (order.printStatus == "READY" || order.printStatus == "READY_FOR_PICKUP") {
                            Button(
                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.COMPLETED) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complete Order")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewingInvoiceOrder = order },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invoice")
                        }

                        if (order.printStatus != "COMPLETED" && order.printStatus != "CANCELLED") {
                            OutlinedButton(
                                onClick = { cancelOrderTarget = order },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (order.paymentStatus == "VERIFIED" && order.printStatus == "CANCELLED") {
                            OutlinedButton(
                                onClick = { viewModel.refundOrder(order.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Refund")
                            }
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

    cancelOrderTarget?.let { order ->
        var cancelReason by remember { mutableStateOf("Customer requested cancellation") }
        Dialog(onDismissRequest = { cancelOrderTarget = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cancel Order #${order.orderNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { cancelOrderTarget = null }) {
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.cancelOrder(order.id, cancelReason)
                                cancelOrderTarget = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Confirm Cancel")
                        }
                    }
                }
            }
        }
    }
}
