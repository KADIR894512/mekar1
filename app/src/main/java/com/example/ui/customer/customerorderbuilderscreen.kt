package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.local.entities.ServiceEntity
import com.example.ui.CustomerNavScreen
import com.example.ui.MasterPrinterViewModel
import java.util.Locale

@Composable
fun CustomerOrderBuilderScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val services by viewModel.customerServices.collectAsState()
    val pricingRules by viewModel.pricingRules.collectAsState()
    val rulesMap = remember(pricingRules) { pricingRules.associate { it.ruleKey to it.value } }

    // Upload state
    var documentName by remember { mutableStateOf("Annual_Financial_Report_2026.pdf") }
    var documentSizeKb by remember { mutableStateOf(1450L) }
    var pageCount by remember { mutableIntStateOf(12) }
    var hasUploadedFile by remember { mutableStateOf(true) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Service & Specs
    var selectedService by remember(services) { mutableStateOf<ServiceEntity?>(services.firstOrNull()) }
    var paperSize by remember { mutableStateOf("A4") }
    var isColor by remember { mutableStateOf(false) }
    var isDuplex by remember { mutableStateOf(true) }
    var copies by remember { mutableIntStateOf(1) }
    var pageRange by remember { mutableStateOf("All Pages") }
    var paperType by remember { mutableStateOf("Standard 75gsm Bond") }
    var orientation by remember { mutableStateOf("Portrait") }

    // Finishing
    var binding by remember { mutableStateOf("None") }
    var lamination by remember { mutableStateOf("None") }
    var scanning by remember { mutableStateOf(false) }

    // Coupon
    var couponInput by remember { mutableStateOf("") }
    var appliedCoupon by remember { mutableStateOf<String?>(null) }
    var couponDiscountAmount by remember { mutableStateOf(0.0) }
    var couponMessage by remember { mutableStateOf<String?>(null) }

    // Calculate dynamic price
    val pageRate = if (isColor) (rulesMap["page_color"] ?: 10.0) else (rulesMap["page_bw"] ?: 2.0)
    val paperRate = when (paperSize) {
        "A3" -> rulesMap["paper_a3"] ?: 2.0
        "A5" -> rulesMap["paper_a5"] ?: 0.3
        "Letter" -> rulesMap["paper_letter"] ?: 0.5
        "Legal" -> rulesMap["paper_legal"] ?: 1.0
        else -> rulesMap["paper_a4"] ?: 0.5
    }

    val totalPages = pageCount * copies
    var rawSubtotal = totalPages * (pageRate + paperRate)
    if (isDuplex) {
        val duplexDiscount = rulesMap["duplex_discount_pct"] ?: 10.0
        rawSubtotal *= (1.0 - (duplexDiscount / 100.0))
    }

    // Finishing add-ons
    var finishingTotal = 0.0
    if (binding == "Spiral Wire Binding") finishingTotal += (rulesMap["binding_spiral"] ?: 50.0) * copies
    if (binding == "Hardcover Foil") finishingTotal += (rulesMap["binding_hardcover"] ?: 150.0) * copies
    if (lamination == "Gloss Lamination") finishingTotal += (rulesMap["lamination_gloss"] ?: 30.0) * pageCount * copies
    if (lamination == "Matte Lamination") finishingTotal += (rulesMap["lamination_matte"] ?: 40.0) * pageCount * copies
    if (scanning) finishingTotal += (rulesMap["scanning_per_page"] ?: 5.0) * pageCount

    rawSubtotal += finishingTotal
    val minOrder = rulesMap["min_order_charge"] ?: 10.0
    if (rawSubtotal < minOrder) rawSubtotal = minOrder

    val taxable = maxOf(0.0, rawSubtotal - couponDiscountAmount)
    val taxRate = (rulesMap["gst_tax_percent"] ?: 18.0) / 100.0
    val gstTax = taxable * taxRate
    val finalTotal = taxable + gstTax

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Print Job",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 1. Document Upload Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "1. DOCUMENT ATTACHMENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (hasUploadedFile) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(documentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("$pageCount pages • ${(documentSizeKb / 1024.0).format(2)} MB • PDF/Print-ready", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { showPreviewDialog = true }) {
                                Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { hasUploadedFile = false }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                            .clickable {
                                documentName = "Contract_Master_Print_Doc.pdf"
                                documentSizeKb = 2048
                                pageCount = 8
                                hasUploadedFile = true
                            }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Text("Click to Upload PDF, Word, or High-Res Image", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Max file size 100MB • Auto page count detection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 2. Service & Paper Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "2. PRINT SERVICE & SPECS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("Select Paper Size", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A4", "A3", "A5", "Letter", "Legal").forEach { size ->
                        FilterChip(
                            selected = paperSize == size,
                            onClick = { paperSize = size },
                            label = { Text(size) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Print Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(if (isColor) "Colour (Vibrant Laser HD)" else "Black & White (Sharp Laser)", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isColor,
                        onCheckedChange = { isColor = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Duplex (Double-Sided)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Save paper and receive 10% duplex discount", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isDuplex,
                        onCheckedChange = { isDuplex = it }
                    )
                }

                // Copies & Page count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Number of Copies", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (copies > 1) copies-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        Text("$copies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { copies++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }

                Text("Paper Grade", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Standard 75gsm", "Premium 100gsm", "Glossy 260gsm").forEach { grade ->
                        FilterChip(
                            selected = paperType.startsWith(grade.take(8)),
                            onClick = { paperType = grade },
                            label = { Text(grade) }
                        )
                    }
                }
            }
        }

        // 3. Finishing & Post-Processing
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "3. BINDING & FINISHING (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("Booklet Binding", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("None", "Spiral Wire Binding", "Hardcover Foil").forEach { opt ->
                        FilterChip(
                            selected = binding == opt,
                            onClick = { binding = opt },
                            label = { Text(opt) }
                        )
                    }
                }

                Text("Surface Lamination", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("None", "Gloss Lamination", "Matte Lamination").forEach { opt ->
                        FilterChip(
                            selected = lamination == opt,
                            onClick = { lamination = opt },
                            label = { Text(opt) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Include High-Res Optical Scan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Scan output directly to cloud PDF (+₹5/page)", style = MaterialTheme.typography.labelSmall)
                    }
                    Checkbox(checked = scanning, onCheckedChange = { scanning = it })
                }
            }
        }

        // 4. Coupon Code
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "4. PROMO CODE / COUPON",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = couponInput,
                        onValueChange = { couponInput = it },
                        placeholder = { Text("e.g. WELCOME10, PRINT50") },
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val code = couponInput.trim().uppercase()
                            if (code == "WELCOME10") {
                                appliedCoupon = "WELCOME10"
                                couponDiscountAmount = rawSubtotal * 0.10
                                couponMessage = "Applied! 10% discount subtracted."
                            } else if (code == "PRINT50") {
                                if (rawSubtotal >= 100) {
                                    appliedCoupon = "PRINT50"
                                    couponDiscountAmount = 50.0
                                    couponMessage = "Applied! Flat ₹50 discount subtracted."
                                } else {
                                    couponMessage = "Requires min order of ₹100."
                                }
                            } else {
                                couponMessage = "Invalid or expired coupon code."
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply")
                    }
                }
                if (couponMessage != null) {
                    Text(
                        text = couponMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (appliedCoupon != null) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // 5. Itemized Price Breakdown & Place Order
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LIVE PRICE BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$pageCount pgs × $copies copies (${if (isColor) "Colour" else "B&W"}, $paperSize)")
                    Text("₹${String.format(Locale.US, "%.2f", rawSubtotal - finishingTotal)}")
                }

                if (finishingTotal > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Finishing & Binding Add-ons")
                        Text("+₹${String.format(Locale.US, "%.2f", finishingTotal)}")
                    }
                }

                if (couponDiscountAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Coupon Discount ($appliedCoupon)", color = Color(0xFF10B981))
                        Text("-₹${String.format(Locale.US, "%.2f", couponDiscountAmount)}", color = Color(0xFF10B981))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GST / Tax (${(rulesMap["gst_tax_percent"] ?: 18.0).toInt()}%)")
                    Text("+₹${String.format(Locale.US, "%.2f", gstTax)}")
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Calculated Price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "₹${String.format(Locale.US, "%.2f", finalTotal)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val activeService = selectedService ?: services.firstOrNull() ?: return@Button
                        viewModel.placeCustomerOrder(
                            service = activeService,
                            paperSize = paperSize,
                            isColor = isColor,
                            isDuplex = isDuplex,
                            copies = copies,
                            pageRange = pageRange,
                            pageCount = pageCount,
                            paperType = paperType,
                            binding = binding,
                            lamination = lamination,
                            scanning = scanning,
                            thermal = false,
                            documentName = documentName,
                            documentSizeBytes = documentSizeKb * 1024,
                            couponCode = appliedCoupon,
                            onSuccess = { order ->
                                viewModel.setCustomerScreen(CustomerNavScreen.ORDER_TRACKING)
                            },
                            onError = { err ->
                                // handled in toast
                            }
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Order & Proceed to Payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Document Preview Dialog
    if (showPreviewDialog) {
        Dialog(onDismissRequest = { showPreviewDialog = false }) {
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
                        Text("Document Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showPreviewDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Simulated high-fidelity sheet preview
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MASTER PRINTER PREVIEW", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Page 1 of $pageCount", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Divider()
                            Text(documentName, style = MaterialTheme.typography.titleSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                            Text(
                                "Specifications: $paperSize, ${if (isColor) "Colour" else "Monochrome"}, ${if (isDuplex) "Duplex" else "Single-Sided"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Placeholder lines for preview
                            repeat(5) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (it == 4) 0.6f else 1f)
                                        .height(10.dp)
                                        .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showPreviewDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Preview")
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(Locale.US, this)
