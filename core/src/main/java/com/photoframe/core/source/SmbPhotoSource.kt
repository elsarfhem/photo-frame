package com.photoframe.core.source

import android.util.Log
import com.photoframe.core.data.SmbPhotoDataSource
import com.photoframe.core.model.Photo
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.smb.SmbClient

/**
 * Photo source implementation for SMB/Samba network shares.
 *
 * Wraps existing SMB scanning logic (SmbClient + SmbPhotoDataSource)
 * and adapts it to the PhotoSource interface.
 *
 * Thread Safety: All methods are thread-safe via coroutine dispatchers.
 *
 * Lifecycle:
 * 1. Create instance with configuration
 * 2. Call validate() to test connection
 * 3. Call scanPhotos() to load photo list
 *
 * @param id Unique identifier for this source
 * @param displayName Display name for UI
 * @param isEnabled Whether this source is enabled
 * @param connection SMB connection configuration
 * @param password SMB password (decrypted from CredentialStore)
 * @param smbClient SMB client for network operations
 * @param smbPhotoDataSource Data source for scanning photos
 */
class SmbPhotoSource(
    override val id: String,
    override val displayName: String,
    override val isEnabled: Boolean,
    private val connection: SmbConnection,
    private val password: String,
    private val smbClient: SmbClient,
    private val smbPhotoDataSource: SmbPhotoDataSource
) : PhotoSource {

    override val type: PhotoSourceType = PhotoSourceType.SMB

    /**
     * Scans SMB share for photos.
     *
     * Process:
     * 1. Connect to SMB share if not already connected
     * 2. Scan folder recursively for photos
     * 3. Return list of photos with metadata
     *
     * Thread Safety: Safe to call from any coroutine.
     *
     * Error Handling: Returns Result.Error if:
     * - Connection fails (network, auth, etc.)
     * - Folder not found
     * - Permission denied
     * - Scan timeout (30 seconds)
     *
     * @return Result.Success with photos (empty list if folder is empty),
     *         Result.Error if scan failed
     */
    override suspend fun scanPhotos(maxPhotos: Int?): Result<List<Photo>> {
        Log.d(TAG, "scanPhotos: Starting scan for source '$displayName'")
        Log.d(TAG, "scanPhotos: Server=${connection.serverUrl}, Path=${connection.sharePath}")

        return try {
            // Ensure connected
            if (!smbClient.isConnected()) {
                Log.d(TAG, "scanPhotos: Not connected, attempting connection...")
                val connectResult = smbClient.connect(connection, password)
                if (connectResult !is Result.Success) {
                    val errorMsg = if (connectResult is Result.Error) {
                        connectResult.message ?: connectResult.exception.message ?: "Connection failed"
                    } else {
                        "Connection failed"
                    }
                    Log.e(TAG, "scanPhotos: Connection FAILED: $errorMsg")
                    return Result.error(
                        Exception("Failed to connect to SMB share: $errorMsg"),
                        "Could not connect to ${connection.serverUrl}"
                    )
                }
                Log.d(TAG, "scanPhotos: Connection SUCCESS")
            } else {
                Log.d(TAG, "scanPhotos: Already connected")
            }

            // Scan for photos
            Log.d(TAG, "scanPhotos: Starting folder scan...")
            val scanResult = smbPhotoDataSource.scanFolder(connection, maxPhotos = maxPhotos)
            when (scanResult) {
                is Result.Success -> {
                    // Success - return photos (may be empty list)
                    val photoCount = scanResult.data.size
                    Log.d(TAG, "scanPhotos: Scan SUCCESS - Found $photoCount photos")
                    if (photoCount > 0) {
                        Log.d(TAG, "scanPhotos: First photo: ${scanResult.data.first().fileName}")
                    } else {
                        Log.w(TAG, "scanPhotos: No photos found in ${connection.fullPath}")
                    }
                    Result.success(scanResult.data)
                }
                is Result.Error -> {
                    // Scan failed
                    Log.e(TAG, "scanPhotos: Scan FAILED: ${scanResult.message}", scanResult.exception)
                    Result.error(
                        scanResult.exception,
                        scanResult.message ?: "Failed to scan folder"
                    )
                }
                is Result.Loading -> {
                    // Should not happen
                    Log.e(TAG, "scanPhotos: Unexpected Loading state")
                    Result.error(
                        IllegalStateException("Unexpected loading state"),
                        "Scan returned unexpected state"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "scanPhotos: Exception during scan", e)
            Result.error(
                e,
                "Failed to scan SMB source '$displayName': ${e.message}"
            )
        }
    }

    companion object {
        private const val TAG = "SmbPhotoSource"
    }

    /**
     * Validates SMB connection and credentials.
     *
     * Tests:
     * - Can connect to server
     * - Credentials are valid
     * - Share exists and is accessible
     * - Path exists
     *
     * Thread Safety: Safe to call from any coroutine.
     *
     * @return Result.Success if valid and accessible,
     *         Result.Error with descriptive error if invalid
     */
    override suspend fun validate(): Result<Unit> {
        return try {
            // Try to connect
            val connectResult = smbClient.connect(connection, password)
            when (connectResult) {
                is Result.Success -> {
                    // Connection successful - verify path exists
                    val listResult = smbClient.listFiles(connection.fullPath)
                    when (listResult) {
                        is Result.Success -> {
                            // Path exists and is accessible
                            Result.success(Unit)
                        }
                        is Result.Error -> {
                            Result.error(
                                listResult.exception,
                                "Path '${connection.sharePath}' not found or not accessible"
                            )
                        }
                        is Result.Loading -> {
                            Result.error(
                                IllegalStateException("Unexpected loading state"),
                                "Path validation returned unexpected state"
                            )
                        }
                    }
                }
                is Result.Error -> {
                    Result.error(
                        connectResult.exception,
                        "Cannot connect to ${connection.serverUrl}: ${connectResult.message}"
                    )
                }
                is Result.Loading -> {
                    Result.error(
                        IllegalStateException("Unexpected loading state"),
                        "Connection returned unexpected state"
                    )
                }
            }
        } catch (e: Exception) {
            Result.error(
                e,
                "Validation failed for SMB source '$displayName': ${e.message}"
            )
        }
    }

    /**
     * Estimates number of photos in SMB share.
     *
     * Currently not implemented (would require full scan).
     * Future: Could be optimized with metadata caching.
     *
     * @return null (estimate unavailable)
     */
    override suspend fun estimatePhotoCount(): Int? {
        // Not implemented - would require full scan
        // Could be added with metadata caching in future
        return null
    }

    override fun toString(): String {
        return "SmbPhotoSource(id=$id, displayName=$displayName, " +
                "serverUrl=${connection.serverUrl}, sharePath=${connection.sharePath}, " +
                "enabled=$isEnabled)"
    }
}
