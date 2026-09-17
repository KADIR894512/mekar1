package com.example.ui.cloud

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.CloudPublicFile
import com.example.ui.MasterPrinterViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CloudPublicFilesScreen(
    viewModel: MasterPrinterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val publicFiles by viewModel.publicFiles.collectAsState()
    val firebaseConnState by viewModel.firebaseConnectionState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Files, 1: Publish New, 2: Kiosk Code Finder
    var searchQuery by remember { mutableStateOf("") }
    var selectedFileForQr by remember { mutableStateOf<CloudPublicFile?>(null) }
    var filterType by remember { mutableStateOf("ALL") }

    // New file form fields
    var fileTitle by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("PDF") }
    var fileSizeKb by remember { mutableStateOf("2450") }
    var pageCount by remember { mutableStateOf("4") }
    var isPublic by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    // Quick preset files to simulate instant document uploads
    val documentPresets = listOf(
        Triple("Project Specification Report", "Project_Specification_Report.pdf", "PDF"),
        Triple("Cadastral Blueprint Drawing", "Cadastral_Civil_Drawing_A1.pdf", "BLUEPRINT"),
        Triple("Corporate Visiting Card Front-Back", "Visiting_Card_DieCut.pdf", "VISITING_CARD"),
        Triple("Multipage Restaurant Menu Booklet", "Restaurant_Menu_Deluxe.pdf", "DOCUMENT"),
        Triple("High-Gloss Promotional Poster", "Mega_Sale_Poster_A2.png", "IMAGE")
    )

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    val filteredFiles = publicFiles.filter { file ->
        val matchesQuery = file.title.contains(searchQuery, ignoreCase = true) ||
                file.fileName.contains(searchQuery, ignoreCase = true) ||
                file.publicPin.contains(searchQuery, ignoreCase = true)
        val matchesType = filterType == "ALL" || file.fileType == filterType
        matchesQuery && matchesType
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header Banner with Data Connection Status
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF101726)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cloud_data_status_banner")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Connected",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CLOUD DATA CONNECTED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE SYNC ACTIVE",
                                    color = Color(0xFF22C55E),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "डाटा से कनेक्टेड है • फ़ाइलें पब्लिक करें, QR कोड बनाएं और कहीं से भी प्रिंट करें",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = { selectedTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("फाइल पब्लिक करें", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            indicator = {},
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            "पब्लिक फ़ाइलें (${publicFiles.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            "नई फाइल पब्लिक करें",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            "Kiosk QR & PIN",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> {
                // All Public Files Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("फाइल नाम, शीर्षक या PIN से खोजें...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("public_files_search"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type filter chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("ALL", "PDF", "BLUEPRINT", "VISITING_CARD", "DOCUMENT", "IMAGE").forEach { type ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "कोई फाइल नहीं मिली",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ऊपर दिए गए 'नई फाइल पब्लिक करें' बटन पर क्लिक करके पहली फाइल पब्लिश करें",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { selectedTab = 1 },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("फाइल अपलोड व पब्लिक करें")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFiles, key = { it.id }) { file ->
                            PublicFileItemCard(
                                file = file,
                                onShowQr = { selectedFileForQr = file },
                                onCopyLink = { copyToClipboard(file.publicUrl, "Public URL") },
                                onCopyPin = { copyToClipboard(file.publicPin, "Kiosk PIN") },
                                onToggleVisibility = { isPub -> viewModel.togglePublicFileVisibility(file.id, isPub) },
                                onDelete = { viewModel.deleteCloudFile(file.id) },
                                onPrint = {
                                    viewModel.recordFilePrint(file.id)
                                    Toast.makeText(context, "'${file.title}' को प्रिंट कतार में भेजा गया!", Toast.LENGTH_SHORT).show()
                                },
                                onDownload = {
                                    viewModel.recordFileDownload(file.id)
                                    Toast.makeText(context, "डाउनलोड लिंक खोला गया", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Publish New File Form
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📄 क्लाउड पर नई फाइल पब्लिक करें (Publish File to Cloud)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "फ़ाइल को पब्लिश करने के बाद यह तुरंत लाइव लिंक, Kiosk 6-Digit PIN और QR कोड के साथ तैयार हो जाएगी।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = "⚡ त्वरित दस्तावेज चुनें (Quick Presets):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    documentPresets.forEach { (pTitle, pFileName, pType) ->
                                        OutlinedButton(
                                            onClick = {
                                                fileTitle = pTitle
                                                fileName = pFileName
                                                fileType = pType
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(pTitle, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedTextField(
                                    value = fileTitle,
                                    onValueChange = { fileTitle = it },
                                    label = { Text("फाइल का शीर्षक (Title)*") },
                                    placeholder = { Text("उदा. वार्षिक मूल्य सूची 2026") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pub_file_title_input"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = fileName,
                                        onValueChange = { fileName = it },
                                        label = { Text("फ़ाइल का नाम (File Name)*") },
                                        placeholder = { Text("Document.pdf") },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("pub_file_name_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = fileType,
                                        onValueChange = { fileType = it.uppercase() },
                                        label = { Text("प्रकार (Type)") },
                                        placeholder = { Text("PDF / CAD / IMG") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = pageCount,
                                        onValueChange = { pageCount = it },
                                        label = { Text("पेज संख्या (Pages)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = fileSizeKb,
                                        onValueChange = { fileSizeKb = it },
                                        label = { Text("साइज (KB)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("विवरण व प्रिंट निर्देश (Description / Instructions)") },
                                    placeholder = { Text("कस्टमर व कियोस्क ऑपरेटर्स के लिए निर्देश...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    maxLines = 3
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "सार्वजनिक रूप से उपलब्ध रखें (Make Public)",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "सभी कियोस्क, ग्राहक व वेब ब्राउज़र से डायरेक्ट एक्सेस व प्रिंट की अनुमति दें",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Switch(
                                            checked = isPublic,
                                            onCheckedChange = { isPublic = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF0284C7),
                                                checkedTrackColor = Color(0xFF0284C7).copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (fileName.isBlank() && fileTitle.isBlank()) {
                                            Toast.makeText(context, "कृपया शीर्षक या फाइल नाम दर्ज करें", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isPublishing = true
                                        val actualTitle = fileTitle.ifBlank { fileName }
                                        val actualFileName = if (fileName.isBlank()) "$actualTitle.pdf" else fileName
                                        val pages = pageCount.toIntOrNull() ?: 1
                                        val size = fileSizeKb.toLongOrNull() ?: 1024L

                                        viewModel.publishCloudFile(
                                            title = actualTitle,
                                            fileName = actualFileName,
                                            fileType = fileType.ifBlank { "PDF" },
                                            fileSizeKb = size,
                                            pageCount = pages,
                                            isPublic = isPublic,
                                            description = description
                                        ) { published ->
                                            isPublishing = false
                                            selectedTab = 0
                                            selectedFileForQr = published
                                            Toast.makeText(context, "फ़ाइल सफलतापूर्वक पब्लिक की गई!", Toast.LENGTH_LONG).show()
                                            fileTitle = ""
                                            fileName = ""
                                            description = ""
                                        }
                                    },
                                    enabled = !isPublishing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("publish_file_submit_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isPublishing) "पब्लिश हो रहा है..." else "क्लाउड पर पब्लिक करें व QR बनाएं",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Kiosk QR & PIN Finder Tab
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E202A)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "📱 Kiosk Direct QR & 6-Digit PIN System",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFF8DC)
                                )
                                Text(
                                    text = "ग्राहक किसी भी Master Printer कियोस्क या टचस्क्रीन टर्मिनल पर 6-Digit PIN डालकर अपनी पब्लिक फाइल तुरंत प्रिंट कर सकते हैं।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )

                                Divider(color = Color(0xFFD4AF37).copy(alpha = 0.3f))

                                publicFiles.forEach { file ->
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file.title, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(file.fileName, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            }

                                            Surface(
                                                color = Color(0xFFD4AF37).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.clickable { copyToClipboard(file.publicPin, "Kiosk PIN") }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                                    Text(file.publicPin, color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Code and Public Share Dialog
    selectedFileForQr?.let { file ->
        Dialog(
            onDismissRequest = { selectedFileForQr = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌐 पब्लिक क्लाउड फ़ाइल शेयर",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { selectedFileForQr = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // QR Graphic Presentation
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(140.dp)
                            )
                            Text(
                                text = "SCAN AT ANY KIOSK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // Kiosk PIN Highlight
                    Surface(
                        color = Color(0xFFD4AF37).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Express Kiosk 6-Digit PIN", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                                Text(file.publicPin, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F), fontFamily = FontFamily.Monospace)
                            }
                            OutlinedButton(
                                onClick = { copyToClipboard(file.publicPin, "Kiosk PIN") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy PIN", fontSize = 12.sp)
                            }
                        }
                    }

                    // Public Link Row
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Public Live Link", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(
                                    file.publicUrl,
                                    fontSize = 12.sp,
                                    color = Color(0xFF38BDF8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { copyToClipboard(file.publicUrl, "Public Link") }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = Color(0xFF38BDF8))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.recordFilePrint(file.id)
                                Toast.makeText(context, "फ़ाइल प्रिंट कतार में भेजी गई!", Toast.LENGTH_SHORT).show()
                                selectedFileForQr = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print Now", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                copyToClipboard("Master Printer File: ${file.title}\nLink: ${file.publicUrl}\nPIN: ${file.publicPin}", "Share Text")
                                Toast.makeText(context, "WhatsApp/Web शेयर संदेश कॉपी किया गया", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share File", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicFileItemCard(
    file: CloudPublicFile,
    onShowQr: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyPin: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (file.fileType) {
                                "PDF" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                "BLUEPRINT" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                "VISITING_CARD" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                else -> Color(0xFF10B981).copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = file.fileType.take(3),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = when (file.fileType) {
                            "PDF" -> Color(0xFFDC2626)
                            "BLUEPRINT" -> Color(0xFF2563EB)
                            "VISITING_CARD" -> Color(0xFFD97706)
                            else -> Color(0xFF059669)
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = file.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("•", color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${file.pageCount} Pages (${file.fileSizeKb / 1024.0} MB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Public / Private badge
                Surface(
                    color = if (file.isPublic) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (file.isPublic) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (file.isPublic) Color(0xFF22C55E) else Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (file.isPublic) "PUBLIC" else "PRIVATE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (file.isPublic) Color(0xFF22C55E) else Color(0xFF64748B)
                        )
                    }
                }
            }

            if (file.description.isNotBlank()) {
                Text(
                    text = file.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Express PIN and link info row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Kiosk PIN:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            file.publicPin,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onCopyPin() }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Prints: ${file.printCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", color = MaterialTheme.colorScheme.outline)
                        Text("Downloads: ${file.downloadCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onShowQr,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("QR Code", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onCopyLink,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Link", fontSize = 12.sp)
                }

                Button(
                    onClick = onPrint,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
