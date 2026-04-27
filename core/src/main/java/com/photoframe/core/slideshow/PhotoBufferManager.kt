package com.photoframe.core.slideshow

import android.graphics.Bitmap
import android.util.Log
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.image.ImageCache
import com.photoframe.core.model.Photo
import com.photoframe.core.model.Result
import com.photoframe.core.network.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
    private val networkMonitor: NetworkMonitor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()

    // Dynamic buffer sizing based on device memory
    // Each photo ≈ 16MB (2560x1600 ARGB_8888). Reserve 50% of heap for buffer.
    private val bufferSize: Int = calculateBufferSize()
    private val preloadAhead: Int = (bufferSize - 2).coerceAtLeast(2) // Reserve 1 for current, 1 for previous
    private val preloadBehind: Int = (bufferSize / 5).coerceAtLeast(1)

    // Buffer state: Map of photo path to loaded bitmap
    private val buffer = LinkedHashMap<String, Bitmap>(bufferSize, 0.75f, true)

    // Blacklist: paths that repeatedly fail to load (timeout, decode error, etc.)
    // Skipped instantly on future attempts — prevents the same bad file from consuming timeout budget.
    // Cleared on clear() and syncPhotoList() (fresh photo list = fresh start).
    private val blacklistedPaths = mutableSetOf<String>()

    // Current photo list and index
    private var photos: List<Photo> = emptyList()
    private var currentIndex: Int = -1

    // Loading state
    private val _loadingState = MutableStateFlow<BufferLoadingState>(BufferLoadingState.Idle)
    val loadingState: StateFlow<BufferLoadingState> = _loadingState.asStateFlow()

    // Pre-loading jobs
    private val preloadJobs = mutableMapOf<String, Job>()

    // Managed coroutine scope for background jobs (prevents orphaned coroutines)
    private var scope = CoroutineScope(SupervisorJob() + ioDispatcher)

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
        Log.d(TAG, "initialize: bufferSize=$bufferSize, preloadAhead=$preloadAhead, preloadBehind=$preloadBehind")
        // STEP 1: Validate and set state under mutex (fast, no I/O)
        val photoToLoad: Photo
        mutex.withLock {
            try {
                if (photoList.isEmpty()) {
                    return Result.error(
                        IllegalArgumentException("Photo list cannot be empty"),
                        "Cannot initialize buffer with empty photo list"
                    )
                }

                if (startIndex !in photoList.indices) {
                    return Result.error(
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
                photoToLoad = photos[currentIndex]
            } catch (e: Exception) {
                _loadingState.value = BufferLoadingState.Error(e)
                return Result.error(e, "Failed to initialize buffer: ${e.message}")
            }
        }

        // STEP 2: Load initial photo WITHOUT holding mutex (up to 5s I/O).
        // Walk the whole list — unreachable SMB photos blacklist fast (or skip instantly
        // when offline), so exhausting the cap on a mixed local+SMB library was the real
        // failure mode. Cap still exists to guarantee termination if every path fails.
        val maxInitAttempts = photoList.size
        var loaded = false

        for (attempt in 0 until maxInitAttempts) {
            val photo = mutex.withLock { photos[currentIndex] }

            // Offline: skip SMB photos instantly (would fail anyway, waste timeout budget)
            if (!networkMonitor.isNetworkAvailable.value && photo.path.startsWith("smb://")) {
                mutex.withLock { currentIndex = (currentIndex + 1) % photos.size }
                continue
            }

            if (photo.isVideo) {
                Log.d(TAG, "initialize: Current item is video, skipping bitmap load: ${photo.fileName}")
                mutex.withLock { _loadingState.value = BufferLoadingState.Ready }
                loaded = true
                break
            }

            val result = try {
                withTimeout(PRELOAD_TIMEOUT_MS) { imageCache.load(photo.path) }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "initialize: Timeout loading ${photo.fileName}, trying next (attempt ${attempt + 1}/$maxInitAttempts)")
                mutex.withLock { currentIndex = (currentIndex + 1) % photos.size }
                continue
            }

            when (result) {
                is Result.Success -> {
                    mutex.withLock {
                        addToBuffer(photo.path, result.data)
                        _loadingState.value = BufferLoadingState.Ready
                    }
                    Log.d(TAG, "initialize: Current photo loaded: ${photo.fileName}")
                    loaded = true
                    break
                }
                is Result.Error -> {
                    Log.w(TAG, "initialize: Failed to load ${photo.fileName}, trying next (attempt ${attempt + 1}/$maxInitAttempts)")
                    mutex.withLock { currentIndex = (currentIndex + 1) % photos.size }
                }
                is Result.Loading -> { /* Should not happen */ }
            }
        }

        if (!loaded) {
            val e = IllegalStateException("Failed to load any of the first $maxInitAttempts photos")
            mutex.withLock { _loadingState.value = BufferLoadingState.Error(e) }
            return Result.error(e, "Could not load initial photo after $maxInitAttempts attempts")
        }

        // STEP 4: Preload next photos in background (non-blocking)
        scope.launch {
            for (i in 1..preloadAhead) {
                val nextIndex: Int
                val photo: Photo
                mutex.withLock {
                    nextIndex = (currentIndex + i) % photos.size
                    photo = photos[nextIndex]
                }

                if (photo.isVideo) {
                    Log.d(TAG, "initialize: Skipping video preload: ${photo.fileName}")
                    continue
                }

                val alreadyBuffered = mutex.withLock { buffer.containsKey(photo.path) }
                if (!alreadyBuffered) {
                    val preloadResult = try {
                        withTimeout(PRELOAD_TIMEOUT_MS) {
                            imageCache.load(photo.path)
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.w(TAG, "initialize: Timeout preloading ${photo.fileName}")
                        continue
                    }

                    when (preloadResult) {
                        is Result.Success -> {
                            mutex.withLock {
                                addToBuffer(photo.path, preloadResult.data)
                            }
                            Log.d(TAG, "initialize: Preloaded +$i ahead: ${photo.fileName}")
                        }
                        is Result.Error -> {
                            Log.w(TAG, "initialize: Failed to preload ${photo.fileName}: ${preloadResult.message}")
                        }
                        is Result.Loading -> { /* Should not happen */ }
                    }
                }
            }
        }

        return Result.success(Unit)
    }

    /**
     * Subscribes to photo list updates from repository StateFlow.
     * FIX 3: Prevents stale photo list desynchronization.
     *
     * Should be called after initialize() to keep buffer's photo list
     * in sync with repository changes (e.g., background scans, shuffles).
     *
     * Thread Safety: Safe to call concurrently. Runs in managed scope.
     *
     * @param photoListFlow StateFlow from repository providing updated photos
     */
    fun subscribeToPhotoUpdates(photoListFlow: StateFlow<List<Photo>>) {
        scope.launch {
            photoListFlow.collect { updatedPhotos ->
                syncPhotoList(updatedPhotos)
            }
        }
    }

    /**
     * Syncs the buffer's internal photo list with an updated photo list.
     * Called when repository photo list changes (e.g., after background scan).
     *
     * FIX 3: Prevents stale photo list desynchronization by keeping buffer's
     * photo list in sync with repository changes.
     *
     * Thread Safety: Safe to call concurrently. Acquires mutex.
     *
     * @param updatedPhotoList New photo list from repository
     */
    suspend fun syncPhotoList(updatedPhotoList: List<Photo>) {
        mutex.withLock {
            if (updatedPhotoList.isEmpty()) {
                Log.w(TAG, "syncPhotoList: Updated photo list is empty, keeping current")
                return@withLock
            }

            // Only update if lists differ (avoid unnecessary updates)
            if (photos == updatedPhotoList) {
                return@withLock
            }

            Log.d(TAG, "syncPhotoList: Syncing from ${photos.size} to ${updatedPhotoList.size} photos")

            // Check if current photo still exists in updated list
            val currentPhotoPath = photos.getOrNull(currentIndex)?.path
            val newIndex = if (currentPhotoPath != null) {
                updatedPhotoList.indexOfFirst { it.path == currentPhotoPath }
            } else {
                -1
            }

            // Update photo list
            photos = updatedPhotoList

            // Update index if current photo still exists, otherwise keep current index (clamped)
            if (newIndex >= 0) {
                currentIndex = newIndex
                Log.d(TAG, "syncPhotoList: Current photo still exists at index $newIndex")
            } else if (currentIndex >= photos.size) {
                // Current index out of bounds - clamp to last photo
                currentIndex = (photos.size - 1).coerceAtLeast(0)
                Log.w(TAG, "syncPhotoList: Current index out of bounds, clamped to $currentIndex")
            } else {
                Log.d(TAG, "syncPhotoList: Kept current index $currentIndex (still valid)")
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
        // Timeout is 50% of display interval — gives breathing room for the auto-advance loop
        // and prevents a single slow photo from consuming the entire display cycle
        val timeoutPerAttempt = (displayIntervalMs / 2).coerceAtLeast(2_000L)

        // maxRetries counts only real I/O failures (timeout, decode error).
        // Blacklisted/offline-SMB skips are free (instant) and capped at
        // photos.size to prevent infinite loops if every path is unreachable.
        val maxRetries = 3
        var retriesSoFar = 0
        var consecutiveSkips = 0
        val maxConsecutiveSkips = mutex.withLock { photos.size + 1 }

        while (retriesSoFar < maxRetries && consecutiveSkips < maxConsecutiveSkips) {
            try {
                // STEP 1: Acquire mutex, advance index, decide action
                var photoToLoad: Photo? = null
                val earlyResult = mutex.withLock {
                    if (photos.isEmpty()) {
                        return Result.error(
                            IllegalStateException("Buffer not initialized"),
                            "Call initialize() before getting photos"
                        )
                    }

                    // Advance to next index (wrap around)
                    currentIndex = (currentIndex + 1) % photos.size
                    val photo = photos[currentIndex]

                    // Skip blacklisted paths instantly (no timeout wasted)
                    if (blacklistedPaths.contains(photo.path)) {
                        Log.d(TAG, "getNextPhoto: Skipping blacklisted ${photo.fileName}")
                        return@withLock null // signal: skip to next iteration
                    }

                    // Skip SMB paths when offline (avoid wasted timeout on unreachable host)
                    if (!networkMonitor.isNetworkAvailable.value && photo.path.startsWith("smb://")) {
                        return@withLock null // signal: skip to next iteration
                    }

                    // Handle videos immediately
                    if (photo.isVideo) {
                        Log.d(TAG, "getNextPhoto: Video detected, skipping bitmap load: ${photo.fileName}")
                        preloadNext(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(null)
                    }

                    // If in buffer, return it
                    if (buffer.containsKey(photo.path)) {
                        val bitmap = buffer[photo.path]
                        preloadNext(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(bitmap)
                    }

                    // Needs loading
                    _loadingState.value = BufferLoadingState.Loading
                    photoToLoad = photo
                    photo // non-null = proceed to load
                }

                // Blacklisted/offline skip — free, doesn't count as retry
                if (earlyResult == null) {
                    consecutiveSkips++
                    continue
                }
                consecutiveSkips = 0 // Reset once we attempt a real load

                val photo = photoToLoad!!

                // STEP 2: Load photo WITHOUT holding mutex (with dynamic timeout)
                val loadResult = try {
                    withTimeout(timeoutPerAttempt) {
                        imageCache.load(photo.path)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timeout loading ${photo.fileName} after ${timeoutPerAttempt}ms, blacklisting")
                    blacklistPath(photo.path)
                    retriesSoFar++
                    continue
                }

                // STEP 3: Reacquire mutex, update buffer, release mutex
                when (loadResult) {
                    is Result.Success -> {
                        mutex.withLock {
                            addToBuffer(photo.path, loadResult.data)
                            preloadNext(displayIntervalMs)
                            _loadingState.value = BufferLoadingState.Ready
                        }
                        return Result.success(loadResult.data)
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Failed to load ${photo.fileName}, blacklisting: ${loadResult.message}")
                        blacklistPath(photo.path)
                        retriesSoFar++
                    }
                    is Result.Loading -> {
                        Log.w(TAG, "Unexpected loading state for ${photo.fileName}, trying next")
                        retriesSoFar++
                    }
                }
            } catch (e: CancellationException) {
                // Rethrow CancellationException — never swallow coroutine cancellation
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting next photo: ${e.message}, trying next")
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
        // Timeout is 50% of display interval — gives breathing room for the auto-advance loop
        // and prevents a single slow photo from consuming the entire display cycle
        val timeoutPerAttempt = (displayIntervalMs / 2).coerceAtLeast(2_000L)

        // maxRetries counts only real I/O failures (timeout, decode error).
        // Blacklisted/offline-SMB skips are free (instant) and don't count.
        val maxRetries = 3
        var retriesSoFar = 0
        var consecutiveSkips = 0
        val maxConsecutiveSkips = mutex.withLock { photos.size + 1 }

        while (retriesSoFar < maxRetries && consecutiveSkips < maxConsecutiveSkips) {
            try {
                // STEP 1: Acquire mutex, advance index backward, decide action
                var photoToLoad: Photo? = null
                val earlyResult = mutex.withLock {
                    if (photos.isEmpty()) {
                        return Result.error(
                            IllegalStateException("Buffer not initialized"),
                            "Call initialize() before getting photos"
                        )
                    }

                    // Go back to previous index (wrap around)
                    currentIndex = if (currentIndex == 0) photos.size - 1 else currentIndex - 1
                    val photo = photos[currentIndex]

                    // Skip blacklisted paths instantly (no timeout wasted)
                    if (blacklistedPaths.contains(photo.path)) {
                        Log.d(TAG, "getPreviousPhoto: Skipping blacklisted ${photo.fileName}")
                        return@withLock null // signal: skip to next iteration
                    }

                    // Skip SMB paths when offline (avoid wasted timeout on unreachable host)
                    if (!networkMonitor.isNetworkAvailable.value && photo.path.startsWith("smb://")) {
                        return@withLock null // signal: skip to next iteration
                    }

                    // Handle videos immediately
                    if (photo.isVideo) {
                        Log.d(TAG, "getPreviousPhoto: Video detected, skipping bitmap load: ${photo.fileName}")
                        preloadPrevious(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(null)
                    }

                    // If in buffer, return it
                    if (buffer.containsKey(photo.path)) {
                        val bitmap = buffer[photo.path]
                        preloadPrevious(displayIntervalMs)
                        _loadingState.value = BufferLoadingState.Ready
                        return Result.success(bitmap)
                    }

                    // Needs loading
                    _loadingState.value = BufferLoadingState.Loading
                    photoToLoad = photo
                    photo // non-null = proceed to load
                }

                // Blacklisted/offline skip — free, doesn't count as retry
                if (earlyResult == null) {
                    consecutiveSkips++
                    continue
                }
                consecutiveSkips = 0

                val photo = photoToLoad!!

                // STEP 2: Load photo WITHOUT holding mutex (with dynamic timeout)
                val loadResult = try {
                    withTimeout(timeoutPerAttempt) {
                        imageCache.load(photo.path)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timeout loading ${photo.fileName} after ${timeoutPerAttempt}ms, blacklisting")
                    blacklistPath(photo.path)
                    retriesSoFar++
                    continue
                }

                // STEP 3: Reacquire mutex, update buffer, release mutex
                when (loadResult) {
                    is Result.Success -> {
                        mutex.withLock {
                            addToBuffer(photo.path, loadResult.data)
                            preloadPrevious(displayIntervalMs)
                            _loadingState.value = BufferLoadingState.Ready
                        }
                        return Result.success(loadResult.data)
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Failed to load ${photo.fileName}, blacklisting: ${loadResult.message}")
                        blacklistPath(photo.path)
                        retriesSoFar++
                    }
                    is Result.Loading -> {
                        Log.w(TAG, "Unexpected loading state for ${photo.fileName}, trying previous")
                        retriesSoFar++
                    }
                }
            } catch (e: CancellationException) {
                // Rethrow CancellationException — never swallow coroutine cancellation
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting previous photo: ${e.message}, trying previous")
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

        // Preload photos ahead (count based on device memory)
        for (i in 1..preloadAhead) {
            val nextIndex = (currentIndex + i) % photos.size
            val photoToPreload = photos[nextIndex]

            // Skip videos - no bitmap to preload
            if (photoToPreload.isVideo) {
                Log.d(TAG, "preloadNext: Skipping video preload: ${photoToPreload.fileName}")
                continue
            }

            // Skip blacklisted paths — don't waste preload timeout on known-bad files
            if (blacklistedPaths.contains(photoToPreload.path)) continue

            // Only preload if not already in buffer or being loaded
            if (buffer.containsKey(photoToPreload.path) || preloadJobs.containsKey(photoToPreload.path)) {
                continue
            }

            // Start new preload job
            val job = scope.launch {
                // PRELOAD FIX: Use dynamic timeout instead of fixed 5s
                val result = try {
                    withTimeout(preloadTimeout) {
                        imageCache.load(photoToPreload.path)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timeout preloading ${photoToPreload.fileName} after ${preloadTimeout}ms, blacklisting")
                    blacklistPath(photoToPreload.path)
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
                        Log.d(TAG, "Preloaded photo +$i ahead: ${photoToPreload.fileName}")
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Failed to preload ${photoToPreload.fileName}, blacklisting: ${result.message}")
                        blacklistPath(photoToPreload.path)
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

        // Preload photos behind (count based on device memory)
        for (i in 1..preloadBehind) {
            val prevIndex = (currentIndex - i + photos.size) % photos.size
            val photoToPreload = photos[prevIndex]

            // Skip videos - no bitmap to preload
            if (photoToPreload.isVideo) {
                Log.d(TAG, "preloadPrevious: Skipping video preload: ${photoToPreload.fileName}")
                continue
            }

            // Skip blacklisted paths — don't waste preload timeout on known-bad files
            if (blacklistedPaths.contains(photoToPreload.path)) continue

            // Only preload if not already in buffer or being loaded
            if (buffer.containsKey(photoToPreload.path) || preloadJobs.containsKey(photoToPreload.path)) {
                continue
            }

            // Start new preload job
            val job = scope.launch {
                // PRELOAD FIX: Use dynamic timeout instead of fixed 5s
                val result = try {
                    withTimeout(preloadTimeout) {
                        imageCache.load(photoToPreload.path)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timeout preloading ${photoToPreload.fileName} after ${preloadTimeout}ms, blacklisting")
                    blacklistPath(photoToPreload.path)
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
                        Log.d(TAG, "Preloaded photo -$i backward: ${photoToPreload.fileName}")
                    }
                    is Result.Error -> {
                        Log.w(TAG, "Failed to preload ${photoToPreload.fileName}, blacklisting: ${result.message}")
                        blacklistPath(photoToPreload.path)
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
     * Blacklists a photo path so it's skipped instantly on future load attempts.
     * Prevents the same problematic file from repeatedly consuming timeout budget.
     * Thread-safe: uses synchronized access on the set.
     */
    private fun blacklistPath(path: String) {
        synchronized(blacklistedPaths) {
            // Cap blacklist size to prevent unbounded growth
            if (blacklistedPaths.size >= MAX_BLACKLIST_SIZE) {
                val oldest = blacklistedPaths.first()
                blacklistedPaths.remove(oldest)
            }
            blacklistedPaths.add(path)
            Log.d(TAG, "Blacklisted path (${blacklistedPaths.size} total): ${path.substringAfterLast('/')}")
        }
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
        // Note: Do NOT call bitmap.recycle() — evicted bitmaps may still be referenced
        // by the ViewModel StateFlow / Compose rendering pipeline. The GC will free them
        // once all references are released (safe on API 26+).
        while (buffer.size > bufferSize) {
            val oldestKey = buffer.keys.first()
            buffer.remove(oldestKey)
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

            // Clear all bitmaps (GC will free memory when references are released)
            buffer.clear()

            // Clear blacklist — fresh start on re-initialization
            blacklistedPaths.clear()

            // Reset state
            photos = emptyList()
            currentIndex = -1
            _loadingState.value = BufferLoadingState.Idle
        }

        // Cancel managed scope and recreate it so it's usable after re-initialization
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    }

    /**
     * Removes SMB entries from the blacklist so the next photo load retries them.
     * Called on network recovery: previously unreachable SMB photos may now work.
     *
     * Thread Safety: Uses synchronized access on the set.
     */
    fun clearSmbBlacklist() {
        synchronized(blacklistedPaths) {
            val before = blacklistedPaths.size
            blacklistedPaths.removeAll { it.startsWith("smb://") }
            val removed = before - blacklistedPaths.size
            if (removed > 0) {
                Log.d(TAG, "Cleared $removed SMB entries from blacklist on network recovery")
            }
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

            // Keep only current photo in buffer (GC frees evicted bitmaps)
            val currentBitmap = currentPhotoPath?.let { buffer[it] }
            buffer.clear()
            if (currentBitmap != null && currentPhotoPath != null) {
                buffer[currentPhotoPath] = currentBitmap
            }

            Log.d(TAG, "Reduced buffer to minimum (1 photo) due to memory pressure")
        }
    }

    companion object {
        /** Minimum buffer size regardless of memory. */
        private const val MIN_BUFFER_SIZE = 5

        /** Maximum buffer size to avoid excessive memory usage. */
        private const val MAX_BUFFER_SIZE = 8

        /** Estimated bytes per downsampled photo (2560x1600 ARGB_8888). */
        private const val ESTIMATED_PHOTO_BYTES = 16L * 1024 * 1024 // 16MB

        /** Fraction of max heap to allocate for photo buffer. */
        private const val HEAP_FRACTION = 0.25

        /**
         * Preload timeout: 5 seconds per photo.
         * Fix #3: Prevents indefinite hangs during background preloading.
         */
        private const val PRELOAD_TIMEOUT_MS = 5_000L

        /** Maximum blacklist size to prevent unbounded memory growth. */
        private const val MAX_BLACKLIST_SIZE = 100

        private const val TAG = "PhotoBufferManager"

        /**
         * Calculates buffer size based on available device heap memory.
         * Allocates up to 50% of max heap for photo buffer.
         */
        private fun calculateBufferSize(): Int {
            val maxHeap = Runtime.getRuntime().maxMemory()
            val budgetBytes = (maxHeap * HEAP_FRACTION).toLong()
            val calculated = (budgetBytes / ESTIMATED_PHOTO_BYTES).toInt()
            val size = calculated.coerceIn(MIN_BUFFER_SIZE, MAX_BUFFER_SIZE)
            Log.d(TAG, "Buffer sizing: heap=${maxHeap / (1024*1024)}MB, " +
                "budget=${budgetBytes / (1024*1024)}MB, bufferSize=$size photos")
            return size
        }
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
