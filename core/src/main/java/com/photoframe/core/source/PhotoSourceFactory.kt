package com.photoframe.core.source

import android.net.Uri
import com.photoframe.core.data.LocalPhotoDataSource
import com.photoframe.core.data.SmbPhotoDataSource
import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.Result
import com.photoframe.core.model.SourceConfig
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.security.CredentialStore
import com.photoframe.core.smb.SmbClient
import javax.inject.Inject

/**
 * Factory for creating PhotoSource instances from configurations.
 *
 * Handles:
 * - Converting PhotoSourceConfig to concrete PhotoSource implementations
 * - Loading encrypted credentials
 * - Injecting required dependencies
 *
 * Thread Safety: All methods are thread-safe.
 *
 * @param smbClient SMB client for network operations
 * @param smbPhotoDataSource Data source for SMB scanning
 * @param localPhotoDataSource Data source for local scanning
 * @param credentialStore Store for encrypted credentials
 */
class PhotoSourceFactory @Inject constructor(
    private val smbClient: SmbClient,
    private val smbPhotoDataSource: SmbPhotoDataSource,
    private val localPhotoDataSource: LocalPhotoDataSource,
    private val credentialStore: CredentialStore
) {
    /**
     * Creates a PhotoSource from configuration.
     *
     * @param config Source configuration
     * @return Result.Success with PhotoSource, or Result.Error if creation failed
     */
    suspend fun createSource(config: PhotoSourceConfig): Result<PhotoSource> {
        return try {
            when (val sourceConfig = config.config) {
                is SourceConfig.SmbConfig -> {
                    createSmbSource(config.id, config.displayName, config.isEnabled, sourceConfig)
                }
                is SourceConfig.LocalConfig -> {
                    createLocalSource(config.id, config.displayName, config.isEnabled, sourceConfig)
                }
                is SourceConfig.SampleConfig -> {
                    createSampleSource(config.id, config.displayName, config.isEnabled)
                }
            }
        } catch (e: Exception) {
            Result.error(
                e,
                "Failed to create source '${config.displayName}': ${e.message}"
            )
        }
    }

    /**
     * Creates an SMB photo source.
     *
     * @param id Source ID
     * @param displayName Display name
     * @param isEnabled Whether enabled
     * @param config SMB configuration
     * @return Result with SmbPhotoSource
     */
    private suspend fun createSmbSource(
        id: String,
        displayName: String,
        isEnabled: Boolean,
        config: SourceConfig.SmbConfig
    ): Result<PhotoSource> {
        return try {
            // Load password from credential store
            val passwordResult = credentialStore.retrievePassword(getCredentialKey(id))
            if (passwordResult !is Result.Success) {
                return Result.error(
                    IllegalStateException("Password not found"),
                    "SMB password for '$displayName' not found in credential store"
                )
            }

            val password = passwordResult.data

            // Create SMB connection
            // Build serverUrl from config
            val serverUrl = "smb://${config.server}/${config.share}"
            val sharePath = config.path

            val connection = SmbConnection(
                serverUrl = serverUrl,
                sharePath = sharePath,
                username = config.username,
                domain = config.domain
            )

            // Create source
            val source = SmbPhotoSource(
                id = id,
                displayName = displayName,
                isEnabled = isEnabled,
                connection = connection,
                password = password,
                smbClient = smbClient,
                smbPhotoDataSource = smbPhotoDataSource
            )

            Result.success(source)
        } catch (e: Exception) {
            Result.error(
                e,
                "Failed to create SMB source: ${e.message}"
            )
        }
    }

    /**
     * Creates a local photo source.
     *
     * @param id Source ID
     * @param displayName Display name
     * @param isEnabled Whether enabled
     * @param config Local configuration
     * @return Result with LocalPhotoSource
     */
    private fun createLocalSource(
        id: String,
        displayName: String,
        isEnabled: Boolean,
        config: SourceConfig.LocalConfig
    ): Result<PhotoSource> {
        return try {
            // Convert URI strings to Uri objects
            val folderUris = config.folderUris.map { uriString ->
                Uri.parse(uriString)
            }

            // Create source
            val source = LocalPhotoSource(
                id = id,
                displayName = displayName,
                isEnabled = isEnabled,
                folderUris = folderUris,
                localPhotoDataSource = localPhotoDataSource
            )

            Result.success(source)
        } catch (e: Exception) {
            Result.error(
                e,
                "Failed to create local source: ${e.message}"
            )
        }
    }

    /**
     * Creates a sample/demo photo source.
     *
     * @param id Source ID
     * @param displayName Display name
     * @param isEnabled Whether enabled
     * @return Result with SampleDataPhotoSource
     */
    private fun createSampleSource(
        id: String,
        displayName: String,
        isEnabled: Boolean
    ): Result<PhotoSource> {
        return Result.success(
            SampleDataPhotoSource(
                id = id,
                displayName = displayName,
                isEnabled = isEnabled
            )
        )
    }

    /**
     * Gets the credential store key for a source.
     *
     * @param sourceId Source ID
     * @return Credential key
     */
    private fun getCredentialKey(sourceId: String): String {
        return "photo_source_$sourceId"
    }

    companion object {
        /**
         * Prefix for credential keys in credential store.
         */
        const val CREDENTIAL_KEY_PREFIX = "photo_source_"
    }
}
