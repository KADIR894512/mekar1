package com.example.ui.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AdminNavScreen
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val totalOrders by viewModel.totalOrdersCount.collectAsState()
    val totalRev by viewModel.totalRevenue.collectAsState()
    val pendingCount by viewModel.pendingOrdersCount.collectAsState()
    val printingCount by viewModel.printingOrdersCount.collectAsState()
    val completedCount by viewModel.completedOrdersCount.collectAsState()
    val failedCount by viewModel.failedOrdersCount.collectAsState()
    val refundCount by viewModel.refundOrdersCount.collectAsState()
    val customerCount by viewModel.customerCount.collectAsState()
    val activePrinters by viewModel.activePrinterCount.collectAsState()
    val offlinePrinters by viewModel.offlinePrinterCount.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MASTER PRINTER Executive Control",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Real-time production metrics, active hardware fleet, and revenue analytics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Top Financial & Order Stat Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Revenue",
                    value = "₹${String.format(Locale.US, "%.2f", totalRev)}",
                    subtitle = "100% Verified settlements",
                    icon = Icons.Default.AttachMoney,
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Orders",
                    value = "$totalOrders",
                    subtitle = "$completedCount fulfilled",
                    icon = Icons.Default.ShoppingCart,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Production Operations Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Printing Now",
                    value = "$printingCount",
                    subtitle = "$pendingCount pending in queue",
                    icon = Icons.Default.Print,
                    iconColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Online Hardware",
                    value = "$activePrinters",
                    subtitle = "$offlinePrinters attention needed",
                    icon = Icons.Default.Layers,
                    iconColor = if (offlinePrinters > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary KPI Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Active Customers",
                    value = "$customerCount",
                    icon = Icons.Default.People,
                    iconColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Refunds / Failed",
                    value = "${refundCount + failedCount}",
                    subtitle = "$refundCount refunds processed",
                    icon = Icons.Default.RemoveShoppingCart,
                    iconColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Production & Order Volume Chart
        item {
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
                                text = "Weekly Order Volume & Spool Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily job count across all walk-in and cloud submissions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SimpleBarChart(
                        labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                        values = listOf(14f, 22f, 35f, 28f, 45f, 52f, 18f),
                        chartColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Operational Quick Actions
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Administrative Shortcuts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setAdminScreen(AdminNavScreen.QUEUE) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Print Queue")
                        }
                        Button(
                            onClick = { viewModel.setAdminScreen(AdminNavScreen.PRINTERS) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Printers Fleet")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setAdminScreen(AdminNavScreen.APP_CONTROL) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("App Control Center")
                        }
                        OutlinedButton(
                            onClick = { viewModel.setAdminScreen(AdminNavScreen.PRICING) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pricing & Rules")
                        }
                    }
                }
            }
        }
    }
}
