package com.photoframe.core.source

import android.net.Uri
import com.photoframe.core.data.LocalPhotoDataSource
import com.photoframe.core.model.Photo
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result

/**
 * Photo source implementation for local device storage.
 *
 * Uses MediaStore API to scan photos from user-selected folders.
 * Supports scoped storage (Android 10+) - no broad permissions needed.
 *
 * Thread Safety: All methods are thread-safe via LocalPhotoDataSource.
 *
 * Lifecycle:
 * 1. User selects folders via SAF/MediaPicker
 * 2. App persists URI permissions
 * 3. Create instance with folder URIs
 * 4. Call validate() to check permissions
 * 5. Call scanPhotos() to load photo list
 *
 * @param id Unique identifier for this source
 * @param displayName Display name for UI
 * @param isEnabled Whether this source is enabled
 * @param folderUris List of content:// URIs for selected folders
 * @param localPhotoDataSource Data source for MediaStore queries
 */
class LocalPhotoSource(
    override val id: String,
    override val displayName: String,
    override val isEnabled: Boolean,
    private val folderUris: List<Uri>,
    private val localPhotoDataSource: LocalPhotoDataSource
) : PhotoSource {

    override val type: PhotoSourceType = PhotoSourceType.LOCAL

    /**
     * Scans local folders for photos.
     *
     * Uses MediaStore to query images from selected folders.
     * Returns photos with content:// URIs that can be loaded by Coil.
     *
     * Thread Safety: Safe to call from any coroutine.
     *
     * Error Handling: Returns Result.Error if:
     * - Permissions revoked (SecurityException)
     * - Folders no longer accessible
     * - MediaStore query fails
     *
     * @return Result.Success with photos (empty list if no photos),
     *         Result.Error if scan failed
     */
    override suspend fun scanPhotos(): Result<List<Photo>> {
        return try {
            if (folderUris.isEmpty()) {
                // No folders configured - scan all media
                localPhotoDataSource.scanAllMedia()
            } else {
                // Scan specific folders
                localPhotoDataSource.scanFolders(folderUris)
            }
        } catch (e: Exception) {
            Result.error(
                e,
                "Failed to scan local source '$displayName': ${e.message}"
            )
        }
    }

    /**
     * Validates local source configuration.
     *
     * Checks:
     * - Folder URIs are not empty (unless scanning all media)
     * - URIs are still accessible
     * - Permissions are still granted
     *
     * Thread Safety: Safe to call from any coroutine.
     *
     * @return Result.Success if valid and accessible,
     *         Result.Error if invalid or permissions revoked
     */
    override suspend fun validate(): Result<Unit> {
        return try {
            // Try a test scan to verify access
            val testResult = if (folderUris.isEmpty()) {
                localPhotoDataSource.scanAllMedia()
            } else {
                localPhotoDataSource.scanFolders(folderUris)
            }

            when (testResult) {
                is Result.Success -> {
                    // Access successful
                    Result.success(Unit)
                }
                is Result.Error -> {
                    // Access failed
                    Result.error(
                        testResult.exception,
                        "Cannot access local folders: ${testResult.message}"
                    )
                }
                is Result.Loading -> {
                    Result.error(
                        IllegalStateException("Unexpected loading state"),
                        "Validation returned unexpected state"
                    )
                }
            }
        } catch (e: Exception) {
            Result.error(
                e,
                "Validation failed for local source '$displayName': ${e.message}"
            )
        }
    }

    /**
     * Estimates number of photos in local folders.
     *
     * Uses MediaStore count query for efficiency.
     * Returns null if estimation is not feasible.
     *
     * @return Estimated photo count, or null
     */
    override suspend fun estimatePhotoCount(): Int? {
        return try {
            // Quick estimate by scanning (in production, use COUNT query)
            val result = scanPhotos()
            if (result is Result.Success) {
                result.data.size
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun toString(): String {
        return "LocalPhotoSource(id=$id, displayName=$displayName, " +
                "folderCount=${folderUris.size}, enabled=$isEnabled)"
    }
}
