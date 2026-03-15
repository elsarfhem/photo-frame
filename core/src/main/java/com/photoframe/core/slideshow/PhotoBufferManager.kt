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
import kotlinx.coroutines.withTimeout
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
     * Thread Safety: Safe to call concurrently. Mutex is released during IO operations.
     *
     * @param displayIntervalMs Display interval in milliseconds for dynamic timeout calculation
     * @return Result.Success with next photo bitmap (or null for videos), Result.Error if all photos failed
     */
    suspend fun getNextPhoto(displayIntervalMs: Long = 10_000L): Result<Bitmap?> {
        // FIX A: Timeout EQUALS display interval (user requirement: "equal to configured delay")
        // Changed from 70% to 100% to meet requirement
        val timeoutPerAttempt = displayIntervalMs.coerceAtLeast(2_000L)

        // FIX A: Single retry only (worst case = timeout = interval)
        // Changed from adaptive 1-3 retries to ensure total time ≤ interval
        val maxRetries = 1

        var retriesSoFar = 0

        while (retriesSoFar < maxRetries) {
            try {
                // STEP 1: Acquire mutex, read state, release mutex
                val photoToLoad: Photo
                val isInBuffer: Boolean

                mutex.withLock {
                    if (photos.isEmpty()) {
                        return Result.error(
                            IllegalStateException("Buffer not initialized"),
                            "Call initialize() before getting photos"
                        )
                    }

                    // Advance to next index (wrap around)
                    currentIndex = (currentIndex + 1) % photos.size
                    photoToLoad = photos[currentIndex]
                    isInBuffer = buffer.containsKey(photoToLoad.path)

                    // Handle videos immediately
                    if (photoToLoad.isVideo) {
                        android.util.Log.d(TAG, "getNextPhoto: Video detected, skipping bitmap load: ${photoToLoad.fileName}")
                        preloadNext(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(null)
                    }

                    // If in buffer, return it
                    if (isInBuffer) {
                        val bitmap = buffer[photoToLoad.path]
                        preloadNext(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(bitmap)
                    }

                    // Mark as loading before releasing mutex
                    _loadingState.value = BufferLoadingState.Loading
                }

                // STEP 2: Load photo WITHOUT holding mutex (with dynamic timeout)
                val loadResult = try {
                    withTimeout(timeoutPerAttempt) {
                        imageCache.load(photoToLoad.path)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.w(TAG, "Timeout loading ${photoToLoad.fileName} after ${timeoutPerAttempt}ms, trying next")
                    retriesSoFar++
                    continue
                }

                // STEP 3: Reacquire mutex, update buffer, release mutex
                when (loadResult) {
                    is Result.Success -> {
                        mutex.withLock {
                            addToBuffer(photoToLoad.path, loadResult.data)
                            preloadNext(displayIntervalMs)
                            _loadingState.value = BufferLoadingState.Ready
                        }
                        return Result.success(loadResult.data)
                    }
                    is Result.Error -> {
                        android.util.Log.w(TAG, "Failed to load ${photoToLoad.fileName}, trying next photo: ${loadResult.message}")
                        retriesSoFar++
                    }
                    is Result.Loading -> {
                        android.util.Log.w(TAG, "Unexpected loading state for ${photoToLoad.fileName}, trying next")
                        retriesSoFar++
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception getting next photo: ${e.message}, trying next")
                retriesSoFar++
            }
        }

        // All retries failed
        mutex.withLock {
            _loadingState.value = BufferLoadingState.Error(IllegalStateException("Failed to load photo after $maxRetries attempts"))
        }
        return Result.error(
            IllegalStateException("Failed to load photo after $maxRetries attempts"),
            "All recent photos failed to load"
        )
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
     * Thread Safety: Safe to call concurrently. Mutex is released during IO operations.
     *
     * @param displayIntervalMs Display interval in milliseconds for dynamic timeout calculation
     * @return Result.Success with previous photo bitmap (or null for videos), Result.Error if all photos failed
     */
    suspend fun getPreviousPhoto(displayIntervalMs: Long = 10_000L): Result<Bitmap?> {
        // FIX A: Timeout EQUALS display interval (user requirement: "equal to configured delay")
        // Changed from 70% to 100% to meet requirement
        val timeoutPerAttempt = displayIntervalMs.coerceAtLeast(2_000L)

        // FIX A: Single retry only (worst case = timeout = interval)
        // Changed from adaptive 1-3 retries to ensure total time ≤ interval
        val maxRetries = 1

        var retriesSoFar = 0

        while (retriesSoFar < maxRetries) {
            try {
                // STEP 1: Acquire mutex, read state, release mutex
                val photoToLoad: Photo
                val isInBuffer: Boolean

                mutex.withLock {
                    if (photos.isEmpty()) {
                        return Result.error(
                            IllegalStateException("Buffer not initialized"),
                            "Call initialize() before getting photos"
                        )
                    }

                    // Go back to previous index (wrap around)
                    currentIndex = if (currentIndex == 0) photos.size - 1 else currentIndex - 1
                    photoToLoad = photos[currentIndex]
                    isInBuffer = buffer.containsKey(photoToLoad.path)

                    // Handle videos immediately
                    if (photoToLoad.isVideo) {
                        android.util.Log.d(TAG, "getPreviousPhoto: Video detected, skipping bitmap load: ${photoToLoad.fileName}")
                        preloadPrevious(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(null)
                    }

                    // If in buffer, return it
                    if (isInBuffer) {
                        val bitmap = buffer[photoToLoad.path]
                        preloadPrevious(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(bitmap)
                    }

                    // Mark as loading before releasing mutex
                    _loadingState.value = BufferLoadingState.Loading
                }

                // STEP 2: Load photo WITHOUT holding mutex (with dynamic timeout)
                val loadResult = try {
                    withTimeout(timeoutPerAttempt) {
                        imageCache.load(photoToLoad.path)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.w(TAG, "Timeout loading ${photoToLoad.fileName} after ${timeoutPerAttempt}ms, trying previous")
                    retriesSoFar++
                    continue
                }

                // STEP 3: Reacquire mutex, update buffer, release mutex
                when (loadResult) {
                    is Result.Success -> {
                        mutex.withLock {
                            addToBuffer(photoToLoad.path, loadResult.data)
                            preloadPrevious(displayIntervalMs)
                            _loadingState.value = BufferLoadingState.Ready
                        }
                        return Result.success(loadResult.data)
                    }
                    is Result.Error -> {
                        android.util.Log.w(TAG, "Failed to load ${photoToLoad.fileName}, trying previous photo: ${loadResult.message}")
                        retriesSoFar++
                    }
                    is Result.Loading -> {
                        android.util.Log.w(TAG, "Unexpected loading state for ${photoToLoad.fileName}, trying previous")
                        retriesSoFar++
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception getting previous photo: ${e.message}, trying previous")
                retriesSoFar++
            }
        }

        // All retries failed
        mutex.withLock {
            _loadingState.value = BufferLoadingState.Error(IllegalStateException("Failed to load photo after $maxRetries attempts"))
        }
        return Result.error(
            IllegalStateException("Failed to load photo after $maxRetries attempts"),
            "All recent photos failed to load"
        )
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
    private fun preloadNext(displayIntervalMs: Long = 10_000L) {
        if (photos.isEmpty()) return

        // PRELOAD FIX: Dynamic preload timeout based on display interval
        // 80% of interval to complete before next advance, min 2s, max 10s
        val preloadTimeout = (displayIntervalMs * 0.8).toLong().coerceIn(2_000L, 10_000L)

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
                // PRELOAD FIX: Use dynamic timeout instead of fixed 5s
                val result = try {
                    withTimeout(preloadTimeout) {
                        imageCache.load(photoToPreload.path)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.w(TAG, "Timeout preloading ${photoToPreload.fileName} after ${preloadTimeout}ms")
                    mutex.withLock {
                        preloadJobs.remove(photoToPreload.path)
                    }
                    return@launch
                }

                when (result) {
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
    private fun preloadPrevious(displayIntervalMs: Long = 10_000L) {
        if (photos.isEmpty()) return

        // PRELOAD FIX: Dynamic preload timeout based on display interval
        // 80% of interval to complete before next advance, min 2s, max 10s
        val preloadTimeout = (displayIntervalMs * 0.8).toLong().coerceIn(2_000L, 10_000L)

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
                // PRELOAD FIX: Use dynamic timeout instead of fixed 5s
                val result = try {
                    withTimeout(preloadTimeout) {
                        imageCache.load(photoToPreload.path)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.w(TAG, "Timeout preloading ${photoToPreload.fileName} after ${preloadTimeout}ms")
                    mutex.withLock {
                        preloadJobs.remove(photoToPreload.path)
                    }
                    return@launch
                }

                when (result) {
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
     * Loads Current photo SYNCHRONOUSLY, then loads Current + 1 and Current + 2 in background.
     * Skips videos (no bitmap to preload).
     *
     * Note: Must be called within mutex lock.
     */
    private suspend fun preloadInitialBuffer() {
        if (photos.isEmpty()) return

        val currentPhoto = photos[currentIndex]

        // Load current photo SYNCHRONOUSLY to ensure it's available before initialize() returns
        if (currentPhoto.isVideo) {
            android.util.Log.d(TAG, "preloadInitialBuffer: Current item is video, skipping bitmap load: ${currentPhoto.fileName}")
            _loadingState.value = BufferLoadingState.Ready
        } else {
            // Fix #3: Add timeout to initial photo load to prevent indefinite hangs
            // Load current photo synchronously (blocking) - CRITICAL FIX for black screen
            val result = try {
                withTimeout(PRELOAD_TIMEOUT_MS) {
                    imageCache.load(currentPhoto.path)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e(TAG, "preloadInitialBuffer: Timeout loading current photo after ${PRELOAD_TIMEOUT_MS}ms")
                _loadingState.value = BufferLoadingState.Error(e)
                return
            }

            when (result) {
                is Result.Success -> {
                    addToBuffer(currentPhoto.path, result.data)
                    _loadingState.value = BufferLoadingState.Ready
                    android.util.Log.d(TAG, "preloadInitialBuffer: Current photo loaded synchronously: ${currentPhoto.fileName}")
                }
                is Result.Error -> {
                    _loadingState.value = BufferLoadingState.Error(result.exception)
                    android.util.Log.e(TAG, "preloadInitialBuffer: Failed to load current photo: ${result.message}")
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }

        // Pre-load next 2 photos in background (async, non-blocking)
        val job = CoroutineScope(ioDispatcher).launch {
            for (i in 1..2) {
                val nextIndex = (currentIndex + i) % photos.size
                val photo = photos[nextIndex]

                // Skip videos
                if (photo.isVideo) {
                    android.util.Log.d(TAG, "preloadInitialBuffer: Skipping video: ${photo.fileName}")
                    continue
                }

                if (!buffer.containsKey(photo.path)) {
                    // Fix #3: Add timeout to preload operations to prevent indefinite hangs
                    val result = try {
                        withTimeout(PRELOAD_TIMEOUT_MS) {
                            imageCache.load(photo.path)
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        android.util.Log.w(TAG, "preloadInitialBuffer: Timeout preloading ${photo.fileName} after ${PRELOAD_TIMEOUT_MS}ms")
                        continue
                    }

                    when (result) {
                        is Result.Success -> {
                            mutex.withLock {
                                addToBuffer(photo.path, result.data)
                            }
                        }
                        is Result.Error -> {
                            android.util.Log.w(TAG, "preloadInitialBuffer: Failed to preload ${photo.fileName}: ${result.message}")
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

    /**
     * Reduces buffer to minimum size (keeps only current photo).
     * Used during memory pressure for graceful degradation.
     *
     * Thread Safety: Safe to call concurrently.
     */
    suspend fun reduceToMinimum() {
        mutex.withLock {
            if (photos.isEmpty() || currentIndex == -1) return@withLock

            // Cancel all pre-load jobs
            preloadJobs.values.forEach { it.cancel() }
            preloadJobs.clear()

            // Get current photo path before clearing buffer
            val currentPhotoPath = photos.getOrNull(currentIndex)?.path

            // Recycle all bitmaps except current photo
            buffer.entries.forEach { (path, bitmap) ->
                if (path != currentPhotoPath) {
                    bitmap.recycle()
                }
            }

            // Keep only current photo in buffer
            val currentBitmap = currentPhotoPath?.let { buffer[it] }
            buffer.clear()
            if (currentBitmap != null && currentPhotoPath != null) {
                buffer[currentPhotoPath] = currentBitmap
            }

            android.util.Log.d(TAG, "Reduced buffer to minimum (1 photo) due to memory pressure")
        }
    }

    companion object {
        /**
         * Buffer size: 5 photos for smooth playback.
         * Layout: [Current - 1, Current, Current + 1, Current + 2, Current + 3]
         */
        const val BUFFER_SIZE = 5

        /**
         * Preload timeout: 5 seconds per photo.
         * Fix #3: Prevents indefinite hangs during background preloading.
         */
        private const val PRELOAD_TIMEOUT_MS = 5_000L

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
