package com.photoframe.core.smb

import android.util.Log
import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile as JcifsSmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
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
 * - Connection timeout: 5 seconds
 * - Response timeout: 10 seconds
 * - Socket timeout: 10 seconds
 *
 * I/O Cancellation: All blocking operations are wrapped in `runInterruptible`
 * so that coroutine cancellation interrupts the blocked thread. File reads use
 * chunked I/O (64KB) with interrupt checks between chunks for fast cancellation.
 *
 * Thread Safety: Protected by Mutex for all mutable state operations.
 * Safe to call from multiple coroutines concurrently.
 *
 * @param ioDispatcher Dispatcher for blocking I/O operations
 * @param connectionTimeoutMs TCP connection timeout in milliseconds (default: 5000)
 * @param responseTimeoutMs SMB response timeout in milliseconds (default: 10000)
 * @param socketTimeoutMs Socket read timeout in milliseconds (default: 10000)
 */
class JcifsSmbClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectionTimeoutMs: Long = 5_000,
    private val responseTimeoutMs: Long = 10_000,
    private val socketTimeoutMs: Long = 10_000
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
     * - Sets reduced timeouts for fast cancellation
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
            setProperty("jcifs.smb.client.soTimeout", socketTimeoutMs.toString())

            // Disable DFS referral resolution — NAS doesn't use DFS, but jcifs
            // attempts a lookup on every file open, holding a global lock for ~6s on timeout.
            // This caused cascading thread starvation and OOM kills during 24/7 operation.
            setProperty("jcifs.smb.client.dfs.disabled", "true")

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

                val context = baseContext.withCredentials(authenticator)

                // Test connection — runInterruptible converts cancellation → thread interrupt
                val testPath = connection.serverUrl.trimEnd('/') + "/"
                runInterruptible(ioDispatcher) {
                    val smbFile = JcifsSmbFile(testPath, context)
                    smbFile.exists()
                }

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
            runInterruptible(ioDispatcher) {
                val smbDir = JcifsSmbFile(directoryPath, context)

                if (!smbDir.exists()) {
                    return@runInterruptible Result.error(
                        IOException("Directory not found: $directoryPath"),
                        "Directory does not exist"
                    )
                }

                if (!smbDir.isDirectory) {
                    return@runInterruptible Result.error(
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
            }
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
            runInterruptible(ioDispatcher) {
                val smbFile = JcifsSmbFile(filePath, context)

                if (!smbFile.exists()) {
                    return@runInterruptible Result.error(
                        IOException("File not found: $filePath"),
                        "File does not exist"
                    )
                }

                if (smbFile.isDirectory) {
                    return@runInterruptible Result.error(
                        IOException("Path is a directory: $filePath"),
                        "Path must be a file"
                    )
                }

                val fileSize = smbFile.length()
                if (fileSize > MAX_FILE_SIZE_BYTES) {
                    return@runInterruptible Result.error(
                        IOException("File too large: ${fileSize / (1024 * 1024)}MB exceeds ${MAX_FILE_SIZE_BYTES / (1024 * 1024)}MB limit"),
                        "File too large to load into memory"
                    )
                }

                // Chunked read with interrupt checks between chunks.
                // readBytes() blocks until EOF with no cancellation points.
                // Reading in 64KB chunks lets us check Thread.interrupted()
                // between reads, so coroutine cancellation (via runInterruptible)
                // takes effect within one chunk rather than waiting for socket timeout.
                smbFile.inputStream.use { input ->
                    val output = ByteArrayOutputStream(fileSize.toInt().coerceAtMost(INITIAL_BUFFER_BYTES))
                    val chunk = ByteArray(READ_CHUNK_BYTES)
                    var bytesRead: Int

                    while (input.read(chunk).also { bytesRead = it } != -1) {
                        if (Thread.interrupted()) {
                            throw InterruptedIOException("SMB read interrupted")
                        }
                        output.write(chunk, 0, bytesRead)
                    }

                    Result.success(output.toByteArray())
                }
            }
        } catch (e: InterruptedIOException) {
            Log.d(TAG, "readFile interrupted (coroutine cancelled): $filePath")
            Result.error(e, "Read cancelled")
        } catch (e: SmbException) {
            Result.error(e, mapSmbExceptionMessage(e))
        } catch (e: IOException) {
            Result.error(e, "Network error: ${e.message}")
        } catch (e: Exception) {
            Result.error(e, "Failed to read file: ${e.message}")
        }
    }

    /**
     * Streams an SMB file directly to a local file without holding it in memory.
     * Uses the same chunked read approach as [readFile] but writes to disk.
     */
    override suspend fun readFileToFile(filePath: String, destFile: File): Result<Long> {
        val context = currentContext
            ?: return Result.error(
                IllegalStateException("Not connected"),
                "Must call connect() before reading files"
            )

        return try {
            runInterruptible(ioDispatcher) {
                val smbFile = JcifsSmbFile(filePath, context)

                if (!smbFile.exists()) {
                    return@runInterruptible Result.error(
                        IOException("File not found: $filePath"),
                        "File does not exist"
                    )
                }

                if (smbFile.isDirectory) {
                    return@runInterruptible Result.error(
                        IOException("Path is a directory: $filePath"),
                        "Path must be a file"
                    )
                }

                smbFile.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val chunk = ByteArray(READ_CHUNK_BYTES)
                        var totalBytes = 0L
                        var bytesRead: Int

                        while (input.read(chunk).also { bytesRead = it } != -1) {
                            if (Thread.interrupted()) {
                                throw InterruptedIOException("SMB read interrupted")
                            }
                            output.write(chunk, 0, bytesRead)
                            totalBytes += bytesRead
                        }

                        Result.success(totalBytes)
                    }
                }
            }
        } catch (e: InterruptedIOException) {
            Log.d(TAG, "readFileToFile interrupted (coroutine cancelled): $filePath")
            destFile.delete()
            Result.error(e, "Read cancelled")
        } catch (e: SmbException) {
            destFile.delete()
            Result.error(e, mapSmbExceptionMessage(e))
        } catch (e: IOException) {
            destFile.delete()
            Result.error(e, "Network error: ${e.message}")
        } catch (e: Exception) {
            destFile.delete()
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

            val config = createSecureConfiguration()
            val baseContext = BaseContext(config)

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

            val context = baseContext.withCredentials(authenticator)

            // Test connection — runInterruptible converts cancellation → thread interrupt
            runInterruptible(ioDispatcher) {
                val testPath = connection.serverUrl.trimEnd('/') + "/"
                val smbFile = JcifsSmbFile(testPath, context)
                smbFile.exists()
            }

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

    companion object {
        private const val TAG = "JcifsSmbClient"

        /** Chunk size for interruptible file reads (64KB). */
        private const val READ_CHUNK_BYTES = 64 * 1024

        /** Initial buffer capacity hint to avoid resizing (1MB). */
        private const val INITIAL_BUFFER_BYTES = 1024 * 1024

        /** Maximum file size we'll read into memory (100MB). */
        private const val MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024
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
