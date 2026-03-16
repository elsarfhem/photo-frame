package com.photoframe.core.source

import com.photoframe.core.model.Photo
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result

/**
 * Abstract interface for photo sources.
 *
 * Provides a uniform abstraction for different types of photo sources
 * (SMB network shares, local storage, cloud storage, etc.).
 *
 * Each source implementation is responsible for:
 * - Scanning for photos
 * - Validating configuration
 * - Handling source-specific errors
 *
 * Thread Safety: All methods should be thread-safe and callable from any coroutine.
 *
 * Implementations:
 * - SmbPhotoSource: Network share via SMB protocol
 * - LocalPhotoSource: Local device storage via MediaStore
 *
 * Usage:
 * ```
 * val source: PhotoSource = LocalPhotoSource(...)
 * val result = source.scanPhotos()
 * when (result) {
 *     is Result.Success -> displayPhotos(result.data)
 *     is Result.Error -> showError(result.message)
 * }
 * ```
 */
interface PhotoSource {
    /**
     * Unique identifier for this source instance.
     * Used to identify and manage sources in settings.
     *
     * Example: "smb-home-nas", "local-dcim", "smb-office-photos"
     */
    val id: String

    /**
     * Type of source (SMB, LOCAL, etc.).
     * Determines which UI and configuration options to show.
     */
    val type: PhotoSourceType

    /**
     * Human-readable display name for UI.
     * Shows in settings and source management screens.
     *
     * Example: "Home NAS Photos", "Camera Roll (DCIM)"
     */
    val displayName: String

    /**
     * Whether this source is currently enabled.
     * Disabled sources are not scanned during photo loading.
     */
    val isEnabled: Boolean

    /**
     * Scans this source for photos.
     *
     * Implementations should:
     * - Handle connection/authentication
     * - Recursively scan for supported image files
     * - Return photos with proper metadata
     * - Handle errors gracefully
     *
     * Thread Safety: Safe to call from any coroutine.
     *
     * @return Result.Success with list of photos (may be empty),
     *         Result.Error if scan failed completely
     */
    suspend fun scanPhotos(maxPhotos: Int? = null): Result<List<Photo>>

    /**
     * Validates this source's configuration.
     *
     * Checks:
     * - Required fields are present
     * - Connectivity (if network source)
     * - Permissions (if local source)
     * - Credentials (if authentication required)
     *
     * Thread Safety: Safe to call from any coroutine.
     *
     * @return Result.Success if valid and accessible,
     *         Result.Error with descriptive message if invalid
     */
    suspend fun validate(): Result<Unit>

    /**
     * Gets an estimate of the number of photos in this source.
     * Used for progress indicators during scanning.
     *
     * Thread Safety: Safe to call from any coroutine.
     *
     * @return Estimated photo count, or null if estimate unavailable
     */
    suspend fun estimatePhotoCount(): Int? = null
}
