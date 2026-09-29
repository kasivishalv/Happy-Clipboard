package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppLockOverlay(
    onUnlock: (String) -> Boolean,
    modifier: Modifier = Modifier,
    isPinSet: Boolean = true,
    onSetPin: ((String, String) -> Boolean)? = null
) {
    var pin by remember { mutableStateOf("") }
    var setupPin by remember { mutableStateOf<String?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val subtitleText = when {
        !isPinSet && !isConfirming -> "Create a 4-digit security PIN"
        !isPinSet && isConfirming -> "Confirm your 4-digit PIN"
        else -> "Enter your 4-digit security PIN"
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_lock_overlay"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Locked",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Happy Clipboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("pin_subtitle")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Dots Row
            Row(
                modifier = Modifier.testTag("pin_dots_row"),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("pin_error_message")
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Numeric Keypad
            val digits = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in digits) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (digit in row) {
                            if (digit.isEmpty()) {
                                Spacer(modifier = Modifier.size(68.dp))
                            } else if (digit == "DEL") {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .testTag("keypad_del")
                                        .clickable {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                                errorMessage = null
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .testTag("keypad_$digit")
                                        .clickable {
                                            if (pin.length < 4) {
                                                val newPin = pin + digit
                                                pin = newPin
                                                errorMessage = null
                                                if (newPin.length == 4) {
                                                    if (!isPinSet) {
                                                        if (!isConfirming) {
                                                            setupPin = newPin
                                                            pin = ""
                                                            isConfirming = true
                                                            errorMessage = null
                                                        } else {
                                                            if (newPin == setupPin) {
                                                                val success = onSetPin?.invoke(setupPin!!, newPin)
                                                                    ?: onUnlock(newPin)
                                                                if (!success) {
                                                                    errorMessage = "Failed to set PIN. Try again."
                                                                    setupPin = null
                                                                    isConfirming = false
                                                                    pin = ""
                                                                }
                                                            } else {
                                                                errorMessage = "PINs do not match. Try again."
                                                                setupPin = null
                                                                isConfirming = false
                                                                pin = ""
                                                            }
                                                        }
                                                    } else {
                                                        val success = onUnlock(newPin)
                                                        if (!success) {
                                                            errorMessage = "Incorrect PIN"
                                                            pin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = digit,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
