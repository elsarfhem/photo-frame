package com.photoframe.core.scheduling

import android.content.Context
import androidx.work.*
import com.photoframe.core.model.SlideshowSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages slideshow scheduling using WorkManager.
 *
 * Schedules daily start and stop of the slideshow based on SlideshowSettings.
 * Uses WorkManager for reliable, persistent scheduling across reboots.
 *
 * Thread Safety: All methods are thread-safe and can be called from any thread.
 * WorkManager operations are thread-safe by design.
 *
 * Phase 5: Settings & Scheduling
 *
 * @param context Application context for WorkManager
 */
@Singleton
class ScheduleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedules daily start and stop of slideshow based on settings.
     *
     * Cancels any existing schedule before creating a new one.
     *
     * @param settings Slideshow settings with schedule configuration
     * @return True if schedule was created successfully
     */
    fun scheduleDaily(settings: SlideshowSettings): Boolean {
        if (!settings.scheduleEnabled) {
            android.util.Log.i(TAG, "Schedule not enabled, skipping")
            return false
        }

        // Cancel existing schedule
        cancelSchedule()

        android.util.Log.i(TAG, "Scheduling slideshow: ${settings.scheduleStartTime} - ${settings.scheduleEndTime}")

        try {
            // Schedule START work
            val startDelay = calculateDelayUntilTime(settings.scheduleStartTime)
            val startWorkRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setInitialDelay(startDelay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ScheduleWorker.KEY_ACTION to ScheduleWorker.ACTION_START_SLIDESHOW
                    )
                )
                .addTag(TAG_START_WORK)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false) // Run even on low battery
                        .build()
                )
                .build()

            // Schedule STOP work
            val stopDelay = calculateDelayUntilTime(settings.scheduleEndTime)
            val stopWorkRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setInitialDelay(stopDelay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ScheduleWorker.KEY_ACTION to ScheduleWorker.ACTION_STOP_SLIDESHOW
                    )
                )
                .addTag(TAG_STOP_WORK)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            // Enqueue work requests
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_START,
                ExistingPeriodicWorkPolicy.REPLACE,
                startWorkRequest
            )

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_STOP,
                ExistingPeriodicWorkPolicy.REPLACE,
                stopWorkRequest
            )

            android.util.Log.i(TAG, "Schedule created: start in ${startDelay.toMinutes()} min, stop in ${stopDelay.toMinutes()} min")
            return true

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to schedule slideshow", e)
            return false
        }
    }

    /**
     * Cancels the current slideshow schedule.
     */
    fun cancelSchedule() {
        android.util.Log.i(TAG, "Cancelling slideshow schedule")
        workManager.cancelUniqueWork(WORK_NAME_START)
        workManager.cancelUniqueWork(WORK_NAME_STOP)
    }

    /**
     * Checks if a schedule is currently active.
     *
     * @return True if schedule work is enqueued or running
     */
    fun isScheduleActive(): Boolean {
        val startWorkInfo = workManager.getWorkInfosForUniqueWork(WORK_NAME_START).get()
        val stopWorkInfo = workManager.getWorkInfosForUniqueWork(WORK_NAME_STOP).get()

        val hasActiveStart = startWorkInfo.any { info ->
            info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING
        }

        val hasActiveStop = stopWorkInfo.any { info ->
            info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING
        }

        return hasActiveStart || hasActiveStop
    }

    /**
     * Calculates delay until the specified time today or tomorrow.
     *
     * If the target time has already passed today, schedules for tomorrow.
     *
     * @param targetTime Target time (HH:mm)
     * @return Duration until target time
     */
    private fun calculateDelayUntilTime(targetTime: LocalTime): Duration {
        val now = LocalDateTime.now()
        val todayTarget = now.toLocalDate().atTime(targetTime)

        return if (now.isBefore(todayTarget)) {
            // Target time is later today
            Duration.between(now, todayTarget)
        } else {
            // Target time has passed today, schedule for tomorrow
            val tomorrowTarget = now.toLocalDate().plusDays(1).atTime(targetTime)
            Duration.between(now, tomorrowTarget)
        }
    }

    companion object {
        private const val TAG = "ScheduleManager"
        private const val WORK_NAME_START = "slideshow_schedule_start"
        private const val WORK_NAME_STOP = "slideshow_schedule_stop"
        private const val TAG_START_WORK = "slideshow_start"
        private const val TAG_STOP_WORK = "slideshow_stop"
    }
}
