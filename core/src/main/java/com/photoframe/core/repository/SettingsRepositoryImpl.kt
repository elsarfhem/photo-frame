package com.photoframe.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Result
import com.photoframe.core.model.SlideshowSettings
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.model.TransitionType
import com.photoframe.core.security.CredentialStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore Preferences.
 *
 * Storage:
 * - SMB connection config → DataStore (server, share, username, domain)
 * - SMB password → CredentialStore (encrypted with Android Keystore)
 * - Display settings → DataStore (interval, shuffle, transition)
 *
 * Thread Safety:
 * - DataStore operations are thread-safe by design
 * - CredentialStore operations are thread-safe (uses Keystore + Mutex)
 * - StateFlow updates are atomic and thread-safe
 *
 * @param dataStore DataStore for preferences storage
 * @param credentialStore CredentialStore for encrypted password storage
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val credentialStore: CredentialStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    private val _smbConnection = MutableStateFlow<SmbConnection?>(null)
    override val smbConnection: StateFlow<SmbConnection?> = _smbConnection.asStateFlow()

    private val _slideshowSettings = MutableStateFlow(SlideshowSettings.DEFAULT)
    override val slideshowSettings: StateFlow<SlideshowSettings> = _slideshowSettings.asStateFlow()

    init {
        // Note: In a real implementation, you'd launch a coroutine here to load initial settings
        // For now, settings are loaded on-demand via load methods
    }

    override suspend fun saveSmbConnection(connection: SmbConnection, password: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                // Save password to encrypted storage first
                val passwordResult = credentialStore.storePassword(KEY_SMB_PASSWORD, password)
                if (passwordResult is Result.Error) {
                    return@withContext passwordResult
                }

                // Save connection config to DataStore
                dataStore.edit { prefs ->
                    prefs[KEY_SMB_SERVER_URL] = connection.serverUrl
                    prefs[KEY_SMB_SHARE_PATH] = connection.sharePath
                    prefs[KEY_SMB_USERNAME] = connection.username
                    connection.domain?.let { prefs[KEY_SMB_DOMAIN] = it } ?: prefs.remove(KEY_SMB_DOMAIN)
                }

                // Update StateFlow
                _smbConnection.value = connection

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Failed to save SMB connection: ${e.message}")
            }
        }

    override suspend fun loadSmbConnection(): Result<SmbConnection?> = withContext(ioDispatcher) {
        try {
            val prefs = dataStore.data.first()

            val serverUrl = prefs[KEY_SMB_SERVER_URL]
            val sharePath = prefs[KEY_SMB_SHARE_PATH]
            val username = prefs[KEY_SMB_USERNAME]

            if (serverUrl == null || sharePath == null || username == null) {
                // No connection configured
                _smbConnection.value = null
                return@withContext Result.success(null)
            }

            val domain = prefs[KEY_SMB_DOMAIN]

            val connection = SmbConnection(
                serverUrl = serverUrl,
                sharePath = sharePath,
                username = username,
                domain = domain
            )

            // Update StateFlow
            _smbConnection.value = connection

            Result.success(connection)
        } catch (e: Exception) {
            Result.error(e, "Failed to load SMB connection: ${e.message}")
        }
    }

    override suspend fun getSmbPassword(): Result<String> = withContext(ioDispatcher) {
        credentialStore.retrievePassword(KEY_SMB_PASSWORD)
    }

    override suspend fun clearSmbConnection(): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Clear password from credential store
            val passwordResult = credentialStore.deletePassword(KEY_SMB_PASSWORD)
            if (passwordResult is Result.Error) {
                return@withContext passwordResult
            }

            // Clear connection from DataStore
            dataStore.edit { prefs ->
                prefs.remove(KEY_SMB_SERVER_URL)
                prefs.remove(KEY_SMB_SHARE_PATH)
                prefs.remove(KEY_SMB_USERNAME)
                prefs.remove(KEY_SMB_DOMAIN)
            }

            // Update StateFlow
            _smbConnection.value = null

            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to clear SMB connection: ${e.message}")
        }
    }

    override suspend fun saveSlideshowSettings(settings: SlideshowSettings): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                dataStore.edit { prefs ->
                    prefs[KEY_DISPLAY_INTERVAL] = settings.displayIntervalSeconds
                    prefs[KEY_SHUFFLE] = if (settings.shuffleEnabled) 1 else 0
                    prefs[KEY_TRANSITION_TYPE] = settings.transitionType.name
                    prefs[KEY_PAN_ANIMATION] = if (settings.panAnimationEnabled) 1 else 0
                }

                // Update StateFlow
                _slideshowSettings.value = settings

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Failed to save slideshow settings: ${e.message}")
            }
        }

    override suspend fun loadSlideshowSettings(): Result<SlideshowSettings> = withContext(ioDispatcher) {
        try {
            val prefs = dataStore.data.first()

            val displayInterval = prefs[KEY_DISPLAY_INTERVAL] ?: SlideshowSettings.DEFAULT.displayIntervalSeconds
            val shuffleEnabled = (prefs[KEY_SHUFFLE] ?: 0) == 1
            val transitionTypeName = prefs[KEY_TRANSITION_TYPE] ?: TransitionType.FADE.name
            val panAnimationEnabled = (prefs[KEY_PAN_ANIMATION] ?: 1) == 1 // Default to true

            val transitionType = try {
                TransitionType.valueOf(transitionTypeName)
            } catch (e: IllegalArgumentException) {
                TransitionType.FADE // Default if invalid value
            }

            val settings = SlideshowSettings(
                displayIntervalSeconds = displayInterval,
                shuffleEnabled = shuffleEnabled,
                transitionType = transitionType,
                panAnimationEnabled = panAnimationEnabled
            )

            // Update StateFlow
            _slideshowSettings.value = settings

            Result.success(settings)
        } catch (e: Exception) {
            Result.error(e, "Failed to load slideshow settings: ${e.message}")
        }
    }

    override suspend fun isFirstLaunch(): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val prefs = dataStore.data.first()
            val isFirst = prefs[KEY_FIRST_LAUNCH] ?: true // Default to true if not set
            Result.success(isFirst)
        } catch (e: Exception) {
            Result.error(e, "Failed to check first launch: ${e.message}")
        }
    }

    override suspend fun markFirstLaunchComplete(): Result<Unit> = withContext(ioDispatcher) {
        try {
            dataStore.edit { prefs ->
                prefs[KEY_FIRST_LAUNCH] = false
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to mark first launch complete: ${e.message}")
        }
    }

    companion object {
        // DataStore keys for SMB connection
        private val KEY_SMB_SERVER_URL = stringPreferencesKey("smb_server_url")
        private val KEY_SMB_SHARE_PATH = stringPreferencesKey("smb_share_path")
        private val KEY_SMB_USERNAME = stringPreferencesKey("smb_username")
        private val KEY_SMB_DOMAIN = stringPreferencesKey("smb_domain")

        // DataStore keys for slideshow settings
        private val KEY_DISPLAY_INTERVAL = intPreferencesKey("display_interval_seconds")
        private val KEY_SHUFFLE = intPreferencesKey("shuffle")
        private val KEY_TRANSITION_TYPE = stringPreferencesKey("transition_type")
        private val KEY_PAN_ANIMATION = intPreferencesKey("pan_animation_enabled")

        // DataStore keys for app state
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")

        // CredentialStore key for SMB password
        private const val KEY_SMB_PASSWORD = "smb_password"
    }
}
