package com.photoframe.core.smb

import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection

/**
 * Interface for SMB/Samba client operations.
 *
 * Provides methods for connecting to SMB shares, listing files, and reading file contents.
 * Designed for testability - implementations can be mocked or faked in tests.
 *
 * Thread Safety: All methods are suspend functions and must be safe to call from any coroutine.
 * Implementations must handle concurrent access safely.
 *
 * Security: Implementations MUST enforce SMB 2.0+ protocol (reject SMB 1.x) per P0 security requirement.
 */
interface SmbClient {
    /**
     * Establishes a connection to the SMB share.
     *
     * @param connection SMB connection configuration
     * @param password SMB password (should be decrypted from CredentialStore)
     * @return Result.Success if connected, Result.Error if connection failed
     */
    suspend fun connect(connection: SmbConnection, password: String): Result<Unit>

    /**
     * Closes the current SMB connection.
     * Safe to call multiple times or when not connected.
     *
     * @return Result.Success if disconnected, Result.Error if error occurred
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Lists all files in the specified SMB directory path.
     * Does NOT include subdirectories - use for single-level listing.
     *
     * @param directoryPath Full SMB path to directory (e.g., "smb://server/share/folder")
     * @return Result.Success with list of SmbFile entries, Result.Error if listing failed
     */
    suspend fun listFiles(directoryPath: String): Result<List<SmbFile>>

    /**
     * Reads the contents of a file from the SMB share.
     *
     * @param filePath Full SMB path to file (e.g., "smb://server/share/folder/image.jpg")
     * @return Result.Success with file bytes, Result.Error if read failed
     */
    suspend fun readFile(filePath: String): Result<ByteArray>

    /**
     * Tests the SMB connection without fully connecting.
     * Useful for validating credentials and server availability.
     *
     * @param connection SMB connection configuration
     * @param password SMB password
     * @return Result.Success if connection test passed, Result.Error with details if failed
     */
    suspend fun testConnection(connection: SmbConnection, password: String): Result<Unit>

    /**
     * Returns true if currently connected to an SMB share.
     */
    fun isConnected(): Boolean
}

/**
 * Represents a file or directory entry in an SMB share.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * @property path Full SMB path to the file/directory
 * @property name File/directory name (without path)
 * @property isDirectory True if this is a directory, false if file
 * @property size File size in bytes (0 for directories)
 * @property lastModified Timestamp when the file was last modified (milliseconds since epoch)
 */
data class SmbFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)
