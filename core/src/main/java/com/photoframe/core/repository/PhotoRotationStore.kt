package com.photoframe.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.photoframe.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists user-applied rotation overrides per photo.
 * Stores rotation degrees (0, 90, 180, 270) keyed by photo path hash.
 */
@Singleton
class PhotoRotationStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getRotation(photoPath: String): Int = withContext(ioDispatcher) {
        val prefs = dataStore.data.first()
        prefs[rotationKey(photoPath)] ?: 0
    }

    suspend fun setRotation(photoPath: String, degrees: Int) = withContext(ioDispatcher) {
        dataStore.edit { prefs ->
            val key = rotationKey(photoPath)
            val normalized = ((degrees % 360) + 360) % 360
            if (normalized == 0) prefs.remove(key) else prefs[key] = normalized
        }
    }

    private fun rotationKey(path: String): Preferences.Key<Int> =
        intPreferencesKey("photo_rotation_${path.hashCode()}")
}
