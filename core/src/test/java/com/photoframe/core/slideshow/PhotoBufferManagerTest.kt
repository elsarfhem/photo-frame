package com.photoframe.core.slideshow

import android.graphics.Bitmap
import com.photoframe.core.image.ImageCache
import com.photoframe.core.model.Photo
import com.photoframe.core.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PhotoBufferManager.
 *
 * Tests:
 * - Buffer initialization
 * - LRU eviction (max 4 photos)
 * - Next/previous navigation
 * - Pre-loading behavior
 * - Error handling
 * - Edge cases (empty list, single photo)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoBufferManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var imageCache: ImageCache
    private lateinit var bufferManager: PhotoBufferManager

    // Test data
    private val testPhotos = listOf(
        Photo("smb://server/photo1.jpg", "photo1.jpg", 1000, System.currentTimeMillis(), "image/jpeg"),
        Photo("smb://server/photo2.jpg", "photo2.jpg", 2000, System.currentTimeMillis(), "image/jpeg"),
        Photo("smb://server/photo3.jpg", "photo3.jpg", 3000, System.currentTimeMillis(), "image/jpeg"),
        Photo("smb://server/photo4.jpg", "photo4.jpg", 4000, System.currentTimeMillis(), "image/jpeg"),
        Photo("smb://server/photo5.jpg", "photo5.jpg", 5000, System.currentTimeMillis(), "image/jpeg"),
        Photo("smb://server/photo6.jpg", "photo6.jpg", 6000, System.currentTimeMillis(), "image/jpeg")
    )

    private val testBitmap: Bitmap = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        imageCache = mockk()
        bufferManager = PhotoBufferManager(imageCache, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize with empty list returns error`() = runTest {
        val result = bufferManager.initialize(emptyList())

        assertTrue(result is Result.Error)
        assertEquals("Cannot initialize buffer with empty photo list", (result as Result.Error).message)
    }

    @Test
    fun `initialize with invalid start index returns error`() = runTest {
        val result = bufferManager.initialize(testPhotos, startIndex = 10)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `initialize loads current photo successfully`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        val result = bufferManager.initialize(testPhotos, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Result.Success)
        assertNotNull(bufferManager.getCurrentPhoto())
        assertEquals(testPhotos[0], bufferManager.getCurrentPhotoMetadata())
    }

    @Test
    fun `getNextPhoto advances to next photo`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(testPhotos, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = bufferManager.getNextPhoto()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Result.Success)
        assertEquals(testPhotos[1], bufferManager.getCurrentPhotoMetadata())
    }

    @Test
    fun `getNextPhoto wraps around to first photo`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(testPhotos, startIndex = testPhotos.size - 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = bufferManager.getNextPhoto()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Result.Success)
        assertEquals(testPhotos[0], bufferManager.getCurrentPhotoMetadata())
    }

    @Test
    fun `getPreviousPhoto goes to previous photo`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(testPhotos, startIndex = 2)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = bufferManager.getPreviousPhoto()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Result.Success)
        assertEquals(testPhotos[1], bufferManager.getCurrentPhotoMetadata())
    }

    @Test
    fun `getPreviousPhoto wraps around to last photo`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(testPhotos, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = bufferManager.getPreviousPhoto()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result is Result.Success)
        assertEquals(testPhotos[testPhotos.size - 1], bufferManager.getCurrentPhotoMetadata())
    }

    @Test
    fun `buffer size never exceeds 4 photos`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(testPhotos, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Navigate through several photos
        repeat(10) {
            bufferManager.getNextPhoto()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val bufferSize = bufferManager.getBufferSize()
        assertTrue(bufferSize <= PhotoBufferManager.BUFFER_SIZE)
    }

    @Test
    fun `clear removes all photos from buffer`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(testPhotos, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        bufferManager.clear()

        assertNull(bufferManager.getCurrentPhoto())
        assertNull(bufferManager.getCurrentPhotoMetadata())
        assertEquals(0, bufferManager.getBufferSize())
    }

    @Test
    fun `buffer pre-loads next photos in background`() = runTest {
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(testPhotos, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that multiple photos are loaded (current + pre-loaded)
        coVerify(atLeast = 2) { imageCache.load(any()) }
    }

    @Test
    fun `handles image load failure gracefully`() = runTest {
        coEvery { imageCache.load(testPhotos[0].path) } returns Result.success(testBitmap)
        coEvery { imageCache.load(testPhotos[1].path) } returns Result.error(
            Exception("Failed to load"),
            "Load error"
        )

        bufferManager.initialize(testPhotos, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        // First photo should load successfully
        assertNotNull(bufferManager.getCurrentPhoto())

        // Next photo load should fail but return error, not crash
        val result = bufferManager.getNextPhoto()
        assertTrue(result is Result.Error)
    }

    @Test
    fun `single photo list works correctly`() = runTest {
        val singlePhoto = listOf(testPhotos[0])
        coEvery { imageCache.load(any()) } returns Result.success(testBitmap)

        bufferManager.initialize(singlePhoto, startIndex = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Next should wrap to same photo
        bufferManager.getNextPhoto()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(singlePhoto[0], bufferManager.getCurrentPhotoMetadata())

        // Previous should wrap to same photo
        bufferManager.getPreviousPhoto()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(singlePhoto[0], bufferManager.getCurrentPhotoMetadata())
    }

    @Test
    fun `loading state transitions correctly`() = runTest {
        coEvery { imageCache.load(any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(testBitmap)
        }

        bufferManager.initialize(testPhotos, startIndex = 0)

        // Should start as Loading
        var currentState = bufferManager.loadingState.value
        assertTrue(currentState is BufferLoadingState.Loading)

        testDispatcher.scheduler.advanceUntilIdle()

        // Should become Ready after loading
        currentState = bufferManager.loadingState.value
        assertTrue(currentState is BufferLoadingState.Ready)
    }
}
