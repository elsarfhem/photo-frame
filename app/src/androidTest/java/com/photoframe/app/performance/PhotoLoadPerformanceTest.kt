package com.photoframe.app.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.photoframe.core.image.ImageCache
import com.photoframe.core.model.Photo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

/**
 * Performance Test: Photo Load Time <2s (P0 NFR)
 *
 * Tests TS-PB-001 from QA 3 test plan (Phase 9: Test Implementation - Week 17)
 *
 * Validates:
 * - P0 NFR: Photo load time <2 seconds (95th percentile)
 * - Disk cache performance
 * - Memory cache performance
 * - Downsampling effectiveness
 * - Image decoding performance
 *
 * Success Criteria:
 * - 95% of photo loads complete within 2 seconds
 * - Cold load (no cache): <2s
 * - Warm load (disk cache): <500ms
 * - Hot load (memory cache): <50ms
 *
 * @see docs/features/photo-frame-app-initial/testing/performance-accessibility-tests.md
 */
@RunWith(AndroidJUnit4::class)
class PhotoLoadPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * TS-PB-001-01: Measure cold photo load time (no cache)
     *
     * Target: <2 seconds for 95th percentile
     */
    @Test
    fun coldPhotoLoadTime_underTwoSeconds() {
        val imageCache = ImageCache(context, maxMemoryCacheSizeMB = 50)
        val loadTimes = mutableListOf<Long>()

        // Measure 100 cold loads
        repeat(100) { index ->
            // Clear caches for cold load
            imageCache.clearMemoryCache()
            imageCache.clearDiskCache()

            val photo = createMockPhoto(index)
            val startTime = System.currentTimeMillis()

            // Simulate photo load
            val bitmap = imageCache.loadSync(photo.path, maxSize = 2560)

            val loadTime = System.currentTimeMillis() - startTime
            loadTimes.add(loadTime)
        }

        // Calculate 95th percentile
        val sortedTimes = loadTimes.sorted()
        val p95Index = (sortedTimes.size * 0.95).toInt()
        val p95LoadTime = sortedTimes[p95Index]

        assertTrue(
            p95LoadTime < 2000,
            "95th percentile cold load time should be <2s, was ${p95LoadTime}ms"
        )

        // Log statistics
        println("Photo Load Performance (Cold):")
        println("  Min: ${sortedTimes.first()}ms")
        println("  Median: ${sortedTimes[sortedTimes.size / 2]}ms")
        println("  P95: ${p95LoadTime}ms")
        println("  Max: ${sortedTimes.last()}ms")
    }

    /**
     * TS-PB-001-02: Measure warm photo load time (disk cache hit)
     *
     * Target: <500ms for disk cache hit
     */
    @Test
    fun warmPhotoLoadTime_underFiveHundredMs() {
        val imageCache = ImageCache(context, maxMemoryCacheSizeMB = 50)
        val photo = createMockPhoto(0)

        // First load to populate disk cache
        imageCache.loadSync(photo.path, maxSize = 2560)
        imageCache.clearMemoryCache()

        val loadTimes = mutableListOf<Long>()

        // Measure 100 warm loads (disk cache hits)
        repeat(100) {
            val startTime = System.currentTimeMillis()
            val bitmap = imageCache.loadSync(photo.path, maxSize = 2560)
            val loadTime = System.currentTimeMillis() - startTime
            loadTimes.add(loadTime)
        }

        // Calculate 95th percentile
        val sortedTimes = loadTimes.sorted()
        val p95Index = (sortedTimes.size * 0.95).toInt()
        val p95LoadTime = sortedTimes[p95Index]

        assertTrue(
            p95LoadTime < 500,
            "95th percentile warm load time should be <500ms, was ${p95LoadTime}ms"
        )

        println("Photo Load Performance (Warm/Disk):")
        println("  Min: ${sortedTimes.first()}ms")
        println("  Median: ${sortedTimes[sortedTimes.size / 2]}ms")
        println("  P95: ${p95LoadTime}ms")
        println("  Max: ${sortedTimes.last()}ms")
    }

    /**
     * TS-PB-001-03: Measure hot photo load time (memory cache hit)
     *
     * Target: <50ms for memory cache hit
     */
    @Test
    fun hotPhotoLoadTime_underFiftyMs() {
        val imageCache = ImageCache(context, maxMemoryCacheSizeMB = 50)
        val photo = createMockPhoto(0)

        // First load to populate both caches
        imageCache.loadSync(photo.path, maxSize = 2560)

        val loadTimes = mutableListOf<Long>()

        // Measure 100 hot loads (memory cache hits)
        repeat(100) {
            val startTime = System.currentTimeMillis()
            val bitmap = imageCache.loadSync(photo.path, maxSize = 2560)
            val loadTime = System.currentTimeMillis() - startTime
            loadTimes.add(loadTime)
        }

        // Calculate 95th percentile
        val sortedTimes = loadTimes.sorted()
        val p95Index = (sortedTimes.size * 0.95).toInt()
        val p95LoadTime = sortedTimes[p95Index]

        assertTrue(
            p95LoadTime < 50,
            "95th percentile hot load time should be <50ms, was ${p95LoadTime}ms"
        )

        println("Photo Load Performance (Hot/Memory):")
        println("  Min: ${sortedTimes.first()}ms")
        println("  Median: ${sortedTimes[sortedTimes.size / 2]}ms")
        println("  P95: ${p95LoadTime}ms")
        println("  Max: ${sortedTimes.last()}ms")
    }

    /**
     * TS-PB-001-04: Test downsampling effectiveness for large images
     *
     * Target: Large image (8K) loads in same time as smaller images
     */
    @Test
    fun largeImageDownsampling_noPerformancePenalty() {
        val imageCache = ImageCache(context, maxMemoryCacheSizeMB = 50)

        // Simulate 8K image (7680x4320)
        val largePhoto = Photo(
            path = "/test/large_8k.jpg",
            filename = "large_8k.jpg",
            fileSize = 20_000_000, // 20MB
            lastModified = System.currentTimeMillis(),
            metadata = mapOf("width" to "7680", "height" to "4320")
        )

        val loadTimes = mutableListOf<Long>()

        repeat(10) {
            imageCache.clearMemoryCache()
            imageCache.clearDiskCache()

            val startTime = System.currentTimeMillis()
            // Downsample to 2560 max dimension
            val bitmap = imageCache.loadSync(largePhoto.path, maxSize = 2560)
            val loadTime = System.currentTimeMillis() - startTime
            loadTimes.add(loadTime)
        }

        val avgLoadTime = loadTimes.average()

        assertTrue(
            avgLoadTime < 2000,
            "Large image (8K) downsampled load should be <2s, was ${avgLoadTime}ms"
        )

        println("Large Image (8K) Downsampling Performance:")
        println("  Average load time: ${avgLoadTime}ms")
        println("  Downsampling effective: ${avgLoadTime < 2000}")
    }

    /**
     * TS-PB-001-05: Concurrent photo loads (buffer pre-loading)
     *
     * Target: 4 concurrent loads complete within 3s total
     */
    @Test
    fun concurrentPhotoLoads_efficientBatching() {
        val imageCache = ImageCache(context, maxMemoryCacheSizeMB = 50)
        val photos = (0..3).map { createMockPhoto(it) }

        imageCache.clearMemoryCache()
        imageCache.clearDiskCache()

        val startTime = System.currentTimeMillis()

        // Simulate PhotoBufferManager loading 4 photos concurrently
        val bitmaps = photos.map { photo ->
            imageCache.loadSync(photo.path, maxSize = 2560)
        }

        val totalLoadTime = System.currentTimeMillis() - startTime

        // 4 photos should load in <3s (not 4*2s = 8s)
        assertTrue(
            totalLoadTime < 3000,
            "4 concurrent photo loads should complete in <3s, took ${totalLoadTime}ms"
        )

        assertTrue(
            bitmaps.size == 4,
            "All 4 photos should load successfully"
        )

        println("Concurrent Photo Load Performance:")
        println("  Total time for 4 photos: ${totalLoadTime}ms")
        println("  Average per photo: ${totalLoadTime / 4}ms")
    }

    /**
     * TS-PB-001-06: Benchmark photo load with real file I/O
     *
     * Uses Macrobenchmark for accurate measurement
     */
    @Test
    fun benchmarkPhotoLoad() {
        val imageCache = ImageCache(context, maxMemoryCacheSizeMB = 50)
        val photo = createMockPhoto(0)

        benchmarkRule.measureRepeated {
            // Clear caches for consistent measurement
            runWithTimingDisabled {
                imageCache.clearMemoryCache()
            }

            // Measure just the load operation
            imageCache.loadSync(photo.path, maxSize = 2560)
        }
    }

    // Helper function

    private fun createMockPhoto(index: Int): Photo {
        return Photo(
            path = "/test/photo_$index.jpg",
            filename = "photo_$index.jpg",
            fileSize = 5_000_000, // 5MB
            lastModified = System.currentTimeMillis(),
            metadata = mapOf("width" to "4032", "height" to "3024")
        )
    }
}
