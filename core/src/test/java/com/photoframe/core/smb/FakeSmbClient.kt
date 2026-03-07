package com.photoframe.core.smb

import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection

/**
 * Fake implementation of SmbClient for testing.
 *
 * Allows testing without a real SMB server.
 * Per Senior Dev 2 requirement: SMB test doubles for testability.
 *
 * Usage:
 * ```
 * val fakeClient = FakeSmbClient()
 * fakeClient.addFile("smb://server/share/photo1.jpg", byteArrayOf(...))
 * fakeClient.addFile("smb://server/share/folder/photo2.jpg", byteArrayOf(...))
 *
 * // Test scanning
 * val dataSource = SmbPhotoDataSource(fakeClient, Dispatchers.IO)
 * val result = dataSource.scanFolder(connection)
 * ```
 *
 * Thread Safety: This is a test double - thread safety is not required for single-threaded tests.
 */
class FakeSmbClient : SmbClient {

    private var connected = false
    private var currentConnection: SmbConnection? = null
    private val files = mutableMapOf<String, FakeFile>()

    // Test configuration
    var shouldFailConnect = false
    var shouldFailDisconnect = false
    var shouldFailListFiles = false
    var shouldFailReadFile = false
    var shouldFailTestConnection = false

    var connectErrorMessage = "Connection failed"
    var listFilesErrorMessage = "Failed to list files"
    var readFileErrorMessage = "Failed to read file"

    override suspend fun connect(connection: SmbConnection, password: String): Result<Unit> {
        return if (shouldFailConnect) {
            Result.error(Exception(connectErrorMessage), connectErrorMessage)
        } else {
            connected = true
            currentConnection = connection
            Result.success(Unit)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return if (shouldFailDisconnect) {
            Result.error(Exception("Disconnect failed"), "Disconnect failed")
        } else {
            connected = false
            currentConnection = null
            Result.success(Unit)
        }
    }

    override suspend fun listFiles(directoryPath: String): Result<List<SmbFile>> {
        if (!connected) {
            return Result.error(
                IllegalStateException("Not connected"),
                "Must call connect() before listing files"
            )
        }

        if (shouldFailListFiles) {
            return Result.error(Exception(listFilesErrorMessage), listFilesErrorMessage)
        }

        // Find all files that are direct children of this directory
        val normalizedPath = directoryPath.trimEnd('/')
        val children = files.values
            .filter { file ->
                val filePath = file.smbFile.path.substringBeforeLast('/')
                filePath == normalizedPath
            }
            .map { it.smbFile }

        return Result.success(children)
    }

    override suspend fun readFile(filePath: String): Result<ByteArray> {
        if (!connected) {
            return Result.error(
                IllegalStateException("Not connected"),
                "Must call connect() before reading files"
            )
        }

        if (shouldFailReadFile) {
            return Result.error(Exception(readFileErrorMessage), readFileErrorMessage)
        }

        val file = files[filePath]
            ?: return Result.error(
                Exception("File not found: $filePath"),
                "File does not exist"
            )

        if (file.smbFile.isDirectory) {
            return Result.error(
                Exception("Path is a directory: $filePath"),
                "Path must be a file"
            )
        }

        return Result.success(file.content)
    }

    override suspend fun testConnection(connection: SmbConnection, password: String): Result<Unit> {
        return if (shouldFailTestConnection) {
            Result.error(Exception("Connection test failed"), "Connection test failed")
        } else {
            Result.success(Unit)
        }
    }

    override fun isConnected(): Boolean = connected

    // Test helper methods

    /**
     * Adds a file to the fake SMB share.
     *
     * @param path Full SMB path (e.g., "smb://server/share/folder/file.jpg")
     * @param content File content as byte array
     * @param size File size in bytes (defaults to content length)
     * @param lastModified Last modified timestamp (defaults to current time)
     */
    fun addFile(
        path: String,
        content: ByteArray = byteArrayOf(),
        size: Long = content.size.toLong(),
        lastModified: Long = System.currentTimeMillis()
    ) {
        val name = path.substringAfterLast('/')
        val smbFile = SmbFile(
            path = path,
            name = name,
            isDirectory = false,
            size = size,
            lastModified = lastModified
        )
        files[path] = FakeFile(smbFile, content)
    }

    /**
     * Adds a directory to the fake SMB share.
     *
     * @param path Full SMB path (e.g., "smb://server/share/folder")
     */
    fun addDirectory(path: String) {
        val name = path.trimEnd('/').substringAfterLast('/')
        val smbFile = SmbFile(
            path = path,
            name = name,
            isDirectory = true,
            size = 0,
            lastModified = System.currentTimeMillis()
        )
        files[path] = FakeFile(smbFile, byteArrayOf())
    }

    /**
     * Clears all files and directories from the fake SMB share.
     */
    fun clear() {
        files.clear()
    }

    /**
     * Returns the number of files and directories in the fake SMB share.
     */
    fun fileCount(): Int = files.size

    /**
     * Sets up a typical photo folder structure for testing.
     *
     * Structure:
     * - smb://server/share/photo1.jpg
     * - smb://server/share/photo2.png
     * - smb://server/share/subfolder/photo3.jpg
     * - smb://server/share/subfolder/photo4.heic
     */
    fun setupTypicalPhotoStructure(serverUrl: String = "smb://server/share") {
        addFile("$serverUrl/photo1.jpg", createFakeImageBytes(100))
        addFile("$serverUrl/photo2.png", createFakeImageBytes(200))
        addDirectory("$serverUrl/subfolder")
        addFile("$serverUrl/subfolder/photo3.jpg", createFakeImageBytes(150))
        addFile("$serverUrl/subfolder/photo4.heic", createFakeImageBytes(180))
    }

    /**
     * Creates fake image bytes for testing.
     * Just returns a byte array with the specified size.
     */
    private fun createFakeImageBytes(size: Int): ByteArray {
        return ByteArray(size) { it.toByte() }
    }

    private data class FakeFile(
        val smbFile: SmbFile,
        val content: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FakeFile

            if (smbFile != other.smbFile) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = smbFile.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }
}
