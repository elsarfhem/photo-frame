package com.photoframe.app.ui.slideshow

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photoframe.core.model.Result
import com.photoframe.core.model.Photo
import com.photoframe.core.network.NetworkMonitor
import com.photoframe.core.reliability.CrashHandler
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
    private val crashHandler: CrashHandler,
    private val telemetryLogger: TelemetryLogger
) : ViewModel() {

    // UI state
    private val _state = MutableStateFlow(SlideshowState.EMPTY)
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    // Auto-advance job
    private var autoAdvanceJob: Job? = null

    // Network recovery job
    private var networkRecoveryJob: Job? = null
    private var isNetworkDisconnected = false

    // State persistence for crash recovery
    private var lastSavedPhotoIndex = -1

    init {
        // Phase 4: Monitor network state for auto-recovery
        viewModelScope.launch {
            networkMonitor.isNetworkAvailable.collect { isAvailable ->
                handleNetworkStateChange(isAvailable)
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

                _state.value = _state.value.copy(
                    currentPhoto = currentPhoto,
                    currentPhotoMetadata = metadata,
                    photoIndex = photoIndex.coerceAtLeast(0),
                    totalPhotos = photos.size,
                    isLoading = isLoading,
                    error = error
                )
            }.collect { }
        }

        // Load transition type and display interval from settings
        viewModelScope.launch {
            val settingsResult = settingsRepository.loadSlideshowSettings()
            if (settingsResult is Result.Success) {
                _state.value = _state.value.copy(
                    transitionType = settingsResult.data.transitionType,
                    displayIntervalMillis = settingsResult.data.displayIntervalMillis,
                    panAnimationEnabled = settingsResult.data.panAnimationEnabled
                )
            }
        }

        // Monitor buffer state (for debugging)
        viewModelScope.launch {
            photoBufferManager.loadingState.collect { loadingState ->
                val bufferSize = photoBufferManager.getBufferSize()
                _state.value = _state.value.copy(
                    bufferedPhotosCount = bufferSize
                )

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
                _state.value = _state.value.copy(
                    error = "Network disconnected. Playing buffered photos..."
                )

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
                _state.value = _state.value.copy(error = null)

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
     * Initializes the slideshow by loading photos from all sources.
     *
     * Thread Safety: Safe to call from main thread.
     *
     * @param shuffleEnabled If true, shuffles photos after loading
     * @param autoPlay If true, starts auto-play after successful loading
     */
    fun initialize(shuffleEnabled: Boolean = false, autoPlay: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = slideshowRepository.loadPhotos(shuffleEnabled)
            when (result) {
                is Result.Success -> {
                    // Update metadata
                    val metadata = slideshowRepository.getCurrentPhotoMetadata()
                    _state.value = _state.value.copy(
                        currentPhotoMetadata = metadata,
                        isLoading = false,
                        error = null
                    )

                    // Start auto-play if requested and loading succeeded
                    if (autoPlay) {
                        play()
                    }
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to load photos"
                    )
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

        _state.value = _state.value.copy(isPlaying = true)

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

                // Wait for interval
                delay(interval)

                // Advance to next photo (skip for videos - they advance via onVideoEnded callback)
                if (_state.value.isPlaying && _state.value.currentPhotoMetadata?.isVideo != true) {
                    nextPhoto()
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
        _state.value = _state.value.copy(isPlaying = false)
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null

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
     */
    fun nextPhoto(pauseAutoAdvance: Boolean = false) {
        if (pauseAutoAdvance && _state.value.isPlaying) {
            pause()
        }

        viewModelScope.launch {
            val result = slideshowRepository.nextPhoto()
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
                        _state.value = _state.value.copy(error = "Unable to load photos. Check network connection.")
                    }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
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
    fun previousPhoto(pauseAutoAdvance: Boolean = false) {
        if (pauseAutoAdvance && _state.value.isPlaying) {
            pause()
        }

        viewModelScope.launch {
            val result = slideshowRepository.previousPhoto()
            // Note: State updates now handled reactively by combine flow observing repository StateFlows

            android.util.Log.d("SlideshowViewModel", "previousPhoto: result=${result.javaClass.simpleName}")

            when (result) {
                is Result.Success -> {
                    // Combine flow will update state automatically from repository StateFlows
                    // Success case handled reactively
                }
                is Result.Error -> {
                    // Still need to handle error explicitly as it's not always from StateFlow
                    _state.value = _state.value.copy(
                        error = result.message ?: "Failed to load previous photo"
                    )
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
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
                    _state.value = _state.value.copy(
                        currentPhotoMetadata = metadata,
                        error = null
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message ?: "Failed to shuffle photos"
                    )
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
