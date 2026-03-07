package com.photoframe.core.model

import androidx.compose.runtime.Immutable

/**
 * Configuration for connecting to an SMB/Samba network share.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * SECURITY NOTE: This class contains sensitive data (username, password).
 * - Never log this class or its properties
 * - Never include in analytics events
 * - Password should be stored encrypted in DataStore via CredentialStore
 *
 * @property serverUrl SMB server URL (e.g., "smb://192.168.1.100/photos" or "smb://nas.local/media")
 * @property sharePath Path to the photo folder on the share (e.g., "/family/vacation")
 * @property username Username for SMB authentication
 * @property domain Windows domain name (optional, use null for workgroup)
 */
@Immutable
data class SmbConnection(
    val serverUrl: String,
    val sharePath: String,
    val username: String,
    val domain: String? = null
) {
    /**
     * Returns the full SMB path (server + share path).
     */
    val fullPath: String
        get() = serverUrl.trimEnd('/') + "/" + sharePath.trim('/')

    /**
     * Returns a sanitized string representation that excludes sensitive data.
     * Safe for logging and debugging.
     */
    fun toSafeString(): String =
        "SmbConnection(serverUrl=$serverUrl, sharePath=$sharePath, username=${username.take(2)}***, domain=$domain)"

    override fun toString(): String = toSafeString()

    companion object {
        /**
         * Validates an SMB server URL format.
         * Returns true if valid, false otherwise.
         */
        fun isValidServerUrl(url: String): Boolean {
            return url.startsWith("smb://", ignoreCase = true) && url.length > 6
        }
    }
}
