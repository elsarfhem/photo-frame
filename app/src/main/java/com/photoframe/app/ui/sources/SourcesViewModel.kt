package com.photoframe.app.ui.sources

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result
import com.photoframe.core.repository.MultiSourcePhotoRepository
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.security.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing photo sources.
 *
 * Responsibilities:
 * - Load and display configured sources
 * - Add new sources (SMB, local)
 * - Remove sources
 * - Enable/disable sources
 * - Validate source configurations
 *
 * @param multiSourceRepository Repository for multi-source management
 * @param settingsRepository Repository for accessing settings
 * @param credentialStore Credential store for SMB passwords
 */
@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val multiSourceRepository: MultiSourcePhotoRepository,
    private val settingsRepository: SettingsRepository,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _state = MutableStateFlow(SourcesState())
    val state: StateFlow<SourcesState> = _state.asStateFlow()

    init {
        loadSources()
    }

    /**
     * Loads all configured photo sources.
     */
    private fun loadSources() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            multiSourceRepository.photoSources.collect { sources ->
                _state.update {
                    it.copy(
                        sources = sources,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Adds a new SMB source.
     */
    fun addSmbSource(
        displayName: String,
        server: String,
        share: String,
        path: String,
        domain: String,
        username: String,
        password: String
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // "demo" as the server field bypasses real SMB setup entirely and
            // adds a bundled sample-photo source instead. Other SMB fields are ignored.
            if (server.trim().equals("demo", ignoreCase = true)) {
                val sourceId = "sample-${System.currentTimeMillis()}"
                val sourceConfig = PhotoSourceConfig.createSample(
                    id = sourceId,
                    displayName = displayName.ifBlank { "Sample Photos" }
                )

                val result = multiSourceRepository.addPhotoSource(sourceConfig)

                when (result) {
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                showAddDialog = false,
                                successMessage = "Source added successfully"
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                error = result.message ?: "Failed to add source"
                            )
                        }
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
                return@launch
            }

            // Validate required fields
            val validationError = when {
                server.isBlank() -> "Server is required"
                share.isBlank() -> "Share is required"
                username.isBlank() -> "Username is required"
                password.isBlank() -> "Password is required"
                else -> null
            }

            if (validationError != null) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = validationError
                    )
                }
                return@launch
            }

            // Generate unique ID
            val sourceId = "smb-${System.currentTimeMillis()}"

            // Create source config
            val sourceConfig = PhotoSourceConfig.createSmb(
                id = sourceId,
                displayName = displayName.ifBlank { "SMB Share (${server})" },
                server = server,
                share = share,
                path = path,
                domain = domain.ifBlank { "WORKGROUP" },
                username = username,
                isEnabled = true
            )

            // Store password in credential store BEFORE adding source
            // Use source-specific credential key format: "photo_source_<sourceId>"
            val credentialKey = "photo_source_$sourceId"
            val storePasswordResult = credentialStore.storePassword(credentialKey, password)

            if (storePasswordResult !is Result.Success) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to store password securely"
                    )
                }
                return@launch
            }

            // Add source
            val result = multiSourceRepository.addPhotoSource(sourceConfig)

            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            showAddDialog = false,
                            successMessage = "Source added successfully"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = result.message ?: "Failed to add source"
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Adds a new local storage source.
     */
    fun addLocalSource(
        displayName: String,
        folderUris: List<Uri>
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // Generate unique ID
            val sourceId = "local-${System.currentTimeMillis()}"

            // Convert URIs to strings
            val uriStrings = folderUris.map { it.toString() }

            // Create source config
            val sourceConfig = PhotoSourceConfig.createLocal(
                id = sourceId,
                displayName = displayName.ifBlank { "Local Storage (${folderUris.size} folders)" },
                folderUris = uriStrings,
                isEnabled = true
            )

            // Add source
            val result = multiSourceRepository.addPhotoSource(sourceConfig)

            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            showAddDialog = false,
                            successMessage = "Source added successfully"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = result.message ?: "Failed to add source"
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Removes a photo source.
     */
    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = multiSourceRepository.removePhotoSource(sourceId)

            when (result) {
                is Result.Success -> {
                    // Clean up stored password for this source
                    val credentialKey = "photo_source_$sourceId"
                    credentialStore.deletePassword(credentialKey)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Source removed successfully"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to remove source"
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Toggles source enabled state.
     */
    fun toggleSourceEnabled(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            val result = multiSourceRepository.setSourceEnabled(sourceId, enabled)

            when (result) {
                is Result.Success -> {
                    // State updated via flow
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(error = result.message ?: "Failed to update source")
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Validates a source configuration.
     */
    fun validateSource(sourceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isValidating = true, error = null) }

            val result = multiSourceRepository.validateSource(sourceId)

            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isValidating = false,
                            successMessage = "Source is valid and accessible"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isValidating = false,
                            error = result.message ?: "Source validation failed"
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Shows add source dialog.
     */
    fun showAddDialog(sourceType: PhotoSourceType? = null) {
        _state.update {
            it.copy(
                showAddDialog = true,
                selectedSourceType = sourceType
            )
        }
    }

    /**
     * Hides add source dialog.
     */
    fun hideAddDialog() {
        _state.update {
            it.copy(
                showAddDialog = false,
                selectedSourceType = null
            )
        }
    }

    /**
     * Shows confirmation dialog for removing source.
     */
    fun showRemoveConfirmation(sourceId: String) {
        _state.update {
            it.copy(
                showRemoveConfirmation = true,
                sourceIdToRemove = sourceId
            )
        }
    }

    /**
     * Hides remove confirmation dialog.
     */
    fun hideRemoveConfirmation() {
        _state.update {
            it.copy(
                showRemoveConfirmation = false,
                sourceIdToRemove = null
            )
        }
    }

    /**
     * Clears success message.
     */
    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Clears error message.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Shows edit source dialog.
     */
    fun showEditDialog(sourceId: String) {
        val source = _state.value.sources.find { it.id == sourceId }
        _state.update {
            it.copy(
                showEditDialog = true,
                sourceToEdit = source
            )
        }
    }

    /**
     * Hides edit source dialog.
     */
    fun hideEditDialog() {
        _state.update {
            it.copy(
                showEditDialog = false,
                sourceToEdit = null
            )
        }
    }

    /**
     * Edits an existing SMB source.
     */
    fun editSmbSource(
        sourceId: String,
        displayName: String,
        server: String,
        share: String,
        path: String,
        domain: String,
        username: String,
        password: String
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // Validate required fields (password is optional on edit)
            val validationError = when {
                server.isBlank() -> "Server is required"
                share.isBlank() -> "Share is required"
                username.isBlank() -> "Username is required"
                else -> null
            }

            if (validationError != null) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = validationError
                    )
                }
                return@launch
            }

            // Create updated source config
            val sourceConfig = PhotoSourceConfig.createSmb(
                id = sourceId,
                displayName = displayName.ifBlank { "SMB Share (${server})" },
                server = server,
                share = share,
                path = path,
                domain = domain.ifBlank { "WORKGROUP" },
                username = username,
                isEnabled = true
            )

            // Update password in credential store only if provided
            if (password.isNotBlank()) {
                val credentialKey = "photo_source_$sourceId"
                val storePasswordResult = credentialStore.storePassword(credentialKey, password)

                if (storePasswordResult !is Result.Success) {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to store password securely"
                        )
                    }
                    return@launch
                }
            }

            // Update source
            val result = multiSourceRepository.updatePhotoSource(sourceConfig)

            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            showEditDialog = false,
                            sourceToEdit = null,
                            successMessage = "Source updated successfully"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = result.message ?: "Failed to update source"
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Edits an existing local storage source.
     */
    fun editLocalSource(
        sourceId: String,
        displayName: String,
        folderUris: List<Uri>
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            // Convert URIs to strings
            val uriStrings = folderUris.map { it.toString() }

            // Create updated source config
            val sourceConfig = PhotoSourceConfig.createLocal(
                id = sourceId,
                displayName = displayName.ifBlank { "Local Storage (${folderUris.size} folders)" },
                folderUris = uriStrings,
                isEnabled = true
            )

            // Update source
            val result = multiSourceRepository.updatePhotoSource(sourceConfig)

            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            showEditDialog = false,
                            sourceToEdit = null,
                            successMessage = "Source updated successfully"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = result.message ?: "Failed to update source"
                        )
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }
}

/**
 * UI state for sources screen.
 */
data class SourcesState(
    val sources: List<PhotoSourceConfig> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isValidating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val selectedSourceType: PhotoSourceType? = null,
    val showRemoveConfirmation: Boolean = false,
    val sourceIdToRemove: String? = null,
    val showEditDialog: Boolean = false,
    val sourceToEdit: PhotoSourceConfig? = null
)
