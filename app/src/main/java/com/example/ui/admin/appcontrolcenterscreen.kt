package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.BannerEntity
import com.example.ui.MasterPrinterViewModel
import com.example.ui.security.BiometricSecuredBadge
import com.example.ui.security.rememberBiometricSecurityController
import com.example.ui.theme.parseHexColor

@Composable
fun AppControlCenterScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsMap.collectAsState()
    val banners by viewModel.allBanners.collectAsState()

    var appName by remember(settings) { mutableStateOf(settings["app_name"] ?: "MASTER PRINTER") }
    var storeName by remember(settings) { mutableStateOf(settings["store_name"] ?: "MASTER PRINTER Express Studio") }
    var primaryColor by remember(settings) { mutableStateOf(settings["primary_color"] ?: "#0066FF") }
    var secondaryColor by remember(settings) { mutableStateOf(settings["secondary_color"] ?: "#0F172A") }
    var isDarkMode by remember(settings) { mutableStateOf((settings["is_dark_mode"] ?: "false").toBoolean()) }

    var heroTitle by remember(settings) { mutableStateOf(settings["hero_title"] ?: "MASTER PRINTER Express Studio") }
    var heroSubtitle by remember(settings) { mutableStateOf(settings["hero_subtitle"] ?: "Instant walk-in & cloud laser printing, thesis binding & high-gloss photo services.") }
    var announcement by remember(settings) { mutableStateOf(settings["announcement"] ?: "⚡ 2400 DPI Photo Gloss & Spiral Binding ready in 10 minutes.") }
    var kioskCode by remember(settings) { mutableStateOf(settings["kiosk_code"] ?: "MP-KIOSK-01") }

    var phone by remember(settings) { mutableStateOf(settings["business_phone"] ?: "+91 98765 43210") }
    var whatsapp by remember(settings) { mutableStateOf(settings["business_whatsapp"] ?: "+91 98765 43210") }
    var email by remember(settings) { mutableStateOf(settings["business_email"] ?: "orders@masterprinter.com") }
    var address by remember(settings) { mutableStateOf(settings["business_address"] ?: "Plot 42, Digital Print Avenue, Tech Park") }

    var editingBanner by remember { mutableStateOf<BannerEntity?>(null) }
    var showAddBannerDialog by remember { mutableStateOf(false) }
    val (biometricGuard, BiometricFallbackDialog) = rememberBiometricSecurityController(viewModel)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Control Center",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    BiometricSecuredBadge(text = "Master Admin")
                }
                Text(
                    text = "Dynamically control branding, homepage banners, and business info without rebuilding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Dynamic Branding & Theming
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Brand Identity & Theme Palette", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("Application Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store / Hub Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = primaryColor,
                            onValueChange = { primaryColor = it },
                            label = { Text("Primary Brand Hex") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(primaryColor, MaterialTheme.colorScheme.primary))
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = secondaryColor,
                            onValueChange = { secondaryColor = it },
                            label = { Text("Secondary Brand Hex") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(secondaryColor, Color.Black))
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enforce Dark Canvas Mode", fontWeight = FontWeight.SemiBold)
                            Text("Sets high-contrast dark palette for all clients", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it })
                    }

                    Button(
                        onClick = {
                            biometricGuard.authenticate(
                                title = "Update Store Branding",
                                subtitle = "Authorize applying live theme, palette and store brand identity",
                                description = "Admin biometric authorization required to modify visual theme."
                            ) {
                                viewModel.updateSetting("app_name", appName)
                                viewModel.updateSetting("store_name", storeName)
                                viewModel.updateSetting("primary_color", primaryColor)
                                viewModel.updateSetting("secondary_color", secondaryColor)
                                viewModel.updateSetting("is_dark_mode", isDarkMode.toString())
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Branding Live")
                    }
                }
            }
        }

        // 2. Homepage Content Manager
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Homepage Copy & Kiosk Config", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = heroTitle,
                        onValueChange = { heroTitle = it },
                        label = { Text("Hero Banner Headline") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = heroSubtitle,
                        onValueChange = { heroSubtitle = it },
                        label = { Text("Hero Banner Subtitle") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = { Text("Top Announcement Ticker") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = kioskCode,
                        onValueChange = { kioskCode = it },
                        label = { Text("Store Kiosk QR Station Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            biometricGuard.authenticate(
                                title = "Publish Store Content",
                                subtitle = "Authorize publishing customer portal & kiosk text changes",
                                description = "Admin biometric authorization required to update storefront copy."
                            ) {
                                viewModel.updateSetting("hero_title", heroTitle)
                                viewModel.updateSetting("hero_subtitle", heroSubtitle)
                                viewModel.updateSetting("announcement", announcement)
                                viewModel.updateSetting("kiosk_code", kioskCode)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Publish Homepage Updates")
                    }
                }
            }
        }

        // 3. Business Location & Helpline
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Store Location & Helplines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Helpline Phone") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp Orders") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Business Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Physical Studio Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            biometricGuard.authenticate(
                                title = "Update Business Helplines",
                                subtitle = "Authorize updating public support contacts and studio address",
                                description = "Admin biometric authorization required to update studio location."
                            ) {
                                viewModel.updateSetting("business_phone", phone)
                                viewModel.updateSetting("business_whatsapp", whatsapp)
                                viewModel.updateSetting("business_email", email)
                                viewModel.updateSetting("business_address", address)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Business Contact")
                    }
                }
            }
        }

        // 4. Promotional Banners Manager
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Promotional Banners (${banners.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAddBannerDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Banner")
                }
            }
        }

        items(banners) { banner ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(banner.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(banner.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Button: ${banner.buttonText} → ${banner.actionDestination}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            biometricGuard.authenticate(
                                title = "Delete Promotional Asset",
                                subtitle = "Authorize permanent deletion of banner '${banner.title}'",
                                description = "Admin biometric authorization required to remove marketing assets."
                            ) {
                                viewModel.deleteBanner(banner.id)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Add Banner Dialog
    if (showAddBannerDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newSubtitle by remember { mutableStateOf("") }
        var newButtonText by remember { mutableStateOf("View Details") }
        var newDestination by remember { mutableStateOf("order") }

        Dialog(onDismissRequest = { showAddBannerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create Promotional Banner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Banner Headline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSubtitle,
                        onValueChange = { newSubtitle = it },
                        label = { Text("Banner Subtitle / Offer Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newButtonText,
                        onValueChange = { newButtonText = it },
                        label = { Text("Button Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showAddBannerDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    biometricGuard.authenticate(
                                        title = "Publish New Banner",
                                        subtitle = "Deploy banner '$newTitle' across store apps & kiosks",
                                        description = "Admin biometric authentication required to publish marketing banners."
                                    ) {
                                        viewModel.saveBanner(
                                            BannerEntity(
                                                title = newTitle,
                                                subtitle = newSubtitle,
                                                buttonText = newButtonText,
                                                actionDestination = newDestination,
                                                isActive = true,
                                                displayOrder = banners.size + 1
                                            )
                                        )
                                        showAddBannerDialog = false
                                    }
                                }
                            }
                        ) {
                            Text("Add Banner")
                        }
                    }
                }
            }
        }
    }

    BiometricFallbackDialog()
}
