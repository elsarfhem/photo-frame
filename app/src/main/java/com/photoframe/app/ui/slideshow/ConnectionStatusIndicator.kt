package com.photoframe.app.ui.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Connection status indicator for slideshow screen.
 *
 * Shows a colored dot in the corner indicating connection state:
 * - Green: Connected
 * - Yellow: Connecting
 * - Red: Disconnected
 *
 * Tap to show connection details dialog.
 *
 * Phase 5: Settings & Scheduling
 *
 * @param isConnected True if connected to SMB
 * @param isConnecting True if currently connecting
 * @param error Connection error message (null if no error)
 * @param modifier Modifier for positioning
 */
@Composable
fun ConnectionStatusIndicator(
    isConnected: Boolean,
    isConnecting: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val statusColor = when {
        isConnecting -> Color(0xFFFFA500) // Orange/Yellow
        isConnected -> Color(0xFF4CAF50) // Green
        else -> Color(0xFFF44336) // Red
    }

    val statusText = when {
        isConnecting -> "Connecting..."
        isConnected -> "Connected"
        else -> "Disconnected"
    }

    // Status indicator dot
    Box(
        modifier = modifier
            .padding(16.dp)
            .size(16.dp)
            .background(statusColor, CircleShape)
            .clickable { showDialog = true }
    )

    // Connection details dialog
    if (showDialog) {
        ConnectionDetailsDialog(
            isConnected = isConnected,
            isConnecting = isConnecting,
            error = error,
            statusColor = statusColor,
            statusText = statusText,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Dialog showing connection details.
 */
@Composable
private fun ConnectionDetailsDialog(
    isConnected: Boolean,
    isConnecting: Boolean,
    error: String?,
    statusColor: Color,
    statusText: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.titleLarge
                )

                Divider()

                // Status indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Error message (if any)
                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Additional info
                if (isConnected) {
                    Text(
                        text = "SMB connection is active. Photos are loading from the network share.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isConnecting) {
                    Text(
                        text = "Attempting to connect to SMB share...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Not connected to SMB share. Check your network connection and settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
