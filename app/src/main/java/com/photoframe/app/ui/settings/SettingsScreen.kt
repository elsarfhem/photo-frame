package com.photoframe.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photoframe.core.model.TransitionType

/**
 * Settings screen for configuring photo sources and display settings.
 *
 * Material 3 design with form sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSources: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Show save success/error snackbar
    LaunchedEffect(state.saveResult) {
        when (state.saveResult) {
            is SaveResult.Success -> {
                // Success handled by caller (navigate back)
            }
            is SaveResult.Failure -> {
                // Error shown in UI
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Photo Sources Section
            PhotoSourcesSection(
                onManageSources = onNavigateToSources
            )

            Divider()

            // Display Settings Section
            DisplaySettingsSection(
                state = state,
                onIntervalChange = viewModel::updateDisplayInterval,
                onTransitionChange = viewModel::updateTransitionType,
                onShuffleChange = viewModel::toggleShuffle,
                onPanAnimationChange = viewModel::togglePanAnimation
            )

            Divider()

            // Action Buttons
            ActionButtons(
                state = state,
                onSave = viewModel::saveSettings,
                onReset = viewModel::resetToDefaults,
                onDismissSaveResult = viewModel::clearSaveResult
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplaySettingsSection(
    state: SettingsState,
    onIntervalChange: (Int) -> Unit,
    onTransitionChange: (TransitionType) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onPanAnimationChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Display Settings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )

        // Display Interval Dropdown
        var intervalExpanded by remember { mutableStateOf(false) }
        val intervalOptions = listOf(10, 15, 30, 60, 300) // 10s, 15s, 30s, 1min, 5min

        ExposedDropdownMenuBox(
            expanded = intervalExpanded,
            onExpandedChange = { intervalExpanded = it }
        ) {
            OutlinedTextField(
                value = formatInterval(state.displayInterval),
                onValueChange = {},
                readOnly = true,
                label = { Text("Display Interval") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = intervalExpanded,
                onDismissRequest = { intervalExpanded = false }
            ) {
                intervalOptions.forEach { seconds ->
                    DropdownMenuItem(
                        text = { Text(formatInterval(seconds)) },
                        onClick = {
                            onIntervalChange(seconds)
                            intervalExpanded = false
                        }
                    )
                }
            }
        }

        // Transition Type Radio Buttons
        Text(
            text = "Transition Effect",
            style = MaterialTheme.typography.titleMedium
        )

        TransitionType.values().forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp) // Minimum touch target
            ) {
                RadioButton(
                    selected = state.transitionType == type,
                    onClick = { onTransitionChange(type) },
                    modifier = Modifier.semantics {
                        contentDescription = when (type) {
                            TransitionType.FADE -> "Select fade transition effect"
                            TransitionType.SLIDE -> "Select slide transition effect"
                            TransitionType.ZOOM_KEN_BURNS -> "Select zoom or Ken Burns transition effect"
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (type) {
                        TransitionType.FADE -> "Fade"
                        TransitionType.SLIDE -> "Slide"
                        TransitionType.ZOOM_KEN_BURNS -> "Zoom / Ken Burns"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Shuffle Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp), // Minimum touch target
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shuffle Photos",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = state.shuffleEnabled,
                onCheckedChange = onShuffleChange,
                modifier = Modifier.semantics {
                    contentDescription = if (state.shuffleEnabled) {
                        "Shuffle photos enabled"
                    } else {
                        "Shuffle photos disabled"
                    }
                }
            )
        }

        // Pan Animation Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp), // Minimum touch target
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pan Animation (No Black Bands)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Slowly pan photos to show full image",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.panAnimationEnabled,
                onCheckedChange = onPanAnimationChange,
                modifier = Modifier.semantics {
                    contentDescription = if (state.panAnimationEnabled) {
                        "Pan animation enabled"
                    } else {
                        "Pan animation disabled"
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionButtons(
    state: SettingsState,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDismissSaveResult: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Save Result
        when (val result = state.saveResult) {
            is SaveResult.Success -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings saved successfully!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(onClick = onDismissSaveResult) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            is SaveResult.Failure -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Failed to save settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onDismissSaveResult) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            null -> {}
        }

        // Save Button
        Button(
            onClick = onSave,
            enabled = !state.isSaving && state.isModified,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp) // Minimum touch target
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saving...")
            } else {
                Text("Save Settings")
            }
        }

        // Reset Button
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp), // Minimum touch target
            enabled = !state.isSaving
        ) {
            Text("Reset to Defaults")
        }
    }
}

/**
 * Formats display interval in seconds to human-readable string.
 */
private fun formatInterval(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds == 60 -> "1 min"
        seconds < 3600 -> "${seconds / 60} min"
        else -> "${seconds / 3600} hr"
    }
}

/**
 * Photo Sources section with navigation to sources management screen.
 */
@Composable
private fun PhotoSourcesSection(
    onManageSources: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Photo Sources",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )

        Text(
            text = "Configure where your photos come from - network shares (SMB), local storage, and more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onManageSources,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Manage Photo Sources")
        }
    }
}
