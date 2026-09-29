package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HappyOrange
import com.example.ui.theme.HappySecurityRestrained
import com.example.ui.theme.HappyTeal900

@Composable
fun ExportBackupDialog(
    itemCount: Int,
    onDismiss: () -> Unit,
    onExport: (password: String?) -> String
) {
    val context = LocalContext.current
    var isEncrypted by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }
    var generatedBackup by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = HappyTeal900
                )
                Text("Export Backup", fontWeight = FontWeight.Bold, color = HappyTeal900)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Exporting $itemCount items from local database",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (generatedBackup == null) {
                    // Option 1: Recommended Encrypted HCB (Default)
                    val option1Shape = RoundedCornerShape(14.dp)
                    Surface(
                        shape = option1Shape,
                        color = if (isEncrypted) HappyTeal900.copy(alpha = 0.08f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isEncrypted) HappyTeal900 else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(option1Shape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isEncrypted,
                                onClick = { isEncrypted = true }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Recommended", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = HappyTeal900)
                                }
                                Text("Encrypted Archive (.hcb)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("AES-256-GCM password protected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 2: Advanced Unencrypted JSON with Warning
                    val option2Shape = RoundedCornerShape(14.dp)
                    Surface(
                        shape = option2Shape,
                        color = if (!isEncrypted) HappyOrange.copy(alpha = 0.08f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (!isEncrypted) HappyOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(option2Shape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isEncrypted,
                                onClick = { isEncrypted = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Advanced", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = HappyOrange)
                                Text("Unencrypted JSON (.json)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Plaintext unencrypted JSON file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (isEncrypted) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Backup Password (Separate from PIN)") },
                            placeholder = { Text("Min 6 characters recommended") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Note: Do not use your 4-digit App PIN. Use a strong password for file exports.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Strong security warning for unencrypted backup
                        val warningShape = RoundedCornerShape(12.dp)
                        Surface(
                            shape = warningShape,
                            color = HappySecurityRestrained.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HappySecurityRestrained.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(warningShape)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = HappySecurityRestrained,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Warning: Unencrypted backup",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HappySecurityRestrained
                                    )
                                    Text(
                                        text = "This file will contain your clipboard history in plain, readable text. Any sensitive clips or passwords will NOT be protected.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val readyShape = RoundedCornerShape(14.dp)
                    Surface(
                        shape = readyShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HappyTeal900.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(readyShape)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Backup ready for export!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = HappyTeal900
                            )
                            Text(
                                text = "Size: ${generatedBackup?.length ?: 0} bytes • Format: ${if (isEncrypted) ".hcb (Encrypted)" else ".json (Unencrypted)"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (generatedBackup == null) {
                Button(
                    onClick = {
                        val pass = if (isEncrypted) password.takeIf { it.isNotBlank() } else null
                        generatedBackup = onExport(pass)
                    },
                    enabled = !isEncrypted || password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HappyTeal900),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isEncrypted) "Generate Encrypted Backup" else "Export Unencrypted JSON")
                }
            } else {
                // Privacy fix: Removed "Copy to clipboard" to avoid pasting entire sensitive history onto system clipboard.
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, generatedBackup)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Save or Share Happy Clipboard Backup"))
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HappyTeal900),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save / Share Backup File")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
