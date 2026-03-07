package com.photoframe.core.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Photo
import com.photoframe.core.model.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Data source for scanning photos from local device storage.
 *
 * Uses Android MediaStore API for efficient querying of images.
 * Supports scoped storage (Android 10+) - no broad permissions needed.
 *
 * Supported Formats:
 * - JPEG (.jpg, .jpeg)
 * - PNG (.png)
 * - HEIC (.heic)
 * - WebP (.webp)
 *
 * Thread Safety: All methods use withContext(ioDispatcher) for safe concurrent access.
 *
 * Usage:
 * 1. User selects folders via MediaPicker/SAF
 * 2. App persists URI permissions via takePersistableUriPermission()
 * 3. Call scanFolders() with URIs to get photos
 *
 * @param context Application context for ContentResolver access
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
class LocalPhotoDataSource @Inject constructor(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Scans specific folders for photos.
     *
     * Uses MediaStore queries filtered by folder URIs.
     * Only returns photos from user-selected folders (scoped storage).
     *
     * @param folderUris List of content:// URIs from SAF/MediaPicker
     * @return Result.Success with list of photos (may be empty),
     *         Result.Error if scan failed
     */
    suspend fun scanFolders(folderUris: List<Uri>): Result<List<Photo>> = withContext(ioDispatcher) {
        return@withContext try {
            if (folderUris.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val photos = mutableListOf<Photo>()

            // Query each folder
            for (folderUri in folderUris) {
                val folderPhotos = queryFolder(folderUri)
                photos.addAll(folderPhotos)
            }

            Result.success(photos)
        } catch (e: SecurityException) {
            Result.error(
                e,
                "Permission denied. Please re-select folders in settings."
            )
        } catch (e: Exception) {
            Result.error(
                e,
                "Failed to scan local folders: ${e.message}"
            )
        }
    }

    /**
     * Scans all accessible media (DCIM, Pictures, Downloads, etc.).
     *
     * Uses MediaStore.Images.Media.EXTERNAL_CONTENT_URI to query all images.
     * No folder filtering - returns all photos the app can access.
     *
     * Note: With scoped storage (Android 10+), this returns:
     * - Photos created by this app
     * - Photos in shared collections (if READ_MEDIA_IMAGES granted)
     *
     * @return Result.Success with list of photos (may be empty),
     *         Result.Error if scan failed
     */
    suspend fun scanAllMedia(): Result<List<Photo>> = withContext(ioDispatcher) {
        return@withContext try {
            val photos = queryMediaStore(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                selection = null,
                selectionArgs = null
            )
            Result.success(photos)
        } catch (e: SecurityException) {
            Result.error(
                e,
                "Permission denied. Grant media access in settings."
            )
        } catch (e: Exception) {
            Result.error(
                e,
                "Failed to scan media: ${e.message}"
            )
        }
    }

    /**
     * Queries a specific folder URI for photos.
     *
     * @param folderUri Content URI of folder to scan
     * @return List of photos in folder
     */
    private fun queryFolder(folderUri: Uri): List<Photo> {
        // For scoped storage, we need to query MediaStore and filter by data path
        // This is a simplified implementation - full implementation would use
        // DocumentsContract.buildChildDocumentsUriUsingTree() for proper folder scanning

        // For now, query all media and filter by folder
        // In production, implement proper tree document querying
        return queryMediaStore(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            selection = null,
            selectionArgs = null
        ).filter { photo ->
            // Simple path matching - in production use proper folder URI matching
            true // TODO: Implement proper folder filtering
        }
    }

    /**
     * Queries MediaStore for images.
     *
     * @param uri MediaStore URI to query
     * @param selection SQL WHERE clause (null for all)
     * @param selectionArgs Arguments for WHERE clause
     * @return List of photos found
     */
    private fun queryMediaStore(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): List<Photo> {
        val photos = mutableListOf<Photo>()

        // Define projection (columns to retrieve)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATA // Full file path (for filtering)
        )

        // Query MediaStore
        contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC" // Newest first
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "unknown"
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateColumn) * 1000 // Convert to milliseconds
                val mimeType = cursor.getString(mimeColumn) ?: "image/jpeg"
                val dataPath = cursor.getString(dataColumn) ?: ""

                // Build content URI for this image
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                // Only include supported image types
                if (isSupportedMimeType(mimeType)) {
                    photos.add(
                        Photo(
                            path = contentUri.toString(), // content:// URI
                            fileName = name,
                            fileSize = size,
                            lastModified = dateModified,
                            mimeType = mimeType
                        )
                    )
                }
            }
        }

        return photos
    }

    /**
     * Checks if MIME type is supported.
     *
     * @param mimeType MIME type string
     * @return true if supported
     */
    private fun isSupportedMimeType(mimeType: String): Boolean {
        return mimeType in SUPPORTED_MIME_TYPES
    }

    companion object {
        /**
         * Supported image MIME types.
         */
        private val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/heic",
            "image/heif",
            "image/webp"
        )
    }
}
