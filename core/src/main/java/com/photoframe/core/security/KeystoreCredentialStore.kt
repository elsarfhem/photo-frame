package com.photoframe.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.photoframe.core.model.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-based credential storage implementation.
 *
 * Security:
 * - Uses Android Keystore for key management (hardware-backed when available)
 * - AES-256 encryption with GCM mode (authenticated encryption)
 * - Each credential stored with unique IV (initialization vector)
 * - Keys never leave the Keystore
 *
 * Thread Safety:
 * - All operations use DataStore which is thread-safe
 * - Keystore operations are synchronized internally
 * - Safe to call from multiple coroutines
 *
 * @param context Application context for DataStore
 */
class KeystoreCredentialStore(
    private val context: Context
) : CredentialStore {

    private val Context.credentialDataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATASTORE_NAME
    )

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // Ensure master key exists
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            generateMasterKey()
        }
    }

    override suspend fun storePassword(key: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Encrypt the password
                val encryptedData = encryptPassword(password)

                // Store encrypted data in DataStore
                context.credentialDataStore.edit { preferences ->
                    preferences[stringPreferencesKey(key)] = encryptedData
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    exception = e,
                    message = "Failed to store password: ${e.message}"
                )
            }
        }

    override suspend fun retrievePassword(key: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Retrieve encrypted data from DataStore
                val preferences = context.credentialDataStore.data.first()
                val encryptedData = preferences[stringPreferencesKey(key)]

                if (encryptedData == null) {
                    return@withContext Result.error(
                        exception = NoSuchElementException("Credential not found for key: $key"),
                        message = "Credential not found"
                    )
                }

                // Decrypt the password
                val decryptedPassword = decryptPassword(encryptedData)
                Result.success(decryptedPassword)
            } catch (e: Exception) {
                Result.error(
                    exception = e,
                    message = "Failed to retrieve password: ${e.message}"
                )
            }
        }

    override suspend fun deletePassword(key: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                context.credentialDataStore.edit { preferences ->
                    preferences.remove(stringPreferencesKey(key))
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    exception = e,
                    message = "Failed to delete password: ${e.message}"
                )
            }
        }

    override suspend fun hasPassword(key: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val preferences = context.credentialDataStore.data.first()
                preferences.contains(stringPreferencesKey(key))
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun clearAll(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                context.credentialDataStore.edit { preferences ->
                    preferences.clear()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    exception = e,
                    message = "Failed to clear credentials: ${e.message}"
                )
            }
        }

    /**
     * Encrypts a password using AES-256 GCM.
     *
     * Returns: Base64-encoded string containing IV and encrypted data separated by ":"
     * Format: "base64(IV):base64(encryptedData)"
     */
    private fun encryptPassword(password: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateMasterKey()

        // Initialize cipher with random IV (GCM mode)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // Encrypt the password
        val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

        // Get the IV used for this encryption
        val iv = cipher.iv

        // Encode IV and encrypted data as Base64
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        // Return as "IV:EncryptedData"
        return "$ivBase64:$encryptedBase64"
    }

    /**
     * Decrypts a password using AES-256 GCM.
     *
     * @param encryptedData Base64-encoded string in format "base64(IV):base64(encryptedData)"
     */
    private fun decryptPassword(encryptedData: String): String {
        // Split IV and encrypted data
        val parts = encryptedData.split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

        // Initialize cipher with the stored IV
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateMasterKey()
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        // Decrypt the password
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Gets the master key from Keystore, or creates it if it doesn't exist.
     */
    private fun getOrCreateMasterKey(): SecretKey {
        return if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
        } else {
            generateMasterKey()
        }
    }

    /**
     * Generates a new AES-256 master key in Android Keystore.
     */
    private fun generateMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false) // Don't require biometric auth for each use
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "photoframe_master_key"
        private const val DATASTORE_NAME = "encrypted_credentials"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256 // AES-256
        private const val GCM_TAG_LENGTH = 128 // 128-bit authentication tag

        /**
         * Key names for storing different credentials.
         */
        object Keys {
            const val SMB_PASSWORD = "smb_password"
        }
    }
}
