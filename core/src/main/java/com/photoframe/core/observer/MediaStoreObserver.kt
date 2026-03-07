package com.photoframe.core.observer

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.photoframe.core.worker.LocalPhotoScanWorker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes MediaStore for new photos and triggers immediate scan.
 *
 * Purpose:
 * - Detects when user takes photos or adds images to device
 * - Triggers immediate background scan via WorkManager
 * - Provides near real-time updates to photo list
 *
 * Lifecycle:
 * - Start observing when app launches
 * - Stop observing when app is destroyed
 * - Automatic restart after app restart
 *
 * Battery Impact: Minimal - passive observation only.
 *
 * Thread Safety: ContentObserver callbacks run on Main thread (via Handler).
 *
 * @param context Application context for ContentResolver and WorkManager
 */
@Singleton
class MediaStoreObserver @Inject constructor(
    private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val workManager: WorkManager = WorkManager.getInstance(context)

    private var contentObserver: ContentObserver? = null

    /**
     * Starts observing MediaStore for image changes.
     *
     * Safe to call multiple times - subsequent calls are no-ops.
     */
    fun start() {
        if (contentObserver != null) {
            // Already observing
            return
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // New photo detected - trigger immediate scan
                triggerImmediateScan()
            }
        }

        // Register observer for external images
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true, // Notify for descendants (all images)
            observer
        )

        contentObserver = observer
    }

    /**
     * Stops observing MediaStore.
     *
     * Call when app is being destroyed to avoid memory leaks.
     */
    fun stop() {
        contentObserver?.let { observer ->
            contentResolver.unregisterContentObserver(observer)
            contentObserver = null
        }
    }

    /**
     * Triggers immediate background scan via WorkManager.
     *
     * Uses expedited work request for faster execution.
     * Replaces any pending scan request (debouncing).
     */
    private fun triggerImmediateScan() {
        val scanRequest = OneTimeWorkRequestBuilder<LocalPhotoScanWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        // Use REPLACE policy to debounce rapid photo additions
        workManager.enqueueUniqueWork(
            IMMEDIATE_SCAN_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            scanRequest
        )
    }

    companion object {
        /**
         * Unique work name for immediate scans.
         * Using REPLACE policy ensures only one scan runs at a time.
         */
        private const val IMMEDIATE_SCAN_WORK_NAME = "local_photo_immediate_scan"
    }
}
