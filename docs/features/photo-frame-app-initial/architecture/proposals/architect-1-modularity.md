# Architecture Proposal - Architect 1 (Modularity-Focused)

**Author**: Architect 1 - Modularity-focused
**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Date**: 2026-03-01
**PRD Reference**: `docs/features/photo-frame-app-initial/requirements/PRD_DRAFT.md`

---

## 1. Overview

### Approach Summary

This architecture proposal prioritizes **clean separation of concerns, reusability, maintainability, and testability**. The design follows Clean Architecture principles with clear boundaries between layers, making the system extensible for future phases (cloud services, advanced features) while remaining testable and maintainable.

### Key Architectural Decisions

1. **Multi-module Android project structure** - Physical separation by feature and layer
2. **Repository pattern with clear abstractions** - Decouples data sources from business logic
3. **UseCase/Interactor layer** - Encapsulates business logic for reusability and testability
4. **Dependency Injection with Hilt** - Facilitates testing and loose coupling
5. **Coroutines + Flow for async operations** - Thread-safe, cancellable, composable
6. **Immutable data models** - Prevents accidental mutations and thread safety issues
7. **Interface-based component contracts** - Enables mockability and future implementations

### Focus Area Priorities

As the modularity-focused architect, my priorities are:
- **Clean separation of concerns**: Each component has a single, well-defined responsibility
- **High cohesion, low coupling**: Related functionality grouped together, minimal dependencies between modules
- **Reusability**: Components designed for potential reuse in Phase 2 (cloud integration)
- **Testability**: All business logic testable in isolation without Android dependencies
- **Clear interfaces**: Well-defined contracts between layers
- **Future extensibility**: Architecture supports Phase 2 cloud services without major refactoring

---

## 2. Architecture Approach

### Module Structure

The application is organized into 8 Gradle modules following feature-based and layer-based separation:

```
photo-frame-android/
├── app/                                    # Main application module
│   ├── src/main/
│   │   ├── kotlin/com/photoframe/
│   │   │   ├── PhotoFrameApplication.kt   # Application class, DI setup
│   │   │   ├── di/                        # App-level DI modules
│   │   │   └── MainActivity.kt            # Single activity host
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── core-common/                            # Shared utilities, no Android deps
│   ├── src/main/kotlin/com/photoframe/core/common/
│   │   ├── result/                        # Result<T> wrapper for error handling
│   │   ├── dispatchers/                   # CoroutineDispatcher abstraction
│   │   └── utils/                         # Pure Kotlin utilities
│   └── build.gradle.kts
│
├── core-data/                              # Data layer abstractions
│   ├── src/main/kotlin/com/photoframe/core/data/
│   │   ├── repository/                    # Repository interfaces
│   │   ├── model/                         # Domain models (immutable)
│   │   └── datasource/                    # DataSource interfaces
│   └── build.gradle.kts
│
├── core-network/                           # SMB/network implementation
│   ├── src/main/kotlin/com/photoframe/core/network/
│   │   ├── smb/                           # SMB client (jcifs-ng wrapper)
│   │   │   ├── SmbClient.kt
│   │   │   ├── SmbConnectionPool.kt
│   │   │   ├── SmbPhotoScanner.kt
│   │   │   └── SmbPhotoLoader.kt
│   │   ├── discovery/                     # Network discovery
│   │   │   └── NetworkDiscoveryService.kt
│   │   └── di/                            # Network DI module
│   └── build.gradle.kts
│
├── core-database/                          # Local persistence (Room)
│   ├── src/main/kotlin/com/photoframe/core/database/
│   │   ├── PhotoFrameDatabase.kt
│   │   ├── dao/                           # DAOs
│   │   │   ├── PhotoDao.kt
│   │   │   └── SettingsDao.kt
│   │   ├── entity/                        # Room entities
│   │   └── di/                            # Database DI module
│   └── build.gradle.kts
│
├── feature-slideshow/                      # Slideshow feature module
│   ├── src/main/kotlin/com/photoframe/feature/slideshow/
│   │   ├── ui/                            # Compose UI
│   │   │   ├── SlideshowScreen.kt
│   │   │   ├── SlideshowViewModel.kt
│   │   │   └── components/              # Reusable UI components
│   │   ├── domain/                        # Feature-specific use cases
│   │   │   ├── usecase/
│   │   │   │   ├── LoadNextPhotoUseCase.kt
│   │   │   │   ├── PreloadPhotosUseCase.kt
│   │   │   │   └── GetRandomPhotoUseCase.kt
│   │   │   └── repository/
│   │   │       └── SlideshowRepository.kt
│   │   ├── data/                          # Feature-specific data layer
│   │   │   ├── repository/
│   │   │   │   └── SlideshowRepositoryImpl.kt
│   │   │   └── cache/
│   │   │       └── PhotoCache.kt
│   │   └── di/                            # Feature DI module
│   └── build.gradle.kts
│
├── feature-settings/                       # Settings feature module
│   ├── src/main/kotlin/com/photoframe/feature/settings/
│   │   ├── ui/
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── SettingsViewModel.kt
│   │   │   └── components/
│   │   ├── domain/
│   │   │   ├── usecase/
│   │   │   │   ├── SaveSmbConfigUseCase.kt
│   │   │   │   ├── TestSmbConnectionUseCase.kt
│   │   │   │   ├── DiscoverSmbServersUseCase.kt
│   │   │   │   └── SaveScheduleUseCase.kt
│   │   │   └── repository/
│   │   │       └── SettingsRepository.kt
│   │   ├── data/
│   │   │   └── repository/
│   │   │       └── SettingsRepositoryImpl.kt
│   │   └── di/
│   └── build.gradle.kts
│
└── feature-scheduling/                     # Scheduling feature module
    ├── src/main/kotlin/com/photoframe/feature/scheduling/
    │   ├── scheduler/
    │   │   ├── SlideshowScheduler.kt      # WorkManager-based scheduler
    │   │   ├── ScreenControlWorker.kt     # Worker for screen on/off
    │   │   └── WakeLockManager.kt         # Wake lock management
    │   ├── domain/
    │   │   └── usecase/
    │   │       ├── ScheduleSlideshowUseCase.kt
    │   │       └── CancelScheduleUseCase.kt
    │   └── di/
    └── build.gradle.kts
```

### Component Design

#### Core Layer

**1. core-common Module**
- **Purpose**: Shared utilities with no Android dependencies
- **Key Components**:
  - `Result<T>`: Sealed class for success/failure handling
  - `DispatcherProvider`: Interface for coroutine dispatchers (testing abstraction)
  - Pure Kotlin utilities (date formatting, string utilities)
- **Dependencies**: None (pure Kotlin)

**2. core-data Module**
- **Purpose**: Domain models and repository interfaces
- **Key Components**:
  - Domain models (immutable data classes):
    ```kotlin
    data class Photo(
        val id: String,
        val path: String,
        val fileName: String,
        val lastModified: Long,
        val sizeBytes: Long,
        val isAvailable: Boolean = true
    )

    data class SmbConfig(
        val serverAddress: String,
        val shareName: String,
        val folderPath: String,
        val username: String?,
        val password: String?,
        val domain: String?,
        val useSubfolders: Boolean
    )

    data class Schedule(
        val id: String,
        val enabledDays: Set<DayOfWeek>,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val isEnabled: Boolean
    )

    data class SlideshowSettings(
        val transitionDurationMs: Long,
        val displayDurationMs: Long,
        val transitionEffect: TransitionEffect,
        val preloadCount: Int
    )
    ```
  - Repository interfaces:
    ```kotlin
    interface PhotoRepository {
        suspend fun scanPhotos(config: SmbConfig): Result<List<Photo>>
        suspend fun getPhotoStream(photo: Photo): Result<InputStream>
        suspend fun getRandomPhoto(excludeIds: Set<String>): Result<Photo>
        fun observePhotoCount(): Flow<Int>
    }

    interface SettingsRepository {
        suspend fun saveSmbConfig(config: SmbConfig): Result<Unit>
        suspend fun getSmbConfig(): Result<SmbConfig?>
        fun observeSmbConfig(): Flow<SmbConfig?>

        suspend fun saveSchedule(schedule: Schedule): Result<Unit>
        suspend fun getSchedule(): Result<Schedule?>
        fun observeSchedule(): Flow<Schedule?>

        suspend fun saveSlideshowSettings(settings: SlideshowSettings): Result<Unit>
        fun observeSlideshowSettings(): Flow<SlideshowSettings>
    }
    ```
- **Dependencies**: core-common, kotlinx-coroutines-core

**3. core-network Module**
- **Purpose**: SMB network operations and discovery
- **Key Components**:
  - `SmbClient`: Wraps jcifs-ng library
    ```kotlin
    interface SmbClient {
        suspend fun connect(config: SmbConfig): Result<SmbConnection>
        suspend fun disconnect(connection: SmbConnection)
        suspend fun testConnection(config: SmbConfig): Result<Boolean>
    }
    ```
  - `SmbPhotoScanner`: Recursively scans SMB shares for photos
    ```kotlin
    interface SmbPhotoScanner {
        suspend fun scanFolder(
            connection: SmbConnection,
            folderPath: String,
            recursive: Boolean
        ): Flow<Photo>
    }
    ```
  - `SmbPhotoLoader`: Loads photo data from SMB shares
    ```kotlin
    interface SmbPhotoLoader {
        suspend fun loadPhoto(
            connection: SmbConnection,
            photo: Photo
        ): Result<ByteArray>
    }
    ```
  - `SmbConnectionPool`: Manages SMB connections for reuse
    ```kotlin
    class SmbConnectionPool(
        private val maxConnections: Int = 3
    ) {
        private val mutex = Mutex()
        private val connections = mutableListOf<SmbConnection>()

        suspend fun acquireConnection(config: SmbConfig): SmbConnection
        suspend fun releaseConnection(connection: SmbConnection)
    }
    ```
  - `NetworkDiscoveryService`: Discovers SMB servers on local network
    ```kotlin
    interface NetworkDiscoveryService {
        suspend fun discoverServers(timeoutMs: Long): Flow<DiscoveredServer>
    }
    ```
- **Dependencies**: core-common, core-data, jcifs-ng, kotlinx-coroutines-core
- **Thread Safety**: All operations use Dispatchers.IO, connection pool protected by Mutex

**4. core-database Module**
- **Purpose**: Local persistence with Room
- **Key Components**:
  - `PhotoFrameDatabase`: Room database
  - `PhotoDao`: CRUD operations for photos
    ```kotlin
    @Dao
    interface PhotoDao {
        @Query("SELECT * FROM photos WHERE isAvailable = 1")
        fun observeAvailablePhotos(): Flow<List<PhotoEntity>>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertPhotos(photos: List<PhotoEntity>)

        @Query("SELECT * FROM photos WHERE isAvailable = 1 ORDER BY RANDOM() LIMIT 1")
        suspend fun getRandomPhoto(): PhotoEntity?

        @Query("UPDATE photos SET isAvailable = 0 WHERE id NOT IN (:currentIds)")
        suspend fun markStalePhotos(currentIds: List<String>)
    }
    ```
  - `SettingsDao`: CRUD operations for settings
    ```kotlin
    @Dao
    interface SettingsDao {
        @Query("SELECT * FROM settings WHERE key = :key")
        fun observeSettingByKey(key: String): Flow<SettingEntity?>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertSetting(setting: SettingEntity)
    }
    ```
- **Dependencies**: core-common, core-data, Room
- **Thread Safety**: Room handles thread safety internally

#### Feature Layer

**5. feature-slideshow Module**
- **Purpose**: Slideshow display and photo management
- **Key Components**:
  - `SlideshowViewModel`: UI state management
    ```kotlin
    class SlideshowViewModel(
        private val loadNextPhotoUseCase: LoadNextPhotoUseCase,
        private val preloadPhotosUseCase: PreloadPhotosUseCase,
        private val observeSlideshowSettingsUseCase: ObserveSlideshowSettingsUseCase
    ) : ViewModel() {

        private val _state = MutableStateFlow<SlideshowState>(SlideshowState.Loading)
        val state: StateFlow<SlideshowState> = _state.asStateFlow()

        fun start() { /* ... */ }
        fun pause() { /* ... */ }
        fun next() { /* ... */ }
    }

    sealed class SlideshowState {
        object Loading : SlideshowState()
        data class Playing(
            val currentPhoto: Photo,
            val preloadedPhotos: List<Photo>
        ) : SlideshowState()
        data class Error(val message: String) : SlideshowState()
        object NoPhotos : SlideshowState()
    }
    ```
  - Use Cases:
    - `LoadNextPhotoUseCase`: Loads next random photo
    - `PreloadPhotosUseCase`: Preloads N photos for smooth transitions
    - `GetRandomPhotoUseCase`: Gets random photo excluding recent ones
  - `SlideshowRepositoryImpl`: Implements photo loading logic
    ```kotlin
    class SlideshowRepositoryImpl(
        private val photoDao: PhotoDao,
        private val smbPhotoLoader: SmbPhotoLoader,
        private val smbClient: SmbClient,
        private val photoCache: PhotoCache,
        private val ioDispatcher: CoroutineDispatcher
    ) : SlideshowRepository {

        override suspend fun getRandomPhoto(excludeIds: Set<String>): Result<Photo> =
            withContext(ioDispatcher) {
                // Implementation
            }

        override suspend fun loadPhotoData(photo: Photo): Result<ByteArray> =
            withContext(ioDispatcher) {
                photoCache.get(photo.id) ?: run {
                    val data = smbPhotoLoader.loadPhoto(connection, photo).getOrThrow()
                    photoCache.put(photo.id, data)
                    data
                }
            }
    }
    ```
  - `PhotoCache`: LRU cache for photo data
    ```kotlin
    class PhotoCache(private val maxSizeBytes: Long) {
        private val mutex = Mutex()
        private val cache = LinkedHashMap<String, ByteArray>(16, 0.75f, true)
        private var currentSizeBytes = 0L

        suspend fun get(key: String): ByteArray? = mutex.withLock {
            cache[key]
        }

        suspend fun put(key: String, value: ByteArray) = mutex.withLock {
            // LRU eviction logic
        }
    }
    ```
- **Dependencies**: core-common, core-data, core-network, core-database, Compose, Hilt
- **Thread Safety**: StateFlow for UI state, Mutex for cache, IO dispatcher for network ops

**6. feature-settings Module**
- **Purpose**: Configuration UI and management
- **Key Components**:
  - `SettingsViewModel`: UI state management
    ```kotlin
    class SettingsViewModel(
        private val saveSmbConfigUseCase: SaveSmbConfigUseCase,
        private val testSmbConnectionUseCase: TestSmbConnectionUseCase,
        private val discoverSmbServersUseCase: DiscoverSmbServersUseCase,
        private val saveScheduleUseCase: SaveScheduleUseCase
    ) : ViewModel() {

        private val _state = MutableStateFlow<SettingsState>(SettingsState.Idle)
        val state: StateFlow<SettingsState> = _state.asStateFlow()

        fun saveSmbConfig(config: SmbConfig) { /* ... */ }
        fun testConnection(config: SmbConfig) { /* ... */ }
        fun discoverServers() { /* ... */ }
    }
    ```
  - Use Cases:
    - `SaveSmbConfigUseCase`: Validates and saves SMB configuration
    - `TestSmbConnectionUseCase`: Tests SMB connection before saving
    - `DiscoverSmbServersUseCase`: Discovers SMB servers on network
    - `SaveScheduleUseCase`: Validates and saves schedule
  - `SettingsRepositoryImpl`: Implements settings persistence
- **Dependencies**: core-common, core-data, core-network, core-database, Compose, Hilt
- **Thread Safety**: StateFlow for UI state, Room for persistence

**7. feature-scheduling Module**
- **Purpose**: Automated scheduling and screen control
- **Key Components**:
  - `SlideshowScheduler`: WorkManager-based scheduler
    ```kotlin
    class SlideshowScheduler(
        private val workManager: WorkManager,
        private val context: Context
    ) {
        fun scheduleSlideshow(schedule: Schedule) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // Calculate next run time
            val delay = calculateDelayUntilNextStart(schedule)

            val request = OneTimeWorkRequestBuilder<ScreenControlWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(workDataOf("action" to "START"))
                .build()

            workManager.enqueueUniqueWork(
                "slideshow_schedule",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelSchedule() {
            workManager.cancelUniqueWork("slideshow_schedule")
        }
    }
    ```
  - `ScreenControlWorker`: Worker for starting/stopping slideshow
    ```kotlin
    class ScreenControlWorker(
        context: Context,
        params: WorkerParameters,
        private val wakeLockManager: WakeLockManager
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            val action = inputData.getString("action")

            return when (action) {
                "START" -> {
                    wakeLockManager.acquireWakeLock()
                    // Start slideshow activity
                    scheduleStopWork()
                    Result.success()
                }
                "STOP" -> {
                    wakeLockManager.releaseWakeLock()
                    // Stop slideshow activity
                    scheduleNextStartWork()
                    Result.success()
                }
                else -> Result.failure()
            }
        }
    }
    ```
  - `WakeLockManager`: Manages wake locks safely
    ```kotlin
    class WakeLockManager(private val context: Context) {
        private var wakeLock: PowerManager.WakeLock? = null
        private val mutex = Mutex()

        suspend fun acquireWakeLock() = mutex.withLock {
            if (wakeLock?.isHeld != true) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "PhotoFrame::WakeLock"
                )
                wakeLock?.acquire(10 * 60 * 1000L) // 10 minute timeout
            }
        }

        suspend fun releaseWakeLock() = mutex.withLock {
            wakeLock?.release()
            wakeLock = null
        }
    }
    ```
- **Dependencies**: core-common, core-data, WorkManager, Hilt
- **Thread Safety**: Mutex for wake lock, WorkManager handles concurrency

### Data Flow

**Photo Loading Flow (Happy Path)**:
```
User/Scheduler → SlideshowViewModel
                      ↓
                LoadNextPhotoUseCase
                      ↓
              SlideshowRepository
                      ↓
          ┌───────────┴───────────┐
          ↓                       ↓
      PhotoDao              SmbPhotoLoader
    (get random)           (load from SMB)
          ↓                       ↓
    Photo metadata          ByteArray data
          ↓                       ↓
          └───────────┬───────────┘
                      ↓
                 PhotoCache
              (LRU, thread-safe)
                      ↓
                  ViewModel
              (StateFlow update)
                      ↓
                Compose UI
              (render photo)
```

**Settings Configuration Flow**:
```
User → SettingsScreen (Compose)
              ↓
       SettingsViewModel
              ↓
      SaveSmbConfigUseCase
       (validation logic)
              ↓
      SettingsRepository
              ↓
    ┌─────────┴─────────┐
    ↓                   ↓
SettingsDao      TestConnection
(persist)          (validate)
    ↓                   ↓
    └─────────┬─────────┘
              ↓
     StateFlow update
              ↓
      UI feedback
```

**Scheduling Flow**:
```
User sets schedule → SettingsViewModel
                            ↓
                    SaveScheduleUseCase
                            ↓
                    SettingsRepository
                            ↓
                      SettingsDao
                    (persist schedule)
                            ↓
                    SlideshowScheduler
                            ↓
                      WorkManager
               (schedule PeriodicWorkRequest)
                            ↓
          At scheduled time: ScreenControlWorker
                            ↓
                  WakeLockManager.acquire()
                            ↓
                  Start MainActivity intent
                            ↓
                  SlideshowScreen displayed
```

### Integration Points

**1. SMB/Samba Network**
- **Library**: jcifs-ng (Apache 2.0 licensed)
- **Pattern**: Connection pooling (max 3 concurrent connections)
- **Error Handling**: Retry with exponential backoff, connection timeout
- **Thread Safety**: All SMB operations on Dispatchers.IO

**2. Local Database (Room)**
- **Purpose**: Photo metadata cache, settings persistence
- **Migration Strategy**: Room auto-migration annotations
- **Thread Safety**: Room enforces main thread checks, all queries suspend functions

**3. WorkManager**
- **Purpose**: Scheduled slideshow start/stop
- **Constraints**: Battery not low, device idle
- **Persistence**: WorkManager persists across reboots

**4. Android System**
- **Wake Locks**: PowerManager for keeping screen on
- **Network**: ConnectivityManager for network state monitoring
- **Permissions**: WAKE_LOCK, INTERNET, ACCESS_NETWORK_STATE

---

## 3. Module Impact Analysis

### New Modules

Since this is a **greenfield project**, all modules are new:

#### app Module
**Files to Create**:
- `PhotoFrameApplication.kt` - Application class
- `MainActivity.kt` - Single activity host
- `di/AppModule.kt` - Application-level DI
- `AndroidManifest.xml` - Permissions, application declaration

**Dependencies**:
- All feature modules
- Hilt Android
- Compose Activity

**Risk Assessment**: ✅ Low Risk - Standard Android app setup

**Effort Estimate**: Small (1-2 days)

#### core-common Module
**Files to Create**:
- `result/Result.kt` - Result wrapper
- `dispatchers/DispatcherProvider.kt` - Dispatcher abstraction
- `utils/DateUtils.kt` - Date utilities

**Dependencies**: kotlinx-coroutines-core

**Risk Assessment**: ✅ Low Risk - Pure Kotlin, no Android deps

**Effort Estimate**: Small (1 day)

#### core-data Module
**Files to Create**:
- `model/Photo.kt`, `SmbConfig.kt`, `Schedule.kt`, `SlideshowSettings.kt`
- `repository/PhotoRepository.kt`, `SettingsRepository.kt`

**Dependencies**: core-common, kotlinx-coroutines-core

**Risk Assessment**: ✅ Low Risk - Just interfaces and data classes

**Effort Estimate**: Small (1-2 days)

#### core-network Module
**Files to Create**:
- `smb/SmbClient.kt`, `SmbConnectionPool.kt`, `SmbPhotoScanner.kt`, `SmbPhotoLoader.kt`
- `discovery/NetworkDiscoveryService.kt`
- `di/NetworkModule.kt`

**Dependencies**: core-common, core-data, jcifs-ng

**Risk Assessment**: ⚙️ Medium Risk - SMB library integration, network discovery complexity

**Effort Estimate**: Large (5-7 days)

#### core-database Module
**Files to Create**:
- `PhotoFrameDatabase.kt`
- `dao/PhotoDao.kt`, `SettingsDao.kt`
- `entity/PhotoEntity.kt`, `SettingEntity.kt`
- `di/DatabaseModule.kt`

**Dependencies**: core-common, core-data, Room

**Risk Assessment**: ✅ Low Risk - Standard Room setup

**Effort Estimate**: Medium (3-4 days)

#### feature-slideshow Module
**Files to Create**:
- `ui/SlideshowScreen.kt`, `SlideshowViewModel.kt`, `components/PhotoDisplay.kt`, `TransitionEffect.kt`
- `domain/usecase/LoadNextPhotoUseCase.kt`, `PreloadPhotosUseCase.kt`, `GetRandomPhotoUseCase.kt`
- `domain/repository/SlideshowRepository.kt`
- `data/repository/SlideshowRepositoryImpl.kt`
- `data/cache/PhotoCache.kt`
- `di/SlideshowModule.kt`

**Dependencies**: All core modules, Compose, Hilt, Coil

**Risk Assessment**: ⚙️ Medium Risk - Complex caching logic, preloading strategy

**Effort Estimate**: Large (7-10 days)

#### feature-settings Module
**Files to Create**:
- `ui/SettingsScreen.kt`, `SettingsViewModel.kt`, `components/SmbConfigForm.kt`, `ScheduleForm.kt`
- `domain/usecase/SaveSmbConfigUseCase.kt`, `TestSmbConnectionUseCase.kt`, `DiscoverSmbServersUseCase.kt`, `SaveScheduleUseCase.kt`
- `domain/repository/SettingsRepository.kt`
- `data/repository/SettingsRepositoryImpl.kt`
- `di/SettingsModule.kt`

**Dependencies**: All core modules, Compose, Hilt

**Risk Assessment**: ⚙️ Medium Risk - Complex form validation, network discovery UI

**Effort Estimate**: Large (5-7 days)

#### feature-scheduling Module
**Files to Create**:
- `scheduler/SlideshowScheduler.kt`, `ScreenControlWorker.kt`, `WakeLockManager.kt`
- `domain/usecase/ScheduleSlideshowUseCase.kt`, `CancelScheduleUseCase.kt`
- `di/SchedulingModule.kt`

**Dependencies**: core-common, core-data, WorkManager, Hilt

**Risk Assessment**: ⚙️ Medium Risk - WorkManager scheduling complexity, wake lock management

**Effort Estimate**: Medium (4-5 days)

### Overall Project Risk Assessment

**High Risk Areas**: None identified

**Medium Risk Areas**:
- SMB network operations (reliability, error handling)
- Photo caching strategy (memory management)
- Scheduling system (battery optimization, edge cases)

**Low Risk Areas**:
- Database layer
- Settings persistence
- UI components

**Total Effort Estimate**: 28-38 development days (approximately 6-8 weeks with one developer)

---

## 4. Technical Decisions

### Decision 1: Multi-Module Architecture

**Choice**: Organize project into 8 Gradle modules by feature and layer

**Rationale** (Modularity Focus):
- **Separation of Concerns**: Each module has a single, clear responsibility
- **Parallel Development**: Multiple developers can work on different modules simultaneously
- **Build Performance**: Gradle can cache and parallelize module builds
- **Testability**: Modules can be tested in isolation
- **Reusability**: Core modules can be reused in future projects or Phase 2 extensions
- **Dependency Control**: Prevents circular dependencies, enforces unidirectional data flow

**Alternatives Considered**:
- **Single-module monolith**: Simpler initially but harder to maintain at scale
- **Package-based organization**: Less enforced separation, easier to violate boundaries

**Trade-offs**:
- **Pros**:
  - Clear boundaries between layers
  - Easier to onboard new developers
  - Better code organization
  - Supports future feature additions without conflicts
- **Cons**:
  - More Gradle configuration files
  - Slightly longer initial setup time
  - More complex navigation between files in IDE

### Decision 2: Repository Pattern with UseCase Layer

**Choice**: Implement Repository pattern with an additional UseCase/Interactor layer

**Rationale** (Modularity Focus):
- **Single Responsibility**: UseCases encapsulate single business operations
- **Testability**: Business logic testable without Android dependencies
- **Reusability**: UseCases can be composed and reused across ViewModels
- **Clear API**: Each UseCase has a clear, focused interface
- **Future Extensions**: Easy to add new UseCases for Phase 2 features

**Alternatives Considered**:
- **ViewModel directly calling Repository**: Simpler but mixes concerns
- **Clean Architecture with Entities**: More layers but potentially over-engineered for this scope

**Trade-offs**:
- **Pros**:
  - Business logic separate from UI logic
  - Easy to unit test
  - Clear entry points for features
  - Supports composition and reuse
- **Cons**:
  - One additional layer of indirection
  - More classes to create and maintain

### Decision 3: Immutable Data Models with Kotlin Data Classes

**Choice**: All domain models are immutable `data class` instances with `val` properties

**Rationale** (Modularity Focus):
- **Thread Safety**: Immutable objects cannot have race conditions
- **Predictability**: State cannot be accidentally modified
- **Testability**: Easy to create test fixtures
- **Copy Semantics**: Kotlin `copy()` method for updates
- **Flow Integration**: Works seamlessly with StateFlow

**Alternatives Considered**:
- **Mutable data classes**: Simpler for some use cases but thread-unsafe
- **Builder pattern**: More verbose, less idiomatic in Kotlin

**Trade-offs**:
- **Pros**:
  - Thread-safe by default
  - No defensive copying needed
  - Clear data flow
- **Cons**:
  - Must use `copy()` for updates
  - Slightly more memory allocations (negligible in practice)

### Decision 4: Coroutines + Flow for Async Operations

**Choice**: Kotlin Coroutines with suspend functions and Flow for reactive streams

**Rationale** (Modularity Focus):
- **Composability**: Suspend functions compose naturally
- **Cancellation**: Built-in cancellation support
- **Testing**: Easy to test with TestDispatchers
- **Thread Safety**: Structured concurrency prevents leaks
- **Android Integration**: Native support in Jetpack libraries

**Alternatives Considered**:
- **RxJava**: More powerful operators but steeper learning curve
- **Callbacks**: Simpler but leads to callback hell

**Trade-offs**:
- **Pros**:
  - Native Kotlin support
  - Easier to read and maintain
  - Better integration with Compose
  - Lower cognitive overhead
- **Cons**:
  - Fewer advanced operators compared to RxJava
  - Team must understand coroutine context and dispatchers

### Decision 5: Hilt for Dependency Injection

**Choice**: Hilt (Dagger wrapper) for compile-time dependency injection

**Rationale** (Modularity Focus):
- **Testability**: Easy to swap implementations for testing
- **Loose Coupling**: Components depend on interfaces, not implementations
- **Lifecycle Awareness**: Android-aware scoping
- **Compile-Time Safety**: Errors caught at compile time
- **Standard Solution**: Official Google recommendation

**Alternatives Considered**:
- **Koin**: Runtime DI, simpler but less safe
- **Manual DI**: Most flexible but boilerplate-heavy

**Trade-offs**:
- **Pros**:
  - Type-safe
  - Performance (compile-time)
  - IDE support
  - Mature ecosystem
- **Cons**:
  - Steep learning curve
  - Longer compile times
  - Verbose annotations

### Decision 6: Room for Local Persistence

**Choice**: Room database for photo metadata and settings

**Rationale** (Modularity Focus):
- **Type Safety**: Compile-time SQL validation
- **Coroutines Support**: Native suspend function support
- **Flow Support**: Reactive queries with Flow
- **Migration Support**: Versioned schema migrations
- **Testing**: In-memory database for tests

**Alternatives Considered**:
- **DataStore**: Good for settings but not suitable for structured data
- **SQLite directly**: More control but more boilerplate

**Trade-offs**:
- **Pros**:
  - Robust and battle-tested
  - Excellent IDE support
  - Type-safe queries
  - Easy to test
- **Cons**:
  - Annotation processing overhead
  - Limited to SQLite features

### Decision 7: jcifs-ng for SMB Protocol

**Choice**: jcifs-ng library for SMB/CIFS protocol support

**Rationale** (Modularity Focus):
- **SMB2/SMB3 Support**: Modern protocol support
- **Active Maintenance**: Regularly updated
- **Apache 2.0 License**: Compatible with commercial use
- **Well-Documented**: Good documentation and examples

**Alternatives Considered**:
- **smbj**: Modern library but less mature
- **Custom implementation**: Too complex and risky

**Trade-offs**:
- **Pros**:
  - Mature and stable
  - Supports authentication methods
  - Good performance
- **Cons**:
  - Large library size (~2MB)
  - Java-based API (not Kotlin-idiomatic)

### Decision 8: WorkManager for Scheduling

**Choice**: WorkManager for scheduled slideshow start/stop

**Rationale** (Modularity Focus):
- **Reliability**: Guaranteed execution even after reboot
- **Battery Optimization**: Respects system constraints
- **Flexible Constraints**: Can specify battery, network, etc.
- **Official Solution**: Google's recommended approach

**Alternatives Considered**:
- **AlarmManager**: Less reliable, battery-intensive
- **JobScheduler**: Deprecated in favor of WorkManager

**Trade-offs**:
- **Pros**:
  - Reliable across Android versions
  - Battery-friendly
  - Survives app restarts
- **Cons**:
  - Not guaranteed to run at exact time
  - Learning curve for constraints

### Decision 9: LRU Cache for Photo Data

**Choice**: Custom LRU cache implementation with Mutex protection

**Rationale** (Modularity Focus):
- **Memory Management**: Automatic eviction of old entries
- **Thread Safety**: Mutex ensures safe concurrent access
- **Customizable**: Can tune cache size based on device memory
- **Testability**: Easy to test cache behavior

**Alternatives Considered**:
- **Android LruCache**: Android-dependent, harder to test
- **No caching**: Would require reloading photos frequently

**Trade-offs**:
- **Pros**:
  - Efficient memory usage
  - Fast access to recent photos
  - Testable without Android
- **Cons**:
  - Additional complexity
  - Must manage cache size carefully

### Decision 10: StateFlow for UI State Management

**Choice**: StateFlow for ViewModel state exposed to UI

**Rationale** (Modularity Focus):
- **Thread Safety**: Atomic updates, safe from any thread
- **Compose Integration**: Native support in Compose
- **Lifecycle Awareness**: Automatic cleanup with ViewModelScope
- **Conflict-Free**: Last value always available to new collectors

**Alternatives Considered**:
- **LiveData**: Android-specific, less type-safe
- **SharedFlow**: More powerful but unnecessary complexity

**Trade-offs**:
- **Pros**:
  - Simple and safe
  - Great IDE support
  - Native Compose integration
- **Cons**:
  - Conflates rapid updates (usually desired for UI)
  - Requires initial value

---

## 5. Trade-offs & Concerns

### Strengths (from Modularity Perspective)

✅ **Clean Separation of Concerns**
- Each module has a single, well-defined responsibility
- Clear boundaries prevent accidental coupling
- Easy to understand where code should live

✅ **High Testability**
- Business logic isolated from Android framework
- All dependencies injected via interfaces
- UseCase pattern enables focused unit tests
- Repository abstraction allows mocking data sources

✅ **Reusability**
- Core modules can be reused in other projects
- UseCases can be composed for new features
- Network and database layers ready for Phase 2 cloud integration

✅ **Maintainability**
- Small, focused modules easier to understand
- Clear dependency graph prevents spaghetti code
- Immutable models prevent accidental state changes

✅ **Parallel Development**
- Multiple developers can work on different modules without conflicts
- Clear interfaces define contracts between teams

✅ **Future Extensibility**
- Easy to add new feature modules
- Repository pattern ready for multiple data sources (Phase 2: cloud)
- UseCase layer supports new business logic without UI changes

✅ **Thread Safety by Design**
- Immutable models eliminate race conditions
- Mutex-protected caches ensure safe concurrent access
- StateFlow provides thread-safe state updates
- Clear dispatcher usage prevents main thread blocking

### Weaknesses / Concerns

⚠️ **Complexity for Small Team**
- **Concern**: 8 modules may be overwhelming for a single developer initially
- **Mitigation**:
  - Clear documentation and module responsibility chart
  - Start with core modules first, add feature modules incrementally
  - IDE support makes navigation manageable

⚠️ **More Boilerplate Code**
- **Concern**: Repository + UseCase pattern creates more classes
- **Mitigation**:
  - Each layer serves a clear purpose
  - Code generation (Hilt) reduces manual boilerplate
  - Long-term benefits outweigh initial overhead

⚠️ **Initial Build Time**
- **Concern**: More modules means more Gradle overhead initially
- **Mitigation**:
  - Gradle build caching helps
  - Parallel builds reduce total time
  - Only matters during development, not runtime

⚠️ **Potential Over-Engineering**
- **Concern**: May be too much structure for MVP phase
- **Mitigation**:
  - Architecture scales well to Phase 2 (cloud services)
  - Better to start structured than refactor later
  - Each layer adds tangible value (testability, reusability)

⚠️ **Learning Curve for New Developers**
- **Concern**: Complex dependency graph may confuse newcomers
- **Mitigation**:
  - Comprehensive documentation
  - Clear module naming conventions
  - Dependency diagram included

⚠️ **Dependency Injection Complexity**
- **Concern**: Hilt requires understanding of scoping and modules
- **Mitigation**:
  - Scopes are well-documented
  - Standard patterns from Google examples
  - Compile-time errors guide correct usage

### Trade-offs

**What This Approach Gains**:
- Strong foundation for long-term maintenance
- Easy to test and debug
- Clear code ownership boundaries
- Future-proof for Phase 2 features
- Supports team growth

**What This Approach Costs**:
- Higher initial development time (more classes to create)
- More files to navigate (though IDE helps)
- Requires team to understand architectural patterns
- Some abstractions may feel unnecessary for simple features

---

## 6. Requirements Coverage

This section maps each user story from the PRD to the architectural components responsible for implementation.

### US-1.1: Manual SMB Configuration
**User Story**: As a user, I want to manually configure my SMB/Samba server connection so that the app can access my photos.

**Covered By**:
- `feature-settings` module:
  - `SettingsScreen` (Compose UI for form input)
  - `SettingsViewModel` (UI state management)
  - `SaveSmbConfigUseCase` (validation and persistence)
- `core-network` module:
  - `SmbClient` (connection testing)
- `core-database` module:
  - `SettingsDao` (persist configuration)

**Implementation Approach**:
- User fills out SMB form (server, share, credentials)
- ViewModel validates input (non-empty fields)
- UseCase calls `SmbClient.testConnection()` to verify
- If successful, saves to Room via `SettingsDao`
- UI shows success/error feedback

---

### US-1.2: Network Discovery
**User Story**: As a user, I want the app to discover available SMB servers on my network so that I don't have to manually type server addresses.

**Covered By**:
- `feature-settings` module:
  - `SettingsScreen` (discovery UI)
  - `SettingsViewModel` (manage discovery state)
  - `DiscoverSmbServersUseCase` (discovery logic)
- `core-network` module:
  - `NetworkDiscoveryService` (NetBIOS discovery)

**Implementation Approach**:
- User taps "Discover Servers" button
- ViewModel calls `DiscoverSmbServersUseCase`
- UseCase collects Flow from `NetworkDiscoveryService.discoverServers()`
- Discovered servers shown in UI list
- User selects server, auto-populates form

---

### US-1.3: Connection Testing
**User Story**: As a user, I want to test my SMB connection before saving so that I know the configuration works.

**Covered By**:
- `feature-settings` module:
  - `TestSmbConnectionUseCase` (test logic)
- `core-network` module:
  - `SmbClient.testConnection()` (actual connection test)

**Implementation Approach**:
- User taps "Test Connection" button
- ViewModel calls `TestSmbConnectionUseCase`
- UseCase creates test connection using `SmbClient`
- Attempts to list files in root directory
- Returns success/failure with error message

---

### US-2.1: Photo Discovery and Scanning
**User Story**: As a user, I want the app to automatically discover and catalog all supported photo files in my configured SMB folder so that I can view them in the slideshow.

**Covered By**:
- `feature-slideshow` module:
  - `ScanPhotosUseCase` (trigger scanning)
- `core-network` module:
  - `SmbPhotoScanner` (recursive folder scanning)
- `core-database` module:
  - `PhotoDao` (store photo metadata)

**Implementation Approach**:
- On first launch or when SMB config changes, trigger scan
- `SmbPhotoScanner.scanFolder()` emits Flow<Photo>
- Filter by extension (.jpg, .jpeg, .png)
- Insert into Room database via `PhotoDao`
- Mark old photos as unavailable if not found

---

### US-2.2: Subfolder Inclusion
**User Story**: As a user, I want to optionally include subfolders in the photo scan so that I can organize my photos hierarchically.

**Covered By**:
- `core-data` module:
  - `SmbConfig.useSubfolders` (boolean flag)
- `core-network` module:
  - `SmbPhotoScanner` (recursive parameter)

**Implementation Approach**:
- User toggles "Include Subfolders" in settings
- Saved in `SmbConfig.useSubfolders`
- Passed to `SmbPhotoScanner.scanFolder(recursive = config.useSubfolders)`
- Scanner recursively walks directories if enabled

---

### US-2.3: Photo Metadata Caching
**User Story**: As a user, I want the app to cache photo metadata locally so that browsing is fast even when the network is slow.

**Covered By**:
- `core-database` module:
  - `PhotoDao` (persistent cache)
  - `PhotoEntity` (cached metadata)

**Implementation Approach**:
- After scanning, photo metadata stored in Room
- Random photo selection queries local database
- No network call needed for metadata
- Only load image data when displaying

---

### US-3.1: Random Slideshow Playback
**User Story**: As a user, I want photos to display in random order so that the slideshow feels fresh and unpredictable.

**Covered By**:
- `feature-slideshow` module:
  - `GetRandomPhotoUseCase` (random selection logic)
- `core-database` module:
  - `PhotoDao.getRandomPhoto()` (SQL ORDER BY RANDOM())

**Implementation Approach**:
- ViewModel calls `GetRandomPhotoUseCase`
- UseCase queries `PhotoDao.getRandomPhoto()`
- Excludes recently shown photos (last N IDs tracked in memory)
- Returns random photo not in exclusion list

---

### US-3.2: Configurable Display Duration
**User Story**: As a user, I want to configure how long each photo displays so that I can control the slideshow pace.

**Covered By**:
- `feature-settings` module:
  - `SettingsScreen` (duration slider)
  - `SaveSlideshowSettingsUseCase` (save settings)
- `core-data` module:
  - `SlideshowSettings.displayDurationMs` (stored value)

**Implementation Approach**:
- User adjusts slider in settings (5s - 60s range)
- Saved to Room via `SettingsDao`
- ViewModel observes `SlideshowSettings` Flow
- Applies delay between photo changes

---

### US-3.3: Smooth Transitions
**User Story**: As a user, I want smooth transitions between photos so that the slideshow looks professional.

**Covered By**:
- `feature-slideshow` module:
  - `SlideshowScreen` (Compose animations)
  - `TransitionEffect` (crossfade, slide, etc.)

**Implementation Approach**:
- Compose AnimatedContent for transitions
- Configurable transition effect (crossfade default)
- Duration pulled from `SlideshowSettings.transitionDurationMs`
- GPU-accelerated animations via Compose

---

### US-3.4: Read-Ahead Buffering
**User Story**: As a user, I expect photos to load without delay so that the slideshow plays smoothly without waiting.

**Covered By**:
- `feature-slideshow` module:
  - `PreloadPhotosUseCase` (preload next N photos)
  - `PhotoCache` (LRU cache for loaded photos)

**Implementation Approach**:
- While displaying current photo, preload next 2-3 photos
- `PreloadPhotosUseCase` loads photos into `PhotoCache`
- Cache holds ByteArray data for quick access
- LRU eviction prevents memory exhaustion

---

### US-4.1: Daily Time-Based Scheduling
**User Story**: As a user, I want to schedule when the slideshow runs so that it turns on automatically during certain hours.

**Covered By**:
- `feature-settings` module:
  - `SettingsScreen` (schedule configuration UI)
  - `SaveScheduleUseCase` (validation and save)
- `feature-scheduling` module:
  - `SlideshowScheduler` (WorkManager scheduling)
  - `ScreenControlWorker` (start/stop actions)

**Implementation Approach**:
- User sets start/end times in settings
- Saved to Room via `SettingsDao`
- `SlideshowScheduler.scheduleSlideshow()` called with `Schedule`
- WorkManager enqueues work at start time
- `ScreenControlWorker` launches activity and acquires wake lock

---

### US-4.2: Day-of-Week Selection
**User Story**: As a user, I want to specify which days the schedule applies so that I can run the slideshow only on weekends.

**Covered By**:
- `core-data` module:
  - `Schedule.enabledDays` (Set<DayOfWeek>)
- `feature-scheduling` module:
  - `SlideshowScheduler` (day-based logic)

**Implementation Approach**:
- User selects days in settings (multi-select)
- Stored in `Schedule.enabledDays`
- `SlideshowScheduler` checks current day before enqueuing work
- Skips scheduling if current day not enabled

---

### US-4.3: Automatic Wake/Sleep
**User Story**: As a user, I want the device to automatically wake and sleep according to the schedule so that I don't have to manually control it.

**Covered By**:
- `feature-scheduling` module:
  - `ScreenControlWorker` (wake/sleep actions)
  - `WakeLockManager` (wake lock management)

**Implementation Approach**:
- At start time: `ScreenControlWorker` acquires wake lock (SCREEN_BRIGHT_WAKE_LOCK)
- Launches MainActivity with FLAG_TURN_SCREEN_ON
- At end time: releases wake lock, finishes activity
- Device screen dims naturally after wake lock released

---

### US-5.1: Settings Screen
**User Story**: As a user, I want a simple settings screen where I can configure all slideshow options.

**Covered By**:
- `feature-settings` module:
  - `SettingsScreen` (Compose UI)
  - `SettingsViewModel` (state management)
  - Components for each setting type (SMB config, schedule, slideshow settings)

**Implementation Approach**:
- Single Compose screen with sections:
  - SMB Configuration
  - Slideshow Settings (duration, transition)
  - Schedule Configuration
- Each section uses reusable components
- ViewModel handles all save operations

---

### US-5.2: Schedule Enable/Disable Toggle
**User Story**: As a user, I want a simple toggle to enable or disable the schedule without deleting my configuration.

**Covered By**:
- `core-data` module:
  - `Schedule.isEnabled` (boolean flag)
- `feature-scheduling` module:
  - `SlideshowScheduler.cancelSchedule()` (disable scheduling)

**Implementation Approach**:
- Toggle switch in settings
- When disabled: `SlideshowScheduler.cancelSchedule()` cancels WorkManager work
- When enabled: re-schedules using existing `Schedule`
- Configuration preserved in database

---

## 7. Debate Summary

**Note**: This section will be updated after collaborative debate with Architect 2 (Performance-focused) and Architect 3 (Simplicity-focused).

### Initial Proposal Status

This is the initial proposal from Architect 1 (Modularity-focused). Feedback and consensus will be documented here after team discussion.

### Expected Discussion Points

I anticipate my teammates may raise concerns about:

**From Architect 2 (Performance-focused)**:
- UseCase layer adding latency
- Repository abstraction overhead
- Cache implementation performance
- Module boundaries impacting build time

**From Architect 3 (Simplicity-focused)**:
- Too many modules for MVP
- Repository + UseCase adding complexity
- Could be simpler with fewer abstractions
- Over-engineering for greenfield project

### My Position

I believe the modularity benefits outweigh the costs because:
1. Phase 2 cloud integration will benefit from clean abstractions
2. Testability is critical for network-dependent app
3. Clear boundaries prevent future technical debt
4. Team can grow without architecture refactor

I'm open to simplifying if teammates present compelling arguments, especially for areas where modularity doesn't add clear value.

---

## 8. Concurrency & Thread Safety

### Concurrent Operations Identified

This application has significant concurrency requirements due to network I/O, UI updates, and background scheduling:

**1. SMB Network Operations**
- **Description**: All SMB operations (scanning, photo loading) run on background threads
- **Concurrency Level**: Up to 3 concurrent SMB connections (pool limit)
- **Thread Pool**: Dispatchers.IO (shared thread pool)

**2. Photo Cache Access**
- **Description**: Multiple coroutines may read/write cache simultaneously
- **Concurrency Level**: Unbounded (any number of concurrent requests)
- **Protection**: Mutex synchronization

**3. Database Access**
- **Description**: Multiple ViewModels may query/update database
- **Concurrency Level**: Unbounded (Room handles internally)
- **Protection**: Room's built-in thread safety

**4. UI State Updates**
- **Description**: Background operations update ViewModel state for UI rendering
- **Concurrency Level**: Multiple background operations may complete simultaneously
- **Protection**: StateFlow atomic updates

**5. WorkManager Background Tasks**
- **Description**: Scheduled work runs independently of main app lifecycle
- **Concurrency Level**: Single worker at a time (WorkManager constraint)
- **Protection**: WorkManager guarantees sequential execution

**6. Wake Lock Management**
- **Description**: Acquire/release wake lock from worker and activity
- **Concurrency Level**: Potential concurrent access from worker and UI
- **Protection**: Mutex synchronization

### Thread Safety Guarantees

**Component: SmbClient & SmbConnectionPool**
- **Guarantee**: Thread-safe via connection pool with Mutex
- **Mechanism**: Mutex protects connection list, all operations on Dispatchers.IO
- **Behavior**: Up to 3 concurrent SMB operations, blocks if pool exhausted
- **Code Pattern**:
  ```kotlin
  class SmbConnectionPool {
      private val mutex = Mutex()
      private val connections = mutableListOf<SmbConnection>()

      suspend fun acquireConnection(config: SmbConfig): SmbConnection = mutex.withLock {
          // Safe concurrent access
      }
  }
  ```

**Component: PhotoCache**
- **Guarantee**: Thread-safe via Mutex for all read/write operations
- **Mechanism**: Mutex wraps LinkedHashMap access
- **Behavior**: Sequential access, LRU eviction protected
- **Code Pattern**:
  ```kotlin
  class PhotoCache {
      private val mutex = Mutex()
      private val cache = LinkedHashMap<String, ByteArray>()

      suspend fun get(key: String): ByteArray? = mutex.withLock {
          cache[key]
      }
  }
  ```

**Component: PhotoDao & SettingsDao (Room)**
- **Guarantee**: Thread-safe (Room's built-in guarantee)
- **Mechanism**: Room enforces main thread checks for non-suspend functions
- **Behavior**: All suspend functions safe to call from any dispatcher
- **Documentation**: Room documentation guarantees thread safety for suspend functions

**Component: ViewModels (StateFlow)**
- **Guarantee**: Thread-safe state updates via StateFlow
- **Mechanism**: StateFlow provides atomic compare-and-set semantics
- **Behavior**: Last update always visible to collectors, updates never lost
- **Code Pattern**:
  ```kotlin
  class SlideshowViewModel : ViewModel() {
      private val _state = MutableStateFlow<SlideshowState>(SlideshowState.Loading)
      val state: StateFlow<SlideshowState> = _state.asStateFlow()

      // Thread-safe updates
      _state.value = newState
  }
  ```

**Component: WakeLockManager**
- **Guarantee**: Thread-safe via Mutex
- **Mechanism**: Mutex protects wake lock acquire/release
- **Behavior**: Only one wake lock operation at a time, prevents double-acquire
- **Code Pattern**:
  ```kotlin
  class WakeLockManager {
      private val mutex = Mutex()
      private var wakeLock: PowerManager.WakeLock? = null

      suspend fun acquireWakeLock() = mutex.withLock {
          // Safe acquire
      }
  }
  ```

**Component: Repositories**
- **Guarantee**: Thread-safe (no mutable shared state)
- **Mechanism**: All operations are suspend functions, delegate to thread-safe components
- **Behavior**: Stateless, no synchronization needed

**Component: UseCases**
- **Guarantee**: Thread-safe (no mutable shared state)
- **Mechanism**: Stateless, all operations are suspend functions
- **Behavior**: Can be called from multiple coroutines safely

### Synchronization Mechanisms

| Component | Mechanism | Reason |
|-----------|-----------|--------|
| PhotoCache | Mutex | Protects mutable HashMap, ensures LRU invariants |
| SmbConnectionPool | Mutex | Protects connection list, ensures pool size limits |
| WakeLockManager | Mutex | Prevents concurrent acquire/release, ensures single wake lock |
| StateFlow | Built-in CAS | Atomic state updates, safe multi-collector |
| Room DAOs | Room internals | Thread-safe by design for suspend functions |
| Immutable Models | Immutability | No synchronization needed, cannot be modified |

### Dispatcher Usage (Kotlin Coroutines)

**Dispatchers.Main**:
- **Used For**: UI updates, Compose recomposition
- **Components**: ViewModels (StateFlow collectors), Compose UI
- **Pattern**: `viewModelScope.launch` defaults to Main

**Dispatchers.IO**:
- **Used For**: Network I/O, file I/O, database access
- **Components**:
  - All SMB operations (SmbClient, SmbPhotoScanner, SmbPhotoLoader)
  - PhotoCache (suspended I/O if needed)
  - Repository implementations
- **Pattern**: `withContext(Dispatchers.IO) { ... }`

**Dispatchers.Default**:
- **Used For**: CPU-intensive operations (if needed)
- **Components**: Image decoding (if not using Coil), large data processing
- **Pattern**: `withContext(Dispatchers.Default) { ... }`

### Race Condition Prevention

**Potential Race Condition 1: Photo Cache Concurrent Modification**
- **Scenario**: Multiple coroutines try to add/remove cache entries simultaneously
- **Prevention**: Mutex wraps all cache operations
- **Code**:
  ```kotlin
  suspend fun put(key: String, value: ByteArray) = mutex.withLock {
      cache[key] = value
      evictIfNeeded()
  }
  ```

**Potential Race Condition 2: Wake Lock Double-Acquire**
- **Scenario**: Worker and Activity both try to acquire wake lock
- **Prevention**: Mutex + check if already held
- **Code**:
  ```kotlin
  suspend fun acquireWakeLock() = mutex.withLock {
      if (wakeLock?.isHeld != true) {
          wakeLock = powerManager.newWakeLock(...)
          wakeLock?.acquire()
      }
  }
  ```

**Potential Race Condition 3: SMB Connection Pool Exhaustion**
- **Scenario**: More than 3 concurrent requests for connections
- **Prevention**: Mutex-protected pool, suspends until connection available
- **Code**:
  ```kotlin
  suspend fun acquireConnection(config: SmbConfig): SmbConnection = mutex.withLock {
      while (connections.size >= maxConnections) {
          // Could use Condition or Channel, but for simplicity, fail-fast
          throw ConnectionPoolExhaustedException()
      }
      // Create or reuse connection
  }
  ```

**Potential Race Condition 4: StateFlow Rapid Updates**
- **Scenario**: Background loads complete rapidly, multiple state updates
- **Prevention**: StateFlow conflates updates (only last value matters)
- **Behavior**: Safe and desired for UI state (latest state is what user should see)

**Potential Race Condition 5: Database Stale Photo Marking**
- **Scenario**: Scan updates photos while slideshow queries photos
- **Prevention**: Room transactions + immutable Photo objects
- **Code**:
  ```kotlin
  @Transaction
  suspend fun updatePhotos(newPhotos: List<PhotoEntity>) {
      val newIds = newPhotos.map { it.id }
      insertPhotos(newPhotos)
      markStalePhotos(newIds)
  }
  ```

### Performance Under Concurrent Load

**Expected Behavior**:
- **SMB Operations**: Up to 3 concurrent operations, others wait or fail-fast
- **Cache Access**: Sequential due to Mutex, acceptable since operations are fast (HashMap lookup)
- **Database Access**: Room uses connection pool, concurrent reads optimized
- **UI Updates**: StateFlow handles rapid updates gracefully (conflation)

**Bottlenecks / Contention Points**:
1. **SMB Connection Pool**: Limited to 3 connections, could be bottleneck if many concurrent requests
   - **Mitigation**: Preload photos during idle time, use cache aggressively
2. **Photo Cache Mutex**: Could contend under heavy load
   - **Mitigation**: Cache operations are fast (HashMap), unlikely to be bottleneck
3. **Network Bandwidth**: Actual bottleneck for photo loading
   - **Mitigation**: Preloading, image scaling/compression

**Stress Testing Recommendations**:
- Test with 100+ photos, rapid photo changes
- Test network disconnection during photo load
- Test concurrent settings saves while slideshow running
- Test wake lock acquire/release during screen transitions

### Concurrency Testing Strategy

**Unit Tests**:
```kotlin
@Test
fun `PhotoCache handles concurrent access correctly`() = runTest {
    val cache = PhotoCache(maxSizeBytes = 1024 * 1024)

    val jobs = List(100) { index ->
        launch {
            cache.put("key$index", ByteArray(1024))
        }
    }

    jobs.joinAll()

    // Verify no corruption
    assertEquals(100, cache.size())
}

@Test
fun `WakeLockManager prevents double-acquire`() = runTest {
    val wakeLockManager = WakeLockManager(context)

    val jobs = List(10) {
        launch {
            wakeLockManager.acquireWakeLock()
        }
    }

    jobs.joinAll()

    // Verify only one wake lock held
    assertTrue(wakeLockManager.isHeld())
    assertEquals(1, wakeLockManager.wakeLockCount())
}
```

**Integration Tests**:
- Test photo loading while settings being saved
- Test slideshow running while WorkManager schedules next run
- Test rapid start/stop cycles

---

## 9. Implementation Considerations

### Testing Strategy

**Unit Tests**:
- **UseCases**: Mock repository, test business logic in isolation
- **Repositories**: Mock data sources, test data flow
- **ViewModels**: Mock use cases, test state transitions
- **Cache**: Test LRU eviction, thread safety

**Integration Tests**:
- **Database**: In-memory Room database, test queries
- **SMB Operations**: Mock SMB client, test error handling
- **End-to-End**: Test full flow from UI to network

**UI Tests**:
- **Compose**: Test composable functions with preview parameters
- **Navigation**: Test screen transitions
- **User Actions**: Test button clicks, form submissions

**Test Coverage Targets**:
- UseCases: 90%+
- Repositories: 80%+
- ViewModels: 80%+
- UI: 60%+ (critical paths)

### Backward Compatibility

**Not Applicable**: Greenfield project, no backward compatibility concerns.

For future versions:
- Room migration strategy documented
- Settings schema versioning planned
- API versioning not needed (local-only app)

### Migration/Rollout Approach

**Initial Release (MVP Phase 1)**:
- Staged rollout to alpha/beta testers
- Monitor crash reports (Firebase Crashlytics)
- Collect feedback on settings UX

**Future Phases**:
- **Phase 2 (Cloud Services)**: Add new repositories, reuse core modules
- **Phase 3 (Advanced Features)**: Add new feature modules

**Rollback Plan**:
- Not critical (user-side app), but support uninstall/reinstall
- Settings backed up to allow restore

### Monitoring and Observability

**Logging Strategy**:
- **Error Level**: Failed SMB connections, database errors, worker failures
- **Warn Level**: Cache evictions, retry attempts
- **Info Level**: Slideshow start/stop, settings changes
- **Debug Level**: Photo loads, state transitions

**Metrics to Track**:
- SMB connection success rate
- Photo load times (p50, p95, p99)
- Cache hit rate
- WorkManager execution reliability
- App crashes and ANRs

**Tools**:
- **Firebase Crashlytics**: Crash reporting
- **Timber**: Structured logging
- **Android Profiler**: Performance analysis during development

### Performance Targets

- **Photo Load Time**: < 1 second for typical photo (2-5MB JPEG over WiFi)
- **Transition Smoothness**: 60fps during crossfade transitions
- **Memory Usage**: < 200MB total (including cache)
- **Battery Impact**: < 5% battery drain per hour of slideshow
- **App Launch Time**: < 2 seconds cold start

### Security Considerations

**SMB Credentials**:
- Store encrypted using Android EncryptedSharedPreferences
- Never log passwords
- Clear from memory after use

**Network Security**:
- Require user to be on local network (no internet requirement)
- Validate SSL certificates if HTTPS APIs added in Phase 2

**Permissions**:
- INTERNET: Required for SMB network access
- WAKE_LOCK: Required for automated wake/sleep
- ACCESS_NETWORK_STATE: Required for network discovery

### Accessibility

**Compose Semantics**:
- All interactive elements have contentDescription
- Semantic roles for buttons, text fields
- Screen reader support for navigation

**Settings UI**:
- Large touch targets (48dp minimum)
- High contrast mode support
- Adjustable text sizes

### Localization

**Initial Release**: English only

**Future Support**:
- String resources in `values/strings.xml`
- All user-facing strings extracted
- Ready for translation to other languages

---

## Conclusion

This modularity-focused architecture provides a solid foundation for the Digital Photo Frame app. The multi-module structure ensures clean separation of concerns, high testability, and extensibility for future phases. While the approach introduces some additional complexity compared to a monolithic structure, the long-term benefits of maintainability, testability, and reusability justify the investment.

I look forward to feedback from Architect 2 (Performance-focused) and Architect 3 (Simplicity-focused) to refine this proposal through collaborative debate.

---

**Status**: Initial Proposal Complete - Ready for Team Review

**Next Steps**:
1. Await proposals from Architect 2 and Architect 3
2. Review and critique teammate proposals
3. Engage in collaborative debate
4. Reach consensus on final architecture
5. Update this document with consensus decisions
