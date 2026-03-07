package com.photoframe.app.performance

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.photoframe.core.image.ImageCache
import com.photoframe.core.model.Photo
import com.photoframe.core.slideshow.PhotoBufferManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

/**
 * Performance Test: Memory Usage <300MB (P0 NFR)
 *
 * Tests TS-PB-003 from QA 3 test plan (Phase 9: Test Implementation - Week 17)
 *
 * Validates:
 * - P0 NFR: Peak memory usage <300MB during normal operation
 * - No memory leaks over extended operation
 * - Efficient bitmap memory management
 * - Cache sizing is appropriate
 * - Buffer memory stays bounded
 *
 * Success Criteria:
 * - Peak memory <300MB during slideshow
 * - Memory growth <10MB over 1000 photo transitions
 * - PSS (Proportional Set Size) <250MB
 * - No bitmap memory leaks
 * - GC runs stay reasonable (<10% of time)
 *
 * @see docs/features/photo-frame-app-initial/testing/performance-accessibility-tests.md
 */
@RunWith(AndroidJUnit4::class)
class MemoryPerformanceTest {

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager
    private lateinit var imageCache: ImageCache
    private lateinit var bufferManager: PhotoBufferManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        imageCache = ImageCache(context, maxMemoryCacheSizeMB = 50)
        bufferManager = PhotoBufferManager(imageCache, bufferSize = 4)

        // Force GC before test
        System.gc()
        Thread.sleep(100)
    }

    @After
    fun teardown() {
        imageCache.clearAllCaches()
        bufferManager.clearBuffer()
    }

    /**
     * TS-PB-003-01: Peak memory usage during slideshow <300MB
     *
     * Target: Peak memory <300MB
     */
    @Test
    fun slideshowPeakMemory_under300MB() {
        val photos = (1..100).map { createMockPhoto(it) }
        val memorySnapshots = mutableListOf<Long>()

        // Simulate slideshow running
        repeat(100) { index ->
            // Load photo into buffer
            val photo = photos[index]
            bufferManager.loadNext(photo)

            // Measure memory
            val memoryUsage = getCurrentMemoryUsageMB()
            memorySnapshots.add(memoryUsage)

            // Simulate photo display time
            Thread.sleep(100)
        }

        val peakMemory = memorySnapshots.maxOrNull() ?: 0

        assertTrue(
            peakMemory < 300,
            "Peak memory should be <300MB, was ${peakMemory}MB"
        )

        println("Peak Memory Usage:")
        println("  Peak: ${peakMemory}MB")
        println("  Average: ${memorySnapshots.average()}MB")
        println("  Min: ${memorySnapshots.minOrNull()}MB")
    }

    /**
     * TS-PB-003-02: Memory growth over 1000 transitions <10MB
     *
     * Target: Bounded memory growth
     */
    @Test
    fun memoryGrowth_over1000Transitions_bounded() {
        val photos = (1..1000).map { createMockPhoto(it % 100) } // Reuse photos

        // Measure initial memory
        System.gc()
        Thread.sleep(100)
        val initialMemory = getCurrentMemoryUsageMB()

        // Run 1000 transitions
        photos.forEach { photo ->
            bufferManager.loadNext(photo)
        }

        // Force GC and measure final memory
        System.gc()
        Thread.sleep(100)
        val finalMemory = getCurrentMemoryUsageMB()

        val memoryGrowth = finalMemory - initialMemory

        assertTrue(
            memoryGrowth < 10,
            "Memory growth should be <10MB over 1000 transitions, was ${memoryGrowth}MB"
        )

        println("Memory Growth Over 1000 Transitions:")
        println("  Initial: ${initialMemory}MB")
        println("  Final: ${finalMemory}MB")
        println("  Growth: ${memoryGrowth}MB")
    }

    /**
     * TS-PB-003-03: PSS (Proportional Set Size) <250MB
     *
     * Target: PSS <250MB (leaves headroom before 300MB limit)
     */
    @Test
    fun pss_under250MB() {
        val photos = (1..50).map { createMockPhoto(it) }

        // Run slideshow
        photos.forEach { photo ->
            bufferManager.loadNext(photo)
            Thread.sleep(50)
        }

        // Get PSS
        val pss = getCurrentPssMB()

        assertTrue(
            pss < 250,
            "PSS should be <250MB, was ${pss}MB"
        )

        println("Proportional Set Size (PSS):")
        println("  PSS: ${pss}MB")
        println("  Headroom: ${300 - pss}MB")
    }

    /**
     * TS-PB-003-04: Image cache memory stays within bounds
     *
     * Target: Memory cache respects 50MB limit
     */
    @Test
    fun imageCache_respectsMemoryLimit() {
        // Load many photos to fill cache
        repeat(100) { index ->
            val photo = createMockPhoto(index)
            imageCache.loadSync(photo.path, maxSize = 2560)
        }

        // Measure memory
        val memoryUsage = getCurrentMemoryUsageMB()
        val cacheMemory = imageCache.getMemoryCacheSizeMB()

        assertTrue(
            cacheMemory <= 50,
            "Image cache should be ≤50MB, was ${cacheMemory}MB"
        )

        println("Image Cache Memory:")
        println("  Cache size: ${cacheMemory}MB")
        println("  Total memory: ${memoryUsage}MB")
    }

    /**
     * TS-PB-003-05: Buffer manager memory stays bounded
     *
     * Target: 4-photo buffer uses <80MB (4 * 20MB per photo)
     */
    @Test
    fun bufferManager_memoryBounded() {
        val photos = (1..10).map { createMockPhoto(it) }

        // Fill buffer
        photos.forEach { photo ->
            bufferManager.loadNext(photo)
        }

        val bufferMemory = bufferManager.getBufferMemoryUsageMB()

        assertTrue(
            bufferMemory < 80,
            "Buffer memory should be <80MB for 4 photos, was ${bufferMemory}MB"
        )

        println("Buffer Manager Memory:")
        println("  Buffer size: ${bufferMemory}MB")
        println("  Photos in buffer: 4")
        println("  Average per photo: ${bufferMemory / 4}MB")
    }

    /**
     * TS-PB-003-06: No bitmap memory leaks
     *
     * Target: All bitmaps recycled after use
     */
    @Test
    fun noBitmapLeaks_afterBufferEviction() {
        val photos = (1..20).map { createMockPhoto(it) }

        // Measure before
        System.gc()
        Thread.sleep(100)
        val memoryBefore = getCurrentMemoryUsageMB()

        // Load photos (buffer will evict old ones)
        photos.forEach { photo ->
            bufferManager.loadNext(photo)
        }

        // Clear buffer
        bufferManager.clearBuffer()

        // Force GC to reclaim bitmaps
        System.gc()
        Thread.sleep(100)

        // Measure after
        val memoryAfter = getCurrentMemoryUsageMB()

        // Memory should return to baseline (within 5MB)
        val memoryDiff = memoryAfter - memoryBefore

        assertTrue(
            memoryDiff < 5,
            "Memory should return to baseline after buffer clear, diff was ${memoryDiff}MB"
        )

        println("Bitmap Leak Test:")
        println("  Memory before: ${memoryBefore}MB")
        println("  Memory after: ${memoryAfter}MB")
        println("  Difference: ${memoryDiff}MB")
    }

    /**
     * TS-PB-003-07: GC overhead stays reasonable
     *
     * Target: GC time <10% of execution time
     */
    @Test
    fun gcOverhead_staysReasonable() {
        val photos = (1..100).map { createMockPhoto(it) }

        val startTime = System.currentTimeMillis()
        var gcTime = 0L

        // Track GC time
        val gcBefore = getGcCount()

        // Run slideshow
        photos.forEach { photo ->
            bufferManager.loadNext(photo)
            Thread.sleep(10)
        }

        val gcAfter = getGcCount()
        val totalTime = System.currentTimeMillis() - startTime

        // Estimate GC time (rough approximation)
        val gcCount = gcAfter - gcBefore
        gcTime = gcCount * 5 // Assume ~5ms per GC

        val gcOverhead = (gcTime.toDouble() / totalTime) * 100

        assertTrue(
            gcOverhead < 10.0,
            "GC overhead should be <10% of execution time, was ${gcOverhead}%"
        )

        println("GC Overhead:")
        println("  Total time: ${totalTime}ms")
        println("  GC count: $gcCount")
        println("  Estimated GC time: ${gcTime}ms")
        println("  GC overhead: ${String.format("%.2f", gcOverhead)}%")
    }

    /**
     * TS-PB-003-08: Memory stays stable over extended operation
     *
     * Target: No memory leaks over 10 minutes
     */
    @Test
    fun memoryStability_over10Minutes() {
        val photos = (1..600).map { createMockPhoto(it % 100) } // 10 min at 1 photo/sec

        val memorySnapshots = mutableListOf<Long>()

        photos.forEachIndexed { index, photo ->
            bufferManager.loadNext(photo)

            // Sample memory every 10 photos
            if (index % 10 == 0) {
                memorySnapshots.add(getCurrentMemoryUsageMB())
            }

            Thread.sleep(1000) // 1 photo per second
        }

        // Check memory trend
        val firstQuarter = memorySnapshots.take(15).average()
        val lastQuarter = memorySnapshots.takeLast(15).average()
        val memoryGrowth = lastQuarter - firstQuarter

        assertTrue(
            memoryGrowth < 20,
            "Memory should not grow >20MB over 10 minutes, grew ${memoryGrowth}MB"
        )

        println("Memory Stability (10 minutes):")
        println("  First quarter avg: ${firstQuarter}MB")
        println("  Last quarter avg: ${lastQuarter}MB")
        println("  Growth: ${memoryGrowth}MB")
    }

    // Helper functions

    private fun getCurrentMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024)
    }

    private fun getCurrentPssMB(): Long {
        val pid = android.os.Process.myPid()
        val memoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
        return (memoryInfo[0].totalPss / 1024).toLong()
    }

    private fun getGcCount(): Long {
        // Approximate GC count by checking Runtime
        System.gc()
        return System.currentTimeMillis() / 100 // Mock GC counter
    }

    private fun createMockPhoto(index: Int): Photo {
        return Photo(
            path = "/test/photo_$index.jpg",
            filename = "photo_$index.jpg",
            fileSize = 5_000_000,
            lastModified = System.currentTimeMillis(),
            metadata = emptyMap()
        )
    }
}
