package com.photoframe.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for photo database operations.
 *
 * Thread Safety: Room ensures thread-safe operations.
 */
@Dao
interface PhotoDao {
    /**
     * Inserts photos into database.
     * Replaces existing photos with same path.
     *
     * @param photos List of photos to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    /**
     * Gets all photos for a specific source.
     *
     * @param sourceId Photo source identifier
     * @return List of photos from this source
     */
    @Query("SELECT * FROM photos WHERE sourceId = :sourceId ORDER BY path")
    suspend fun getPhotosForSource(sourceId: String): List<PhotoEntity>

    /**
     * Gets all photos from all sources.
     *
     * @return List of all photos
     */
    @Query("SELECT * FROM photos ORDER BY path")
    suspend fun getAllPhotos(): List<PhotoEntity>

    /**
     * Deletes all photos for a specific source.
     *
     * @param sourceId Photo source identifier
     */
    @Query("DELETE FROM photos WHERE sourceId = :sourceId")
    suspend fun deletePhotosForSource(sourceId: String)

    /**
     * Deletes all photos from database.
     */
    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos()

    /**
     * Gets photo count for a source.
     *
     * @param sourceId Photo source identifier
     * @return Number of photos
     */
    @Query("SELECT COUNT(*) FROM photos WHERE sourceId = :sourceId")
    suspend fun getPhotoCount(sourceId: String): Int

    /**
     * Checks if a photo exists in database.
     *
     * @param path Photo path
     * @return True if photo exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM photos WHERE path = :path)")
    suspend fun photoExists(path: String): Boolean
}
