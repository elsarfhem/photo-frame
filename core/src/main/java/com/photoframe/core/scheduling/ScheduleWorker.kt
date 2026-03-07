package com.photoframe.core.scheduling

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photoframe.core.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for scheduled slideshow start/stop.
 *
 * Handles two actions:
 * - START_SLIDESHOW: Starts the slideshow at scheduled time
 * - STOP_SLIDESHOW: Stops the slideshow and turns off display at scheduled time
 *
 * Uses WAKE_LOCK to ensure the device wakes up for scheduled actions.
 *
 * Thread Safety: Worker is instantiated and run on a background thread by WorkManager.
 *
 * Phase 5: Settings & Scheduling
 *
 * Note: This is a simplified implementation. In production, you would need to:
 * 1. Send broadcasts to MainActivity to start/stop slideshow
 * 2. Control screen on/off via PowerManager
 * 3. Handle wake locks properly
 *
 * @param context Application context
 * @param params Worker parameters from WorkManager
 * @param settingsRepository Repository for loading settings (injected by Hilt)
 */
class ScheduleWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()

        android.util.Log.i(TAG, "Executing scheduled action: $action")

        // Acquire wake lock to ensure device is awake
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PhotoFrame::ScheduleWorker"
        ).apply {
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }

        try {
            when (action) {
                ACTION_START_SLIDESHOW -> startSlideshow()
                ACTION_STOP_SLIDESHOW -> stopSlideshow()
                else -> {
                    android.util.Log.w(TAG, "Unknown action: $action")
                    return Result.failure()
                }
            }

            return Result.success()

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to execute scheduled action: $action", e)
            return Result.retry()

        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    /**
     * Starts the slideshow by broadcasting intent to MainActivity.
     */
    private fun startSlideshow() {
        android.util.Log.i(TAG, "Starting scheduled slideshow")

        // Send broadcast to start slideshow
        val intent = Intent(ACTION_SCHEDULE_START_SLIDESHOW).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.sendBroadcast(intent)

        // Optionally, turn on the screen
        turnOnScreen()
    }

    /**
     * Stops the slideshow by broadcasting intent to MainActivity.
     */
    private fun stopSlideshow() {
        android.util.Log.i(TAG, "Stopping scheduled slideshow")

        // Send broadcast to stop slideshow
        val intent = Intent(ACTION_SCHEDULE_STOP_SLIDESHOW).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.sendBroadcast(intent)

        // Optionally, turn off the screen
        turnOffScreen()
    }

    /**
     * Turns on the screen using PowerManager.
     *
     * Requires WAKE_LOCK permission.
     */
    private fun turnOnScreen() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "PhotoFrame::ScreenOn"
            )
            wakeLock.acquire(SCREEN_WAKE_LOCK_TIMEOUT_MS)
            wakeLock.release()

            android.util.Log.i(TAG, "Screen turned on")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to turn on screen", e)
        }
    }

    /**
     * Turns off the screen using PowerManager.
     *
     * Note: This requires DEVICE_ADMIN permission in production.
     * For MVP, this is a placeholder.
     */
    private fun turnOffScreen() {
        try {
            // In production, you would need to:
            // 1. Register as device admin
            // 2. Call DevicePolicyManager.lockNow()
            //
            // For MVP, we just log the action
            android.util.Log.i(TAG, "Screen turn-off requested (not implemented in MVP)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to turn off screen", e)
        }
    }

    companion object {
        private const val TAG = "ScheduleWorker"

        /**
         * Input data key for the action to perform.
         */
        const val KEY_ACTION = "action"

        /**
         * Action: Start the slideshow.
         */
        const val ACTION_START_SLIDESHOW = "start_slideshow"

        /**
         * Action: Stop the slideshow.
         */
        const val ACTION_STOP_SLIDESHOW = "stop_slideshow"

        /**
         * Broadcast action for starting slideshow (sent to MainActivity).
         */
        const val ACTION_SCHEDULE_START_SLIDESHOW = "com.photoframe.ACTION_SCHEDULE_START_SLIDESHOW"

        /**
         * Broadcast action for stopping slideshow (sent to MainActivity).
         */
        const val ACTION_SCHEDULE_STOP_SLIDESHOW = "com.photoframe.ACTION_SCHEDULE_STOP_SLIDESHOW"

        /**
         * Wake lock timeout: 5 minutes (for safety).
         */
        private const val WAKE_LOCK_TIMEOUT_MS = 5 * 60 * 1000L

        /**
         * Screen wake lock timeout: 5 seconds.
         */
        private const val SCREEN_WAKE_LOCK_TIMEOUT_MS = 5 * 1000L
    }
}
