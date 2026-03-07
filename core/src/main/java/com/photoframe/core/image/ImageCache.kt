package com.photoframe.core.image

import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.decode.BitmapFactoryDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Result
import com.photoframe.core.smb.SmbClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Coil ImageLoader configured for photo frame requirements.
     */
    private val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            // Memory cache configuration (50MB)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(MEMORY_CACHE_SIZE_BYTES.toInt())
                    .build()
            }
            // Disk cache configuration (100MB)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_SIZE_BYTES)
                    .build()
            }
            // Custom components: SMB fetcher and bitmap decoder
            .components {
                add(SmbFetcher.Factory(smbClient, ioDispatcher))
                add(BitmapFactoryDecoder.Factory())
            }
            // Respect cache headers (no network checks)
            .respectCacheHeaders(false)
            .build()
    }

    /**
     * Loads a photo from the given path.
     *
     * Supports:
     * - SMB URLs (smb://server/share/path/photo.jpg)
     * - Local file paths (file:///path/photo.jpg)
     * - HTTP URLs (http://example.com/photo.jpg) - for future use
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
            // Build image request with downsampling
            val request = ImageRequest.Builder(context)
                .data(path)
                // Downsample to screen resolution (2560x1600)
                .size(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
                // Allow hardware bitmaps for better performance
                .allowHardware(true)
                // Use RGB_565 for memory efficiency (Phase 1), upgrade to ARGB_8888 later if needed
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()

            // Execute request
            when (val result = imageLoader.execute(request)) {
                is SuccessResult -> {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        Result.success(bitmap)
                    } else {
                        Result.error(
                            IllegalStateException("Failed to extract bitmap from drawable"),
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
        } catch (e: OutOfMemoryError) {
            // Handle OOM gracefully
            Result.error(
                e,
                "Out of memory while loading image. Image may be too large."
            )
        } catch (e: Exception) {
            Result.error(e, "Failed to load image: ${e.message}")
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
