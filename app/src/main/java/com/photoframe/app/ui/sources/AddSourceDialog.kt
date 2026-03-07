package com.photoframe.app.ui.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.photoframe.core.model.PhotoSourceType

/**
 * Dialog for adding a new photo source.
 *
 * Features:
 * - Choose source type (SMB or Local)
 * - Type-specific configuration forms
 * - Validation
 * - Local folder picker integration
 *
 * Material 3 design with tabs for source types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceDialog(
    selectedType: PhotoSourceType?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAddSmb: (
        displayName: String,
        server: String,
        share: String,
        path: String,
        domain: String,
        username: String,
        password: String
    ) -> Unit,
    onAddLocal: (
        displayName: String,
        folderUris: List<Uri>
    ) -> Unit
) {
    var sourceType by remember { mutableStateOf(selectedType ?: PhotoSourceType.SMB) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Photo Source")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Source type selector
                if (selectedType == null) {
                    SourceTypeSelector(
                        selectedType = sourceType,
                        onTypeSelected = { sourceType = it }
                    )
                }

                Divider()

                // Type-specific form
                when (sourceType) {
                    PhotoSourceType.SMB -> {
                        SmbSourceForm(
                            isSaving = isSaving,
                            onAdd = onAddSmb,
                            onCancel = onDismiss
                        )
                    }
                    PhotoSourceType.LOCAL -> {
                        LocalSourceForm(
                            isSaving = isSaving,
                            onAdd = onAddLocal,
                            onCancel = onDismiss
                        )
                    }
                }
            }
        },
        confirmButton = {}, // Handled by forms
        dismissButton = {} // Handled by forms
    )
}

/**
 * Selector for choosing source type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceTypeSelector(
    selectedType: PhotoSourceType,
    onTypeSelected: (PhotoSourceType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Source Type",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == PhotoSourceType.SMB,
                onClick = { onTypeSelected(PhotoSourceType.SMB) },
                label = { Text("Network (SMB)") },
                leadingIcon = {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = selectedType == PhotoSourceType.LOCAL,
                onClick = { onTypeSelected(PhotoSourceType.LOCAL) },
                label = { Text("Local Storage") },
                leadingIcon = {
                    Icon(Icons.Default.Smartphone, contentDescription = null)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Form for adding SMB source.
 */
@Composable
private fun SmbSourceForm(
    isSaving: Boolean,
    onAdd: (String, String, String, String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("/") }
    var domain by remember { mutableStateOf("WORKGROUP") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Display name
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name (Optional)") },
            placeholder = { Text("e.g., Home NAS") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Server
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Server *") },
            placeholder = { Text("e.g., 192.168.1.100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Share
        OutlinedTextField(
            value = share,
            onValueChange = { share = it },
            label = { Text("Share *") },
            placeholder = { Text("e.g., photos") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Path
        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text("Path") },
            placeholder = { Text("/") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Domain
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = { Text("Domain") },
            placeholder = { Text("WORKGROUP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password *") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            singleLine = true
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    onAdd(
                        displayName,
                        server,
                        share,
                        path,
                        domain,
                        username,
                        password
                    )
                },
                enabled = !isSaving && server.isNotBlank() && share.isNotBlank() && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Add Source")
            }
        }
    }
}

/**
 * Form for adding local storage source.
 */
@Composable
private fun LocalSourceForm(
    isSaving: Boolean,
    onAdd: (String, List<Uri>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf("") }
    var selectedFolders by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Folder picker launcher
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { folderUri ->
            // Take persistable URI permission
            context.contentResolver.takePersistableUriPermission(
                folderUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // Add to selected folders
            selectedFolders = selectedFolders + folderUri
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Display name
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name (Optional)") },
            placeholder = { Text("e.g., Camera Photos") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Selected folders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selected Folders",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (selectedFolders.isEmpty()) {
                    Text(
                        text = "No folders selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    selectedFolders.forEach { uri ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uri.path?.substringAfterLast('/') ?: uri.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    selectedFolders = selectedFolders.filter { it != uri }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove folder",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Add folder button
                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select Folder")
                }
            }
        }

        // Info text
        Text(
            text = "Select one or more folders containing photos. The app will scan these folders for images.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    onAdd(displayName, selectedFolders)
                },
                enabled = !isSaving && selectedFolders.isNotEmpty()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Add Source")
            }
        }
    }
}
