package com.photoframe.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Configuration for a photo source.
 *
 * Persisted in DataStore as JSON. Represents a configured source
 * (SMB share, local folder, etc.) with all necessary configuration.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * Serialization: Uses kotlinx.serialization for DataStore persistence.
 *
 * @property id Unique identifier for this source (e.g., "smb-1", "local-dcim")
 * @property type Type of source (SMB, LOCAL, etc.)
 * @property displayName User-facing name (e.g., "Home NAS", "Camera Roll")
 * @property isEnabled Whether this source is currently enabled
 * @property config Source-specific configuration (see SourceConfig sealed class)
 */
@Immutable
@Serializable
data class PhotoSourceConfig(
    val id: String,
    val type: PhotoSourceType,
    val displayName: String,
    val isEnabled: Boolean,
    val config: SourceConfig
) {
    companion object {
        /**
         * Creates a new SMB source configuration.
         *
         * @param id Unique ID for this source
         * @param displayName Display name
         * @param server SMB server address
         * @param share Share name
         * @param path Path within share
         * @param domain Domain/workgroup
         * @param username Username
         * @param isEnabled Whether enabled
         * @return PhotoSourceConfig for SMB
         */
        fun createSmb(
            id: String,
            displayName: String,
            server: String,
            share: String,
            path: String,
            domain: String,
            username: String,
            isEnabled: Boolean = true
        ): PhotoSourceConfig {
            return PhotoSourceConfig(
                id = id,
                type = PhotoSourceType.SMB,
                displayName = displayName,
                isEnabled = isEnabled,
                config = SourceConfig.SmbConfig(
                    server = server,
                    share = share,
                    path = path,
                    domain = domain,
                    username = username
                )
            )
        }

        /**
         * Creates a new local source configuration.
         *
         * @param id Unique ID for this source
         * @param displayName Display name
         * @param folderUris List of content:// URIs for selected folders
         * @param isEnabled Whether enabled
         * @return PhotoSourceConfig for local storage
         */
        fun createLocal(
            id: String,
            displayName: String,
            folderUris: List<String>,
            isEnabled: Boolean = true
        ): PhotoSourceConfig {
            return PhotoSourceConfig(
                id = id,
                type = PhotoSourceType.LOCAL,
                displayName = displayName,
                isEnabled = isEnabled,
                config = SourceConfig.LocalConfig(
                    folderUris = folderUris
                )
            )
        }
    }
}

/**
 * Source-specific configuration.
 *
 * Sealed class allows type-safe configuration for different source types.
 * Each source type has its own configuration data class.
 *
 * Thread Safety: Immutable sealed class, safe to share across threads.
 *
 * Serialization: Uses kotlinx.serialization polymorphic serialization.
 */
@Serializable
sealed class SourceConfig {
    /**
     * Configuration for SMB/Samba network share.
     *
     * Note: Password is NOT stored here - it's stored separately
     * in KeystoreCredentialStore for security.
     *
     * @property server Server address (hostname or IP)
     * @property share Share name
     * @property path Path within share (e.g., "/photos")
     * @property domain Domain or workgroup name
     * @property username Username for authentication
     */
    @Serializable
    data class SmbConfig(
        val server: String,
        val share: String,
        val path: String,
        val domain: String,
        val username: String
    ) : SourceConfig()

    /**
     * Configuration for local device storage.
     *
     * Uses content:// URIs from Storage Access Framework (SAF).
     * URIs must have persistable permissions via takePersistableUriPermission().
     *
     * @property folderUris List of content:// URIs to scan
     *
     * Example URIs:
     * - "content://com.android.providers.media.documents/tree/primary%3ADCIM"
     * - "content://com.android.providers.media.documents/tree/primary%3APictures"
     */
    @Serializable
    data class LocalConfig(
        val folderUris: List<String>
    ) : SourceConfig()

    // Future: GoogleDriveConfig, DropboxConfig, FtpConfig, etc.
}
