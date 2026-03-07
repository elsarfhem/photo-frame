package com.photoframe.core.scheduling

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.photoframe.core.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom WorkerFactory for Hilt dependency injection into Workers.
 *
 * Required for injecting dependencies into ScheduleWorker.
 *
 * Phase 5: Settings & Scheduling
 */
@Singleton
class ScheduleWorkerFactory @Inject constructor(
    private val settingsRepository: SettingsRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ScheduleWorker::class.java.name -> {
                ScheduleWorker(appContext, workerParameters, settingsRepository)
            }
            else -> null
        }
    }
}
