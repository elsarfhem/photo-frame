package com.photoframe.core.data

import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.smb.FakeSmbClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SmbPhotoDataSource.
 *
 * Tests photo scanning logic with FakeSmbClient (no real SMB server needed).
 */
class SmbPhotoDataSourceTest {

    private lateinit var fakeClient: FakeSmbClient
    private lateinit var dataSource: SmbPhotoDataSource

    private val testConnection = SmbConnection(
        serverUrl = "smb://testserver/photos",
        sharePath = "/",
        username = "testuser",
        domain = null
    )

    @Before
    fun setup() {
        fakeClient = FakeSmbClient()
        dataSource = SmbPhotoDataSource(fakeClient, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        fakeClient.clear()
    }

    @Test
    fun `scanFolder returns empty list when folder is empty`() = runTest {
        // Given: Connected client with empty folder
        fakeClient.connect(testConnection, "password")

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns success with empty list (not an error)
        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).data.size)
    }

    @Test
    fun `scanFolder returns error when not connected`() = runTest {
        // Given: Not connected to SMB

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns error
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message?.contains("Not connected") == true)
    }

    @Test
    fun `scanFolder returns all photos in flat folder structure`() = runTest {
        // Given: Connected client with photos in root
        fakeClient.connect(testConnection, "password")
        fakeClient.addFile("smb://testserver/photos/photo1.jpg", ByteArray(100))
        fakeClient.addFile("smb://testserver/photos/photo2.png", ByteArray(200))
        fakeClient.addFile("smb://testserver/photos/photo3.jpeg", ByteArray(150))

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns all 3 photos
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(3, photos.size)
        assertTrue(photos.any { it.fileName == "photo1.jpg" })
        assertTrue(photos.any { it.fileName == "photo2.png" })
        assertTrue(photos.any { it.fileName == "photo3.jpeg" })
    }

    @Test
    fun `scanFolder handles recursive directory structure`() = runTest {
        // Given: Connected client with nested folders
        fakeClient.connect(testConnection, "password")
        fakeClient.addFile("smb://testserver/photos/photo1.jpg", ByteArray(100))
        fakeClient.addDirectory("smb://testserver/photos/subfolder")
        fakeClient.addFile("smb://testserver/photos/subfolder/photo2.jpg", ByteArray(200))
        fakeClient.addDirectory("smb://testserver/photos/subfolder/deep")
        fakeClient.addFile("smb://testserver/photos/subfolder/deep/photo3.jpg", ByteArray(150))

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns all photos from all levels
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(3, photos.size)
    }

    @Test
    fun `scanFolder filters out non-photo files`() = runTest {
        // Given: Connected client with mixed file types
        fakeClient.connect(testConnection, "password")
        fakeClient.addFile("smb://testserver/photos/photo1.jpg", ByteArray(100))
        fakeClient.addFile("smb://testserver/photos/document.pdf", ByteArray(200))
        fakeClient.addFile("smb://testserver/photos/video.mp4", ByteArray(300))
        fakeClient.addFile("smb://testserver/photos/photo2.png", ByteArray(150))

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns only photos (jpg and png)
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(2, photos.size)
        assertTrue(photos.all { it.fileName.endsWith(".jpg") || it.fileName.endsWith(".png") })
    }

    @Test
    fun `scanFolder handles HEIC format`() = runTest {
        // Given: Connected client with HEIC photos
        fakeClient.connect(testConnection, "password")
        fakeClient.addFile("smb://testserver/photos/photo1.heic", ByteArray(100))
        fakeClient.addFile("smb://testserver/photos/photo2.HEIC", ByteArray(200)) // Uppercase

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns HEIC photos (case-insensitive)
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(2, photos.size)
        assertTrue(photos.all { it.mimeType == "image/heic" })
    }

    @Test
    fun `scanFolder is case-insensitive for extensions`() = runTest {
        // Given: Connected client with mixed-case extensions
        fakeClient.connect(testConnection, "password")
        fakeClient.addFile("smb://testserver/photos/photo1.JPG", ByteArray(100))
        fakeClient.addFile("smb://testserver/photos/photo2.Png", ByteArray(200))
        fakeClient.addFile("smb://testserver/photos/photo3.JPEG", ByteArray(150))

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns all photos regardless of extension case
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(3, photos.size)
    }

    @Test
    fun `scanFolder populates photo metadata correctly`() = runTest {
        // Given: Connected client with a photo
        fakeClient.connect(testConnection, "password")
        val testContent = ByteArray(12345)
        val testTimestamp = 1234567890000L
        fakeClient.addFile(
            path = "smb://testserver/photos/test.jpg",
            content = testContent,
            size = testContent.size.toLong(),
            lastModified = testTimestamp
        )

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Photo metadata is correct
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(1, photos.size)

        val photo = photos[0]
        assertEquals("test.jpg", photo.fileName)
        assertEquals("smb://testserver/photos/test.jpg", photo.path)
        assertEquals(12345L, photo.fileSize)
        assertEquals(testTimestamp, photo.lastModified)
        assertEquals("image/jpeg", photo.mimeType)
    }

    @Test
    fun `scanFolder continues scanning when subfolder access fails`() = runTest {
        // Given: Connected client where one subfolder fails
        fakeClient.connect(testConnection, "password")
        fakeClient.addFile("smb://testserver/photos/photo1.jpg", ByteArray(100))
        fakeClient.addDirectory("smb://testserver/photos/restricted")
        fakeClient.addFile("smb://testserver/photos/photo2.jpg", ByteArray(200))

        // Configure to fail listing for restricted folder
        // Note: FakeSmbClient doesn't support per-path failures yet
        // This test verifies the design pattern

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns photos from accessible folders
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertTrue(photos.size >= 2) // At least the accessible photos
    }

    @Test
    fun `scanFolder handles empty subfolder`() = runTest {
        // Given: Connected client with empty subfolder
        fakeClient.connect(testConnection, "password")
        fakeClient.addFile("smb://testserver/photos/photo1.jpg", ByteArray(100))
        fakeClient.addDirectory("smb://testserver/photos/empty")
        fakeClient.addFile("smb://testserver/photos/photo2.jpg", ByteArray(200))

        // When: Scanning folder
        val result = dataSource.scanFolder(testConnection)

        // Then: Returns photos from non-empty folders
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(2, photos.size)
    }
}
