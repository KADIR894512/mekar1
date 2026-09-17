package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.UserRole
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminOrderManagementScreen
import com.example.ui.admin.AdminPrinterManagementScreen
import com.example.ui.admin.ApiIntegrationsScreen
import com.example.ui.admin.AppControlCenterScreen
import com.example.ui.admin.AuditLogsScreen
import com.example.ui.admin.CustomerManagementScreen
import com.example.ui.admin.PricingManagerScreen
import com.example.ui.admin.ReportsScreen
import com.example.ui.admin.StaffManagementScreen
import com.example.ui.auth.AuthScreen
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import com.example.ui.components.WebTabletLaptopApkDialog
import com.example.ui.components.RoleBadge
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.CustomerOrderBuilderScreen
import com.example.ui.customer.CustomerOrderTrackingScreen
import com.example.ui.customer.CustomerOrdersHistoryScreen
import com.example.ui.customer.QrPrintScreen
import com.example.ui.cloud.CloudPublicFilesScreen
import androidx.compose.material.icons.filled.CloudUpload
import com.example.ui.operator.PrinterOperatorTerminalScreen
import com.example.ui.staff.StaffPortalScreen
import com.example.ui.theme.MasterPrinterTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterPrinterApp(viewModel: MasterPrinterViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val currentPortal by viewModel.currentPortal.collectAsState()
    val customerScreen by viewModel.customerScreen.collectAsState()
    val adminScreen by viewModel.adminScreen.collectAsState()
    val settings by viewModel.settingsMap.collectAsState()

    val primaryHex = settings["primary_color"]
    val secondaryHex = settings["secondary_color"]
    val isDarkMode = settings["is_dark_mode"]?.toBoolean() ?: false
    val appTitle = settings["app_name"] ?: "MASTER PRINTER"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showPortalSwitchDialog by remember { mutableStateOf(false) }
    var showSessionSecurityDialog by remember { mutableStateOf(false) }
    var showWebTabletLaptopDialog by remember { mutableStateOf(false) }

    // Display global toasts
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    MasterPrinterTheme(
        darkTheme = isDarkMode,
        customPrimaryHex = primaryHex,
        customSecondaryHex = secondaryHex
    ) {
        if (currentUser == null) {
            AuthScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    // Authenticated
                }
            )
        } else {
            val user = currentUser!!
            val userRole = UserRole.fromString(user.role)

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(300.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Column {
                                    Text(appTitle, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                    RoleBadge(role = user.role)
                                }
                            }
                            Text(user.fullName.ifBlank { user.username }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("SWITCH PORTAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            AppPortal.values().forEach { portal ->
                                val hasAccess = userRole.level >= portal.minRole.level
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            portal.title,
                                            fontWeight = if (currentPortal == portal) FontWeight.Bold else FontWeight.Normal,
                                            color = if (hasAccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    },
                                    selected = currentPortal == portal,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        viewModel.switchPortal(portal)
                                    }
                                )
                            }

                            if (currentPortal == AppPortal.ADMIN || currentPortal == AppPortal.SUPER_ADMIN) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("ADMINISTRATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                                val adminScreens = listOf(
                                    AdminNavScreen.DASHBOARD,
                                    AdminNavScreen.ORDERS,
                                    AdminNavScreen.CLOUD_FILES,
                                    AdminNavScreen.PRINTERS,
                                    AdminNavScreen.APP_CONTROL,
                                    AdminNavScreen.PRICING,
                                    AdminNavScreen.INTEGRATIONS,
                                    AdminNavScreen.CUSTOMERS,
                                    AdminNavScreen.STAFF,
                                    AdminNavScreen.REPORTS,
                                    AdminNavScreen.AUDIT_LOGS
                                )

                                adminScreens.forEach { screen ->
                                    NavigationDrawerItem(
                                        label = { Text(screen.title) },
                                        selected = adminScreen == screen,
                                        onClick = {
                                            coroutineScope.launch { drawerState.close() }
                                            viewModel.setAdminScreen(screen)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Card(
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showWebTabletLaptopDialog = true
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Web • Tablet • Laptop", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("Setup & APK Direct Links", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Card(
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showSessionSecurityDialog = true
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Encrypted Token Session", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("AES-256-GCM Keystore", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    viewModel.logout()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out")
                            }
                        }
                    }
                }
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isTabletOrExpanded = maxWidth >= 600.dp

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = appTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${currentPortal.title} • ${user.fullName.ifBlank { user.username }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    RoleBadge(role = user.role)
                                    IconButton(onClick = {
                                        if (currentPortal == AppPortal.CUSTOMER) {
                                            viewModel.setCustomerScreen(CustomerNavScreen.CLOUD_FILES)
                                        } else {
                                            viewModel.setAdminScreen(AdminNavScreen.CLOUD_FILES)
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = "Cloud Public Files",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { showWebTabletLaptopDialog = true }) {
                                        Icon(Icons.Default.Devices, contentDescription = "Website, Tablet & Laptop Setup")
                                    }
                                    IconButton(onClick = { showSessionSecurityDialog = true }) {
                                        Icon(Icons.Default.Security, contentDescription = "Security & Token Details")
                                    }
                                    IconButton(onClick = { showPortalSwitchDialog = true }) {
                                        Icon(Icons.Default.Tune, contentDescription = "Switch Portal")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            if (currentPortal == AppPortal.CUSTOMER && !isTabletOrExpanded) {
                                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                    NavigationBarItem(
                                        selected = customerScreen == CustomerNavScreen.HOME,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.HOME) },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text("Home") }
                                    )
                                    NavigationBarItem(
                                        selected = customerScreen == CustomerNavScreen.ORDER_BUILDER,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.ORDER_BUILDER) },
                                        icon = { Icon(Icons.Default.Print, contentDescription = "Print") },
                                        label = { Text("Print") }
                                    )
                                    NavigationBarItem(
                                        selected = customerScreen == CustomerNavScreen.QR_FLOW,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.QR_FLOW) },
                                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Kiosk QR") },
                                        label = { Text("Kiosk QR") }
                                    )
                                    NavigationBarItem(
                                        selected = customerScreen == CustomerNavScreen.ORDER_TRACKING,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.ORDER_TRACKING) },
                                        icon = { Icon(Icons.Default.Receipt, contentDescription = "Track") },
                                        label = { Text("Track") }
                                    )
                                    NavigationBarItem(
                                        selected = customerScreen == CustomerNavScreen.ORDERS_HISTORY,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.ORDERS_HISTORY) },
                                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                        label = { Text("History") }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            // If tablet/laptop and in customer portal, show NavigationRail for ergonomic side navigation
                            if (currentPortal == AppPortal.CUSTOMER && isTabletOrExpanded) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    header = {
                                        IconButton(onClick = { showWebTabletLaptopDialog = true }) {
                                            Icon(Icons.Default.Devices, contentDescription = "Devices", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                ) {
                                    NavigationRailItem(
                                        selected = customerScreen == CustomerNavScreen.HOME,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.HOME) },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text("Home") }
                                    )
                                    NavigationRailItem(
                                        selected = customerScreen == CustomerNavScreen.ORDER_BUILDER,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.ORDER_BUILDER) },
                                        icon = { Icon(Icons.Default.Print, contentDescription = "Print") },
                                        label = { Text("Print") }
                                    )
                                    NavigationRailItem(
                                        selected = customerScreen == CustomerNavScreen.QR_FLOW,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.QR_FLOW) },
                                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Kiosk QR") },
                                        label = { Text("Kiosk QR") }
                                    )
                                    NavigationRailItem(
                                        selected = customerScreen == CustomerNavScreen.ORDER_TRACKING,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.ORDER_TRACKING) },
                                        icon = { Icon(Icons.Default.Receipt, contentDescription = "Track") },
                                        label = { Text("Track") }
                                    )
                                    NavigationRailItem(
                                        selected = customerScreen == CustomerNavScreen.ORDERS_HISTORY,
                                        onClick = { viewModel.setCustomerScreen(CustomerNavScreen.ORDERS_HISTORY) },
                                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                        label = { Text("History") }
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                when (currentPortal) {
                                    AppPortal.CUSTOMER -> {
                                        when (customerScreen) {
                                            CustomerNavScreen.HOME, CustomerNavScreen.SERVICES_CATALOG -> CustomerHomeScreen(viewModel = viewModel)
                                            CustomerNavScreen.ORDER_BUILDER -> CustomerOrderBuilderScreen(viewModel = viewModel)
                                            CustomerNavScreen.CLOUD_FILES -> CloudPublicFilesScreen(viewModel = viewModel)
                                            CustomerNavScreen.QR_FLOW -> QrPrintScreen(viewModel = viewModel)
                                            CustomerNavScreen.ORDER_TRACKING -> CustomerOrderTrackingScreen(viewModel = viewModel)
                                            CustomerNavScreen.ORDERS_HISTORY -> CustomerOrdersHistoryScreen(viewModel = viewModel)
                                            else -> CustomerHomeScreen(viewModel = viewModel)
                                        }
                                    }
                                    AppPortal.PRINTER_OPERATOR -> {
                                        PrinterOperatorTerminalScreen(viewModel = viewModel)
                                    }
                                    AppPortal.STAFF -> {
                                        StaffPortalScreen(viewModel = viewModel)
                                    }
                                    AppPortal.ADMIN, AppPortal.SUPER_ADMIN -> {
                                        when (adminScreen) {
                                            AdminNavScreen.DASHBOARD -> AdminDashboardScreen(viewModel = viewModel)
                                            AdminNavScreen.ORDERS -> AdminOrderManagementScreen(viewModel = viewModel)
                                            AdminNavScreen.CLOUD_FILES -> CloudPublicFilesScreen(viewModel = viewModel)
                                            AdminNavScreen.PRINTERS, AdminNavScreen.QUEUE -> AdminPrinterManagementScreen(viewModel = viewModel)
                                            AdminNavScreen.APP_CONTROL -> AppControlCenterScreen(viewModel = viewModel)
                                            AdminNavScreen.PRICING -> PricingManagerScreen(viewModel = viewModel)
                                            AdminNavScreen.INTEGRATIONS -> ApiIntegrationsScreen(viewModel = viewModel)
                                            AdminNavScreen.CUSTOMERS -> CustomerManagementScreen(viewModel = viewModel)
                                            AdminNavScreen.STAFF -> StaffManagementScreen(viewModel = viewModel)
                                            AdminNavScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                                            AdminNavScreen.AUDIT_LOGS -> AuditLogsScreen(viewModel = viewModel)
                                            else -> AdminDashboardScreen(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showPortalSwitchDialog) {
                Dialog(onDismissRequest = { showPortalSwitchDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Switch Portal View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Current Account: ${user.username} (${userRole.displayName})", style = MaterialTheme.typography.bodySmall)

                            AppPortal.values().forEach { portal ->
                                val hasAccess = userRole.level >= portal.minRole.level
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (currentPortal == portal) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(portal.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Requires ${portal.minRole.displayName}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.switchPortal(portal)
                                                showPortalSwitchDialog = false
                                            },
                                            enabled = hasAccess,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Switch")
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showPortalSwitchDialog = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }

            if (showSessionSecurityDialog) {
                val metadata = viewModel.sessionManager.getSessionMetadata()
                val permissions = viewModel.sessionManager.effectivePermissions.collectAsState().value
                val remainingMinutes = metadata.remainingSeconds / 60

                Dialog(onDismissRequest = { showSessionSecurityDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Column {
                                    Text("User Session Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Hardware Keystore Security", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Active Account", style = MaterialTheme.typography.labelSmall)
                                        Text(user.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Assigned Role", style = MaterialTheme.typography.labelSmall)
                                        RoleBadge(role = user.role)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Clearance Level", style = MaterialTheme.typography.labelSmall)
                                        Text("${userRole.level}/100", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Encryption Standard", style = MaterialTheme.typography.labelSmall)
                                        Text("AES-256-GCM", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Token Preview", style = MaterialTheme.typography.labelSmall)
                                        Text(metadata.tokenPreview ?: "N/A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Session Validity", style = MaterialTheme.typography.labelSmall)
                                        Text("${remainingMinutes / 60}h ${remainingMinutes % 60}m left", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Effective Permissions (${permissions.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                if (permissions.isEmpty()) {
                                    Text("Standard Customer Access (Self-Service)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    permissions.take(6).forEach { perm ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text(perm.description, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    if (permissions.size > 6) {
                                        Text("+ ${permissions.size - 6} additional administrative permissions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val newToken = viewModel.sessionManager.generateSecureToken("mp_auth_")
                                        val newRefreshToken = viewModel.sessionManager.generateRefreshToken("mp_rf_")
                                        viewModel.sessionManager.refreshAuthToken(newToken, newRefreshToken)
                                        Toast.makeText(context, "Authentication token rotated and encrypted", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rotate Token", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = { showSessionSecurityDialog = false },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            if (showWebTabletLaptopDialog) {
                WebTabletLaptopApkDialog(
                    onDismiss = { showWebTabletLaptopDialog = false }
                )
            }
        }
    }
}
