# Architecture: Multi-Source Photo Loading

**Phase 2: Architecture Design**
**Date**: 2026-03-06

## Overview

Transform the photo loading system from single SMB source to support multiple configurable sources (SMB, local storage, and extensible for future sources).

## Current Architecture

```
SlideshowRepository (interface)
    ↓
SlideshowRepositoryImpl
    ├── settingsRepository.loadSmbConnection() → SmbConnection
    ├── smbClient.connect()
    └── smbPhotoDataSource.scanFolder() → List<Photo>
```

**Limitations:**
- Hardcoded to SMB only
- Single source at a time
- Tight coupling between repository and SMB-specific logic

## Proposed Architecture

### 1. Core Abstraction: PhotoSource Interface

Create abstraction for different photo sources:

```kotlin
/**
 * Abstract interface for photo sources.
 * Each source type (SMB, Local, Cloud) implements this interface.
 */
interface PhotoSource {
    /**
     * Unique identifier for this source instance.
     */
    val id: String

    /**
     * Type of source (SMB, LOCAL, etc.).
     */
    val type: PhotoSourceType

    /**
     * Display name for UI.
     */
    val displayName: String

    /**
     * Whether this source is enabled.
     */
    val isEnabled: Boolean

    /**
     * Scans this source for photos.
     * @return Result with list of photos from this source
     */
    suspend fun scanPhotos(): Result<List<Photo>>

    /**
     * Validates source configuration.
     * @return Result.Success if valid, Result.Error with message if invalid
     */
    suspend fun validate(): Result<Unit>
}

enum class PhotoSourceType {
    SMB,
    LOCAL
    // Future: GOOGLE_DRIVE, DROPBOX, etc.
}
```

### 2. Source Implementations

#### SmbPhotoSource
```kotlin
class SmbPhotoSource(
    override val id: String,
    override val displayName: String,
    override val isEnabled: Boolean,
    private val connection: SmbConnection,
    private val password: String,
    private val smbClient: SmbClient,
    private val smbPhotoDataSource: SmbPhotoDataSource
) : PhotoSource {
    override val type = PhotoSourceType.SMB

    override suspend fun scanPhotos(): Result<List<Photo>> {
        // Connect to SMB
        // Scan folder
        // Return photos
    }

    override suspend fun validate(): Result<Unit> {
        // Test connection
    }
}
```

#### LocalPhotoSource
```kotlin
class LocalPhotoSource(
    override val id: String,
    override val displayName: String,
    override val isEnabled: Boolean,
    private val folderUris: List<Uri>,  // Selected folders via MediaPicker/SAF
    private val localPhotoDataSource: LocalPhotoDataSource
) : PhotoSource {
    override val type = PhotoSourceType.LOCAL

    override suspend fun scanPhotos(): Result<List<Photo>> {
        // Scan local folders using MediaStore
        // Return photos
    }

    override suspend fun validate(): Result<Unit> {
        // Check if URIs are still accessible
    }
}
```

### 3. New Data Source: LocalPhotoDataSource

```kotlin
/**
 * Data source for scanning photos from local device storage.
 * Uses MediaStore API for efficient querying.
 * Supports scoped storage (Android 10+).
 */
class LocalPhotoDataSource @Inject constructor(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Scans local storage for photos using MediaStore.
     * @param folderUris List of folder URIs to scan (from SAF/MediaPicker)
     * @return Result with list of photos found
     */
    suspend fun scanFolders(folderUris: List<Uri>): Result<List<Photo>>

    /**
     * Scans all media store images (DCIM, Pictures, Downloads, etc.).
     * @return Result with list of all accessible photos
     */
    suspend fun scanAllMedia(): Result<List<Photo>>

    /**
     * Registers MediaStore observer for new photo detection.
     * @param onChange Callback when new photos are detected
     */
    fun observeMediaStore(onChange: () -> Unit): ContentObserver
}
```

**MediaStore Implementation:**
- Query `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
- Filter by folder URIs if provided
- Use projection to get: `_ID`, `DISPLAY_NAME`, `SIZE`, `DATE_MODIFIED`, `MIME_TYPE`
- Convert to Photo objects with `content://` URIs
- No permissions needed for scoped storage (user-selected folders)

### 4. Multi-Source Repository

```kotlin
/**
 * Repository that aggregates photos from multiple sources.
 * Replaces direct dependency on SmbPhotoDataSource.
 */
interface MultiSourcePhotoRepository : SlideshowRepository {
    /**
     * StateFlow of configured photo sources.
     */
    val photoSources: StateFlow<List<PhotoSourceConfig>>

    /**
     * Adds a new photo source.
     * @param source Photo source configuration
     */
    suspend fun addPhotoSource(source: PhotoSourceConfig): Result<Unit>

    /**
     * Removes a photo source.
     * @param sourceId ID of source to remove
     */
    suspend fun removePhotoSource(sourceId: String): Result<Unit>

    /**
     * Updates a photo source.
     * @param source Updated source configuration
     */
    suspend fun updatePhotoSource(source: PhotoSourceConfig): Result<Unit>

    /**
     * Enables/disables a photo source.
     * @param sourceId ID of source to toggle
     * @param enabled New enabled state
     */
    suspend fun setSourceEnabled(sourceId: String, enabled: Boolean): Result<Unit>
}

class MultiSourcePhotoRepositoryImpl @Inject constructor(
    private val sourcesManager: PhotoSourcesManager,
    private val photoBufferManager: PhotoBufferManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MultiSourcePhotoRepository {

    override suspend fun loadPhotos(shuffleEnabled: Boolean): Result<Int> {
        // 1. Get all enabled sources
        // 2. Scan each source in parallel (using async)
        // 3. Aggregate results
        // 4. Shuffle if requested (across all sources)
        // 5. Initialize buffer
    }
}
```

### 5. Photo Aggregation Strategy

**Random Mix Implementation:**
```kotlin
private suspend fun aggregatePhotos(
    sources: List<PhotoSource>,
    shuffle: Boolean
): Result<List<Photo>> {
    // Scan all enabled sources in parallel
    val photoLists = sources
        .filter { it.isEnabled }
        .map { source ->
            async(ioDispatcher) {
                source.scanPhotos()
            }
        }
        .awaitAll()

    // Collect all photos
    val allPhotos = photoLists
        .filterIsInstance<Result.Success<List<Photo>>>()
        .flatMap { it.data }

    // Shuffle for random mix
    val finalPhotos = if (shuffle) {
        allPhotos.shuffled()
    } else {
        allPhotos
    }

    return if (finalPhotos.isEmpty()) {
        Result.error(
            IllegalStateException("No photos found"),
            "No photos found in any enabled source"
        )
    } else {
        Result.success(finalPhotos)
    }
}
```

### 6. Settings/Configuration Model

```kotlin
/**
 * Configuration for a photo source.
 * Persisted in DataStore as JSON.
 */
@Serializable
data class PhotoSourceConfig(
    val id: String,
    val type: PhotoSourceType,
    val displayName: String,
    val isEnabled: Boolean,
    val config: SourceConfig
)

@Serializable
sealed class SourceConfig {
    @Serializable
    data class SmbConfig(
        val server: String,
        val share: String,
        val path: String,
        val domain: String,
        val username: String
        // Password stored separately in CredentialStore
    ) : SourceConfig()

    @Serializable
    data class LocalConfig(
        val folderUris: List<String>  // Persistable URI strings
    ) : SourceConfig()
}
```

**DataStore Schema:**
```json
{
  "photo_sources": [
    {
      "id": "smb-1",
      "type": "SMB",
      "displayName": "Family Photos (NAS)",
      "isEnabled": true,
      "config": {
        "type": "smb",
        "server": "192.168.1.100",
        "share": "photos",
        "path": "/family",
        "domain": "WORKGROUP",
        "username": "user"
      }
    },
    {
      "id": "local-1",
      "type": "LOCAL",
      "displayName": "Camera (DCIM)",
      "isEnabled": true,
      "config": {
        "type": "local",
        "folderUris": [
          "content://com.android.providers.media.documents/tree/primary%3ADCIM"
        ]
      }
    }
  ]
}
```

### 7. Background Scanning with WorkManager

```kotlin
/**
 * Background worker for scanning local photos.
 * Triggered periodically or by MediaStore observer.
 */
class LocalPhotoScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val localPhotoDataSource: LocalPhotoDataSource,
    private val sourcesManager: PhotoSourcesManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Get all enabled local sources
        val localSources = sourcesManager.getEnabledSources()
            .filter { it.type == PhotoSourceType.LOCAL }

        // Scan each source
        localSources.forEach { source ->
            val scanResult = source.scanPhotos()
            // Cache results for quick slideshow startup
        }

        return Result.success()
    }
}
```

**WorkManager Setup:**
```kotlin
// In CoreModule or WorkerModule
val scanRequest = PeriodicWorkRequestBuilder<LocalPhotoScanWorker>(
    repeatInterval = 1,
    repeatIntervalTimeUnit = TimeUnit.HOURS
)
    .setConstraints(
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
    )
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "local_photo_scan",
        ExistingPeriodicWorkPolicy.KEEP,
        scanRequest
    )
```

### 8. MediaStore Observer

```kotlin
/**
 * Observes MediaStore for new photos.
 * Triggers immediate scan when photos are added.
 */
class MediaStoreObserver @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager
) {
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Trigger immediate scan
            val scanRequest = OneTimeWorkRequestBuilder<LocalPhotoScanWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            workManager.enqueue(scanRequest)
        }
    }

    fun start() {
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    fun stop() {
        context.contentResolver.unregisterContentObserver(contentObserver)
    }
}
```

### 9. Photo Model Updates

**No changes needed to Photo model** - it's already generic:
```kotlin
data class Photo(
    val path: String,  // Can be smb:// or content:// URI
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String
)
```

**Optional: Add source metadata:**
```kotlin
data class Photo(
    val path: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String,
    val sourceId: String? = null,  // NEW: Track which source this came from
    val sourceType: PhotoSourceType? = null  // NEW: Track source type
)
```

### 10. Coil Integration for Local Photos

Coil already supports `content://` URIs out of the box, but we can optimize:

```kotlin
/**
 * Custom Coil Fetcher for local photos.
 * Optimizes thumbnail loading from MediaStore.
 */
class LocalPhotoFetcher(
    private val context: Context,
    private val data: Uri
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        // Use MediaStore thumbnail API for efficient loading
        val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(
                data,
                Size(1920, 1080),
                null
            )
        } else {
            // Fallback to full load
            context.contentResolver.openInputStream(data)?.use {
                BitmapFactory.decodeStream(it)
            }
        }

        return DrawableResult(
            drawable = BitmapDrawable(context.resources, thumbnail),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }
}
```

## Migration Strategy

### Phase 1: Abstraction Layer (Non-Breaking)
1. Create PhotoSource interface
2. Create PhotoSourceConfig model
3. Implement SmbPhotoSource (wraps existing logic)
4. Keep SlideshowRepositoryImpl working as-is

### Phase 2: Multi-Source Repository
1. Create MultiSourcePhotoRepositoryImpl
2. Migrate SlideshowRepositoryImpl to use PhotoSourcesManager
3. Add backward compatibility for existing SMB config

### Phase 3: Local Source
1. Implement LocalPhotoDataSource
2. Implement LocalPhotoSource
3. Add WorkManager background scanning
4. Add MediaStore observer

### Phase 4: Settings UI
1. Update settings screens to support multiple sources
2. Add source add/remove/edit UI
3. Migrate existing SMB settings

## Data Flow

```
User Configures Sources (UI)
    ↓
PhotoSourceConfig → DataStore + CredentialStore
    ↓
MultiSourcePhotoRepository.loadPhotos()
    ↓
PhotoSourcesManager.getEnabledSources()
    ↓
[SmbPhotoSource.scanPhotos(), LocalPhotoSource.scanPhotos()] (parallel)
    ↓
Aggregate + Shuffle (random mix)
    ↓
PhotoBufferManager.initialize()
    ↓
Display in slideshow
```

## File Structure

```
core/src/main/java/com/photoframe/core/
├── model/
│   ├── Photo.kt (existing - no changes)
│   ├── PhotoSourceConfig.kt (NEW)
│   └── PhotoSourceType.kt (NEW)
├── source/
│   ├── PhotoSource.kt (NEW - interface)
│   ├── SmbPhotoSource.kt (NEW)
│   └── LocalPhotoSource.kt (NEW)
├── data/
│   ├── SmbPhotoDataSource.kt (existing)
│   ├── LocalPhotoDataSource.kt (NEW)
│   └── PhotoSourcesManager.kt (NEW)
├── repository/
│   ├── SlideshowRepository.kt (existing - extend interface)
│   ├── MultiSourcePhotoRepositoryImpl.kt (NEW)
│   └── SettingsRepository.kt (update for multiple sources)
├── worker/
│   ├── LocalPhotoScanWorker.kt (NEW)
│   └── MediaStoreObserver.kt (NEW)
└── di/
    └── CoreModule.kt (update DI bindings)
```

## Backwards Compatibility

**Migration Path for Existing Users:**
1. On first launch with new version, detect existing SmbConnection in DataStore
2. Convert to PhotoSourceConfig with id="smb-migrated-1"
3. Save to new photo_sources list
4. Mark migration complete
5. Keep working exactly as before (single SMB source)

```kotlin
suspend fun migrateExistingSmbConfig() {
    val existingSmb = settingsRepository.loadSmbConnection()
    if (existingSmb is Result.Success && existingSmb.data != null) {
        val connection = existingSmb.data
        val sourceConfig = PhotoSourceConfig(
            id = "smb-migrated-1",
            type = PhotoSourceType.SMB,
            displayName = "SMB Share (${connection.server})",
            isEnabled = true,
            config = SourceConfig.SmbConfig(
                server = connection.server,
                share = connection.share,
                path = connection.path,
                domain = connection.domain,
                username = connection.username
            )
        )
        sourcesManager.addSource(sourceConfig)
    }
}
```

## Performance Considerations

1. **Parallel Scanning**: Use `async` to scan multiple sources simultaneously
2. **MediaStore Thumbnails**: Use MediaStore thumbnail API for local photos (faster than full decode)
3. **Background Caching**: WorkManager scans and caches local photo list for instant slideshow start
4. **Incremental Loading**: Keep existing IncrementalPhotoLoader pattern for large collections
5. **Source Isolation**: One source failure doesn't block others (collect all successful results)

## Security Considerations

1. **Scoped Storage**: Use SAF/MediaPicker - no broad storage permissions needed
2. **Credential Storage**: Continue using KeystoreCredentialStore for SMB passwords
3. **URI Permissions**: Persist URI permissions using `takePersistableUriPermission()`
4. **Source Validation**: Validate each source before loading (test connectivity, check permissions)

## Testing Strategy

1. **Unit Tests**: Test PhotoSource implementations independently
2. **Integration Tests**: Test multi-source aggregation logic
3. **E2E Tests**: Test UI flows for adding/removing sources
4. **Performance Tests**: Test with 10,000+ photos across multiple sources
5. **Migration Tests**: Test backward compatibility with existing SMB config

## Next Steps

1. Review architecture with team
2. Create detailed implementation tasks
3. Proceed to Phase 4: Test Planning
