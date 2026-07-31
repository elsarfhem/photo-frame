package com.photoframe.app.ui.sources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.PhotoSourceType

/**
 * Screen for managing photo sources.
 *
 * Features:
 * - List of configured sources
 * - Add new source (SMB or local)
 * - Remove source
 * - Enable/disable source
 * - Validate source
 *
 * Material 3 design with cards for each source.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onNavigateBack: () -> Unit,
    viewModel: SourcesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success/error messages
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Sources") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Source") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.sources.isEmpty() -> {
                    // Initial loading
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.sources.isEmpty() -> {
                    // Empty state
                    EmptySourcesState(
                        onAddSource = { viewModel.showAddDialog() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // List of sources
                    SourcesList(
                        sources = state.sources,
                        isValidating = state.isValidating,
                        onToggleEnabled = viewModel::toggleSourceEnabled,
                        onEdit = viewModel::showEditDialog,
                        onRemove = viewModel::showRemoveConfirmation,
                        onValidate = viewModel::validateSource
                    )
                }
            }
        }
    }

    // Add source dialog
    if (state.showAddDialog) {
        AddSourceDialog(
            selectedType = state.selectedSourceType,
            isSaving = state.isSaving,
            onDismiss = viewModel::hideAddDialog,
            onAddSmb = viewModel::addSmbSource,
            onAddLocal = viewModel::addLocalSource
        )
    }

    // Remove confirmation dialog
    if (state.showRemoveConfirmation && state.sourceIdToRemove != null) {
        RemoveSourceConfirmationDialog(
            onConfirm = {
                viewModel.removeSource(state.sourceIdToRemove!!)
                viewModel.hideRemoveConfirmation()
            },
            onDismiss = viewModel::hideRemoveConfirmation
        )
    }

    // Edit source dialog
    if (state.showEditDialog && state.sourceToEdit != null) {
        EditSourceDialog(
            source = state.sourceToEdit!!,
            isSaving = state.isSaving,
            onDismiss = viewModel::hideEditDialog,
            onEditSmb = viewModel::editSmbSource,
            onEditLocal = viewModel::editLocalSource
        )
    }
}

/**
 * Empty state shown when no sources configured.
 */
@Composable
private fun EmptySourcesState(
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "No photo sources configured",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Add a photo source to start your slideshow",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onAddSource,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Source")
        }
    }
}

/**
 * List of configured sources.
 */
@Composable
private fun SourcesList(
    sources: List<PhotoSourceConfig>,
    isValidating: Boolean,
    onToggleEnabled: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onRemove: (String) -> Unit,
    onValidate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sources, key = { it.id }) { source ->
            SourceCard(
                source = source,
                isValidating = isValidating,
                onToggleEnabled = { enabled -> onToggleEnabled(source.id, enabled) },
                onEdit = { onEdit(source.id) },
                onRemove = { onRemove(source.id) },
                onValidate = { onValidate(source.id) }
            )
        }
    }
}

/**
 * Card displaying a single photo source.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceCard(
    source: PhotoSourceConfig,
    isValidating: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onValidate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (source.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Source type icon
                    Icon(
                        imageVector = when (source.type) {
                            PhotoSourceType.SMB -> Icons.Default.Cloud
                            PhotoSourceType.LOCAL -> Icons.Default.Smartphone
                            PhotoSourceType.SAMPLE -> Icons.Default.PhotoLibrary
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column {
                        Text(
                            text = source.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = source.type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Enable/disable switch
                Switch(
                    checked = source.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            // Source details
            SourceDetails(source)

            Divider()

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Validate button
                TextButton(
                    onClick = onValidate,
                    enabled = !isValidating
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Edit button - sample source has no user-editable fields
                    if (source.type != PhotoSourceType.SAMPLE) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }

                    // Remove button
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}

/**
 * Displays source-specific details.
 */
@Composable
private fun SourceDetails(source: PhotoSourceConfig) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (source.config) {
            is com.photoframe.core.model.SourceConfig.SmbConfig -> {
                val config = source.config as com.photoframe.core.model.SourceConfig.SmbConfig
                DetailRow("Server", config.server)
                DetailRow("Share", config.share)
                DetailRow("Path", config.path)
                DetailRow("Username", config.username)
            }
            is com.photoframe.core.model.SourceConfig.LocalConfig -> {
                val config = source.config as com.photoframe.core.model.SourceConfig.LocalConfig
                DetailRow("Folders", "${config.folderUris.size} selected")
            }
            is com.photoframe.core.model.SourceConfig.SampleConfig -> {
                DetailRow("Content", "Bundled sample photos")
            }
        }
    }
}

/**
 * Single detail row with label and value.
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Confirmation dialog for removing a source.
 */
@Composable
private fun RemoveSourceConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null)
        },
        title = {
            Text("Remove Source?")
        },
        text = {
            Text("Are you sure you want to remove this photo source? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
