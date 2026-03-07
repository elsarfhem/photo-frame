package com.photoframe.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable error content component with actionable guidance.
 *
 * Provides clear error messages with suggested actions and retry button.
 * Improves UX by giving users clear steps to resolve errors.
 *
 * Phase 6: Polish & Bug Fixes
 *
 * @param error Error message or exception message
 * @param errorType Type of error for appropriate icon and guidance
 * @param onRetry Callback when user taps retry button
 * @param onActionClick Optional callback for additional action (e.g., "Open Settings")
 * @param actionLabel Label for additional action button
 */
@Composable
fun EnhancedErrorContent(
    error: String,
    errorType: ErrorType = ErrorType.GENERIC,
    onRetry: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = errorType.icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = errorType.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorType.guidance,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Retry button
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }

        // Optional action button
        if (onActionClick != null && actionLabel != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Error type with associated icon, title, and guidance.
 */
enum class ErrorType(
    val icon: ImageVector,
    val title: String,
    val guidance: String
) {
    NETWORK(
        icon = Icons.Default.Error,
        title = "Connection Error",
        guidance = "Check your network connection and ensure the SMB server is reachable."
    ),
    SMB_AUTH(
        icon = Icons.Default.Error,
        title = "Authentication Failed",
        guidance = "Verify your SMB credentials in settings. Username and password must be correct."
    ),
    SMB_NOT_FOUND(
        icon = Icons.Default.Error,
        title = "Share Not Found",
        guidance = "The SMB share path does not exist. Check the server and share name in settings."
    ),
    NO_PHOTOS(
        icon = Icons.Default.Error,
        title = "No Photos Found",
        guidance = "No compatible photos (JPEG, PNG, HEIC) found in the configured share path."
    ),
    PERMISSION_DENIED(
        icon = Icons.Default.Error,
        title = "Permission Denied",
        guidance = "You don't have permission to access this share. Check your SMB credentials."
    ),
    GENERIC(
        icon = Icons.Default.Error,
        title = "Something Went Wrong",
        guidance = "An unexpected error occurred. Try again or check your settings."
    );

    companion object {
        /**
         * Maps error message to appropriate error type.
         */
        fun fromMessage(message: String): ErrorType {
            return when {
                message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) -> NETWORK

                message.contains("authentication", ignoreCase = true) ||
                message.contains("credentials", ignoreCase = true) ||
                message.contains("401", ignoreCase = true) -> SMB_AUTH

                message.contains("not found", ignoreCase = true) ||
                message.contains("404", ignoreCase = true) ||
                message.contains("does not exist", ignoreCase = true) -> SMB_NOT_FOUND

                message.contains("no photos", ignoreCase = true) ||
                message.contains("empty", ignoreCase = true) -> NO_PHOTOS

                message.contains("permission", ignoreCase = true) ||
                message.contains("denied", ignoreCase = true) ||
                message.contains("403", ignoreCase = true) -> PERMISSION_DENIED

                else -> GENERIC
            }
        }
    }
}
