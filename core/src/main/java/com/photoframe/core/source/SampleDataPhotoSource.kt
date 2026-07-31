package com.photoframe.core.source

import com.photoframe.core.model.Photo
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result

/**
 * Photo source implementation for bundled sample/demo content.
 *
 * Serves a fixed set of JPEGs bundled as app assets. No network,
 * no credentials, no filesystem permissions required — used to let
 * a user (or a store reviewer) see the slideshow working without
 * configuring a real SMB share.
 *
 * Thread Safety: Stateless, safe to call from any coroutine.
 *
 * @param id Unique identifier for this source
 * @param displayName Display name for UI
 * @param isEnabled Whether this source is enabled
 */
class SampleDataPhotoSource(
    override val id: String,
    override val displayName: String,
    override val isEnabled: Boolean
) : PhotoSource {

    override val type: PhotoSourceType = PhotoSourceType.SAMPLE

    override suspend fun scanPhotos(maxPhotos: Int?): Result<List<Photo>> {
        val photos = SAMPLE_ASSET_NAMES.map { fileName ->
            Photo(
                path = "file:///android_asset/$SAMPLE_ASSET_DIR/$fileName",
                fileName = fileName,
                fileSize = 0L,
                lastModified = 0L,
                mimeType = "image/jpeg"
            )
        }
        val limited = maxPhotos?.let { photos.take(it) } ?: photos
        return Result.success(limited)
    }

    override suspend fun validate(): Result<Unit> = Result.success(Unit)

    override suspend fun estimatePhotoCount(): Int = SAMPLE_ASSET_NAMES.size

    override fun toString(): String {
        return "SampleDataPhotoSource(id=$id, displayName=$displayName, enabled=$isEnabled)"
    }

    companion object {
        const val SAMPLE_ASSET_DIR = "sample_photos"
        val SAMPLE_ASSET_NAMES = listOf(
            "sample_photo_1.jpg",
            "sample_photo_2.jpg",
            "sample_photo_3.jpg",
            "sample_photo_4.jpg",
            "sample_photo_5.jpg",
            "sample_photo_6.jpg"
        )
    }
}
