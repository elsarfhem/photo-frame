# Concurrency & Thread Safety Guidelines for Feature Implementation

## Purpose

This document provides guidelines for agents (Architects, Developers, Senior Devs) to ensure the **features they design and implement** are thread-safe, handle parallelism correctly, and avoid race conditions in the application code.

## For Architects: Design for Concurrency

When designing architecture, consider:

### 1. Identify Concurrent Operations

**Questions to Ask**:
- Will this feature run operations in parallel?
- Can multiple threads access this component simultaneously?
- Are there background tasks (network calls, database queries)?
- Will users trigger multiple operations before the first completes?

**Document in Architecture**:
```markdown
## Concurrency Considerations

### Concurrent Operations
- API calls execute on background threads (Dispatchers.IO)
- UI updates must happen on main thread
- Cache can be accessed from multiple threads

### Thread Safety Requirements
- Repository: Must be thread-safe (multiple ViewModels may access)
- Cache: Requires synchronization for concurrent reads/writes
- ViewModel: StateFlow ensures thread-safe state updates
```

### 2. Choose Thread-Safe Patterns

**Recommended Patterns**:

**Kotlin Coroutines** (Preferred for Android/Kotlin):
- Use `suspend` functions for async operations
- Use `Flow` for reactive streams
- Use `StateFlow`/`SharedFlow` for shared state
- Use appropriate dispatchers (IO, Main, Default)

**Immutable Data Structures**:
- Prefer `data class` with `val` properties
- Use `copy()` for updates
- Avoid mutable collections in shared state

**Synchronized Access**:
- Use `Mutex` for critical sections
- Use `AtomicReference` for single values
- Use `ConcurrentHashMap` for concurrent maps
- Use `synchronized` blocks when necessary

### 3. Design Data Flow for Thread Safety

```kotlin
// Example: Thread-safe data flow
//
// UI Thread          Background Thread
//     |                    |
//     v                    v
// ViewModel  -------->  Repository
//     |                    |
//     |                    v
//     |               API Client (IO)
//     |                    |
//     v                    v
// StateFlow <---------- Cache (synchronized)
//     |
//     v
//   UI Update (Main)
```

**Document Thread Boundaries**:
- Where does work move from main thread to background?
- Where does work move back to main thread?
- What data structures are shared across threads?

### 4. Prevent Race Conditions in Design

**Common Race Conditions to Avoid**:

**Check-Then-Act**:
```kotlin
// BAD - Race condition
if (!cache.contains(key)) {
    cache.put(key, fetchFromApi())
}

// GOOD - Atomic operation
cache.getOrPut(key) { fetchFromApi() }
```

**Read-Modify-Write**:
```kotlin
// BAD - Race condition
var count = counter.get()
count++
counter.set(count)

// GOOD - Atomic operation
counter.incrementAndGet()
```

**Multiple State Updates**:
```kotlin
// BAD - Not atomic
state.value = state.value.copy(loading = true)
state.value = state.value.copy(data = newData)

// GOOD - Single atomic update
state.value = state.value.copy(loading = true, data = newData)
```

## For Developers: Implement Thread-Safe Code

### 1. Use Coroutines Correctly

**Always Specify Dispatcher**:
```kotlin
class LoyaltyRepositoryImpl(
    private val api: LoyaltyApiClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getPoints(): Result<Points> = withContext(ioDispatcher) {
        // This runs on IO thread pool
        api.fetchPoints()
    }
}
```

**Don't Block Coroutines**:
```kotlin
// BAD - Blocks coroutine thread
suspend fun fetchData() = withContext(Dispatchers.IO) {
    Thread.sleep(1000) // Blocks thread!
    api.getData()
}

// GOOD - Suspends without blocking
suspend fun fetchData() = withContext(Dispatchers.IO) {
    delay(1000) // Suspends, doesn't block
    api.getData()
}
```

### 2. Protect Shared Mutable State

**Use Mutex for Critical Sections**:
```kotlin
class CacheImpl {
    private val mutex = Mutex()
    private val data = mutableMapOf<String, String>()

    suspend fun put(key: String, value: String) {
        mutex.withLock {
            data[key] = value
        }
    }

    suspend fun get(key: String): String? {
        mutex.withLock {
            return data[key]
        }
    }
}
```

**Use Atomic Types for Single Values**:
```kotlin
class Counter {
    private val count = AtomicInteger(0)

    fun increment(): Int = count.incrementAndGet()

    fun get(): Int = count.get()
}
```

**Use Thread-Safe Collections**:
```kotlin
// For concurrent access
private val cache = ConcurrentHashMap<String, Data>()

// Or protect with mutex
private val mutex = Mutex()
private val cache = mutableMapOf<String, Data>()
```

### 3. StateFlow for UI State

**Thread-Safe State Updates**:
```kotlin
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _state.value = UiState.Loading

            val result = withContext(Dispatchers.IO) {
                repository.getData()
            }

            _state.value = when {
                result.isSuccess -> UiState.Success(result.getOrNull()!!)
                else -> UiState.Error(result.exceptionOrNull()!!.message)
            }
        }
    }
}
```

**Why This is Thread-Safe**:
- StateFlow updates are atomic
- Only `_state` (private MutableStateFlow) can be modified
- Public `state` is read-only
- Collectors receive updates safely

### 4. Handle Concurrent API Calls

**Prevent Duplicate Requests**:
```kotlin
class Repository {
    private val activeRequests = ConcurrentHashMap<String, Deferred<Data>>()

    suspend fun getData(id: String): Data {
        // If request already in flight, wait for it
        val existing = activeRequests[id]
        if (existing != null && existing.isActive) {
            return existing.await()
        }

        // Start new request
        val deferred = coroutineScope.async(Dispatchers.IO) {
            api.fetchData(id)
        }

        activeRequests[id] = deferred

        try {
            return deferred.await()
        } finally {
            activeRequests.remove(id)
        }
    }
}
```

**Debounce User Actions**:
```kotlin
class SearchViewModel : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    init {
        searchQuery
            .debounce(300) // Wait 300ms after last input
            .distinctUntilChanged() // Only if query changed
            .mapLatest { query ->
                searchRepository.search(query)
            }
            .collect { results ->
                _searchResults.value = results
            }
    }
}
```

### 5. Proper Error Handling in Concurrent Code

**Handle Cancellation**:
```kotlin
suspend fun fetchWithRetry(): Result<Data> {
    return try {
        withContext(Dispatchers.IO) {
            api.fetchData()
        }
    } catch (e: CancellationException) {
        // Don't catch cancellation - let it propagate
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Timeout for Long Operations**:
```kotlin
suspend fun fetchWithTimeout(): Result<Data> {
    return try {
        withTimeout(5000) { // 5 second timeout
            api.fetchData()
        }
    } catch (e: TimeoutCancellationException) {
        Result.failure(TimeoutException("Request timed out"))
    }
}
```

## For Senior Devs: Review for Concurrency Issues

### Code Review Checklist

**Thread Safety**:
- [ ] Are mutable variables properly synchronized?
- [ ] Are collections thread-safe or protected?
- [ ] Are state updates atomic?
- [ ] Is shared state minimized?

**Coroutine Usage**:
- [ ] Are appropriate dispatchers used?
- [ ] Are blocking calls wrapped in `withContext(Dispatchers.IO)`?
- [ ] Is `delay()` used instead of `Thread.sleep()`?
- [ ] Are cancellations handled correctly?

**Race Conditions**:
- [ ] No check-then-act patterns without synchronization
- [ ] No read-modify-write patterns without atomicity
- [ ] No assumptions about operation ordering
- [ ] No shared mutable state without protection

**Deadlocks**:
- [ ] No nested lock acquisition
- [ ] No blocking operations while holding locks
- [ ] Proper lock ordering if multiple locks needed

**Performance**:
- [ ] Are operations appropriately parallelized?
- [ ] Are unnecessary synchronizations avoided?
- [ ] Are coroutines preferred over threads?
- [ ] Is work appropriately distributed across threads?

### Common Anti-Patterns to Flag

**1. Mutable State Without Synchronization**:
```kotlin
// BAD
class Cache {
    private val data = mutableMapOf<String, String>()

    fun put(key: String, value: String) {
        data[key] = value // Not thread-safe!
    }
}
```

**2. Blocking Main Thread**:
```kotlin
// BAD
fun onClick() {
    val data = repository.fetchData() // Blocks UI!
    updateUI(data)
}

// GOOD
fun onClick() {
    viewModelScope.launch {
        val data = withContext(Dispatchers.IO) {
            repository.fetchData()
        }
        updateUI(data)
    }
}
```

**3. Shared ViewModel State Without Flow**:
```kotlin
// BAD
class ViewModel {
    var state: UiState = UiState.Loading // Not thread-safe!
}

// GOOD
class ViewModel {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
}
```

**4. Callback Hell with Threads**:
```kotlin
// BAD
Thread {
    val data1 = api.fetch1()
    Thread {
        val data2 = api.fetch2()
        runOnUiThread {
            updateUI(data1, data2)
        }
    }.start()
}.start()

// GOOD
viewModelScope.launch {
    val data1 = async(Dispatchers.IO) { api.fetch1() }
    val data2 = async(Dispatchers.IO) { api.fetch2() }
    updateUI(data1.await(), data2.await())
}
```

## Backend Concurrency Considerations

### For Backend Services (Node.js, Java, Go)

**Node.js (Event Loop)**:
- Don't block event loop with CPU-intensive tasks
- Use worker threads for heavy computation
- Use async/await correctly
- Handle Promise rejections

**Java/Spring Boot (Thread Pools)**:
- Use `@Async` for background tasks
- Configure thread pools appropriately
- Use `CompletableFuture` for async operations
- Protect shared resources with locks

**Go (Goroutines)**:
- Use channels for communication
- Use sync.Mutex for shared state
- Avoid sharing memory, communicate via channels
- Use context for cancellation

## Testing Concurrent Code

### Unit Tests for Thread Safety

**Test with Multiple Threads**:
```kotlin
@Test
fun `cache is thread-safe under concurrent access`() = runBlocking {
    val cache = CacheImpl()
    val jobs = List(100) { index ->
        launch {
            cache.put("key$index", "value$index")
        }
    }

    jobs.joinAll()

    // Verify all 100 items were stored
    assertEquals(100, cache.size())
}
```

**Test for Race Conditions**:
```kotlin
@Test
fun `counter increments correctly under concurrent access`() = runBlocking {
    val counter = Counter()
    val jobs = List(1000) {
        launch {
            counter.increment()
        }
    }

    jobs.joinAll()

    // If not thread-safe, count will be < 1000
    assertEquals(1000, counter.get())
}
```

**Test Cancellation**:
```kotlin
@Test
fun `operation cancels correctly`() = runBlocking {
    val job = launch {
        repository.longRunningOperation()
    }

    delay(100)
    job.cancel()

    // Verify operation was cancelled
    assertTrue(job.isCancelled)
}
```

## Documentation Requirements

Architects and Developers should document:

**In Architecture Doc**:
- Which components are accessed concurrently
- Thread safety guarantees
- Dispatcher usage
- Synchronization mechanisms

**In Code Comments**:
```kotlin
/**
 * Thread-safe cache implementation.
 *
 * Concurrency: Protected by Mutex for all read/write operations.
 * Can be safely accessed from multiple coroutines.
 */
class CacheImpl {
    private val mutex = Mutex()
    // ...
}
```

## Summary

### Key Principles

1. **Design for Concurrency**: Architects identify concurrent operations early
2. **Use Modern Primitives**: Coroutines > Threads, Flow > Callbacks
3. **Protect Shared State**: Mutex, Atomic types, or avoid sharing
4. **Document Thread Safety**: Be explicit about guarantees
5. **Test Concurrency**: Write tests that exercise concurrent access
6. **Review Thoroughly**: Senior devs check for race conditions

### Remember

- **Concurrency bugs are hard to reproduce** - get it right the first time
- **Prefer immutability** - immutable data can't have race conditions
- **Minimize shared state** - less sharing = fewer problems
- **Use appropriate tools** - coroutines, flows, actors for different scenarios
- **Test under load** - race conditions appear under concurrent access

This ensures the features you build are robust, thread-safe, and handle parallelism correctly.
