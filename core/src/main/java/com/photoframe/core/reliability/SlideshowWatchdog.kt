package com.photoframe.core.reliability

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.photoframe.core.repository.SlideshowRepository
import com.photoframe.core.telemetry.TelemetryLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that monitors slideshow health and restarts if stalled.
 *
 * P0 BLOCKING: Addresses "No watchdog to detect and recover from slideshow stalls" (Senior Dev 3).
 *
 * Strategy:
 * - Run as foreground service with notification (required for 24/7 operation)
 * - Monitor current photo changes
 * - Detect if slideshow stalls (no photo change for 2x display interval)
 * - Auto-restart slideshow if stalled
 * - Log watchdog events to Crashlytics
 *
 * Thread Safety: Single-threaded service with coroutine-based monitoring.
 * StateFlow updates are atomic.
 *
 * Integration:
 * - Started by SlideshowViewModel when slideshow begins
 * - Stopped when slideshow ends or app exits
 *
 * Note: This is a foreground service to prevent Android from killing it during 24/7 operation.
 */
@AndroidEntryPoint
class SlideshowWatchdog : Service() {

    @Inject
    lateinit var slideshowRepository: SlideshowRepository

    @Inject
    lateinit var telemetryLogger: TelemetryLogger

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null

    // Watchdog state
    private val _watchdogState = MutableStateFlow<WatchdogState>(WatchdogState.Idle)
    val watchdogState: StateFlow<WatchdogState> = _watchdogState.asStateFlow()

    // Last photo change tracking
    private var lastPhotoIndex: Int = -1
    private var lastPhotoChangeTime: Long = 0L
    private var displayIntervalMs: Long = DEFAULT_DISPLAY_INTERVAL_MS
    private var stallThresholdMs: Long = displayIntervalMs * 2

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                displayIntervalMs = intent.getLongExtra(EXTRA_DISPLAY_INTERVAL_MS, DEFAULT_DISPLAY_INTERVAL_MS)
                stallThresholdMs = displayIntervalMs * 2
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
            ACTION_UPDATE_INTERVAL -> {
                displayIntervalMs = intent.getLongExtra(EXTRA_DISPLAY_INTERVAL_MS, DEFAULT_DISPLAY_INTERVAL_MS)
                stallThresholdMs = displayIntervalMs * 2
            }
        }

        // Return START_STICKY to ensure service is restarted if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        scope.cancel()
    }

    /**
     * Starts monitoring the slideshow for stalls.
     * Runs as foreground service with notification.
     */
    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return // Already monitoring

        // Start foreground service with notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        _watchdogState.value = WatchdogState.Monitoring

        // Initialize tracking
        lastPhotoChangeTime = System.currentTimeMillis()
        lastPhotoIndex = -1

        // Start monitoring coroutine
        monitoringJob = scope.launch {
            // Initial state
            lastPhotoIndex = slideshowRepository.getCurrentPhotoIndex()
            lastPhotoChangeTime = System.currentTimeMillis()

            while (true) {
                try {
                    // Check every 5 seconds
                    delay(CHECK_INTERVAL_MS)

                    // Get current photo index
                    val currentPhotoIndex = slideshowRepository.getCurrentPhotoIndex()

                    // Check if photo changed
                    if (currentPhotoIndex != lastPhotoIndex) {
                        // Photo changed, update tracking
                        lastPhotoIndex = currentPhotoIndex
                        lastPhotoChangeTime = System.currentTimeMillis()
                        _watchdogState.value = WatchdogState.Monitoring
                    } else {
                        // Photo hasn't changed, check if stalled
                        val timeSinceLastChange = System.currentTimeMillis() - lastPhotoChangeTime
                        if (timeSinceLastChange > stallThresholdMs) {
                            // Slideshow is stalled
                            handleStalledSlideshow()
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Coroutine was cancelled (normal shutdown) - exit loop
                    android.util.Log.d(TAG, "Watchdog monitoring cancelled")
                    throw e  // Re-throw to properly cancel the coroutine
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in watchdog monitoring", e)
                    telemetryLogger.logEvent("watchdog_monitoring_error", e.message, e)
                }
            }
        }
    }

    /**
     * Stops monitoring the slideshow.
     */
    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _watchdogState.value = WatchdogState.Idle
    }

    /**
     * Handles a stalled slideshow by attempting restart.
     *
     * Actions:
     * - Log stall event to Crashlytics
     * - Attempt to advance to next photo
     * - If that fails, reload entire slideshow
     * - Update watchdog state
     */
    private suspend fun handleStalledSlideshow() {
        android.util.Log.w(TAG, "Slideshow stall detected, attempting recovery")
        _watchdogState.value = WatchdogState.Recovering

        // Log to Crashlytics
        val stallDurationMs = System.currentTimeMillis() - lastPhotoChangeTime
        telemetryLogger.logWatchdogStallDetected(stallDurationMs)

        try {
            // First attempt: Try to advance to next photo
            val result = slideshowRepository.nextPhoto()
            if (result is com.photoframe.core.model.Result.Success) {
                android.util.Log.i(TAG, "Slideshow recovered by advancing to next photo")
                telemetryLogger.logWatchdogRecoverySuccess()
                lastPhotoIndex = slideshowRepository.getCurrentPhotoIndex()
                lastPhotoChangeTime = System.currentTimeMillis()
                _watchdogState.value = WatchdogState.Monitoring
                return
            }

            // Second attempt: Reload entire slideshow
            android.util.Log.w(TAG, "Advance failed, attempting full slideshow reload")
            telemetryLogger.logBreadcrumb("Watchdog: advance failed, attempting full reload")

            // Note: Full reload requires coordination with ViewModel
            // For now, just reset tracking and let user/app handle it
            lastPhotoChangeTime = System.currentTimeMillis()
            _watchdogState.value = WatchdogState.Failed

            // TODO: Send broadcast to MainActivity to trigger reload
            val intent = Intent(ACTION_SLIDESHOW_STALLED)
            sendBroadcast(intent)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to recover stalled slideshow", e)
            telemetryLogger.logWatchdogRecoveryFailed()
            _watchdogState.value = WatchdogState.Failed
        }
    }

    /**
     * Creates the notification channel for the foreground service.
     * Required for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // Low importance to avoid user distraction
            ).apply {
                description = "Monitors slideshow health in the background"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the foreground service notification.
     *
     * @return Notification for foreground service
     */
    private fun createNotification(): Notification {
        // TODO: Replace with actual MainActivity class
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Photo Frame Active")
            .setContentText("Monitoring slideshow")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Cannot be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "SlideshowWatchdog"

        // Service actions
        const val ACTION_START_MONITORING = "com.photoframe.WATCHDOG_START"
        const val ACTION_STOP_MONITORING = "com.photoframe.WATCHDOG_STOP"
        const val ACTION_UPDATE_INTERVAL = "com.photoframe.WATCHDOG_UPDATE_INTERVAL"
        const val ACTION_SLIDESHOW_STALLED = "com.photoframe.SLIDESHOW_STALLED"

        // Intent extras
        const val EXTRA_DISPLAY_INTERVAL_MS = "display_interval_ms"

        // Notification
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "slideshow_watchdog"
        private const val CHANNEL_NAME = "Slideshow Monitor"

        // Monitoring parameters
        private const val CHECK_INTERVAL_MS = 5_000L // Check every 5 seconds
        private const val DEFAULT_DISPLAY_INTERVAL_MS = 10_000L // Default 10 seconds

        /**
         * Creates an intent to start the watchdog service.
         *
         * @param context Application context
         * @param displayIntervalMs Display interval in milliseconds
         * @return Intent to start service
         */
        fun createStartIntent(context: Context, displayIntervalMs: Long): Intent {
            return Intent(context, SlideshowWatchdog::class.java).apply {
                action = ACTION_START_MONITORING
                putExtra(EXTRA_DISPLAY_INTERVAL_MS, displayIntervalMs)
            }
        }

        /**
         * Creates an intent to stop the watchdog service.
         *
         * @param context Application context
         * @return Intent to stop service
         */
        fun createStopIntent(context: Context): Intent {
            return Intent(context, SlideshowWatchdog::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
        }

        /**
         * Creates an intent to update the display interval.
         *
         * @param context Application context
         * @param displayIntervalMs New display interval in milliseconds
         * @return Intent to update interval
         */
        fun createUpdateIntervalIntent(context: Context, displayIntervalMs: Long): Intent {
            return Intent(context, SlideshowWatchdog::class.java).apply {
                action = ACTION_UPDATE_INTERVAL
                putExtra(EXTRA_DISPLAY_INTERVAL_MS, displayIntervalMs)
            }
        }
    }
}

/**
 * Represents the watchdog monitoring state.
 *
 * Thread Safety: Immutable sealed class, safe to share across threads.
 */
sealed class WatchdogState {
    /**
     * Watchdog is idle (not monitoring).
     */
    object Idle : WatchdogState()

    /**
     * Watchdog is actively monitoring the slideshow.
     */
    object Monitoring : WatchdogState()

    /**
     * Watchdog detected a stall and is attempting recovery.
     */
    object Recovering : WatchdogState()

    /**
     * Watchdog recovery failed.
     */
    object Failed : WatchdogState()
}
