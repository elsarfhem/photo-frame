package com.photoframe.core.security

/**
 * Interface for securely storing and retrieving SMB credentials.
 *
 * Implementations MUST encrypt credentials using Android Keystore.
 *
 * Thread Safety: Implementations must be thread-safe for concurrent access.
 */
interface CredentialStore {
    /**
     * Stores SMB password securely (encrypted).
     *
     * @param key Unique identifier for the credential (e.g., "smb_password")
     * @param password The password to store (will be encrypted)
     * @return Result.Success if stored successfully, Result.Error if failed
     */
    suspend fun storePassword(key: String, password: String): com.photoframe.core.model.Result<Unit>

    /**
     * Retrieves and decrypts SMB password.
     *
     * @param key Unique identifier for the credential
     * @return Result.Success with decrypted password, Result.Error if not found or decryption failed
     */
    suspend fun retrievePassword(key: String): com.photoframe.core.model.Result<String>

    /**
     * Deletes a stored credential.
     *
     * @param key Unique identifier for the credential
     * @return Result.Success if deleted successfully, Result.Error if failed
     */
    suspend fun deletePassword(key: String): com.photoframe.core.model.Result<Unit>

    /**
     * Checks if a credential exists.
     *
     * @param key Unique identifier for the credential
     * @return true if credential exists, false otherwise
     */
    suspend fun hasPassword(key: String): Boolean

    /**
     * Clears all stored credentials.
     * Use with caution - this cannot be undone.
     *
     * @return Result.Success if cleared successfully, Result.Error if failed
     */
    suspend fun clearAll(): com.photoframe.core.model.Result<Unit>
}
