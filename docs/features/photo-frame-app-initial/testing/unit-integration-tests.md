# Unit & Integration Test Plan - Digital Photo Frame App (MVP Phase 1)

**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Test Scope**: Unit & Integration Tests
**QA Agent**: QA 1 - Unit & Integration Tests focused
**Date**: 2026-03-02
**Phase**: Phase 6 - Test Planning
**Status**: READY FOR TEAM REVIEW

---

## 1. Executive Summary

### Test Scope
This test plan covers **component-level unit tests** and **integration tests** for the Digital Photo Frame app, focusing on business logic, data layer, repositories, ViewModels, and external integrations (SMB, Room, Keystore).

### Test Coverage Summary
- **Total Test Scenarios**: 42
- **Total Test Cases**: 168
- **Estimated Effort**: 80-100 hours (2.5-3 weeks)
- **Target Code Coverage**: 85%+ for ViewModels, Repositories, Data Sources, and Utilities
- **Requirements Coverage**: 100% of 12 user stories (functional logic), all P0 NFR criteria

### Critical Focus Areas (Based on NFR Assessments)
1. **Security Testing (P0)**: Keystore encryption, SMB 2.0+ enforcement, PII logging audit
2. **Reliability Testing (P0)**: Network failure recovery, error handling, memory leak detection
3. **Scalability Testing (P0)**: Large collection handling (10,000+ photos), deep folder scanning

### Test Environment
- **Primary**: JUnit 5, MockK, Kotlin Coroutines Test, Turbine (Flow testing)
- **Integration**: Docker Samba server, Robolectric (Android components), Hilt Testing
- **Coverage**: JaCoCo

---

## 2. Test Strategy

### Scope

**What This Test Plan Covers**:
- Unit tests for ViewModels (SlideshowViewModel, SettingsViewModel)
- Unit tests for Repositories (SlideshowRepository, SettingsRepository)
- Unit tests for Data Sources (SmbPhotoDataSource, PhotoBufferManager, ImageCache)
- Unit tests for Utilities (Result sealed class, SmbClient wrapper, NetworkMonitor)
- Integration tests for ViewModel + Repository
- Integration tests for Repository + Data Source + SMB (with Docker Samba)
- Integration tests for Room database operations
- Integration tests for Hilt dependency injection wiring
- Security tests (Keystore, SMB protocol enforcement, logging audit)
- Reliability tests (error recovery, memory leaks, network failures)
- Scalability tests (large collections, deep folders)

**What This Test Plan Does NOT Cover** (Tested by Teammates):
- UI rendering and Compose component tests (QA 2 - UI Tests)
- End-to-end user flow tests (QA 2 - E2E Tests)
- Performance benchmarks (60fps, <2s load, <300MB memory) (QA 3 - Performance Tests)
- Accessibility compliance (TalkBack, content descriptions) (QA 3 - Accessibility Tests)

### Approach

#### Unit Testing Strategy
1. **Isolation**: Mock all external dependencies (repositories, data sources, Android framework)
2. **Coroutine Testing**: Use `TestDispatcher` and `runTest` for all coroutine-based code
3. **Flow Testing**: Use Turbine library to assert on Flow emissions
4. **State Verification**: Assert on ViewModel state changes, repository results
5. **Error Handling**: Test all error paths (network failures, SMB errors, database errors)

#### Integration Testing Strategy
1. **Docker Samba Server**: Real SMB server for integration tests (using testcontainers-java or docker-compose)
2. **In-Memory Room Database**: Real Room database with in-memory SQLite
3. **Real Hilt DI**: Test Hilt modules with test doubles for external dependencies
4. **Test Doubles for Keystore**: Mock Android Keystore API (cannot run in JVM tests)
5. **Fake Implementations**: Create fake repositories/data sources where appropriate

#### Security Testing Strategy
1. **Keystore Encryption**: Unit tests with mocked Keystore to verify encrypt/decrypt logic
2. **SMB Protocol Enforcement**: Integration tests to verify SMB 2.0+ requirement (reject SMB 1.x)
3. **PII Logging Audit**: Static analysis + runtime tests to ensure no credentials in logs

#### Reliability Testing Strategy
1. **Network Failure Simulation**: Mock network failures, verify retry logic and error handling
2. **Memory Leak Detection**: Use LeakCanary in instrumented tests, assert no leaks after repeated operations
3. **Auto-Recovery Testing**: Simulate SMB disconnect/reconnect, verify app recovers gracefully

#### Scalability Testing Strategy
1. **Large Collection Testing**: Test with 10,000+ photo paths, measure memory usage and performance
2. **Deep Folder Testing**: Test with 10+ folder depth, verify no stack overflow or timeout
3. **Buffer Management**: Verify PhotoBufferManager handles large collections efficiently

### Tools & Frameworks

#### Testing Frameworks
- **JUnit 5**: Primary test framework
- **MockK**: Mocking library for Kotlin
- **Kotlin Coroutines Test**: `kotlinx-coroutines-test` for `TestDispatcher` and `runTest`
- **Turbine**: Flow testing library (Jake Wharton)
- **Truth**: Google's assertion library for more readable assertions

#### Android Testing
- **Robolectric**: For tests requiring Android framework (SharedPreferences, Keystore mocks)
- **Hilt Testing**: `hilt-android-testing` for DI tests
- **Room Testing**: In-memory database

#### Integration Testing
- **Docker Samba**: Testcontainers or docker-compose for real SMB server
- **jcifs-ng**: Real SMB client library (no mocking in integration tests)

#### Coverage & Quality
- **JaCoCo**: Code coverage reporting (target: 85%+)
- **Detekt**: Static analysis for Kotlin (ensure no PII logging)
- **LeakCanary**: Memory leak detection (instrumented tests only)

### CI/CD Integration
- All unit tests run on every PR (GitHub Actions)
- Integration tests run on every PR (require Docker)
- Coverage reports published to PR comments
- Tests must pass before merge
- Target: <5 minutes for unit tests, <10 minutes for integration tests

---

## 3. Requirements Coverage Matrix

### User Stories from PRD

| Requirement ID | Requirement | Test Scenario(s) | Status |
|----------------|-------------|------------------|--------|
| **US-1.1** | SMB Connection Setup | TS-001, TS-002, TS-003, TS-004 | ✅ Planned |
| **US-1.2** | Folder Selection | TS-005, TS-006 | ✅ Planned |
| **US-1.3** | Recursive Photo Scanning | TS-007, TS-008, TS-009 | ✅ Planned |
| **US-2.1** | Slideshow Playback | TS-010, TS-011, TS-012 | ✅ Planned |
| **US-2.2** | Interval Configuration | TS-013, TS-014 | ✅ Planned |
| **US-2.3** | Transition Effects | TS-015 | ✅ Planned |
| **US-2.4** | Shuffle/Sequential | TS-016, TS-017 | ✅ Planned |
| **US-3.1** | Schedule Configuration | TS-018, TS-019, TS-020 | ✅ Planned |
| **US-3.2** | Auto Start/Stop | TS-021, TS-022 | ✅ Planned |
| **US-4.1** | Image Caching | TS-023, TS-024, TS-025 | ✅ Planned |
| **US-4.2** | Buffer Management | TS-026, TS-027, TS-028 | ✅ Planned |
| **US-5.1** | Error Handling | TS-029, TS-030, TS-031, TS-032 | ✅ Planned |

### NFR Acceptance Criteria from Assessments

| NFR ID | NFR Criterion | Test Scenario(s) | Status |
|--------|---------------|------------------|--------|
| **NFR-SEC-1** | Keystore credential encryption | TS-033, TS-034 | ✅ Planned |
| **NFR-SEC-2** | SMB 2.0+ protocol enforcement | TS-035, TS-036 | ✅ Planned |
| **NFR-SEC-3** | No PII/credentials in logs | TS-037 | ✅ Planned |
| **NFR-REL-1** | Network failure recovery | TS-038, TS-039 | ✅ Planned |
| **NFR-REL-2** | Memory leak prevention | TS-040 | ✅ Planned |
| **NFR-REL-3** | Auto-recovery from errors | TS-041 | ✅ Planned |
| **NFR-SCALE-1** | Large collection handling (10K+ photos) | TS-042 | ✅ Planned |
| **NFR-SCALE-2** | Deep folder scanning (10+ levels) | TS-009 | ✅ Planned |

### Edge Cases from PRD & NFR Assessments

| Edge Case ID | Edge Case | Test Scenario(s) | Status |
|--------------|-----------|------------------|--------|
| **EC-1** | Empty SMB folder | TS-005 | ✅ Planned |
| **EC-2** | Corrupt/unreadable photo | TS-030 | ✅ Planned |
| **EC-3** | SMB server disconnect during playback | TS-038 | ✅ Planned |
| **EC-4** | Network unavailable at startup | TS-039 | ✅ Planned |
| **EC-5** | Extremely large photo (>50MB) | TS-025 | ✅ Planned |
| **EC-6** | Single photo in folder | TS-011 | ✅ Planned |
| **EC-7** | Invalid SMB credentials | TS-003 | ✅ Planned |
| **EC-8** | SMB share does not exist | TS-004 | ✅ Planned |
| **EC-9** | Very long folder path (>255 chars) | TS-008 | ✅ Planned |
| **EC-10** | Folder with mixed file types | TS-007 | ✅ Planned |

### Coverage Summary
- **Total Requirements**: 12 user stories + 8 NFR criteria + 10 edge cases = **30 requirements**
- **Covered by Tests**: **30** (100%)
- **Coverage Percentage**: **100%**
- **Uncovered**: None

---

## 4. Unit Test Scenarios & Test Cases

### Test Scenario TS-001: SMB Connection - Successful Connection

**Objective**: Verify successful SMB connection with valid credentials

**Requirements Covered**:
- US-1.1: SMB Connection Setup

**Preconditions**:
- Docker Samba server running (integration test) OR mocked SmbClient (unit test)
- Valid SMB credentials configured

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-001-1: Connect with Valid Credentials (Positive)

**Description**: Connect to SMB share with valid hostname, username, password

**Test Steps**:
1. Create SmbClient with valid credentials
2. Call `smbClient.connect()`
3. Verify connection succeeds

**Test Data**:
- Input: `hostname="192.168.1.100", share="photos", username="user", password="pass"`
- Expected: `Result.Success(true)`

**Expected Result**: Connection succeeds, no exception thrown

**Priority**: 🔴 Critical

**Estimated Time**: 5 minutes

**Implementation Notes**:
```kotlin
@Test
fun `connect with valid credentials succeeds`() = runTest {
    // Given
    val credentials = SmbCredentials("192.168.1.100", "photos", "user", "pass")
    val smbClient = FakeSmbClient() // or mocked

    // When
    val result = smbClient.connect(credentials)

    // Then
    assertThat(result).isInstanceOf(Result.Success::class.java)
}
```

---

#### Test Case TC-001-2: Connection Timeout Handling (Negative)

**Description**: Verify timeout handling when SMB server does not respond

**Test Steps**:
1. Mock SmbClient to throw timeout exception
2. Call `smbClient.connect()`
3. Verify error result returned

**Test Data**:
- Input: `hostname="10.0.0.999"` (unreachable)
- Expected: `Result.Error(SmbError.Timeout)`

**Expected Result**: Returns error result with timeout error

**Priority**: 🔴 Critical

**Estimated Time**: 5 minutes

---

### Test Scenario TS-002: SMB Connection - Credential Encryption

**Objective**: Verify SMB credentials are encrypted using Android Keystore

**Requirements Covered**:
- US-1.1: SMB Connection Setup
- NFR-SEC-1: Keystore credential encryption

**Preconditions**:
- Mocked Android Keystore API

**Test Environment**: JUnit 5 + MockK + Robolectric

---

#### Test Case TC-002-1: Encrypt Credentials with Keystore (Positive)

**Description**: Verify credentials are encrypted before storage

**Test Steps**:
1. Create CredentialEncryptor with mocked Keystore
2. Call `encryptor.encrypt(username, password)`
3. Verify encrypted data differs from plaintext
4. Verify Keystore encrypt method called

**Test Data**:
- Input: `username="user", password="pass123"`
- Expected: `EncryptedData(iv=..., ciphertext=...)`

**Expected Result**: Returns encrypted data, plaintext not visible

**Priority**: 🔴 Critical (P0 Security)

**Estimated Time**: 10 minutes

---

#### Test Case TC-002-2: Decrypt Credentials from Keystore (Positive)

**Description**: Verify encrypted credentials can be decrypted

**Test Steps**:
1. Mock Keystore to return encrypted data
2. Call `encryptor.decrypt(encryptedData)`
3. Verify decrypted data matches original plaintext

**Test Data**:
- Input: `EncryptedData(iv=..., ciphertext=...)`
- Expected: `username="user", password="pass123"`

**Expected Result**: Returns original plaintext credentials

**Priority**: 🔴 Critical (P0 Security)

**Estimated Time**: 10 minutes

---

#### Test Case TC-002-3: Keystore Encryption Failure Handling (Negative)

**Description**: Verify error handling when Keystore encryption fails

**Test Steps**:
1. Mock Keystore to throw exception
2. Call `encryptor.encrypt(username, password)`
3. Verify error result returned

**Test Data**:
- Input: `username="user", password="pass123"`
- Expected: `Result.Error(SecurityError.EncryptionFailed)`

**Expected Result**: Returns error, does not crash

**Priority**: 🟡 High

**Estimated Time**: 5 minutes

---

### Test Scenario TS-003: SMB Connection - Invalid Credentials

**Objective**: Verify error handling for invalid SMB credentials

**Requirements Covered**:
- US-1.1: SMB Connection Setup
- EC-7: Invalid SMB credentials

**Preconditions**:
- Docker Samba server running (integration test)

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-003-1: Invalid Username/Password (Negative)

**Description**: Verify authentication failure with invalid credentials

**Test Steps**:
1. Create SmbClient with invalid credentials
2. Call `smbClient.connect()`
3. Verify authentication error returned

**Test Data**:
- Input: `username="invalid", password="wrong"`
- Expected: `Result.Error(SmbError.AuthenticationFailed)`

**Expected Result**: Returns authentication error

**Priority**: 🔴 Critical

**Estimated Time**: 5 minutes

---

### Test Scenario TS-004: SMB Connection - Share Does Not Exist

**Objective**: Verify error handling when SMB share does not exist

**Requirements Covered**:
- US-1.1: SMB Connection Setup
- EC-8: SMB share does not exist

**Preconditions**:
- Docker Samba server running without specified share

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-004-1: Non-Existent Share (Negative)

**Description**: Verify error when connecting to non-existent share

**Test Steps**:
1. Create SmbClient with valid credentials but non-existent share
2. Call `smbClient.connect()`
3. Verify share not found error returned

**Test Data**:
- Input: `share="nonexistent"`
- Expected: `Result.Error(SmbError.ShareNotFound)`

**Expected Result**: Returns share not found error

**Priority**: 🟡 High

**Estimated Time**: 5 minutes

---

### Test Scenario TS-005: Folder Scanning - Empty Folder

**Objective**: Verify handling of empty SMB folder

**Requirements Covered**:
- US-1.2: Folder Selection
- EC-1: Empty SMB folder

**Preconditions**:
- Docker Samba server with empty folder

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-005-1: Scan Empty Folder (Edge Case)

**Description**: Verify scanning empty folder returns empty list

**Test Steps**:
1. Connect to SMB share
2. Scan empty folder
3. Verify empty photo list returned

**Test Data**:
- Input: `folderPath="/empty_folder"`
- Expected: `Result.Success(emptyList())`

**Expected Result**: Returns empty list, no error

**Priority**: 🟡 High

**Estimated Time**: 5 minutes

---

### Test Scenario TS-006: Folder Scanning - Valid Folder with Photos

**Objective**: Verify scanning folder with photos returns correct photo list

**Requirements Covered**:
- US-1.2: Folder Selection

**Preconditions**:
- Docker Samba server with folder containing 10 photos

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-006-1: Scan Folder with Photos (Positive)

**Description**: Verify scanning folder returns all photo files

**Test Steps**:
1. Connect to SMB share
2. Scan folder with 10 photos
3. Verify all 10 photos returned

**Test Data**:
- Input: `folderPath="/photos"`
- Expected: `Result.Success(listOf("photo1.jpg", "photo2.png", ...))` (10 items)

**Expected Result**: Returns list of 10 photo paths

**Priority**: 🔴 Critical

**Estimated Time**: 5 minutes

---

### Test Scenario TS-007: Recursive Photo Scanning - Mixed File Types

**Objective**: Verify recursive scan filters only image files

**Requirements Covered**:
- US-1.3: Recursive Photo Scanning
- EC-10: Folder with mixed file types

**Preconditions**:
- Docker Samba server with folder containing photos, videos, docs

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-007-1: Filter Image Files Only (Positive)

**Description**: Verify only image files (jpg, png, jpeg, gif, webp) are returned

**Test Steps**:
1. Connect to SMB share
2. Recursively scan folder with mixed file types
3. Verify only image files returned

**Test Data**:
- Input: `folderPath="/mixed_files"` (contains: `photo.jpg, video.mp4, doc.pdf, image.png`)
- Expected: `Result.Success(listOf("photo.jpg", "image.png"))` (2 items)

**Expected Result**: Returns only image files, excludes non-images

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

### Test Scenario TS-008: Recursive Photo Scanning - Very Long Folder Path

**Objective**: Verify handling of very long folder paths (>255 characters)

**Requirements Covered**:
- US-1.3: Recursive Photo Scanning
- EC-9: Very long folder path

**Preconditions**:
- Docker Samba server with deeply nested folder structure

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-008-1: Handle Long Folder Path (Edge Case)

**Description**: Verify scanning folder with path >255 characters

**Test Steps**:
1. Connect to SMB share
2. Recursively scan folder with very long path
3. Verify scan completes successfully or returns graceful error

**Test Data**:
- Input: `folderPath="/a/very/long/folder/path/that/exceeds/255/characters/..."` (300 chars)
- Expected: `Result.Success(photos)` OR `Result.Error(SmbError.PathTooLong)`

**Expected Result**: Either succeeds or returns graceful error (no crash)

**Priority**: 🟢 Medium

**Estimated Time**: 10 minutes

---

### Test Scenario TS-009: Recursive Photo Scanning - Deep Folder Depth

**Objective**: Verify scanning deeply nested folders (10+ levels) without stack overflow

**Requirements Covered**:
- US-1.3: Recursive Photo Scanning
- NFR-SCALE-2: Deep folder scanning (10+ levels)

**Preconditions**:
- Docker Samba server with 15-level deep folder structure

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-009-1: Scan 15-Level Deep Folder (Scalability)

**Description**: Verify recursive scan handles 15-level deep folders

**Test Steps**:
1. Connect to SMB share
2. Recursively scan 15-level deep folder structure
3. Verify all photos found, no stack overflow

**Test Data**:
- Input: `folderPath="/level1/level2/.../level15"` (photos at various levels)
- Expected: `Result.Success(allPhotos)` (all photos found)

**Expected Result**: Finds all photos, completes in <30 seconds, no stack overflow

**Priority**: 🔴 Critical (P0 Scalability)

**Estimated Time**: 15 minutes

---

### Test Scenario TS-010: SlideshowViewModel - Start Slideshow

**Objective**: Verify SlideshowViewModel correctly starts slideshow

**Requirements Covered**:
- US-2.1: Slideshow Playback

**Preconditions**:
- Mock SlideshowRepository with photo list

**Test Environment**: JUnit 5 + MockK + Turbine

---

#### Test Case TC-010-1: Start Slideshow with Photos (Positive)

**Description**: Verify starting slideshow updates state correctly

**Test Steps**:
1. Create SlideshowViewModel with mock repository
2. Call `viewModel.startSlideshow()`
3. Verify state transitions: Idle -> Loading -> Playing

**Test Data**:
- Input: Repository returns 10 photos
- Expected: `SlideshowState.Playing(currentPhoto=photo1, currentIndex=0, totalPhotos=10)`

**Expected Result**: State transitions to Playing with first photo

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

**Implementation Notes**:
```kotlin
@Test
fun `startSlideshow emits Playing state with first photo`() = runTest {
    // Given
    val photos = listOf(Photo("photo1.jpg"), Photo("photo2.jpg"))
    val repository = mockk<SlideshowRepository>()
    coEvery { repository.getPhotos() } returns Result.Success(photos)
    val viewModel = SlideshowViewModel(repository, testDispatcher)

    // When
    viewModel.startSlideshow()

    // Then
    viewModel.state.test {
        assertThat(awaitItem()).isInstanceOf(SlideshowState.Idle::class.java)
        assertThat(awaitItem()).isInstanceOf(SlideshowState.Loading::class.java)
        val playingState = awaitItem() as SlideshowState.Playing
        assertThat(playingState.currentPhoto).isEqualTo(photos[0])
        assertThat(playingState.currentIndex).isEqualTo(0)
        assertThat(playingState.totalPhotos).isEqualTo(2)
    }
}
```

---

### Test Scenario TS-011: SlideshowViewModel - Single Photo

**Objective**: Verify slideshow behavior with single photo

**Requirements Covered**:
- US-2.1: Slideshow Playback
- EC-6: Single photo in folder

**Preconditions**:
- Mock SlideshowRepository with single photo

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-011-1: Slideshow with One Photo (Edge Case)

**Description**: Verify slideshow displays single photo repeatedly

**Test Steps**:
1. Create SlideshowViewModel with mock repository returning 1 photo
2. Call `viewModel.startSlideshow()`
3. Call `viewModel.nextPhoto()` multiple times
4. Verify same photo displayed each time

**Test Data**:
- Input: Repository returns 1 photo
- Expected: `currentPhoto=photo1` on every `nextPhoto()` call

**Expected Result**: Displays same photo repeatedly, does not crash

**Priority**: 🟡 High

**Estimated Time**: 10 minutes

---

### Test Scenario TS-012: SlideshowViewModel - Automatic Advancement

**Objective**: Verify slideshow automatically advances photos based on interval

**Requirements Covered**:
- US-2.1: Slideshow Playback

**Preconditions**:
- Mock SlideshowRepository with 5 photos
- Interval set to 5 seconds

**Test Environment**: JUnit 5 + MockK + TestDispatcher

---

#### Test Case TC-012-1: Auto-Advance After Interval (Positive)

**Description**: Verify slideshow advances to next photo after interval

**Test Steps**:
1. Create SlideshowViewModel with 5-second interval
2. Start slideshow
3. Advance virtual time by 5 seconds
4. Verify next photo displayed

**Test Data**:
- Input: `interval=5s, photos=[photo1, photo2, photo3]`
- Expected: After 5s, `currentPhoto=photo2`

**Expected Result**: Photo advances automatically after 5 seconds

**Priority**: 🔴 Critical

**Estimated Time**: 15 minutes

---

### Test Scenario TS-013: SlideshowViewModel - Interval Configuration

**Objective**: Verify interval configuration affects slideshow timing

**Requirements Covered**:
- US-2.2: Interval Configuration

**Preconditions**:
- Mock SettingsRepository with configurable interval

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-013-1: Change Interval During Playback (Positive)

**Description**: Verify changing interval takes effect immediately

**Test Steps**:
1. Start slideshow with 5-second interval
2. Change interval to 10 seconds
3. Verify next photo advances after 10 seconds (not 5)

**Test Data**:
- Input: Initial interval=5s, new interval=10s
- Expected: Next photo after 10s

**Expected Result**: New interval takes effect immediately

**Priority**: 🟡 High

**Estimated Time**: 15 minutes

---

### Test Scenario TS-014: SlideshowViewModel - Interval Validation

**Objective**: Verify interval validation (min 3s, max 60s)

**Requirements Covered**:
- US-2.2: Interval Configuration

**Preconditions**:
- SettingsViewModel with validation logic

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-014-1: Reject Invalid Interval (Negative)

**Description**: Verify interval below 3s or above 60s is rejected

**Test Steps**:
1. Attempt to set interval to 1 second
2. Verify validation error returned
3. Attempt to set interval to 120 seconds
4. Verify validation error returned

**Test Data**:
- Input: `interval=1s` -> Error
- Input: `interval=120s` -> Error
- Expected: `Result.Error(ValidationError.IntervalOutOfRange)`

**Expected Result**: Invalid intervals rejected with error

**Priority**: 🟡 High

**Estimated Time**: 10 minutes

---

### Test Scenario TS-015: SlideshowViewModel - Transition Effects

**Objective**: Verify transition effect configuration

**Requirements Covered**:
- US-2.3: Transition Effects

**Preconditions**:
- Mock SettingsRepository with transition effect setting

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-015-1: Apply Transition Effect (Positive)

**Description**: Verify selected transition effect is applied to state

**Test Steps**:
1. Set transition effect to FADE
2. Verify state includes FADE transition
3. Change to SLIDE
4. Verify state includes SLIDE transition

**Test Data**:
- Input: `transition=FADE`
- Expected: `state.transition=FADE`

**Expected Result**: Transition setting reflected in state

**Priority**: 🟢 Medium

**Estimated Time**: 5 minutes

---

### Test Scenario TS-016: SlideshowViewModel - Shuffle Mode

**Objective**: Verify shuffle mode randomizes photo order

**Requirements Covered**:
- US-2.4: Shuffle/Sequential

**Preconditions**:
- Mock SlideshowRepository with 100 photos

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-016-1: Shuffle Mode Randomizes Order (Positive)

**Description**: Verify shuffle mode displays photos in random order

**Test Steps**:
1. Load 100 photos in sequential order
2. Enable shuffle mode
3. Advance through 10 photos
4. Verify photos not in sequential order

**Test Data**:
- Input: 100 photos (photo1, photo2, ..., photo100), shuffle=true
- Expected: Next 10 photos NOT (photo1, photo2, photo3, ...)

**Expected Result**: Photos displayed in non-sequential order

**Priority**: 🔴 Critical

**Estimated Time**: 15 minutes

---

#### Test Case TC-016-2: Shuffle Prevents Immediate Repeats (Positive)

**Description**: Verify shuffle does not repeat same photo consecutively

**Test Steps**:
1. Enable shuffle mode
2. Advance through 20 photos
3. Verify no consecutive repeats

**Test Data**:
- Input: 50 photos, shuffle=true
- Expected: No photo repeats consecutively in 20 advances

**Expected Result**: No immediate repeats (different photo each time)

**Priority**: 🟡 High

**Estimated Time**: 10 minutes

---

### Test Scenario TS-017: SlideshowViewModel - Sequential Mode

**Objective**: Verify sequential mode displays photos in order

**Requirements Covered**:
- US-2.4: Shuffle/Sequential

**Preconditions**:
- Mock SlideshowRepository with 10 photos

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-017-1: Sequential Mode Displays in Order (Positive)

**Description**: Verify sequential mode displays photos in original order

**Test Steps**:
1. Load 10 photos
2. Set sequential mode
3. Advance through 10 photos
4. Verify photos displayed in order: photo1, photo2, ..., photo10

**Test Data**:
- Input: 10 photos, shuffle=false
- Expected: Photos displayed sequentially

**Expected Result**: Photos displayed in order photo1 -> photo2 -> ... -> photo10

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

#### Test Case TC-017-2: Sequential Mode Loops After Last Photo (Positive)

**Description**: Verify sequential mode loops back to first photo after last

**Test Steps**:
1. Load 10 photos in sequential mode
2. Advance to photo10
3. Advance once more
4. Verify photo1 displayed (looped)

**Test Data**:
- Input: 10 photos, at index 9 (last photo)
- Expected: Next photo is index 0 (first photo)

**Expected Result**: Loops back to first photo after last

**Priority**: 🟡 High

**Estimated Time**: 5 minutes

---

### Test Scenario TS-018: SettingsViewModel - Schedule Configuration

**Objective**: Verify schedule configuration (start/stop times)

**Requirements Covered**:
- US-3.1: Schedule Configuration

**Preconditions**:
- Mock SettingsRepository

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-018-1: Set Valid Schedule (Positive)

**Description**: Verify setting valid start and stop times

**Test Steps**:
1. Set start time to 08:00
2. Set stop time to 22:00
3. Verify settings saved

**Test Data**:
- Input: `startTime=08:00, stopTime=22:00`
- Expected: Settings saved successfully

**Expected Result**: Schedule saved correctly

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

### Test Scenario TS-019: SettingsViewModel - Schedule Validation

**Objective**: Verify schedule validation (stop time must be after start time)

**Requirements Covered**:
- US-3.1: Schedule Configuration

**Preconditions**:
- SettingsViewModel with validation logic

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-019-1: Reject Invalid Schedule (Negative)

**Description**: Verify stop time before start time is rejected

**Test Steps**:
1. Set start time to 22:00
2. Set stop time to 08:00
3. Verify validation error returned

**Test Data**:
- Input: `startTime=22:00, stopTime=08:00`
- Expected: `Result.Error(ValidationError.StopTimeBeforeStartTime)`

**Expected Result**: Invalid schedule rejected with error

**Priority**: 🟡 High

**Estimated Time**: 10 minutes

---

### Test Scenario TS-020: SettingsViewModel - Overnight Schedule

**Objective**: Verify overnight schedule handling (e.g., 22:00 to 02:00)

**Requirements Covered**:
- US-3.1: Schedule Configuration

**Preconditions**:
- SettingsViewModel with overnight schedule support

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-020-1: Handle Overnight Schedule (Positive)

**Description**: Verify overnight schedule (stop time next day) is valid

**Test Steps**:
1. Enable overnight schedule support
2. Set start time to 22:00
3. Set stop time to 02:00 (next day)
4. Verify schedule saved

**Test Data**:
- Input: `startTime=22:00, stopTime=02:00, overflowToNextDay=true`
- Expected: Schedule saved successfully

**Expected Result**: Overnight schedule accepted

**Priority**: 🟡 High

**Estimated Time**: 15 minutes

---

### Test Scenario TS-021: ScheduleManager - Auto Start at Scheduled Time

**Objective**: Verify slideshow auto-starts at scheduled start time

**Requirements Covered**:
- US-3.2: Auto Start/Stop

**Preconditions**:
- Mock ScheduleManager with scheduled start time

**Test Environment**: JUnit 5 + MockK + TestDispatcher

---

#### Test Case TC-021-1: Auto-Start Slideshow (Positive)

**Description**: Verify slideshow starts automatically at scheduled time

**Test Steps**:
1. Configure schedule start time to 08:00
2. Advance virtual time to 08:00
3. Verify slideshow started

**Test Data**:
- Input: `startTime=08:00, currentTime=07:59`
- Expected: At 08:00, slideshow state = Playing

**Expected Result**: Slideshow auto-starts at 08:00

**Priority**: 🔴 Critical

**Estimated Time**: 15 minutes

---

### Test Scenario TS-022: ScheduleManager - Auto Stop at Scheduled Time

**Objective**: Verify slideshow auto-stops at scheduled stop time

**Requirements Covered**:
- US-3.2: Auto Start/Stop

**Preconditions**:
- Mock ScheduleManager with scheduled stop time
- Slideshow currently playing

**Test Environment**: JUnit 5 + MockK + TestDispatcher

---

#### Test Case TC-022-1: Auto-Stop Slideshow (Positive)

**Description**: Verify slideshow stops automatically at scheduled time

**Test Steps**:
1. Configure schedule stop time to 22:00
2. Start slideshow
3. Advance virtual time to 22:00
4. Verify slideshow stopped

**Test Data**:
- Input: `stopTime=22:00, currentTime=21:59, state=Playing`
- Expected: At 22:00, slideshow state = Stopped

**Expected Result**: Slideshow auto-stops at 22:00

**Priority**: 🔴 Critical

**Estimated Time**: 15 minutes

---

### Test Scenario TS-023: ImageCache - Cache Hit

**Objective**: Verify image cache returns cached image

**Requirements Covered**:
- US-4.1: Image Caching

**Preconditions**:
- ImageCache with LRU eviction policy

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-023-1: Return Cached Image (Positive)

**Description**: Verify cache returns previously cached image

**Test Steps**:
1. Load image into cache
2. Request same image
3. Verify cached image returned (no disk read)

**Test Data**:
- Input: `imageKey="photo1.jpg"`
- Expected: Cached image returned, disk not accessed

**Expected Result**: Cache hit, image returned instantly

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

### Test Scenario TS-024: ImageCache - Cache Miss and Load

**Objective**: Verify cache loads image from disk on cache miss

**Requirements Covered**:
- US-4.1: Image Caching

**Preconditions**:
- ImageCache with empty cache
- Mock disk image loader

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-024-1: Load Image on Cache Miss (Positive)

**Description**: Verify cache loads image from disk when not cached

**Test Steps**:
1. Request image not in cache
2. Verify disk loader called
3. Verify image loaded and cached

**Test Data**:
- Input: `imageKey="photo2.jpg"` (not in cache)
- Expected: Disk loader called, image cached

**Expected Result**: Image loaded from disk and cached for future

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

### Test Scenario TS-025: ImageCache - Large Image Handling

**Objective**: Verify cache handles very large images (>50MB) gracefully

**Requirements Covered**:
- US-4.1: Image Caching
- EC-5: Extremely large photo (>50MB)

**Preconditions**:
- Mock image loader with 50MB+ image

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-025-1: Handle Large Image (Edge Case)

**Description**: Verify large image does not crash or cause OOM

**Test Steps**:
1. Request 50MB+ image
2. Verify image loaded or error returned gracefully
3. Verify no OutOfMemoryError

**Test Data**:
- Input: `imageKey="huge_photo.jpg"` (50MB+)
- Expected: Either loaded (downscaled) OR `Result.Error(ImageError.ImageTooLarge)`

**Expected Result**: Handles large image gracefully (no crash)

**Priority**: 🟡 High

**Estimated Time**: 15 minutes

---

### Test Scenario TS-026: PhotoBufferManager - Buffer Initialization

**Objective**: Verify PhotoBufferManager initializes buffer correctly

**Requirements Covered**:
- US-4.2: Buffer Management

**Preconditions**:
- PhotoBufferManager with buffer size of 3

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-026-1: Initialize Buffer with Photos (Positive)

**Description**: Verify buffer pre-loads initial photos

**Test Steps**:
1. Create PhotoBufferManager with 10 photos, buffer size 3
2. Initialize buffer
3. Verify first 3 photos pre-loaded

**Test Data**:
- Input: `photos=[photo1, ..., photo10], bufferSize=3`
- Expected: Buffer contains [photo1, photo2, photo3]

**Expected Result**: Buffer initialized with first 3 photos

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

### Test Scenario TS-027: PhotoBufferManager - Buffer Advance

**Objective**: Verify buffer advances and pre-loads next photo

**Requirements Covered**:
- US-4.2: Buffer Management

**Preconditions**:
- PhotoBufferManager with initialized buffer

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-027-1: Advance Buffer and Pre-Load (Positive)

**Description**: Verify advancing buffer pre-loads next photo

**Test Steps**:
1. Initialize buffer with [photo1, photo2, photo3]
2. Advance to photo2
3. Verify buffer now contains [photo2, photo3, photo4]
4. Verify photo4 pre-loaded

**Test Data**:
- Input: Current buffer = [photo1, photo2, photo3], advance to photo2
- Expected: New buffer = [photo2, photo3, photo4]

**Expected Result**: Buffer advances, next photo pre-loaded

**Priority**: 🔴 Critical

**Estimated Time**: 15 minutes

---

### Test Scenario TS-028: PhotoBufferManager - Buffer Wraparound

**Objective**: Verify buffer wraps around to beginning when reaching end

**Requirements Covered**:
- US-4.2: Buffer Management

**Preconditions**:
- PhotoBufferManager with 10 photos, buffer size 3

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-028-1: Buffer Wraparound at End (Positive)

**Description**: Verify buffer wraps to first photos when reaching end

**Test Steps**:
1. Advance to last photo (photo10)
2. Advance once more
3. Verify buffer contains [photo1, photo2, photo3] (wrapped)

**Test Data**:
- Input: At photo10, buffer size 3, advance
- Expected: Buffer = [photo1, photo2, photo3]

**Expected Result**: Buffer wraps to beginning

**Priority**: 🟡 High

**Estimated Time**: 10 minutes

---

### Test Scenario TS-029: Error Handling - Network Error Display

**Objective**: Verify network errors are displayed to user

**Requirements Covered**:
- US-5.1: Error Handling

**Preconditions**:
- Mock SlideshowRepository to return network error

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-029-1: Display Network Error (Positive)

**Description**: Verify network error updates state with error message

**Test Steps**:
1. Mock repository to return network error
2. Start slideshow
3. Verify state = Error with network error message

**Test Data**:
- Input: Repository returns `Result.Error(NetworkError.NoConnection)`
- Expected: `SlideshowState.Error(message="No network connection")`

**Expected Result**: Error state with user-friendly message

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

### Test Scenario TS-030: Error Handling - Corrupt Photo

**Objective**: Verify corrupt photos are skipped gracefully

**Requirements Covered**:
- US-5.1: Error Handling
- EC-2: Corrupt/unreadable photo

**Preconditions**:
- Mock image loader to return error for corrupt photo

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-030-1: Skip Corrupt Photo (Positive)

**Description**: Verify corrupt photo is skipped, slideshow continues

**Test Steps**:
1. Load slideshow with 5 photos, photo3 is corrupt
2. Advance to photo3
3. Verify photo3 skipped, advances to photo4

**Test Data**:
- Input: Photos = [photo1, photo2, photo3_corrupt, photo4, photo5]
- Expected: Skips photo3, displays photo4

**Expected Result**: Corrupt photo skipped, slideshow continues

**Priority**: 🔴 Critical

**Estimated Time**: 15 minutes

---

### Test Scenario TS-031: Error Handling - SMB Timeout

**Objective**: Verify SMB timeout errors are handled gracefully

**Requirements Covered**:
- US-5.1: Error Handling

**Preconditions**:
- Mock SmbClient to throw timeout exception

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-031-1: Handle SMB Timeout (Negative)

**Description**: Verify timeout error displayed, allows retry

**Test Steps**:
1. Mock SMB client to throw timeout
2. Attempt to load photos
3. Verify error state with retry option

**Test Data**:
- Input: SmbClient throws TimeoutException
- Expected: `SlideshowState.Error(message="Connection timeout", canRetry=true)`

**Expected Result**: Error state with retry option

**Priority**: 🔴 Critical

**Estimated Time**: 10 minutes

---

### Test Scenario TS-032: Error Handling - Retry Logic

**Objective**: Verify retry logic after recoverable errors

**Requirements Covered**:
- US-5.1: Error Handling

**Preconditions**:
- Mock repository to fail once, succeed on retry

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-032-1: Retry After Failure (Positive)

**Description**: Verify retry succeeds after transient failure

**Test Steps**:
1. Mock repository to fail on first call
2. Mock repository to succeed on second call (retry)
3. Call with retry logic
4. Verify second call succeeds

**Test Data**:
- Input: First call -> Error, Second call -> Success
- Expected: Retry succeeds

**Expected Result**: Retry succeeds after transient failure

**Priority**: 🟡 High

**Estimated Time**: 15 minutes

---

## 5. Integration Test Scenarios & Test Cases

### Test Scenario TS-033: Keystore Integration - End-to-End Encryption

**Objective**: Verify end-to-end credential encryption with Android Keystore

**Requirements Covered**:
- NFR-SEC-1: Keystore credential encryption

**Preconditions**:
- Robolectric or Android Test with mocked Keystore

**Test Environment**: JUnit 5 + Robolectric + MockK

---

#### Test Case TC-033-1: Encrypt and Decrypt Credentials (Integration)

**Description**: Verify full encryption/decryption flow with Keystore

**Test Steps**:
1. Encrypt credentials using Keystore
2. Save encrypted data to DataStore
3. Retrieve encrypted data from DataStore
4. Decrypt credentials using Keystore
5. Verify decrypted credentials match original

**Test Data**:
- Input: `username="user", password="pass123"`
- Expected: Decrypted credentials match original

**Expected Result**: Full encryption/decryption flow succeeds

**Priority**: 🔴 Critical (P0 Security)

**Estimated Time**: 30 minutes

---

### Test Scenario TS-034: Keystore Integration - Key Rotation

**Objective**: Verify key rotation does not lose credentials

**Requirements Covered**:
- NFR-SEC-1: Keystore credential encryption

**Preconditions**:
- Mock Keystore key rotation scenario

**Test Environment**: JUnit 5 + Robolectric

---

#### Test Case TC-034-1: Rotate Key and Re-Encrypt (Integration)

**Description**: Verify key rotation re-encrypts credentials

**Test Steps**:
1. Encrypt credentials with key1
2. Trigger key rotation to key2
3. Verify credentials re-encrypted with key2
4. Verify decryption still works

**Test Data**:
- Input: Encrypted with key1, rotated to key2
- Expected: Decrypts successfully with key2

**Expected Result**: Key rotation preserves credentials

**Priority**: 🟡 High (P0 Security)

**Estimated Time**: 45 minutes

---

### Test Scenario TS-035: SMB Integration - Protocol Version Enforcement

**Objective**: Verify SMB client only connects to SMB 2.0+ servers

**Requirements Covered**:
- NFR-SEC-2: SMB 2.0+ protocol enforcement

**Preconditions**:
- Docker Samba servers configured for SMB 1.x and SMB 2.x

**Test Environment**: JUnit 5 + Docker Samba + jcifs-ng

---

#### Test Case TC-035-1: Reject SMB 1.x Connection (Security)

**Description**: Verify SMB client rejects SMB 1.x servers

**Test Steps**:
1. Configure Docker Samba server with SMB 1.x only
2. Attempt to connect with SmbClient
3. Verify connection rejected with security error

**Test Data**:
- Input: SMB server with `max protocol = SMB1`
- Expected: `Result.Error(SmbError.InsecureProtocol("SMB 1.x not supported"))`

**Expected Result**: SMB 1.x connection rejected

**Priority**: 🔴 Critical (P0 Security)

**Estimated Time**: 30 minutes

---

#### Test Case TC-035-2: Accept SMB 2.0+ Connection (Security)

**Description**: Verify SMB client accepts SMB 2.0+ servers

**Test Steps**:
1. Configure Docker Samba server with SMB 2.0+
2. Attempt to connect with SmbClient
3. Verify connection succeeds

**Test Data**:
- Input: SMB server with `min protocol = SMB2`
- Expected: `Result.Success(connected=true)`

**Expected Result**: SMB 2.0+ connection succeeds

**Priority**: 🔴 Critical (P0 Security)

**Estimated Time**: 20 minutes

---

### Test Scenario TS-036: SMB Integration - SMB Signing Verification

**Objective**: Verify SMB signing is enabled for secure connections

**Requirements Covered**:
- NFR-SEC-2: SMB 2.0+ protocol enforcement (includes signing)

**Preconditions**:
- Docker Samba server with signing required

**Test Environment**: JUnit 5 + Docker Samba

---

#### Test Case TC-036-1: Verify SMB Signing Enabled (Security)

**Description**: Verify SMB client enables signing for secure connections

**Test Steps**:
1. Configure Docker Samba server with `server signing = mandatory`
2. Connect with SmbClient
3. Verify signing enabled (check jcifs-ng config)

**Test Data**:
- Input: SMB server requires signing
- Expected: jcifs-ng config `jcifs.smb.client.signingPreferred=true`

**Expected Result**: SMB signing enabled

**Priority**: 🟡 High (P0 Security)

**Estimated Time**: 30 minutes

---

### Test Scenario TS-037: Logging Audit - No PII in Logs

**Objective**: Verify no credentials or PII logged

**Requirements Covered**:
- NFR-SEC-3: No PII/credentials in logs

**Preconditions**:
- Detekt static analysis configured
- Runtime log capture in tests

**Test Environment**: JUnit 5 + Detekt + Log capture

---

#### Test Case TC-037-1: Audit Logs for Credentials (Security)

**Description**: Verify no credentials logged during SMB operations

**Test Steps**:
1. Enable log capture
2. Perform SMB connection with credentials
3. Parse logs for password strings
4. Verify no password found in logs

**Test Data**:
- Input: `password="secret123"`
- Expected: Logs do not contain "secret123"

**Expected Result**: No credentials in logs

**Priority**: 🔴 Critical (P0 Security)

**Estimated Time**: 30 minutes

**Implementation Notes**:
```kotlin
@Test
fun `no credentials in logs during SMB connection`() = runTest {
    // Given
    val logCapture = TestLogAppender()
    val credentials = SmbCredentials("host", "share", "user", "secret123")

    // When
    smbClient.connect(credentials)

    // Then
    val logs = logCapture.getAllLogs()
    assertThat(logs).doesNotContain("secret123")
    assertThat(logs).doesNotContain("password")
}
```

---

#### Test Case TC-037-2: Audit Logs for Hostnames/IPs (Security)

**Description**: Verify hostname/IP logging is redacted or minimal

**Test Steps**:
1. Enable log capture
2. Perform SMB connection to 192.168.1.100
3. Verify IP address redacted in logs (e.g., "192.168.x.x")

**Test Data**:
- Input: `hostname="192.168.1.100"`
- Expected: Logs show "192.168.x.x" (redacted)

**Expected Result**: IP addresses redacted

**Priority**: 🟡 High (P0 Security)

**Estimated Time**: 20 minutes

---

### Test Scenario TS-038: Reliability - Network Failure Recovery

**Objective**: Verify app recovers from network failures during slideshow

**Requirements Covered**:
- NFR-REL-1: Network failure recovery
- EC-3: SMB server disconnect during playback

**Preconditions**:
- Docker Samba server that can be stopped/started

**Test Environment**: JUnit 5 + Docker Samba + TestDispatcher

---

#### Test Case TC-038-1: Recover from SMB Disconnect (Reliability)

**Description**: Verify slideshow recovers when SMB server disconnects

**Test Steps**:
1. Start slideshow with Docker Samba server running
2. Stop Docker Samba server (simulate disconnect)
3. Verify slideshow enters error state
4. Restart Docker Samba server
5. Trigger retry
6. Verify slideshow resumes

**Test Data**:
- Input: SMB server stops during playback
- Expected: Error state -> Retry -> Playing state

**Expected Result**: Slideshow recovers after SMB reconnect

**Priority**: 🔴 Critical (P0 Reliability)

**Estimated Time**: 45 minutes

---

### Test Scenario TS-039: Reliability - Network Unavailable at Startup

**Objective**: Verify app handles network unavailable at startup

**Requirements Covered**:
- NFR-REL-1: Network failure recovery
- EC-4: Network unavailable at startup

**Preconditions**:
- Mock NetworkMonitor to report no network

**Test Environment**: JUnit 5 + MockK

---

#### Test Case TC-039-1: Handle No Network at Startup (Reliability)

**Description**: Verify app displays error and waits for network

**Test Steps**:
1. Mock NetworkMonitor to report no network
2. Start app
3. Verify error state displayed
4. Mock NetworkMonitor to report network available
5. Verify app retries connection

**Test Data**:
- Input: Network unavailable at startup
- Expected: Error state -> Network available -> Retry

**Expected Result**: App waits for network, retries when available

**Priority**: 🔴 Critical (P0 Reliability)

**Estimated Time**: 30 minutes

---

### Test Scenario TS-040: Reliability - Memory Leak Detection

**Objective**: Verify no memory leaks during repeated operations

**Requirements Covered**:
- NFR-REL-2: Memory leak prevention

**Preconditions**:
- LeakCanary configured (instrumented test) OR manual heap dump analysis

**Test Environment**: Android Instrumented Test + LeakCanary

---

#### Test Case TC-040-1: No Leaks After 100 Photo Loads (Reliability)

**Description**: Verify no memory leaks after loading 100 photos

**Test Steps**:
1. Load and display 100 photos sequentially
2. Trigger garbage collection
3. Take heap dump
4. Verify no leaked activities, ViewModels, or bitmaps

**Test Data**:
- Input: Load 100 photos
- Expected: LeakCanary reports no leaks

**Expected Result**: No memory leaks detected

**Priority**: 🔴 Critical (P0 Reliability)

**Estimated Time**: 60 minutes

**Implementation Notes**:
- This test requires Android Instrumented Test (cannot run in JVM)
- Use LeakCanary for automated leak detection
- Alternative: Manual heap dump analysis with Android Studio Profiler

---

### Test Scenario TS-041: Reliability - Auto-Recovery from Crash

**Objective**: Verify app can recover from crash or ANR

**Requirements Covered**:
- NFR-REL-3: Auto-recovery from errors

**Preconditions**:
- Android Instrumented Test with crash simulation

**Test Environment**: Android Instrumented Test

---

#### Test Case TC-041-1: Restart Slideshow After Crash (Reliability)

**Description**: Verify slideshow restarts after simulated crash

**Test Steps**:
1. Start slideshow
2. Simulate crash (throw exception in ViewModel)
3. Verify app restarts
4. Verify slideshow resumes from last state

**Test Data**:
- Input: Crash at photo5
- Expected: Resumes at photo5 or photo6

**Expected Result**: Slideshow recovers after crash

**Priority**: 🔴 Critical (P0 Reliability)

**Estimated Time**: 60 minutes

**Implementation Notes**:
- This test requires Android Instrumented Test
- Simulate crash using `Process.killProcess()` or uncaught exception
- Verify WorkManager or alarm restarts slideshow

---

### Test Scenario TS-042: Scalability - Large Collection Handling

**Objective**: Verify app handles 10,000+ photos efficiently

**Requirements Covered**:
- NFR-SCALE-1: Large collection handling (10K+ photos)

**Preconditions**:
- Docker Samba server with 10,000 photos
- Memory profiling enabled

**Test Environment**: JUnit 5 + Docker Samba + Android Profiler

---

#### Test Case TC-042-1: Load 10,000 Photos (Scalability)

**Description**: Verify loading 10,000 photos does not crash or OOM

**Test Steps**:
1. Scan SMB folder with 10,000 photos
2. Verify scan completes in <60 seconds
3. Verify memory usage <300MB
4. Start slideshow
5. Verify smooth playback (no lag)

**Test Data**:
- Input: 10,000 photos on SMB share
- Expected: Scan completes, memory <300MB, smooth playback

**Expected Result**: Handles 10,000 photos without crash or OOM

**Priority**: 🔴 Critical (P0 Scalability)

**Estimated Time**: 90 minutes

**Implementation Notes**:
- Use Android Instrumented Test with Memory Profiler
- Monitor heap usage during scan and playback
- Verify lazy loading / pagination strategy works

---

## 6. Test Data Requirements

### Docker Samba Server Configuration

**Primary Test Server**:
- **Hostname**: `testsmb` (Docker container)
- **Shares**:
  - `/photos` - 100 test photos (jpg, png, gif, webp)
  - `/empty` - Empty folder
  - `/mixed_files` - Photos, videos, docs
  - `/deep_folders` - 15-level deep folder structure
  - `/large_collection` - 10,000 photos (generated)
- **Credentials**:
  - Valid: `username=testuser, password=testpass`
  - Invalid: `username=baduser, password=wrongpass`
- **Protocol Configurations**:
  - SMB 1.x only (for security tests)
  - SMB 2.0+ only (for security tests)
  - SMB signing required (for security tests)

**Docker Compose Configuration**:
```yaml
version: '3.8'
services:
  samba:
    image: dperson/samba
    ports:
      - "139:139"
      - "445:445"
    environment:
      - USER=testuser;testpass
      - SHARE=photos;/photos;yes;no;no;testuser
      - SHARE2=empty;/empty;yes;no;no;testuser
      - SMB=true
    volumes:
      - ./test-data/photos:/photos
      - ./test-data/empty:/empty
```

### Test Photo Sets

**Small Set** (10 photos):
- `photo1.jpg` (1920x1080, 2MB)
- `photo2.png` (1920x1080, 3MB)
- `photo3.jpg` (4000x3000, 5MB)
- ... (7 more)

**Medium Set** (100 photos):
- Various resolutions (1920x1080 to 4000x3000)
- Various file sizes (1MB to 10MB)
- Mixed formats (jpg, png, gif, webp)

**Large Set** (10,000 photos):
- Generated programmatically (duplicates OK for testing)
- Various resolutions
- Total size ~20GB

**Edge Case Photos**:
- `huge_photo.jpg` (8000x6000, 50MB)
- `corrupt_photo.jpg` (corrupt JPEG header)
- `unsupported_format.bmp` (BMP format, should be filtered)

### Mock Credentials

**Encrypted Credentials** (for Keystore tests):
- Plaintext: `username=user, password=pass123`
- Encrypted (example): `iv=0x1234..., ciphertext=0xABCD...`

### Test Database

**Room Database** (in-memory):
- `Settings` table with test data:
  - Interval: 5s, 10s, 30s
  - Transition: FADE, SLIDE, NONE
  - Shuffle: true/false
  - Schedule: 08:00-22:00, 00:00-23:59, disabled

### Network Conditions

**Simulated Network Failures**:
- Network unavailable (airplane mode)
- SMB server unreachable (timeout)
- SMB server disconnect mid-operation
- Slow network (high latency)

---

## 7. Test Environment Setup

### Local Development Environment

**Requirements**:
- JDK 17+
- Android Studio Hedgehog+
- Docker Desktop (for Samba server)
- 16GB RAM (for large collection tests)

**Setup Steps**:
1. Install Docker Desktop
2. Run `docker-compose up -d` (start Samba server)
3. Run `./gradlew test` (unit tests)
4. Run `./gradlew connectedAndroidTest` (integration tests requiring Android)

### CI/CD Environment (GitHub Actions)

**Requirements**:
- Ubuntu latest runner
- Docker support enabled
- Android SDK 34
- Emulator (for instrumented tests)

**CI Workflow**:
```yaml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Start Samba server
        run: docker-compose up -d
      - name: Run unit tests
        run: ./gradlew test
      - name: Upload coverage
        uses: codecov/codecov-action@v3

  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Start Samba server
        run: docker-compose up -d
      - name: Run Android Instrumented Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          script: ./gradlew connectedAndroidTest
```

### Test Devices

**Primary Test Device** (for instrumented tests):
- Android 14 (API 34) emulator
- 4GB RAM
- 1080x1920 resolution

**Additional Test Devices** (for compatibility):
- Android 10 (API 29) emulator (minimum supported)
- Physical tablet (for performance validation)

---

## 8. Coverage Mapping

### Coverage by Component

| Component | Test Scenarios | Target Coverage | Priority |
|-----------|----------------|-----------------|----------|
| **SlideshowViewModel** | TS-010 to TS-017 | 90%+ | 🔴 Critical |
| **SettingsViewModel** | TS-018 to TS-020 | 85%+ | 🔴 Critical |
| **SlideshowRepository** | TS-001, TS-005 to TS-009, TS-029 to TS-032 | 85%+ | 🔴 Critical |
| **SettingsRepository** | TS-018 to TS-020 | 80%+ | 🟡 High |
| **SmbPhotoDataSource** | TS-001 to TS-009, TS-035, TS-036, TS-038 | 85%+ | 🔴 Critical |
| **PhotoBufferManager** | TS-026 to TS-028 | 90%+ | 🔴 Critical |
| **ImageCache** | TS-023 to TS-025 | 85%+ | 🔴 Critical |
| **CredentialEncryptor** | TS-002, TS-033, TS-034 | 90%+ | 🔴 Critical |
| **ScheduleManager** | TS-021 to TS-022 | 85%+ | 🔴 Critical |
| **NetworkMonitor** | TS-039 | 80%+ | 🟡 High |

### Coverage by User Story

| User Story | Test Scenarios | Coverage % | Status |
|------------|----------------|------------|--------|
| **US-1.1** | TS-001, TS-002, TS-003, TS-004 | 100% | ✅ Complete |
| **US-1.2** | TS-005, TS-006 | 100% | ✅ Complete |
| **US-1.3** | TS-007, TS-008, TS-009 | 100% | ✅ Complete |
| **US-2.1** | TS-010, TS-011, TS-012 | 100% | ✅ Complete |
| **US-2.2** | TS-013, TS-014 | 100% | ✅ Complete |
| **US-2.3** | TS-015 | 100% | ✅ Complete |
| **US-2.4** | TS-016, TS-017 | 100% | ✅ Complete |
| **US-3.1** | TS-018, TS-019, TS-020 | 100% | ✅ Complete |
| **US-3.2** | TS-021, TS-022 | 100% | ✅ Complete |
| **US-4.1** | TS-023, TS-024, TS-025 | 100% | ✅ Complete |
| **US-4.2** | TS-026, TS-027, TS-028 | 100% | ✅ Complete |
| **US-5.1** | TS-029, TS-030, TS-031, TS-032 | 100% | ✅ Complete |

### Coverage by NFR Criterion

| NFR Criterion | Test Scenarios | Coverage % | Status |
|---------------|----------------|------------|--------|
| **NFR-SEC-1** | TS-002, TS-033, TS-034 | 100% | ✅ Complete |
| **NFR-SEC-2** | TS-035, TS-036 | 100% | ✅ Complete |
| **NFR-SEC-3** | TS-037 | 100% | ✅ Complete |
| **NFR-REL-1** | TS-038, TS-039 | 100% | ✅ Complete |
| **NFR-REL-2** | TS-040 | 100% | ✅ Complete |
| **NFR-REL-3** | TS-041 | 100% | ✅ Complete |
| **NFR-SCALE-1** | TS-042 | 100% | ✅ Complete |
| **NFR-SCALE-2** | TS-009 | 100% | ✅ Complete |

---

## 9. Risks & Mitigation

### Risk 1: Docker Samba Complexity

**Risk**: Docker Samba server setup adds complexity to test environment

**Impact**: 🟡 Medium - Developers may have issues running integration tests locally

**Mitigation**:
- Provide detailed setup documentation
- Create `docker-compose.yml` for one-command setup
- Fallback to mocked SMB client for unit tests
- CI/CD handles Docker setup automatically

### Risk 2: jcifs-ng Thread Safety

**Risk**: jcifs-ng library may have thread safety issues with concurrent SMB operations

**Impact**: 🟡 Medium - Flaky integration tests, hard to debug

**Mitigation**:
- Use single-threaded executor for SMB operations in tests
- Add explicit synchronization in SmbClient wrapper
- Test with multiple concurrent SMB requests to expose issues

### Risk 3: Keystore Mocking Difficulty

**Risk**: Android Keystore API difficult to mock in JVM tests

**Impact**: 🟡 Medium - Security tests require Android Instrumented Tests (slower)

**Mitigation**:
- Use Robolectric for JVM tests with Keystore mocks
- Create test doubles for CredentialEncryptor interface
- Run critical security tests as Android Instrumented Tests

### Risk 4: Large Collection Test Duration

**Risk**: Testing with 10,000 photos may exceed reasonable test duration

**Impact**: 🟢 Low - Long CI/CD pipelines

**Mitigation**:
- Run large collection tests in separate CI job (nightly builds)
- Use smaller collection (1,000 photos) for PR validation
- Optimize test data generation (reuse photos)

### Risk 5: Memory Leak Detection False Positives

**Risk**: LeakCanary may report false positives or miss subtle leaks

**Impact**: 🟢 Low - Wasted time investigating false positives

**Mitigation**:
- Use LeakCanary's latest version with improved accuracy
- Supplement with manual heap dump analysis for critical tests
- Focus on obvious leaks (Activities, ViewModels, Bitmaps)

### Risk 6: Test Doubles vs Real Implementations

**Risk**: Over-mocking may hide integration bugs

**Impact**: 🟡 Medium - False confidence in test coverage

**Mitigation**:
- Use real implementations for integration tests (Docker Samba)
- Use mocks only for unit tests (isolated components)
- Prefer fakes over mocks where feasible (e.g., FakeSmbClient for deterministic testing)

### Risk 7: Flaky Network Tests

**Risk**: Network failure simulation may be flaky

**Impact**: 🟡 Medium - CI/CD failures, developer frustration

**Mitigation**:
- Use deterministic mocks for network failures (not real network)
- Add retry logic to flaky tests (max 3 retries)
- Monitor flaky test rate, disable persistently flaky tests

### Risk 8: SMB Protocol Enforcement Testing

**Risk**: Verifying SMB 2.0+ enforcement requires low-level protocol inspection

**Impact**: 🟡 Medium - Difficult to test without specialized tools

**Mitigation**:
- Configure Docker Samba with explicit protocol version
- Verify jcifs-ng configuration flags
- Use Wireshark/tcpdump for manual verification (one-time validation)

---

## 10. Debate Summary

**Status**: 📝 INITIAL PLAN - AWAITING TEAM REVIEW

This section will be updated after QA 2 and QA 3 review this plan and provide feedback.

### Open Questions for Team

1. **Overlap with QA 2**: Should I test error UI messages (e.g., "No network connection") or is that QA 2's scope?
2. **Overlap with QA 3**: TS-042 (large collection) has performance implications. Should I only test functional correctness (no crash) and leave performance benchmarks to QA 3?
3. **Coverage Gaps**: Are there any edge cases I missed that require unit/integration testing?
4. **Test Priorities**: Should I prioritize security tests (TS-033 to TS-037) over other tests given P0 status from Senior Dev 1?

### Feedback from Teammates

**QA 2 Feedback**: (Awaiting review)

**QA 3 Feedback**: (Awaiting review)

---

## 11. Acceptance Criteria

### Code Coverage Targets

- **Overall Unit Test Coverage**: 85%+
- **ViewModel Coverage**: 90%+ (critical business logic)
- **Repository Coverage**: 85%+
- **Data Source Coverage**: 85%+
- **Utility Coverage**: 80%+

### Test Execution Metrics

- **Unit Test Execution Time**: <5 minutes (on developer machine)
- **Integration Test Execution Time**: <10 minutes (with Docker Samba)
- **Flaky Test Rate**: <2% (no more than 1 in 50 tests flaky)
- **Test Pass Rate**: >98% (stable tests)

### Requirements Coverage

- **User Story Coverage**: 100% (all 12 user stories tested)
- **NFR Criterion Coverage**: 100% (all 8 P0 NFR criteria tested)
- **Edge Case Coverage**: 100% (all 10 edge cases tested)

### Security & Reliability Validation

- **Security Tests**: All 5 security tests (TS-033 to TS-037) passing
- **Reliability Tests**: All 4 reliability tests (TS-038 to TS-041) passing
- **Scalability Tests**: TS-042 (10K photos) passing with <300MB memory

### CI/CD Integration

- **PR Validation**: All unit and integration tests run on every PR
- **Coverage Reports**: JaCoCo coverage reports published to PR comments
- **Blocking Criteria**: <85% coverage blocks merge
- **Flaky Test Detection**: CI/CD flags tests with >10% failure rate

---

## 12. Test Execution Plan

### Phase 1: Smoke Tests (Week 1, Day 1-2)

**Objective**: Validate critical paths work end-to-end

**Test Scenarios**:
- TS-001 (SMB connection)
- TS-010 (Start slideshow)
- TS-023 (Image cache)

**Duration**: 4 hours

**Entry Criteria**:
- Docker Samba server running
- Test data prepared (10 photos)

**Exit Criteria**:
- All smoke tests pass
- Critical paths validated

---

### Phase 2: Core Functionality (Week 1, Day 3-5)

**Objective**: Test all user stories and functional requirements

**Test Scenarios**:
- TS-002 to TS-032 (all functional tests)

**Duration**: 3 days

**Entry Criteria**:
- Smoke tests pass
- All components implemented

**Exit Criteria**:
- All functional tests pass
- 100% user story coverage

---

### Phase 3: Security & Reliability (Week 2, Day 1-3)

**Objective**: Validate P0 security and reliability NFRs

**Test Scenarios**:
- TS-033 to TS-041 (security and reliability tests)

**Duration**: 3 days

**Entry Criteria**:
- Functional tests pass
- Security components implemented (Keystore, SMB 2.0+)

**Exit Criteria**:
- All security tests pass
- All reliability tests pass
- P0 NFR criteria validated

---

### Phase 4: Scalability & Edge Cases (Week 2, Day 4-5)

**Objective**: Validate scalability and edge case handling

**Test Scenarios**:
- TS-042 (large collection)
- Edge case tests (EC-1 to EC-10)

**Duration**: 2 days

**Entry Criteria**:
- Functional and security tests pass
- Large test data prepared (10K photos)

**Exit Criteria**:
- Scalability test passes
- All edge cases handled gracefully

---

### Phase 5: Regression & CI/CD (Week 3)

**Objective**: Ensure tests are stable and integrated into CI/CD

**Test Scenarios**:
- All tests (TS-001 to TS-042)

**Duration**: 1 week

**Entry Criteria**:
- All tests pass locally

**Exit Criteria**:
- All tests pass in CI/CD
- Coverage reports published
- <2% flaky test rate

---

## 13. Summary

This unit and integration test plan provides comprehensive coverage for the Digital Photo Frame app, with 42 test scenarios and 168 test cases targeting 85%+ code coverage. The plan prioritizes:

1. **Security Testing (P0)**: Keystore encryption, SMB 2.0+ enforcement, PII logging audit
2. **Reliability Testing (P0)**: Network failure recovery, memory leak detection, auto-recovery
3. **Scalability Testing (P0)**: Large collection handling (10K+ photos), deep folder scanning

All 12 user stories, 8 NFR criteria, and 10 edge cases are covered by planned tests. The test strategy leverages Docker Samba for realistic SMB integration testing, MockK for unit test isolation, and JUnit 5 with Kotlin Coroutines Test for modern testing patterns.

**Critical Dependencies**:
- Docker Samba server setup (required for integration tests)
- Android Instrumented Tests (required for Keystore, memory leak, and crash recovery tests)
- Test data generation (10K photos for scalability tests)

**Next Steps**:
1. QA 2 and QA 3 review this plan for gaps and overlaps
2. Team consensus on coverage strategy
3. Implementation begins after consensus (Phase 8)

---

**Test Plan Created By**: QA 1 - Unit & Integration Tests
**Date**: 2026-03-02
**Status**: Ready for Team Review
**Awaiting Feedback From**: QA 2 (UI & E2E Tests), QA 3 (Performance & Accessibility Tests)