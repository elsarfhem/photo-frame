package com.photoframe.core.repository

import android.graphics.Bitmap
import android.util.Log
import com.photoframe.core.data.PhotoSourcesManager
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Photo
import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.Result
import com.photoframe.core.slideshow.PhotoBufferManager
import com.photoframe.core.source.PhotoSource
import com.photoframe.core.source.PhotoSourceFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Multi-source photo repository implementation.
 *
 * Aggregates photos from multiple sources (SMB, local, etc.) and provides
 * a unified interface for slideshow display.
 *
 * Features:
 * - Parallel scanning of multiple sources
 * - Random mix aggregation across all sources
 * - Isolated error handling (one source failure doesn't block others)
 * - Dynamic source management (add/remove/update)
 *
 * Thread Safety: All methods are thread-safe using Mutex protection.
 *
 * @param photoSourcesManager Manager for source configurations
 * @param photoSourceFactory Factory for creating PhotoSource instances
 * @param photoBufferManager Buffer manager for photo pre-loading
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
@Singleton
class MultiSourcePhotoRepositoryImpl @Inject constructor(
    private val photoSourcesManager: PhotoSourcesManager,
    private val photoSourceFactory: PhotoSourceFactory,
    private val photoBufferManager: PhotoBufferManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MultiSourcePhotoRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()

    // Photo list state
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    override val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    // Current photo bitmap state
    private val _currentPhoto = MutableStateFlow<Bitmap?>(null)
    override val currentPhoto: StateFlow<Bitmap?> = _currentPhoto.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    // Photo sources state - convert Flow to StateFlow
    override val photoSources: StateFlow<List<PhotoSourceConfig>> =
        photoSourcesManager.sources.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Current photo index
    private var currentIndex: Int = -1

    /**
     * Loads photos from all enabled sources.
     *
     * Strategy:
     * 1. Get all enabled sources
     * 2. Create PhotoSource instances
     * 3. Scan sources in parallel
     * 4. Aggregate results
     * 5. Shuffle if enabled (Fisher-Yates)
     * 6. Initialize buffer
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param shuffleEnabled If true, shuffles aggregated photos
     * @return Result.Success with total photo count, Result.Error if failed
     */
    override suspend fun loadPhotos(shuffleEnabled: Boolean): Result<Int> = withContext(ioDispatcher) {
        Log.d(TAG, "loadPhotos: Starting photo load (shuffle=$shuffleEnabled)")
        return@withContext mutex.withLock {
            _isLoading.value = true
            _error.value = null

            try {
                // Get enabled source configs
                val sourceConfigs = photoSourcesManager.getEnabledSources()
                Log.d(TAG, "loadPhotos: Found ${sourceConfigs.size} enabled source(s)")

                sourceConfigs.forEachIndexed { index, config ->
                    Log.d(TAG, "loadPhotos: Source $index: ${config.displayName} (type=${config.type}, enabled=${config.isEnabled})")
                }

                if (sourceConfigs.isEmpty()) {
                    Log.w(TAG, "loadPhotos: No sources configured!")
                    _isLoading.value = false
                    _error.value = "No photo sources configured"
                    return@withContext Result.error(
                        IllegalStateException("No sources"),
                        "No photo sources configured. Add a source in settings."
                    )
                }

                // Scan all sources in parallel
                Log.d(TAG, "loadPhotos: Starting parallel source scanning...")
                val allPhotos = scanSourcesInParallel(sourceConfigs)
                Log.d(TAG, "loadPhotos: Parallel scan complete - Total photos: ${allPhotos.size}")

                if (allPhotos.isEmpty()) {
                    Log.w(TAG, "loadPhotos: No photos found in any source!")
                    _isLoading.value = false
                    _error.value = "No photos found in any source"
                    return@withContext Result.error(
                        IllegalStateException("No photos found"),
                        "No photos found in enabled sources"
                    )
                }

                // Shuffle if enabled (random mix across all sources)
                val finalPhotos = if (shuffleEnabled) {
                    fisherYatesShuffle(allPhotos)
                } else {
                    allPhotos
                }

                // Update state
                _photos.value = finalPhotos
                currentIndex = 0

                // Initialize buffer
                val bufferResult = photoBufferManager.initialize(finalPhotos, currentIndex)
                if (bufferResult !is Result.Success) {
                    val errorMsg = if (bufferResult is Result.Error) {
                        bufferResult.message ?: bufferResult.exception.message
                    } else {
                        "Unknown error"
                    }
                    throw Exception("Failed to initialize buffer: $errorMsg")
                }

                // Get first photo
                val firstPhoto = photoBufferManager.getCurrentPhoto()
                _currentPhoto.value = firstPhoto

                _isLoading.value = false
                _error.value = null

                Result.success(finalPhotos.size)
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to load photos: ${e.message}"
                Result.error(e, "Failed to load photos from sources")
            }
        }
    }

    /**
     * Scans multiple sources in parallel.
     *
     * Uses coroutines to scan sources concurrently for better performance.
     * Isolated error handling - one source failure doesn't block others.
     *
     * @param sourceConfigs List of source configurations to scan
     * @return Aggregated list of photos from all successful sources
     */
    private suspend fun scanSourcesInParallel(
        sourceConfigs: List<PhotoSourceConfig>
    ): List<Photo> = coroutineScope {
        Log.d(TAG, "scanSourcesInParallel: Scanning ${sourceConfigs.size} source(s) in parallel...")

        // Create source instances and scan in parallel
        val scanResults = sourceConfigs.map { config ->
            async(ioDispatcher) {
                try {
                    Log.d(TAG, "scanSourcesInParallel: Creating source '${config.displayName}'...")
                    // Create source instance
                    val sourceResult = photoSourceFactory.createSource(config)
                    if (sourceResult !is Result.Success) {
                        val errorMsg = if (sourceResult is Result.Error) {
                            sourceResult.message ?: "Unknown error"
                        } else {
                            "Factory returned non-success result"
                        }
                        Log.e(TAG, "scanSourcesInParallel: FAILED to create source '${config.displayName}': $errorMsg")
                        return@async emptyList<Photo>()
                    }

                    val source = sourceResult.data as PhotoSource
                    Log.d(TAG, "scanSourcesInParallel: Source '${config.displayName}' created, starting scan...")

                    // Scan photos
                    val scanResult = source.scanPhotos()
                    when (scanResult) {
                        is Result.Success -> {
                            Log.d(TAG, "scanSourcesInParallel: Source '${config.displayName}' scan SUCCESS - ${scanResult.data.size} photos")
                            scanResult.data
                        }
                        is Result.Error -> {
                            // Log error but continue with other sources
                            Log.e(TAG, "scanSourcesInParallel: Source '${config.displayName}' scan FAILED: ${scanResult.message}", scanResult.exception)
                            emptyList()
                        }
                        is Result.Loading -> {
                            Log.w(TAG, "scanSourcesInParallel: Source '${config.displayName}' returned Loading state")
                            emptyList()
                        }
                    }
                } catch (e: Exception) {
                    // Source failed - return empty list (don't fail entire scan)
                    Log.e(TAG, "scanSourcesInParallel: EXCEPTION scanning source '${config.displayName}'", e)
                    emptyList()
                }
            }
        }

        // Await all scans and flatten results
        val results = scanResults.awaitAll()
        results.flatten()
    }

    /**
     * Shuffles photos using Fisher-Yates algorithm.
     *
     * Thread Safety: Safe to call concurrently.
     */
    override suspend fun shufflePhotos(): Result<Int> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            if (currentPhotos.isEmpty()) {
                return@withContext Result.error(
                    IllegalStateException("No photos loaded"),
                    "Load photos before shuffling"
                )
            }

            // Get current photo to maintain position
            val currentPhoto = if (currentIndex in currentPhotos.indices) {
                currentPhotos[currentIndex]
            } else {
                null
            }

            // Shuffle using Fisher-Yates
            val shuffled = fisherYatesShuffle(currentPhotos)
            _photos.value = shuffled

            // Find current photo in shuffled list
            if (currentPhoto != null) {
                currentIndex = shuffled.indexOfFirst { it.path == currentPhoto.path }
                if (currentIndex == -1) currentIndex = 0
            } else {
                currentIndex = 0
            }

            // Re-initialize buffer with shuffled list
            val bufferResult = photoBufferManager.initialize(shuffled, currentIndex)
            if (bufferResult !is Result.Success) {
                val errorMsg = if (bufferResult is Result.Error) {
                    bufferResult.message ?: bufferResult.exception.message
                } else {
                    "Unknown error"
                }
                val exception = if (bufferResult is Result.Error) {
                    bufferResult.exception
                } else {
                    Exception("Unknown error")
                }
                return@withContext Result.error(
                    exception,
                    "Failed to re-initialize buffer: $errorMsg"
                )
            }

            // Update current photo bitmap
            val bitmap = photoBufferManager.getCurrentPhoto()
            _currentPhoto.value = bitmap

            Result.success(shuffled.size)
        }
    }

    override suspend fun nextPhoto(): Result<Bitmap> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            if (currentPhotos.isEmpty()) {
                return@withContext Result.error(
                    IllegalStateException("No photos loaded"),
                    "Load photos before navigating"
                )
            }

            val result = photoBufferManager.getNextPhoto()
            when (result) {
                is Result.Success -> {
                    currentIndex = (currentIndex + 1) % currentPhotos.size
                    _currentPhoto.value = result.data
                    _error.value = null
                    Result.success(result.data)
                }
                is Result.Error -> {
                    _error.value = "Failed to load next photo: ${result.message}"
                    Result.error(result.exception, result.message ?: "Failed to load next photo")
                }
                is Result.Loading -> {
                    Result.error(
                        IllegalStateException("Unexpected loading state"),
                        "Buffer returned loading state"
                    )
                }
            }
        }
    }

    override suspend fun previousPhoto(): Result<Bitmap> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            if (currentPhotos.isEmpty()) {
                return@withContext Result.error(
                    IllegalStateException("No photos loaded"),
                    "Load photos before navigating"
                )
            }

            val result = photoBufferManager.getPreviousPhoto()
            when (result) {
                is Result.Success -> {
                    currentIndex = if (currentIndex == 0) currentPhotos.size - 1 else currentIndex - 1
                    _currentPhoto.value = result.data
                    _error.value = null
                    Result.success(result.data)
                }
                is Result.Error -> {
                    _error.value = "Failed to load previous photo: ${result.message}"
                    Result.error(result.exception, result.message ?: "Failed to load previous photo")
                }
                is Result.Loading -> {
                    Result.error(
                        IllegalStateException("Unexpected loading state"),
                        "Buffer returned loading state"
                    )
                }
            }
        }
    }

    override suspend fun getCurrentPhotoMetadata(): Photo? = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            if (currentIndex in currentPhotos.indices) {
                currentPhotos[currentIndex]
            } else {
                null
            }
        }
    }

    override suspend fun getCurrentPhotoIndex(): Int = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            currentIndex
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        mutex.withLock {
            _photos.value = emptyList()
            _currentPhoto.value = null
            _isLoading.value = false
            _error.value = null
            currentIndex = -1

            photoBufferManager.clear()
        }
    }

    // Multi-source specific methods

    override suspend fun addPhotoSource(source: PhotoSourceConfig): Result<Unit> {
        return photoSourcesManager.addSource(source)
    }

    override suspend fun removePhotoSource(sourceId: String): Result<Unit> {
        return photoSourcesManager.removeSource(sourceId)
    }

    override suspend fun updatePhotoSource(source: PhotoSourceConfig): Result<Unit> {
        return photoSourcesManager.updateSource(source)
    }

    override suspend fun setSourceEnabled(sourceId: String, enabled: Boolean): Result<Unit> {
        return photoSourcesManager.setSourceEnabled(sourceId, enabled)
    }

    override suspend fun validateSource(sourceId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext try {
            // Get source config
            val configResult = photoSourcesManager.getSource(sourceId)
            if (configResult !is Result.Success) {
                return@withContext Result.error(
                    IllegalArgumentException("Source not found"),
                    "Source '$sourceId' does not exist"
                )
            }

            val config = configResult.data

            // Create source instance
            val sourceResult = photoSourceFactory.createSource(config)
            if (sourceResult !is Result.Success) {
                val errorMsg = if (sourceResult is Result.Error) {
                    sourceResult.message ?: "Failed to create source"
                } else {
                    "Failed to create source"
                }
                val exception = if (sourceResult is Result.Error) {
                    sourceResult.exception
                } else {
                    Exception("Failed to create source")
                }
                return@withContext Result.error(exception, errorMsg)
            }

            val source = sourceResult.data as PhotoSource

            // Validate
            source.validate()
        } catch (e: Exception) {
            Result.error(e, "Validation failed: ${e.message}")
        }
    }

    /**
     * Fisher-Yates shuffle algorithm for unbiased randomization.
     *
     * Time Complexity: O(n)
     * Space Complexity: O(n) - creates new list
     *
     * @param list List to shuffle
     * @return New shuffled list
     */
    private fun <T> fisherYatesShuffle(list: List<T>): List<T> {
        val mutableList = list.toMutableList()
        val size = mutableList.size

        for (i in size - 1 downTo 1) {
            val j = Random.nextInt(i + 1)
            // Swap elements
            val temp = mutableList[i]
            mutableList[i] = mutableList[j]
            mutableList[j] = temp
        }

        return mutableList
    }

    companion object {
        private const val TAG = "MultiSourcePhotoRepo"
    }
}
