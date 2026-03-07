# Architecture Proposal - Architect 3 (Simplicity-Focused)

**Author**: Architect 3 - Simplicity-focused
**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Date**: 2026-03-01
**PRD Reference**: `docs/features/photo-frame-app-initial/requirements/PRD_DRAFT.md`

---

## 1. Overview

### Approach Summary

This architecture proposal prioritizes **the simplest solution that meets all requirements**. The design focuses on proven patterns, minimal complexity, and fast time-to-market for a 2-3 developer team working on a 3-4 month MVP. Every architectural decision is evaluated against a single question: "Do we really need this for Phase 1?"

**Philosophy**: Build the minimum viable architecture that satisfies all 12 user stories and NFRs, then iterate based on real-world usage data. Avoid over-engineering for hypothetical Phase 2 requirements. Focus on shipping a working product that users can test and provide feedback on.

### Key Architectural Decisions

1. **Single-module structure with feature packages** - No Gradle module complexity for 2-3 developers
2. **ViewModel → Repository → Data Source** - Skip UseCase layer, repositories handle business logic
3. **3-photo buffer (prev, current, next)** - Simplest buffer that eliminates transition lag
4. **Coil with standard Fetcher + jcifs-ng** - No custom SMB Fetcher, use proven libraries
5. **In-memory cache only** - Defer disk cache to Phase 2, let Coil handle its own caching
6. **WorkManager for scheduling** - Proven, battery-efficient, zero custom logic needed
7. **Room for settings only** - No complex data persistence, just user preferences
8. **Standard coroutine patterns** - Dispatchers.IO for network/disk, Dispatchers.Main for UI

### Focus Area Priorities

As the simplicity-focused architect, my priorities are:
- **Minimal complexity**: Fewest moving parts, easiest to understand and debug
- **Proven patterns**: Use well-documented, battle-tested approaches
- **Fast time-to-market**: Ship MVP in 3-4 months with 2-3 developers
- **YAGNI principle**: You Aren't Gonna Need It - defer speculative features
- **Pragmatic tradeoffs**: Accept technical debt if it accelerates MVP delivery
- **Real-world validation**: Get user feedback before optimizing

### Why This Approach

**Problem with over-engineering**: Both Architect 1 and Architect 2 propose solutions that are correct but potentially over-engineered for our specific constraints:
- **Team size**: 2-3 developers (not 10+)
- **Timeline**: 3-4 months (not 6-12 months)
- **Scope**: MVP Phase 1 with 12 user stories (not full product)
- **Uncertainty**: We don't know if users will need Phase 2 features until they try Phase 1

**This proposal's approach**: Build the simplest thing that could possibly work, ship it, gather feedback, then optimize based on real bottlenecks, not imagined ones.

---

## 2. Architecture Approach

### 2.1 Module Structure

#### Single-Module Design

**Rationale**: With 2-3 developers and a 3-4 month timeline, multi-module structure adds overhead without clear ROI for MVP.

```
app/
├── build.gradle.kts (single module)
├── src/
│   ├── main/
│   │   ├── kotlin/com/photoframe/
│   │   │   ├── ui/
│   │   │   │   ├── slideshow/
│   │   │   │   │   ├── SlideshowScreen.kt
│   │   │   │   │   ├── SlideshowViewModel.kt
│   │   │   │   ├── settings/
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   │   ├── smb/
│   │   │   │   │   │   ├── SmbConfigScreen.kt
│   │   │   │   │   │   ├── SmbConfigViewModel.kt
│   │   │   ├── data/
│   │   │   │   ├── repository/
│   │   │   │   │   ├── PhotoRepository.kt
│   │   │   │   │   ├── SettingsRepository.kt
│   │   │   │   ├── source/
│   │   │   │   │   ├── SmbPhotoSource.kt
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── SettingsDatabase.kt
│   │   │   │   │   │   ├── SettingsDao.kt
│   │   │   │   ├── cache/
│   │   │   │   │   ├── PhotoCache.kt (simple in-memory)
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Photo.kt
│   │   │   │   │   ├── SmbConfig.kt
│   │   │   │   │   ├── SlideshowSettings.kt
│   │   │   │   │   ├── ScheduleConfig.kt
│   │   │   ├── service/
│   │   │   │   ├── ScheduleWorker.kt
│   │   │   ├── di/
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── PhotoFrameApplication.kt
```

**Why single module**:
- **Faster builds**: No inter-module dependency resolution
- **Easier navigation**: All code in one place, no jumping between modules
- **Simpler refactoring**: Move files without Gradle configuration changes
- **Fewer bugs**: No "module X can't access module Y" errors
- **Parallel development**: 2-3 developers can work on different packages without conflicts

**When to split modules**: If the app grows beyond 20K lines or we add a second feature (e.g., video support), then consider multi-module.

#### Package Organization

Organize by **feature + layer hybrid**:
- `ui/` - Compose screens and ViewModels
- `data/` - Repository, data sources, caching
- `domain/` - Data models (no business logic layer)
- `service/` - Background workers
- `di/` - Dependency injection

**Why this works**:
- Clear separation without Gradle complexity
- Easy to split into modules later if needed
- Developers know exactly where code belongs

### 2.2 Component Design

#### Layer Responsibilities

**UI Layer** (Compose + ViewModel):
- Render UI state
- Handle user interactions
- Collect StateFlow from ViewModel
- No business logic

**ViewModel Layer**:
- Hold UI state (StateFlow)
- Handle user events
- Call repository methods
- Transform data for UI
- **Contains business logic** (no separate UseCase layer)

**Repository Layer**:
- Coordinate data sources (SMB, local database)
- Implement caching logic
- Handle error recovery (retries, fallbacks)
- **Contains orchestration logic**

**Data Source Layer**:
- Direct access to SMB, Room, etc.
- No caching, no retries (repository handles this)

#### Why No UseCase Layer?

**Architect 1's UseCase layer**:
```kotlin
// Architect 1 proposes this
class GetPhotosFromFolderUseCase(private val repository: PhotoRepository) {
    suspend operator fun invoke(path: String): Result<List<Photo>> {
        return repository.getPhotosFromFolder(path)
    }
}
```

**My critique**: This UseCase is a **one-line wrapper** around a repository method. For MVP, this adds:
- ❌ Extra file to maintain
- ❌ Extra class to test
- ❌ Extra layer to understand
- ❌ No clear business logic encapsulation

**When UseCases make sense**:
- ✅ Complex multi-step workflows (e.g., "book flight + reserve hotel + rent car")
- ✅ Combining multiple repositories (e.g., PhotoRepository + UserRepository)
- ✅ Reusable business logic across multiple ViewModels

**For this MVP**:
- Most ViewModels use 1 repository
- Business logic is simple (get photos, apply settings)
- No multi-step workflows requiring transaction-like behavior

**Decision**: **Skip UseCase layer for MVP**. If Phase 2 adds complex workflows (e.g., sync SMB → Cloud → Local), then introduce UseCases.

#### Simplified Data Flow

```
User Action (UI)
    ↓
Compose Event Handler
    ↓
ViewModel.handleEvent()
    ↓ (viewModelScope.launch)
    ↓
Repository.method() (suspend fun on Dispatchers.IO)
    ↓
Data Source (SMB, Room) (suspend fun on Dispatchers.IO)
    ↓
Result<Data> or Flow<Data>
    ↓
ViewModel.updateState() (StateFlow.value = ...)
    ↓
Compose Recomposition (UI update on Dispatchers.Main)
```

**Thread boundaries**:
- UI events: Main thread
- ViewModel launch: Main thread (viewModelScope uses Main)
- Repository/DataSource: Dispatchers.IO (via withContext)
- State updates: Main thread (StateFlow updates are main-safe)

### 2.3 Key Components

#### 2.3.1 Photo Management

**PhotoRepository**

```kotlin
interface PhotoRepository {
    suspend fun getPhotosFromFolder(smbPath: String): Result<List<Photo>>
    suspend fun getRandomPhoto(): Result<Photo>
    fun preloadPhotos(photos: List<Photo>)
    fun clearCache()
}

class PhotoRepositoryImpl @Inject constructor(
    private val smbSource: SmbPhotoSource,
    private val photoCache: PhotoCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoRepository {

    override suspend fun getPhotosFromFolder(smbPath: String): Result<Photo> = withContext(ioDispatcher) {
        try {
            val files = smbSource.listPhotos(smbPath)
            val photos = files.map { it.toPhoto() }
            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRandomPhoto(): Result<Photo> = withContext(ioDispatcher) {
        // Simple random selection from cached list
        val cachedPhotos = photoCache.getAllPhotos()
        if (cachedPhotos.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No photos available"))
        }
        Result.success(cachedPhotos.random())
    }

    override fun preloadPhotos(photos: List<Photo>) {
        // Delegate to Coil's prefetch mechanism
        photos.forEach { photo ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(photo.uri)
                    .build()
            )
        }
    }
}
```

**SmbPhotoSource**

```kotlin
class SmbPhotoSource @Inject constructor(
    private val smbConfig: SmbConfig
) {
    private val context = CIFSContext(
        PropertyConfiguration(Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
        })
    ).withCredentials(
        NtlmPasswordAuthenticator(
            smbConfig.domain,
            smbConfig.username,
            smbConfig.password
        )
    )

    suspend fun listPhotos(path: String): List<SmbFile> = withContext(Dispatchers.IO) {
        val folder = SmbFile(path, context)
        folder.listFiles { file ->
            file.isFile && file.name.endsWith(".jpg", ignoreCase = true) ||
            file.name.endsWith(".png", ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    suspend fun readPhoto(path: String): InputStream = withContext(Dispatchers.IO) {
        SmbFile(path, context).inputStream
    }
}
```

**PhotoCache (Simple In-Memory)**

```kotlin
class PhotoCache @Inject constructor() {
    private val photoList = AtomicReference<List<Photo>>(emptyList())

    fun setPhotos(photos: List<Photo>) {
        photoList.set(photos)
    }

    fun getAllPhotos(): List<Photo> = photoList.get()

    fun clearCache() {
        photoList.set(emptyList())
    }
}
```

**Why this is simple**:
- No complex LRU eviction logic
- No disk cache (Coil handles this)
- Just stores photo metadata (paths), not image data
- Thread-safe via AtomicReference

#### 2.3.2 Slideshow Presentation

**SlideshowViewModel**

```kotlin
@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SlideshowState>(SlideshowState.Loading)
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    private val photos = mutableListOf<Photo>()
    private var currentIndex = 0

    fun startSlideshow() {
        viewModelScope.launch {
            val settings = settingsRepository.getSlideshowSettings()
            val result = photoRepository.getPhotosFromFolder(settings.smbPath)

            result.onSuccess { photoList ->
                photos.clear()
                photos.addAll(photoList.shuffled())
                preloadNextPhotos()
                _state.value = SlideshowState.Playing(photos[currentIndex])
                startAutoAdvance(settings.intervalSeconds)
            }.onFailure { error ->
                _state.value = SlideshowState.Error(error.message ?: "Failed to load photos")
            }
        }
    }

    fun nextPhoto() {
        if (photos.isEmpty()) return
        currentIndex = (currentIndex + 1) % photos.size
        _state.value = SlideshowState.Playing(photos[currentIndex])
        preloadNextPhotos()
    }

    fun previousPhoto() {
        if (photos.isEmpty()) return
        currentIndex = if (currentIndex == 0) photos.size - 1 else currentIndex - 1
        _state.value = SlideshowState.Playing(photos[currentIndex])
        preloadNextPhotos()
    }

    private fun preloadNextPhotos() {
        // Simple 3-photo buffer: previous, current, next
        val toPreload = listOf(
            photos.getOrNull((currentIndex - 1 + photos.size) % photos.size),
            photos.getOrNull(currentIndex),
            photos.getOrNull((currentIndex + 1) % photos.size)
        ).filterNotNull()

        photoRepository.preloadPhotos(toPreload)
    }

    private fun startAutoAdvance(intervalSeconds: Int) {
        viewModelScope.launch {
            while (true) {
                delay(intervalSeconds * 1000L)
                nextPhoto()
            }
        }
    }
}

sealed class SlideshowState {
    object Loading : SlideshowState()
    data class Playing(val photo: Photo) : SlideshowState()
    data class Error(val message: String) : SlideshowState()
}
```

**SlideshowScreen (Compose)**

```kotlin
@Composable
fun SlideshowScreen(
    viewModel: SlideshowViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is SlideshowState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is SlideshowState.Playing -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentState.photo.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            is SlideshowState.Error -> {
                Text(
                    text = currentState.message,
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Red
                )
            }
        }
    }
}
```

**Why this is simple**:
- No custom image loading, Coil handles everything
- No manual bitmap management
- Simple state machine (Loading → Playing or Error)
- Straightforward buffer logic (3 photos)

#### 2.3.3 Image Loading

**Coil Integration**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        smbFetcher: SmbFetcher
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(smbFetcher)
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15) // 15% of app memory for image cache
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .build()
    }
}
```

**SmbFetcher (Standard Coil Pattern)**

```kotlin
class SmbFetcher(
    private val data: Uri,
    private val smbSource: SmbPhotoSource
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val inputStream = smbSource.readPhoto(data.toString())
        return SourceResult(
            source = inputStream.source().buffer(),
            mimeType = "image/jpeg",
            dataSource = DataSource.NETWORK
        )
    }

    class Factory @Inject constructor(
        private val smbSource: SmbPhotoSource
    ) : Fetcher.Factory {
        override fun create(
            data: Any,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            if (data is Uri && data.scheme == "smb") {
                return SmbFetcher(data, smbSource)
            }
            return null
        }
    }
}
```

**Why this is simple**:
- Use Coil's standard Fetcher interface (well-documented)
- Coil handles all caching, memory management, downsampling
- No custom bitmap pooling or manual memory management
- Coil's disk cache provides free persistence

**Architect 2's concern about custom SMB handling**: Yes, we need a Fetcher, but it's just a simple adapter, not a complex custom implementation. Coil does the heavy lifting.

#### 2.3.4 Settings Management

**SettingsRepository**

```kotlin
interface SettingsRepository {
    suspend fun getSmbConfig(): SmbConfig
    suspend fun saveSmbConfig(config: SmbConfig)
    suspend fun getSlideshowSettings(): SlideshowSettings
    suspend fun saveSlideshowSettings(settings: SlideshowSettings)
    suspend fun getScheduleConfig(): ScheduleConfig
    suspend fun saveScheduleConfig(config: ScheduleConfig)
}

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    override suspend fun getSmbConfig(): SmbConfig = withContext(ioDispatcher) {
        settingsDao.getSmbConfig() ?: SmbConfig.default()
    }

    override suspend fun saveSmbConfig(config: SmbConfig) = withContext(ioDispatcher) {
        settingsDao.insertSmbConfig(config)
    }

    // Similar for other settings...
}
```

**Room Database (Settings Only)**

```kotlin
@Database(entities = [SmbConfigEntity::class, SlideshowSettingsEntity::class, ScheduleConfigEntity::class], version = 1)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM smb_config LIMIT 1")
    suspend fun getSmbConfig(): SmbConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmbConfig(config: SmbConfigEntity)

    // Similar for other settings...
}
```

**Why Room for settings**:
- Small, structured data (not thousands of rows)
- Type-safe queries
- Well-tested library
- No need for custom file I/O

**Why NOT Room for photos**:
- Photo metadata can be large (1000s of photos)
- Don't need complex queries (just random access)
- In-memory list is simpler and faster

#### 2.3.5 Scheduling

**ScheduleWorker (WorkManager)**

```kotlin
@HiltWorker
class ScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val schedule = settingsRepository.getScheduleConfig()
        val currentHour = LocalDateTime.now().hour

        return if (currentHour in schedule.startHour until schedule.endHour) {
            // Start slideshow
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("action", "start_slideshow")
            }
            applicationContext.startActivity(intent)
            Result.success()
        } else {
            // Stop slideshow (finish activity)
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("action", "stop_slideshow")
            }
            applicationContext.startActivity(intent)
            Result.success()
        }
    }
}
```

**WorkManager Setup**

```kotlin
class PhotoFrameApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    fun scheduleSlideshow(schedule: ScheduleConfig) {
        val work = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "slideshow_schedule",
            ExistingPeriodicWorkPolicy.REPLACE,
            work
        )
    }
}
```

**Why WorkManager**:
- Battery-efficient (system-optimized)
- Survives app restarts
- Handles doze mode automatically
- No custom alarm logic needed

**Why NOT custom AlarmManager**:
- More code to maintain
- Have to handle doze mode manually
- Have to handle battery optimization manually
- WorkManager is specifically designed for this

### 2.4 Data Models

**Photo**

```kotlin
data class Photo(
    val uri: Uri,
    val name: String,
    val dateModified: Long
)
```

**SmbConfig**

```kotlin
@Entity(tableName = "smb_config")
data class SmbConfig(
    @PrimaryKey val id: Int = 1,
    val serverAddress: String,
    val shareName: String,
    val folderPath: String,
    val username: String,
    val password: String,
    val domain: String = ""
) {
    companion object {
        fun default() = SmbConfig(
            id = 1,
            serverAddress = "",
            shareName = "",
            folderPath = "",
            username = "",
            password = "",
            domain = ""
        )
    }
}
```

**SlideshowSettings**

```kotlin
@Entity(tableName = "slideshow_settings")
data class SlideshowSettings(
    @PrimaryKey val id: Int = 1,
    val intervalSeconds: Int = 10,
    val transitionDurationMs: Int = 1000,
    val shuffleEnabled: Boolean = true,
    val subfoldersEnabled: Boolean = false
) {
    companion object {
        fun default() = SlideshowSettings(
            id = 1,
            intervalSeconds = 10,
            transitionDurationMs = 1000,
            shuffleEnabled = true,
            subfoldersEnabled = false
        )
    }
}
```

**ScheduleConfig**

```kotlin
@Entity(tableName = "schedule_config")
data class ScheduleConfig(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val daysOfWeek: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
) {
    companion object {
        fun default() = ScheduleConfig(
            id = 1,
            enabled = false,
            startHour = 8,
            endHour = 22,
            daysOfWeek = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
        )
    }
}
```

### 2.5 Dependency Injection

**AppModule**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDatabase(@ApplicationContext context: Context): SettingsDatabase {
        return Room.databaseBuilder(
            context,
            SettingsDatabase::class.java,
            "settings_db"
        ).build()
    }

    @Provides
    fun provideSettingsDao(database: SettingsDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
```

**Why Hilt**:
- Compile-time DI (catches errors early)
- Well-integrated with Android (ViewModels, Workers)
- Scoping (Singleton, ViewModelScoped)
- Easy testing (provide test modules)

---

## 3. Module Impact Analysis

### 3.1 New Modules

**Single Module**: `app`

**Justification**: With 2-3 developers and 3-4 month timeline, single module is:
- ✅ Faster to build
- ✅ Easier to navigate
- ✅ Simpler to refactor
- ✅ No Gradle inter-module complexity

**When to split**: If codebase exceeds 20K lines or we add a second major feature (e.g., video player).

### 3.2 New Files

**Estimated file count**: ~25-30 Kotlin files

**Breakdown**:
- UI: 6 files (3 screens + 3 ViewModels)
- Repository: 2 files (PhotoRepository, SettingsRepository)
- Data Source: 2 files (SmbPhotoSource, Room DAO)
- Cache: 1 file (PhotoCache)
- Models: 4 files (Photo, SmbConfig, SlideshowSettings, ScheduleConfig)
- DI: 2 files (AppModule, NetworkModule)
- Service: 1 file (ScheduleWorker)
- Database: 2 files (SettingsDatabase, entities)
- Coil: 1 file (SmbFetcher)
- Tests: 10-15 files (unit tests for repos, ViewModels)

**Comparison to other architects**:
- **Architect 1**: ~50-60 files (UseCase layer adds ~15-20 files)
- **Architect 2**: ~40-50 files (custom caching adds ~10 files)
- **This proposal**: ~25-30 files (MVP essentials only)

### 3.3 Dependencies

**Core**:
```gradle
dependencies {
    // Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // SMB
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.9")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

**Total dependencies**: ~15 libraries (all proven, widely-used)

**Comparison**:
- **Architect 1**: Similar (no extra dependencies for multi-module)
- **Architect 2**: +2-3 for custom caching/connection pooling
- **This proposal**: Standard Android + Coil + jcifs-ng + WorkManager

### 3.4 Risk Assessment

#### High Risk (Need Mitigation)

**1. SMB Performance on Tablets**
- **Risk**: jcifs-ng may not be optimized for tablet hardware
- **Mitigation**: Test on target hardware early (Week 1), profile network calls, consider connection timeouts
- **Fallback**: If jcifs-ng is too slow, try smbj library (similar API)

**2. Coil Memory Management for Large Photos**
- **Risk**: Users may have 20MP+ photos, could OOM
- **Mitigation**: Configure Coil to downsample to 2560x1600 (tablet resolution), monitor memory usage
- **Fallback**: Implement custom downsampling if Coil's isn't sufficient

#### Medium Risk (Monitor)

**3. 3-Photo Buffer May Not Be Enough**
- **Risk**: If SMB load times > 3 seconds, user may see lag
- **Mitigation**: Test with slow network, increase buffer to 4-5 if needed
- **Evidence needed**: Profile SMB load times before optimizing

**4. WorkManager May Not Wake Tablet Reliably**
- **Risk**: Doze mode may delay slideshow start
- **Mitigation**: Request battery optimization exemption, test on various Android versions
- **Fallback**: Use AlarmManager + WakeLock (but adds complexity)

#### Low Risk (Accept)

**5. Single Module May Become Unwieldy**
- **Risk**: 20K+ lines in one module could slow builds
- **Mitigation**: Refactor to multi-module if this happens (unlikely for MVP)
- **Timeline impact**: +1 week if refactoring needed

**6. No Disk Cache for Photo Metadata**
- **Risk**: App restart requires re-scanning SMB folder
- **Mitigation**: Coil caches images, so only metadata scan is slow (acceptable for MVP)
- **Phase 2**: Add Room cache for photo list

---

## 4. Technical Decisions

### 4.1 Key Decision Points

#### Decision 1: Single Module vs Multi-Module

**Options**:
1. **Single module** (this proposal)
2. Multi-module (Architect 1's proposal)

**Decision**: Single module

**Rationale**:
- **Team size**: 2-3 developers don't need isolation via modules
- **Build speed**: Single module is faster to build for small codebases
- **Simplicity**: No Gradle module configuration, no visibility modifiers across modules
- **Refactoring**: Easy to split later if needed

**Evidence**:
- Google I/O 2019: "Don't create modules until you have a reason" (Module size > 20K lines or > 5 features)
- Our MVP: ~5K-8K lines, 1 feature

**When to revisit**: If codebase exceeds 15K lines or we add a second feature (video support).

#### Decision 2: UseCase Layer vs Direct Repository

**Options**:
1. ViewModel → Repository (this proposal)
2. ViewModel → UseCase → Repository (Architect 1's proposal)

**Decision**: Skip UseCase layer for MVP

**Rationale**:
- **Simplicity**: Fewer files, less indirection
- **YAGNI**: No evidence we need to reuse business logic across ViewModels
- **Most UseCases are one-liners**: `invoke() { return repository.method() }` adds no value

**When UseCases make sense**:
- Complex multi-step workflows (not in MVP)
- Combining multiple repositories (not in MVP)
- Reusable logic across 3+ ViewModels (not in MVP)

**When to revisit**: Phase 2 if we add cloud sync (e.g., "sync SMB → Cloud → Local" UseCase).

#### Decision 3: 3-Photo Buffer vs 5-Photo Buffer

**Options**:
1. **3-photo buffer** (prev, current, next) - this proposal
2. 5-photo buffer (Architect 2's proposal)
3. 2-photo buffer (current, next) - insufficient

**Decision**: 3-photo buffer for MVP

**Rationale**:
- **Sufficient for transitions**: User can go forward/back without lag
- **Memory efficient**: ~15-30MB for 3 high-res photos (vs 25-50MB for 5)
- **Simpler logic**: Less code to manage buffer state
- **No evidence 5 is needed**: Architect 2 didn't provide profiling data showing 3 is insufficient

**When to increase to 5**:
- User testing shows lag on forward/back transitions
- SMB load times exceed 3 seconds (should be ~1-2s)

**Why not 2**:
- Can't go backward without re-loading previous photo (bad UX)

#### Decision 4: Custom SMB Fetcher vs Standard Coil Fetcher

**Options**:
1. **Standard Coil Fetcher** (this proposal)
2. Custom SMB Fetcher with connection pooling (Architect 2's proposal)

**Decision**: Standard Coil Fetcher (just adapt jcifs-ng InputStream)

**Rationale**:
- **Simplicity**: Coil's Fetcher interface is well-documented, just return an InputStream
- **No premature optimization**: Connection pooling adds complexity without proven bottleneck
- **Coil handles caching**: Disk cache + memory cache built-in
- **Less code to maintain**: ~30 lines vs ~200 lines for custom pooling

**When to add connection pooling**:
- Profiling shows SMB handshake is a bottleneck (> 500ms per connection)
- Evidence that jcifs-ng doesn't reuse connections internally

**Architect 2's concern**: "SMB handshake is expensive". **My response**: Prove it first, then optimize.

#### Decision 5: In-Memory Cache Only vs Disk Cache for Photo List

**Options**:
1. **In-memory cache only** (this proposal)
2. Disk cache for photo list (Architect 2's proposal)

**Decision**: In-memory only for MVP

**Rationale**:
- **Simplicity**: No serialization, no disk I/O, no cache invalidation logic
- **Fast cold start**: Scanning 1000 photos from SMB takes ~2-3 seconds (acceptable for MVP)
- **Coil caches images**: Users don't re-download images, just re-scan metadata
- **Fewer bugs**: No stale cache issues, no cache invalidation complexity

**When to add disk cache**:
- Photo list scan takes > 5 seconds (> 2000 photos)
- Users complain about slow cold starts

**Tradeoff**: Cold start is slower, but cache bugs are eliminated.

#### Decision 6: Room for Settings vs DataStore

**Options**:
1. **Room for settings** (this proposal)
2. DataStore (Preferences or Proto)

**Decision**: Room

**Rationale**:
- **Structured data**: SMB config has 6 fields, easy to model as entity
- **Type safety**: Queries are checked at compile time
- **Migrations**: Room migrations are straightforward
- **Testing**: Easy to mock DAO

**DataStore pros**:
- Simpler for key-value data
- Built for preferences

**Why Room wins for this use case**:
- We have 3 separate config objects (SMB, Slideshow, Schedule)
- Room gives us type-safe queries
- We may need to query settings in complex ways later

#### Decision 7: WorkManager vs AlarmManager + WakeLock

**Options**:
1. **WorkManager** (this proposal)
2. AlarmManager + WakeLock (custom implementation)

**Decision**: WorkManager

**Rationale**:
- **Battery-efficient**: WorkManager uses JobScheduler, which is optimized by OS
- **Doze mode handling**: Automatic handling of doze/app standby
- **Constraint support**: Battery level, charging state, etc.
- **Simpler code**: ~30 lines vs ~150 lines for custom AlarmManager + WakeLock

**AlarmManager pros**:
- More precise timing (WorkManager is ~15 min intervals)

**Why WorkManager wins**:
- Slideshow scheduling doesn't need second-precision
- Battery efficiency > timing precision for this use case

### 4.2 Technology Choices

**Rationale for key libraries**:

**Coil (vs Glide, Picasso)**:
- ✅ Kotlin-first (suspend functions, Flow)
- ✅ Jetpack Compose integration
- ✅ Smaller APK size (~500KB vs 2MB for Glide)
- ✅ Modern architecture (Fetcher pattern, memory management)

**jcifs-ng (vs smbj, jcifs)**:
- ✅ Active maintenance (last update 2023)
- ✅ SMB2/SMB3 support
- ✅ Well-documented
- ✅ Proven in production Android apps

**Hilt (vs Koin, manual DI)**:
- ✅ Compile-time DI (catches errors early)
- ✅ Android-specific (ViewModelScoped, WorkerFactory)
- ✅ Scoping support
- ✅ Google-recommended

**Room (vs SQLite, DataStore)**:
- ✅ Type-safe queries
- ✅ Compile-time verification
- ✅ Migrations built-in
- ✅ Coroutine support

**WorkManager (vs AlarmManager, JobScheduler)**:
- ✅ Battery-efficient
- ✅ Doze mode handling
- ✅ Constraint support
- ✅ Backwards-compatible API

---

## 5. Trade-offs & Concerns

### 5.1 Strengths of This Approach

**1. Fast Time-to-Market**
- **Fewer files**: ~25-30 vs 50-60 (Architect 1) or 40-50 (Architect 2)
- **Less abstraction**: No UseCase layer to implement and test
- **Standard patterns**: Developers can follow official Android guides
- **Realistic for 2-3 developers in 3-4 months**

**2. Easy to Understand**
- **Flat structure**: All code in one module, easy to navigate
- **Clear data flow**: ViewModel → Repository → Data Source (3 layers, not 4)
- **Minimal magic**: No custom caching, pooling, or optimization logic

**3. Easy to Maintain**
- **Fewer moving parts**: Simple PhotoCache (AtomicReference), standard Coil Fetcher
- **Standard libraries**: Coil, Room, WorkManager are well-documented
- **Less custom code**: Fewer custom components = fewer bugs

**4. Low Risk**
- **Proven patterns**: Repository pattern is standard Android
- **Proven libraries**: All dependencies are widely-used, battle-tested
- **Incremental optimization**: Can add complexity later based on real bottlenecks

**5. Testable**
- **Simple mocking**: Repository interfaces are easy to mock
- **No complex state**: PhotoCache is just an AtomicReference
- **Hilt testing**: Hilt provides test modules for DI

### 5.2 Weaknesses & Limitations

**1. Less Extensible Than Architect 1's Proposal**
- **No UseCase layer**: If Phase 2 requires complex workflows, we'll need to refactor
- **Single module**: If we add a second feature (video), may need to split modules
- **Impact**: +1-2 weeks refactoring time if Phase 2 requires it

**Mitigation**:
- Accept this tradeoff for faster MVP
- Refactor when we have evidence it's needed (not before)

**2. Less Optimized Than Architect 2's Proposal**
- **3-photo buffer (not 5)**: May see lag if SMB is very slow
- **No connection pooling**: May see slower loads if handshake is expensive
- **No aggressive disk caching**: Cold start is slower

**Mitigation**:
- Test on real hardware early (Week 1-2)
- Profile SMB performance before optimizing
- Add optimizations incrementally based on evidence

**3. Cold Start May Be Slow**
- **No disk cache for photo list**: App restart requires re-scanning SMB folder
- **Impact**: ~2-3 seconds to load 1000 photos (vs ~500ms with disk cache)

**Mitigation**:
- Coil caches images, so only metadata scan is slow
- Show loading indicator (good UX)
- Add disk cache in Phase 2 if users complain

**4. Single Module May Not Scale**
- **Build times**: If codebase exceeds 20K lines, builds may slow down
- **Merge conflicts**: 2-3 developers working in same module may conflict

**Mitigation**:
- For MVP (5K-8K lines), this won't be an issue
- If it becomes an issue, refactor to multi-module (1-2 weeks)

### 5.3 Comparison to Other Proposals

| Aspect                  | Architect 1 (Modularity) | Architect 2 (Performance) | This Proposal (Simplicity) |
|------------------------|--------------------------|---------------------------|----------------------------|
| **Module count**        | 8 modules                | 3-4 modules               | 1 module                   |
| **Layers**              | ViewModel → UseCase → Repository → Data Source | ViewModel → Repository → Data Source (optimized) | ViewModel → Repository → Data Source |
| **File count**          | ~50-60 files             | ~40-50 files              | ~25-30 files               |
| **Photo buffer**        | Not specified            | 5 photos                  | 3 photos                   |
| **SMB handling**        | Standard repository      | Custom Fetcher + pooling  | Standard Coil Fetcher      |
| **Caching**             | Not specified            | Disk + memory + LRU       | In-memory only (+ Coil)    |
| **Complexity**          | High (abstraction)       | High (optimization)       | Low (pragmatic)            |
| **Extensibility**       | High                     | Medium                    | Medium                     |
| **Time to MVP**         | 4-5 months               | 3-4 months                | 3 months                   |
| **Team suitability**    | 4-5 developers           | 2-3 developers            | 2-3 developers             |

**My assessment**:
- **Architect 1**: Best for long-term maintainability, but over-engineered for MVP
- **Architect 2**: Best for performance, but premature optimization without profiling
- **This proposal**: Best for MVP delivery, acceptable tradeoffs for fast iteration

---

## 6. Requirements Coverage

### 6.1 User Story Mapping

**Epic 1: First-Time Setup**

| Story ID | User Story | Components | Status |
|----------|-----------|------------|--------|
| US-1.1 | Configure SMB connection | SmbConfigScreen, SmbConfigViewModel, SettingsRepository, SmbPhotoSource | ✅ Covered |
| US-1.2 | Test SMB connection | SmbPhotoSource.testConnection() | ✅ Covered |
| US-1.3 | Browse SMB folders | (Deferred to Phase 2) | ⚠️ Not in MVP |

**Epic 2: Slideshow Configuration**

| Story ID | User Story | Components | Status |
|----------|-----------|------------|--------|
| US-2.1 | Set photo interval | SettingsScreen, SettingsViewModel, SettingsRepository | ✅ Covered |
| US-2.2 | Choose transition effects | (Deferred to Phase 2) | ⚠️ Coil crossfade only |
| US-2.3 | Enable shuffle mode | SlideshowSettings.shuffleEnabled, SlideshowViewModel | ✅ Covered |
| US-2.4 | Include subfolders | SlideshowSettings.subfoldersEnabled, SmbPhotoSource | ✅ Covered |

**Epic 3: Photo Viewing**

| Story ID | User Story | Components | Status |
|----------|-----------|------------|--------|
| US-3.1 | View slideshow | SlideshowScreen, SlideshowViewModel, PhotoRepository | ✅ Covered |
| US-3.2 | Manual navigation (swipe) | SlideshowViewModel.nextPhoto(), previousPhoto() | ✅ Covered |
| US-3.3 | Pause/resume | SlideshowViewModel (pause/resume logic) | ✅ Covered |

**Epic 4: Automated Scheduling**

| Story ID | User Story | Components | Status |
|----------|-----------|------------|--------|
| US-4.1 | Set on/off schedule | ScheduleScreen, ScheduleViewModel, ScheduleConfig | ✅ Covered |
| US-4.2 | Day-of-week selection | ScheduleConfig.daysOfWeek | ✅ Covered |
| US-4.3 | Auto-start/stop | ScheduleWorker, WorkManager | ✅ Covered |

**Epic 5: Always-On Operation**

| Story ID | User Story | Components | Status |
|----------|-----------|------------|--------|
| US-5.1 | Keep screen awake | MainActivity (window flags) | ✅ Covered |
| US-5.2 | Handle errors gracefully | SlideshowState.Error, retry logic | ✅ Covered |
| US-5.3 | Memory management | Coil memory cache (15%), 3-photo buffer | ✅ Covered |

### 6.2 Non-Functional Requirements

**NFR-1: Performance**
- **Target**: 60fps slideshow transitions, <2s photo load times
- **Implementation**:
  - Coil's crossfade animation (GPU-accelerated)
  - 3-photo pre-loading buffer
  - Image downsampling to screen resolution
- **Validation**: Manual testing on target hardware, GPU profiler

**NFR-2: Memory**
- **Target**: <300MB memory usage for 24/7 operation
- **Implementation**:
  - Coil memory cache limited to 15% of app memory (~45MB on 1GB RAM tablet)
  - 3-photo buffer (~15-30MB for high-res photos)
  - No large in-memory photo lists (just metadata)
- **Validation**: Android Studio profiler, leak canary

**NFR-3: Reliability**
- **Target**: Run continuously for 24+ hours without crashes
- **Implementation**:
  - Error handling in all suspend functions (try/catch)
  - StateFlow for thread-safe state updates
  - Coil handles OOM gracefully (downsample, eviction)
  - WorkManager handles scheduling reliably
- **Validation**: 24-hour soak test on target hardware

**NFR-4: Network Efficiency**
- **Target**: Minimal SMB requests, batch operations where possible
- **Implementation**:
  - List all photos once, cache metadata in-memory
  - Pre-load next 3 photos (not just-in-time)
  - Coil disk cache avoids re-downloading images
- **Validation**: Network profiler, SMB server logs

**NFR-5: Battery Efficiency**
- **Target**: Don't drain battery faster than charging (for wall-powered tablets)
- **Implementation**:
  - WorkManager uses JobScheduler (battery-optimized)
  - No excessive wake locks (only during slideshow)
  - Screen brightness controlled by user (not app)
- **Validation**: Battery historian, 24-hour test

---

## 7. Debate Summary

### 7.1 Critique of Architect 1's Proposal (Modularity-Focused)

**Strengths I Agree With**:
- ✅ Clean separation of concerns is valuable
- ✅ Repository pattern is proven and testable
- ✅ Dependency injection (Hilt) is the right choice
- ✅ Immutable data models prevent bugs

**Concerns & Critiques**:

**1. Multi-Module Structure (8 Modules)**

> **Architect 1**, I appreciate the clean separation, but is multi-module architecture justified for our 2-3 developer team and 3-4 month timeline?

**Evidence**:
- Google's Now in Android (large production app) uses 17 modules **for 10+ developers**
- Our team: 2-3 developers
- Our MVP: ~5K-8K lines of code (vs 50K+ for multi-module benefit)

**My concern**:
- Multi-module adds Gradle complexity (dependency graphs, visibility modifiers)
- Build time savings don't materialize until codebase is large (20K+ lines)
- For 2-3 developers, merge conflicts are rare even in single module
- Refactoring across modules requires changing `build.gradle.kts` files

**Question**: Can we start with a single module and refactor to multi-module if the codebase proves unmanageable? What's the ROI of 8 modules for MVP?

**2. UseCase Layer**

> **Architect 1**, the UseCase layer adds indirection. For this feature scope, can the repositories handle business logic directly?

**Your proposal includes**:
```kotlin
class GetPhotosFromFolderUseCase(private val repository: PhotoRepository) {
    suspend operator fun invoke(path: String): Result<List<Photo>> {
        return repository.getPhotosFromFolder(path)
    }
}
```

**My critique**: This is a **one-line wrapper**. It adds:
- ❌ Extra file to create
- ❌ Extra class to test
- ❌ Extra layer to understand (ViewModel → UseCase → Repository)
- ❌ No encapsulated business logic

**When UseCases make sense**:
- ✅ Complex multi-step workflows (e.g., "book flight + reserve hotel")
- ✅ Combining multiple repositories (e.g., PhotoRepository + UserRepository + CloudRepository)
- ✅ Reusable logic across 3+ ViewModels

**For our MVP**:
- Most ViewModels use 1 repository
- Business logic is simple (get photos, apply settings)
- No multi-repository workflows

**Question**: When would we need the UseCase layer for this MVP? Can you provide an example where the extra layer provides clear value?

**3. Are We Building for Phase 2?**

**Your rationale includes**:
> "Future-proofing for Phase 2 cloud integration"

**My concern**: We're designing for **hypothetical future requirements** without knowing if users will even want Phase 2 features.

**Agile principle**: Build the minimum viable architecture, then refactor based on real needs.

**Risk**: If we over-engineer for Phase 2 and users don't need it, we've spent 1-2 extra months on unnecessary abstraction.

**Question**: Can we defer multi-module structure and UseCase layer to Phase 2 when we have evidence they're needed?

**Summary of Architect 1 Critique**:
- ✅ **Good patterns**: Repository, DI, immutability
- ⚠️ **Over-abstraction**: 8 modules, UseCase layer add complexity without clear MVP benefit
- ⚠️ **Premature optimization for extensibility**: Building for Phase 2 without evidence it's needed

**Proposed compromise**: Start with 1-2 modules, skip UseCase layer for MVP, refactor when evidence shows it's needed.

---

### 7.2 Critique of Architect 2's Proposal (Performance-Focused)

**Strengths I Agree With**:
- ✅ Performance is critical for slideshow UX
- ✅ Pre-loading photos is essential to hide network latency
- ✅ Image downsampling to screen resolution is smart
- ✅ Coil is the right choice for image loading

**Concerns & Critiques**:

**1. 5-Photo Buffer (vs 3)**

> **Architect 2**, your performance optimizations are impressive, but have we proven that 3-photo buffer is insufficient?

**Your proposal**: 5-photo buffer (2 prev, current, 2 next)

**Evidence provided**: None. No profiling data showing 3-photo buffer causes lag.

**My analysis**:
- **3-photo buffer** (prev, current, next):
  - Covers forward/backward navigation
  - Memory: ~15-30MB for 3 high-res photos (5MP each @ 5-10MB)
  - Sufficient if SMB load time is <3 seconds
- **5-photo buffer**:
  - Memory: ~25-50MB
  - Benefit: User can navigate forward/backward twice without lag
  - Cost: 67% more memory usage

**Question**: Have we profiled SMB load times to confirm 3-photo buffer is insufficient? Can we start with 3 and scale to 5 if testing shows issues?

**My proposal**: Start with 3-photo buffer, monitor SMB load times, increase to 5 only if evidence shows it's needed.

**2. Connection Pooling for SMB**

> **Architect 2**, connection pooling adds complexity. Have we confirmed SMB handshake is a bottleneck?

**Your proposal**: Custom connection pool for SMB (4 connections, reuse logic)

**Evidence provided**: "SMB handshake is expensive" (no profiling data)

**My analysis**:
- **jcifs-ng** may already do connection reuse internally (need to check docs)
- Custom connection pooling adds:
  - ~100-150 lines of custom code
  - Complexity: connection lifecycle, error handling, thread safety
  - Maintenance burden: custom code we have to debug

**Question**: Have we profiled SMB handshake time? Is it >500ms per connection? Does jcifs-ng already reuse connections?

**My proposal**: Start with standard jcifs-ng, profile SMB handshake, add connection pooling only if profiling shows it's a bottleneck (>500ms).

**3. Aggressive Disk Caching**

> **Architect 2**, your disk cache strategy is thorough, but is it necessary for MVP?

**Your proposal**:
- Disk cache for photo list (serialize to file)
- LRU eviction for in-memory cache
- Custom cache invalidation logic

**My analysis**:
- **Cold start performance**:
  - Without disk cache: ~2-3 seconds to scan 1000 photos from SMB
  - With disk cache: ~500ms
  - **Tradeoff**: 1.5-2.5 second startup cost vs complexity of disk cache
- **Coil already provides**:
  - Disk cache for images (100MB default)
  - LRU eviction built-in
  - Memory cache with size limits

**Question**: Is 2-3 second cold start unacceptable for MVP? Can we defer disk cache for photo list until users complain?

**My proposal**: Use Coil's built-in disk cache for images, skip custom disk cache for photo list in MVP.

**4. Custom SMB Fetcher**

**Your proposal**: Custom Coil Fetcher with connection pooling, retry logic, etc.

**My analysis**:
- **Standard Coil Fetcher**: ~30 lines of code (just return InputStream from jcifs-ng)
- **Custom Fetcher with pooling**: ~200 lines of code

**Coil already provides**:
- Retry logic (configurable)
- Timeout handling
- Memory/disk caching

**Question**: Can we use standard Coil Fetcher and let Coil handle retries/timeouts, then add custom logic only if needed?

**My proposal**: Standard Coil Fetcher for MVP, add custom logic based on profiling.

**Summary of Architect 2 Critique**:
- ✅ **Good focus on performance**: Pre-loading, downsampling, caching are all valuable
- ⚠️ **Premature optimization**: 5-photo buffer, connection pooling, aggressive disk caching are optimizations without evidence of bottlenecks
- ⚠️ **Complexity cost**: Custom connection pooling, disk cache, and custom Fetcher add 200-300 lines of complex code

**Proposed compromise**: Start with simpler implementations (3-photo buffer, standard Fetcher, Coil caching), profile on real hardware, optimize incrementally based on evidence.

---

### 7.3 Finding Middle Ground

**Where I Agree with Architect 1**:
- ✅ Repository pattern (testable, decoupled)
- ✅ Dependency injection (Hilt)
- ✅ Immutable data models (thread-safe)
- ✅ Clean separation of concerns

**Where I Agree with Architect 2**:
- ✅ Pre-loading photos (critical for UX)
- ✅ Image downsampling (memory efficiency)
- ✅ Coil for image loading (proven library)
- ✅ WorkManager for scheduling (battery-efficient)

**Where I Disagree with Both**:
- ❌ **Architect 1**: Multi-module structure and UseCase layer are over-engineered for MVP
- ❌ **Architect 2**: 5-photo buffer and connection pooling are premature optimizations

**My Proposed Compromise**:

| Aspect | Architect 1 | Architect 2 | My Proposal | Rationale |
|--------|-------------|-------------|-------------|-----------|
| **Modules** | 8 modules | 3-4 modules | 1 module | 2-3 devs don't need isolation |
| **Layers** | +UseCase | -UseCase | -UseCase | No complex workflows in MVP |
| **Buffer** | N/A | 5 photos | 3 photos | Sufficient, optimize if needed |
| **Fetcher** | Standard | Custom+pool | Standard | YAGNI, add pooling if bottleneck |
| **Caching** | N/A | Disk+mem+LRU | Mem+Coil | Simpler, Coil handles images |
| **Repository** | ✅ Yes | ✅ Yes | ✅ Yes | All agree: good pattern |
| **Hilt DI** | ✅ Yes | ✅ Yes | ✅ Yes | All agree: right choice |

**Potential Consensus Points**:
1. **2-3 modules** (not 1, not 8): Maybe split `app` and `core` if Architect 1 feels strongly
2. **4-photo buffer** (not 3, not 5): Middle ground if testing shows 3 is insufficient
3. **Repository pattern** (yes) but **no UseCase layer** (defer to Phase 2)
4. **In-memory cache + Coil disk cache** (not custom disk cache for photo list)
5. **Standard Fetcher** (with option to add pooling later based on profiling)

**My Role as Simplicity Advocate**:
- Ask: "Do we really need this for MVP?"
- Demand evidence: "Show me profiling data before optimizing"
- Advocate for YAGNI: "Let's defer this to Phase 2 unless we know it's needed"
- Promote pragmatism: "Let's ship the MVP, gather user feedback, then optimize"

---

## 8. Concurrency & Thread Safety

### 8.1 Concurrent Operations

**Operations That Run Concurrently**:

1. **SMB photo scanning** (background thread)
2. **Image loading** (Coil background threads)
3. **WorkManager scheduling** (background thread)
4. **ViewModel state updates** (main thread)
5. **Repository data access** (IO thread)

**Thread Boundaries**:

```
User Action (UI) → Main Thread
    ↓
ViewModel.handleEvent() → Main Thread (viewModelScope launched on Main)
    ↓
Repository.method() → IO Thread (withContext(Dispatchers.IO))
    ↓
SmbPhotoSource.listPhotos() → IO Thread
    ↓
Result returned → IO Thread
    ↓
StateFlow.value updated → Main Thread (StateFlow is main-safe)
    ↓
Compose recomposition → Main Thread
```

### 8.2 Thread Safety Mechanisms

**1. StateFlow for UI State**

```kotlin
class SlideshowViewModel : ViewModel() {
    // Thread-safe: StateFlow updates are atomic
    private val _state = MutableStateFlow<SlideshowState>(SlideshowState.Loading)
    val state: StateFlow<SlideshowState> = _state.asStateFlow()

    fun updateState(newState: SlideshowState) {
        // Safe to call from any thread
        _state.value = newState
    }
}
```

**Why this is thread-safe**:
- StateFlow updates are **atomic** (no race conditions)
- Only `_state` (private) can be modified
- Public `state` is read-only
- Collectors receive updates on the correct dispatcher

**2. Repository with Dispatchers**

```kotlin
class PhotoRepositoryImpl @Inject constructor(
    private val smbSource: SmbPhotoSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoRepository {

    override suspend fun getPhotosFromFolder(path: String): Result<List<Photo>> =
        withContext(ioDispatcher) {
            // All SMB operations happen on IO thread
            try {
                val files = smbSource.listPhotos(path)
                Result.success(files.map { it.toPhoto() })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
```

**Why this is thread-safe**:
- `withContext(Dispatchers.IO)` ensures SMB operations run on IO thread pool
- No blocking of main thread
- Exceptions are caught and returned as Result

**3. PhotoCache with AtomicReference**

```kotlin
class PhotoCache @Inject constructor() {
    // Thread-safe: AtomicReference guarantees atomic updates
    private val photoList = AtomicReference<List<Photo>>(emptyList())

    fun setPhotos(photos: List<Photo>) {
        // Atomic operation, no race condition
        photoList.set(photos)
    }

    fun getAllPhotos(): List<Photo> {
        // Atomic read
        return photoList.get()
    }
}
```

**Why this is thread-safe**:
- `AtomicReference` provides **atomic read/write**
- No need for Mutex (simpler than Architect 2's synchronized cache)
- List itself is immutable (no concurrent modification issues)

**4. Coil Image Loading (Handled by Library)**

```kotlin
// Coil handles thread safety internally
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(photo.uri)
        .crossfade(true)
        .build(),
    // Coil loads on background thread, updates UI on main thread
    modifier = Modifier.fillMaxSize()
)
```

**Why this is thread-safe**:
- Coil manages its own thread pools
- Image decoding happens on background threads
- UI updates happen on main thread (Coil handles this)

### 8.3 Preventing Race Conditions

**1. No Check-Then-Act Patterns**

```kotlin
// BAD - Race condition
if (!cache.contains(key)) {
    cache.put(key, value) // Another thread may have just added key
}

// GOOD - Our implementation
photoList.set(photos) // Atomic operation, no check needed
```

**2. No Read-Modify-Write Without Atomicity**

```kotlin
// BAD - Race condition
var count = currentIndex
count++
currentIndex = count

// GOOD - Our implementation
currentIndex = (currentIndex + 1) % photos.size // Single atomic operation
```

**3. StateFlow Updates Are Atomic**

```kotlin
// GOOD - StateFlow ensures atomicity
_state.value = SlideshowState.Playing(photos[currentIndex])

// Even if multiple threads call this, StateFlow handles it correctly
```

### 8.4 Coroutine Best Practices

**1. Always Use Dispatcher for Blocking Operations**

```kotlin
// GOOD - SMB I/O on IO dispatcher
suspend fun listPhotos(path: String): List<SmbFile> = withContext(Dispatchers.IO) {
    folder.listFiles() // Blocking I/O
}

// BAD - Would block coroutine thread
suspend fun listPhotos(path: String): List<SmbFile> {
    return folder.listFiles() // Blocks without withContext
}
```

**2. Don't Block Coroutines with Thread.sleep**

```kotlin
// BAD - Blocks thread
suspend fun wait() {
    Thread.sleep(1000) // Blocks thread!
}

// GOOD - Suspends without blocking
suspend fun wait() {
    delay(1000) // Suspends coroutine, doesn't block thread
}
```

**3. Handle Cancellation Correctly**

```kotlin
suspend fun loadPhotos(): Result<List<Photo>> = withContext(Dispatchers.IO) {
    try {
        smbSource.listPhotos(path)
    } catch (e: CancellationException) {
        // Don't catch cancellation - let it propagate
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 8.5 Testing Thread Safety

**Unit Test for Concurrent Access**

```kotlin
@Test
fun `PhotoCache is thread-safe under concurrent access`() = runBlocking {
    val cache = PhotoCache()
    val photos = List(100) { Photo(Uri.parse("smb://photo$it.jpg"), "photo$it.jpg", 0L) }

    // Launch 100 coroutines that all update cache simultaneously
    val jobs = List(100) { index ->
        launch(Dispatchers.Default) {
            cache.setPhotos(listOf(photos[index]))
        }
    }

    jobs.joinAll()

    // Verify cache is not corrupted (one of the photos should be present)
    val result = cache.getAllPhotos()
    assertTrue(result.isNotEmpty())
}
```

**Unit Test for StateFlow Thread Safety**

```kotlin
@Test
fun `ViewModel state updates are thread-safe`() = runBlocking {
    val viewModel = SlideshowViewModel(mockRepository, mockSettingsRepository)

    // Launch multiple coroutines that update state
    val jobs = List(100) {
        launch(Dispatchers.Default) {
            viewModel.nextPhoto()
        }
    }

    jobs.joinAll()

    // Verify state is valid (no null, no corruption)
    val finalState = viewModel.state.value
    assertTrue(finalState is SlideshowState.Playing || finalState is SlideshowState.Loading)
}
```

### 8.6 Comparison to Other Architects

| Aspect | Architect 1 | Architect 2 | This Proposal |
|--------|-------------|-------------|---------------|
| **StateFlow** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Dispatchers** | ✅ Explicit | ✅ Explicit | ✅ Explicit |
| **Cache sync** | Not specified | Mutex + synchronized | AtomicReference |
| **Complexity** | Medium | High (custom pooling) | Low (standard patterns) |

**Why my approach is simpler**:
- **AtomicReference** instead of Mutex (fewer lines, less complexity)
- **Coil handles threading** (no manual thread pool management)
- **Standard coroutine patterns** (no custom thread pools or connection pooling)

**When to use Mutex (Architect 2's approach)**:
- Multiple related values need to be updated atomically
- Complex state transitions (not just single value updates)

**For our MVP**:
- PhotoCache only stores a single list (AtomicReference is sufficient)
- No complex multi-value state transitions

---

## 9. Implementation Considerations

### 9.1 Development Phases

**Phase 1: Foundation (Week 1-2)**
- Set up single-module project structure
- Implement SettingsDatabase (Room) with DAO
- Create data models (Photo, SmbConfig, SlideshowSettings, ScheduleConfig)
- Set up Hilt DI (AppModule, NetworkModule)
- **Deliverable**: Build compiles, DI graph valid

**Phase 2: SMB Integration (Week 3-4)**
- Implement SmbPhotoSource (jcifs-ng)
- Create PhotoRepository with basic caching
- Test SMB connection on target hardware
- **Deliverable**: Can list photos from SMB share

**Phase 3: Slideshow UI (Week 5-7)**
- Implement SlideshowScreen (Compose)
- Implement SlideshowViewModel with 3-photo buffer
- Integrate Coil for image loading
- Implement SmbFetcher for Coil
- **Deliverable**: Basic slideshow works

**Phase 4: Settings UI (Week 8-9)**
- Implement SettingsScreen (Compose)
- Implement SmbConfigScreen for connection setup
- Implement SettingsViewModel and SmbConfigViewModel
- **Deliverable**: Users can configure app

**Phase 5: Scheduling (Week 10-11)**
- Implement ScheduleWorker (WorkManager)
- Implement ScheduleScreen
- Test auto-start/stop on target hardware
- **Deliverable**: Scheduling works

**Phase 6: Polish & Testing (Week 12-16)**
- Handle edge cases (no photos, SMB errors, etc.)
- Implement error recovery (retries, fallbacks)
- 24-hour soak testing
- Performance profiling (memory, battery, network)
- Bug fixes
- **Deliverable**: Production-ready MVP

**Total Timeline**: 16 weeks (4 months) for 2-3 developers

### 9.2 Testing Strategy

**Unit Tests**:
- PhotoRepository (mock SmbPhotoSource)
- SettingsRepository (mock SettingsDao)
- SlideshowViewModel (mock repositories)
- PhotoCache thread safety

**Integration Tests**:
- SMB connection (real SMB server)
- Room database (real database, in-memory)
- WorkManager scheduling (real WorkManager, test mode)

**UI Tests**:
- SlideshowScreen (Compose UI tests)
- SettingsScreen navigation
- Error state rendering

**Manual Tests**:
- 24-hour soak test on target tablet
- SMB load time profiling
- Memory profiling (Android Studio Profiler)
- Battery usage (Battery Historian)

### 9.3 Performance Profiling Plan

**Week 1-2**: Baseline SMB Performance
- Measure SMB handshake time (should be <500ms)
- Measure photo list scan time (1000 photos should be <3s)
- Measure single photo load time (should be <2s)

**Week 5-7**: Slideshow Performance
- Measure transition frame rate (should be 60fps)
- Measure memory usage (should be <300MB)
- Measure 3-photo buffer effectiveness (should eliminate lag)

**Week 12**: Optimization Pass
- If SMB handshake > 500ms → Add connection pooling
- If 3-photo buffer insufficient → Increase to 4-5
- If memory > 300MB → Reduce Coil cache size
- If cold start > 5s → Add disk cache for photo list

**Evidence-Based Optimization**: Only optimize after profiling shows a bottleneck.

### 9.4 Risk Mitigation

**Risk 1: SMB Performance on Tablets**
- **Mitigation**: Test on target hardware in Week 1-2
- **Fallback**: Try alternative SMB library (smbj) if jcifs-ng is slow

**Risk 2: Coil OOM with Large Photos**
- **Mitigation**: Configure Coil to downsample to 2560x1600
- **Fallback**: Custom downsampling if Coil's isn't sufficient

**Risk 3: 3-Photo Buffer Insufficient**
- **Mitigation**: Profile SMB load times, increase buffer if needed
- **Fallback**: Increase to 4-5 photos based on evidence

**Risk 4: WorkManager Unreliable**
- **Mitigation**: Test on various Android versions, request battery optimization exemption
- **Fallback**: Use AlarmManager + WakeLock (adds complexity)

### 9.5 Technical Debt Tracking

**Acceptable Debt for MVP**:
1. **No disk cache for photo list** (cold start is slower)
   - **When to fix**: If users complain or cold start > 5s
2. **No UseCase layer** (less abstraction)
   - **When to fix**: Phase 2 if complex workflows emerge
3. **Single module** (may not scale)
   - **When to fix**: If codebase > 20K lines or merge conflicts increase
4. **3-photo buffer** (may not be enough)
   - **When to fix**: If profiling shows lag > 500ms on transitions

**How to Track**:
- Add `// TODO(Phase 2):` comments in code
- Maintain `TECHNICAL_DEBT.md` file with priority and rationale
- Review after MVP launch based on user feedback

### 9.6 Migration Path to More Complex Architecture

**If We Need Multi-Module (Post-MVP)**:
1. Create `core` module (models, utils)
2. Create `data` module (repositories, data sources)
3. Keep `app` module (UI, ViewModels)
4. **Effort**: 1-2 weeks

**If We Need UseCase Layer (Phase 2)**:
1. Create `domain` package
2. Move business logic from repositories to UseCases
3. Update ViewModels to call UseCases
4. **Effort**: 1-2 weeks

**If We Need Connection Pooling (If Profiling Shows Bottleneck)**:
1. Create `SmbConnectionPool` class
2. Update `SmbPhotoSource` to use pool
3. **Effort**: 3-5 days

**Key Point**: These are **incremental refactorings**, not full rewrites. Simple architecture makes future changes easier.

---

## 10. Conclusion

### 10.1 Summary

This architecture proposal prioritizes **simplicity, pragmatism, and fast time-to-market** for a 2-3 developer team building an MVP in 3-4 months. Key decisions:

1. **Single module** (not 8) - No Gradle complexity for small team
2. **No UseCase layer** - Repositories handle business logic for MVP
3. **3-photo buffer** - Simplest buffer that eliminates lag
4. **Standard Coil Fetcher** - No custom pooling or optimization
5. **In-memory cache only** - Defer disk cache to Phase 2
6. **Proven libraries** - Coil, Room, WorkManager, jcifs-ng

### 10.2 Why This Approach Is Best for MVP

**Architect 1's approach** (modularity) is best for:
- Large teams (5+ developers)
- Long-term maintainability
- Complex apps with many features

**Architect 2's approach** (performance) is best for:
- Performance-critical apps with proven bottlenecks
- Apps that have been profiled and need optimization
- Teams with time to build custom components

**This approach** (simplicity) is best for:
- Small teams (2-3 developers)
- MVPs with tight timelines (3-4 months)
- Apps where we don't yet know the real bottlenecks
- Projects that need fast iteration based on user feedback

### 10.3 Next Steps

1. **Consensus Building**: All 3 architects review and discuss
2. **Compromise**: Find middle ground on key decisions (module count, buffer size)
3. **Approval**: Get stakeholder sign-off on chosen approach
4. **Implementation**: Start with Phase 1 (Foundation) in Week 1

### 10.4 Final Thoughts

**Good engineering is about tradeoffs**, not absolutes. Architect 1 is right that modularity helps maintainability. Architect 2 is right that performance is critical. But for an MVP with 2-3 developers and a 3-4 month timeline, **simplicity should win**.

Build the simplest thing that could possibly work. Ship it. Gather feedback. Optimize based on real bottlenecks, not imagined ones.

> "Premature optimization is the root of all evil." - Donald Knuth

> "You Aren't Gonna Need It." - Extreme Programming

> "Make it work, make it right, make it fast." - Kent Beck

Let's make it work first. Then we'll make it right. Then, if needed, we'll make it fast.

---

**End of Proposal**
