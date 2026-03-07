package com.photoframe.core.model

import androidx.compose.runtime.Immutable

/**
 * Represents a photo from an SMB share.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * @property path Full SMB path to the photo file (e.g., "smb://server/share/folder/photo.jpg")
 * @property fileName Name of the file (e.g., "photo.jpg")
 * @property fileSize Size of the file in bytes
 * @property lastModified Timestamp when the file was last modified (milliseconds since epoch)
 * @property mimeType MIME type of the photo (e.g., "image/jpeg")
 */
@Immutable
data class Photo(
    val path: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String
) {
    /**
     * Returns the file extension (e.g., "jpg", "png").
     */
    val extension: String
        get() = fileName.substringAfterLast('.', "")

    /**
     * Returns true if this is a JPEG image.
     */
    val isJpeg: Boolean
        get() = mimeType.equals("image/jpeg", ignoreCase = true) ||
                extension.lowercase() in setOf("jpg", "jpeg")

    /**
     * Returns true if this is a PNG image.
     */
    val isPng: Boolean
        get() = mimeType.equals("image/png", ignoreCase = true) ||
                extension.lowercase() == "png"

    /**
     * Returns true if this is a video file.
     */
    val isVideo: Boolean
        get() = mimeType.startsWith("video/", ignoreCase = true) ||
                extension.lowercase() in setOf("mp4", "mov", "avi", "mkv", "webm", "m4v")
}
