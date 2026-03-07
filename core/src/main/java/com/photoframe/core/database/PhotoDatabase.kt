package com.photoframe.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for photo metadata persistence.
 *
 * Enables instant startup by caching photo metadata locally.
 * Background sync keeps data fresh.
 *
 * Version 1: Initial schema
 */
@Database(
    entities = [PhotoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhotoDatabase : RoomDatabase() {
    /**
     * Photo data access object.
     */
    abstract fun photoDao(): PhotoDao
}
