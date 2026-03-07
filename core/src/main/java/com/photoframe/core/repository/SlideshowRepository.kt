package com.photoframe.core.repository

import android.graphics.Bitmap
import com.photoframe.core.model.Photo
import com.photoframe.core.model.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for slideshow photo management.
 *
 * Provides methods for:
 * - Loading photos from SMB share
 * - Shuffling photo order
 * - Navigating through photos (next/previous)
 * - Managing photo buffer
 *
 * Integration Points:
 * - SmbPhotoDataSource (Phase 2): Scans SMB share for photos
 * - PhotoBufferManager (Phase 3): Manages 4-photo buffer
 * - ImageCache (Phase 3): Loads and caches photo bitmaps
 *
 * Thread Safety: All methods are thread-safe and can be called from any coroutine.
 *
 * State Management: Exposes reactive StateFlows for UI observation.
 */
interface SlideshowRepository {
    /**
     * StateFlow of current photo list.
     * Emits empty list if no photos loaded.
     */
    val photos: StateFlow<List<Photo>>

    /**
     * StateFlow of current photo bitmap.
     * Emits null if no photo is currently loaded.
     */
    val currentPhoto: StateFlow<Bitmap?>

    /**
     * StateFlow of loading state.
     * Indicates whether photos are being loaded from SMB.
     */
    val isLoading: StateFlow<Boolean>

    /**
     * StateFlow of error state.
     * Emits error message if loading failed, null otherwise.
     */
    val error: StateFlow<String?>

    /**
     * Loads photos from the configured SMB share.
     * Initializes the photo buffer with loaded photos.
     *
     * Retry Logic: Automatically retries on failure with exponential backoff.
     * - Retry 1: After 2 seconds
     * - Retry 2: After 4 seconds
     * - Retry 3: After 8 seconds
     * - Max retries: 3
     *
     * Thread Safety: Safe to call concurrently. Cancels any in-progress loads.
     *
     * @param shuffleEnabled If true, shuffles photos after loading
     * @return Result.Success with photo count, Result.Error if loading failed
     */
    suspend fun loadPhotos(shuffleEnabled: Boolean = false): Result<Int>

    /**
     * Shuffles the current photo list using Fisher-Yates algorithm.
     * Maintains current photo position (doesn't change displayed photo).
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Result.Success with shuffled count, Result.Error if no photos loaded
     */
    suspend fun shufflePhotos(): Result<Int>

    /**
     * Advances to the next photo in the list.
     * Wraps around to the beginning if at the end.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Result.Success with next photo bitmap, Result.Error if failed
     */
    suspend fun nextPhoto(): Result<Bitmap>

    /**
     * Goes back to the previous photo in the list.
     * Wraps around to the end if at the beginning.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Result.Success with previous photo bitmap, Result.Error if failed
     */
    suspend fun previousPhoto(): Result<Bitmap>

    /**
     * Gets the current photo metadata.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Current photo metadata, or null if no photo loaded
     */
    suspend fun getCurrentPhotoMetadata(): Photo?

    /**
     * Gets the current photo index in the list.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Current index (0-based), or -1 if no photos loaded
     */
    suspend fun getCurrentPhotoIndex(): Int

    /**
     * Clears the photo list and buffer.
     * Resets repository to initial state.
     *
     * Thread Safety: Safe to call concurrently.
     */
    suspend fun clear()
}
