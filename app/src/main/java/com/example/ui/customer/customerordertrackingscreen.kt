package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.OrderStatus
import com.example.ui.CustomerNavScreen
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.InvoiceDialog
import com.example.ui.components.OrderStatusBadge
import java.util.Locale

@Composable
fun CustomerOrderTrackingScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val selectedId by viewModel.selectedOrderId.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val printers by viewModel.allPrinters.collectAsState()
    val queue by viewModel.activeQueue.collectAsState()
    val settings by viewModel.settingsMap.collectAsState()

    val order = allOrders.find { it.id == selectedId } ?: allOrders.firstOrNull()
    val assignedPrinter = printers.find { it.id == order?.assignedPrinterId }
    val matchingJob = queue.find { it.orderId == order?.id }

    var showInvoiceDialog by remember { mutableStateOf(false) }
    var txnIdInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf<String?>(null) }

    val storeName = settings["store_name"] ?: "MASTER PRINTER Hub"
    val storeAddress = settings["business_address"] ?: "Digital Print Avenue, Central Tech Park"

    if (order == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active order selected.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ORDER #${order.orderNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = order.serviceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OrderStatusBadge(status = order.printStatus)
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Document", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.documentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${order.pageCount} pages • ${order.copies} copies", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "₹${String.format(Locale.US, "%.2f", order.finalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Payment: ${order.paymentStatus}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Live Print Progress Timeline
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "PRINT PIPELINE STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val steps = listOf(
                    "Payment Verified" to (order.paymentStatus == "VERIFIED" || order.printStatus != "PAYMENT_PENDING"),
                    "In Master Queue" to (order.printStatus in listOf("QUEUED", "PRINTING", "READY_FOR_PICKUP", "COMPLETED")),
                    "Printing Hardware Active" to (order.printStatus in listOf("PRINTING", "READY_FOR_PICKUP", "COMPLETED")),
                    "Ready for Pickup" to (order.printStatus in listOf("READY_FOR_PICKUP", "COMPLETED")),
                    "Fulfilled / Completed" to (order.printStatus == "COMPLETED")
                )

                steps.forEachIndexed { index, (label, isPassed) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isPassed) Color(0xFF10B981) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPassed) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isPassed) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPassed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (order.printStatus == "PRINTING" && matchingJob != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Printhead Progress", style = MaterialTheme.typography.labelSmall)
                            Text("${matchingJob.progressPercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { matchingJob.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                if (assignedPrinter != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Assigned Station:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${assignedPrinter.name} (${assignedPrinter.location})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Real Payment Checkout Section (If not yet verified)
        if (order.paymentStatus != "VERIFIED") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "SECURE PAYMENT SETTLEMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "To enter the MASTER PRINTER hardware spooler, please complete payment for ₹${String.format(Locale.US, "%.2f", order.finalAmount)}.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // UPI QR code representation
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                            Text("UPI ID: masterprinter@okhdfcbank", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Scan via PhonePe / GPay / Paytm or pay at store counter", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = txnIdInput,
                        onValueChange = { txnIdInput = it },
                        label = { Text("Transaction Reference / UTR / Cashier POS Receipt") },
                        placeholder = { Text("e.g. UPI8829471928") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (verificationError != null) {
                        Text(
                            text = verificationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val ref = if (txnIdInput.isNotBlank()) txnIdInput.trim() else "UPI-INTENT-${(100000..999999).random()}"
                            isVerifying = true
                            verificationError = null
                            viewModel.verifyPayment(
                                orderId = order.id,
                                method = "UPI Payment",
                                txnId = ref
                            ) { success, err ->
                                isVerifying = false
                                if (!success) {
                                    verificationError = err
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (isVerifying) "Verifying with Gateway..." else "Verify & Send to Printer Queue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Buttons: Tax Invoice & Cancellation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showInvoiceDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Tax Invoice")
            }

            if (order.printStatus == "PAYMENT_PENDING" || order.printStatus == "QUEUED") {
                OutlinedButton(
                    onClick = {
                        viewModel.cancelOrder(order.id, "Customer requested cancellation")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel")
                }
            }
        }
    }

    if (showInvoiceDialog) {
        InvoiceDialog(
            order = order,
            storeName = storeName,
            storeAddress = storeAddress,
            onDismiss = { showInvoiceDialog = false }
        )
    }
}
