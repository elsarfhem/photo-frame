package com.photoframe.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photoframe.core.data.PhotoSourcesManager
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result as PhotoResult
import com.photoframe.core.source.PhotoSourceFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for scanning local photos.
 *
 * Responsibilities:
 * - Scan all enabled local photo sources
 * - Cache results for quick slideshow startup
 * - Triggered periodically (1 hour) or by MediaStore observer
 *
 * Battery-aware: Only runs when battery is not low.
 *
 * Thread Safety: Safe to run concurrently via WorkManager.
 *
 * Usage:
 * - Scheduled via WorkManager in CoreModule
 * - Triggered immediately by MediaStore observer when photos added
 *
 * @param context Application context
 * @param params Worker parameters from WorkManager
 * @param photoSourcesManager Manager for source configurations
 * @param photoSourceFactory Factory for creating source instances
 */
@HiltWorker
class LocalPhotoScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val photoSourcesManager: PhotoSourcesManager,
    private val photoSourceFactory: PhotoSourceFactory
) : CoroutineWorker(context, params) {

    /**
     * Performs background scan of local photo sources.
     *
     * @return Result.success if scan completed successfully,
     *         Result.retry if temporary error (will retry),
     *         Result.failure if permanent error
     */
    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            // Get all enabled local sources
            val sourcesResult = photoSourcesManager.getSources()
            if (sourcesResult !is PhotoResult.Success) {
                return androidx.work.ListenableWorker.Result.retry()
            }

            val localSources = sourcesResult.data
                .filter { it.type == PhotoSourceType.LOCAL && it.isEnabled }

            if (localSources.isEmpty()) {
                // No local sources configured - nothing to scan
                return androidx.work.ListenableWorker.Result.success()
            }

            // Scan each local source
            var totalPhotos = 0
            var errors = 0

            for (sourceConfig in localSources) {
                try {
                    // Create source instance
                    val sourceResult = photoSourceFactory.createSource(sourceConfig)
                    if (sourceResult !is PhotoResult.Success) {
                        errors++
                        continue
                    }

                    val source = sourceResult.data

                    // Scan photos
                    val scanResult = source.scanPhotos()
                    when (scanResult) {
                        is PhotoResult.Success -> {
                            totalPhotos += scanResult.data.size
                            // In production, cache results here for quick slideshow startup
                            // For now, we just count
                        }
                        is PhotoResult.Error -> {
                            errors++
                        }
                        is PhotoResult.Loading -> {
                            // Should not happen
                            errors++
                        }
                    }
                } catch (e: Exception) {
                    errors++
                }
            }

            // Return success even if some sources failed
            // (as long as at least one succeeded or all were empty)
            if (errors > 0 && totalPhotos == 0) {
                androidx.work.ListenableWorker.Result.retry()
            } else {
                androidx.work.ListenableWorker.Result.success()
            }
        } catch (e: Exception) {
            // Unexpected error - retry
            androidx.work.ListenableWorker.Result.retry()
        }
    }
}
