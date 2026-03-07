package com.photoframe.core.reliability

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.telemetry.TelemetryLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

/**
 * Handles uncaught exceptions and implements auto-restart logic.
 *
 * P0 BLOCKING: Addresses "No mechanism to recover from crash/ANR - device becomes bricked" (Senior Dev 3).
 *
 * Strategy:
 * - Set Thread.setDefaultUncaughtExceptionHandler() in Application class
 * - Log crash to Crashlytics before restart
 * - Restart MainActivity automatically after crash
 * - Preserve last slideshow state in DataStore (current photo index)
 * - Resume from last position on restart
 * - Add crash counter to prevent restart loops (max 3 restarts/hour)
 *
 * Thread Safety: Synchronized access to crash counter, DataStore operations are thread-safe.
 *
 * Integration:
 * - Set up in PhotoFrameApplication.onCreate()
 * - Provide main activity class via setMainActivity()
 *
 * @param context Application context for restart intent
 * @param dataStore DataStore for persisting crash counter and slideshow state
 * @param telemetryLogger Telemetry logger for Crashlytics integration
 * @param dispatcher Coroutine dispatcher for background operations
 */
@Singleton
class CrashHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val telemetryLogger: TelemetryLogger,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : Thread.UncaughtExceptionHandler {

    private val scope = CoroutineScope(dispatcher)
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    // Main activity class for restart (set during initialization)
    private var mainActivityClass: Class<*>? = null

    /**
     * Sets the main activity class for restart.
     * Must be called during Application.onCreate() before crash occurs.
     *
     * @param activityClass Main activity class (e.g., MainActivity::class.java)
     */
    fun setMainActivity(activityClass: Class<*>) {
        this.mainActivityClass = activityClass
    }

    /**
     * Installs this crash handler as the default uncaught exception handler.
     * Preserves the existing handler as a fallback.
     *
     * Thread Safety: Must be called from Application.onCreate() (main thread).
     */
    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * Handles uncaught exceptions.
     * Logs crash, checks restart eligibility, and restarts app if safe.
     *
     * Thread Safety: Safe to call from any thread (exception handler thread).
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Log crash to console
            android.util.Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

            // Log to Crashlytics
            telemetryLogger.logException(throwable)
            telemetryLogger.setCustomKey("crash_thread", thread.name)
            telemetryLogger.logBreadcrumb("Auto-restart will be attempted")

            // Check if we should restart (prevent restart loops)
            runBlocking {
                if (shouldAttemptRestart()) {
                    recordCrash()
                    restartApp()
                } else {
                    android.util.Log.e(TAG, "Too many crashes in past hour, not restarting")
                    telemetryLogger.logAutoRestartBlocked()
                    // Delegate to default handler (shows crash dialog)
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in crash handler", e)
            // Fallback to default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Checks if we should attempt auto-restart.
     * Prevents restart loops by limiting to 3 restarts per hour.
     *
     * Thread Safety: Safe to call from any thread (uses synchronized DataStore).
     *
     * @return true if restart is safe, false if too many recent crashes
     */
    private suspend fun shouldAttemptRestart(): Boolean {
        val preferences = dataStore.data.first()
        val crashCount = preferences[KEY_CRASH_COUNT] ?: 0
        val firstCrashTime = preferences[KEY_FIRST_CRASH_TIME] ?: 0L
        val currentTime = System.currentTimeMillis()

        // Reset counter if more than 1 hour has passed
        if (currentTime - firstCrashTime > CRASH_WINDOW_MS) {
            dataStore.edit { prefs ->
                prefs[KEY_CRASH_COUNT] = 0
                prefs[KEY_FIRST_CRASH_TIME] = 0L
            }
            return true
        }

        // Check if crash count exceeds limit
        return crashCount < MAX_CRASHES_PER_HOUR
    }

    /**
     * Records a crash occurrence.
     * Increments crash counter and sets first crash time if needed.
     *
     * Thread Safety: Safe to call from any thread (uses synchronized DataStore).
     */
    private suspend fun recordCrash() {
        val currentTime = System.currentTimeMillis()
        dataStore.edit { prefs ->
            val crashCount = prefs[KEY_CRASH_COUNT] ?: 0
            val firstCrashTime = prefs[KEY_FIRST_CRASH_TIME] ?: 0L

            if (firstCrashTime == 0L) {
                // First crash in window
                prefs[KEY_FIRST_CRASH_TIME] = currentTime
                prefs[KEY_CRASH_COUNT] = 1
            } else {
                // Subsequent crash
                prefs[KEY_CRASH_COUNT] = crashCount + 1
            }
        }
    }

    /**
     * Restarts the application by launching the main activity.
     * Clears the task stack and exits the process.
     *
     * Thread Safety: Safe to call from any thread.
     */
    private fun restartApp() {
        try {
            val activityClass = mainActivityClass
            if (activityClass == null) {
                android.util.Log.e(TAG, "Main activity class not set, cannot restart")
                telemetryLogger.logEvent("auto_restart_failed_no_activity", "Main activity class not set")
                exitProcess(1)
                return
            }

            // Create restart intent
            val intent = Intent(context, activityClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_RESTARTED_AFTER_CRASH, true)
            }

            // Create pending intent for restart
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule restart
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            alarmManager?.set(
                android.app.AlarmManager.RTC,
                System.currentTimeMillis() + 1000, // Restart after 1 second
                pendingIntent
            )

            telemetryLogger.logAutoRestart()

            // Exit process
            exitProcess(0)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to restart app", e)
            telemetryLogger.logEvent("auto_restart_failed", "Exception during restart", e)
            exitProcess(1)
        }
    }

    /**
     * Saves the current slideshow state for recovery after restart.
     * Called by SlideshowViewModel when state changes.
     *
     * Thread Safety: Safe to call from any thread (uses coroutine scope).
     *
     * @param photoIndex Current photo index
     * @param totalPhotos Total number of photos
     * @param isPlaying True if slideshow is playing
     */
    fun saveSlideshowState(photoIndex: Int, totalPhotos: Int, isPlaying: Boolean) {
        scope.launch {
            try {
                dataStore.edit { prefs ->
                    prefs[KEY_LAST_PHOTO_INDEX] = photoIndex
                    prefs[KEY_TOTAL_PHOTOS] = totalPhotos
                    prefs[KEY_WAS_PLAYING] = if (isPlaying) 1 else 0
                    prefs[KEY_STATE_SAVED_TIME] = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to save slideshow state", e)
            }
        }
    }

    /**
     * Loads the saved slideshow state after restart.
     * Called by SlideshowViewModel during initialization if restarted after crash.
     *
     * Thread Safety: Safe to call from any thread (uses suspend function).
     *
     * @return Saved slideshow state, or null if no state saved or expired
     */
    suspend fun loadSlideshowState(): SlideshowState? {
        return try {
            val preferences = dataStore.data.first()
            val photoIndex = preferences[KEY_LAST_PHOTO_INDEX] ?: return null
            val totalPhotos = preferences[KEY_TOTAL_PHOTOS] ?: return null
            val wasPlaying = (preferences[KEY_WAS_PLAYING] ?: 0) == 1
            val savedTime = preferences[KEY_STATE_SAVED_TIME] ?: 0L

            // Check if state is stale (> 5 minutes old)
            if (System.currentTimeMillis() - savedTime > STATE_EXPIRY_MS) {
                return null
            }

            SlideshowState(
                photoIndex = photoIndex,
                totalPhotos = totalPhotos,
                wasPlaying = wasPlaying
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load slideshow state", e)
            null
        }
    }

    /**
     * Clears the saved slideshow state.
     * Called after successful state restoration.
     *
     * Thread Safety: Safe to call from any thread (uses coroutine scope).
     */
    fun clearSlideshowState() {
        scope.launch {
            try {
                dataStore.edit { prefs ->
                    prefs.remove(KEY_LAST_PHOTO_INDEX)
                    prefs.remove(KEY_TOTAL_PHOTOS)
                    prefs.remove(KEY_WAS_PLAYING)
                    prefs.remove(KEY_STATE_SAVED_TIME)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to clear slideshow state", e)
            }
        }
    }

    companion object {
        private const val TAG = "CrashHandler"

        /**
         * Intent extra key for restart after crash flag.
         */
        const val EXTRA_RESTARTED_AFTER_CRASH = "restarted_after_crash"

        /**
         * Maximum number of crashes allowed per hour before blocking auto-restart.
         */
        private const val MAX_CRASHES_PER_HOUR = 3

        /**
         * Crash window duration: 1 hour in milliseconds.
         */
        private val CRASH_WINDOW_MS = TimeUnit.HOURS.toMillis(1)

        /**
         * State expiry duration: 5 minutes in milliseconds.
         * Prevents stale state from being restored.
         */
        private val STATE_EXPIRY_MS = TimeUnit.MINUTES.toMillis(5)

        // DataStore keys
        private val KEY_CRASH_COUNT = intPreferencesKey("crash_count")
        private val KEY_FIRST_CRASH_TIME = longPreferencesKey("first_crash_time")
        private val KEY_LAST_PHOTO_INDEX = intPreferencesKey("last_photo_index")
        private val KEY_TOTAL_PHOTOS = intPreferencesKey("total_photos")
        private val KEY_WAS_PLAYING = intPreferencesKey("was_playing")
        private val KEY_STATE_SAVED_TIME = longPreferencesKey("state_saved_time")
    }
}

/**
 * Saved slideshow state for recovery after crash.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * @param photoIndex Current photo index (0-based)
 * @param totalPhotos Total number of photos in slideshow
 * @param wasPlaying True if slideshow was playing when crashed
 */
data class SlideshowState(
    val photoIndex: Int,
    val totalPhotos: Int,
    val wasPlaying: Boolean
)
