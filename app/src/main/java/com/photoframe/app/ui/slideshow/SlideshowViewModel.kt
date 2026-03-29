package com.photoframe.app.ui.slideshow

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoframe.core.model.Result
import com.photoframe.core.model.Photo
import com.photoframe.core.model.SlideshowSettings
import com.photoframe.core.network.NetworkMonitor
import com.photoframe.core.reliability.CrashHandler
import com.photoframe.core.reliability.MemoryMonitor
import com.photoframe.core.reliability.MemoryState
import com.photoframe.core.reliability.SlideshowWatchdog
import com.photoframe.core.repository.PhotoRotationStore
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.telemetry.TelemetryLogger
import com.photoframe.core.repository.SlideshowRepository
import com.photoframe.core.slideshow.PhotoBufferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the slideshow screen.
 *
 * Architecture: MVVM pattern
 * - ViewModel: Manages UI state and business logic
 * - Repository: Provides data and operations
 *
 * Lifecycle: Scoped to Activity lifecycle via Hilt
 *
 * Features:
 * - Auto-advance timer with configurable interval
 * - Play/pause controls
 * - Manual navigation (next/previous)
 * - Photo shuffling
 * - Reactive state updates via StateFlow
 * - In-process watchdog for stall detection and recovery
 *
 * Thread Safety: All public methods are safe to call from main thread.
 * Internal operations run on appropriate dispatchers via repositories.
 *
 * Phase 4 (Reliability Features):
 * - Network recovery: Auto-reconnect when network returns
 * - Crash recovery: Save/restore slideshow state
 * - Watchdog integration: Start/stop monitoring service
 * - In-process watchdog: Detects and recovers from stalled slideshow
 *
 * @param context Application context for service management
 * @param slideshowRepository Repository for photo management
 * @param settingsRepository Repository for slideshow settings
 * @param photoBufferManager Buffer manager for monitoring buffer state
 * @param networkMonitor Network connectivity monitor
 * @param memoryMonitor Memory monitor for buffer recovery after memory pressure
 * @param crashHandler Crash handler for state preservation
 * @param telemetryLogger Telemetry logger for Crashlytics integration
 */
@HiltViewModel
class SlideshowViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val slideshowRepository: SlideshowRepository,
    private val settingsRepository: SettingsRepository,
    private val photoBufferManager: PhotoBufferManager,
    private val networkMonitor: NetworkMonitor,
    private val memoryMonitor: MemoryMonitor,
    private val crashHandler: CrashHandler,
    private val telemetryLogger: TelemetryLogger,
    private val photoRotationStore: PhotoRotationStore
) : ViewModel() {

    // UI state
    private val _state = MutableStateFlow(SlideshowState.EMPTY)
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    // Auto-advance job
    private var autoAdvanceJob: Job? = null

    // Next photo job for cancel-and-replace pattern (prevents coroutine pile-up)
    private var nextPhotoJob: Job? = null

    // Previous photo job for cancel-and-replace pattern (prevents coroutine pile-up)
    private var previousPhotoJob: Job? = null

    // Network recovery job
    private var networkRecoveryJob: Job? = null
    private var isNetworkDisconnected = false

    // Memory recovery tracking
    private var wasMemoryCritical = false

    // State persistence for crash recovery
    private var lastSavedPhotoIndex = -1

    // Initialization flag to filter transient errors during startup
    private var isInitialized = false

    // In-process watchdog
    private var watchdogJob: Job? = null

    // Initialization timeout watchdog (P1 fix)
    private var initializationTimeoutJob: Job? = null

    @Volatile
    private var lastSuccessfulAdvanceMs: Long = 0L

    init {
        // Phase 4: Monitor network state for auto-recovery
        viewModelScope.launch {
            networkMonitor.isNetworkAvailable.collect { isAvailable ->
                handleNetworkStateChange(isAvailable)
            }
        }

        // Monitor memory state for buffer recovery after memory pressure
        viewModelScope.launch {
            memoryMonitor.memoryState.collect { memoryState ->
                handleMemoryStateChange(memoryState)
            }
        }

        // Observe repositories and update UI state
        // All state flows from repository are observed reactively - no suspend calls needed
        viewModelScope.launch {
            combine(
                slideshowRepository.currentPhoto,
                slideshowRepository.currentPhotoMetadata,
                slideshowRepository.currentPhotoIndex,
                slideshowRepository.photos,
                slideshowRepository.isLoading,
                slideshowRepository.error
            ) { values ->
                val currentPhoto = values[0] as Bitmap?
                val metadata = values[1] as Photo?
                val photoIndex = values[2] as Int
                val photos = values[3] as List<Photo>
                val isLoading = values[4] as Boolean
                val error = values[5] as String?

                // Suppress errors that should not interrupt the slideshow:
                // 1. Buffer initialization errors before slideshow is ready
                // 2. Transient load errors when photos are already loaded (skip silently)
                val filteredError = when {
                    !isInitialized && error?.contains("Call initialize() before") == true -> null
                    photos.isNotEmpty() && error != null -> {
                        android.util.Log.w("SlideshowViewModel", "Suppressing transient error (photos loaded): $error")
                        null
                    }
                    else -> error
                }

                // Load persisted rotation for the current photo
                val rotation = if (metadata != null && !metadata.isVideo) {
                    photoRotationStore.getRotation(metadata.path)
                } else 0

                _state.update { it.copy(
                    currentPhoto = currentPhoto,
                    currentPhotoMetadata = metadata,
                    photoIndex = photoIndex.coerceAtLeast(0),
                    totalPhotos = photos.size,
                    isLoading = isLoading,
                    error = filteredError,
                    currentRotation = rotation
                ) }
            }.collect { }
        }

        // Load settings and initialize slideshow automatically
        viewModelScope.launch {
            val settingsResult = settingsRepository.loadSlideshowSettings()
            if (settingsResult is Result.Success) {
                val settings = settingsResult.data
                _state.update { it.copy(
                    transitionType = settings.transitionType,
                    displayIntervalMillis = settings.displayIntervalMillis,
                    panAnimationEnabled = settings.panAnimationEnabled
                ) }

                // Initialize slideshow with settings-based shuffle and auto-play
                // This eliminates the timing gap where LaunchedEffect would be used
                // FIX 5: Set isInitialized BEFORE initialize() suspends to prevent race window
                isInitialized = true
                initialize(shuffleEnabled = settings.shuffleEnabled, autoPlay = true)
            }
        }

        // Observe settings changes and apply them dynamically to running slideshow
        viewModelScope.launch {
            var isFirstEmission = true

            settingsRepository.slideshowSettings.collect { settings ->
                // Skip the first emission (initial settings load at app start)
                if (isFirstEmission) {
                    isFirstEmission = false
                    return@collect
                }

                android.util.Log.d("SlideshowViewModel", "Settings changed, applying dynamically")

                // Update UI state with new settings
                _state.update { it.copy(
                    transitionType = settings.transitionType,
                    displayIntervalMillis = settings.displayIntervalMillis,
                    panAnimationEnabled = settings.panAnimationEnabled
                ) }

                // Only apply changes if slideshow is initialized and has photos
                if (!isInitialized || _state.value.totalPhotos == 0) {
                    android.util.Log.d("SlideshowViewModel", "Slideshow not ready, skipping settings application")
                    return@collect
                }

                val wasPlaying = _state.value.isPlaying

                // Apply shuffle if it's currently enabled (re-shuffle on any settings save)
                if (settings.shuffleEnabled) {
                    android.util.Log.d("SlideshowViewModel", "Shuffle is enabled - reshuffling photos")
                    shuffle()
                }

                // Restart auto-advance if playing to apply new display interval immediately
                if (wasPlaying) {
                    android.util.Log.d("SlideshowViewModel", "Restarting auto-advance with new settings")
                    pause()
                    play()
                }
            }
        }

        // Monitor buffer state (for debugging)
        viewModelScope.launch {
            photoBufferManager.loadingState.collect { loadingState ->
                val bufferSize = photoBufferManager.getBufferSize()
                _state.update { it.copy(
                    bufferedPhotosCount = bufferSize
                ) }

                // Update telemetry context
                val currentIndex = slideshowRepository.getCurrentPhotoIndex()
                val totalPhotos = _state.value.totalPhotos
                telemetryLogger.setSlideshowContext(currentIndex, totalPhotos, bufferSize)
            }
        }

        // Phase 4: Check if restarted after crash and restore state
        viewModelScope.launch {
            val savedState = crashHandler.loadSlideshowState()
            if (savedState != null) {
                android.util.Log.i("SlideshowViewModel", "Restoring slideshow state after crash: index=${savedState.photoIndex}")
                // TODO: Implement state restoration logic
                // This requires coordination with initialize() to jump to saved index
                crashHandler.clearSlideshowState()
            }
        }
    }

    /**
     * Handles network state changes for auto-recovery.
     *
     * P0 BLOCKING: Addresses "No auto-recovery from network disconnect" (Senior Dev 3).
     *
     * Strategy:
     * - When network lost: Continue with buffered photos, show warning
     * - When network restored: Auto-reconnect and resume loading
     * - Automatic retry every 30 seconds while disconnected
     *
     * P0 PART 3: Auto-initialize on network restore if in error state
     * P2: Uses isRetry=true to show "Retrying..." state in UI
     */
    private fun handleNetworkStateChange(isAvailable: Boolean) {
        if (!isAvailable) {
            // Network lost
            if (!isNetworkDisconnected) {
                isNetworkDisconnected = true
                android.util.Log.w("SlideshowViewModel", "Network disconnected, continuing with buffered photos")
                telemetryLogger.logNetworkDisconnect()

                // Show warning in UI but DON'T block isReady — use a separate warning field
                // Note: Setting error here would make isReady=false, hiding the slideshow
                // Instead we log and let buffered photos continue playing
                android.util.Log.w("SlideshowViewModel", "Network disconnected. Buffered photos will continue.")

                // Start retry job (every 30 seconds)
                startNetworkRetryJob()
            }
        } else {
            // Network restored
            if (isNetworkDisconnected) {
                isNetworkDisconnected = false
                android.util.Log.i("SlideshowViewModel", "Network restored, resuming normal operation")
                telemetryLogger.logNetworkReconnect()

                // Stop retry job
                networkRecoveryJob?.cancel()
                networkRecoveryJob = null

                // Check if recovery is needed BEFORE clearing the error
                val needsRecovery = isInitialized && _state.value.totalPhotos == 0

                // Clear network-related errors
                _state.update { currentState ->
                    if (currentState.error?.contains("Network") == true ||
                        currentState.error?.contains("network") == true) {
                        currentState.copy(error = null, isRetrying = false)
                    } else {
                        currentState
                    }
                }

                // P0 PART 3: Auto-retry initialize() if we had no photos at startup
                // This fixes the scenario: network unavailable at boot → error shown → network comes back later
                if (needsRecovery) {
                    android.util.Log.i("SlideshowViewModel", "Network restored with no photos loaded, auto-retrying initialize()")
                    initialize(shuffleEnabled = false, autoPlay = true, isRetry = true)
                }
            }
        }
    }

    /**
     * Starts background retry job for network recovery.
     * Retries every 30 seconds while disconnected.
     */
    private fun startNetworkRetryJob() {
        networkRecoveryJob?.cancel()
        networkRecoveryJob = viewModelScope.launch {
            while (isNetworkDisconnected) {
                delay(NETWORK_RETRY_INTERVAL_MS)
                if (isNetworkDisconnected && networkMonitor.isNetworkAvailable.value) {
                    // Network came back, handleNetworkStateChange will be called
                    break
                }
            }
        }
    }

    /**
     * Handles memory state changes for buffer recovery.
     *
     * Strategy:
     * - When Critical: Memory pressure detected, buffer reduced to minimum
     * - When returning to Normal: Restore buffer by preloading next photos
     */
    private fun handleMemoryStateChange(memoryState: MemoryState) {
        when (memoryState) {
            is MemoryState.Critical -> {
                // Memory pressure detected - buffer has been reduced to minimum
                wasMemoryCritical = true
                android.util.Log.w("SlideshowViewModel", "Critical memory pressure: ${memoryState.usagePercent}%")
            }
            is MemoryState.Warning -> {
                // Warning threshold - memory cache cleared but buffer intact
                android.util.Log.d("SlideshowViewModel", "Memory warning: ${memoryState.usagePercent}%")
            }
            is MemoryState.Normal -> {
                // Memory returned to normal
                if (wasMemoryCritical && isInitialized && _state.value.totalPhotos > 0) {
                    // Recovered from critical memory - buffer needs restoration
                    wasMemoryCritical = false
                    android.util.Log.i("SlideshowViewModel", "Memory recovered from critical, restoring buffer preloading")
                    // Buffer will automatically refill as we navigate through photos
                    // No explicit action needed - the next photo load will trigger preload
                }
            }
        }
    }

    /**
     * Initializes the slideshow by loading photos from all sources.
     *
     * P0 FIX: Adds exponential backoff retry for transient SMB failures.
     * - Retries with 2s, 4s, 8s delays on transient errors (timeout, connection, network)
     * - Includes network gate: skips SMB if WiFi not ready (fast-fail vs socket timeout)
     * - Only shows persistent errors (auth, path) to user
     *
     * P1 FIX: Adds initialization timeout watchdog.
     * - If initialize() doesn't complete within 60s, forces a retry
     * - Prevents app from getting stuck on loading screen indefinitely
     *
     * P2 FIX: Adds isRetrying flag for UI feedback.
     * - Shows "Retrying..." state when timeout or network recovery triggers auto-retry
     * - Helps user understand app is not frozen, just recovering
     *
     * Thread Safety: Safe to call from main thread.
     *
     * @param shuffleEnabled If true, shuffles photos after loading
     * @param autoPlay If true, starts auto-play after successful loading
     * @param isRetry If true, indicates this is a retry attempt (used to set isRetrying flag)
     */
    fun initialize(shuffleEnabled: Boolean = false, autoPlay: Boolean = false, isRetry: Boolean = false) {
        viewModelScope.launch {
            // P2: Set isRetrying if this is an auto-retry (timeout or network recovery)
            _state.update { it.copy(
                isLoading = true,
                isRetrying = isRetry,
                error = if (isRetry) "Retrying..." else null
            ) }

            // P1: Start initialization timeout watchdog
            val initializationStartTime = System.currentTimeMillis()
            initializationTimeoutJob?.cancel()
            initializationTimeoutJob = viewModelScope.launch {
                delay(INITIALIZATION_TIMEOUT_MS)
                
                // Check if initialization is still ongoing
                if (_state.value.isLoading && _state.value.totalPhotos == 0) {
                    android.util.Log.w("SlideshowViewModel", "INITIALIZATION TIMEOUT: No progress after ${INITIALIZATION_TIMEOUT_MS}ms, forcing retry")
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Initialization timed out, retrying..."
                    ) }
                    telemetryLogger.logInitializationTimeout()
                    
                    // Force a retry after a brief delay
                    delay(1000L)
                    initialize(shuffleEnabled = shuffleEnabled, autoPlay = autoPlay, isRetry = true)
                }
            }

            val backoffDelays = listOf(2000L, 4000L, 8000L) // 2s, 4s, 8s
            var attempt = 0

            while (attempt <= backoffDelays.size) {
                // Part 1: Network gate - skip SMB if WiFi not ready (fast-fail)
                if (!networkMonitor.isNetworkAvailable.value) {
                    android.util.Log.d("SlideshowViewModel", "Network not available, retrying after delay...")
                    if (attempt < backoffDelays.size) {
                        delay(backoffDelays[attempt])
                        attempt++
                        continue
                    } else {
                        _state.update { it.copy(
                            isLoading = false,
                            isRetrying = false,
                            error = "Network not available after retries"
                        ) }
                        initializationTimeoutJob?.cancel()
                        return@launch
                    }
                }

                // Part 2: Attempt to load photos
                val result = slideshowRepository.loadPhotos(shuffleEnabled)
                when (result) {
                    is Result.Success -> {
                        // Update metadata
                        val metadata = slideshowRepository.getCurrentPhotoMetadata()
                        _state.update { it.copy(
                            currentPhotoMetadata = metadata,
                            isLoading = false,
                            isRetrying = false,
                            error = null
                        ) }

                        // Cancel timeout watchdog on success
                        initializationTimeoutJob?.cancel()
                        initializationTimeoutJob = null

                        // Start auto-play if requested and loading succeeded
                        if (autoPlay) {
                            play()
                        }
                        return@launch
                    }
                    is Result.Error -> {
                        val errorMsg = result.message ?: "Failed to load photos"
                        val isTransient = errorMsg.contains(Regex("timeout|connection|refused|network|socket", RegexOption.IGNORE_CASE))

                        if (isTransient && attempt < backoffDelays.size) {
                            // Transient error - retry with backoff
                            android.util.Log.d("SlideshowViewModel", "Transient error (attempt ${attempt + 1}/3): $errorMsg. Retrying in ${backoffDelays[attempt]}ms...")
                            delay(backoffDelays[attempt])
                            attempt++
                        } else {
                            // Persistent error or exhausted retries - show to user
                            val finalError = if (attempt >= backoffDelays.size && isTransient) {
                                "Failed to load photos after retries"
                            } else {
                                errorMsg
                            }
                            _state.update { it.copy(
                                isLoading = false,
                                isRetrying = false,
                                error = finalError
                            ) }
                            android.util.Log.e("SlideshowViewModel", "Initialization failed: $finalError")
                            
                            // Cancel timeout watchdog on final error
                            initializationTimeoutJob?.cancel()
                            initializationTimeoutJob = null
                            
                            return@launch
                        }
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            }
        }
    }

    /**
     * Starts auto-advancing the slideshow.
     * Uses display interval from SettingsRepository.
     *
     * Includes try-catch to prevent silent death of the auto-advance loop,
     * and starts the in-process watchdog for stall detection.
     *
     * Thread Safety: Safe to call from main thread.
     */
    fun play() {
        if (_state.value.isPlaying) return // Already playing

        _state.update { it.copy(isPlaying = true) }
        lastSuccessfulAdvanceMs = System.currentTimeMillis()

        startInProcessWatchdog()
        startWatchdogService(_state.value.displayIntervalMillis)

        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            // Advance immediately on first iteration so play() feels responsive
            var isFirstIteration = true
            while (true) {
                try {
                    val interval = _state.value.displayIntervalMillis

                    if (isFirstIteration) {
                        isFirstIteration = false
                    } else {
                        delay(interval)
                    }

                    // Skip advance for videos (they advance via onVideoEnded callback)
                    if (!_state.value.isPlaying) continue
                    if (_state.value.currentPhotoMetadata?.isVideo == true) continue

                    // Load next photo SYNCHRONOUSLY — prevents job pile-up.
                    // Previous pattern: fire-and-forget nextPhoto() caused concurrent
                    // loads that exhausted dispatcher threads on slow networks.
                    val result = slideshowRepository.nextPhoto(interval)
                    when (result) {
                        is Result.Success -> {
                            lastSuccessfulAdvanceMs = System.currentTimeMillis()
                            val currentIndex = _state.value.photoIndex
                            if (currentIndex != lastSavedPhotoIndex) {
                                crashHandler.saveSlideshowState(
                                    photoIndex = currentIndex,
                                    totalPhotos = _state.value.totalPhotos,
                                    isPlaying = true
                                )
                                lastSavedPhotoIndex = currentIndex
                            }
                        }
                        is Result.Error -> {
                            // Update timestamp even on error — the loop IS advancing, just
                            // hitting bad files. Prevents false watchdog stall detection.
                            lastSuccessfulAdvanceMs = System.currentTimeMillis()
                            android.util.Log.e("SlideshowViewModel", "Auto-advance photo load error: ${result.message}")
                            telemetryLogger.logPhotoLoadFailed(
                                _state.value.currentPhotoMetadata?.path ?: "unknown",
                                result.message ?: "Failed to load next photo"
                            )
                            // Continue loop — will try next photo on next iteration
                        }
                        is Result.Loading -> { /* Should not happen */ }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("SlideshowViewModel", "Auto-advance loop error, recovering", e)
                    telemetryLogger.logAutoAdvanceError(e.message ?: "Unknown error")
                    delay(2_000L)
                }
            }
        }
    }

    /**
     * In-process watchdog that detects stalled slideshow and forces recovery.
     *
     * Checks every [WATCHDOG_CHECK_INTERVAL_MS] if the slideshow has advanced.
     * If no advance for 2x display interval + grace period, forces restart.
     */
    private fun startInProcessWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch {
            while (true) {
                delay(WATCHDOG_CHECK_INTERVAL_MS)

                if (!_state.value.isPlaying) continue

                // Don't trigger stall detection during video playback —
                // videos advance via onVideoEnded callback, not the auto-advance timer
                if (_state.value.currentPhotoMetadata?.isVideo == true) continue

                val interval = _state.value.displayIntervalMillis
                val stallThreshold = interval * 2 + WATCHDOG_GRACE_MS
                val timeSinceAdvance = System.currentTimeMillis() - lastSuccessfulAdvanceMs

                if (timeSinceAdvance > stallThreshold && lastSuccessfulAdvanceMs > 0) {
                    android.util.Log.w("SlideshowViewModel",
                        "WATCHDOG: Stall detected! ${timeSinceAdvance}ms since last advance (threshold: ${stallThreshold}ms)")

                    // Recovery: cancel stuck jobs, force restart auto-advance
                    nextPhotoJob?.cancel()
                    autoAdvanceJob?.cancel()

                    lastSuccessfulAdvanceMs = System.currentTimeMillis()
                    _state.update { it.copy(isPlaying = false) }

                    // Restart playback
                    play()
                    break // Exit this watchdog instance; play() starts a new one
                }
            }
        }
    }

    /**
     * Pauses auto-advancing the slideshow.
     *
     * Phase 4: Stops watchdog service.
     *
     * Thread Safety: Safe to call from main thread.
     */
    fun pause() {
        _state.update { it.copy(isPlaying = false) }
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
        nextPhotoJob?.cancel()
        nextPhotoJob = null
        previousPhotoJob?.cancel()
        previousPhotoJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        stopWatchdogService()
    }

    fun rotateClockwise() {
        val path = _state.value.currentPhotoMetadata?.path ?: return
        val newRotation = (_state.value.currentRotation + 90) % 360
        _state.update { it.copy(currentRotation = newRotation) }
        viewModelScope.launch { photoRotationStore.setRotation(path, newRotation) }
    }

    fun rotateCounterClockwise() {
        val path = _state.value.currentPhotoMetadata?.path ?: return
        val newRotation = ((_state.value.currentRotation - 90) + 360) % 360
        _state.update { it.copy(currentRotation = newRotation) }
        viewModelScope.launch { photoRotationStore.setRotation(path, newRotation) }
    }

    /**
     * Advances to the next photo.
     * Pauses auto-advance if manually triggered.
     *
     * Phase 4: Saves slideshow state for crash recovery.
     *
     * Thread Safety: Safe to call from main thread.
     *
     * @param pauseAutoAdvance If true, pauses auto-advance (default true for manual navigation)
     * @param displayIntervalMs Display interval for dynamic timeout calculation (only used for auto-advance)
     */
    fun nextPhoto(pauseAutoAdvance: Boolean = false, displayIntervalMs: Long = 10_000L) {
        // Guard: Don't allow navigation before initialization completes
        if (!isInitialized) {
            android.util.Log.w("SlideshowViewModel", "nextPhoto called before initialization, ignoring")
            return
        }

        if (pauseAutoAdvance && _state.value.isPlaying) {
            pause()
        }

        // Set navigation direction for transition animation
        _state.update { it.copy(navigationDirection = NavigationDirection.FORWARD) }

        // Fix #1: Cancel previous auto-advance job to prevent coroutine pile-up
        if (!pauseAutoAdvance) {
            nextPhotoJob?.cancel()
        }

        val job = viewModelScope.launch {
            // Fix #2: Pass display interval for dynamic timeout calculation
            val result = slideshowRepository.nextPhoto(displayIntervalMs)
            // Note: State updates now handled reactively by combine flow observing repository StateFlows

            android.util.Log.d("SlideshowViewModel", "nextPhoto: result=${result.javaClass.simpleName}")

            when (result) {
                is Result.Success -> {
                    // Track successful advance for watchdog
                    lastSuccessfulAdvanceMs = System.currentTimeMillis()

                    // Combine flow will update state automatically from repository StateFlows
                    // Just handle crash recovery state saving here
                    val currentIndex = _state.value.photoIndex
                    if (currentIndex != lastSavedPhotoIndex) {
                        crashHandler.saveSlideshowState(
                            photoIndex = currentIndex,
                            totalPhotos = _state.value.totalPhotos,
                            isPlaying = _state.value.isPlaying
                        )
                        lastSavedPhotoIndex = currentIndex
                    }
                }
                is Result.Error -> {
                    val errorMsg = result.message ?: "Failed to load next photo"
                    android.util.Log.e("SlideshowViewModel", "Photo load error: $errorMsg")
                    val photoPath = _state.value.currentPhotoMetadata?.path ?: "unknown"
                    telemetryLogger.logPhotoLoadFailed(photoPath, errorMsg)
                    // Don't show error screen — auto-advance will try the next photo
                }
                is Result.Loading -> { /* Should not happen */ }
            }
        }

        // Fix #1: Track job for auto-advance (allows cancellation)
        if (!pauseAutoAdvance) {
            nextPhotoJob = job
        }
    }

    /**
     * Goes back to the previous photo.
     * Pauses auto-advance if manually triggered.
     *
     * Thread Safety: Safe to call from main thread.
     *
     * @param pauseAutoAdvance If true, pauses auto-advance (default true for manual navigation)
     */
    fun previousPhoto(pauseAutoAdvance: Boolean = false, displayIntervalMs: Long = 10_000L) {
        // Guard: Don't allow navigation before initialization completes
        if (!isInitialized) {
            android.util.Log.w("SlideshowViewModel", "previousPhoto called before initialization, ignoring")
            return
        }

        if (pauseAutoAdvance && _state.value.isPlaying) {
            pause()
        }

        // Set navigation direction for transition animation
        _state.update { it.copy(navigationDirection = NavigationDirection.BACKWARD) }

        // FIX C: Cancel previous job to prevent coroutine pile-up (matching nextPhoto pattern)
        if (!pauseAutoAdvance) {
            previousPhotoJob?.cancel()
        }

        val job = viewModelScope.launch {
            // FIX C: Pass display interval for dynamic timeout calculation
            val result = slideshowRepository.previousPhoto(displayIntervalMs)
            // Note: State updates now handled reactively by combine flow observing repository StateFlows

            android.util.Log.d("SlideshowViewModel", "previousPhoto: result=${result.javaClass.simpleName}")

            when (result) {
                is Result.Success -> {
                    lastSuccessfulAdvanceMs = System.currentTimeMillis()
                }
                is Result.Error -> {
                    val errorMsg = result.message ?: "Failed to load previous photo"
                    android.util.Log.e("SlideshowViewModel", "Photo load error: $errorMsg")
                    val photoPath = _state.value.currentPhotoMetadata?.path ?: "unknown"
                    telemetryLogger.logPhotoLoadFailed(photoPath, errorMsg)
                    // Don't show error screen — auto-advance will try the next photo
                }
                is Result.Loading -> { /* Should not happen */ }
            }
        }

        // FIX C: Track job for cancellation (matching nextPhoto pattern)
        if (!pauseAutoAdvance) {
            previousPhotoJob = job
        }
    }

    /**
     * Shuffles the photo order.
     * Maintains current photo position.
     *
     * Thread Safety: Safe to call from main thread.
     */
    fun shuffle() {
        viewModelScope.launch {
            val result = slideshowRepository.shufflePhotos()
            when (result) {
                is Result.Success -> {
                    val metadata = slideshowRepository.getCurrentPhotoMetadata()
                    _state.update { it.copy(
                        currentPhotoMetadata = metadata,
                        error = null
                    ) }
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        error = result.message ?: "Failed to shuffle photos"
                    ) }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Retries loading photos after an error.
     *
     * Thread Safety: Safe to call from main thread.
     */
    fun retry() {
        initialize(shuffleEnabled = false, autoPlay = true)
    }

    fun reload() {
        pause()
        viewModelScope.launch {
            slideshowRepository.clear()
            _state.value = SlideshowState.EMPTY
            val settingsResult = settingsRepository.loadSlideshowSettings()
            val settings = (settingsResult as? Result.Success)?.data ?: SlideshowSettings.DEFAULT
            _state.update { it.copy(
                transitionType = settings.transitionType,
                displayIntervalMillis = settings.displayIntervalMillis,
                panAnimationEnabled = settings.panAnimationEnabled
            ) }
            initialize(shuffleEnabled = settings.shuffleEnabled, autoPlay = true)
        }
    }

    /**
     * Clears the slideshow and resets to initial state.
     *
     * Thread Safety: Safe to call from main thread.
     */
    fun clear() {
        pause()
        viewModelScope.launch {
            slideshowRepository.clear()
            _state.value = SlideshowState.EMPTY
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
        nextPhotoJob?.cancel()
        previousPhotoJob?.cancel()
        networkRecoveryJob?.cancel()
        watchdogJob?.cancel()
        initializationTimeoutJob?.cancel()
        stopWatchdogService()
    }

    private fun startWatchdogService(displayIntervalMs: Long) {
        val intent = SlideshowWatchdog.createStartIntent(context, displayIntervalMs)
        context.startForegroundService(intent)
    }

    private fun stopWatchdogService() {
        val intent = SlideshowWatchdog.createStopIntent(context)
        context.stopService(intent)
    }

    companion object {
        private const val NETWORK_RETRY_INTERVAL_MS = 30_000L
        private const val WATCHDOG_CHECK_INTERVAL_MS = 5_000L
        private const val WATCHDOG_GRACE_MS = 5_000L
        private const val INITIALIZATION_TIMEOUT_MS = 60_000L // 60 seconds
    }
}
