package com.photoframe.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.photoframe.core.reliability.CrashHandler
import com.photoframe.core.reliability.MemoryMonitor
import com.photoframe.core.worker.LocalPhotoScanWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application class for the Photo Frame app.
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Implements Configuration.Provider for custom WorkManager configuration with Hilt.
 *
 * Phase 4 (Reliability Features):
 * - Installs CrashHandler for auto-recovery from crashes
 * - Starts MemoryMonitor for preemptive cache clearing
 *
 * Multi-Source Feature:
 * - Schedules periodic background scanning of local photo sources
 * - Triggers immediate scan on app launch
 */
@HiltAndroidApp
class PhotoFrameApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var crashHandler: CrashHandler

    @Inject
    lateinit var memoryMonitor: MemoryMonitor

    @Inject
    lateinit var workManager: WorkManager

    override fun onCreate() {
        super.onCreate()

        // P0 BLOCKING: Install crash handler for auto-restart on crash
        // Addresses: "No mechanism to recover from crash/ANR - device becomes bricked"
        crashHandler.setMainActivity(MainActivity::class.java)
        crashHandler.install()

        // P0 BLOCKING: Start memory monitoring to prevent OOM crashes
        // Addresses: "Memory leaks virtually guaranteed for 24/7 operation"
        memoryMonitor.startMonitoring()

        // Multi-Source Feature: Set up background scanning for local photos
        setupLocalPhotoScanning()
    }

    /**
     * Provides WorkManager configuration with Hilt's WorkerFactory.
     * Required for @HiltWorker to work properly.
     * Uses EntryPoint to avoid circular dependency during initialization.
     */
    override val workManagerConfiguration: Configuration
        get() {
            val workerFactory = EntryPointAccessors.fromApplication(
                this,
                WorkerFactoryEntryPoint::class.java
            ).hiltWorkerFactory()

            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    /**
     * Sets up periodic background scanning for local photo sources.
     *
     * Configuration:
     * - Runs every 1 hour
     * - Only when battery is not low
     * - Persists across app restarts
     * - Triggers immediate scan on first launch
     */
    private fun setupLocalPhotoScanning() {
        // Schedule periodic work (1 hour intervals)
        val periodicScanRequest = PeriodicWorkRequestBuilder<LocalPhotoScanWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        // Enqueue with KEEP policy - don't restart if already scheduled
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SCAN_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicScanRequest
        )

        // Trigger immediate scan on app launch
        val immediateScanRequest = OneTimeWorkRequestBuilder<LocalPhotoScanWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        // Use REPLACE policy to ensure only one immediate scan
        workManager.enqueueUniqueWork(
            IMMEDIATE_SCAN_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateScanRequest
        )
    }

    companion object {
        /**
         * Unique work name for periodic local photo scanning.
         */
        private const val PERIODIC_SCAN_WORK_NAME = "local_photo_periodic_scan"

        /**
         * Unique work name for immediate scan on app launch.
         */
        private const val IMMEDIATE_SCAN_WORK_NAME = "local_photo_immediate_scan"
    }
}

/**
 * Hilt EntryPoint for accessing HiltWorkerFactory.
 * Needed to break circular dependency during Application initialization.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerFactoryEntryPoint {
    fun hiltWorkerFactory(): HiltWorkerFactory
}
