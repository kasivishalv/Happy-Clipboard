package com.example.ui.screens

import com.example.core.security.SecurityPreferences
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ClipboardUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.HappyOrange
import com.example.ui.theme.HappySecurityRestrained
import com.example.ui.theme.HappyTeal300
import com.example.ui.theme.HappyTeal600
import com.example.ui.theme.HappyTeal900

@Composable
fun SettingsScreen(
    uiState: ClipboardUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = viewModel.preferences

    var duplicateMode by remember { mutableStateOf(prefs.duplicateHandling) }
    var maxHistoryCapacity by remember { mutableStateOf(prefs.maxHistoryCapacity) }
    var retentionDays by remember { mutableIntStateOf(prefs.retentionDays) }
    var otpMinutes by remember { mutableIntStateOf(prefs.otpAutoDeleteMinutes) }
    var isAppLockEnabled by remember { mutableStateOf(prefs.isAppLockEnabled) }
    var isBiometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled) }
    var isScreenSecurityEnabled by remember { mutableStateOf(prefs.isSecureScreensEnabled) }
    var sensitiveHandlingMode by remember { mutableStateOf(prefs.sensitiveHandling) } // SAVE_AND_MASK, ASK_OR_MASK, DONT_SAVE
    var excludedPackages by remember { mutableStateOf(prefs.excludedPackages) }
    var themeMode by remember { mutableStateOf(prefs.themeMode) }

    var showAddPackageDialog by remember { mutableStateOf(false) }
    var newExcludedPackageInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var currentPinText by remember { mutableStateOf("") }
    var newPinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf<String?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Customize capture, security, privacy & backups",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ==========================================
        // 1. Clipboard
        // ==========================================
        SettingsCard(title = "Clipboard", icon = Icons.Default.ContentPaste) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-save Copied Content", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Happy Clipboard attempts automatic clipboard capture using supported Android mechanisms. Capture behavior can be affected by Android background restrictions, device power management, user restrictions, and OS-version-specific clipboard privacy behavior.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.isMonitoringActive,
                    onCheckedChange = { viewModel.toggleMonitoring() },
                    modifier = Modifier.testTag("auto_save_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text("Duplicate Handling", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        duplicateMode = "MERGE"
                        viewModel.updatePreferences(duplicateHandling = "MERGE")
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = duplicateMode == "MERGE",
                    onClick = {
                        duplicateMode = "MERGE"
                        viewModel.updatePreferences(duplicateHandling = "MERGE")
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Merge duplicates (Recommended)", style = MaterialTheme.typography.bodyMedium)
                    Text("Increments copy count and moves item to top", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        duplicateMode = "KEEP_ALL"
                        viewModel.updatePreferences(duplicateHandling = "KEEP_ALL")
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = duplicateMode == "KEEP_ALL",
                    onClick = {
                        duplicateMode = "KEEP_ALL"
                        viewModel.updatePreferences(duplicateHandling = "KEEP_ALL")
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Keep all copies", style = MaterialTheme.typography.bodyMedium)
                    Text("Creates a new timeline entry for every copy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Purge System Clipboard", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Instantly empties the Android OS clipboard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = { viewModel.purgeSystemClipboard(context) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Clear OS Clip", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ==========================================
        // 2. Security
        // ==========================================
        SettingsCard(title = "Security", icon = Icons.Default.Security) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("App Lock (PIN Protection)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Locks app with a 4-digit security passcode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !prefs.isPinSet) {
                            showPinDialog = true
                        } else {
                            isAppLockEnabled = enabled
                            viewModel.updatePreferences(appLock = enabled)
                        }
                    }
                )
            }

            if (isAppLockEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showPinDialog = true }) {
                        Text("Change Security PIN", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Biometric Authentication", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Unlock with Fingerprint or Face Recognition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { enabled ->
                        isBiometricEnabled = enabled
                        viewModel.updatePreferences(biometric = enabled)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Prevent Screen Leak (FLAG_SECURE)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Hides preview in Recent Apps & prevents screenshots", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isScreenSecurityEnabled,
                    onCheckedChange = { enabled ->
                        isScreenSecurityEnabled = enabled
                        viewModel.updatePreferences(screenSecurity = enabled)
                    }
                )
            }
        }

        // ==========================================
        // 3. Backup & Import
        // ==========================================
        SettingsCard(title = "Backup & Import", icon = Icons.Default.Storage) {
            Text("Keep an encrypted archive of your clipboard history and collections safe.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.setShowExportDialog(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Create Backup", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.setShowImportDialog(true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Import Backup", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==========================================
        // 4. Cloud Sync & Privacy Model
        // ==========================================
        SettingsCard(title = "Cloud Sync", icon = Icons.Default.CloudDone) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Offline-First & Privacy-Centric", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Happy Clipboard works completely offline. No cloud account or Internet connection is required for clipboard capture, history, search, collections, or local backups. Optional encrypted cloud backup/synchronization is on our roadmap and disabled by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ==========================================
        // 5. Appearance
        // ==========================================
        SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Select application appearance (Default: System)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val themeOptions = listOf(
                    "SYSTEM" to "System",
                    "LIGHT" to "Light",
                    "DARK" to "Dark"
                )
                for ((mode, label) in themeOptions) {
                    val isSelected = themeMode.equals(mode, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                themeMode = mode
                                viewModel.setThemeMode(mode)
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Active identity: Happy Clipboard (Dark Teal #16697A, Secondary Teal #489FB5, Light Teal #82C0CC, Warm Off-White #EDE7E3, Accent Orange #FFA62B, Destructive #D95D39)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val swatches = listOf(
                    Color(0xFF16697A),
                    Color(0xFF489FB5),
                    Color(0xFF82C0CC),
                    Color(0xFFEDE7E3),
                    Color(0xFFFFA62B),
                    Color(0xFFD95D39)
                )
                for (swatch in swatches) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(swatch)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        // ==========================================
        // 6. Notifications
        // ==========================================
        SettingsCard(title = "Notifications", icon = Icons.Default.Notifications) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Capture Feedback Toast", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Confirms when a new clip is captured in background", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = true,
                    onCheckedChange = { }
                )
            }
        }

        // ==========================================
        // 7. Privacy
        // ==========================================
        SettingsCard(title = "Privacy", icon = Icons.Default.Visibility) {
            // Sensitive Content Policy (3-tier safety layer)
            Text("Sensitive Content Policy", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Controls capture behavior when passwords, API keys, or OTPs are detected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Option 1: Save and mask
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sensitiveHandlingMode = "SAVE_AND_MASK"
                            viewModel.updatePreferences(sensitiveHandling = "SAVE_AND_MASK")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sensitiveHandlingMode == "SAVE_AND_MASK",
                        onClick = {
                            sensitiveHandlingMode = "SAVE_AND_MASK"
                            viewModel.updatePreferences(sensitiveHandling = "SAVE_AND_MASK")
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Save & Mask (Recommended)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Saves clip securely; requires manual tap to reveal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Option 2: Ask before saving
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sensitiveHandlingMode = "ASK"
                            viewModel.updatePreferences(sensitiveHandling = "ASK")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sensitiveHandlingMode == "ASK",
                        onClick = {
                            sensitiveHandlingMode = "ASK"
                            viewModel.updatePreferences(sensitiveHandling = "ASK")
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Ask / Confirm", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Prompts before retaining sensitive secrets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Option 3: Do not save
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sensitiveHandlingMode = "DONT_SAVE"
                            viewModel.updatePreferences(sensitiveHandling = "DONT_SAVE")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sensitiveHandlingMode == "DONT_SAVE",
                        onClick = {
                            sensitiveHandlingMode = "DONT_SAVE"
                            viewModel.updatePreferences(sensitiveHandling = "DONT_SAVE")
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Don't Save", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Discards passwords, OTPs, and API keys automatically", style = MaterialTheme.typography.bodySmall, color = HappySecurityRestrained)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // OTP Auto-Delete Timer
            Text("OTP / 2FA Code Auto-Delete", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 5, 10, 30).forEach { mins ->
                    val isSelected = otpMinutes == mins
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) HappyOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) HappyOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                otpMinutes = mins
                                viewModel.updatePreferences(otpMinutes = mins)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = if (mins == 0) "Off" else "${mins}m",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF8A4600) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Excluded Applications
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Excluded Applications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Clipboard updates from these packages are ignored", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = {
                        newExcludedPackageInput = ""
                        showAddPackageDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Package", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (excludedPackages.isEmpty()) {
                Text(
                    text = "No apps excluded. Add banking or password manager packages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (pkg in excludedPackages.toList()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = pkg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        val updated = excludedPackages - pkg
                                        excludedPackages = updated
                                        viewModel.updatePreferences(excludedPackages = updated)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = HappySecurityRestrained,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. Advanced
        // ==========================================
        SettingsCard(title = "Advanced", icon = Icons.Default.Tune) {
            // Max Items
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Max History Clips",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (maxHistoryCapacity == SecurityPreferences.CAPACITY_UNLIMITED) "Unlimited" else "$maxHistoryCapacity items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val row1 = listOf("200", "500", "1000", "5000")
                val row2 = listOf("10000", "25000", "50000", SecurityPreferences.CAPACITY_UNLIMITED)

                listOf(row1, row2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { count ->
                            val isSelected = maxHistoryCapacity == count
                            val displayText = if (count == SecurityPreferences.CAPACITY_UNLIMITED) "Unlimited" else count
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("max_history_${count.lowercase()}")
                                    .clickable {
                                        maxHistoryCapacity = count
                                        viewModel.updatePreferences(maxHistoryCapacity = count)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Retention Days
            Text("Retention Period (Auto-cleanup)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7, 30, 90, 0).forEach { days ->
                    val isSelected = retentionDays == days
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                retentionDays = days
                                viewModel.updatePreferences(retentionDays = days)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = if (days == 0) "Forever" else "${days}d",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Clear Non-Favorite Clips
            OutlinedButton(
                onClick = { showClearConfirmDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HappySecurityRestrained),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear History (Keep Favorites)")
            }
        }

        // ==========================================
        // 9. Diagnostics & Developer Verification
        // ==========================================
        SettingsCard(title = "Clipboard Diagnostics", icon = Icons.Default.Tune) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Capture & Monitoring", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Auto Capture:", style = MaterialTheme.typography.bodySmall)
                            Text(if (prefs.isAutoCaptureEnabled) "ON" else "OFF", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monitor Status:", style = MaterialTheme.typography.bodySmall)
                            Text("ACTIVE", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service:", style = MaterialTheme.typography.bodySmall)
                            Text("FOREGROUND", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sensitive Policy:", style = MaterialTheme.typography.bodySmall)
                            Text(prefs.sensitiveHandling, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Excluded Packages:", style = MaterialTheme.typography.bodySmall)
                            Text("${prefs.excludedPackages.size} registered", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Local Storage & Security", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Database Engine:", style = MaterialTheme.typography.bodySmall)
                            Text("Room SQLite (Sandbox Protected)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Max Capacity Limit:", style = MaterialTheme.typography.bodySmall)
                            Text(if (prefs.isCapacityUnlimited) "Unlimited" else "${prefs.maxHistoryCapacity} items", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("HCB Container:", style = MaterialTheme.typography.bodySmall)
                            Text("HCB_V2 (PBKDF2 600k / AES-256-GCM)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Screen Security:", style = MaterialTheme.typography.bodySmall)
                            Text(if (prefs.isSecureScreensEnabled) "FLAG_SECURE ON" else "OFF", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Battery Optimization:", style = MaterialTheme.typography.bodySmall)
                            Text("Adaptive OS Managed", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                testFeedback?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val testClip = "Diagnostic Test Clip #${System.currentTimeMillis() % 10000}"
                        viewModel.addNewClip(testClip, "Happy Clipboard Diagnostics")
                        testFeedback = "Test clip injected successfully into local repository!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Clipboard Diagnostic Test")
                }
            }
        }

        // ==========================================
        // 10. About & Security Architecture
        // ==========================================
        SettingsCard(title = "About", icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Happy Clipboard v1.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "A modern, offline-first clipboard manager for Android. Local Room SQLite persistence in app sandbox (MODE_PRIVATE) with PIN app-lock, biometric authentication, FLAG_SECURE screen leak protection, and HCB_V2 AES-256-GCM encrypted backups (PBKDF2-HMAC-SHA256 with 600,000 iterations).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }

    // Set / Change PIN Dialog
    if (showPinDialog) {
        val isChanging = prefs.isPinSet
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                currentPinText = ""
                newPinText = ""
                confirmPinText = ""
                pinDialogError = null
            },
            shape = RoundedCornerShape(24.dp),
            title = { Text(if (isChanging) "Change 4-Digit Security PIN" else "Set 4-Digit Security PIN") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isChanging) {
                        OutlinedTextField(
                            value = currentPinText,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    currentPinText = it
                                    pinDialogError = null
                                }
                            },
                            label = { Text("Current PIN (4 digits)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("current_pin_input")
                        )
                    }

                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                newPinText = it
                                pinDialogError = null
                            }
                        },
                        label = { Text(if (isChanging) "New PIN (4 digits)" else "PIN (4 digits)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmPinText,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                confirmPinText = it
                                pinDialogError = null
                            }
                        },
                        label = { Text("Confirm PIN (4 digits)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_pin_input")
                    )

                    if (pinDialogError != null) {
                        Text(
                            text = pinDialogError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("pin_dialog_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isChanging) {
                            if (!prefs.verifyPin(currentPinText)) {
                                pinDialogError = "Incorrect current PIN"
                                return@Button
                            }
                            if (newPinText.length != 4) {
                                pinDialogError = "New PIN must be 4 digits"
                                return@Button
                            }
                            if (newPinText != confirmPinText) {
                                pinDialogError = "PINs do not match"
                                return@Button
                            }
                            val success = viewModel.changePin(currentPinText, newPinText, confirmPinText)
                            if (success) {
                                showPinDialog = false
                                currentPinText = ""
                                newPinText = ""
                                confirmPinText = ""
                                pinDialogError = null
                            } else {
                                pinDialogError = "Failed to update PIN"
                            }
                        } else {
                            if (newPinText.length != 4) {
                                pinDialogError = "PIN must be 4 digits"
                                return@Button
                            }
                            if (newPinText != confirmPinText) {
                                pinDialogError = "PINs do not match"
                                return@Button
                            }
                            val success = viewModel.setAppPin(newPinText, confirmPinText)
                            if (success) {
                                isAppLockEnabled = true
                                showPinDialog = false
                                currentPinText = ""
                                newPinText = ""
                                confirmPinText = ""
                                pinDialogError = null
                            } else {
                                pinDialogError = "Failed to set PIN"
                            }
                        }
                    },
                    enabled = (!isChanging || currentPinText.length == 4) &&
                            newPinText.length == 4 && confirmPinText.length == 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    currentPinText = ""
                    newPinText = ""
                    confirmPinText = ""
                    pinDialogError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Clear Clipboard History?") },
            text = { Text("All non-favorite items will be permanently deleted. Pinned favorites and collections will remain safe.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory(keepFavorites = true)
                        showClearConfirmDialog = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HappySecurityRestrained)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Package Exclusion Dialog
    if (showAddPackageDialog) {
        AlertDialog(
            onDismissRequest = { showAddPackageDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Exclude Package") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter application package name to ignore:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newExcludedPackageInput,
                        onValueChange = { newExcludedPackageInput = it.trim() },
                        placeholder = { Text("com.example.bank or keepass") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newExcludedPackageInput.isNotBlank()) {
                            val updated = excludedPackages + newExcludedPackageInput
                            excludedPackages = updated
                            viewModel.updatePreferences(excludedPackages = updated)
                            showAddPackageDialog = false
                            newExcludedPackageInput = ""
                        }
                    },
                    enabled = newExcludedPackageInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Add Exclusion")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPackageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            content()
        }
    }
}
