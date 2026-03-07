package com.photoframe.core.repository

import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for multi-source photo management.
 *
 * Extends SlideshowRepository with multi-source capabilities:
 * - Manage multiple photo sources (SMB, local, etc.)
 * - Aggregate photos from all enabled sources
 * - Add/remove/update sources dynamically
 *
 * Thread Safety: All methods are thread-safe and can be called from any coroutine.
 *
 * Backwards Compatibility: Fully compatible with SlideshowRepository.
 * Existing single-source code works unchanged.
 */
interface MultiSourcePhotoRepository : SlideshowRepository {
    /**
     * StateFlow of configured photo sources.
     * Emits updates when sources are added/removed/updated.
     */
    val photoSources: StateFlow<List<PhotoSourceConfig>>

    /**
     * Adds a new photo source.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param source Photo source configuration
     * @return Result.Success if added, Result.Error if failed or ID exists
     */
    suspend fun addPhotoSource(source: PhotoSourceConfig): Result<Unit>

    /**
     * Removes a photo source.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param sourceId ID of source to remove
     * @return Result.Success if removed, Result.Error if not found or failed
     */
    suspend fun removePhotoSource(sourceId: String): Result<Unit>

    /**
     * Updates a photo source configuration.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param source Updated source configuration
     * @return Result.Success if updated, Result.Error if not found or failed
     */
    suspend fun updatePhotoSource(source: PhotoSourceConfig): Result<Unit>

    /**
     * Enables or disables a photo source.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param sourceId ID of source to toggle
     * @param enabled New enabled state
     * @return Result.Success if updated, Result.Error if not found or failed
     */
    suspend fun setSourceEnabled(sourceId: String, enabled: Boolean): Result<Unit>

    /**
     * Validates a photo source configuration.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param sourceId ID of source to validate
     * @return Result.Success if valid, Result.Error with details if invalid
     */
    suspend fun validateSource(sourceId: String): Result<Unit>
}
