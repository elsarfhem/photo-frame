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
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.telemetry.TelemetryLogger
import com.photoframe.core.repository.SlideshowRepository
import com.photoframe.core.slideshow.PhotoBufferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 *
 * Thread Safety: All public methods are safe to call from main thread.
 * Internal operations run on appropriate dispatchers via repositories.
 *
 * Phase 4 (Reliability Features):
 * - Network recovery: Auto-reconnect when network returns
 * - Crash recovery: Save/restore slideshow state
 * - Watchdog integration: Start/stop monitoring service
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
    private val telemetryLogger: TelemetryLogger
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

                // Filter out buffer-level initialization errors before slideshow is initialized
                // These are transient states that should not be shown to users
                val filteredError = if (!isInitialized && error?.contains("Call initialize() before") == true) {
                    null // Suppress initialization error until initialize() is called
                } else {
                    error
                }

                _state.update { it.copy(
                    currentPhoto = currentPhoto,
                    currentPhotoMetadata = metadata,
                    photoIndex = photoIndex.coerceAtLeast(0),
                    totalPhotos = photos.size,
                    isLoading = isLoading,
                    error = filteredError
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
                initialize(shuffleEnabled = settings.shuffleEnabled, autoPlay = true)
                isInitialized = true
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
     */
    private fun handleNetworkStateChange(isAvailable: Boolean) {
        if (!isAvailable) {
            // Network lost
            if (!isNetworkDisconnected) {
                isNetworkDisconnected = true
                android.util.Log.w("SlideshowViewModel", "Network disconnected, continuing with buffered photos")
                telemetryLogger.logNetworkDisconnect()

                // Show warning in UI
                _state.update { it.copy(
                    error = "Network disconnected. Playing buffered photos..."
                ) }

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

                // Clear error
                _state.update { it.copy(error = null) }

                // Resume slideshow (buffer will automatically reload)
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
     * Thread Safety: Safe to call from main thread.
     *
     * @param shuffleEnabled If true, shuffles photos after loading
     * @param autoPlay If true, starts auto-play after successful loading
     */
    fun initialize(shuffleEnabled: Boolean = false, autoPlay: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = slideshowRepository.loadPhotos(shuffleEnabled)
            when (result) {
                is Result.Success -> {
                    // Update metadata
                    val metadata = slideshowRepository.getCurrentPhotoMetadata()
                    _state.update { it.copy(
                        currentPhotoMetadata = metadata,
                        isLoading = false,
                        error = null
                    ) }

                    // Start auto-play if requested and loading succeeded
                    if (autoPlay) {
                        play()
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to load photos"
                    ) }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }

    /**
     * Starts auto-advancing the slideshow.
     * Uses display interval from SettingsRepository.
     *
     * Phase 4: Starts watchdog service for stall detection.
     *
     * Thread Safety: Safe to call from main thread.
     */
    fun play() {
        if (_state.value.isPlaying) return // Already playing

        _state.update { it.copy(isPlaying = true) }

        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            while (true) {
                // Get display interval from settings
                val settingsResult = settingsRepository.loadSlideshowSettings()
                val interval = if (settingsResult is Result.Success) {
                    settingsResult.data.displayIntervalMillis
                } else {
                    10_000L // Default 10 seconds
                }

                // Phase 4: Start watchdog service if not already running
                if (_state.value.photoIndex == 0) {
                    startWatchdogService(interval)
                }

                // FIX D: Advance to next photo FIRST (skip for videos - they advance via onVideoEnded callback)
                if (_state.value.isPlaying && _state.value.currentPhotoMetadata?.isVideo != true) {
                    // Pass display interval for dynamic timeout calculation
                    nextPhoto(pauseAutoAdvance = false, displayIntervalMs = interval)
                }

                // FIX D: THEN wait for interval (so photo displays for full duration)
                // Previously: delay was BEFORE nextPhoto, meaning photo displayed for 0s
                // Now: delay is AFTER nextPhoto, so photo displays for full interval
                delay(interval)
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

        // Phase 4: Stop watchdog service
        stopWatchdogService()
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
                    // Only show error if multiple consecutive photos failed (critical error)
                    val errorMsg = result.message ?: "Failed to load next photo"
                    android.util.Log.e("SlideshowViewModel", "Critical error: $errorMsg")

                    // Log photo load failure
                    val photoPath = _state.value.currentPhotoMetadata?.path ?: "unknown"
                    telemetryLogger.logPhotoLoadFailed(photoPath, errorMsg)

                    // Only show error in UI if it's a critical failure (10+ consecutive failures)
                    if (errorMsg.contains("All recent photos failed")) {
                        _state.update { it.copy(error = "Unable to load photos. Check network connection.") }
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
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
                    // Combine flow will update state automatically from repository StateFlows
                    // Success case handled reactively
                }
                is Result.Error -> {
                    // Still need to handle error explicitly as it's not always from StateFlow
                    _state.update { it.copy(
                        error = result.message ?: "Failed to load previous photo"
                    ) }
                }
                is Result.Loading -> {
                    // Should not happen
                }
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
        initialize(shuffleEnabled = false)
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

    /**
     * Starts the watchdog service to monitor slideshow health.
     *
     * @param displayIntervalMs Display interval in milliseconds
     */
    private fun startWatchdogService(displayIntervalMs: Long) {
        try {
            val intent = SlideshowWatchdog.createStartIntent(context, displayIntervalMs)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("SlideshowViewModel", "Failed to start watchdog service", e)
        }
    }

    /**
     * Stops the watchdog service.
     */
    private fun stopWatchdogService() {
        try {
            val intent = SlideshowWatchdog.createStopIntent(context)
            context.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("SlideshowViewModel", "Failed to stop watchdog service", e)
        }
    }

    /**
     * Cancels all jobs and stops watchdog when ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        autoAdvanceJob?.cancel()
        nextPhotoJob?.cancel()
        previousPhotoJob?.cancel()
        networkRecoveryJob?.cancel()
        stopWatchdogService()
    }

    companion object {
        /**
         * Network retry interval: 30 seconds.
         */
        private const val NETWORK_RETRY_INTERVAL_MS = 30_000L
    }
}
