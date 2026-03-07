# Architecture Decision Record: Digital Photo Frame MVP

**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Date**: 2026-03-01
**Status**: APPROVED
**Decision Makers**: Synthesis Agent (input from 3 Architects)
**PRD Reference**: `docs/features/photo-frame-app-initial/requirements/PRD_DRAFT.md`

---

## Context

### Problem Statement

We need to design and implement a Digital Photo Frame application for Android tablets that:
- Streams photos from SMB/Samba network shares
- Displays photos in a continuous slideshow with smooth transitions
- Operates reliably 24/7 with automated scheduling
- Provides a "set it and forget it" kiosk-style experience
- Can be implemented by 2-3 developers in 3-4 months (MVP Phase 1)

This is a **greenfield project** (new application) with no existing codebase constraints. Phase 2 (cloud integration, offline mode) is 6+ months away.

### Requirements Summary

**Functional Requirements** (12 User Stories):
1. Connect to SMB network share with authentication
2. Scan photo library and display as slideshow
3. Configure display interval (3-60 seconds per photo)
4. Choose transition effects (crossfade, slide, zoom)
5. Manual navigation (swipe left/right for next/previous photo)
6. Shuffle mode (randomize photo order)
7. Automated scheduling (start/stop times, e.g., 8am-10pm)
8. Settings persistence (save configuration across restarts)
9. Connection testing (validate SMB credentials before saving)
10. Error handling (graceful degradation, user-friendly messages)
11. Display system information (debug screen with connection status, memory usage)
12. Support landscape and portrait photos (mixed orientations)

**Non-Functional Requirements**:
- **Performance**: Photo load <2s, 60fps transitions, <300MB memory
- **Reliability**: 24/7 operation without crashes, handle network errors gracefully
- **Usability**: <5 minute setup time, clear error messages
- **Security**: Secure password storage (DataStore for MVP, encrypt in Phase 2)
- **Maintainability**: Standard Android patterns, testable architecture

### Constraints

- **Team**: 2-3 developers (Android experience assumed)
- **Timeline**: 3-4 months for MVP Phase 1
- **Target Hardware**: Android tablets (8-10", 2560x1600 resolution, 4GB+ RAM)
- **Network**: Local SMB shares (Windows, Samba, NAS devices)
- **Scope**: SMB-only for MVP, cloud services deferred to Phase 2

---

## Decision

### Chosen Architecture: "Pragmatic Modularity"

We adopt a **balanced synthesis** of three architectural proposals (Modularity, Performance, Simplicity):

**Base Philosophy**: Build the simplest architecture that meets all requirements while preserving extensibility for Phase 2. Favor proven patterns over experimental optimizations. Profile before optimizing.

**Module Structure**: 2 Gradle modules (`:app`, `:core`)

**Architecture Pattern**: MVVM (ViewModel → Repository → DataSource), skip UseCase layer for MVP

**Photo Buffer**: 4 photos (Current - 1, Current, Current + 1, Current + 2)

**Caching**: In-memory LRU (100MB) + Coil disk cache (512MB)

**SMB Integration**: Standard jcifs-ng, defer connection pooling until profiling shows it's needed

**Thread Safety**: Standard coroutines + single Mutex + @Immutable data classes

**Image Loading**: Coil with screen resolution downsampling (2560x1600)

**Scheduling**: WorkManager for automated start/stop

**Dependency Injection**: Hilt (standard setup)

---

## Key Design Decisions

### Decision 1: Module Structure (2 Gradle Modules)

**Decision**: Use 2 Gradle modules (`:app`, `:core`) instead of 8 (Arch 1) or 1 (Arch 3)

**Rationale**:
- **Balance complexity vs. structure**: 2 modules provides enough separation (UI vs. business logic) without the overhead of 8 modules
- **Team size consideration**: 2-3 developers don't need 8 modules for parallel development
- **Testability**: `:core` module is framework-agnostic, testable without Android emulator
- **Performance isolation**: Network/data layer in `:core` can be profiled independently
- **Extensibility**: Easy to add more modules in Phase 2 if needed (e.g., `:cloud` module)

**Alternatives Considered**:

**Alternative A: 8 Gradle Modules (Architect 1 - Modularity)**
- Modules: `:app`, `:feature:slideshow`, `:feature:settings`, `:domain`, `:data`, `:network:smb`, `:core:common`, `:core:ui`
- **Pros**: Excellent separation of concerns, compile-time dependency enforcement, each module highly testable
- **Cons**: High overhead for 2-3 developers, longer Gradle sync times, context-switching during debugging
- **Why Not Chosen**: Over-engineered for MVP scope. Team will spend significant time navigating module boundaries. 8 modules justifiable for 10+ developers or very large apps, not 3-4 month MVP.

**Alternative B: 1 Gradle Module (Architect 3 - Simplicity)**
- Single module with feature packages: `ui/`, `data/`, `domain/`, `utils/`
- **Pros**: Zero module overhead, fast compile times, easy navigation, lowest cognitive load
- **Cons**: No compile-time dependency enforcement, harder to test data layer without Android framework
- **Why Not Chosen**: Too little structure for extensibility. Phase 2 will add cloud services, which benefits from separate modules. 1 module works for simple apps but risky for this scope.

**Alternative C: 3-4 Modules (Architect 2 - Performance)**
- Modules: `:app`, `:core`, `:network`, `:ui`
- **Pros**: Balanced structure, isolates network layer for optimization
- **Cons**: Still more overhead than 2 modules, `:ui` module separation may not add value
- **Why Not Chosen**: 4 modules is close to our decision, but `:ui` and `:network` can be packages in `:core` without losing much testability. 2 modules is simpler.

**Consequences**:
- ✅ **Positive**: Fast development velocity, low Gradle overhead, still testable
- ✅ **Positive**: Easy to add modules in Phase 2 without major refactoring
- ⚠️ **Negative**: Less strict compile-time enforcement of layer boundaries (rely on code review)
- ⚠️ **Negative**: `:core` module may grow large over time (mitigate with feature packages)

---

### Decision 2: Skip UseCase/Interactor Layer for MVP

**Decision**: Use ViewModel → Repository → DataSource pattern, defer UseCase layer to Phase 2

**Rationale**:
- **Consensus**: 2 out of 3 architects (Arch 2 & 3) agreed to defer UseCases
- **Simplicity**: MVP business logic is simple (load photos, display slideshow). No complex orchestration like cloud sync or offline mode.
- **Time savings**: Saves 2-3 weeks implementation time (20+ UseCase classes avoided)
- **Testability**: ViewModels remain testable by mocking repositories
- **Refactoring cost**: 1-2 weeks to add UseCases in Phase 2 (acceptable for 6+ month timeline)

**Alternatives Considered**:

**Alternative A: Include UseCase Layer Now (Architect 1 - Modularity)**
- Pattern: ViewModel → UseCase → Repository → DataSource
- Example:
  ```kotlin
  class LoadNextPhotoUseCase(
      private val slideshowRepository: SlideshowRepository,
      private val imageCache: ImageCache
  ) {
      suspend operator fun invoke(): Result<Photo> {
          return slideshowRepository.getNextPhoto()
              .onSuccess { imageCache.preload(it) }
      }
  }
  ```
- **Pros**: Business logic reusable across ViewModels, Single Responsibility Principle, easier to unit test without Android framework
- **Cons**: Adds indirection (ViewModel → UseCase → Repository), 20+ UseCase classes to maintain, may be over-abstraction for simple CRUD
- **Why Not Chosen**: MVP business logic doesn't justify the abstraction. Phase 2 (cloud sync, offline mode) will benefit from UseCases, but MVP is too simple. Team consensus favors deferring.

**Consequences**:
- ✅ **Positive**: Faster MVP implementation (2-3 weeks saved)
- ✅ **Positive**: Simpler codebase with fewer classes to maintain
- ✅ **Positive**: Still testable with mocked repositories
- ⚠️ **Negative**: Business logic lives in ViewModels (may grow large over time)
- ⚠️ **Negative**: Phase 2 refactoring needed (1-2 weeks to extract UseCases from ViewModels)

**Mitigation**:
- Keep ViewModels focused (single responsibility: manage UI state, delegate to repositories)
- When Phase 2 starts, extract complex business logic (cloud sync, conflict resolution) into UseCases

---

### Decision 3: Photo Buffer Size (4 Photos)

**Decision**: Use a 4-photo buffer [Current - 1, Current, Current + 1, Current + 2] instead of 3 (Arch 1 & 3) or 5 (Arch 2)

**Rationale**:
- **Compromise**: Middle ground between 3 (simple) and 5 (resilient)
- **Performance**: 4 photos provide better resilience to network hiccups than 3, without the memory overhead of 5
- **Memory**: ~60MB for 4 photos (ARGB_8888, downsampled to 2560x1600), well under 300MB limit
- **Navigation**: Supports reverse navigation (Current - 1) plus 2-photo read-ahead for smooth auto-advance

**Alternatives Considered**:

**Alternative A: 3 Photos [Prev, Current, Next] (Architect 1 & 3)**
- **Pros**: Lower memory (~45MB), standard pattern, sufficient for 60fps transitions
- **Cons**: Less resilient to SMB latency spikes, no extra read-ahead buffer
- **Why Not Chosen**: 3 photos is adequate but leaves no margin for network issues. 4 photos adds only ~15MB memory but significant resilience.

**Alternative B: 5 Photos [Current - 2, Current - 1, Current, Current + 1, Current + 2] (Architect 2)**
- **Pros**: Maximum resilience to network issues, supports bidirectional navigation, completely hides SMB latency
- **Cons**: Higher memory (~75MB), longer initial buffer fill, more complex buffer management
- **Why Not Chosen**: 5 photos is over-optimization for MVP. 4 photos achieves 90% of the benefit with lower complexity. Can increase to 5 later if profiling shows it's needed.

**Consequences**:
- ✅ **Positive**: Better resilience than 3-photo buffer (network hiccups don't stutter slideshow)
- ✅ **Positive**: Lower memory than 5-photo buffer (~15MB savings)
- ✅ **Positive**: Supports reverse navigation (swipe right for previous photo)
- ⚠️ **Negative**: Slightly more complex buffer management than 3 photos (mitigate with unit tests)

**Performance Calculation**:
- Downsampled image: 2560 × 1600 × 4 bytes (ARGB_8888) = ~16MB per photo
- 4-photo buffer: 4 × 16MB = ~64MB
- Total memory budget: <300MB (NFR)
- Remaining for cache: ~236MB (plenty of headroom)

---

### Decision 4: Caching Strategy (In-Memory + Coil Disk Cache)

**Decision**: Use in-memory LRU cache (100MB) + Coil's disk cache (512MB), skip Room database for MVP

**Rationale**:
- **Simplicity**: Coil is battle-tested and handles disk caching automatically
- **Proven performance**: Coil used by thousands of Android apps, efficient memory management
- **Time savings**: No Room setup, migration, or cache invalidation logic
- **Sufficient for MVP**: No persistent metadata needed (no offline mode, no "recently viewed" features)

**Alternatives Considered**:

**Alternative A: Room Database + In-Memory Cache (Architect 1)**
- Room for persistent metadata:
  ```kotlin
  @Entity
  data class PhotoCacheEntity(
      @PrimaryKey val path: String,
      val lastAccessed: Long,
      val fileSize: Long,
      val thumbnailPath: String?
  )
  ```
- **Pros**: Persistent metadata survives app restarts, can implement "recently viewed" features
- **Cons**: Adds Room dependency, more complex cache invalidation, may be overkill for MVP (no offline mode)
- **Why Not Chosen**: Room is valuable for structured data, but MVP doesn't need persistent photo metadata. Coil's cache is sufficient for slideshow use case. Defer Room to Phase 2 if needed.

**Alternative B: Custom Time-Aware LRU Cache (Architect 2)**
- Custom cache with TTL (time-to-live):
  ```kotlin
  class TimeAwareLRUCache(maxSize: Int, private val ttl: Duration) {
      fun get(key: String): Bitmap? {
          val entry = cache[key] ?: return null
          if (entry.isExpired()) {
              cache.remove(key)
              return null
          }
          return entry.bitmap
      }
  }
  ```
- **Pros**: Fine-grained control over eviction, optimized for slideshow access patterns
- **Cons**: More complex implementation, need to tune TTL values, requires performance testing
- **Why Not Chosen**: Premature optimization. Coil's LRU cache is sufficient for MVP. Can implement custom cache later if profiling shows eviction issues.

**Consequences**:
- ✅ **Positive**: Simplest implementation (leverage Coil's cache)
- ✅ **Positive**: Proven performance (Coil is mature and well-tested)
- ✅ **Positive**: Less code to maintain and debug
- ⚠️ **Negative**: Less control over eviction policy (mitigate: configure Coil's cache size)
- ⚠️ **Negative**: No persistent metadata (acceptable for MVP, add Room in Phase 2 if needed)

---

### Decision 5: SMB Optimization (Standard jcifs-ng, Defer Connection Pooling)

**Decision**: Use standard jcifs-ng library without connection pooling or memory-mapped I/O for MVP

**Rationale**:
- **Consensus**: 2 out of 3 architects (Arch 1 & 3) favor standard approach
- **Proven library**: jcifs-ng is fast enough for most SMB scenarios (supports SMB 2.0-3.1.1)
- **Profile first**: Connection pooling can be added later if profiling shows SMB handshake is slow
- **Time savings**: Saves 2-3 weeks implementation time (no custom connection management)
- **Lower risk**: Standard approach has fewer bugs than custom connection pooling

**Alternatives Considered**:

**Alternative A: Connection Pooling + Memory-Mapped I/O (Architect 2)**
- Connection pool:
  ```kotlin
  class SmbConnectionPool(maxConnections: Int = 5) {
      private val pool = ConcurrentLinkedQueue<SmbFile>()
      suspend fun <T> withConnection(block: (SmbFile) -> T): T {
          val connection = pool.poll() ?: createConnection()
          try { return block(connection) }
          finally { pool.offer(connection) }
      }
  }
  ```
- Memory-mapped I/O for large files (reduces copy operations)
- **Pros**: Reduces SMB handshake overhead (reuse connections), faster reads with memory-mapped I/O
- **Cons**: Complex connection management (leaks, timeouts), memory-mapped I/O requires local cache (more disk I/O)
- **Why Not Chosen**: Premature optimization. jcifs-ng reuses connections for sequential reads (slideshow pattern). Connection pooling valuable if loading many photos concurrently (not our use case). Can add later if profiling shows it's needed.

**Consequences**:
- ✅ **Positive**: Simplest approach (standard jcifs-ng usage)
- ✅ **Positive**: Faster time-to-market (save 2-3 weeks)
- ✅ **Positive**: Lower risk of bugs in connection management
- ⚠️ **Negative**: May have higher latency than optimized approach (mitigate: profile early, add pooling if needed)
- ⚠️ **Negative**: No connection reuse between requests (acceptable: jcifs-ng handles this internally for sequential reads)

**Optimization Plan** (if needed):
1. **Week 8-9**: Performance testing on target hardware (tablet + SMB share)
2. **If <2s NFR not met**: Add connection pooling first (lowest complexity)
3. **If still not met**: Increase buffer to 5 photos (more read-ahead)
4. **If still not met**: Consider memory-mapped I/O or other optimizations

---

### Decision 6: Thread Safety Strategy (Standard Coroutines + Single Mutex)

**Decision**: Use standard coroutine patterns (Dispatchers.IO for I/O, StateFlow for reactive updates) with single Mutex for buffer management, @Immutable data classes

**Rationale**:
- **Proven patterns**: Structured concurrency is standard in modern Android apps
- **Easy to reason about**: Single Mutex simplifies debugging, clear ownership of critical sections
- **Thread safety**: @Immutable data classes prevent accidental mutations, StateFlow is thread-safe
- **Good enough performance**: Mutex contention unlikely for slideshow use case (low concurrent access)

**Alternatives Considered**:

**Alternative A: Lock-Free Data Structures (Architect 2)**
- Use `AtomicReference`, `ConcurrentHashMap` where possible:
  ```kotlin
  class PhotoBuffer {
      private val buffer = AtomicReference<List<Photo>>(emptyList())
      fun updateBuffer(photos: List<Photo>) {
          buffer.set(photos) // Lock-free update
      }
  }
  ```
- **Pros**: Higher throughput, no lock contention
- **Cons**: Lock-free code is harder to reason about and debug, more complex to get right
- **Why Not Chosen**: Unnecessary complexity for MVP. Slideshow use case has low concurrent access (one auto-advance coroutine + occasional user gesture). Mutex is sufficient and simpler.

**Consequences**:
- ✅ **Positive**: Standard patterns, easy to understand and maintain
- ✅ **Positive**: Single Mutex prevents data races, clear ownership
- ✅ **Positive**: @Immutable data classes prevent accidental mutations
- ⚠️ **Negative**: Mutex can become bottleneck if overused (mitigate: use only for buffer updates, keep critical sections short)

**Concurrency Plan**:
- All I/O operations run on `Dispatchers.IO` (network, disk)
- UI updates run on `Dispatchers.Main` (StateFlow handles thread switching)
- Buffer updates protected by single Mutex (serialize access)
- Immutable data classes passed between layers (no shared mutable state)

---

### Decision 7: Image Loading Library (Coil with Downsampling)

**Decision**: Use Coil image loading library with aggressive downsampling to screen resolution (2560x1600)

**Rationale**:
- **Consensus**: All 3 architects agreed on Coil
- **Proven performance**: Coil is battle-tested, used by thousands of apps
- **Coroutine-native**: Coil uses suspend functions (fits our coroutine architecture)
- **Memory efficiency**: Built-in downsampling, LRU eviction, disk caching
- **Extensibility**: Supports custom Fetchers (can add SMB Fetcher if needed)

**Downsampling Importance**:
- Full resolution photo: 4000×3000 = 12MP = ~48MB (ARGB_8888)
- Downsampled to screen: 2560×1600 = 4.1MP = ~16MB (ARGB_8888)
- **Memory savings**: 3x reduction per photo, critical for <300MB NFR

**Alternatives Considered**:

**Alternative A: Glide**
- Similar to Coil (memory management, disk cache)
- **Pros**: Mature library, large community
- **Cons**: Not coroutine-native (uses callbacks), Java-based (less idiomatic Kotlin)
- **Why Not Chosen**: Coil is more modern, coroutine-native, better Kotlin integration

**Alternative B: Picasso**
- Simpler API than Coil/Glide
- **Pros**: Lightweight, easy to use
- **Cons**: Less actively maintained, lacks some features (e.g., video thumbnails, SVG support)
- **Why Not Chosen**: Coil is more feature-complete and actively maintained

**Consequences**:
- ✅ **Positive**: Proven performance and reliability
- ✅ **Positive**: Memory-efficient (downsampling + LRU cache)
- ✅ **Positive**: Coroutine-native (fits our architecture)
- ⚠️ **Negative**: Learning curve for team (mitigate: good documentation, many examples)

---

### Decision 8: Scheduling Mechanism (WorkManager)

**Decision**: Use WorkManager for automated slideshow start/stop based on schedule

**Rationale**:
- **Consensus**: All 3 architects agreed on WorkManager
- **Battery efficiency**: WorkManager is optimized for background tasks (Doze mode compatible)
- **Reliability**: Guaranteed execution even after device reboot
- **Constraints support**: Can require network connectivity (needed for SMB access)

**Alternatives Considered**:

**Alternative A: AlarmManager**
- Older scheduling API
- **Pros**: Precise timing (can trigger at exact time)
- **Cons**: Less battery-efficient, doesn't survive app updates, requires manual reboot handling
- **Why Not Chosen**: WorkManager is modern replacement for AlarmManager, better battery efficiency

**Alternative B: Foreground Service**
- Keep app running in foreground 24/7
- **Pros**: Maximum reliability, no scheduling needed
- **Cons**: Always consumes resources (battery drain), requires persistent notification, overkill for scheduled tasks
- **Why Not Chosen**: WorkManager achieves same goal with better battery efficiency

**Consequences**:
- ✅ **Positive**: Battery-efficient scheduling
- ✅ **Positive**: Survives device reboots and app updates
- ✅ **Positive**: Constraints support (network required)
- ⚠️ **Negative**: Timing not precise (±15 minutes, acceptable for daily schedule like 8am-10pm)

**Schedule Implementation**:
```kotlin
val startRequest = PeriodicWorkRequestBuilder<SlideshowWorker>(15, TimeUnit.MINUTES)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
```

---

### Decision 9: Dependency Injection Framework (Hilt)

**Decision**: Use Hilt for dependency injection

**Rationale**:
- **Consensus**: All 3 architects agreed on Hilt
- **Type-safe**: Compile-time validation prevents runtime DI errors
- **Android-optimized**: Hilt is built on Dagger, optimized for Android lifecycle
- **Standard**: Hilt is Google's recommended DI framework for Android

**Alternatives Considered**:

**Alternative A: Koin**
- Pure Kotlin DI framework (no annotation processing)
- **Pros**: Simpler setup, no kapt overhead, easier to learn
- **Cons**: Runtime validation (errors discovered at runtime, not compile-time), slower dependency resolution
- **Why Not Chosen**: Hilt's compile-time validation is valuable for reliability (catch errors early)

**Alternative B: Manual DI (no framework)**
- Use factory pattern, manual dependency passing
- **Pros**: No framework dependency, full control
- **Cons**: Boilerplate code, harder to maintain, no compile-time validation
- **Why Not Chosen**: For 30+ classes (ViewModels, Repositories, DataSources), manual DI is too much boilerplate

**Consequences**:
- ✅ **Positive**: Type-safe, compile-time validated
- ✅ **Positive**: Standard Android pattern (team familiar)
- ✅ **Positive**: Facilitates testing (mock dependencies easily)
- ⚠️ **Negative**: Kapt compilation overhead (acceptable for this project size)

---

## Alternatives Considered (High-Level)

### Alternative 1: Pure Modularity Approach (Architect 1)

**Description**: Clean Architecture with 8 Gradle modules, UseCase layer, Room database

**Module Structure**:
- `:app` (orchestration only, 100-200 lines)
- `:feature:slideshow` (UI + ViewModels)
- `:feature:settings` (UI + ViewModels)
- `:domain` (Use Cases, 20+ classes)
- `:data` (Repositories, 10+ classes)
- `:network:smb` (SMB implementation)
- `:core:common` (utilities)
- `:core:ui` (shared UI components)

**Pros**:
- Excellent separation of concerns
- Each module highly testable in isolation
- Easy to add new features without touching existing code
- Enforces dependency rules at compile-time
- Supports parallel development by multiple developers

**Cons**:
- 8 modules for 2-3 developers on MVP is significant overhead
- Gradle sync times increase with module count
- Context-switching between modules during debugging
- More boilerplate (module build.gradle files, dependency declarations)
- May slow down initial development velocity

**Why Not Chosen**:
This architecture is ideal for large teams (10+ developers) or very large apps (50+ features), but over-engineered for our MVP scope. The team will spend significant time navigating module boundaries, managing Gradle dependencies, and context-switching during debugging. The testability benefits don't justify the overhead for 2-3 developers on a 3-4 month timeline.

**Key Insight from Architect 1**:
"Clean Architecture is valuable for long-term maintainability, but MVP scope doesn't justify 8 modules. Consider 2-3 modules as middle ground."

---

### Alternative 2: Pure Performance Approach (Architect 2)

**Description**: Performance-optimized architecture with aggressive pre-loading, connection pooling, 5-photo buffer

**Key Optimizations**:
- 5-photo buffer [Current - 2, Current - 1, Current, Current + 1, Current + 2]
- Connection pooling (reuse SMB connections)
- Memory-mapped I/O for large files
- Custom time-aware LRU cache with TTL
- Parallel directory scanning
- Lock-free data structures (AtomicReference, ConcurrentHashMap)

**Pros**:
- Pre-optimized for all NFRs (very likely to meet <2s, 60fps, <300MB)
- Resilient to network hiccups (5-photo buffer keeps slideshow running)
- Supports bidirectional navigation (reverse with extra buffer)
- Maximum throughput (lock-free primitives)

**Cons**:
- Higher memory usage (~75MB for 5-photo buffer vs ~45MB for 3)
- Complex connection management (leaks, timeouts)
- More code to maintain and debug (custom cache, connection pool)
- Premature optimization (may optimize for bottlenecks that don't exist)
- Requires extensive performance testing to validate gains

**Why Not Chosen**:
While performance is critical for a 24/7 slideshow app, this approach optimizes before profiling. Many optimizations (connection pooling, memory-mapped I/O, 5-photo buffer) may provide marginal gains over standard approach (jcifs-ng + Coil + 4-photo buffer). The MVP strategy is to build with proven libraries, profile early (Week 8), and optimize only if NFRs aren't met. This saves 2-3 weeks implementation time and reduces risk of bugs in complex optimization code.

**Key Insight from Architect 2**:
"Performance is critical, but profile first. If standard approach meets NFRs, avoid premature optimization. If not, add connection pooling as first step (lowest complexity, highest impact)."

---

### Alternative 3: Pure Simplicity Approach (Architect 3)

**Description**: MVP-first architecture with 1 Gradle module, minimal abstractions, 3-photo buffer

**Module Structure**:
- Single module with feature packages: `ui/`, `data/`, `domain/`, `utils/`
- ViewModel → Repository → DataSource (skip UseCase layer)
- 3-photo buffer [Prev, Current, Next]
- In-memory cache only (rely on Coil's disk cache)
- Standard jcifs-ng (no custom optimizations)

**Pros**:
- Zero Gradle module overhead for 2-3 developers
- Fast compile times, instant refactoring across packages
- Easy navigation, all code in one place
- Lower cognitive load for team
- Fastest time-to-market (2-3 months possible)

**Cons**:
- No compile-time dependency enforcement (relies on code review)
- Harder to enforce layer boundaries (no physical separation)
- May need refactoring if project grows significantly
- Parallel development less isolated (more merge conflicts)
- Less structure for Phase 2 cloud integration

**Why Not Chosen**:
While simplicity is a core value, 1 module is too little structure for extensibility. Phase 2 will add cloud services, offline mode, and conflict resolution—features that benefit from module separation. The refactoring cost to split 1 module into multiple modules later is higher than starting with 2 modules. Additionally, `:core` module testability (without Android framework) is valuable for unit testing business logic.

**Key Insight from Architect 3**:
"Simplicity is critical for MVP velocity, but 1 module may be too simple. Consider 2 modules as compromise: `:app` (UI) + `:core` (business logic)."

---

## Trade-offs Accepted

### Trade-off 1: Modularity vs. Implementation Speed

**Gained**: Faster MVP implementation (2 modules vs 8)
- Save 3-4 weeks on Gradle module setup, dependency management, navigation overhead
- Smaller team (2-3 developers) works more efficiently without excessive module boundaries

**Lost**: Strict compile-time enforcement of layer boundaries
- No physical module separation between data layer components (rely on code review)
- Potential for developers to accidentally violate layer boundaries (e.g., ViewModel directly accessing DataSource)

**Justification**: For a 2-3 developer team on 3-4 month MVP, the time savings outweigh the benefits of strict boundaries. Code review and team discipline can enforce boundaries without module complexity. If project grows in Phase 2, we can split `:core` into more modules.

---

### Trade-off 2: Performance Optimization vs. Simplicity

**Gained**: Simpler codebase, lower risk, faster implementation
- Standard jcifs-ng usage (no custom connection pooling or memory-mapped I/O)
- Coil's standard cache (no custom time-aware LRU)
- 4-photo buffer (not 5)
- Save 2-3 weeks implementation time, less code to debug

**Lost**: Potential performance gains from aggressive optimization
- Connection pooling may reduce SMB latency by 100-200ms (if handshake is slow)
- 5-photo buffer more resilient to network hiccups
- Custom cache may have better eviction policy for slideshow pattern

**Justification**: Premature optimization is risky—we don't know actual bottlenecks without profiling. Standard approach with proven libraries (jcifs-ng + Coil) likely meets NFRs (<2s load, 60fps). If profiling shows it doesn't, we have a clear optimization path (add connection pooling, increase buffer). This "profile first, optimize later" approach is safer and faster for MVP.

---

### Trade-off 3: Abstraction (UseCases) vs. Time-to-Market

**Gained**: Faster MVP implementation, fewer classes to maintain
- Skip 20+ UseCase classes (LoadNextPhotoUseCase, PreloadBufferUseCase, etc.)
- Direct ViewModel → Repository calls (simpler call chain)
- Save 2-3 weeks implementation time

**Lost**: Business logic reusability and separation
- Business logic lives in ViewModels (may grow large over time)
- Harder to reuse logic across multiple ViewModels
- Phase 2 refactoring needed (1-2 weeks to extract UseCases)

**Justification**: MVP business logic is simple (load photos, display slideshow). UseCases add value when orchestrating complex operations (e.g., cloud sync: load local + remote, merge, resolve conflicts). Phase 2 will benefit from UseCases, but MVP doesn't justify the abstraction. The refactoring cost (1-2 weeks) is acceptable for 6+ month timeline until Phase 2.

---

### Trade-off 4: Room Database vs. Simplicity

**Gained**: Simpler data layer, less code to maintain
- No Room setup (entities, DAOs, migrations)
- No cache invalidation logic
- Rely on Coil's disk cache (proven, battle-tested)

**Lost**: Persistent photo metadata
- No "recently viewed" tracking
- No persistent cache of photo list (must rescan SMB on each app start)
- No offline metadata storage

**Justification**: MVP doesn't require offline mode or persistent metadata. Coil's disk cache is sufficient for image caching. If Phase 2 adds offline features, we can add Room then. For MVP, simplicity wins.

---

## Consequences

### Positive Consequences

#### 1. Fast Time-to-Market
- ✅ 2 modules instead of 8 saves 3-4 weeks setup time
- ✅ Skip UseCases saves 2-3 weeks implementation
- ✅ Standard libraries (Coil, jcifs-ng) avoid custom code
- **Result**: MVP implementable in 3-4 months by 2-3 developers

#### 2. Lower Complexity, Easier Maintenance
- ✅ 2 modules easier to navigate than 8 (less context-switching)
- ✅ Standard Android patterns (MVVM, Repository, Hilt) familiar to team
- ✅ Less custom code to debug (no connection pooling, custom cache)
- **Result**: Easier onboarding for new developers, faster debugging

#### 3. Proven Performance with Standard Libraries
- ✅ Coil is battle-tested (used by thousands of apps)
- ✅ jcifs-ng supports modern SMB 2.0-3.1.1 (good performance)
- ✅ WorkManager is battery-efficient and reliable
- **Result**: High confidence in meeting NFRs without custom optimization

#### 4. Testability Preserved
- ✅ `:core` module testable without Android emulator
- ✅ Repositories mockable for ViewModel unit tests
- ✅ Hilt facilitates dependency injection in tests
- **Result**: >80% unit test coverage achievable

#### 5. Extensibility for Phase 2
- ✅ Repository abstractions make adding UseCases easy (1-2 weeks)
- ✅ 2 modules allow adding `:cloud` module in Phase 2
- ✅ Standard patterns support cloud sync, offline mode
- **Result**: Phase 2 refactoring cost is manageable

---

### Negative Consequences

#### 1. Less Strict Architectural Boundaries
- ⚠️ No compile-time enforcement of layer boundaries (2 modules vs 8)
- ⚠️ Developers could accidentally violate patterns (e.g., ViewModel → DataSource)
- **Mitigation**: Code review discipline, architecture documentation, linting rules

#### 2. Potential Performance Gaps
- ⚠️ Standard SMB approach may not meet <2s NFR (need profiling to confirm)
- ⚠️ 4-photo buffer less resilient than 5 (network hiccups may cause stuttering)
- **Mitigation**: Profile early (Week 8), have optimization plan ready (connection pooling, increase buffer)

#### 3. Phase 2 Refactoring Required
- ⚠️ Adding UseCases in Phase 2 requires extracting logic from ViewModels (1-2 weeks)
- ⚠️ Adding Room for offline mode requires data layer changes
- **Mitigation**: Keep ViewModels focused (single responsibility), maintain clean repository abstractions

#### 4. ViewModels May Grow Large
- ⚠️ Business logic in ViewModels (not UseCases) may lead to large ViewModel classes
- ⚠️ Harder to reuse logic across multiple ViewModels
- **Mitigation**: Extract complex logic into repository methods, add UseCases when needed

---

### Neutral Consequences

#### 1. Team Consensus Followed
- ℹ️ 2 out of 3 architects agreed on most decisions (defer UseCases, standard SMB, 3-4 photo buffer)
- ℹ️ Final architecture is a synthesis, not one architect's pure vision
- **Result**: Balanced approach that addresses concerns from all perspectives

#### 2. MVP Philosophy Embraced
- ℹ️ "Build the simplest thing that works, then iterate based on data"
- ℹ️ Deferred optimizations to be added if profiling shows they're needed
- **Result**: Pragmatic approach for startup/MVP mindset

---

## Implementation Notes

### Risks

#### Risk 1: SMB Performance Slower Than Expected
- **Description**: Standard jcifs-ng may not meet <2s photo load NFR on slow networks or high-latency SMB servers
- **Likelihood**: Medium (depends on user's network/hardware)
- **Impact**: High (core NFR not met, slideshow feels laggy)
- **Mitigation**:
  1. Profile early (Week 8) on target hardware (tablet + real SMB share)
  2. If <2s NFR not met, add connection pooling (estimated 1 week implementation)
  3. If still not met, increase buffer to 5 photos (more read-ahead)
  4. If still not met, consider memory-mapped I/O or other optimizations

#### Risk 2: Memory Leaks During 24/7 Operation
- **Description**: Long-running coroutines or leaked references may cause memory leaks, leading to app crash after hours/days
- **Likelihood**: Medium (common issue in 24/7 apps)
- **Impact**: High (app unusable for primary use case)
- **Mitigation**:
  1. Use LeakCanary during development (detect leaks early)
  2. Stress test: Run slideshow for 24 hours, monitor memory with Android Profiler
  3. Use lifecycle-aware components (`viewModelScope`, `lifecycleScope`)
  4. Avoid capturing Activity/Fragment references in long-lived coroutines

#### Risk 3: Large Photo Libraries (10,000+) Slow to Scan
- **Description**: Recursive SMB directory scanning may take >5s for large libraries, poor UX on app start
- **Likelihood**: High (power users may have 10,000+ photos)
- **Impact**: Medium (annoying but not critical)
- **Mitigation**:
  1. Show progress indicator during scan ("Scanning library... X photos found")
  2. Cache photo list in DataStore (avoid rescanning on every start)
  3. Implement incremental scan (scan in background, start slideshow with first batch)
  4. Future: Parallel directory scanning (multiple coroutines)

#### Risk 4: jcifs-ng Compatibility Issues
- **Description**: jcifs-ng may have issues with certain SMB server types (old Samba versions, NAS-specific configurations)
- **Likelihood**: Low (jcifs-ng is mature and supports SMB 2.0-3.1.1)
- **Impact**: High (users can't connect to their SMB shares)
- **Mitigation**:
  1. Test with multiple SMB server types (Windows, Samba, Synology NAS, QNAP, etc.)
  2. Provide clear error messages (guide users to check SMB version, credentials)
  3. Support both SMB 2.0 and SMB 3.0 (configure in jcifs-ng properties)

#### Risk 5: WorkManager Scheduling Unreliable
- **Description**: WorkManager may not trigger at exact scheduled time (±15 min window), or may not trigger at all due to battery optimization
- **Likelihood**: Low (WorkManager is generally reliable)
- **Impact**: Medium (slideshow doesn't start/stop at scheduled time)
- **Mitigation**:
  1. Request "Ignore Battery Optimization" permission (ask user during setup)
  2. Add AlarmManager fallback (for precise timing if needed)
  3. Test on multiple devices with different battery optimization policies

---

### Dependencies

**External Libraries** (`:core` module):
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0`
- `eu.agno3.jcifs:jcifs-ng:2.1.9` (SMB client)
- `io.coil-kt:coil:2.5.0` (image loading)
- `androidx.datastore:datastore-preferences:1.0.0` (settings)
- `com.google.dagger:hilt-android:2.50` (DI)

**External Libraries** (`:app` module):
- `androidx.compose.ui:ui:1.6.0` (Compose UI)
- `androidx.compose.material3:material3:1.2.0` (Material Design)
- `androidx.activity:activity-compose:1.8.0`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0`
- `io.coil-kt:coil-compose:2.5.0` (Coil Compose integration)
- `androidx.work:work-runtime-ktx:2.9.0` (WorkManager)
- `androidx.navigation:navigation-compose:2.7.6` (Navigation)

**Build Tools**:
- Gradle 8.2.0
- Kotlin 1.9.0
- Android Gradle Plugin 8.2.0

---

### Assumptions

1. **Target hardware**: Modern Android tablet (8-10", 2560x1600 resolution, 4GB+ RAM, Android 10+)
2. **Photo library size**: 1,000-10,000 photos (typical user library)
3. **Network speed**: 100Mbps+ LAN (typical home network)
4. **SMB server**: Supports SMB 2.0+ (Windows 7+, modern Samba, NAS devices)
5. **User technical skill**: Can configure SMB URL, username, password (not complete beginner)
6. **Photo formats**: JPEG, PNG primarily (some GIF, BMP, WebP)
7. **Photo resolutions**: Mix of resolutions (some 4K, some 1080p, some lower)
8. **Phase 2 timeline**: 6+ months away (allows time for MVP feedback before refactoring)

---

## Validation Plan

How will we know if this architecture is successful?

### Performance Metrics (Week 8-9 Profiling)

- ✅ **Photo load time**: <2s (P95) from `nextPhoto()` call to bitmap rendered on screen
- ✅ **Transition frame rate**: 60fps (measure with GPU profiler, frame timing graph)
- ✅ **Memory usage**: <300MB total (measure over 24-hour stress test)
- ✅ **Startup time**: <3s from cold start to first photo displayed
- ✅ **SMB scan time**: <5s for 1,000 photos, <30s for 10,000 photos

**Measurement Tools**:
- Android Profiler (CPU, Memory, Network tabs)
- GPU Profiler (frame timing analysis)
- Custom instrumented tests (measure time between operations)

---

### Functional Validation (Week 11-12 Testing)

- ✅ All 12 user stories from PRD implemented and tested
- ✅ Slideshow plays continuously for 24 hours without crash
- ✅ SMB connection works with 3+ server types (Windows, Samba, Synology NAS)
- ✅ Settings persist across app restarts (DataStore working)
- ✅ Schedule starts/stops slideshow automatically (WorkManager working)
- ✅ Manual navigation (swipe left/right) works smoothly
- ✅ Error handling graceful (network disconnect, invalid credentials, empty library)

---

### Quality Validation

- ✅ **Unit test coverage**: >80% (measured with JaCoCo)
- ✅ **Integration test coverage**: >70%
- ✅ **UI test coverage**: Critical paths tested (slideshow playback, settings, schedule)
- ✅ **No memory leaks**: Verified with LeakCanary over 24-hour stress test
- ✅ **No ANR errors**: Verified with StrictMode + stress testing
- ✅ **Code review approval**: Senior developers review architecture and implementation

---

### User Acceptance Validation

- ✅ **Setup time**: <5 minutes from app install to first slideshow play
- ✅ **User feedback**: Survey 10+ beta testers, collect feedback on usability and reliability
- ✅ **Error clarity**: Users can understand and fix errors (e.g., "Check your SMB username/password")
- ✅ **24/7 reliability**: Beta testers report slideshow runs for days without intervention

---

## References

- **PRD**: `docs/features/photo-frame-app-initial/requirements/PRD_DRAFT.md`
- **Architecture Proposals**:
  - Modularity: `docs/features/photo-frame-app-initial/architecture/proposals/architect-1-modularity.md`
  - Performance: `docs/features/photo-frame-app-initial/architecture/proposals/architect-2-performance.md`
  - Simplicity: `docs/features/photo-frame-app-initial/architecture/proposals/architect-3-simplicity.md`
- **Proposal Comparison**: `docs/features/photo-frame-app-initial/architecture/PROPOSAL_COMPARISON.md`
- **Final Architecture**: `docs/features/photo-frame-app-initial/architecture/FINAL_ARCHITECTURE.md`
- **External References**:
  - [Android Architecture Samples](https://github.com/android/architecture-samples)
  - [Jetpack Compose Guide](https://developer.android.com/jetpack/compose)
  - [Hilt Dependency Injection](https://dagger.dev/hilt/)
  - [Coil Image Loading](https://coil-kt.github.io/coil/)
  - [jcifs-ng Documentation](https://github.com/AgNO3/jcifs-ng)

---

## Decision Makers

- **Synthesis Agent**: Primary decision maker (this ADR)
- **Input from**:
  - Architect 1 (Modularity focus)
  - Architect 2 (Performance focus)
  - Architect 3 (Simplicity focus)
- **Approval**: Coordinator (Phase 4 validation)

---

## Date

**2026-03-01**

---

## Revision History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-03-01 | Initial ADR (Phase 4 synthesis complete) | Synthesis Agent |

---

**END OF ARCHITECTURE DECISION RECORD**

This ADR documents all key architectural decisions, alternatives considered, trade-offs accepted, and consequences for the Digital Photo Frame MVP. It serves as the authoritative record of "why we built it this way" for future maintainers and Phase 2 developers.
