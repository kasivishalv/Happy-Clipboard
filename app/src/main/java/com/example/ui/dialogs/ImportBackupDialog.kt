package com.example.ui.dialogs

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.HappyTeal300
import com.example.ui.theme.HappyTeal600
import com.example.ui.theme.HappyTeal900
import org.json.JSONObject

@Composable
fun ImportBackupDialog(
    onDismiss: () -> Unit,
    onImport: (content: String, password: String?, mode: String) -> Unit
) {
    val context = LocalContext.current
    var backupContent by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var importMode by remember { mutableStateOf("MERGE") } // MERGE or REPLACE

    // Detect payload characteristics
    val isEncryptedHcb = remember(backupContent) {
        val trimmed = backupContent.trim()
        trimmed.startsWith("HCB_V1:") || trimmed.startsWith("HCB_V2:") ||
                (trimmed.contains("\"isEncrypted\":true") || trimmed.contains("\"format\":\"hcb\""))
    }

    // Try parsing basic metadata if JSON
    val metadata = remember(backupContent) {
        try {
            val json = JSONObject(backupContent.trim())
            val itemsCount = json.optJSONArray("items")?.length() ?: 0
            val collectionsCount = json.optJSONArray("collections")?.length() ?: 0
            val isEnc = json.optBoolean("isEncrypted", false)
            Triple(itemsCount, collectionsCount, isEnc)
        } catch (_: Exception) {
            null
        }
    }

    val displayFileName = if (isEncryptedHcb) "happy_clipboard_backup.hcb" else "clipboard_export.json"
    val displayBackupType = if (isEncryptedHcb) "Encrypted Archive (.hcb)" else "Standard JSON Backup"
    val displayItemCount = metadata?.first ?: (if (backupContent.isNotBlank()) "Auto-detect on import" else "--")
    val displayCollectionsCount = metadata?.second ?: (if (backupContent.isNotBlank()) "Included" else "--")
    val displayImages = "Supported"
    val isEncrypted = isEncryptedHcb || (metadata?.third == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Import Backup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HappyTeal900
                )
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = cm.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString()
                            if (!text.isNullOrBlank()) {
                                backupContent = text
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Backup Details Card
                val detailsShape = RoundedCornerShape(14.dp)
                Surface(
                    shape = detailsShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(detailsShape)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailRow(label = "File Name", value = displayFileName)
                        DetailRow(label = "Backup Type", value = displayBackupType)
                        DetailRow(label = "Items", value = "$displayItemCount")
                        DetailRow(label = "Collections", value = "$displayCollectionsCount")
                        DetailRow(label = "Images", value = displayImages)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Encryption Status",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isEncrypted) HappyOrange.copy(alpha = 0.15f) else HappyTeal300.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isEncrypted) HappyOrange else HappyTeal900,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isEncrypted) "AES-256 Encrypted (.hcb)" else "Plaintext",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEncrypted) Color(0xFF8A4600) else HappyTeal900
                                    )
                                }
                            }
                        }
                    }
                }

                // Payload Input
                OutlinedTextField(
                    value = backupContent,
                    onValueChange = { backupContent = it },
                    label = { Text("Backup Payload (.hcb or JSON)") },
                    placeholder = { Text("Paste payload string...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Notice & Input for .hcb files
                if (isEncryptedHcb || isEncrypted) {
                    val noticeShape = RoundedCornerShape(12.dp)
                    Surface(
                        shape = noticeShape,
                        color = HappyOrange.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HappyOrange.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(noticeShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = HappyOrange, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Password required for encrypted .hcb file",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8A4600)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Decryption Password") },
                        placeholder = { Text("Enter the password used during export") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Import Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = importMode == "MERGE",
                        onClick = { importMode = "MERGE" }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Merge", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Keep existing items and add new ones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = importMode == "REPLACE",
                        onClick = { importMode = "REPLACE" }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Replace All", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Overwrite current history with backup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (backupContent.isNotBlank()) {
                        val pass = password.takeIf { it.isNotBlank() }
                        onImport(backupContent.trim(), pass, importMode)
                    }
                },
                enabled = backupContent.isNotBlank() && (!isEncrypted || password.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = HappyTeal900),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
