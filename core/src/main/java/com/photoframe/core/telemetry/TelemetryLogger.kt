package com.photoframe.core.telemetry

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Telemetry logger for production monitoring and debugging.
 *
 * P0 BLOCKING: Provides observability for 24/7 deployed tablets.
 *
 * Integration:
 * - Firebase Crashlytics for crash reporting
 * - Custom events for non-fatal errors
 * - Breadcrumbs for debugging context
 * - Custom keys for contextual data
 *
 * Thread Safety: All methods are thread-safe.
 * Crashlytics handles internal synchronization.
 *
 * Events Logged:
 * - photo_load_failed: Photo failed to load from SMB
 * - network_disconnect: Network disconnected during slideshow
 * - network_reconnect: Network restored after disconnect
 * - memory_warning: Memory usage reached 75% threshold
 * - memory_critical: Memory usage reached 90% threshold
 * - memory_recovery_attempted: Emergency cache clearing triggered
 * - auto_recovery: Crash handler auto-restarted app
 * - auto_restart_blocked: Too many crashes, restart blocked
 * - watchdog_stall_detected: Slideshow stalled, watchdog triggered
 * - watchdog_recovery_success: Watchdog successfully recovered slideshow
 * - watchdog_recovery_failed: Watchdog failed to recover slideshow
 *
 * Note: Firebase Crashlytics integration is stubbed out for now.
 * In production, you would:
 * 1. Add Firebase SDK to build.gradle
 * 2. Add google-services.json
 * 3. Uncomment Crashlytics calls below
 *
 * @constructor Creates a telemetry logger
 */
@Singleton
class TelemetryLogger @Inject constructor() {

    /**
     * Logs a non-fatal error event.
     *
     * @param event Event name (e.g., "photo_load_failed")
     * @param message Optional message for context
     * @param throwable Optional exception
     */
    fun logEvent(event: String, message: String? = null, throwable: Throwable? = null) {
        // Console logging for development
        if (throwable != null) {
            android.util.Log.e(TAG, "[$event] $message", throwable)
        } else {
            android.util.Log.i(TAG, "[$event] $message")
        }

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().apply {
        //     log("$event: $message")
        //     if (throwable != null) {
        //         recordException(throwable)
        //     }
        // }
    }

    /**
     * Logs a breadcrumb for debugging context.
     * Breadcrumbs are included in crash reports.
     *
     * @param message Breadcrumb message
     */
    fun logBreadcrumb(message: String) {
        android.util.Log.d(TAG, "[Breadcrumb] $message")

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().log(message)
    }

    /**
     * Sets a custom key for contextual data.
     * Custom keys are included in crash reports.
     *
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: String) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * Sets a custom key with integer value.
     *
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Int) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * Sets a custom key with long value.
     *
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Long) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * Sets a custom key with boolean value.
     *
     * @param key Key name
     * @param value Key value
     */
    fun setCustomKey(key: String, value: Boolean) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    /**
     * Sets a user identifier for crash reports.
     *
     * @param userId User ID (e.g., device serial number)
     */
    fun setUserId(userId: String) {
        android.util.Log.d(TAG, "[UserId] $userId")

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    /**
     * Logs an exception to Crashlytics.
     * Use for non-fatal exceptions that should be tracked.
     *
     * @param throwable Exception to log
     */
    fun logException(throwable: Throwable) {
        android.util.Log.e(TAG, "Exception logged", throwable)

        // TODO: Firebase Crashlytics integration
        // FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    // Convenience methods for common events

    /**
     * Logs photo load failure.
     *
     * @param photoPath Path to photo that failed to load
     * @param error Error message or exception
     */
    fun logPhotoLoadFailed(photoPath: String, error: String) {
        logEvent("photo_load_failed", "Failed to load $photoPath: $error")
        setCustomKey("last_failed_photo", photoPath)
    }

    /**
     * Logs network disconnect event.
     */
    fun logNetworkDisconnect() {
        logEvent("network_disconnect", "Network disconnected during slideshow")
        logBreadcrumb("Network state: disconnected")
    }

    /**
     * Logs network reconnect event.
     */
    fun logNetworkReconnect() {
        logEvent("network_reconnect", "Network restored after disconnect")
        logBreadcrumb("Network state: connected")
    }

    /**
     * Logs memory warning (75% threshold).
     *
     * @param usagePercent Memory usage percentage
     */
    fun logMemoryWarning(usagePercent: Double) {
        logEvent("memory_warning", "Memory usage at ${usagePercent.toInt()}%")
        setCustomKey("memory_usage_percent", usagePercent.toInt())
        logBreadcrumb("Memory warning triggered, clearing memory cache")
    }

    /**
     * Logs critical memory (90% threshold).
     *
     * @param usagePercent Memory usage percentage
     */
    fun logMemoryCritical(usagePercent: Double) {
        logEvent("memory_critical", "Memory usage at ${usagePercent.toInt()}%")
        setCustomKey("memory_usage_percent", usagePercent.toInt())
        logBreadcrumb("Critical memory level, performing emergency cache clear")
    }

    /**
     * Logs memory recovery attempt.
     */
    fun logMemoryRecoveryAttempted() {
        logEvent("memory_recovery_attempted", "Emergency cache clearing and GC triggered")
        logBreadcrumb("Memory recovery: cache cleared, GC requested")
    }

    /**
     * Logs auto-restart after crash.
     */
    fun logAutoRestart() {
        logEvent("auto_recovery", "App auto-restarted after crash")
        logBreadcrumb("CrashHandler: auto-restart initiated")
    }

    /**
     * Logs auto-restart blocked (too many crashes).
     */
    fun logAutoRestartBlocked() {
        logEvent("auto_restart_blocked", "Too many crashes in past hour, restart blocked")
        logBreadcrumb("CrashHandler: restart blocked due to crash loop protection")
    }

    /**
     * Logs watchdog stall detection.
     *
     * @param stallDurationMs Duration of stall in milliseconds
     */
    fun logWatchdogStallDetected(stallDurationMs: Long) {
        logEvent("watchdog_stall_detected", "Slideshow stalled for ${stallDurationMs}ms")
        setCustomKey("stall_duration_ms", stallDurationMs)
        logBreadcrumb("Watchdog: stall detected, attempting recovery")
    }

    /**
     * Logs successful watchdog recovery.
     */
    fun logWatchdogRecoverySuccess() {
        logEvent("watchdog_recovery_success", "Watchdog successfully recovered slideshow")
        logBreadcrumb("Watchdog: recovery successful, slideshow resumed")
    }

    /**
     * Logs failed watchdog recovery.
     */
    fun logWatchdogRecoveryFailed() {
        logEvent("watchdog_recovery_failed", "Watchdog failed to recover slideshow")
        logBreadcrumb("Watchdog: recovery failed, manual intervention required")
    }

    /**
     * Logs an auto-advance loop error that was caught and recovered from.
     */
    fun logAutoAdvanceError(error: String) {
        logEvent("auto_advance_error", "Auto-advance loop error: $error")
        logBreadcrumb("Auto-advance: caught error, recovering: $error")
    }

    /**
     * Sets slideshow context keys for crash reports.
     *
     * @param currentPhotoIndex Current photo index
     * @param totalPhotos Total number of photos
     * @param bufferSize Current buffer size
     */
    fun setSlideshowContext(currentPhotoIndex: Int, totalPhotos: Int, bufferSize: Int) {
        setCustomKey("current_photo_index", currentPhotoIndex)
        setCustomKey("total_photos", totalPhotos)
        setCustomKey("buffer_size", bufferSize)
    }

    companion object {
        private const val TAG = "TelemetryLogger"
    }
}
