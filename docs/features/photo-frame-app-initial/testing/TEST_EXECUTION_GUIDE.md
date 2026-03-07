# Test Execution Guide - Photo Frame Android App

**Version**: 1.0
**Date**: 2026-03-04
**Phase**: Phase 9 - Test Implementation & Execution

---

## Overview

This guide provides step-by-step instructions for executing all test suites for the Photo Frame Android App MVP. Tests are organized by priority and execution environment.

---

## Test Suite Organization

### 1. Unit Tests (Fast - Local JVM)
**Location**: `core/src/test/java/`
**Execution Time**: ~2-5 minutes
**Run Frequency**: Every commit

### 2. Integration Tests (Medium - Android Emulator)
**Location**: `app/src/androidTest/java/`
**Execution Time**: ~10-30 minutes
**Run Frequency**: Every PR

### 3. Performance Tests (Slow - Physical Device Recommended)
**Location**: `app/src/androidTest/java/com/photoframe/app/performance/`
**Execution Time**: ~1-2 hours
**Run Frequency**: Weekly or before release

### 4. 7-Day Stress Test (Very Slow - Firebase Test Lab)
**Location**: `app/src/androidTest/java/com/photoframe/app/stress/`
**Execution Time**: 168 hours (7 days)
**Run Frequency**: Before major releases

---

## Prerequisites

### Development Environment
- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Physical Device Requirements (Recommended for Performance Tests)
- Android 9.0 (API 28) or higher
- 2GB+ RAM
- 16GB+ storage
- WiFi connectivity
- Access to test SMB server

### Firebase Test Lab Setup (For 7-Day Stress Test)
1. Enable Firebase Test Lab API
2. Create Google Cloud project
3. Configure service account with Test Lab permissions
4. Set up billing account
5. Create GCS bucket for test results

### Test SMB Server Setup
- SMB 2.0+ enabled server
- Test share with read permissions
- 100+ test photos (JPEG format)
- Known good credentials

---

## Test Execution Instructions

### Quick Start - Run All Unit Tests

```bash
cd /path/to/photo-frame-android

# Run all unit tests
./gradlew test

# Run specific module
./gradlew :core:test

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport

# View coverage report
open core/build/reports/jacoco/test/html/index.html
```

**Expected Result**: All tests pass, coverage >80%

---

### Run P0 Security Tests

**Tests**: 26 test cases
**Duration**: ~2-3 minutes

```bash
# Run security tests only
./gradlew test --tests "*KeystoreCredentialStoreTest*"
./gradlew test --tests "*SmbProtocolEnforcementTest*"
./gradlew test --tests "*PiiLoggingAuditTest*"
```

**Success Criteria**:
- ✅ All 26 tests pass
- ✅ No PII in logs
- ✅ SMB 1.x connections rejected
- ✅ Credentials encrypted with Keystore

---

### Run P0 Reliability Tests

**Tests**: 38 test cases
**Duration**: ~3-5 minutes

```bash
# Network recovery tests
./gradlew test --tests "*NetworkRecoveryTest*"

# Memory leak tests
./gradlew test --tests "*MemoryLeakDetectionTest*"

# Auto-recovery tests
./gradlew test --tests "*AutoRecoveryTest*"
```

**Success Criteria**:
- ✅ Network recovery with exponential backoff
- ✅ No memory leaks over 10K photo loads
- ✅ Auto-restart after crashes
- ✅ OOM recovery with cache clearing

---

### Run P0 Scalability Tests

**Tests**: 8 test cases
**Duration**: ~5-10 minutes

```bash
# Large collection tests
./gradlew test --tests "*LargeCollectionTest*"
```

**Success Criteria**:
- ✅ 10,000 photos scan within 30 seconds
- ✅ Memory <300MB with 10K collection
- ✅ Incremental loading (first 100 photos <2s)

---

### Run Performance Benchmark Tests

**Tests**: 27 test cases
**Duration**: ~1-2 hours
**Requires**: Physical device (emulator not accurate for performance)

```bash
# Connect physical device
adb devices

# Run performance tests on device
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.photoframe.app.performance

# Or run individual suites
./gradlew connectedAndroidTest --tests "*PhotoLoadPerformanceTest*"
./gradlew connectedAndroidTest --tests "*TransitionPerformanceTest*"
./gradlew connectedAndroidTest --tests "*MemoryPerformanceTest*"
./gradlew connectedAndroidTest --tests "*ColdStartPerformanceTest*"
```

**Success Criteria**:
- ✅ Photo load time <2s (95th percentile)
- ✅ Transitions maintain 60fps, jank <5%
- ✅ Peak memory <300MB
- ✅ Cold start <3s (95th percentile)

**View Results**:
```bash
# Performance metrics report
open app/build/reports/androidTests/connected/index.html
```

---

### Run UI Component Tests

**Tests**: 21 test cases
**Duration**: ~10-20 minutes

```bash
# Run UI tests
./gradlew connectedAndroidTest --tests "*SlideshowScreenTest*"
./gradlew connectedAndroidTest --tests "*SettingsScreenTest*"
```

**Success Criteria**:
- ✅ All UI interactions work
- ✅ State changes reflected in UI
- ✅ Form validation displays errors
- ✅ Gestures trigger correct actions

---

### Run E2E User Flow Tests

**Tests**: 14 test cases
**Duration**: ~30-60 minutes

```bash
# Run E2E tests
./gradlew connectedAndroidTest --tests "*FirstTimeSetupFlowTest*"
./gradlew connectedAndroidTest --tests "*DailyUsageFlowTest*"
```

**Success Criteria**:
- ✅ First-time setup completes in <3 minutes
- ✅ Zero-touch operation for 1 hour
- ✅ Network disruption recovery
- ✅ Memory stable over extended operation

---

### Run Accessibility Tests

**Tests**: 30 test cases
**Duration**: ~15-30 minutes

```bash
# Run accessibility tests
./gradlew connectedAndroidTest --tests "*TalkBackNavigationTest*"
./gradlew connectedAndroidTest --tests "*TouchTargetSizeTest*"
./gradlew connectedAndroidTest --tests "*ColorContrastTest*"
./gradlew connectedAndroidTest --tests "*HighContrastModeTest*"
```

**Success Criteria**:
- ✅ All interactive elements have content descriptions
- ✅ Touch targets ≥48dp
- ✅ Color contrast ≥4.5:1 (normal text), ≥3:1 (UI components)
- ✅ TalkBack can complete first-time setup

**Manual Testing with TalkBack**:
1. Enable TalkBack: Settings → Accessibility → TalkBack
2. Navigate app using only TalkBack gestures
3. Verify all critical flows accessible

---

### Run 7-Day Stress Test (Firebase Test Lab)

**Test**: 1 comprehensive test
**Duration**: 168 hours (7 days)
**Cost**: ~$100-150 per run

#### Step 1: Build APKs

```bash
# Build release APK
./gradlew assembleRelease

# Build test APK
./gradlew assembleAndroidTest

# Verify APKs created
ls -lh app/build/outputs/apk/release/app-release.apk
ls -lh app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

#### Step 2: Authenticate to Google Cloud

```bash
# Authenticate
gcloud auth login

# Set project
gcloud config set project YOUR_PROJECT_ID

# Verify Firebase Test Lab access
gcloud firebase test android models list
```

#### Step 3: Start Stress Test

**Option A: Use Configuration File** (Recommended)

```bash
# Run stress test using config file
gcloud firebase test android run \
  --config firebase-test-lab-config.yml \
  --test-targets "class com.photoframe.app.stress.SevenDayStressTest#sevenDayStressTest"
```

**Option B: Manual Command**

```bash
gcloud firebase test android run \
  --type instrumentation \
  --app app/build/outputs/apk/release/app-release.apk \
  --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=oriole,version=33,locale=en_US,orientation=landscape \
  --device model=flame,version=30,locale=en_US,orientation=landscape \
  --device model=redfin,version=31,locale=en_US,orientation=landscape \
  --device model=blueline,version=29,locale=en_US,orientation=landscape \
  --device model=crosshatch,version=28,locale=en_US,orientation=landscape \
  --test-targets "class com.photoframe.app.stress.SevenDayStressTest#sevenDayStressTest" \
  --timeout 170h \
  --results-bucket gs://photo-frame-stress-tests \
  --results-dir stress-test-$(date +%Y%m%d-%H%M%S) \
  --num-flaky-test-attempts 0 \
  --record-video \
  --performance-metrics \
  --environment-variables clearPackageData=true \
  --directories-to-pull /sdcard
```

#### Step 4: Monitor Test Progress

```bash
# List running tests
gcloud firebase test android list

# View test results in Firebase Console
open https://console.firebase.google.com/project/YOUR_PROJECT_ID/testlab/histories
```

**Real-Time Monitoring**:
- **Crashlytics Dashboard**: Monitor crashes in real-time
- **Cloud Logging**: View test heartbeat logs every 1 minute
- **Performance Monitoring**: Track memory usage every 5 minutes

#### Step 5: Download Results (After 7 Days)

```bash
# Download all test results
gsutil -m cp -r gs://photo-frame-stress-tests/stress-test-*/* ./stress-test-results/

# Extract key metrics
grep "CRASH" ./stress-test-results/*/stress_test_log.txt | wc -l
grep "Memory usage:" ./stress-test-results/*/stress_test_log.txt | tail -20
```

#### Step 6: Analyze Results

**Success Criteria**:
- ✅ Crash-free rate >99.5% (max 8 crashes over 168 hours)
- ✅ Memory <300MB peak throughout
- ✅ Performance degradation <5% over 7 days
- ✅ 60,000+ photo transitions completed
- ✅ No manual intervention required

**Failure Scenarios**:
- ❌ >8 crashes = Investigate crash logs in Crashlytics
- ❌ Memory leak detected = Run memory profiler, fix leaks
- ❌ Performance degradation >5% = Profile CPU/GPU usage
- ❌ Stalled slideshow = Check watchdog logs, ANR traces

---

### Run CI/CD Tests (GitHub Actions)

Tests automatically run on:
- Every pull request
- Every push to main
- Weekly schedule (for stress tests)

**Manual Trigger**:
1. Go to GitHub Actions tab
2. Select workflow: "CI - Unit & Integration Tests"
3. Click "Run workflow"

**View Results**:
- Test reports attached as artifacts
- JUnit XML reports in `app/build/test-results/`
- Coverage reports in `app/build/reports/coverage/`

---

## Test Configuration

### Gradle Test Configuration

**File**: `app/build.gradle.kts`

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        animationsDisabled = true

        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

dependencies {
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.2.0")
}
```

### Test Filters

**Run only P0 tests**:
```bash
./gradlew test --tests "*P0*"
```

**Run by category**:
```bash
# Security tests
./gradlew test --tests "*.security.*"

# Reliability tests
./gradlew test --tests "*.reliability.*"

# Performance tests
./gradlew connectedAndroidTest --tests "*.performance.*"

# Accessibility tests
./gradlew connectedAndroidTest --tests "*.accessibility.*"
```

---

## Troubleshooting

### Tests Fail with "Device not found"

```bash
# Check connected devices
adb devices

# Restart ADB server
adb kill-server
adb start-server

# Or use emulator
emulator -avd Pixel_6_API_33 &
```

### Tests Fail with OutOfMemoryError

```bash
# Increase test JVM memory
./gradlew test -Dorg.gradle.jvmargs="-Xmx4g -XX:MaxMetaspaceSize=512m"
```

### Firebase Test Lab: "Insufficient quota"

- Check billing account is active
- Verify Test Lab API is enabled
- Check daily quota limits
- Upgrade billing plan if needed

### Tests Pass Locally But Fail in CI

- Check CI environment has correct SDK versions
- Verify emulator configuration matches local
- Check for flaky tests (non-deterministic behavior)
- Increase test timeout in CI configuration

---

## Best Practices

### Before Committing Code

1. ✅ Run all unit tests: `./gradlew test`
2. ✅ Run affected integration tests
3. ✅ Check code coverage: `./gradlew jacocoTestReport`
4. ✅ Fix any failing tests

### Before Creating PR

1. ✅ Run full test suite: `./gradlew test connectedAndroidTest`
2. ✅ Run lint checks: `./gradlew lint`
3. ✅ Verify no accessibility regressions
4. ✅ Check CI passes all tests

### Before Release

1. ✅ Run all unit and integration tests
2. ✅ Run performance benchmarks on physical devices
3. ✅ Run accessibility tests with TalkBack enabled
4. ✅ Execute 7-day stress test on Firebase Test Lab
5. ✅ Manual QA walkthrough of critical user flows
6. ✅ Review all test reports and metrics

---

## Test Report Generation

### Coverage Report

```bash
# Generate coverage report
./gradlew jacocoTestReport

# View report
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### JUnit XML Reports

**Location**: `app/build/test-results/testDebugUnitTest/`

Import into CI tools like Jenkins, TeamCity, or GitHub Actions.

### Performance Reports

**Location**: `app/build/reports/androidTests/connected/`

Includes:
- Test execution times
- Device screenshots
- Test logs
- Performance metrics (if Macrobenchmark enabled)

---

## Continuous Integration

### GitHub Actions Workflows

**File**: `.github/workflows/ci.yml`

Runs on every PR:
- Unit tests
- Integration tests (on emulator)
- Lint checks
- Coverage report

**File**: `.github/workflows/stress-test.yml`

Runs weekly (Sunday midnight):
- 7-day stress test on Firebase Test Lab
- Automated result analysis
- Slack notifications

---

## Metrics & Targets

### Code Coverage Targets
- **Overall**: >80%
- **Core module**: >85%
- **Critical paths** (security, reliability): >95%

### Performance Targets
- **Photo load time**: <2s (95th percentile)
- **Transition smoothness**: 60fps, <5% jank
- **Memory usage**: <300MB peak
- **Cold start**: <3s (95th percentile)

### Reliability Targets
- **Crash-free rate**: >99.5%
- **Uptime**: >99% over 7 days
- **Network recovery**: <30s to reconnect

---

## Support & Resources

- **Documentation**: `/docs/features/photo-frame-app-initial/testing/`
- **Test Plans**: QA 1, QA 2, QA 3 test plans
- **Architecture**: ADR documents
- **Firebase Console**: https://console.firebase.google.com/
- **GitHub Issues**: Report test failures or flaky tests

---

**Document Version**: 1.0
**Last Updated**: 2026-03-04
**Maintainer**: QA Team
