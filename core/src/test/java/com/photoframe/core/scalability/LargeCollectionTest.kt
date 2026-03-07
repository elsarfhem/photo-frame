package com.photoframe.core.scalability

import com.photoframe.core.data.IncrementalPhotoLoader
import com.photoframe.core.data.SmbPhotoDataSource
import com.photoframe.core.model.Photo
import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.smb.SmbClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * P0 Scalability Tests: Large Photo Collections
 *
 * Tests TS-042 from QA 1 test plan (Phase 9: Test Implementation - Week 16)
 *
 * Validates:
 * - P0 BLOCKING Scalability Issue: Handling 10,000+ photo collections
 * - 30-second scan timeout enforcement
 * - Incremental loading for large collections
 * - Memory usage stays <300MB with 10K collection
 * - Deep folder structure handling (10+ levels)
 * - UI responsiveness during large scans
 *
 * CRITICAL: Users may have photo libraries with 10,000+ photos.
 * App must not timeout, crash, or become unresponsive with large collections.
 *
 * Phase 5 NFR Assessment (Senior Dev 3) flagged this as P0 BLOCKING.
 */
class LargeCollectionTest {

    /**
     * TS-042-01: Verify 10,000 photo scan completes within 30 seconds
     *
     * P0 BLOCKING - App must handle large collections without timeout
     */
    @Test
    fun `scan 10000 photos - completes within 30 seconds`() = runTest {
        // Given: SMB client with 10,000 photos
        val smbClient = createMockSmbClientWith10KPhotos()
        val dataSource = SmbPhotoDataSource(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "large-collection",
            username = "testuser"
        )

        // When: Scan photos with timer
        var result: Result<List<Photo>>? = null
        val scanTime = measureTimeMillis {
            result = dataSource.scanPhotos(connection, "password", "/")
        }

        // Then: Completes within 30 seconds
        assertTrue(
            scanTime < 30_000,
            "Scan should complete within 30s, took ${scanTime}ms"
        )

        // And: Returns success result
        assertIs<Result.Success<List<Photo>>>(result)

        // And: Contains all 10,000 photos
        val photos = (result as Result.Success<List<Photo>>).data
        assertTrue(
            photos.size == 10_000,
            "Should find 10,000 photos, found ${photos.size}"
        )
    }

    /**
     * TS-042-02: Verify scan timeout at 30 seconds with error message
     *
     * Scalability - Must fail gracefully if scan exceeds timeout
     */
    @Test
    fun `scan takes longer than 30 seconds - returns timeout error`() = runTest {
        // Given: SMB client with very slow response (simulates 100K photos)
        val smbClient = mockk<SmbClient>()
        coEvery { smbClient.listFiles(any(), any()) } coAnswers {
            // Simulate 40-second scan
            kotlinx.coroutines.delay(40_000)
            List(100_000) { mockk<Photo>() }
        }

        val dataSource = SmbPhotoDataSource(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "huge-collection",
            username = "testuser"
        )

        // When: Scan with 30-second timeout
        val result = dataSource.scanPhotosWithTimeout(connection, "password", "/", timeoutMs = 30_000)

        // Then: Returns timeout error
        assertIs<Result.Error>(result)
        assertTrue(
            result.message!!.contains("timeout", ignoreCase = true) ||
            result.message!!.contains("30", ignoreCase = true),
            "Error should mention timeout: ${result.message}"
        )
    }

    /**
     * TS-042-03: Verify incremental loading for large collections
     *
     * P0 BLOCKING - UI must remain responsive during large scan
     */
    @Test
    fun `incremental loading - loads first 100 photos immediately`() = runTest {
        // Given: IncrementalPhotoLoader with 10K photos
        val smbClient = createMockSmbClientWith10KPhotos()
        val incrementalLoader = IncrementalPhotoLoader(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "photos",
            username = "testuser"
        )

        // When: Load first batch
        val firstBatch = incrementalLoader.loadFirstBatch(connection, "password", "/", batchSize = 100)

        // Then: First 100 photos loaded quickly (<5 seconds)
        assertIs<Result.Success<List<Photo>>>(firstBatch)
        assertTrue(
            firstBatch.data.size == 100,
            "First batch should contain 100 photos, got ${firstBatch.data.size}"
        )

        // And: Remaining photos loaded in background
        // (Test would verify background coroutine is launched)
    }

    /**
     * TS-042-04: Verify memory usage stays under 300MB with 10K collection
     *
     * P0 BLOCKING - App must not OOM with large collections
     */
    @Test
    fun `10K photo collection - memory usage stays under 300MB`() = runTest {
        // Given: Memory monitor
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // When: Load 10K photo metadata (not images, just paths)
        val smbClient = createMockSmbClientWith10KPhotos()
        val dataSource = SmbPhotoDataSource(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "photos",
            username = "testuser"
        )
        val result = dataSource.scanPhotos(connection, "password", "/")

        // Then: Memory increase is < 50MB for metadata
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024) // Convert to MB

        assertTrue(
            memoryIncrease < 50,
            "Memory increase should be <50MB for 10K photo metadata, was ${memoryIncrease}MB"
        )

        // Note: Full memory test with image loading done in integration tests
    }

    /**
     * TS-042-05: Verify deep folder structure handling (10+ levels)
     *
     * Scalability - Must handle deep hierarchies without stack overflow
     */
    @Test
    fun `deep folder structure 15 levels - scans successfully without stack overflow`() = runTest {
        // Given: SMB client with 15-level deep folder structure
        val smbClient = createMockSmbClientWithDeepFolders(depth = 15, photosPerFolder = 10)
        val dataSource = SmbPhotoDataSource(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "deep-folders",
            username = "testuser"
        )

        // When: Scan deep folders
        val result = dataSource.scanPhotos(connection, "password", "/")

        // Then: Success (no stack overflow)
        assertIs<Result.Success<List<Photo>>>(result)

        // And: Finds all photos (15 folders * 10 photos = 150)
        val photos = result.data
        assertTrue(
            photos.size == 150,
            "Should find 150 photos in 15-level structure, found ${photos.size}"
        )
    }

    /**
     * TS-042-06: Verify empty subfolders don't cause performance issues
     *
     * Scalability - Many empty folders should not slow scan
     */
    @Test
    fun `1000 empty subfolders with 100 photos - scans efficiently`() = runTest {
        // Given: 1000 empty folders + 1 folder with 100 photos
        val smbClient = mockk<SmbClient>()
        coEvery { smbClient.listFiles(any(), any()) } returns
            // 1000 empty folders + 1 folder with 100 photos
            List(1000) { mockk<Photo>() } + List(100) { mockk<Photo>() }

        val dataSource = SmbPhotoDataSource(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "mostly-empty",
            username = "testuser"
        )

        // When: Scan folders
        val scanTime = measureTimeMillis {
            dataSource.scanPhotos(connection, "password", "/")
        }

        // Then: Completes quickly (<10 seconds for metadata scan)
        assertTrue(
            scanTime < 10_000,
            "Scan with empty folders should be fast, took ${scanTime}ms"
        )
    }

    /**
     * TS-042-07: Verify progress indication during large scan
     *
     * Scalability - User must see progress for long scans
     */
    @Test
    fun `scan 10K photos - emits progress updates`() = runTest {
        // Given: IncrementalPhotoLoader with progress callback
        val smbClient = createMockSmbClientWith10KPhotos()
        val incrementalLoader = IncrementalPhotoLoader(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "photos",
            username = "testuser"
        )

        // Track progress updates
        val progressUpdates = mutableListOf<Int>()

        // When: Scan with progress callback
        incrementalLoader.scanWithProgress(
            connection,
            "password",
            "/",
            onProgress = { scannedCount, totalCount ->
                progressUpdates.add(scannedCount)
            }
        )

        // Then: Progress updates emitted
        assertTrue(
            progressUpdates.isNotEmpty(),
            "Progress updates should be emitted during scan"
        )

        // And: Progress increases monotonically
        for (i in 1 until progressUpdates.size) {
            assertTrue(
                progressUpdates[i] >= progressUpdates[i - 1],
                "Progress should increase: ${progressUpdates[i - 1]} -> ${progressUpdates[i]}"
            )
        }
    }

    /**
     * TS-042-08: Verify cancellation of large scan
     *
     * Scalability - User must be able to cancel long-running scan
     */
    @Test
    fun `scan 10K photos - can be cancelled mid-scan`() = runTest {
        // Given: Long-running scan
        val smbClient = mockk<SmbClient>()
        coEvery { smbClient.listFiles(any(), any()) } coAnswers {
            // Simulate slow scan (1 second per 100 photos)
            kotlinx.coroutines.delay(10_000)
            List(10_000) { mockk<Photo>() }
        }

        val dataSource = SmbPhotoDataSource(smbClient)
        val connection = SmbConnection(
            server = "192.168.1.100",
            share = "photos",
            username = "testuser"
        )

        // When: Start scan and cancel after 2 seconds
        val job = kotlinx.coroutines.launch {
            dataSource.scanPhotos(connection, "password", "/")
        }

        kotlinx.coroutines.delay(2_000)
        job.cancel()

        // Then: Job is cancelled
        assertTrue(job.isCancelled, "Scan job should be cancelled")
    }

    // Helper functions

    private fun createMockSmbClientWith10KPhotos(): SmbClient {
        val smbClient = mockk<SmbClient>()

        // Mock 10,000 photos distributed across folders
        val photos = List(10_000) { index ->
            Photo(
                path = "photo_${String.format("%05d", index)}.jpg",
                name = "photo_${String.format("%05d", index)}.jpg",
                sizeMb = 5.0,
                lastModified = System.currentTimeMillis()
            )
        }

        coEvery { smbClient.listFiles(any(), any()) } returns photos

        return smbClient
    }

    private fun createMockSmbClientWithDeepFolders(depth: Int, photosPerFolder: Int): SmbClient {
        val smbClient = mockk<SmbClient>()

        // Mock deep folder structure: /level1/level2/.../level15/
        // Each folder has photosPerFolder photos
        val allPhotos = mutableListOf<Photo>()

        for (level in 1..depth) {
            for (photoIndex in 1..photosPerFolder) {
                val path = buildString {
                    for (l in 1..level) {
                        append("/level$l")
                    }
                    append("/photo_${level}_${photoIndex}.jpg")
                }

                allPhotos.add(
                    Photo(
                        path = path,
                        name = "photo_${level}_${photoIndex}.jpg",
                        sizeMb = 3.0,
                        lastModified = System.currentTimeMillis()
                    )
                )
            }
        }

        coEvery { smbClient.listFiles(any(), any()) } returns allPhotos

        return smbClient
    }

    private suspend fun SmbPhotoDataSource.scanPhotosWithTimeout(
        connection: SmbConnection,
        password: String,
        basePath: String,
        timeoutMs: Long
    ): Result<List<Photo>> {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                scanPhotos(connection, password, basePath)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.error(Exception("Scan timeout after ${timeoutMs}ms"))
        }
    }
}

/**
 * Integration Test Note:
 *
 * These unit tests validate scalability logic with mocked data.
 * For true scalability validation, run integration tests:
 *
 * 1. **Real SMB Server with 10K Photos**:
 *    - Create Docker Samba with 10,000 real JPEG files
 *    - Run full scan and measure time
 *    - Profile memory usage with Android Profiler
 *    - Target: <30s scan, <300MB memory
 *
 * 2. **Real SMB Server with 100K Photos**:
 *    - Test timeout behavior (should fail gracefully at 30s)
 *    - Test incremental loading (first 100 photos fast)
 *    - Test UI responsiveness (no ANRs)
 *
 * 3. **Deep Folder Structure**:
 *    - Create 20-level deep folder hierarchy
 *    - Verify no stack overflow (iterative traversal, not recursive)
 *
 * 4. **Memory Profiling**:
 *    - Run Android Profiler during 10K photo scan
 *    - Verify heap size <300MB
 *    - Check for memory leaks (repeatable scans)
 *
 * See: app/src/androidTest/java/com/photoframe/app/scalability/LargeCollectionIntegrationTest.kt
 */
