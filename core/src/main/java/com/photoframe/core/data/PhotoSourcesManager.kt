package com.photoframe.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// DataStore extension
private val Context.photoSourcesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "photo_sources"
)

/**
 * Manages photo source configurations.
 *
 * Responsibilities:
 * - Persist source configurations to DataStore
 * - Provide reactive access to source list
 * - Add/remove/update sources
 * - Enable/disable sources
 *
 * Thread Safety: All methods are thread-safe using Mutex protection.
 *
 * Storage: Uses DataStore Preferences with JSON serialization.
 * Each source is stored as a separate key: "source_{id}"
 *
 * @param context Application context for DataStore access
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
@Singleton
class PhotoSourcesManager @Inject constructor(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val dataStore = context.photoSourcesDataStore
    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Flow of all configured photo sources.
     *
     * Emits updates whenever sources are added/removed/updated.
     * Returns empty list if no sources configured.
     */
    val sources: Flow<List<PhotoSourceConfig>> = dataStore.data.map { preferences ->
        preferences.asMap()
            .filterKeys { it.name.startsWith(SOURCE_PREFIX) }
            .values
            .mapNotNull { value ->
                try {
                    json.decodeFromString<PhotoSourceConfig>(value as String)
                } catch (e: Exception) {
                    null // Skip invalid entries
                }
            }
            .sortedBy { it.id } // Consistent ordering
    }

    /**
     * Gets all configured photo sources.
     *
     * @return Result.Success with list of sources (may be empty)
     */
    suspend fun getSources(): Result<List<PhotoSourceConfig>> = withContext(ioDispatcher) {
        return@withContext try {
            val sourceList = sources.first()
            Result.success(sourceList)
        } catch (e: Exception) {
            Result.error(e, "Failed to load sources: ${e.message}")
        }
    }

    /**
     * Gets all enabled photo sources.
     *
     * @return List of enabled sources
     */
    suspend fun getEnabledSources(): List<PhotoSourceConfig> = withContext(ioDispatcher) {
        return@withContext try {
            sources.first().filter { it.isEnabled }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets a specific source by ID.
     *
     * @param sourceId Source ID to retrieve
     * @return Result.Success with source, or Result.Error if not found
     */
    suspend fun getSource(sourceId: String): Result<PhotoSourceConfig> = withContext(ioDispatcher) {
        return@withContext try {
            val source = sources.first().find { it.id == sourceId }
            if (source != null) {
                Result.success(source)
            } else {
                Result.error(
                    NoSuchElementException("Source not found"),
                    "Source '$sourceId' does not exist"
                )
            }
        } catch (e: Exception) {
            Result.error(e, "Failed to get source: ${e.message}")
        }
    }

    /**
     * Adds a new photo source.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param source Source configuration to add
     * @return Result.Success if added, Result.Error if failed or ID already exists
     */
    suspend fun addSource(source: PhotoSourceConfig): Result<Unit> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            try {
                // Check if ID already exists
                val existing = sources.first().find { it.id == source.id }
                if (existing != null) {
                    return@withContext Result.error(
                        IllegalArgumentException("Source ID already exists"),
                        "A source with ID '${source.id}' already exists"
                    )
                }

                // Save to DataStore
                dataStore.edit { preferences ->
                    val key = stringPreferencesKey("$SOURCE_PREFIX${source.id}")
                    preferences[key] = json.encodeToString(source)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Failed to add source: ${e.message}")
            }
        }
    }

    /**
     * Updates an existing photo source.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param source Updated source configuration
     * @return Result.Success if updated, Result.Error if not found or failed
     */
    suspend fun updateSource(source: PhotoSourceConfig): Result<Unit> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            try {
                // Check if source exists
                val existing = sources.first().find { it.id == source.id }
                if (existing == null) {
                    return@withContext Result.error(
                        NoSuchElementException("Source not found"),
                        "Source '${source.id}' does not exist"
                    )
                }

                // Update in DataStore
                dataStore.edit { preferences ->
                    val key = stringPreferencesKey("$SOURCE_PREFIX${source.id}")
                    preferences[key] = json.encodeToString(source)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Failed to update source: ${e.message}")
            }
        }
    }

    /**
     * Removes a photo source.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param sourceId ID of source to remove
     * @return Result.Success if removed, Result.Error if not found or failed
     */
    suspend fun removeSource(sourceId: String): Result<Unit> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            try {
                // Check if source exists
                val existing = sources.first().find { it.id == sourceId }
                if (existing == null) {
                    return@withContext Result.error(
                        NoSuchElementException("Source not found"),
                        "Source '$sourceId' does not exist"
                    )
                }

                // Remove from DataStore
                dataStore.edit { preferences ->
                    val key = stringPreferencesKey("$SOURCE_PREFIX$sourceId")
                    preferences.remove(key)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Failed to remove source: ${e.message}")
            }
        }
    }

    /**
     * Enables or disables a photo source.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @param sourceId ID of source to toggle
     * @param enabled New enabled state
     * @return Result.Success if updated, Result.Error if not found or failed
     */
    suspend fun setSourceEnabled(sourceId: String, enabled: Boolean): Result<Unit> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            try {
                // Get existing source
                val source = sources.first().find { it.id == sourceId }
                    ?: return@withContext Result.error(
                        NoSuchElementException("Source not found"),
                        "Source '$sourceId' does not exist"
                    )

                // Update enabled state
                val updated = source.copy(isEnabled = enabled)

                // Save to DataStore
                dataStore.edit { preferences ->
                    val key = stringPreferencesKey("$SOURCE_PREFIX$sourceId")
                    preferences[key] = json.encodeToString(updated)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Failed to update source state: ${e.message}")
            }
        }
    }

    /**
     * Clears all photo sources.
     *
     * Thread Safety: Safe to call concurrently.
     *
     * @return Result.Success if cleared, Result.Error if failed
     */
    suspend fun clearAllSources(): Result<Unit> = withContext(ioDispatcher) {
        return@withContext mutex.withLock {
            try {
                dataStore.edit { preferences ->
                    // Remove all source keys
                    val sourceKeys = preferences.asMap().keys
                        .filter { it.name.startsWith(SOURCE_PREFIX) }
                    sourceKeys.forEach { key ->
                        preferences.remove(key)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(e, "Failed to clear sources: ${e.message}")
            }
        }
    }

    companion object {
        /**
         * Prefix for DataStore keys.
         * Keys are formatted as: "source_{id}"
         */
        private const val SOURCE_PREFIX = "source_"
    }
}
