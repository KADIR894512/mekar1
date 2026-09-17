package com.example.ui.operator

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.ui.security.BiometricSecuredBadge
import com.example.ui.security.rememberBiometricSecurityController
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.PrintJobEntity
import com.example.data.local.entities.PrinterEntity
import com.example.domain.model.OrderStatus
import com.example.domain.model.PrintJobStatus
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.PrinterStatusBadge
import com.example.ui.components.StatusBadge

@Composable
fun PrinterOperatorTerminalScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Queue, 1: Hardware Fleet
    val printJobs by viewModel.activeQueue.collectAsState()
    val allPrinters by viewModel.allPrinters.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()

    var reassigningJob by remember { mutableStateOf<PrintJobEntity?>(null) }
    val (biometricGuard, BiometricFallbackDialog) = rememberBiometricSecurityController(viewModel)

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Operator Station Security",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                BiometricSecuredBadge(text = "Biometric Protected")
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Active Print Queue (${printJobs.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Hardware Fleet (${allPrinters.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (selectedTab == 0) {
            // Live Print Queue
            if (printJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Text("No pending print jobs in queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Newly placed customer orders with verified payments will appear here automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(printJobs) { job ->
                        val matchingPrinter = allPrinters.find { it.id == job.printerId }
                        val matchingOrder = allOrders.find { it.id == job.orderId }

                        PrintJobOperatorCard(
                            job = job,
                            printer = matchingPrinter,
                            orderNumber = matchingOrder?.orderNumber ?: "ORD-${job.orderId}",
                            onUpdateStatus = { newStatus, progress ->
                                if (newStatus == PrintJobStatus.COMPLETED) {
                                    biometricGuard.authenticate(
                                        title = "Complete Print Job",
                                        subtitle = "Authorize job fulfillment & order completion for ORD-${job.orderId}",
                                        description = "Verify operator biometric credentials to complete order."
                                    ) {
                                        viewModel.updateJobStatus(job.id, newStatus, progress)
                                        viewModel.updateOrderStatus(job.orderId, OrderStatus.READY)
                                    }
                                } else {
                                    viewModel.updateJobStatus(job.id, newStatus, progress)
                                    if (newStatus == PrintJobStatus.PRINTING) {
                                        viewModel.updateOrderStatus(job.orderId, OrderStatus.PRINTING)
                                    }
                                }
                            },
                            onReassign = {
                                biometricGuard.authenticate(
                                    title = "Reassign Print Job",
                                    subtitle = "Verify operator identity to transfer Job #${job.id} to another printer station",
                                    description = "Biometric confirmation required for print routing."
                                ) {
                                    reassigningJob = job
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Hardware Fleet Monitor
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allPrinters) { printer ->
                    PrinterHardwareCard(
                        printer = printer,
                        onStatusChange = { newStatus ->
                            biometricGuard.authenticate(
                                title = "Hardware Fleet Status",
                                subtitle = "Authorize setting ${printer.name} to $newStatus mode",
                                description = "Confirm operator biometric credentials to update printer station state."
                            ) {
                                viewModel.updatePrinterStatus(printer.id, newStatus)
                            }
                        }
                    )
                }
            }
        }
    }

    // Reassign Dialog
    reassigningJob?.let { job ->
        Dialog(onDismissRequest = { reassigningJob = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reassign Job #${job.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        BiometricSecuredBadge(text = "Secure Route")
                    }
                    Text("Select target MASTER PRINTER unit to redirect hardware spooler:", style = MaterialTheme.typography.bodySmall)

                    allPrinters.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (p.id == job.printerId) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    biometricGuard.authenticate(
                                        title = "Confirm Queue Redirect",
                                        subtitle = "Transfer Job #${job.id} to ${p.name} (${p.location})",
                                        description = "Biometric authorization for hardware spooler reassignment."
                                    ) {
                                        viewModel.reassignJob(job.id, p.id)
                                        reassigningJob = null
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${p.location} • ${p.connectionType}", style = MaterialTheme.typography.labelSmall)
                                }
                                PrinterStatusBadge(status = p.status)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { reassigningJob = null },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    BiometricFallbackDialog()
}

@Composable
fun PrintJobOperatorCard(
    job: PrintJobEntity,
    printer: PrinterEntity?,
    orderNumber: String,
    onUpdateStatus: (PrintJobStatus, Int) -> Unit,
    onReassign: () -> Unit
) {
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
                    Text("JOB #${job.id} • Order $orderNumber", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(job.documentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                StatusBadge(
                    text = job.status,
                    color = when (job.status) {
                        "PRINTING" -> Color(0xFF2563EB)
                        "COMPLETED" -> Color(0xFF10B981)
                        "FAILED" -> Color(0xFFEF4444)
                        "PAUSED" -> Color(0xFFF59E0B)
                        else -> Color(0xFF64748B)
                    }
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target: ${printer?.name ?: "Printer #${job.printerId}"}", style = MaterialTheme.typography.labelSmall)
                    Text("${job.pageCount} pgs × ${job.copies} • ${job.paperSize} • ${if (job.isColor) "Colour" else "B&W"}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            if (job.status == "PRINTING") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hardware spooling progress", style = MaterialTheme.typography.labelSmall)
                        Text("${job.progressPercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { job.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (job.status == "QUEUED" || job.status == "PAUSED") {
                    Button(
                        onClick = { onUpdateStatus(PrintJobStatus.PRINTING, 35) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Print")
                    }
                } else if (job.status == "PRINTING") {
                    Button(
                        onClick = { onUpdateStatus(PrintJobStatus.COMPLETED, 100) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Done")
                    }
                    OutlinedButton(
                        onClick = { onUpdateStatus(PrintJobStatus.PAUSED, job.progressPercent) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                OutlinedButton(
                    onClick = onReassign,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reassign")
                }
            }
        }
    }
}

@Composable
fun PrinterHardwareCard(
    printer: PrinterEntity,
    onStatusChange: (String) -> Unit
) {
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
                    Text(printer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${printer.manufacturer} ${printer.model} • ${printer.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PrinterStatusBadge(status = printer.status)
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
                    Text("Connection: ${printer.connectionType} (${printer.ipAddress}:${printer.port})", style = MaterialTheme.typography.labelSmall)
                    Text("Media: ${printer.paperSizesSupported}", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val nextStatus = if (printer.status == "ONLINE") "MAINTENANCE" else "ONLINE"
                        onStatusChange(nextStatus)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (printer.status == "ONLINE") "Set Maintenance" else "Set Online")
                }

                OutlinedButton(
                    onClick = { onStatusChange("ONLINE") },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Jam")
                }
            }
        }
    }
}
