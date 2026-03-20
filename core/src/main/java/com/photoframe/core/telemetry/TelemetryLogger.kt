package com.photoframe.core.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Telemetry logger for production monitoring and debugging.
 *
 * Integration:
 * - Firebase Crashlytics for crash reporting
 * - Custom events for non-fatal errors
 * - Breadcrumbs for debugging context
 * - Custom keys for contextual data
 *
 * Thread Safety: All methods are thread-safe.
 * Crashlytics handles internal synchronization.
 */
@Singleton
class TelemetryLogger @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    fun logEvent(event: String, message: String? = null, throwable: Throwable? = null) {
        if (throwable != null) {
            android.util.Log.e(TAG, "[$event] $message", throwable)
        } else {
            android.util.Log.i(TAG, "[$event] $message")
        }

        crashlytics.log("$event: $message")
        if (throwable != null) {
            crashlytics.recordException(throwable)
        }
    }

    fun logBreadcrumb(message: String) {
        android.util.Log.d(TAG, "[Breadcrumb] $message")
        crashlytics.log(message)
    }

    fun setCustomKey(key: String, value: String) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Long) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")
        crashlytics.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        android.util.Log.d(TAG, "[CustomKey] $key = $value")
        crashlytics.setCustomKey(key, value)
    }

    fun setUserId(userId: String) {
        android.util.Log.d(TAG, "[UserId] $userId")
        crashlytics.setUserId(userId)
    }

    fun logException(throwable: Throwable) {
        android.util.Log.e(TAG, "Exception logged", throwable)
        crashlytics.recordException(throwable)
    }

    // Convenience methods for common events

    fun logPhotoLoadFailed(photoPath: String, error: String) {
        logEvent("photo_load_failed", "Failed to load $photoPath: $error")
        setCustomKey("last_failed_photo", photoPath)
    }

    fun logNetworkDisconnect() {
        logEvent("network_disconnect", "Network disconnected during slideshow")
        logBreadcrumb("Network state: disconnected")
    }

    fun logNetworkReconnect() {
        logEvent("network_reconnect", "Network restored after disconnect")
        logBreadcrumb("Network state: connected")
    }

    fun logMemoryWarning(usagePercent: Double) {
        logEvent("memory_warning", "Memory usage at ${usagePercent.toInt()}%")
        setCustomKey("memory_usage_percent", usagePercent.toInt())
        logBreadcrumb("Memory warning triggered, clearing memory cache")
    }

    fun logMemoryCritical(usagePercent: Double) {
        logEvent("memory_critical", "Memory usage at ${usagePercent.toInt()}%")
        setCustomKey("memory_usage_percent", usagePercent.toInt())
        logBreadcrumb("Critical memory level, performing emergency cache clear")
    }

    fun logMemoryRecoveryAttempted() {
        logEvent("memory_recovery_attempted", "Emergency cache clearing and GC triggered")
        logBreadcrumb("Memory recovery: cache cleared, GC requested")
    }

    fun logAutoRestart() {
        logEvent("auto_recovery", "App auto-restarted after crash")
        logBreadcrumb("CrashHandler: auto-restart initiated")
    }

    fun logAutoRestartBlocked() {
        logEvent("auto_restart_blocked", "Too many crashes in past hour, restart blocked")
        logBreadcrumb("CrashHandler: restart blocked due to crash loop protection")
    }

    fun logWatchdogStallDetected(stallDurationMs: Long) {
        logEvent("watchdog_stall_detected", "Slideshow stalled for ${stallDurationMs}ms")
        setCustomKey("stall_duration_ms", stallDurationMs)
        logBreadcrumb("Watchdog: stall detected, attempting recovery")
    }

    fun logWatchdogRecoverySuccess() {
        logEvent("watchdog_recovery_success", "Watchdog successfully recovered slideshow")
        logBreadcrumb("Watchdog: recovery successful, slideshow resumed")
    }

    fun logWatchdogRecoveryFailed() {
        logEvent("watchdog_recovery_failed", "Watchdog failed to recover slideshow")
        logBreadcrumb("Watchdog: recovery failed, manual intervention required")
    }

    fun logAutoAdvanceError(error: String) {
        logEvent("auto_advance_error", "Auto-advance loop error: $error")
        logBreadcrumb("Auto-advance: caught error, recovering: $error")
    }

    fun setSlideshowContext(currentPhotoIndex: Int, totalPhotos: Int, bufferSize: Int) {
        setCustomKey("current_photo_index", currentPhotoIndex)
        setCustomKey("total_photos", totalPhotos)
        setCustomKey("buffer_size", bufferSize)
    }

    companion object {
        private const val TAG = "TelemetryLogger"
    }
}
