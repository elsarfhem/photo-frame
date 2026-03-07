package com.photoframe.core.repository

import android.graphics.Bitmap
import com.photoframe.core.data.SmbPhotoDataSource
import com.photoframe.core.database.PhotoDao
import com.photoframe.core.database.toEntity
import com.photoframe.core.database.toPhoto
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Photo
import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.slideshow.PhotoBufferManager
import com.photoframe.core.smb.SmbClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Implementation of SlideshowRepository.
 *
 * Architecture:
 * - Uses SmbPhotoDataSource to scan SMB share for photos
 * - Uses PhotoBufferManager to manage 4-photo buffer
 * - Provides reactive StateFlows for UI observation
 *
 * Thread Safety: All methods are thread-safe using Mutex protection.
 *
 * Retry Logic: Exponential backoff (2s, 4s, 8s) with max 3 retries.
 *
 * @param smbClient SMB client for connection management
 * @param smbPhotoDataSource Data source for scanning photos
 * @param photoBufferManager Buffer manager for photo pre-loading
 * @param settingsRepository Repository for accessing SMB connection settings
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
@Singleton
class SlideshowRepositoryImpl @Inject constructor(
    private val smbClient: SmbClient,
    private val smbPhotoDataSource: SmbPhotoDataSource,
    private val photoBufferManager: PhotoBufferManager,
    private val settingsRepository: SettingsRepository,
    private val photoDao: PhotoDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SlideshowRepository {

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

    // Current photo index
    private var currentIndex: Int = -1

    // Background scan job
    private var backgroundScanJob: Job? = null

    /**
     * Loads photos from database or SMB share.
     *
     * Strategy:
     * 1. Check database first (instant startup)
     * 2. If DB empty: Quick scan 100 photos from SMB
     * 3. Start slideshow immediately
     * 4. Background: Continue scanning and populate DB
     *
     * Thread Safety: Safe to call concurrently. Only one load operation at a time.
     */
    override suspend fun loadPhotos(shuffleEnabled: Boolean): Result<Int> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            _isLoading.value = true
            _error.value = null

            try {
                // Get SMB connection from settings
                val connectionResult = settingsRepository.loadSmbConnection()
                if (connectionResult !is Result.Success || connectionResult.data == null) {
                    _isLoading.value = false
                    _error.value = "No SMB connection configured"
                    return@withContext Result.error(
                        IllegalStateException("No SMB connection configured"),
                        "Please configure SMB connection in settings"
                    )
                }

                val connection = connectionResult.data
                val sourceId = connection.fullPath

                // Step 1: Check database for cached photos
                val cachedPhotos = photoDao.getPhotosForSource(sourceId).map { it.toPhoto() }

                if (cachedPhotos.isNotEmpty()) {
                    android.util.Log.d(TAG, "Found ${cachedPhotos.size} photos in database - instant startup!")

                    // Use cached photos immediately
                    var photoList = cachedPhotos
                    if (shuffleEnabled) {
                        photoList = fisherYatesShuffle(photoList)
                    }

                    _photos.value = photoList
                    currentIndex = 0

                    // Initialize buffer
                    val bufferResult = photoBufferManager.initialize(photoList, currentIndex)
                    if (bufferResult is Result.Success) {
                        val firstPhoto = photoBufferManager.getCurrentPhoto()
                        _currentPhoto.value = firstPhoto
                        _isLoading.value = false

                        // Start background sync to update DB
                        startBackgroundSync(connection, sourceId, shuffleEnabled)

                        return@withContext Result.success(photoList.size)
                    }
                }

                // Step 2: Database empty - need to scan SMB
                android.util.Log.d(TAG, "Database empty, scanning SMB share")

                // Get SMB password
                val passwordResult = settingsRepository.getSmbPassword()
                if (passwordResult !is Result.Success) {
                    _isLoading.value = false
                    _error.value = "Failed to retrieve SMB password"
                    return@withContext Result.error(
                        IllegalStateException("Failed to retrieve password"),
                        "Password not found"
                    )
                }

                val password = passwordResult.data

                // Connect to SMB share
                if (!smbClient.isConnected()) {
                    val connectResult = smbClient.connect(connection, password)
                    if (connectResult !is Result.Success) {
                        throw Exception("Failed to connect to SMB share")
                    }
                }

                // Quick start: Scan first 100 photos only
                val scanResult = smbPhotoDataSource.scanFolder(connection, maxPhotos = 100)
                when (scanResult) {
                    is Result.Success -> {
                        var photoList = scanResult.data

                        if (photoList.isEmpty()) {
                            _isLoading.value = false
                            _error.value = "No photos found in share"
                            return@withContext Result.error(
                                IllegalStateException("No photos found"),
                                "No photos found in ${connection.fullPath}"
                            )
                        }

                        // Shuffle if enabled
                        if (shuffleEnabled) {
                            photoList = fisherYatesShuffle(photoList)
                        }

                        // Update state
                        _photos.value = photoList
                        currentIndex = 0

                        // Initialize buffer
                        val bufferResult = photoBufferManager.initialize(photoList, currentIndex)
                        if (bufferResult !is Result.Success) {
                            throw Exception("Failed to initialize buffer")
                        }

                        // Get first photo
                        val firstPhoto = photoBufferManager.getCurrentPhoto()
                        _currentPhoto.value = firstPhoto
                        _isLoading.value = false

                        // Start background scan to populate DB
                        startBackgroundScanAndSave(connection, sourceId, photoList, shuffleEnabled)

                        return@withContext Result.success(photoList.size)
                    }
                    is Result.Error -> {
                        throw scanResult.exception
                    }
                    is Result.Loading -> {
                        throw IllegalStateException("Unexpected loading state")
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to load photos: ${e.message}"
                android.util.Log.e(TAG, "Failed to load photos", e)
                return@withContext Result.error(e, "Failed to load photos: ${e.message}")
            }
        }
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
                val (exception, errorMsg) = if (bufferResult is Result.Error) {
                    bufferResult.exception to (bufferResult.message ?: bufferResult.exception.message)
                } else {
                    Exception("Unknown error") to "Unknown error"
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

    /**
     * Advances to next photo.
     *
     * Thread Safety: Safe to call concurrently.
     */
    override suspend fun nextPhoto(): Result<Bitmap> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            if (currentPhotos.isEmpty()) {
                return@withContext Result.error(
                    IllegalStateException("No photos loaded"),
                    "Load photos before navigating"
                )
            }

            // Get next photo from buffer
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

    /**
     * Goes back to previous photo.
     *
     * Thread Safety: Safe to call concurrently.
     */
    override suspend fun previousPhoto(): Result<Bitmap> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            if (currentPhotos.isEmpty()) {
                return@withContext Result.error(
                    IllegalStateException("No photos loaded"),
                    "Load photos before navigating"
                )
            }

            // Get previous photo from buffer
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

    /**
     * Gets current photo metadata.
     *
     * Thread Safety: Safe to call concurrently.
     */
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

    /**
     * Gets current photo index.
     *
     * Thread Safety: Safe to call concurrently.
     */
    override suspend fun getCurrentPhotoIndex(): Int = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            currentIndex
        }
    }

    /**
     * Clears photo list and buffer.
     *
     * Thread Safety: Safe to call concurrently.
     */
    override suspend fun clear() = withContext(ioDispatcher) {
        mutex.withLock {
            // Cancel background scan
            backgroundScanJob?.cancel()
            backgroundScanJob = null

            _photos.value = emptyList()
            _currentPhoto.value = null
            _isLoading.value = false
            _error.value = null
            currentIndex = -1

            photoBufferManager.clear()
        }
    }

    /**
     * Starts background scan after quick start.
     * Progressively scans SMB and saves to database in batches.
     *
     * @param connection SMB connection configuration
     * @param sourceId Source identifier for database
     * @param initialPhotos Already loaded photos from quick start
     * @param shuffleEnabled Whether to shuffle newly added photos
     */
    private fun startBackgroundScanAndSave(
        connection: SmbConnection,
        sourceId: String,
        initialPhotos: List<Photo>,
        shuffleEnabled: Boolean
    ) {
        backgroundScanJob?.cancel()

        backgroundScanJob = CoroutineScope(ioDispatcher).launch {
            try {
                android.util.Log.d(TAG, "Background scan: Starting progressive scan and save to DB")

                // Scan all photos (no limit)
                val fullScanResult = smbPhotoDataSource.scanFolder(connection, maxPhotos = null)
                when (fullScanResult) {
                    is Result.Success -> {
                        val allPhotos = fullScanResult.data
                        android.util.Log.d(TAG, "Background scan: Found ${allPhotos.size} total photos")

                        // Save all photos to database
                        val entities = allPhotos.map { it.toEntity(sourceId) }
                        photoDao.insertPhotos(entities)
                        android.util.Log.d(TAG, "Background scan: Saved ${entities.size} photos to database")

                        // Find new photos not in initial batch
                        val initialPaths = initialPhotos.map { it.path }.toSet()
                        val newPhotos = allPhotos.filter { it.path !in initialPaths }

                        if (newPhotos.isNotEmpty()) {
                            android.util.Log.d(TAG, "Background scan: Adding ${newPhotos.size} new photos to slideshow")

                            // Shuffle new photos if enabled
                            val newPhotosToAdd = if (shuffleEnabled) {
                                fisherYatesShuffle(newPhotos)
                            } else {
                                newPhotos
                            }

                            // Merge with existing photos
                            mutex.withLock {
                                val currentPhotos = _photos.value
                                val mergedPhotos = currentPhotos + newPhotosToAdd
                                _photos.value = mergedPhotos
                                android.util.Log.d(TAG, "Background scan: Updated photo list to ${mergedPhotos.size} photos")
                            }
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e(TAG, "Background scan: Failed - ${fullScanResult.message}", fullScanResult.exception)
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Background scan: Exception", e)
            }
        }
    }

    /**
     * Starts background sync to refresh database.
     * Used when database already has photos.
     *
     * @param connection SMB connection configuration
     * @param sourceId Source identifier for database
     * @param shuffleEnabled Whether to shuffle newly added photos
     */
    private fun startBackgroundSync(
        connection: SmbConnection,
        sourceId: String,
        shuffleEnabled: Boolean
    ) {
        backgroundScanJob?.cancel()

        backgroundScanJob = CoroutineScope(ioDispatcher).launch {
            try {
                android.util.Log.d(TAG, "Background sync: Refreshing database")

                // Get current photos from memory
                val currentPhotos = mutex.withLock { _photos.value }
                val currentPaths = currentPhotos.map { it.path }.toSet()

                // Scan SMB for changes
                val fullScanResult = smbPhotoDataSource.scanFolder(connection, maxPhotos = null)
                when (fullScanResult) {
                    is Result.Success -> {
                        val allPhotos = fullScanResult.data
                        android.util.Log.d(TAG, "Background sync: Found ${allPhotos.size} photos on SMB")

                        // Update database
                        val entities = allPhotos.map { it.toEntity(sourceId) }
                        photoDao.deletePhotosForSource(sourceId)
                        photoDao.insertPhotos(entities)
                        android.util.Log.d(TAG, "Background sync: Updated database")

                        // Find new photos
                        val newPhotos = allPhotos.filter { it.path !in currentPaths }

                        if (newPhotos.isNotEmpty()) {
                            android.util.Log.d(TAG, "Background sync: Adding ${newPhotos.size} new photos")

                            val newPhotosToAdd = if (shuffleEnabled) {
                                fisherYatesShuffle(newPhotos)
                            } else {
                                newPhotos
                            }

                            mutex.withLock {
                                val mergedPhotos = _photos.value + newPhotosToAdd
                                _photos.value = mergedPhotos
                                android.util.Log.d(TAG, "Background sync: Updated to ${mergedPhotos.size} photos")
                            }
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e(TAG, "Background sync: Failed - ${fullScanResult.message}", fullScanResult.exception)
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Background sync: Exception", e)
            }
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
        private const val TAG = "SlideshowRepositoryImpl"
        /**
         * Maximum number of retry attempts for loading photos.
         */
        private const val MAX_RETRIES = 4 // Initial + 3 retries

        /**
         * Initial retry delay: 2 seconds.
         * Doubles with each retry (exponential backoff).
         */
        private const val INITIAL_RETRY_DELAY_MS = 2000L
    }
}
