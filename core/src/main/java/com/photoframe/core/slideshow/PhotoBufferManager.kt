package com.photoframe.core.slideshow

import android.graphics.Bitmap
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.image.ImageCache
import com.photoframe.core.model.Photo
import com.photoframe.core.model.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a 5-photo buffer for smooth slideshow playback.
 *
 * Architecture: Implements ADR Decision 3 - 5-photo buffer pattern
 * Buffer Layout: [Current - 1, Current, Current + 1, Current + 2, Current + 3]
 *
 * Thread Safety: All public methods are thread-safe using Mutex protection.
 * Safe to call from multiple coroutines concurrently.
 *
 * Performance:
 * - Background pre-loading on Dispatchers.IO
 * - LRU eviction when buffer exceeds 5 photos
 * - Concurrent bitmap loading using ImageCache
 *
 * Memory: ~80MB for 5 downsampled photos (2560x1600 @ ARGB_8888)
 *
 * @param imageCache Image loading and caching layer
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
@Singleton
class PhotoBufferManager @Inject constructor(
    private val imageCache: ImageCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()

    // Buffer state: Map of photo path to loaded bitmap
    private val buffer = LinkedHashMap<String, Bitmap>(BUFFER_SIZE, 0.75f, true)

    // Current photo list and index
    private var photos: List<Photo> = emptyList()
    private var currentIndex: Int = -1

    // Loading state
    private val _loadingState = MutableStateFlow<BufferLoadingState>(BufferLoadingState.Idle)
    val loadingState: StateFlow<BufferLoadingState> = _loadingState.asStateFlow()

    // Pre-loading jobs
    private val preloadJobs = mutableMapOf<String, Job>()

    /**
     * Initializes the buffer with a list of photos.
     * Starts at the first photo and begins pre-loading.
     *
     * Thread Safety: Safe to call concurrently. Cancels any existing pre-load jobs.
     *
     * @param photoList List of photos to buffer
     * @param startIndex Initial photo index (default 0)
     * @return Result.Success if initialized, Result.Error if failed
     */
    suspend fun initialize(photoList: List<Photo>, startIndex: Int = 0): Result<Unit> {
        return mutex.withLock {
            try {
                if (photoList.isEmpty()) {
                    return@withLock Result.error(
                        IllegalArgumentException("Photo list cannot be empty"),
                        "Cannot initialize buffer with empty photo list"
                    )
                }

                if (startIndex !in photoList.indices) {
                    return@withLock Result.error(
                        IndexOutOfBoundsException("Invalid start index: $startIndex"),
                        "Start index must be between 0 and ${photoList.size - 1}"
                    )
                }

                // Cancel existing pre-load jobs
                preloadJobs.values.forEach { it.cancel() }
                preloadJobs.clear()

                // Clear buffer
                buffer.clear()

                // Set new photo list and index
                photos = photoList
                currentIndex = startIndex

                _loadingState.value = BufferLoadingState.Loading

                // Pre-load initial buffer (Current, Current + 1, Current + 2)
                preloadInitialBuffer()

                Result.success(Unit)
            } catch (e: Exception) {
                _loadingState.value = BufferLoadingState.Error(e)
                Result.error(e, "Failed to initialize buffer: ${e.message}")
            }
        }
    }

    /**
     * Gets the current photo's bitmap.
     * Returns null if not loaded yet or if buffer is not initialized.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Bitmap if available, null otherwise
     */
    suspend fun getCurrentPhoto(): Bitmap? {
        return mutex.withLock {
            if (currentIndex == -1 || currentIndex !in photos.indices) return@withLock null
            buffer[photos[currentIndex].path]
        }
    }

    /**
     * Gets the current photo metadata.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Photo metadata if available, null otherwise
     */
    suspend fun getCurrentPhotoMetadata(): Photo? {
        return mutex.withLock {
            if (currentIndex == -1 || currentIndex !in photos.indices) return@withLock null
            photos[currentIndex]
        }
    }

    /**
     * Gets the current photo index.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Current index in the photo list
     */
    suspend fun getCurrentIndex(): Int {
        return mutex.withLock {
            currentIndex
        }
    }

    /**
     * Advances to the next media item in the list (photo or video).
     * Wraps around to the beginning if at the end.
     * Triggers pre-loading of the next photo.
     *
     * Video Handling: Returns Result.Success(null) for videos without loading bitmap.
     * Photo Handling: Loads bitmap with auto-retry mechanism.
     *
     * Auto-retry: If a photo fails to load, automatically tries the next one.
     * Silently skips failed photos without surfacing errors to UI.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Result.Success with next photo bitmap (or null for videos), Result.Error if all photos failed
     */
    suspend fun getNextPhoto(): Result<Bitmap?> {
        return mutex.withLock {
            if (photos.isEmpty()) {
                return@withLock Result.error(
                    IllegalStateException("Buffer not initialized"),
                    "Call initialize() before getting photos"
                )
            }

            val maxRetries = 10
            var retriesSoFar = 0

            while (retriesSoFar < maxRetries) {
                try {
                    // Advance to next index (wrap around)
                    currentIndex = (currentIndex + 1) % photos.size

                    val currentPhoto = photos[currentIndex]

                    // Skip bitmap loading for videos
                    if (currentPhoto.isVideo) {
                        android.util.Log.d(TAG, "getNextPhoto: Video detected, skipping bitmap load: ${currentPhoto.fileName}")
                        preloadNext()
                        _loadingState.value = BufferLoadingState.Ready
                        return@withLock Result.success(null)
                    }

                    val bitmap = buffer[currentPhoto.path]

                    if (bitmap != null) {
                        // Photo already in buffer, pre-load next
                        preloadNext()
                        _loadingState.value = BufferLoadingState.Ready
                        return@withLock Result.success(bitmap)
                    } else {
                        // Photo not in buffer, load it synchronously
                        _loadingState.value = BufferLoadingState.Loading
                        when (val result = imageCache.load(currentPhoto.path)) {
                            is Result.Success -> {
                                addToBuffer(currentPhoto.path, result.data)
                                preloadNext()
                                _loadingState.value = BufferLoadingState.Ready
                                return@withLock Result.success(result.data)
                            }
                            is Result.Error -> {
                                // Log error but try next photo silently
                                android.util.Log.w(TAG, "Failed to load ${currentPhoto.fileName}, trying next photo: ${result.message}")
                                retriesSoFar++
                                // Continue loop to try next photo
                            }
                            is Result.Loading -> {
                                // Unexpected state, try next photo
                                android.util.Log.w(TAG, "Unexpected loading state for ${currentPhoto.fileName}, trying next")
                                retriesSoFar++
                                // Continue loop to try next photo
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Exception getting next photo: ${e.message}, trying next")
                    retriesSoFar++
                    // Continue loop to try next photo
                }
            }

            // All retries failed
            _loadingState.value = BufferLoadingState.Error(IllegalStateException("Failed to load photo after $maxRetries attempts"))
            return@withLock Result.error(
                IllegalStateException("Failed to load photo after $maxRetries attempts"),
                "All recent photos failed to load"
            )
        }
    }

    /**
     * Goes back to the previous media item in the list (photo or video).
     * Wraps around to the end if at the beginning.
     * Triggers pre-loading of the previous photo.
     *
     * Video Handling: Returns Result.Success(null) for videos without loading bitmap.
     * Photo Handling: Loads bitmap with auto-retry mechanism.
     *
     * Auto-retry: If a photo fails to load, automatically tries the previous one.
     * Silently skips failed photos without surfacing errors to UI.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Result.Success with previous photo bitmap (or null for videos), Result.Error if all photos failed
     */
    suspend fun getPreviousPhoto(): Result<Bitmap?> {
        return mutex.withLock {
            if (photos.isEmpty()) {
                return@withLock Result.error(
                    IllegalStateException("Buffer not initialized"),
                    "Call initialize() before getting photos"
                )
            }

            val maxRetries = 10
            var retriesSoFar = 0

            while (retriesSoFar < maxRetries) {
                try {
                    // Go back to previous index (wrap around)
                    currentIndex = if (currentIndex == 0) photos.size - 1 else currentIndex - 1

                    val currentPhoto = photos[currentIndex]

                    // Skip bitmap loading for videos
                    if (currentPhoto.isVideo) {
                        android.util.Log.d(TAG, "getPreviousPhoto: Video detected, skipping bitmap load: ${currentPhoto.fileName}")
                        preloadPrevious()
                        _loadingState.value = BufferLoadingState.Ready
                        return@withLock Result.success(null)
                    }

                    val bitmap = buffer[currentPhoto.path]

                    if (bitmap != null) {
                        // Photo already in buffer, pre-load previous
                        preloadPrevious()
                        _loadingState.value = BufferLoadingState.Ready
                        return@withLock Result.success(bitmap)
                    } else {
                        // Photo not in buffer, load it synchronously
                        _loadingState.value = BufferLoadingState.Loading
                        when (val result = imageCache.load(currentPhoto.path)) {
                            is Result.Success -> {
                                addToBuffer(currentPhoto.path, result.data)
                                preloadPrevious()
                                _loadingState.value = BufferLoadingState.Ready
                                return@withLock Result.success(result.data)
                            }
                            is Result.Error -> {
                                // Log error but try previous photo silently
                                android.util.Log.w(TAG, "Failed to load ${currentPhoto.fileName}, trying previous photo: ${result.message}")
                                retriesSoFar++
                                // Continue loop to try previous photo
                            }
                            is Result.Loading -> {
                                // Unexpected state, try previous photo
                                android.util.Log.w(TAG, "Unexpected loading state for ${currentPhoto.fileName}, trying previous")
                                retriesSoFar++
                                // Continue loop to try previous photo
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Exception getting previous photo: ${e.message}, trying previous")
                    retriesSoFar++
                    // Continue loop to try previous photo
                }
            }

            // All retries failed
            _loadingState.value = BufferLoadingState.Error(IllegalStateException("Failed to load photo after $maxRetries attempts"))
            return@withLock Result.error(
                IllegalStateException("Failed to load photo after $maxRetries attempts"),
                "All recent photos failed to load"
            )
        }
    }

    /**
     * Pre-loads the next 2-3 photos in the background for smooth playback.
     * Called automatically after navigation.
     *
     * Aggressive read-ahead ensures photos are ready before user reaches them.
     * Skips videos (no bitmap to preload).
     *
     * Note: Must be called within mutex lock.
     */
    private fun preloadNext() {
        if (photos.isEmpty()) return

        // Preload next 2 photos ahead for smoother playback
        for (i in 1..2) {
            val nextIndex = (currentIndex + i) % photos.size
            val photoToPreload = photos[nextIndex]

            // Skip videos - no bitmap to preload
            if (photoToPreload.isVideo) {
                android.util.Log.d(TAG, "preloadNext: Skipping video preload: ${photoToPreload.fileName}")
                continue
            }

            // Only preload if not already in buffer or being loaded
            if (buffer.containsKey(photoToPreload.path) || preloadJobs.containsKey(photoToPreload.path)) {
                continue
            }

            // Start new preload job
            val job = CoroutineScope(ioDispatcher).launch {
                when (val result = imageCache.load(photoToPreload.path)) {
                    is Result.Success -> {
                        mutex.withLock {
                            addToBuffer(photoToPreload.path, result.data)
                            preloadJobs.remove(photoToPreload.path)
                        }
                        android.util.Log.d(TAG, "Preloaded photo +$i ahead: ${photoToPreload.fileName}")
                    }
                    is Result.Error -> {
                        // Log error but don't fail (will load on-demand or skip later)
                        android.util.Log.w(TAG, "Failed to preload ${photoToPreload.fileName}: ${result.message}")
                        mutex.withLock {
                            preloadJobs.remove(photoToPreload.path)
                        }
                    }
                    is Result.Loading -> {
                        // Should not happen, ignore
                    }
                }
            }

            preloadJobs[photoToPreload.path] = job
        }
    }

    /**
     * Pre-loads the previous 1-2 photos in the background.
     * Called automatically after backward navigation.
     * Skips videos (no bitmap to preload).
     *
     * Note: Must be called within mutex lock.
     */
    private fun preloadPrevious() {
        if (photos.isEmpty()) return

        // Preload previous 1-2 photos for backward navigation
        for (i in 1..1) {  // Only 1 previous for backward, since forward is more common
            val prevIndex = (currentIndex - i + photos.size) % photos.size
            val photoToPreload = photos[prevIndex]

            // Skip videos - no bitmap to preload
            if (photoToPreload.isVideo) {
                android.util.Log.d(TAG, "preloadPrevious: Skipping video preload: ${photoToPreload.fileName}")
                continue
            }

            // Only preload if not already in buffer or being loaded
            if (buffer.containsKey(photoToPreload.path) || preloadJobs.containsKey(photoToPreload.path)) {
                continue
            }

            // Start new preload job
            val job = CoroutineScope(ioDispatcher).launch {
                when (val result = imageCache.load(photoToPreload.path)) {
                    is Result.Success -> {
                        mutex.withLock {
                            addToBuffer(photoToPreload.path, result.data)
                            preloadJobs.remove(photoToPreload.path)
                        }
                        android.util.Log.d(TAG, "Preloaded photo -$i backward: ${photoToPreload.fileName}")
                    }
                    is Result.Error -> {
                        // Log error but don't fail (will load on-demand or skip later)
                        android.util.Log.w(TAG, "Failed to preload ${photoToPreload.fileName}: ${result.message}")
                        mutex.withLock {
                            preloadJobs.remove(photoToPreload.path)
                        }
                    }
                    is Result.Loading -> {
                        // Should not happen, ignore
                    }
                }
            }

            preloadJobs[photoToPreload.path] = job
        }
    }

    /**
     * Pre-loads the initial buffer on initialization.
     * Loads Current, Current + 1, and Current + 2.
     * Skips videos (no bitmap to preload).
     *
     * Note: Must be called within mutex lock.
     */
    private fun preloadInitialBuffer() {
        if (photos.isEmpty()) return

        // Pre-load current photo first (synchronous, blocking)
        val currentPhoto = photos[currentIndex]
        val job = CoroutineScope(ioDispatcher).launch {
            // Skip videos for current photo
            if (currentPhoto.isVideo) {
                android.util.Log.d(TAG, "preloadInitialBuffer: Current item is video, skipping bitmap load: ${currentPhoto.fileName}")
                mutex.withLock {
                    _loadingState.value = BufferLoadingState.Ready
                }
            } else {
                when (val result = imageCache.load(currentPhoto.path)) {
                    is Result.Success -> {
                        mutex.withLock {
                            addToBuffer(currentPhoto.path, result.data)
                            _loadingState.value = BufferLoadingState.Ready
                        }
                    }
                    is Result.Error -> {
                        mutex.withLock {
                            _loadingState.value = BufferLoadingState.Error(result.exception)
                        }
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            }

            // Pre-load next 2 photos in background (skip videos)
            for (i in 1..2) {
                val nextIndex = (currentIndex + i) % photos.size
                val photo = photos[nextIndex]

                // Skip videos
                if (photo.isVideo) {
                    android.util.Log.d(TAG, "preloadInitialBuffer: Skipping video: ${photo.fileName}")
                    continue
                }

                if (!buffer.containsKey(photo.path)) {
                    when (val result = imageCache.load(photo.path)) {
                        is Result.Success -> {
                            mutex.withLock {
                                addToBuffer(photo.path, result.data)
                            }
                        }
                        is Result.Error -> {
                            // Log error but continue
                        }
                        is Result.Loading -> {
                            // Should not happen
                        }
                    }
                }
            }
        }

        preloadJobs[currentPhoto.path] = job
    }

    /**
     * Adds a bitmap to the buffer, enforcing LRU eviction.
     * Keeps buffer size at BUFFER_SIZE (5 photos).
     *
     * Note: Must be called within mutex lock.
     *
     * @param path Photo path (key)
     * @param bitmap Loaded bitmap
     */
    private fun addToBuffer(path: String, bitmap: Bitmap) {
        buffer[path] = bitmap

        // Enforce buffer size limit (LRU eviction)
        while (buffer.size > BUFFER_SIZE) {
            val oldestKey = buffer.keys.first()
            val evictedBitmap = buffer.remove(oldestKey)
            evictedBitmap?.recycle() // Recycle bitmap to free memory
        }
    }

    /**
     * Gets the current buffer size.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Number of photos currently in buffer
     */
    suspend fun getBufferSize(): Int {
        return mutex.withLock {
            buffer.size
        }
    }

    /**
     * Clears the buffer and cancels all pre-load jobs.
     *
     * Thread Safety: Safe to call concurrently.
     */
    suspend fun clear() {
        mutex.withLock {
            // Cancel all pre-load jobs
            preloadJobs.values.forEach { it.cancel() }
            preloadJobs.clear()

            // Recycle all bitmaps
            buffer.values.forEach { it.recycle() }
            buffer.clear()

            // Reset state
            photos = emptyList()
            currentIndex = -1
            _loadingState.value = BufferLoadingState.Idle
        }
    }

    companion object {
        /**
         * Buffer size: 5 photos for smooth playback.
         * Layout: [Current - 1, Current, Current + 1, Current + 2, Current + 3]
         */
        const val BUFFER_SIZE = 5
        private const val TAG = "PhotoBufferManager"
    }
}

/**
 * Represents the loading state of the photo buffer.
 *
 * Thread Safety: Immutable sealed class, safe to share across threads.
 */
sealed class BufferLoadingState {
    /**
     * Buffer is idle (not initialized).
     */
    object Idle : BufferLoadingState()

    /**
     * Buffer is currently loading a photo.
     */
    object Loading : BufferLoadingState()

    /**
     * Buffer is ready (current photo loaded).
     */
    object Ready : BufferLoadingState()

    /**
     * Buffer encountered an error.
     */
    data class Error(val exception: Throwable) : BufferLoadingState()
}
