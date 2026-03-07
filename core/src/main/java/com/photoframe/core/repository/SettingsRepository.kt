package com.photoframe.core.repository

import com.photoframe.core.model.Result
import com.photoframe.core.model.SlideshowSettings
import com.photoframe.core.model.SmbConnection
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing application settings.
 *
 * Provides methods for persisting and retrieving:
 * - SMB connection configuration (with encrypted credentials)
 * - Display/slideshow settings
 *
 * Settings are exposed as StateFlow for reactive UI updates.
 *
 * Thread Safety: All methods are thread-safe and can be called from any coroutine.
 */
interface SettingsRepository {
    /**
     * StateFlow of current SMB connection settings.
     * Emits null if no connection is configured.
     */
    val smbConnection: StateFlow<SmbConnection?>

    /**
     * StateFlow of current slideshow/display settings.
     * Emits default settings if none are configured.
     */
    val slideshowSettings: StateFlow<SlideshowSettings>

    /**
     * Saves SMB connection configuration.
     * Password is automatically encrypted via CredentialStore.
     *
     * @param connection SMB connection configuration (without password)
     * @param password SMB password (will be encrypted before storage)
     * @return Result.Success if saved, Result.Error if save failed
     */
    suspend fun saveSmbConnection(connection: SmbConnection, password: String): Result<Unit>

    /**
     * Loads SMB connection configuration.
     * Returns null if no connection is configured.
     *
     * @return Result.Success with connection (or null), Result.Error if load failed
     */
    suspend fun loadSmbConnection(): Result<SmbConnection?>

    /**
     * Retrieves the decrypted SMB password.
     *
     * @return Result.Success with password, Result.Error if not found or decryption failed
     */
    suspend fun getSmbPassword(): Result<String>

    /**
     * Clears the saved SMB connection and credentials.
     *
     * @return Result.Success if cleared, Result.Error if clear failed
     */
    suspend fun clearSmbConnection(): Result<Unit>

    /**
     * Saves slideshow/display settings.
     *
     * @param settings Slideshow settings to save
     * @return Result.Success if saved, Result.Error if save failed
     */
    suspend fun saveSlideshowSettings(settings: SlideshowSettings): Result<Unit>

    /**
     * Loads slideshow/display settings.
     * Returns default settings if none are configured.
     *
     * @return Result.Success with settings, Result.Error if load failed
     */
    suspend fun loadSlideshowSettings(): Result<SlideshowSettings>

    /**
     * Checks if this is the first launch of the app.
     *
     * @return Result.Success with true if first launch, false otherwise
     */
    suspend fun isFirstLaunch(): Result<Boolean>

    /**
     * Marks the first launch as complete (sets flag to false).
     *
     * @return Result.Success if saved, Result.Error if save failed
     */
    suspend fun markFirstLaunchComplete(): Result<Unit>
}
