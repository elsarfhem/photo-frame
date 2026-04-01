package com.photoframe.core.image

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.logging.AppLogger
import com.photoframe.core.model.Result
import com.photoframe.core.smb.SmbClient
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Image loading and caching layer using Coil.
 *
 * Architecture: Wraps Coil ImageLoader with custom configuration for SMB support.
 *
 * Configuration (per ADR):
 * - Memory cache: 50MB
 * - Disk cache: 100MB
 * - Downsampling: 2560x1600 maximum resolution
 * - Custom Fetcher: SMB URL support via SmbClient
 *
 * Performance:
 * - Hardware-accelerated decoding
 * - Aggressive downsampling to reduce memory (3x reduction from 4K)
 * - LRU eviction for both memory and disk caches
 *
 * Thread Safety: All methods are thread-safe. Coil handles internal synchronization.
 *
 * @param context Application context for Coil initialization
 * @param smbClient SMB client for fetching photos from network shares
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
@Singleton
class ImageCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smbClient: SmbClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appLogger: AppLogger
) {
    /**
     * Coil ImageLoader configured for photo frame requirements.
     */
    private val imageLoader: ImageLoader by lazy {
        // One-time disk cache clear on Coil 3 migration to avoid format incompatibilities
        clearDiskCacheOnMigration()

        ImageLoader.Builder(context)
            // Memory cache configuration (50MB)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(MEMORY_CACHE_SIZE_BYTES)
                    .build()
            }
            // Disk cache configuration (100MB)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(DISK_CACHE_SIZE_BYTES)
                    .build()
            }
            // Custom components: SMB fetcher
            .components {
                add(SmbFetcher.Factory(smbClient, ioDispatcher))
            }
            .build()
    }

    /**
     * One-time disk cache clear when migrating from Coil 2 to Coil 3.
     * Uses SharedPreferences version flag to only run once.
     */
    private fun clearDiskCacheOnMigration() {
        val prefs = context.getSharedPreferences("coil_migration", Context.MODE_PRIVATE)
        val currentVersion = 3
        val storedVersion = prefs.getInt("coil_cache_version", 0)
        if (storedVersion < currentVersion) {
            try {
                val cacheDir = context.cacheDir.resolve("image_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            } catch (e: Exception) {
                try { FirebaseCrashlytics.getInstance().recordException(e) } catch (_: Exception) {}
            }
            prefs.edit().putInt("coil_cache_version", currentVersion).apply()
        }
    }

    /**
     * Loads a photo from the given path.
     *
     * Supports:
     * - SMB URLs (smb://server/share/path/photo.jpg)
     * - Local file paths (file:///path/photo.jpg)
     * - HTTP URLs (http://example.com/photo.jpg) - for future use
     * - RAW files (DNG, CR2, NEF, RW2, ARW) - via RawImageDecoder
     *
     * Thread Safety: Safe to call concurrently from multiple coroutines.
     *
     * Performance:
     * - Downsamples to MAX_IMAGE_WIDTH x MAX_IMAGE_HEIGHT
     * - Uses Coil's memory and disk cache
     * - Returns ARGB_8888 bitmap for quality
     *
     * @param path Full path to the photo (SMB, file, or HTTP URL)
     * @return Result.Success with bitmap, Result.Error if loading failed
     */
    suspend fun load(path: String): Result<Bitmap> = withContext(ioDispatcher) {
        return@withContext try {
            // Check if this is a RAW file
            val extension = path.substringAfterLast('.', "")
            if (RawImageDecoder.isRawFormat(extension)) {
                // Load RAW file using custom decoder
                loadRawImage(path, extension)
            } else {
                // Use Coil for standard formats
                loadStandardImage(path)
            }
        } catch (e: OutOfMemoryError) {
            // Log OOM to persistent log and Crashlytics
            appLogger.log("OOM", "path=$path")
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("oom_photo_path", path)
                log("OOM while loading image: $path")
                recordException(e)
            }
            Result.error(
                e,
                "Out of memory while loading image. Image may be too large."
            )
        } catch (e: Exception) {
            Result.error(e, "Failed to load image: ${e.message}")
        }
    }

    /**
     * Loads standard image formats using Coil.
     */
    private suspend fun loadStandardImage(path: String): Result<Bitmap> {
        // Build image request with downsampling
        val request = ImageRequest.Builder(context)
            .data(path)
            // Downsample to screen resolution (2560x1600)
            .size(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
            // Hardware bitmaps: stored in GPU memory, outside Java heap (reduces GC pressure).
            // Safe because Coil applies EXIF rotation DURING decode (before bitmap creation).
            .allowHardware(true)
            // Use ARGB_8888 for quality
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build()

        // Execute request
        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> {
                try {
                    val bitmap = result.image.toBitmap()
                    Result.success(bitmap)
                } catch (e: Exception) {
                    try { FirebaseCrashlytics.getInstance().recordException(e) } catch (_: Exception) {}
                    Result.error(
                        IllegalStateException("Failed to extract bitmap from Coil 3 Image", e),
                        "Image loaded but could not extract bitmap"
                    )
                }
            }
            is ErrorResult -> {
                Result.error(
                    result.throwable,
                    "Failed to load image: ${result.throwable.message}"
                )
            }
        }
    }

    /**
     * Loads RAW image formats using RawImageDecoder.
     */
    private suspend fun loadRawImage(path: String, extension: String): Result<Bitmap> {
        return try {
            // Read RAW file bytes from SMB
            val bytesResult = smbClient.readFile(path)
            when (bytesResult) {
                is Result.Success -> {
                    // Decode RAW bytes to bitmap
                    RawImageDecoder.decode(bytesResult.data, extension)
                }
                is Result.Error -> {
                    Result.error(
                        bytesResult.exception,
                        "Failed to read RAW file: ${bytesResult.message}"
                    )
                }
                is Result.Loading -> {
                    Result.error(
                        IllegalStateException("Unexpected loading state"),
                        "SMB client returned loading state"
                    )
                }
            }
        } catch (e: Exception) {
            Result.error(e, "Failed to load RAW image: ${e.message}")
        }
    }

    /**
     * Pre-warms the cache by loading an image without returning it.
     * Useful for background pre-loading.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param path Full path to the photo
     * @return Result.Success if pre-loaded, Result.Error if failed
     */
    suspend fun prewarm(path: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            val request = ImageRequest.Builder(context)
                .data(path)
                .size(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
                .allowHardware(true)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()

            when (val result = imageLoader.execute(request)) {
                is SuccessResult -> Result.success(Unit)
                is ErrorResult -> Result.error(
                    result.throwable,
                    "Failed to prewarm image: ${result.throwable.message}"
                )
            }
        } catch (e: Exception) {
            Result.error(e, "Failed to prewarm image: ${e.message}")
        }
    }

    /**
     * Clears the memory cache.
     * Disk cache is preserved for offline use.
     *
     * Thread Safety: Safe to call concurrently.
     */
    fun clearMemoryCache() {
        imageLoader.memoryCache?.clear()
    }

    /**
     * Clears both memory and disk caches.
     * Use sparingly - disk cache improves performance on repeated scans.
     *
     * Thread Safety: Safe to call concurrently.
     */
    suspend fun clearAllCaches() {
        withContext(ioDispatcher) {
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        }
    }

    /**
     * Gets the current memory cache size in bytes.
     *
     * @return Memory cache size, or 0 if unavailable
     */
    fun getMemoryCacheSize(): Long {
        return imageLoader.memoryCache?.size?.toLong() ?: 0L
    }

    /**
     * Gets the current disk cache size in bytes.
     *
     * @return Disk cache size, or 0 if unavailable
     */
    suspend fun getDiskCacheSize(): Long = withContext(ioDispatcher) {
        return@withContext imageLoader.diskCache?.size ?: 0L
    }

    companion object {
        /**
         * Memory cache size: 50MB per ADR.
         * Holds ~3 downsampled photos in memory at 2560x1600 ARGB_8888.
         */
        private const val MEMORY_CACHE_SIZE_BYTES = 50 * 1024 * 1024L // 50MB

        /**
         * Disk cache size: 100MB per ADR.
         * Holds ~6 downsampled photos on disk.
         */
        private const val DISK_CACHE_SIZE_BYTES = 100 * 1024 * 1024L // 100MB

        /**
         * Maximum image width for downsampling: 2560px.
         * Matches target tablet resolution per ADR.
         */
        private const val MAX_IMAGE_WIDTH = 2560

        /**
         * Maximum image height for downsampling: 1600px.
         * Matches target tablet resolution per ADR.
         */
        private const val MAX_IMAGE_HEIGHT = 1600
    }
}
