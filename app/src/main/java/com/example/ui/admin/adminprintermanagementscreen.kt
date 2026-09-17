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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.PrinterEntity
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.PrinterStatusBadge
import com.example.ui.security.BiometricSecuredBadge
import com.example.ui.security.rememberBiometricSecurityController

@Composable
fun AdminPrinterManagementScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val printers by viewModel.allPrinters.collectAsState()
    var showAddPrinterDialog by remember { mutableStateOf(false) }
    var editingPrinter by remember { mutableStateOf<PrinterEntity?>(null) }
    val (biometricGuard, BiometricFallbackDialog) = rememberBiometricSecurityController(viewModel)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            text = "MASTER PRINTER Systems",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        BiometricSecuredBadge(text = "Admin Fleet")
                    }
                    Text(
                        text = "Hardware fleet, spooler daemons, and station assignment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddPrinterDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Printer")
                }
            }
        }

        items(printers) { printer ->
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(printer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (printer.isDefault) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("DEFAULT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("${printer.manufacturer} ${printer.model} • ${printer.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PrinterStatusBadge(status = printer.status)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Protocol: ${printer.connectionType} • IP: ${printer.ipAddress}:${printer.port}", style = MaterialTheme.typography.labelSmall)
                            Text("Capabilities: Paper: ${printer.paperSizesSupported} • Color: ${if (printer.colorSupport) "Yes" else "No"} • Duplex: ${if (printer.duplexSupport) "Yes" else "No"}", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val next = if (printer.status == "ONLINE") "MAINTENANCE" else "ONLINE"
                                biometricGuard.authenticate(
                                    title = "Hardware State Change",
                                    subtitle = "Authorize setting ${printer.name} to $next mode",
                                    description = "Admin biometric authorization required to adjust hardware state."
                                ) {
                                    viewModel.updatePrinterStatus(printer.id, next)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (printer.status == "ONLINE") "Set Maintenance" else "Set Online", style = MaterialTheme.typography.labelMedium)
                        }

                        IconButton(onClick = { editingPrinter = printer }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }

                        IconButton(
                            onClick = {
                                biometricGuard.authenticate(
                                    title = "Delete Hardware Terminal",
                                    subtitle = "Permanently remove ${printer.name} (${printer.ipAddress})",
                                    description = "Admin biometric authorization required for destructive hardware removal."
                                ) {
                                    viewModel.deletePrinter(printer.id)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddPrinterDialog || editingPrinter != null) {
        val editing = editingPrinter
        var pName by remember { mutableStateOf(editing?.name ?: "") }
        var pManufacturer by remember { mutableStateOf(editing?.manufacturer ?: "Canon") }
        var pModel by remember { mutableStateOf(editing?.model ?: "imageRUNNER DX C5840i") }
        var pConnection by remember { mutableStateOf(editing?.connectionType ?: "Wi-Fi/LAN") }
        var pIp by remember { mutableStateOf(editing?.ipAddress ?: "192.168.1.105") }
        var pPort by remember { mutableStateOf((editing?.port ?: 9100).toString()) }
        var pLocation by remember { mutableStateOf(editing?.location ?: "Express Counter") }
        var pSizes by remember { mutableStateOf(editing?.paperSizesSupported ?: "A4, A3, A5") }
        var pColor by remember { mutableStateOf(editing?.colorSupport ?: true) }
        var pDuplex by remember { mutableStateOf(editing?.duplexSupport ?: true) }

        Dialog(onDismissRequest = { showAddPrinterDialog = false; editingPrinter = null }) {
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
                    Text(
                        text = if (editing != null) "Edit Printer Hardware" else "Add New MASTER PRINTER Unit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pManufacturer,
                            onValueChange = { pManufacturer = it },
                            label = { Text("Manufacturer") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pModel,
                            onValueChange = { pModel = it },
                            label = { Text("Model") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = pLocation,
                        onValueChange = { pLocation = it },
                        label = { Text("Location / Studio Bay") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pIp,
                            onValueChange = { pIp = it },
                            label = { Text("IP Address / Host") },
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = pPort,
                            onValueChange = { pPort = it },
                            label = { Text("Port (9100/RAW)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Connection Protocol", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Wi-Fi/LAN", "Bluetooth", "USB/OTG", "Cloud Daemon").forEach { conn ->
                            FilterChip(
                                selected = pConnection == conn,
                                onClick = { pConnection = conn },
                                label = { Text(conn, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color Printing Capable")
                        Switch(checked = pColor, onCheckedChange = { pColor = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hardware Duplex Unit")
                        Switch(checked = pDuplex, onCheckedChange = { pDuplex = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showAddPrinterDialog = false; editingPrinter = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pName.isNotBlank()) {
                                    val printerEntity = PrinterEntity(
                                        id = editing?.id ?: 0L,
                                        printerIdentifier = editing?.printerIdentifier ?: "PRT-${System.currentTimeMillis() % 1000}",
                                        name = pName,
                                        manufacturer = pManufacturer,
                                        model = pModel,
                                        connectionType = pConnection,
                                        ipAddress = pIp,
                                        port = pPort.toIntOrNull() ?: 9100,
                                        location = pLocation,
                                        paperSizesSupported = pSizes,
                                        colorSupport = pColor,
                                        duplexSupport = pDuplex,
                                        status = editing?.status ?: "ONLINE",
                                        isEnabled = true,
                                        isDefault = editing?.isDefault ?: false
                                    )
                                    biometricGuard.authenticate(
                                        title = if (editing != null) "Update Printer Profile" else "Provision Hardware Fleet Unit",
                                        subtitle = "Authorize saving configuration for $pName",
                                        description = "Admin biometric verification required to deploy printer profile."
                                    ) {
                                        viewModel.savePrinter(printerEntity)
                                        showAddPrinterDialog = false
                                        editingPrinter = null
                                    }
                                }
                            }
                        ) {
                            Text("Save Printer")
                        }
                    }
                }
            }
        }
    }

    BiometricFallbackDialog()
}
