package com.photoframe.core.smb

import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile as JcifsSmbFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.Properties

/**
 * jcifs-ng implementation of SmbClient.
 *
 * P0 SECURITY: Enforces SMB 2.0+ protocol minimum (rejects insecure SMB 1.x).
 * This addresses the critical security concern identified in NFR assessment (Senior Dev 1).
 *
 * Configuration:
 * - Minimum protocol: SMB 2.1.0
 * - Maximum protocol: SMB 3.1.1
 * - SMB signing: Preferred (enabled when server supports it)
 * - Connection timeout: 30 seconds
 * - Response timeout: 30 seconds
 *
 * Thread Safety: Protected by Mutex for all mutable state operations.
 * Safe to call from multiple coroutines concurrently.
 *
 * @param connectionTimeoutMs Connection timeout in milliseconds (default: 30000)
 * @param responseTimeoutMs Response timeout in milliseconds (default: 30000)
 */
class JcifsSmbClient(
    private val connectionTimeoutMs: Long = 30000,
    private val responseTimeoutMs: Long = 30000
) : SmbClient {

    private val mutex = Mutex()

    @Volatile
    private var currentContext: CIFSContext? = null

    @Volatile
    private var currentConnection: SmbConnection? = null

    /**
     * Creates the jcifs configuration with P0 security requirements.
     *
     * CRITICAL SECURITY CONFIGURATION:
     * - Enforces SMB 2.1.0 minimum (rejects SMB 1.x which is insecure)
     * - Enables SMB signing for message authentication
     * - Sets reasonable timeouts (30 seconds)
     */
    private fun createSecureConfiguration(): PropertyConfiguration {
        val properties = Properties().apply {
            // P0 SECURITY: Force SMB 2.0+ only (reject SMB 1.x)
            // SMB210 = SMB 2.1.0, SMB311 = SMB 3.1.1
            setProperty("jcifs.smb.client.minVersion", "SMB210")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")

            // Enable SMB signing for security (prevent tampering)
            setProperty("jcifs.smb.client.signingPreferred", "true")

            // Timeouts (in milliseconds)
            setProperty("jcifs.smb.client.connTimeout", connectionTimeoutMs.toString())
            setProperty("jcifs.smb.client.responseTimeout", responseTimeoutMs.toString())
            setProperty("jcifs.smb.client.soTimeout", responseTimeoutMs.toString())

            // Disable DNS lookups for performance
            setProperty("jcifs.resolveOrder", "BCAST")

            // Disable extended security for compatibility with older servers (but still SMB 2.0+)
            setProperty("jcifs.smb.client.useExtendedSecurity", "true")
        }

        return PropertyConfiguration(properties)
    }

    override suspend fun connect(connection: SmbConnection, password: String): Result<Unit> {
        return mutex.withLock {
            try {
                // Validate server URL
                if (!SmbConnection.isValidServerUrl(connection.serverUrl)) {
                    return Result.error(
                        IllegalArgumentException("Invalid SMB server URL: ${connection.serverUrl}"),
                        "Server URL must start with 'smb://'"
                    )
                }

                // Create secure configuration (SMB 2.0+ only)
                val config = createSecureConfiguration()
                val baseContext = BaseContext(config)

                // Create authenticator
                val authenticator = if (connection.domain != null) {
                    // Domain authentication
                    NtlmPasswordAuthenticator(
                        connection.domain,
                        connection.username,
                        password
                    )
                } else {
                    // Workgroup authentication (no domain)
                    NtlmPasswordAuthenticator(
                        connection.username,
                        password
                    )
                }

                // Create authenticated context
                val context = baseContext.withCredentials(authenticator)

                // Test the connection by listing root directory
                val testPath = connection.serverUrl.trimEnd('/') + "/"
                val smbFile = JcifsSmbFile(testPath, context)

                // This will throw if connection fails (authentication, network, etc.)
                smbFile.exists()

                // Connection successful
                currentContext = context
                currentConnection = connection

                Result.success(Unit)
            } catch (e: SmbException) {
                currentContext = null
                currentConnection = null
                Result.error(e, mapSmbExceptionMessage(e))
            } catch (e: IOException) {
                currentContext = null
                currentConnection = null
                Result.error(e, "Network error: ${e.message}")
            } catch (e: Exception) {
                currentContext = null
                currentConnection = null
                Result.error(e, "Connection failed: ${e.message}")
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return mutex.withLock {
            try {
                currentContext = null
                currentConnection = null
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Disconnect failed: ${e.message}")
            }
        }
    }

    override suspend fun listFiles(directoryPath: String): Result<List<SmbFile>> {
        val context = currentContext
            ?: return Result.error(
                IllegalStateException("Not connected"),
                "Must call connect() before listing files"
            )

        return try {
            val smbDir = JcifsSmbFile(directoryPath, context)

            if (!smbDir.exists()) {
                return Result.error(
                    IOException("Directory not found: $directoryPath"),
                    "Directory does not exist"
                )
            }

            if (!smbDir.isDirectory) {
                return Result.error(
                    IOException("Path is not a directory: $directoryPath"),
                    "Path must be a directory"
                )
            }

            val files = smbDir.listFiles()?.mapNotNull { jcifsFile ->
                try {
                    SmbFile(
                        path = jcifsFile.path,
                        name = jcifsFile.name.trimEnd('/'),
                        isDirectory = jcifsFile.isDirectory,
                        size = if (jcifsFile.isDirectory) 0 else jcifsFile.length(),
                        lastModified = jcifsFile.lastModified()
                    )
                } catch (e: Exception) {
                    // Skip files that can't be accessed (permission denied, etc.)
                    null
                }
            } ?: emptyList()

            Result.success(files)
        } catch (e: SmbException) {
            Result.error(e, mapSmbExceptionMessage(e))
        } catch (e: IOException) {
            Result.error(e, "Network error: ${e.message}")
        } catch (e: Exception) {
            Result.error(e, "Failed to list files: ${e.message}")
        }
    }

    override suspend fun readFile(filePath: String): Result<ByteArray> {
        val context = currentContext
            ?: return Result.error(
                IllegalStateException("Not connected"),
                "Must call connect() before reading files"
            )

        return try {
            val smbFile = JcifsSmbFile(filePath, context)

            if (!smbFile.exists()) {
                return Result.error(
                    IOException("File not found: $filePath"),
                    "File does not exist"
                )
            }

            if (smbFile.isDirectory) {
                return Result.error(
                    IOException("Path is a directory: $filePath"),
                    "Path must be a file"
                )
            }

            val bytes = smbFile.inputStream.use { it.readBytes() }
            Result.success(bytes)
        } catch (e: SmbException) {
            Result.error(e, mapSmbExceptionMessage(e))
        } catch (e: IOException) {
            Result.error(e, "Network error: ${e.message}")
        } catch (e: Exception) {
            Result.error(e, "Failed to read file: ${e.message}")
        }
    }

    override suspend fun testConnection(connection: SmbConnection, password: String): Result<Unit> {
        return try {
            // Validate server URL
            if (!SmbConnection.isValidServerUrl(connection.serverUrl)) {
                return Result.error(
                    IllegalArgumentException("Invalid SMB server URL: ${connection.serverUrl}"),
                    "Server URL must start with 'smb://'"
                )
            }

            // Create secure configuration (SMB 2.0+ only)
            val config = createSecureConfiguration()
            val baseContext = BaseContext(config)

            // Create authenticator
            val authenticator = if (connection.domain != null) {
                NtlmPasswordAuthenticator(
                    connection.domain,
                    connection.username,
                    password
                )
            } else {
                NtlmPasswordAuthenticator(
                    connection.username,
                    password
                )
            }

            // Create authenticated context
            val context = baseContext.withCredentials(authenticator)

            // Test the connection
            val testPath = connection.serverUrl.trimEnd('/') + "/"
            val smbFile = JcifsSmbFile(testPath, context)
            smbFile.exists() // Will throw if connection fails

            Result.success(Unit)
        } catch (e: SmbException) {
            Result.error(e, mapSmbExceptionMessage(e))
        } catch (e: IOException) {
            Result.error(e, "Network error: ${e.message}")
        } catch (e: Exception) {
            Result.error(e, "Connection test failed: ${e.message}")
        }
    }

    override fun isConnected(): Boolean {
        return currentContext != null && currentConnection != null
    }

    /**
     * Maps jcifs SmbException to user-friendly error messages.
     */
    private fun mapSmbExceptionMessage(exception: SmbException): String {
        return when (exception.message) {
            "Logon failure: unknown user name or bad password." ->
                "Authentication failed. Please check your username and password."
            "The network path was not found." ->
                "Server not found. Please check the server address."
            "Access is denied." ->
                "Permission denied. Please check your access rights."
            "The network name cannot be found." ->
                "Share not found. Please check the share path."
            else ->
                "SMB error: ${exception.message ?: "Unknown error"}"
        }
    }
}
