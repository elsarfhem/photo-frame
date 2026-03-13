package com.photoframe.app.ui.settings

import com.photoframe.core.model.SlideshowSettings
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.model.TransitionType

/**
 * UI state for the Settings screen.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * @property smbServer SMB server hostname or IP
 * @property smbShare SMB share name
 * @property smbUsername SMB username
 * @property smbPassword SMB password (plain text in UI, encrypted on save)
 * @property smbDomain SMB domain (optional)
 * @property displayInterval Display interval in seconds
 * @property transitionType Transition effect type
 * @property shuffleEnabled Shuffle mode enabled
 * @property panAnimationEnabled Pan animation enabled
 * @property isTestingConnection True when testing SMB connection
 * @property connectionTestResult Result of connection test (null = not tested)
 * @property isSaving True when saving settings
 * @property saveResult Result of save operation (null = not saved)
 * @property validationErrors Map of field name to error message
 * @property isModified True if settings have been modified
 */
data class SettingsState(
    // SMB Configuration
    val smbServer: String = "",
    val smbShare: String = "",
    val smbUsername: String = "",
    val smbPassword: String = "",
    val smbDomain: String = "",

    // Display Settings
    val displayInterval: Int = SlideshowSettings.DEFAULT_DISPLAY_INTERVAL_SECONDS,
    val transitionType: TransitionType = TransitionType.DEFAULT,
    val shuffleEnabled: Boolean = false,
    val panAnimationEnabled: Boolean = true,

    // UI State
    val isTestingConnection: Boolean = false,
    val connectionTestResult: ConnectionTestResult? = null,
    val isSaving: Boolean = false,
    val saveResult: SaveResult? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val isModified: Boolean = false
) {
    companion object {
        val EMPTY = SettingsState()
    }
}

/**
 * Result of SMB connection test.
 */
sealed class ConnectionTestResult {
    object Success : ConnectionTestResult()
    data class Failure(val message: String) : ConnectionTestResult()
}

/**
 * Result of settings save operation.
 */
sealed class SaveResult {
    object Success : SaveResult()
    data class Failure(val message: String) : SaveResult()
}
