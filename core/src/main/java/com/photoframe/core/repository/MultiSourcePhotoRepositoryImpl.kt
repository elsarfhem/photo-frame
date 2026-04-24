package com.photoframe.core.repository

import android.graphics.Bitmap
import android.util.Log
import com.photoframe.core.data.PhotoSourcesManager
import com.photoframe.core.database.PhotoDao
import com.photoframe.core.database.toEntity
import com.photoframe.core.database.toPhoto
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Photo
import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.model.SourceConfig
import com.photoframe.core.network.NetworkMonitor
import com.photoframe.core.security.CredentialStore
import com.photoframe.core.slideshow.PhotoBufferManager
import com.photoframe.core.smb.SmbClient
import com.photoframe.core.source.PhotoSource
import com.photoframe.core.source.PhotoSourceFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val photoDao: PhotoDao,
    private val smbClient: SmbClient,
    private val credentialStore: CredentialStore,
    private val networkMonitor: NetworkMonitor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MultiSourcePhotoRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()

    // Background scan job
    private var backgroundScanJob: Job? = null

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

    // Current photo metadata state
    private val _currentPhotoMetadata = MutableStateFlow<Photo?>(null)
    override val currentPhotoMetadata: StateFlow<Photo?> = _currentPhotoMetadata.asStateFlow()

    // Current photo index state
    private val _currentPhotoIndex = MutableStateFlow(-1)
    override val currentPhotoIndex: StateFlow<Int> = _currentPhotoIndex.asStateFlow()

    // Photo sources state - convert Flow to StateFlow
    override val photoSources: StateFlow<List<PhotoSourceConfig>> =
        photoSourcesManager.sources.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Current photo index (internal tracking)
    private var currentIndex: Int = -1

    /**
     * Loads photos from database cache or scans sources.
     *
     * Strategy:
     * 1. Check database cache for all enabled sources (instant startup)
     * 2. If all cached: Use cached photos immediately
     * 3. If any missing: Scan only missing sources from network
     * 4. Save scanned photos to database
     * 5. Start background sync to refresh all sources
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

                // Step 1: Check database cache for all sources
                val cachedPhotosMap = mutableMapOf<String, List<Photo>>()
                val sourcesToScan = mutableListOf<PhotoSourceConfig>()

                for (config in sourceConfigs) {
                    val sourceId = config.id
                    val cachedPhotos = photoDao.getPhotosForSource(sourceId).map { it.toPhoto() }

                    if (cachedPhotos.isNotEmpty()) {
                        Log.d(TAG, "loadPhotos: Found ${cachedPhotos.size} cached photos for source '${config.displayName}'")
                        cachedPhotosMap[sourceId] = cachedPhotos
                    } else {
                        Log.d(TAG, "loadPhotos: No cache for source '${config.displayName}' - will scan")
                        sourcesToScan.add(config)
                    }
                }

                val allCachedPhotos = cachedPhotosMap.values.flatten()

                // Step 2: If we have cached photos, use them for instant startup
                if (allCachedPhotos.isNotEmpty()) {
                    Log.d(TAG, "loadPhotos: Found ${allCachedPhotos.size} total cached photos - instant startup!")

                    // CRITICAL: Connect all SMB sources before loading photos
                    // This ensures SmbFetcher can load photos immediately without "Not connected" errors
                    Log.d(TAG, "=== COLD STARTUP: Connecting SMB sources before loading...")
                    for (config in sourceConfigs) {
                        if (config.type == PhotoSourceType.SMB && config.config is SourceConfig.SmbConfig) {
                            try {
                                val smbConfig = config.config
                                
                                // Build SMB connection
                                val serverUrl = "smb://${smbConfig.server}/${smbConfig.share}"
                                val connection = SmbConnection(
                                    serverUrl = serverUrl,
                                    sharePath = smbConfig.path,
                                    username = smbConfig.username,
                                    domain = smbConfig.domain
                                )
                                
                                // Get password from credential store
                                val credentialKey = "photo_source_${config.id}"
                                val passwordResult = credentialStore.retrievePassword(credentialKey)
                                
                                if (passwordResult is Result.Success) {
                                    val password = passwordResult.data
                                    
                                    // Connect if not already connected
                                    if (!smbClient.isConnected()) {
                                        Log.d(TAG, "=== COLD STARTUP: Connecting to '${config.displayName}'...")
                                        val connectResult = smbClient.connect(connection, password)
                                        when (connectResult) {
                                            is Result.Success -> {
                                                Log.d(TAG, "=== COLD STARTUP: Connected to '${config.displayName}' successfully")
                                            }
                                            is Result.Error -> {
                                                Log.w(TAG, "=== COLD STARTUP: Failed to connect to '${config.displayName}': ${connectResult.message}")
                                            }
                                            is Result.Loading -> {}
                                        }
                                    } else {
                                        Log.d(TAG, "=== COLD STARTUP: Already connected (reusing connection)")
                                    }
                                } else {
                                    Log.w(TAG, "=== COLD STARTUP: Password not found for '${config.displayName}'")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "=== COLD STARTUP: Exception connecting '${config.displayName}'", e)
                            }
                        }
                    }
                    Log.d(TAG, "=== COLD STARTUP: SMB connection phase complete")

                    var photoList = allCachedPhotos
                    if (shuffleEnabled) {
                        photoList = fisherYatesShuffle(photoList)
                    }

                    // When network is unavailable, prioritize local photos at the front so the
                    // buffer can initialize successfully even if SMB photos dominate the list.
                    if (!networkMonitor.isNetworkAvailable.value) {
                        val (local, remote) = photoList.partition { !it.path.startsWith("smb://") }
                        if (local.isNotEmpty()) {
                            photoList = local + remote
                            Log.d(TAG, "loadPhotos: Offline mode — reordered ${local.size} local photos to front")
                        }
                    }

                    // Initialize buffer BEFORE setting _photos to prevent state desync
                    currentIndex = 0
                    val bufferResult = photoBufferManager.initialize(photoList, currentIndex)
                    if (bufferResult is Result.Success) {
                        // Only set _photos AFTER buffer initialization succeeds
                        _photos.value = photoList

                        // FIX 3: Subscribe buffer to photo updates to prevent stale list desynchronization
                        photoBufferManager.subscribeToPhotoUpdates(_photos)

                        val firstPhoto = photoBufferManager.getCurrentPhoto()
                        _currentPhoto.value = firstPhoto
                        _currentPhotoMetadata.value = photoList.getOrNull(currentIndex)
                        _currentPhotoIndex.value = currentIndex
                        _isLoading.value = false

                        // Start background sync to refresh cache and scan missing sources
                        startBackgroundSync(sourceConfigs, shuffleEnabled)

                        return@withContext Result.success(photoList.size)
                    } else {
                        // Buffer init failed - clear state and fall through to scan missing sources
                        Log.w(TAG, "loadPhotos: Buffer initialization failed for cached photos, will scan sources")
                        _photos.value = emptyList()
                        currentIndex = -1
                    }
                }

                // Step 3: No cache - do a quick scan first (limited photos) to start
                // slideshow fast, then complete the full scan in background.
                Log.d(TAG, "loadPhotos: Quick scan for fast startup, then full scan in background")

                if (sourcesToScan.isEmpty()) {
                    Log.w(TAG, "loadPhotos: No sources to scan and no cached photos!")
                    _isLoading.value = false
                    _error.value = "No photos found in any source"
                    return@withContext Result.error(
                        IllegalStateException("No photos found"),
                        "No photos found in enabled sources"
                    )
                }

                // Quick scan: get first QUICK_SCAN_LIMIT photos to start slideshow fast
                val quickPhotos = scanSourcesInParallel(sourcesToScan, saveToDatabase = false, maxPhotos = QUICK_SCAN_LIMIT)

                if (quickPhotos.isEmpty()) {
                    // No photos found even in quick scan - do full scan as fallback
                    Log.w(TAG, "loadPhotos: Quick scan found nothing, trying full scan...")
                    val fullPhotos = scanSourcesInParallel(sourcesToScan, saveToDatabase = true)
                    if (fullPhotos.isEmpty()) {
                        _isLoading.value = false
                        _error.value = "No photos found in any source"
                        return@withContext Result.error(
                            IllegalStateException("No photos found"),
                            "No photos found in enabled sources"
                        )
                    }
                    val finalPhotos = if (shuffleEnabled) fisherYatesShuffle(fullPhotos) else fullPhotos
                    currentIndex = 0
                    val bufferResult = photoBufferManager.initialize(finalPhotos, currentIndex)
                    if (bufferResult !is Result.Success) throw Exception("Failed to initialize buffer")
                    _photos.value = finalPhotos
                    _currentPhoto.value = photoBufferManager.getCurrentPhoto()
                    _currentPhotoMetadata.value = finalPhotos.getOrNull(currentIndex)
                    _currentPhotoIndex.value = currentIndex
                    _isLoading.value = false
                    _error.value = null
                    return@withContext Result.success(finalPhotos.size)
                }

                // Start slideshow immediately with quick-scan results
                val initialPhotos = if (shuffleEnabled) fisherYatesShuffle(quickPhotos) else quickPhotos
                Log.d(TAG, "loadPhotos: Starting slideshow with ${initialPhotos.size} photos (quick scan)")
                currentIndex = 0
                val bufferResult = photoBufferManager.initialize(initialPhotos, currentIndex)
                if (bufferResult !is Result.Success) throw Exception("Failed to initialize buffer")
                _photos.value = initialPhotos
                _currentPhoto.value = photoBufferManager.getCurrentPhoto()
                _currentPhotoMetadata.value = initialPhotos.getOrNull(currentIndex)
                _currentPhotoIndex.value = currentIndex
                _isLoading.value = false
                _error.value = null

                // Launch full scan in background to discover all remaining photos
                startBackgroundFullScan(sourceConfigs, shuffleEnabled)

                Result.success(initialPhotos.size)
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to load photos: ${e.message}"
                Log.e(TAG, "Failed to load photos", e)
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
     * @param saveToDatabase If true, saves scanned photos to database cache
     * @return Aggregated list of photos from all successful sources
     */
    private suspend fun scanSourcesInParallel(
        sourceConfigs: List<PhotoSourceConfig>,
        saveToDatabase: Boolean = false,
        maxPhotos: Int? = null
    ): List<Photo> = coroutineScope {
        Log.d(TAG, "scanSourcesInParallel: Scanning ${sourceConfigs.size} source(s) in parallel (saveToDb=$saveToDatabase, maxPhotos=$maxPhotos)")

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
                    val scanResult = source.scanPhotos(maxPhotos)
                    when (scanResult) {
                        is Result.Success -> {
                            val photos = scanResult.data
                            Log.d(TAG, "scanSourcesInParallel: Source '${config.displayName}' scan SUCCESS - ${photos.size} photos")

                            // Save to database if requested
                            if (saveToDatabase && photos.isNotEmpty()) {
                                try {
                                    val entities = photos.map { it.toEntity(config.id) }
                                    photoDao.insertPhotos(entities)
                                    Log.d(TAG, "scanSourcesInParallel: Saved ${entities.size} photos to database for source '${config.displayName}'")
                                } catch (e: Exception) {
                                    Log.e(TAG, "scanSourcesInParallel: Failed to save to database for source '${config.displayName}'", e)
                                }
                            }

                            photos
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

            // Update current photo bitmap and metadata atomically
            val bitmap = photoBufferManager.getCurrentPhoto()
            _currentPhoto.value = bitmap
            _currentPhotoMetadata.value = shuffled.getOrNull(currentIndex)
            _currentPhotoIndex.value = currentIndex

            Result.success(shuffled.size)
        }
    }

    override suspend fun nextPhoto(displayIntervalMs: Long): Result<Bitmap?> = withContext(ioDispatcher) {
        // Check precondition under mutex (fast)
        mutex.withLock {
            if (_photos.value.isEmpty()) {
                return@withContext Result.error(
                    IllegalStateException("No photos loaded"),
                    "Load photos before navigating"
                )
            }
        }

        // Load photo WITHOUT holding repo mutex (slow I/O with timeout)
        val result = photoBufferManager.getNextPhoto(displayIntervalMs)

        // Update state under mutex (fast)
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            when (result) {
                is Result.Success -> {
                    currentIndex = photoBufferManager.getCurrentIndex()
                    _currentPhoto.value = result.data
                    _currentPhotoMetadata.value = currentPhotos.getOrNull(currentIndex)
                    _currentPhotoIndex.value = currentIndex
                    _error.value = null
                    Result.success(result.data)
                }
                is Result.Error -> {
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

    override suspend fun previousPhoto(displayIntervalMs: Long): Result<Bitmap?> = withContext(ioDispatcher) {
        // Check precondition under mutex (fast)
        mutex.withLock {
            if (_photos.value.isEmpty()) {
                return@withContext Result.error(
                    IllegalStateException("No photos loaded"),
                    "Load photos before navigating"
                )
            }
        }

        // Load photo WITHOUT holding repo mutex (slow I/O with timeout)
        val result = photoBufferManager.getPreviousPhoto(displayIntervalMs)

        // Update state under mutex (fast)
        return@withContext mutex.withLock {
            val currentPhotos = _photos.value
            when (result) {
                is Result.Success -> {
                    currentIndex = photoBufferManager.getCurrentIndex()
                    _currentPhoto.value = result.data
                    _currentPhotoMetadata.value = currentPhotos.getOrNull(currentIndex)
                    _currentPhotoIndex.value = currentIndex
                    _error.value = null
                    Result.success(result.data)
                }
                is Result.Error -> {
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
     * Starts background sync to refresh database cache.
     * Rescans all enabled sources and updates database.
     *
     * @param sourceConfigs List of source configurations to sync
     * @param shuffleEnabled Whether to shuffle newly discovered photos
     */
    private fun startBackgroundSync(
        sourceConfigs: List<PhotoSourceConfig>,
        shuffleEnabled: Boolean
    ) {
        backgroundScanJob?.cancel()

        backgroundScanJob = CoroutineScope(ioDispatcher).launch {
            try {
                Log.d(TAG, "Background sync: Starting refresh for ${sourceConfigs.size} source(s)")

                // Get current photos from memory
                val currentPhotos = mutex.withLock { _photos.value }
                val currentPaths = currentPhotos.map { it.path }.toSet()

                // Scan all sources in parallel
                val allNewPhotos = mutableListOf<Photo>()

                for (config in sourceConfigs) {
                    try {
                        Log.d(TAG, "Background sync: Scanning source '${config.displayName}'")

                        // Create source instance
                        val sourceResult = photoSourceFactory.createSource(config)
                        if (sourceResult !is Result.Success) {
                            Log.e(TAG, "Background sync: Failed to create source '${config.displayName}'")
                            continue
                        }

                        val source = sourceResult.data as PhotoSource

                        // Scan photos
                        val scanResult = source.scanPhotos()
                        when (scanResult) {
                            is Result.Success -> {
                                val photos = scanResult.data
                                Log.d(TAG, "Background sync: Source '${config.displayName}' - ${photos.size} photos")

                                // Update database
                                val entities = photos.map { it.toEntity(config.id) }
                                photoDao.deletePhotosForSource(config.id)
                                photoDao.insertPhotos(entities)
                                Log.d(TAG, "Background sync: Updated database for source '${config.displayName}'")

                                // Find new photos not in current list
                                val newPhotos = photos.filter { it.path !in currentPaths }
                                allNewPhotos.addAll(newPhotos)
                            }
                            is Result.Error -> {
                                Log.e(TAG, "Background sync: Source '${config.displayName}' failed: ${scanResult.message}", scanResult.exception)
                            }
                            is Result.Loading -> {
                                // Should not happen
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Background sync: Exception for source '${config.displayName}'", e)
                    }
                }

                // Add new photos to slideshow if any found
                if (allNewPhotos.isNotEmpty()) {
                    Log.d(TAG, "Background sync: Adding ${allNewPhotos.size} new photos to slideshow")

                    val newPhotosToAdd = if (shuffleEnabled) {
                        fisherYatesShuffle(allNewPhotos)
                    } else {
                        allNewPhotos
                    }

                    // Merge under mutex but emit OUTSIDE to prevent lock inversion.
                    // StateFlow emission triggers PhotoBufferManager.syncPhotoList() which
                    // acquires buffer mutex — emitting inside repo mutex creates deadlock risk.
                    val mergedPhotos = mutex.withLock {
                        _photos.value + newPhotosToAdd
                    }
                    _photos.value = mergedPhotos
                    Log.d(TAG, "Background sync: Updated photo list to ${mergedPhotos.size} photos")
                } else {
                    Log.d(TAG, "Background sync: No new photos discovered")
                }

                Log.d(TAG, "Background sync: Complete")
            } catch (e: Exception) {
                Log.e(TAG, "Background sync: Exception", e)
            }
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

    /**
     * Launches a full background scan after the quick scan started the slideshow.
     * Scans all sources without limit, saves to DB, and merges new photos into the running slideshow.
     */
    private fun startBackgroundFullScan(
        sourceConfigs: List<PhotoSourceConfig>,
        shuffleEnabled: Boolean
    ) {
        backgroundScanJob?.cancel()

        backgroundScanJob = scope.launch {
            try {
                Log.d(TAG, "Background full scan: Starting for ${sourceConfigs.size} source(s)")

                val quickPaths = _photos.value.map { it.path }.toSet()

                for (config in sourceConfigs) {
                    try {
                        val sourceResult = photoSourceFactory.createSource(config)
                        if (sourceResult !is Result.Success) {
                            Log.e(TAG, "Background full scan: Failed to create source '${config.displayName}'")
                            continue
                        }

                        val source = sourceResult.data as PhotoSource
                        Log.d(TAG, "Background full scan: Scanning source '${config.displayName}'...")

                        val scanResult = source.scanPhotos()
                        when (scanResult) {
                            is Result.Success -> {
                                val photos = scanResult.data
                                Log.d(TAG, "Background full scan: Source '${config.displayName}' - ${photos.size} photos total")

                                // Save all photos to database
                                try {
                                    val entities = photos.map { it.toEntity(config.id) }
                                    photoDao.deletePhotosForSource(config.id)
                                    photoDao.insertPhotos(entities)
                                    Log.d(TAG, "Background full scan: Saved ${entities.size} photos to DB for '${config.displayName}'")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Background full scan: DB save failed for '${config.displayName}'", e)
                                }

                                // Find photos not already in the slideshow
                                val newPhotos = photos.filter { it.path !in quickPaths }
                                if (newPhotos.isNotEmpty()) {
                                    Log.d(TAG, "Background full scan: Adding ${newPhotos.size} new photos to slideshow")
                                    // Merge under mutex, emit outside to prevent lock inversion
                                    val updatedList = mutex.withLock {
                                        val current = _photos.value.toMutableList()
                                        if (shuffleEnabled) {
                                            for (photo in newPhotos) {
                                                val pos = Random.nextInt(current.size + 1)
                                                current.add(pos, photo)
                                            }
                                        } else {
                                            current.addAll(newPhotos)
                                        }
                                        current.toList()
                                    }
                                    _photos.value = updatedList
                                    Log.d(TAG, "Background full scan: Slideshow now has ${updatedList.size} photos")
                                }
                            }
                            is Result.Error -> {
                                Log.e(TAG, "Background full scan: Source '${config.displayName}' failed: ${scanResult.message}")
                            }
                            is Result.Loading -> {}
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Background full scan: Exception for '${config.displayName}'", e)
                    }
                }

                Log.d(TAG, "Background full scan: Complete. Total photos: ${_photos.value.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Background full scan: Exception", e)
            }
        }
    }

    companion object {
        private const val TAG = "MultiSourcePhotoRepo"
        private const val QUICK_SCAN_LIMIT = 200
    }
}
