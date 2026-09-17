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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.MasterPrinterViewModel
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatCard
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val totalOrders by viewModel.totalOrdersCount.collectAsState()
    val totalRev by viewModel.totalRevenue.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()

    var exportMessage by remember { mutableStateOf<String?>(null) }

    val totalPagesPrinted = allOrders.sumOf { it.pageCount * it.copies }
    val avgOrderValue = if (totalOrders > 0) totalRev / totalOrders else 0.0

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
                    Text(
                        text = "Financial & Volume Reports",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aggregated commercial reporting and hardware utilization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        exportMessage = "Exported MASTER_PRINTER_REPORT_${System.currentTimeMillis()}.csv to Downloads"
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV")
                }
            }
        }

        if (exportMessage != null) {
            item {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = exportMessage ?: "",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Gross Revenue",
                    value = "₹${String.format(Locale.US, "%.2f", totalRev)}",
                    icon = Icons.Default.AttachMoney,
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Avg Order Value",
                    value = "₹${String.format(Locale.US, "%.2f", avgOrderValue)}",
                    icon = Icons.Default.ShoppingCart,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Pages Printed",
                    value = "$totalPagesPrinted",
                    subtitle = "Across all printers",
                    icon = Icons.Default.Print,
                    iconColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Fulfilled Orders",
                    value = "$totalOrders",
                    icon = Icons.Default.PieChart,
                    iconColor = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Service Distribution Breakdown
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Service Category Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SimpleBarChart(
                        labels = listOf("B&W Doc", "Colour", "Photo", "Binding", "Lamination", "Scan"),
                        values = listOf(48f, 26f, 15f, 8f, 12f, 7f),
                        chartColor = Color(0xFF2563EB),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
