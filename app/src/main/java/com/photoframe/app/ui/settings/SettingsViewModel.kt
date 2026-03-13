package com.photoframe.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoframe.core.model.Result
import com.photoframe.core.model.SlideshowSettings
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.smb.SmbClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 *
 * Architecture: MVVM pattern
 * - Manages settings UI state
 * - Validates user input
 * - Saves settings to SettingsRepository
 * - Tests SMB connection via SmbClient
 *
 * Thread Safety: All public methods are safe to call from main thread.
 *
 * @param settingsRepository Repository for loading/saving settings
 * @param smbClient Client for testing SMB connection
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val smbClient: SmbClient
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState.EMPTY)
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Loads current settings from SettingsRepository.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // Load SMB connection
            val smbResult = settingsRepository.loadSmbConnection()
            if (smbResult is Result.Success) {
                val connection = smbResult.data
                if (connection != null) {
                    val passwordResult = settingsRepository.getSmbPassword()
                    val password = if (passwordResult is Result.Success) passwordResult.data else ""

                    _state.value = _state.value.copy(
                        smbServer = connection.serverUrl,
                        smbShare = connection.sharePath,
                        smbUsername = connection.username,
                        smbPassword = password,
                        smbDomain = connection.domain ?: ""
                    )
                }
            }

            // Load slideshow settings
            val settingsResult = settingsRepository.loadSlideshowSettings()
            if (settingsResult is Result.Success) {
                val settings = settingsResult.data
                _state.value = _state.value.copy(
                    displayInterval = settings.displayIntervalSeconds,
                    transitionType = settings.transitionType,
                    shuffleEnabled = settings.shuffleEnabled,
                    panAnimationEnabled = settings.panAnimationEnabled,
                    isModified = false
                )
            }
        }
    }

    /**
     * Updates SMB server field.
     */
    fun updateSmbServer(value: String) {
        _state.value = _state.value.copy(
            smbServer = value,
            isModified = true,
            connectionTestResult = null // Reset test result
        )
        validateSmbServer()
    }

    /**
     * Updates SMB share field.
     */
    fun updateSmbShare(value: String) {
        _state.value = _state.value.copy(
            smbShare = value,
            isModified = true,
            connectionTestResult = null
        )
        validateSmbShare()
    }

    /**
     * Updates SMB username field.
     */
    fun updateSmbUsername(value: String) {
        _state.value = _state.value.copy(
            smbUsername = value,
            isModified = true,
            connectionTestResult = null
        )
        validateSmbUsername()
    }

    /**
     * Updates SMB password field.
     */
    fun updateSmbPassword(value: String) {
        _state.value = _state.value.copy(
            smbPassword = value,
            isModified = true,
            connectionTestResult = null
        )
        validateSmbPassword()
    }

    /**
     * Updates SMB domain field.
     */
    fun updateSmbDomain(value: String) {
        _state.value = _state.value.copy(
            smbDomain = value,
            isModified = true,
            connectionTestResult = null
        )
    }

    /**
     * Updates display interval.
     */
    fun updateDisplayInterval(seconds: Int) {
        _state.value = _state.value.copy(
            displayInterval = seconds,
            isModified = true
        )
        validateDisplayInterval()
    }

    /**
     * Updates transition type.
     */
    fun updateTransitionType(type: com.photoframe.core.model.TransitionType) {
        _state.value = _state.value.copy(
            transitionType = type,
            isModified = true
        )
    }

    /**
     * Toggles shuffle mode.
     */
    fun toggleShuffle(enabled: Boolean) {
        _state.value = _state.value.copy(
            shuffleEnabled = enabled,
            isModified = true
        )
    }

    /**
     * Toggles pan animation enabled setting.
     */
    fun togglePanAnimation(enabled: Boolean) {
        _state.value = _state.value.copy(
            panAnimationEnabled = enabled,
            isModified = true
        )
    }

    /**
     * Tests the SMB connection with current settings.
     */
    fun testConnection() {
        viewModelScope.launch {
            val currentState = _state.value

            // Validate first
            if (!validateAllFields()) {
                _state.value = currentState.copy(
                    connectionTestResult = ConnectionTestResult.Failure("Please fix validation errors")
                )
                return@launch
            }

            _state.value = currentState.copy(
                isTestingConnection = true,
                connectionTestResult = null
            )

            val connection = SmbConnection(
                serverUrl = currentState.smbServer,
                sharePath = currentState.smbShare,
                username = currentState.smbUsername,
                domain = currentState.smbDomain.ifBlank { null }
            )

            val result = smbClient.testConnection(connection, currentState.smbPassword)
            _state.value = _state.value.copy(
                isTestingConnection = false,
                connectionTestResult = when (result) {
                    is Result.Success -> ConnectionTestResult.Success
                    is Result.Error -> ConnectionTestResult.Failure(
                        result.message ?: "Connection test failed"
                    )
                    is Result.Loading -> null // Should not happen
                }
            )
        }
    }

    /**
     * Saves all settings to SettingsRepository.
     */
    fun saveSettings() {
        viewModelScope.launch {
            val currentState = _state.value

            // Validate first
            if (!validateAllFields()) {
                _state.value = currentState.copy(
                    saveResult = SaveResult.Failure("Please fix validation errors")
                )
                return@launch
            }

            _state.value = currentState.copy(
                isSaving = true,
                saveResult = null
            )

            // SMB connection save removed - SMB config now managed in separate Photo Sources screen

            // Save slideshow settings
            val slideshowSettings = SlideshowSettings(
                displayIntervalSeconds = currentState.displayInterval,
                transitionType = currentState.transitionType,
                shuffleEnabled = currentState.shuffleEnabled,
                panAnimationEnabled = currentState.panAnimationEnabled
            )

            val settingsResult = settingsRepository.saveSlideshowSettings(slideshowSettings)

            _state.value = _state.value.copy(
                isSaving = false,
                saveResult = when (settingsResult) {
                    is Result.Success -> SaveResult.Success
                    is Result.Error -> SaveResult.Failure(
                        settingsResult.message ?: "Failed to save settings"
                    )
                    is Result.Loading -> null // Should not happen
                },
                isModified = false
            )
        }
    }

    /**
     * Resets all settings to defaults.
     */
    fun resetToDefaults() {
        _state.value = SettingsState(
            displayInterval = SlideshowSettings.DEFAULT_DISPLAY_INTERVAL_SECONDS,
            transitionType = com.photoframe.core.model.TransitionType.DEFAULT,
            shuffleEnabled = false,
            panAnimationEnabled = true,
            isModified = true
        )
    }

    /**
     * Clears the save result (dismisses success/error message).
     */
    fun clearSaveResult() {
        _state.value = _state.value.copy(saveResult = null)
    }

    /**
     * Clears the connection test result.
     */
    fun clearConnectionTestResult() {
        _state.value = _state.value.copy(connectionTestResult = null)
    }

    /**
     * Validates all form fields.
     *
     * @return True if all fields are valid
     */
    private fun validateAllFields(): Boolean {
        // SMB validation removed - SMB config now managed in separate Photo Sources screen
        validateDisplayInterval()

        return _state.value.validationErrors.isEmpty()
    }

    /**
     * Validates SMB server field.
     */
    private fun validateSmbServer() {
        val errors = _state.value.validationErrors.toMutableMap()
        if (_state.value.smbServer.isBlank()) {
            errors["smbServer"] = "Server is required"
        } else {
            errors.remove("smbServer")
        }
        _state.value = _state.value.copy(validationErrors = errors)
    }

    /**
     * Validates SMB share field.
     */
    private fun validateSmbShare() {
        val errors = _state.value.validationErrors.toMutableMap()
        if (_state.value.smbShare.isBlank()) {
            errors["smbShare"] = "Share is required"
        } else {
            errors.remove("smbShare")
        }
        _state.value = _state.value.copy(validationErrors = errors)
    }

    /**
     * Validates SMB username field.
     */
    private fun validateSmbUsername() {
        val errors = _state.value.validationErrors.toMutableMap()
        if (_state.value.smbUsername.isBlank()) {
            errors["smbUsername"] = "Username is required"
        } else {
            errors.remove("smbUsername")
        }
        _state.value = _state.value.copy(validationErrors = errors)
    }

    /**
     * Validates SMB password field.
     */
    private fun validateSmbPassword() {
        val errors = _state.value.validationErrors.toMutableMap()
        if (_state.value.smbPassword.isBlank()) {
            errors["smbPassword"] = "Password is required"
        } else {
            errors.remove("smbPassword")
        }
        _state.value = _state.value.copy(validationErrors = errors)
    }

    /**
     * Validates display interval field.
     */
    private fun validateDisplayInterval() {
        val errors = _state.value.validationErrors.toMutableMap()
        val interval = _state.value.displayInterval
        if (interval < SlideshowSettings.MIN_DISPLAY_INTERVAL ||
            interval > SlideshowSettings.MAX_DISPLAY_INTERVAL
        ) {
            errors["displayInterval"] = "Interval must be between " +
                    "${SlideshowSettings.MIN_DISPLAY_INTERVAL} and " +
                    "${SlideshowSettings.MAX_DISPLAY_INTERVAL} seconds"
        } else {
            errors.remove("displayInterval")
        }
        _state.value = _state.value.copy(validationErrors = errors)
    }

}
