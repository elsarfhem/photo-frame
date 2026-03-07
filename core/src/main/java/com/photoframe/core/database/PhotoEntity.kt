package com.photoframe.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.photoframe.core.model.Photo

/**
 * Room entity for persisting photo metadata.
 *
 * Enables instant startup by loading cached photos from database
 * instead of scanning SMB share every time.
 *
 * @param path Unique SMB path (primary key)
 * @param fileName File name
 * @param fileSize File size in bytes
 * @param lastModified Last modified timestamp (milliseconds)
 * @param mimeType MIME type
 * @param sourceId Photo source identifier
 */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey
    val path: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String,
    val sourceId: String
)

/**
 * Converts PhotoEntity to domain Photo model.
 */
fun PhotoEntity.toPhoto(): Photo {
    return Photo(
        path = path,
        fileName = fileName,
        fileSize = fileSize,
        lastModified = lastModified,
        mimeType = mimeType
    )
}

/**
 * Converts domain Photo to PhotoEntity.
 */
fun Photo.toEntity(sourceId: String): PhotoEntity {
    return PhotoEntity(
        path = path,
        fileName = fileName,
        fileSize = fileSize,
        lastModified = lastModified,
        mimeType = mimeType,
        sourceId = sourceId
    )
}
