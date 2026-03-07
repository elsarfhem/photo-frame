# Final Architecture - Digital Photo Frame App (MVP Phase 1)

**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Date**: 2026-03-01
**Phase**: Phase 4 - Architecture Synthesis
**Status**: APPROVED
**PRD Reference**: `docs/features/photo-frame-app-initial/requirements/PRD_DRAFT.md`
**Comparison Reference**: `architecture/PROPOSAL_COMPARISON.md`

---

## 1. Executive Summary

This document defines the final, unified architecture for the Digital Photo Frame MVP (Phase 1), synthesized from three architectural proposals focusing on modularity, performance, and simplicity.

### Architecture Philosophy

**"Pragmatic Modularity"** - Build the simplest architecture that meets all requirements while preserving extensibility for Phase 2. Favor proven patterns over experimental optimizations. Profile before optimizing.

### Key Design Principles

1. **Simplicity First**: Use standard Android patterns (MVVM, Repository, Hilt) without over-abstraction
2. **Performance Validation**: Meet NFRs with proven libraries, profile early to identify actual bottlenecks
3. **Strategic Modularity**: Use 2 Gradle modules to balance structure and team velocity
4. **Extensibility via Abstraction**: Repository pattern enables future UseCase layer and cloud sync
5. **Concurrency Safety**: Structured concurrency with coroutines, immutable data models, single mutex

### Primary Patterns

- **MVVM** (Model-View-ViewModel) for UI architecture
- **Repository Pattern** for data abstraction
- **Dependency Injection** (Hilt) for loose coupling
- **StateFlow** for reactive UI updates
- **Coroutines** for structured concurrency

### Success Criteria

This architecture successfully addresses:
- ✅ All 12 user stories from PRD
- ✅ Performance NFRs: <2s photo load, 60fps transitions, <300MB memory
- ✅ Implementable by 2-3 developers in 3-4 months
- ✅ Extensible for Phase 2 (cloud sync, offline mode)
- ✅ 24/7 operational stability with automated scheduling

---

## 2. High-Level Architecture

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         :app Module                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                 Presentation Layer                      │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │ │
│  │  │   Slideshow  │  │   Settings   │  │    System    │ │ │
│  │  │   Screen     │  │   Screen     │  │    Screen    │ │ │
│  │  │  (Compose)   │  │  (Compose)   │  │  (Compose)   │ │ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │ │
│  │         │                  │                  │         │ │
│  │         ▼                  ▼                  ▼         │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │ │
│  │  │  Slideshow   │  │   Settings   │  │    System    │ │ │
│  │  │  ViewModel   │  │  ViewModel   │  │  ViewModel   │ │ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │ │
│  └─────────┼──────────────────┼──────────────────┼────────┘ │
│            │                  │                  │           │
│  ┌─────────┼──────────────────┼──────────────────┼────────┐ │
│  │         │    Dependency Injection (Hilt)      │        │ │
│  └─────────┼──────────────────┼──────────────────┼────────┘ │
└────────────┼──────────────────┼──────────────────┼──────────┘
             │                  │                  │
             ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                         :core Module                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                  Repository Layer                       │ │
│  │  ┌──────────────────┐  ┌──────────────┐  ┌──────────┐ │ │
│  │  │    Slideshow     │  │   Settings   │  │  System  │ │ │
│  │  │   Repository     │  │  Repository  │  │Repository│ │ │
│  │  └────────┬─────────┘  └──────┬───────┘  └────┬─────┘ │ │
│  └───────────┼────────────────────┼─────────────────┼──────┘ │
│              │                    │                 │        │
│  ┌───────────┼────────────────────┼─────────────────┼──────┐ │
│  │           │         Domain Layer (Models)        │      │ │
│  │  ┌────────▼────────┐  ┌────────▼────────┐  ┌───▼────┐ │ │
│  │  │     Photo       │  │    Settings     │  │ System │ │ │
│  │  │     Model       │  │     Model       │  │  Info  │ │ │
│  │  └────────┬────────┘  └────────┬────────┘  └───┬────┘ │ │
│  └───────────┼────────────────────┼─────────────────┼──────┘ │
│              │                    │                 │        │
│  ┌───────────┼────────────────────┼─────────────────┼──────┐ │
│  │           │         Data Layer                   │      │ │
│  │  ┌────────▼──────────────┐  ┌─▼──────────────┐  │      │ │
│  │  │  PhotoBufferManager   │  │ SettingsStore  │  │      │ │
│  │  │  (4-photo LRU buffer) │  │  (DataStore)   │  │      │ │
│  │  └────────┬──────────────┘  └────────────────┘  │      │ │
│  │           │                                      │      │ │
│  │  ┌────────▼──────────────┐  ┌─────────────────┐ │      │ │
│  │  │   SmbPhotoDataSource  │  │  ImageCache     │ │      │ │
│  │  │   (jcifs-ng SMB)      │  │  (Coil + LRU)   │ │      │ │
│  │  └───────────────────────┘  └─────────────────┘ │      │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   SMB Network Share   │
              │   (User's NAS/PC)     │
              └───────────────────────┘
```

### Module Structure

#### `:app` Module (Presentation Layer)
- **Responsibilities**:
  - UI implementation (Jetpack Compose screens)
  - ViewModels (UI state management, user interactions)
  - Dependency injection setup (Hilt modules)
  - Application lifecycle management
  - Navigation between screens

- **Key Packages**:
  ```
  com.photoframe/
  ├── ui/
  │   ├── slideshow/       # Slideshow screen (Compose)
  │   ├── settings/        # Settings screen (Compose)
  │   ├── system/          # System info/debug screen
  │   ├── components/      # Shared UI components
  │   └── theme/           # App theming
  ├── viewmodel/
  │   ├── SlideshowViewModel.kt
  │   ├── SettingsViewModel.kt
  │   └── SystemViewModel.kt
  ├── di/
  │   ├── AppModule.kt
  │   ├── ViewModelModule.kt
  │   └── RepositoryModule.kt
  ├── navigation/
  │   └── Navigation.kt
  └── PhotoFrameApplication.kt
  ```

- **Dependencies**:
  - Depends on: `:core` (repositories, models)
  - External: Jetpack Compose, Hilt, Coil-Compose, WorkManager

---

#### `:core` Module (Business Logic & Data Layer)
- **Responsibilities**:
  - Business logic (photo buffer management, slideshow sequencing)
  - Data access (repositories, data sources)
  - Domain models (Photo, Settings, SlideshowState)
  - Network integration (SMB client, image loading)
  - Caching (image cache, buffer management)

- **Key Packages**:
  ```
  com.photoframe.core/
  ├── data/
  │   ├── repository/
  │   │   ├── SlideshowRepository.kt
  │   │   ├── SlideshowRepositoryImpl.kt
  │   │   ├── SettingsRepository.kt
  │   │   ├── SettingsRepositoryImpl.kt
  │   │   └── SystemRepository.kt
  │   ├── source/
  │   │   ├── SmbPhotoDataSource.kt
  │   │   ├── PhotoBufferManager.kt
  │   │   ├── ImageCache.kt
  │   │   └── SettingsDataSource.kt
  │   └── cache/
  │       ├── PhotoBuffer.kt
  │       └── BufferStrategy.kt
  ├── domain/
  │   └── model/
  │       ├── Photo.kt
  │       ├── SlideshowSettings.kt
  │       ├── SlideshowState.kt
  │       ├── TransitionType.kt
  │       ├── SmbConnection.kt
  │       └── SystemInfo.kt
  ├── network/
  │   ├── SmbClient.kt
  │   ├── SmbAuthenticator.kt
  │   └── SmbScanner.kt
  └── util/
      ├── Result.kt
      ├── Logger.kt
      └── Extensions.kt
  ```

- **Dependencies**:
  - No Android framework dependencies (testable without emulator)
  - External: Kotlin Coroutines, jcifs-ng (SMB), Coil, DataStore

---

### Why 2 Modules?

**Rationale**:
- **Simplicity**: Simpler than 8 modules (Arch 1), avoids excessive Gradle overhead for 2-3 developers
- **Structure**: More structure than 1 module (Arch 3), separates UI from business logic
- **Testability**: `:core` module is framework-agnostic, testable without Android emulator
- **Performance**: Network/data layer in `:core` can be profiled and optimized independently
- **Extensibility**: Easy to add more modules in Phase 2 if needed (e.g., `:cloud` module)

**Compromise**:
- Architects 1, 2, 3 proposed 8, 3-4, 1 modules respectively
- 2 modules is the pragmatic middle ground

---

## 3. Component Design

### 3.1 Presentation Layer Components

#### Component: `SlideshowScreen` (Compose UI)

**Responsibility**: Display current photo with transitions, handle user gestures (swipe, tap)

**Interface**:
```kotlin
@Composable
fun SlideshowScreen(
    viewModel: SlideshowViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is SlideshowUiState.Loading -> LoadingIndicator()
            is SlideshowUiState.Playing -> {
                PhotoDisplay(
                    photo = state.currentPhoto,
                    transitionType = state.transitionType,
                    onPhotoClick = { viewModel.toggleControls() },
                    onSwipeLeft = { viewModel.nextPhoto() },
                    onSwipeRight = { viewModel.previousPhoto() }
                )
            }
            is SlideshowUiState.Error -> ErrorDisplay(state.message)
        }

        if (uiState is SlideshowUiState.Playing && uiState.showControls) {
            SlideshowControls(
                onPause = { viewModel.pauseSlideshow() },
                onNext = { viewModel.nextPhoto() },
                onSettings = onNavigateToSettings
            )
        }
    }
}
```

**Implementation Notes**:
- Use `Modifier.pointerInput()` for swipe gestures
- Use `AnimatedContent()` for crossfade transitions
- Show controls on tap, auto-hide after 3 seconds
- Full-screen immersive mode (hide status/nav bars)

---

#### Component: `SlideshowViewModel`

**Responsibility**: Manage UI state, handle user actions, coordinate slideshow playback

**Interface**:
```kotlin
@HiltViewModel
class SlideshowViewModel @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SlideshowUiState>(SlideshowUiState.Loading)
    val uiState: StateFlow<SlideshowUiState> = _uiState.asStateFlow()

    init {
        startSlideshow()
    }

    fun startSlideshow() {
        viewModelScope.launch {
            slideshowRepository.currentPhoto
                .combine(settingsRepository.settings) { photo, settings ->
                    SlideshowUiState.Playing(
                        currentPhoto = photo,
                        transitionType = settings.transitionType,
                        showControls = false
                    )
                }
                .catch { error -> _uiState.value = SlideshowUiState.Error(error.message) }
                .collect { _uiState.value = it }
        }

        viewModelScope.launch {
            while (true) {
                delay(settingsRepository.getIntervalMillis())
                nextPhoto()
            }
        }
    }

    fun nextPhoto() {
        viewModelScope.launch {
            slideshowRepository.nextPhoto()
        }
    }

    fun previousPhoto() {
        viewModelScope.launch {
            slideshowRepository.previousPhoto()
        }
    }

    fun pauseSlideshow() {
        // Cancel auto-advance coroutine
    }

    fun toggleControls() {
        // Show/hide controls overlay
    }
}

sealed interface SlideshowUiState {
    object Loading : SlideshowUiState
    data class Playing(
        val currentPhoto: Photo,
        val transitionType: TransitionType,
        val showControls: Boolean
    ) : SlideshowUiState
    data class Error(val message: String) : SlideshowUiState
}
```

**Dependencies**:
- `SlideshowRepository` (injected)
- `SettingsRepository` (injected)

**Implementation Notes**:
- Use `viewModelScope` for coroutines (auto-cancelled on ViewModel clear)
- Use `StateFlow.combine()` to merge photo + settings streams
- Auto-advance using `delay()` + `while(true)` loop (cancellable)
- Handle errors gracefully (show error UI, allow retry)

---

#### Component: `SettingsScreen` (Compose UI)

**Responsibility**: Configure slideshow settings, manage SMB connections, test connectivity

**Interface**:
```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp)) {
        // SMB Connection Section
        SmbConnectionCard(
            connection = settings.smbConnection,
            onTest = { viewModel.testConnection() },
            onSave = { newConnection -> viewModel.saveConnection(newConnection) }
        )

        // Slideshow Settings Section
        SlideshowSettingsCard(
            interval = settings.displayInterval,
            transitionType = settings.transitionType,
            shuffle = settings.shuffle,
            onIntervalChange = { viewModel.updateInterval(it) },
            onTransitionChange = { viewModel.updateTransition(it) },
            onShuffleChange = { viewModel.updateShuffle(it) }
        )

        // Schedule Section
        ScheduleCard(
            schedule = settings.schedule,
            onScheduleChange = { viewModel.updateSchedule(it) }
        )
    }
}
```

**Implementation Notes**:
- Use `OutlinedTextField` for SMB URL, username, password
- Show password field with visibility toggle
- Test connection button with loading indicator
- Display connection status (success/error message)
- Save settings to DataStore on change (debounced)

---

#### Component: `SettingsViewModel`

**Responsibility**: Manage settings UI state, validate SMB connections, persist settings

**Interface**:
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val slideshowRepository: SlideshowRepository
) : ViewModel() {

    val settings: StateFlow<SlideshowSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SlideshowSettings.default())

    fun testConnection() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Testing
            val result = slideshowRepository.testSmbConnection()
            _connectionStatus.value = when (result) {
                is Result.Success -> ConnectionStatus.Success(result.data)
                is Result.Error -> ConnectionStatus.Error(result.exception.message)
            }
        }
    }

    fun saveConnection(connection: SmbConnection) {
        viewModelScope.launch {
            settingsRepository.updateSmbConnection(connection)
        }
    }

    fun updateInterval(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.updateDisplayInterval(seconds)
        }
    }

    fun updateTransition(type: TransitionType) {
        viewModelScope.launch {
            settingsRepository.updateTransitionType(type)
        }
    }

    fun updateShuffle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShuffle(enabled)
        }
    }

    fun updateSchedule(schedule: SlideshowSchedule) {
        viewModelScope.launch {
            settingsRepository.updateSchedule(schedule)
            // Update WorkManager schedule
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(...)
        }
    }
}
```

**Dependencies**:
- `SettingsRepository` (injected)
- `SlideshowRepository` (for connection testing)

**Implementation Notes**:
- Settings automatically saved to DataStore on update
- Connection test runs in background, shows progress/result
- Schedule changes update WorkManager immediately
- Validation: URL format, non-empty fields, reachable host

---

### 3.2 Repository Layer Components

#### Component: `SlideshowRepository` (Interface)

**Responsibility**: Abstract slideshow business logic and photo data access

**Interface**:
```kotlin
interface SlideshowRepository {
    val currentPhoto: StateFlow<Photo?>
    val bufferState: StateFlow<BufferState>

    suspend fun startSlideshow(): Result<Unit>
    suspend fun stopSlideshow(): Result<Unit>
    suspend fun nextPhoto(): Result<Photo>
    suspend fun previousPhoto(): Result<Photo>
    suspend fun refreshPhotoList(): Result<Int>
    suspend fun testSmbConnection(): Result<Int>
}

data class BufferState(
    val totalPhotos: Int,
    val bufferedPhotos: Int,
    val currentIndex: Int
)
```

**Consumers**: `SlideshowViewModel`, `SettingsViewModel`

**Implementations**: `SlideshowRepositoryImpl`

---

#### Component: `SlideshowRepositoryImpl`

**Responsibility**: Implement slideshow logic, manage photo buffer, coordinate data sources

**Implementation**:
```kotlin
@Singleton
class SlideshowRepositoryImpl @Inject constructor(
    private val photoDataSource: SmbPhotoDataSource,
    private val bufferManager: PhotoBufferManager,
    private val imageCache: ImageCache,
    private val settingsRepository: SettingsRepository
) : SlideshowRepository {

    private val _currentPhoto = MutableStateFlow<Photo?>(null)
    override val currentPhoto: StateFlow<Photo?> = _currentPhoto.asStateFlow()

    private val _bufferState = MutableStateFlow(BufferState(0, 0, 0))
    override val bufferState: StateFlow<BufferState> = _bufferState.asStateFlow()

    private var photoList: List<Photo> = emptyList()
    private var currentIndex: Int = 0

    override suspend fun startSlideshow(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Scan SMB share for photos
            val settings = settingsRepository.getSettings()
            photoList = photoDataSource.scanPhotos(settings.smbConnection)

            // 2. Shuffle if enabled
            if (settings.shuffle) {
                photoList = photoList.shuffled()
            }

            // 3. Initialize buffer with first 4 photos
            val initialPhotos = photoList.take(4)
            bufferManager.initializeBuffer(initialPhotos)

            // 4. Preload first 4 photos into cache
            initialPhotos.forEach { photo ->
                imageCache.preload(photo.path)
            }

            // 5. Set current photo to first photo
            _currentPhoto.value = photoList.firstOrNull()
            currentIndex = 0

            _bufferState.value = BufferState(
                totalPhotos = photoList.size,
                bufferedPhotos = initialPhotos.size,
                currentIndex = 0
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun nextPhoto(): Result<Photo> = withContext(Dispatchers.IO) {
        try {
            // 1. Move to next index (circular)
            currentIndex = (currentIndex + 1) % photoList.size
            val nextPhoto = photoList[currentIndex]

            // 2. Update buffer (evict old, add new)
            val futureIndex = (currentIndex + 3) % photoList.size
            val futurePhoto = photoList[futureIndex]
            bufferManager.updateBuffer(currentIndex, futurePhoto)

            // 3. Preload future photo
            imageCache.preload(futurePhoto.path)

            // 4. Update current photo
            _currentPhoto.value = nextPhoto

            _bufferState.value = _bufferState.value.copy(currentIndex = currentIndex)

            Result.Success(nextPhoto)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun previousPhoto(): Result<Photo> = withContext(Dispatchers.IO) {
        try {
            // Move to previous index (circular)
            currentIndex = if (currentIndex == 0) photoList.size - 1 else currentIndex - 1
            val prevPhoto = photoList[currentIndex]

            _currentPhoto.value = prevPhoto
            _bufferState.value = _bufferState.value.copy(currentIndex = currentIndex)

            Result.Success(prevPhoto)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshPhotoList(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.getSettings()
            val newPhotos = photoDataSource.scanPhotos(settings.smbConnection)
            photoList = if (settings.shuffle) newPhotos.shuffled() else newPhotos

            Result.Success(photoList.size)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun testSmbConnection(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.getSettings()
            val photoCount = photoDataSource.testConnection(settings.smbConnection)
            Result.Success(photoCount)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun stopSlideshow(): Result<Unit> {
        _currentPhoto.value = null
        currentIndex = 0
        photoList = emptyList()
        bufferManager.clear()
        return Result.Success(Unit)
    }
}
```

**Dependencies**:
- `SmbPhotoDataSource` (photo scanning/loading)
- `PhotoBufferManager` (4-photo buffer management)
- `ImageCache` (preloading)
- `SettingsRepository` (slideshow settings)

**Implementation Notes**:
- All operations run on `Dispatchers.IO` (network/disk I/O)
- Circular navigation (index wraps around at end of list)
- Buffer updated on each nextPhoto() call (evict old, add future photo)
- Errors propagated as `Result.Error` (handled by ViewModel)

---

#### Component: `SettingsRepository` (Interface)

**Responsibility**: Abstract settings persistence and retrieval

**Interface**:
```kotlin
interface SettingsRepository {
    val settings: Flow<SlideshowSettings>

    suspend fun getSettings(): SlideshowSettings
    suspend fun updateSmbConnection(connection: SmbConnection)
    suspend fun updateDisplayInterval(seconds: Int)
    suspend fun updateTransitionType(type: TransitionType)
    suspend fun updateShuffle(enabled: Boolean)
    suspend fun updateSchedule(schedule: SlideshowSchedule)
}
```

---

#### Component: `SettingsRepositoryImpl`

**Responsibility**: Persist settings to DataStore, expose reactive settings stream

**Implementation**:
```kotlin
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settings: Flow<SlideshowSettings> = dataStore.data
        .map { preferences ->
            SlideshowSettings(
                smbConnection = SmbConnection(
                    url = preferences[SMB_URL] ?: "",
                    username = preferences[SMB_USERNAME] ?: "",
                    password = preferences[SMB_PASSWORD] ?: "",
                    domain = preferences[SMB_DOMAIN] ?: ""
                ),
                displayInterval = preferences[DISPLAY_INTERVAL] ?: 5,
                transitionType = TransitionType.valueOf(
                    preferences[TRANSITION_TYPE] ?: TransitionType.CROSSFADE.name
                ),
                shuffle = preferences[SHUFFLE] ?: false,
                schedule = SlideshowSchedule(
                    enabled = preferences[SCHEDULE_ENABLED] ?: false,
                    startTime = preferences[SCHEDULE_START] ?: "08:00",
                    endTime = preferences[SCHEDULE_END] ?: "22:00"
                )
            )
        }

    override suspend fun getSettings(): SlideshowSettings {
        return settings.first()
    }

    override suspend fun updateSmbConnection(connection: SmbConnection) {
        dataStore.edit { preferences ->
            preferences[SMB_URL] = connection.url
            preferences[SMB_USERNAME] = connection.username
            preferences[SMB_PASSWORD] = connection.password
            preferences[SMB_DOMAIN] = connection.domain
        }
    }

    override suspend fun updateDisplayInterval(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[DISPLAY_INTERVAL] = seconds
        }
    }

    override suspend fun updateTransitionType(type: TransitionType) {
        dataStore.edit { preferences ->
            preferences[TRANSITION_TYPE] = type.name
        }
    }

    override suspend fun updateShuffle(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHUFFLE] = enabled
        }
    }

    override suspend fun updateSchedule(schedule: SlideshowSchedule) {
        dataStore.edit { preferences ->
            preferences[SCHEDULE_ENABLED] = schedule.enabled
            preferences[SCHEDULE_START] = schedule.startTime
            preferences[SCHEDULE_END] = schedule.endTime
        }
    }

    companion object {
        private val SMB_URL = stringPreferencesKey("smb_url")
        private val SMB_USERNAME = stringPreferencesKey("smb_username")
        private val SMB_PASSWORD = stringPreferencesKey("smb_password")
        private val SMB_DOMAIN = stringPreferencesKey("smb_domain")
        private val DISPLAY_INTERVAL = intPreferencesKey("display_interval")
        private val TRANSITION_TYPE = stringPreferencesKey("transition_type")
        private val SHUFFLE = booleanPreferencesKey("shuffle")
        private val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        private val SCHEDULE_START = stringPreferencesKey("schedule_start")
        private val SCHEDULE_END = stringPreferencesKey("schedule_end")
    }
}
```

**Dependencies**:
- `DataStore<Preferences>` (injected)

**Implementation Notes**:
- DataStore provides type-safe, async key-value storage
- Settings exposed as `Flow` for reactive updates
- All updates are suspend functions (async I/O)
- Password stored in DataStore (consider EncryptedSharedPreferences for production)

---

### 3.3 Data Layer Components

#### Component: `PhotoBufferManager`

**Responsibility**: Manage 4-photo buffer (circular LRU), thread-safe buffer updates

**Implementation**:
```kotlin
@Singleton
class PhotoBufferManager @Inject constructor() {

    private val mutex = Mutex()
    private val bufferSize = 4
    private val buffer = ArrayDeque<Photo>(bufferSize)

    suspend fun initializeBuffer(photos: List<Photo>) = mutex.withLock {
        buffer.clear()
        photos.take(bufferSize).forEach { buffer.addLast(it) }
    }

    suspend fun updateBuffer(currentIndex: Int, newPhoto: Photo) = mutex.withLock {
        // Evict oldest photo (first in buffer)
        if (buffer.size >= bufferSize) {
            buffer.removeFirst()
        }

        // Add new photo to end
        buffer.addLast(newPhoto)
    }

    suspend fun getPhoto(index: Int): Photo? = mutex.withLock {
        buffer.getOrNull(index)
    }

    suspend fun clear() = mutex.withLock {
        buffer.clear()
    }

    suspend fun currentBufferSize(): Int = mutex.withLock {
        buffer.size
    }
}
```

**Dependencies**: None

**Implementation Notes**:
- Uses `ArrayDeque` for efficient add/remove from both ends
- Single `Mutex` protects all buffer operations (thread-safe)
- Buffer size fixed at 4 photos (configurable via constructor)
- FIFO eviction (oldest photo removed first)

**Thread Safety**:
- All public methods use `mutex.withLock` to prevent concurrent modification
- Structured concurrency ensures operations are serialized

---

#### Component: `ImageCache`

**Responsibility**: In-memory image cache with LRU eviction, integrates with Coil

**Implementation**:
```kotlin
@Singleton
class ImageCache @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val maxCacheSize = 100 * 1024 * 1024 // 100MB
    private val memoryCache = LruCache<String, Bitmap>(maxCacheSize)

    private val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizeBytes(maxCacheSize)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(512 * 1024 * 1024) // 512MB disk cache
                .build()
        }
        .build()

    suspend fun preload(photoPath: String) {
        val request = ImageRequest.Builder(context)
            .data(photoPath)
            .size(2560, 1600) // Downsample to screen resolution
            .build()

        imageLoader.execute(request)
    }

    suspend fun loadBitmap(photoPath: String): Bitmap? {
        // Check memory cache first
        memoryCache.get(photoPath)?.let { return it }

        // Load from disk/network via Coil
        val request = ImageRequest.Builder(context)
            .data(photoPath)
            .size(2560, 1600)
            .build()

        val result = imageLoader.execute(request)
        return (result.drawable as? BitmapDrawable)?.bitmap?.also {
            memoryCache.put(photoPath, it)
        }
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    fun clearDiskCache() {
        imageLoader.diskCache?.clear()
    }
}
```

**Dependencies**:
- `Context` (injected)
- Coil `ImageLoader`

**Implementation Notes**:
- **Memory cache**: 100MB LRU (holds ~6-7 full-resolution photos)
- **Disk cache**: 512MB (Coil's disk cache, holds ~30-40 photos)
- **Downsampling**: All photos downsampled to 2560x1600 (max tablet resolution)
- **Preloading**: `preload()` fetches image into cache without returning bitmap
- **Thread-safe**: Coil's ImageLoader handles concurrency internally

**Why Coil**:
- Battle-tested, used by thousands of Android apps
- Efficient memory management (automatic downsampling, LRU eviction)
- Coroutine-native (suspend functions)
- Supports custom Fetchers (can add SMB Fetcher if needed)

---

#### Component: `SmbPhotoDataSource`

**Responsibility**: Scan SMB shares for photos, load photo metadata, test connectivity

**Implementation**:
```kotlin
@Singleton
class SmbPhotoDataSource @Inject constructor(
    private val smbClient: SmbClient
) {

    private val supportedExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")

    suspend fun scanPhotos(connection: SmbConnection): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()

        try {
            val smbFile = smbClient.connect(connection)
            scanDirectory(smbFile, photos)
        } catch (e: Exception) {
            throw PhotoScanException("Failed to scan SMB share: ${e.message}", e)
        }

        photos.sortedBy { it.path }
    }

    private fun scanDirectory(directory: SmbFile, photos: MutableList<Photo>) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanDirectory(file, photos) // Recursive scan
                file.isFile && file.name.hasImageExtension() -> {
                    photos.add(Photo(
                        path = file.path,
                        filename = file.name,
                        size = file.length(),
                        lastModified = file.lastModified()
                    ))
                }
            }
        }
    }

    suspend fun testConnection(connection: SmbConnection): Int = withContext(Dispatchers.IO) {
        try {
            val smbFile = smbClient.connect(connection)
            val photoCount = countPhotos(smbFile)
            photoCount
        } catch (e: Exception) {
            throw ConnectionTestException("Connection test failed: ${e.message}", e)
        }
    }

    private fun countPhotos(directory: SmbFile): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> count += countPhotos(file)
                file.isFile && file.name.hasImageExtension() -> count++
            }
        }
        return count
    }

    suspend fun loadPhotoBytes(path: String, connection: SmbConnection): ByteArray = withContext(Dispatchers.IO) {
        try {
            val smbFile = SmbFile(path, smbClient.getAuthContext(connection))
            smbFile.inputStream().use { it.readBytes() }
        } catch (e: Exception) {
            throw PhotoLoadException("Failed to load photo: ${e.message}", e)
        }
    }

    private fun String.hasImageExtension(): Boolean {
        return supportedExtensions.any { this.endsWith(it, ignoreCase = true) }
    }
}

data class Photo(
    val path: String,
    val filename: String,
    val size: Long,
    val lastModified: Long
)

class PhotoScanException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ConnectionTestException(message: String, cause: Throwable? = null) : Exception(message, cause)
class PhotoLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

**Dependencies**:
- `SmbClient` (injected, wraps jcifs-ng)

**Implementation Notes**:
- **Recursive directory scan**: Scans all subdirectories for photos
- **File filtering**: Only includes files with image extensions (.jpg, .png, etc.)
- **Metadata extraction**: Filename, size, last modified timestamp
- **Error handling**: Custom exceptions for scan/load failures
- **Sorting**: Photos sorted alphabetically by path (user can shuffle)

**Performance Considerations**:
- SMB scanning can be slow for large shares (thousands of photos)
- Future optimization: Cache photo list, only rescan on demand
- Future optimization: Parallel directory scanning (multiple coroutines)

---

#### Component: `SmbClient`

**Responsibility**: Manage SMB connections using jcifs-ng, handle authentication

**Implementation**:
```kotlin
@Singleton
class SmbClient @Inject constructor() {

    fun connect(connection: SmbConnection): SmbFile {
        val authContext = getAuthContext(connection)
        val smbFile = SmbFile(connection.url, authContext)

        // Verify connection
        if (!smbFile.exists()) {
            throw SmbException("SMB path does not exist: ${connection.url}")
        }

        return smbFile
    }

    fun getAuthContext(connection: SmbConnection): CIFSContext {
        val credentials = NtlmPasswordAuthenticator(
            connection.domain,
            connection.username,
            connection.password
        )

        val config = PropertyConfiguration(Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202") // SMB 2.0.2 minimum
            setProperty("jcifs.smb.client.maxVersion", "SMB311") // SMB 3.1.1 maximum
            setProperty("jcifs.smb.client.responseTimeout", "30000") // 30s timeout
        })

        return SingletonContext.getInstance().withCredentials(credentials).withConfig(config)
    }
}

data class SmbConnection(
    val url: String,      // e.g., "smb://192.168.1.100/photos/"
    val username: String, // e.g., "john"
    val password: String, // e.g., "secret"
    val domain: String    // e.g., "WORKGROUP" or ""
)

class SmbException(message: String) : Exception(message)
```

**Dependencies**:
- jcifs-ng library

**Implementation Notes**:
- **Authentication**: NTLM authentication with username/password/domain
- **SMB versions**: Supports SMB 2.0.2 to SMB 3.1.1 (modern protocols)
- **Timeout**: 30-second response timeout (configurable)
- **Connection reuse**: jcifs-ng automatically reuses connections for same host
- **Error handling**: Throws `SmbException` on connection failure

**Security Note**:
- Password stored in DataStore (plaintext) for MVP
- **Phase 2**: Migrate to EncryptedSharedPreferences or Android Keystore

---

### 3.4 Domain Models

#### Model: `Photo`

```kotlin
@Immutable
data class Photo(
    val path: String,        // Full SMB path: "smb://server/share/folder/photo.jpg"
    val filename: String,    // "photo.jpg"
    val size: Long,          // File size in bytes
    val lastModified: Long   // Timestamp (milliseconds since epoch)
)
```

**Immutability**: `@Immutable` annotation ensures thread safety (no accidental mutations)

---

#### Model: `SlideshowSettings`

```kotlin
@Immutable
data class SlideshowSettings(
    val smbConnection: SmbConnection,
    val displayInterval: Int,           // Seconds per photo (default: 5)
    val transitionType: TransitionType, // Crossfade, slide, etc.
    val shuffle: Boolean,                // Randomize photo order
    val schedule: SlideshowSchedule     // Automated start/stop times
) {
    companion object {
        fun default() = SlideshowSettings(
            smbConnection = SmbConnection("", "", "", ""),
            displayInterval = 5,
            transitionType = TransitionType.CROSSFADE,
            shuffle = false,
            schedule = SlideshowSchedule(false, "08:00", "22:00")
        )
    }
}
```

---

#### Model: `TransitionType`

```kotlin
enum class TransitionType {
    CROSSFADE,  // Fade from current to next
    SLIDE_LEFT, // Slide left (next enters from right)
    SLIDE_RIGHT,// Slide right (next enters from left)
    ZOOM,       // Zoom in on next photo
    NONE        // Instant cut (no transition)
}
```

---

#### Model: `SlideshowSchedule`

```kotlin
@Immutable
data class SlideshowSchedule(
    val enabled: Boolean,
    val startTime: String,  // "HH:mm" format (e.g., "08:00")
    val endTime: String     // "HH:mm" format (e.g., "22:00")
)
```

---

#### Model: `SmbConnection`

```kotlin
@Immutable
data class SmbConnection(
    val url: String,      // "smb://192.168.1.100/photos/"
    val username: String,
    val password: String,
    val domain: String    // "WORKGROUP" or ""
)
```

---

### 3.5 Utility Components

#### Component: `Result` (Sealed Class)

**Responsibility**: Type-safe error handling (replaces exceptions in return types)

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
}
```

**Usage**:
```kotlin
val result = slideshowRepository.nextPhoto()
result.onSuccess { photo ->
    println("Loaded photo: ${photo.filename}")
}.onError { error ->
    println("Error: ${error.message}")
}
```

---

## 4. Data Flow

### Flow 1: Slideshow Initialization

```
User opens app
    ↓
SlideshowScreen rendered
    ↓
SlideshowViewModel.init()
    ↓
SlideshowRepository.startSlideshow()
    ↓
1. Load settings from SettingsRepository
2. SmbPhotoDataSource.scanPhotos() → List<Photo>
3. Shuffle photos if enabled
4. PhotoBufferManager.initializeBuffer() (4 photos)
5. ImageCache.preload() for each buffered photo
6. Set currentPhoto to first photo
    ↓
currentPhoto StateFlow emits Photo
    ↓
ViewModel.uiState updates to Playing(photo)
    ↓
SlideshowScreen recomposes with photo
    ↓
Coil loads Bitmap from cache/disk/network
    ↓
Photo displayed on screen
```

**Timing**: <2s from app open to first photo display (NFR)

---

### Flow 2: Auto-Advance to Next Photo

```
Timer triggers (every N seconds)
    ↓
SlideshowViewModel.nextPhoto()
    ↓
SlideshowRepository.nextPhoto()
    ↓
1. Increment currentIndex (circular)
2. Get next photo from photoList
3. PhotoBufferManager.updateBuffer() (evict old, add future photo)
4. ImageCache.preload(futurePhoto) in background
5. Update currentPhoto StateFlow
    ↓
currentPhoto StateFlow emits new Photo
    ↓
ViewModel.uiState updates (triggers recompose)
    ↓
SlideshowScreen crossfade animation
    ↓
New photo displayed (60fps transition)
```

**Timing**: <100ms transition time (NFR: 60fps)

---

### Flow 3: User Swipe Gesture (Next Photo)

```
User swipes left on SlideshowScreen
    ↓
Compose Modifier.pointerInput() detects swipe
    ↓
SlideshowViewModel.nextPhoto() called
    ↓
(Same as Flow 2 - Auto-Advance)
```

---

### Flow 4: Settings Update

```
User changes display interval in SettingsScreen
    ↓
SettingsViewModel.updateInterval(newValue)
    ↓
SettingsRepository.updateDisplayInterval()
    ↓
DataStore.edit { preferences[INTERVAL] = newValue }
    ↓
DataStore emits updated Preferences
    ↓
SettingsRepository.settings Flow emits new SlideshowSettings
    ↓
SlideshowViewModel collects new settings
    ↓
Auto-advance timer updated with new interval
```

**Timing**: <100ms from UI change to settings persisted

---

### Flow 5: Scheduled Start/Stop

```
WorkManager triggers at scheduled time (e.g., 08:00)
    ↓
SlideshowWorker.doWork()
    ↓
Check schedule.enabled && current time == startTime
    ↓
If true: Start slideshow activity
    ↓
SlideshowViewModel.startSlideshow()
    ↓
(Same as Flow 1 - Slideshow Initialization)
```

```
WorkManager triggers at scheduled time (e.g., 22:00)
    ↓
SlideshowWorker.doWork()
    ↓
Check schedule.enabled && current time == endTime
    ↓
If true: Finish slideshow activity
    ↓
SlideshowRepository.stopSlideshow()
    ↓
Clear buffer, reset state
```

---

## 5. Module Impact

### Module: `:app` (New Module)

**New Files to Create**:

**UI Layer** (15-20 files):
- `ui/slideshow/SlideshowScreen.kt` - Main slideshow Compose UI
- `ui/slideshow/PhotoDisplay.kt` - Photo rendering with transitions
- `ui/slideshow/SlideshowControls.kt` - Overlay controls (pause, next, settings)
- `ui/settings/SettingsScreen.kt` - Settings Compose UI
- `ui/settings/SmbConnectionCard.kt` - SMB connection form
- `ui/settings/SlideshowSettingsCard.kt` - Interval, transition, shuffle
- `ui/settings/ScheduleCard.kt` - Automated schedule configuration
- `ui/system/SystemScreen.kt` - Debug/system info screen
- `ui/components/LoadingIndicator.kt` - Loading spinner
- `ui/components/ErrorDisplay.kt` - Error message UI
- `ui/theme/Theme.kt` - Material 3 theming
- `ui/theme/Color.kt` - Color palette
- `ui/theme/Type.kt` - Typography

**ViewModel Layer** (3-4 files):
- `viewmodel/SlideshowViewModel.kt` - Slideshow state management
- `viewmodel/SettingsViewModel.kt` - Settings state management
- `viewmodel/SystemViewModel.kt` - System info state

**DI Layer** (3-4 files):
- `di/AppModule.kt` - Application-level dependencies
- `di/ViewModelModule.kt` - ViewModel factory setup
- `di/RepositoryModule.kt` - Repository bindings

**Navigation** (1 file):
- `navigation/Navigation.kt` - Compose navigation graph

**Application** (1 file):
- `PhotoFrameApplication.kt` - Application class (Hilt setup)

**Manifest** (1 file):
- `AndroidManifest.xml` - App configuration, permissions

**Dependencies to Add** (`app/build.gradle.kts`):
```kotlin
dependencies {
    implementation(project(":core"))

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coil (Compose integration)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
}
```

**Risk Level**: ✅ Low
**Rationale**: New module, no existing code to break. Standard Android patterns.

---

### Module: `:core` (New Module)

**New Files to Create**:

**Repository Layer** (6-8 files):
- `data/repository/SlideshowRepository.kt` - Interface
- `data/repository/SlideshowRepositoryImpl.kt` - Implementation
- `data/repository/SettingsRepository.kt` - Interface
- `data/repository/SettingsRepositoryImpl.kt` - Implementation
- `data/repository/SystemRepository.kt` - System info (battery, storage)

**Data Source Layer** (6-8 files):
- `data/source/SmbPhotoDataSource.kt` - SMB photo scanning/loading
- `data/source/PhotoBufferManager.kt` - 4-photo buffer management
- `data/source/ImageCache.kt` - In-memory + Coil cache integration
- `data/source/SettingsDataSource.kt` - DataStore wrapper

**Domain Models** (6-8 files):
- `domain/model/Photo.kt` - Photo data class
- `domain/model/SlideshowSettings.kt` - Settings data class
- `domain/model/SmbConnection.kt` - SMB connection data class
- `domain/model/TransitionType.kt` - Enum for transition types
- `domain/model/SlideshowSchedule.kt` - Schedule data class
- `domain/model/SlideshowState.kt` - Current state enum

**Network Layer** (3-4 files):
- `network/SmbClient.kt` - jcifs-ng wrapper
- `network/SmbAuthenticator.kt` - Authentication helper
- `network/SmbScanner.kt` - Directory scanning utility

**Utilities** (3-4 files):
- `util/Result.kt` - Sealed class for error handling
- `util/Logger.kt` - Logging helper
- `util/Extensions.kt` - Kotlin extensions

**Dependencies to Add** (`core/build.gradle.kts`):
```kotlin
dependencies {
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // SMB Client
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.9")

    // Coil (image loading)
    implementation("io.coil-kt:coil:2.5.0")

    // DataStore (settings persistence)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
}
```

**Risk Level**: ⚙️ Medium
**Rationale**: Complex business logic (buffer management, SMB scanning). Requires performance testing.

---

## 6. Integration Points

### SMB Network Share Integration

**Protocol**: SMB 2.0.2 to SMB 3.1.1
**Library**: jcifs-ng 2.1.9
**Authentication**: NTLM (username/password/domain)

**Configuration**:
```kotlin
Properties().apply {
    setProperty("jcifs.smb.client.minVersion", "SMB202")
    setProperty("jcifs.smb.client.maxVersion", "SMB311")
    setProperty("jcifs.smb.client.responseTimeout", "30000") // 30s
    setProperty("jcifs.smb.client.soTimeout", "30000")       // 30s
}
```

**Endpoint Format**: `smb://hostname-or-ip/share/folder/`
- Example: `smb://192.168.1.100/photos/`
- Example: `smb://nas.local/media/photos/`

**Error Handling**:
- **Connection timeout**: Show user-friendly error, suggest checking network/credentials
- **Access denied**: Show error, suggest checking username/password
- **Path not found**: Show error, suggest verifying SMB path
- **Network unreachable**: Show error, suggest checking Wi-Fi connection

**Retry Logic**: No automatic retry for MVP (user manually retries)

**Timeout**: 30 seconds for both connection and read operations

**Security**:
- NTLM authentication (username/password)
- No encryption for MVP (SMB 3.0 encryption deferred to Phase 2)
- Password stored in DataStore (plaintext for MVP, encrypt in Phase 2)

---

### Coil Image Loading Integration

**Library**: Coil 2.5.0
**Configuration**:
```kotlin
ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizeBytes(100 * 1024 * 1024) // 100MB
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(512 * 1024 * 1024) // 512MB
            .build()
    }
    .build()
```

**Custom SMB Fetcher** (if needed in future):
```kotlin
class SmbFetcher(
    private val data: String,
    private val options: Options,
    private val smbClient: SmbClient
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bytes = smbClient.loadPhoto(data)
        return SourceResult(
            source = Buffer().write(bytes).asSource(),
            mimeType = "image/jpeg",
            dataSource = DataSource.NETWORK
        )
    }
}
```

**Image Formats Supported**: JPEG, PNG, GIF, BMP, WebP
**Downsampling**: All images downsampled to 2560x1600 (max tablet resolution)
**Caching**: Memory (100MB) + Disk (512MB)

---

### WorkManager Scheduling Integration

**Library**: WorkManager 2.9.0
**Use Case**: Automated slideshow start/stop based on schedule

**Configuration**:
```kotlin
val startRequest = PeriodicWorkRequestBuilder<SlideshowWorker>(15, TimeUnit.MINUTES)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "slideshow_schedule",
        ExistingPeriodicWorkPolicy.REPLACE,
        startRequest
    )
```

**Worker Implementation**:
```kotlin
class SlideshowWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.getSettings()
        val currentTime = LocalTime.now()
        val startTime = LocalTime.parse(settings.schedule.startTime)
        val endTime = LocalTime.parse(settings.schedule.endTime)

        return when {
            !settings.schedule.enabled -> Result.success()
            currentTime >= startTime && currentTime < endTime -> {
                // Start slideshow
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                applicationContext.startActivity(intent)
                Result.success()
            }
            currentTime >= endTime -> {
                // Stop slideshow (finish activity)
                // Send broadcast to finish activity
                Result.success()
            }
            else -> Result.success()
        }
    }
}
```

**Constraints**: Requires network connectivity (to access SMB share)

---

### DataStore Settings Persistence

**Library**: DataStore Preferences 1.0.0
**Use Case**: Persist user settings (SMB connection, display interval, etc.)

**Configuration**:
```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
```

**Keys**:
- `smb_url`: String (SMB share URL)
- `smb_username`: String
- `smb_password`: String (plaintext for MVP, encrypt Phase 2)
- `smb_domain`: String
- `display_interval`: Int (seconds per photo)
- `transition_type`: String (enum name)
- `shuffle`: Boolean
- `schedule_enabled`: Boolean
- `schedule_start`: String (HH:mm format)
- `schedule_end`: String (HH:mm format)

**Migration**: No migration needed for MVP (greenfield)

---

## 7. Non-Functional Requirements Addressed

### Performance (Target: <2s load, 60fps, <300MB memory)

| NFR | Target | Strategy | Validation |
|-----|--------|----------|------------|
| **Photo Load Time** | <2s from tap to display | 4-photo buffer (preload), Coil cache, image downsampling | Performance profiler, measure time from nextPhoto() to onDraw() |
| **Transition Smoothness** | 60fps (16ms per frame) | GPU-accelerated Compose animations, preloaded bitmaps | GPU profiler, frame timing analysis |
| **Memory Usage** | <300MB total | Image downsampling (2560x1600), 100MB memory cache, 4-photo buffer (~60MB) | Memory profiler, track heap allocations |
| **Startup Time** | <3s from cold start to first photo | Lazy initialization, background preloading | App startup profiler |
| **SMB Scan Time** | <5s for 1000 photos | Parallel directory scanning (future optimization) | Profiler, measure scanPhotos() time |

**Downsampling Math**:
- Full resolution: 4000x3000 = 12MP = ~48MB (ARGB_8888)
- Downsampled: 2560x1600 = 4.1MP = ~16MB (ARGB_8888)
- Reduction: 3x smaller in memory

**Buffer Memory**:
- 4 photos × 16MB = ~64MB (worst case)
- Memory cache: 100MB (holds ~6-7 photos)
- Total: ~164MB (well under 300MB limit)

---

### Security

| NFR | Strategy | Risk Level |
|-----|----------|------------|
| **SMB Password Storage** | DataStore (plaintext for MVP) | ⚠️ Medium - Phase 2: EncryptedSharedPreferences |
| **Network Encryption** | SMB 3.0 encryption (if server supports) | ⚙️ Medium - Most SMB 3.0 servers enable encryption |
| **Android Permissions** | Internet, Network State, Wake Lock | ✅ Low - Standard permissions |
| **Local Data** | No photo storage (stream only) | ✅ Low - No sensitive data persisted |

**Phase 2 Security Enhancements**:
- Migrate password storage to Android Keystore
- Force SMB 3.0 with encryption
- Certificate pinning for cloud API

---

### Testability

| Component | Testing Strategy | Mock Points |
|-----------|------------------|-------------|
| **ViewModels** | Unit tests with mocked repositories | `SlideshowRepository`, `SettingsRepository` |
| **Repositories** | Integration tests with fake data sources | `SmbPhotoDataSource`, `SettingsDataSource` |
| **Data Sources** | Unit tests with mocked SMB client | `SmbClient` |
| **UI Screens** | UI tests with Compose testing library | `SlideshowViewModel` (provide test instance) |

**Example: ViewModel Unit Test**:
```kotlin
@Test
fun `nextPhoto updates currentPhoto state`() = runTest {
    val mockRepository = mockk<SlideshowRepository>()
    coEvery { mockRepository.nextPhoto() } returns Result.Success(testPhoto)

    val viewModel = SlideshowViewModel(mockRepository, mockSettingsRepository)
    viewModel.nextPhoto()

    val state = viewModel.uiState.value
    assertTrue(state is SlideshowUiState.Playing)
    assertEquals(testPhoto, (state as SlideshowUiState.Playing).currentPhoto)
}
```

**Example: Repository Integration Test**:
```kotlin
@Test
fun `startSlideshow loads photos and initializes buffer`() = runTest {
    val fakeDataSource = FakeSmbPhotoDataSource(listOf(photo1, photo2, photo3, photo4))
    val repository = SlideshowRepositoryImpl(fakeDataSource, bufferManager, imageCache)

    val result = repository.startSlideshow()

    assertTrue(result is Result.Success)
    assertEquals(photo1, repository.currentPhoto.value)
    assertEquals(4, bufferManager.currentBufferSize())
}
```

**Coverage Targets**:
- Unit tests: >80% coverage
- Integration tests: >70% coverage
- UI tests: Critical user flows (slideshow playback, settings, schedule)

---

### Maintainability

| Aspect | Strategy | Benefit |
|--------|----------|---------|
| **Code Organization** | 2 Gradle modules, feature packages | Easy navigation, clear boundaries |
| **Dependency Injection** | Hilt (compile-time validated) | Type-safe, prevents runtime DI errors |
| **Standard Patterns** | MVVM, Repository, StateFlow | Team familiar with patterns, easy onboarding |
| **Documentation** | KDoc comments on public APIs | Self-documenting code |
| **Error Handling** | Result sealed class, custom exceptions | Type-safe error handling |
| **Logging** | Timber or custom logger | Centralized logging, easy debugging |

**Code Style**: Follow Kotlin coding conventions, ktlint for formatting

---

### Reliability (24/7 Operation)

| Concern | Mitigation | Validation |
|---------|------------|------------|
| **Memory Leaks** | Use lifecycle-aware components, weak references | LeakCanary, memory profiler |
| **Crashes** | Try-catch in critical paths, Result error handling | Crashlytics, stress testing |
| **Thread Safety** | Immutable models, Mutex on shared state, coroutines | Thread safety analyzer, stress tests |
| **Network Errors** | Graceful error handling, retry on next advance | Simulate network failures |
| **Battery Drain** | WorkManager (battery-efficient), disable GPS/sensors | Battery profiler, 24-hour test |

**Stress Testing**:
- Run slideshow for 24 hours, monitor memory/CPU
- Simulate network disconnects during playback
- Test with large photo libraries (10,000+ photos)

---

## 8. Testing Strategy

### Unit Tests (Target: >80% coverage)

**Components to Test**:
- ViewModels (with mocked repositories)
- Repositories (with fake data sources)
- Data sources (with mocked SMB client)
- Models (data class validation)
- Utilities (Result, extensions)

**Example Tests**:
```kotlin
class SlideshowViewModelTest {
    @Test fun `nextPhoto updates currentPhoto state`()
    @Test fun `pauseSlideshow cancels auto-advance`()
    @Test fun `error in repository shows error state`()
}

class SlideshowRepositoryTest {
    @Test fun `startSlideshow loads photos and initializes buffer`()
    @Test fun `nextPhoto advances index circularly`()
    @Test fun `shuffle randomizes photo order`()
}

class PhotoBufferManagerTest {
    @Test fun `initializeBuffer adds 4 photos`()
    @Test fun `updateBuffer evicts oldest photo`()
    @Test fun `buffer is thread-safe under concurrent access`()
}
```

**Mocking Strategy**:
- Use MockK for Kotlin-friendly mocking
- Fake data sources for integration tests (no real SMB access)

---

### Integration Tests (Target: >70% coverage)

**Components to Test**:
- ViewModel + Repository interaction
- Repository + DataSource interaction
- Image loading + caching flow

**Example Tests**:
```kotlin
class SlideshowIntegrationTest {
    @Test fun `end-to-end slideshow playback`() = runTest {
        // Setup: Real repository + fake data sources
        val repository = SlideshowRepositoryImpl(fakeDataSource, bufferManager, fakeCache)
        val viewModel = SlideshowViewModel(repository, fakeSettingsRepo)

        // Act: Start slideshow
        viewModel.startSlideshow()

        // Assert: Current photo loaded
        val state = viewModel.uiState.first { it is Playing }
        assertTrue(state is Playing)

        // Act: Advance to next photo
        viewModel.nextPhoto()

        // Assert: Photo changed
        val newState = viewModel.uiState.first { it is Playing && it.currentPhoto != state.currentPhoto }
        assertNotEquals(state.currentPhoto, newState.currentPhoto)
    }
}
```

---

### UI Tests (Critical Paths)

**Flows to Test**:
1. **Slideshow playback**: Start app → first photo displays → auto-advance to next photo
2. **Manual navigation**: Swipe left → next photo, swipe right → previous photo
3. **Settings**: Open settings → change interval → verify slideshow uses new interval
4. **SMB connection**: Enter SMB URL/credentials → test connection → success message
5. **Schedule**: Enable schedule → set times → verify WorkManager scheduled

**Tools**:
- Compose Testing Library
- Espresso (if needed)
- UI Automator (for system interactions)

**Example UI Test**:
```kotlin
@Test
fun slideshow_displaysPhotosAndAdvances() {
    composeTestRule.setContent {
        SlideshowScreen(
            viewModel = testViewModel,
            onNavigateToSettings = {}
        )
    }

    // Assert: First photo displayed
    composeTestRule.onNodeWithTag("photo_display").assertIsDisplayed()

    // Act: Wait for auto-advance (5 seconds)
    testClock.advanceTimeBy(5000)

    // Assert: Second photo displayed (different from first)
    // (Verify via test ViewModel state)
}
```

---

### Performance Tests

**Metrics to Measure**:
- Photo load time (from nextPhoto() call to bitmap rendered)
- Transition frame rate (should be 60fps)
- Memory usage over 24 hours (should stay <300MB)
- SMB scan time (for various photo counts)

**Tools**:
- Android Profiler (CPU, Memory, Network)
- Macrobenchmark library (for cold start time)
- Custom instrumented tests with timing

**Example Performance Test**:
```kotlin
@Test
fun photoLoadTime_isUnder2Seconds() = runTest {
    val startTime = System.currentTimeMillis()

    repository.nextPhoto()

    val loadTime = System.currentTimeMillis() - startTime
    assertTrue("Photo load time $loadTime ms exceeds 2s", loadTime < 2000)
}
```

---

## 9. Implementation Guidance

### Implementation Phases

#### Phase 1: Foundation (Weeks 1-2)
**Goal**: Setup project structure, DI, basic navigation

**Tasks**:
1. Create 2 Gradle modules (`:app`, `:core`)
2. Setup Hilt dependency injection
3. Create domain models (Photo, Settings, etc.)
4. Setup Compose navigation
5. Create basic UI screens (empty states)

**Deliverables**:
- ✅ Project compiles and runs
- ✅ Navigation between screens works
- ✅ Hilt DI setup complete
- ✅ Domain models defined

---

#### Phase 2: Data Layer (Weeks 3-4)
**Goal**: Implement repositories, data sources, SMB integration

**Tasks**:
1. Implement `SettingsRepository` + DataStore persistence
2. Implement `SmbClient` + jcifs-ng integration
3. Implement `SmbPhotoDataSource` (scan + load)
4. Implement `PhotoBufferManager` (4-photo buffer)
5. Implement `ImageCache` + Coil integration

**Deliverables**:
- ✅ SMB connection works (can list photos)
- ✅ Settings persist across app restarts
- ✅ Buffer management unit tests pass
- ✅ Image cache working with Coil

---

#### Phase 3: Slideshow Logic (Weeks 5-6)
**Goal**: Implement core slideshow functionality

**Tasks**:
1. Implement `SlideshowRepository` (start, next, prev, stop)
2. Implement `SlideshowViewModel` (UI state management)
3. Implement auto-advance timer
4. Implement photo buffer preloading
5. Write unit tests for slideshow logic

**Deliverables**:
- ✅ Slideshow plays automatically
- ✅ Manual navigation (swipe) works
- ✅ Photos preload correctly (no lag)
- ✅ Unit tests >80% coverage

---

#### Phase 4: UI Implementation (Weeks 7-8)
**Goal**: Build Compose UI screens with transitions

**Tasks**:
1. Implement `SlideshowScreen` (photo display + transitions)
2. Implement `SettingsScreen` (forms, validation)
3. Implement crossfade/slide transitions
4. Implement controls overlay (pause, next, settings)
5. Implement error states (connection errors, empty library)

**Deliverables**:
- ✅ Slideshow UI polished (60fps transitions)
- ✅ Settings UI functional (save/load)
- ✅ Error handling graceful
- ✅ UI tests for critical flows

---

#### Phase 5: Scheduling (Week 9)
**Goal**: Implement automated schedule with WorkManager

**Tasks**:
1. Implement `SlideshowWorker` (start/stop based on time)
2. Integrate WorkManager with settings
3. Implement schedule UI (time pickers)
4. Test schedule transitions (start at 8am, stop at 10pm)

**Deliverables**:
- ✅ Schedule starts slideshow automatically
- ✅ Schedule stops slideshow automatically
- ✅ WorkManager constraints working (network required)

---

#### Phase 6: Performance Optimization (Week 10)
**Goal**: Profile and optimize for NFRs

**Tasks**:
1. Performance testing on target hardware (tablet + SMB share)
2. Measure photo load time, memory usage, frame rate
3. Optimize if NFRs not met (increase buffer, connection pooling, etc.)
4. 24-hour stress test (memory leak detection)

**Deliverables**:
- ✅ Photo load time <2s (measured)
- ✅ Transitions 60fps (measured)
- ✅ Memory usage <300MB (measured)
- ✅ No memory leaks or crashes

---

#### Phase 7: Testing & Polish (Weeks 11-12)
**Goal**: Comprehensive testing, bug fixes, polish

**Tasks**:
1. Write UI tests for all critical flows
2. Integration tests for data layer
3. Bug fixes from testing
4. UI polish (loading states, animations, error messages)
5. Documentation (README, user guide)

**Deliverables**:
- ✅ All tests passing (unit, integration, UI)
- ✅ No critical bugs
- ✅ User documentation complete
- ✅ Ready for MVP release

---

### Developer Notes

#### Common Pitfalls to Avoid

1. **Memory Leaks in Coroutines**:
   - Always use `viewModelScope` or `lifecycleScope` for coroutines
   - Cancel coroutines when no longer needed
   - Avoid capturing Activity/Fragment references in long-lived coroutines

2. **SMB Connection Timeouts**:
   - Always use `withContext(Dispatchers.IO)` for SMB operations
   - Set reasonable timeouts (30s default)
   - Handle `SmbException` gracefully (show error to user)

3. **Image Downsampling**:
   - Always specify `size()` in Coil requests (don't load full resolution)
   - Use `scale: Scale.FIT` to preserve aspect ratio
   - Monitor memory usage with profiler

4. **Buffer Management Race Conditions**:
   - Always use `mutex.withLock` for buffer operations
   - Don't access buffer from multiple coroutines without lock
   - Use immutable data classes to prevent accidental mutations

5. **DataStore Writes on Main Thread**:
   - DataStore writes are async (suspend functions)
   - Don't block main thread waiting for DataStore writes
   - Use `Flow.collectAsState()` in Compose for reactive updates

---

#### Useful Code Patterns

**Pattern 1: Safe SMB Operations**
```kotlin
suspend fun <T> safeSmb(operation: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
        Result.Success(operation())
    } catch (e: SmbException) {
        Result.Error(e)
    } catch (e: IOException) {
        Result.Error(e)
    }
}

// Usage:
val result = safeSmb { smbFile.listFiles() }
```

**Pattern 2: Compose State Collection**
```kotlin
@Composable
fun SlideshowScreen(viewModel: SlideshowViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // UI automatically recomposes when uiState changes
}
```

**Pattern 3: Result Handling**
```kotlin
repository.nextPhoto()
    .onSuccess { photo ->
        _uiState.value = Playing(photo)
    }
    .onError { error ->
        _uiState.value = Error(error.message)
    }
```

---

#### References to Similar Code

**MVVM + Repository Pattern**:
- [Android Architecture Samples](https://github.com/android/architecture-samples)
- [Now in Android App](https://github.com/android/nowinandroid)

**Jetpack Compose**:
- [Compose Samples](https://github.com/android/compose-samples)
- [Jetpack Compose Pathways](https://developer.android.com/courses/pathways/compose)

**Hilt Dependency Injection**:
- [Hilt Codelab](https://developer.android.com/codelabs/android-hilt)
- [Dagger Hilt Guide](https://dagger.dev/hilt/)

**Coil Image Loading**:
- [Coil Documentation](https://coil-kt.github.io/coil/)
- [Coil Compose Integration](https://coil-kt.github.io/coil/compose/)

**WorkManager Scheduling**:
- [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
- [WorkManager Codelab](https://developer.android.com/codelabs/android-workmanager)

---

## 10. Risk Assessment & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **SMB performance slower than expected** | Medium | High | Profile early (Week 8), add connection pooling if needed |
| **Memory leaks during 24/7 operation** | Medium | High | Use LeakCanary, stress test for 24 hours, fix leaks |
| **Coil cache not optimized for slideshow** | Low | Medium | Custom LRU eviction policy, profile cache hit rate |
| **jcifs-ng compatibility issues** | Low | High | Test with multiple SMB servers (Windows, Samba, NAS) |
| **WorkManager scheduling unreliable** | Low | Medium | Add alarm-based fallback (AlarmManager) |
| **Tablet sleep mode interrupts slideshow** | Medium | Medium | Use PARTIAL_WAKE_LOCK, request ignore battery optimization |
| **Large photo libraries (10k+) slow to scan** | High | Medium | Show progress indicator, cache photo list, incremental scan |
| **Network disconnects during playback** | High | Medium | Buffer keeps slideshow running, show "reconnecting" UI |
| **User enters invalid SMB credentials** | High | Low | Validate credentials with test connection, clear error messages |
| **Phase 2 refactoring cost higher than expected** | Medium | Medium | Maintain clean repository abstractions, defer UseCases |

---

## 11. Open Questions & Assumptions

### Assumptions

1. **Target hardware**: Modern Android tablet (8-10", 2560x1600 resolution, 4GB+ RAM)
2. **Photo library size**: 1,000-10,000 photos (fits in memory with 4-photo buffer)
3. **Network speed**: 100Mbps+ LAN (typical home network)
4. **SMB server**: Supports SMB 2.0+ (modern Windows, Samba, NAS)
5. **User technical skill**: Can configure SMB URL, username, password (not beginner)

### Open Questions (To Be Resolved During Implementation)

1. **Should we support HTTPS/WebDAV in addition to SMB?**
   - **Decision**: Defer to Phase 2 (focus on SMB for MVP)

2. **Should we support local photos (device storage) in addition to SMB?**
   - **Decision**: Defer to Phase 2 (SMB-only for MVP)

3. **Should we support video files in addition to photos?**
   - **Decision**: Defer to Phase 2 (photos-only for MVP)

4. **Should we implement custom transitions (Ken Burns effect, zoom pan)?**
   - **Decision**: Include basic transitions (crossfade, slide) for MVP, advanced transitions Phase 2

5. **Should we support multiple SMB shares (switch between libraries)?**
   - **Decision**: Single SMB share for MVP, multiple shares Phase 2

6. **Should we cache photo list to avoid rescanning on every start?**
   - **Decision**: Yes, but only if SMB scan time >5s (optimize based on profiling)

7. **Should we support landscape + portrait photos (mixed orientations)?**
   - **Decision**: Yes, use `Modifier.fillMaxSize()` + `ContentScale.Fit` (preserve aspect ratio)

---

## 12. Success Metrics

### Functional Success Criteria (MVP Launch)

- ✅ All 12 user stories from PRD implemented
- ✅ Slideshow plays continuously for 24 hours without crash
- ✅ SMB connection works with major server types (Windows, Samba, Synology NAS)
- ✅ Settings persist across app restarts
- ✅ Schedule starts/stops slideshow automatically

### Performance Success Criteria (Measured)

- ✅ Photo load time <2s (P95, measured with profiler)
- ✅ Transition frame rate 60fps (measured with GPU profiler)
- ✅ Memory usage <300MB (measured over 24 hours)
- ✅ No memory leaks (verified with LeakCanary)
- ✅ No ANR (Application Not Responding) errors

### Quality Success Criteria

- ✅ Unit test coverage >80%
- ✅ Zero critical bugs (app crashes, data loss)
- ✅ <5 medium bugs at launch
- ✅ Code review approval from senior developers
- ✅ Architecture review approval (this document)

### User Acceptance Criteria

- ✅ Users can set up slideshow in <5 minutes (first-time setup)
- ✅ Slideshow runs 24/7 without requiring intervention
- ✅ Transitions are smooth and professional-looking
- ✅ Error messages are clear and actionable

---

## 13. Next Steps

1. **Coordinator Validation**: Review this architecture document for completeness
2. **Senior Dev NFR Review (Phase 5)**: Validate architecture meets NFRs (performance, security, testability)
3. **QA Test Plan (Phase 6)**: QA agents create test cases based on this architecture
4. **Implementation (Phase 7)**: Developer agent implements based on this specification
5. **Performance Profiling (Week 8)**: Validate NFRs on target hardware, optimize if needed

---

## Document Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-03-01 | Initial architecture synthesis (Phase 4 complete) |

---

**END OF FINAL ARCHITECTURE DOCUMENT**

This architecture represents a balanced synthesis of modularity, performance, and simplicity, tailored for a 2-3 developer team building an MVP in 3-4 months. It provides clear implementation guidance while preserving extensibility for Phase 2.
