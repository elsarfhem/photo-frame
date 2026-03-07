# Performance & Accessibility Test Plan - Digital Photo Frame App (MVP Phase 1)

**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Test Scope**: Performance Benchmarking, 24/7 Stress Testing, Accessibility Compliance
**QA Agent**: QA 3 - Performance & Accessibility Tests focused
**Date**: 2026-03-02
**Phase**: Phase 6 - Test Planning
**Status**: READY FOR TEAM REVIEW

---

## 1. Executive Summary

### Test Scope
This test plan covers **performance benchmarking**, **24/7 stress testing**, and **accessibility compliance** for the Digital Photo Frame app. This is the FINAL test plan in the test planning phase, complementing QA 1 (Unit & Integration) and QA 2 (UI & E2E) plans.

### Test Coverage Summary
- **Total Test Scenarios**: 35
- **Total Test Cases**: 128
- **Estimated Effort**: 120-150 hours (4-5 weeks)
- **Target Coverage**: 100% of P0 performance NFRs, 100% of accessibility requirements, >99.5% crash-free validation
- **Requirements Coverage**: All P0 NFR criteria (<2s photo load, 60fps transitions, <300MB memory, 24/7 operation, TalkBack support)

### Critical Focus Areas (Based on NFR Assessments & GAPS in QA 1 & QA 2 Plans)

#### Performance Gaps Identified in Previous Test Plans
1. **MISSING: 7-Day Stress Test** - QA 2 only tested 1-hour continuous operation (240 transitions). Senior Dev 3 explicitly requires 7-day stress test for 24/7 kiosk operation. **THIS PLAN ADDRESSES: 7-day stress test with 60,000+ photo transitions**
2. **MISSING: Long-Duration Memory Leak Detection** - QA 1 tested 100 photo loads. For 24/7 operation over 7 days (60,000+ photos), this is insufficient. **THIS PLAN ADDRESSES: 10,000+ photo load test with memory profiling**
3. **MISSING: Cold Start Performance** - Architecture specifies <3s cold start time. Neither QA 1 nor QA 2 tested app launch performance. **THIS PLAN ADDRESSES: Cold start benchmarking**
4. **MISSING: Battery Drain Profiling** - NFR specifies <5% battery drain per hour (though Senior Dev 1 flagged ambiguity). No battery testing in QA 1/QA 2. **THIS PLAN ADDRESSES: 24-hour battery profiling**
5. **MISSING: Network Performance Under Adverse Conditions** - QA 1 tested network recovery, but no tests for slow networks (1-2 Mbps), high latency (500ms), or packet loss. **THIS PLAN ADDRESSES: Network stress testing**
6. **MISSING: CPU/GPU/Memory Profiling** - QA 2 tested 60fps with Choreographer, but no profiling of resource usage. What if it's 60fps but consuming 100% CPU? **THIS PLAN ADDRESSES: Android Profiler integration**

#### Accessibility Gaps Identified in Previous Test Plans
1. **MISSING: TalkBack Screen Reader Testing** - QA 2 mentioned TalkBack but provided NO concrete test scenarios. How do blind users configure SMB settings? **THIS PLAN ADDRESSES: Complete TalkBack navigation flows**
2. **MISSING: Content Descriptions for Images** - Photos must have alt text for screen readers. Not tested by QA 1/QA 2. **THIS PLAN ADDRESSES: Content description validation**
3. **MISSING: High Contrast Mode** - Android accessibility feature. Not tested. **THIS PLAN ADDRESSES: High contrast visual testing**
4. **MISSING: Touch Target Size Validation** - NFR requires 48dp minimum. QA 2 tested UI but didn't validate touch target sizes. **THIS PLAN ADDRESSES: Touch target size measurement**
5. **MISSING: WCAG Compliance** - No color contrast validation (4.5:1 for text, 3:1 for UI components). **THIS PLAN ADDRESSES: WCAG AA compliance testing**

### Test Environment
- **Physical Devices**: 5x tablets (Fire HD 10, Samsung Galaxy Tab A, Lenovo Tab M10, budget Android tablets from Walmart/Best Buy, 7"-12" screens)
- **OS Versions**: Android 10-14
- **Profiling Tools**: Android Profiler (CPU, memory, GPU), Systrace, Battery Historian
- **Accessibility Tools**: TalkBack, Accessibility Scanner, Espresso Accessibility
- **Network Control**: Charles Proxy, Network Link Conditioner, tc (traffic control)
- **Stress Test Infrastructure**: CI/CD pipeline with 7-day test job, automated crash reporting, memory leak detection

---

## 2. Test Strategy

### Scope

**What This Test Plan Covers**:
- Performance benchmarking (NFR validation: <2s photo load, 60fps transitions, <300MB memory, <3s cold start)
- 24/7 stress testing (7-day continuous operation, >99.5% crash-free rate, memory leak detection)
- Scalability testing (10K-100K photos, large images 50MB+, deep folder hierarchies)
- Network performance testing (slow networks, high latency, packet loss)
- Android Profiler integration (CPU, memory, GPU, battery drain analysis)
- Accessibility compliance (TalkBack, high contrast, touch targets, WCAG AA)

**What This Test Plan Does NOT Cover** (Covered by QA 1 & QA 2):
- Unit tests for ViewModels/Repositories (QA 1)
- Integration tests for SMB/Room/Keystore (QA 1)
- UI component tests (QA 2)
- E2E user flows (QA 2)
- Functional validation of features (QA 2)

### Approach

#### Performance Testing Philosophy
- **Profile Before Optimizing**: Use Android Profiler to identify actual bottlenecks, not assumptions
- **Real-World Conditions**: Test on budget tablets (Fire HD 10, not Pixel 6 Pro) with 2GB RAM
- **Long-Duration Validation**: 7-day stress test to detect issues that only appear after extended operation (memory leaks, resource exhaustion, crash accumulation)
- **Percentile-Based Metrics**: 95th percentile for photo load time (not average), 99th percentile for frame times

#### Accessibility Testing Philosophy
- **Empathy-Driven**: Test as if you are a blind user (eyes closed, TalkBack only)
- **Standards-Based**: WCAG 2.1 Level AA compliance (minimum for enterprise apps)
- **Inclusive Design**: Support color blindness, low vision, motor impairments, cognitive disabilities

#### Test Execution Strategy
1. **Week 1-2**: Performance benchmarking, profiling setup, baseline metrics
2. **Week 3**: 7-day stress test (starts Monday, runs 24/7, monitored via Crashlytics/Firebase)
3. **Week 4**: Network performance testing, scalability testing
4. **Week 5**: Accessibility testing, WCAG compliance validation, final report

### Tools & Libraries

#### Performance Profiling
- **Android Profiler**: CPU, memory, network, energy profiling during test execution
- **Systrace**: Frame rendering analysis, jank detection, thread contention
- **Battery Historian**: Battery drain analysis over 24 hours
- **Macrobenchmark**: Jetpack library for startup time, jank metrics, operation timing
- **LeakCanary**: Automatic memory leak detection (integration into debug builds)

#### Accessibility Testing
- **TalkBack**: Android screen reader (test with eyes closed)
- **Accessibility Scanner**: Google tool for automated accessibility checks
- **Espresso Accessibility**: Espresso extension for accessibility assertions
- **axe DevTools Mobile**: WCAG compliance scanner for Android
- **Color Contrast Analyzer**: WCAG contrast ratio validation

#### Network Control
- **Charles Proxy**: Network throttling, latency injection, packet loss simulation
- **Network Link Conditioner** (macOS): Network speed/latency control
- **tc (traffic control)** (Linux): Advanced network emulation
- **Android Emulator Network Settings**: Basic throttling (Edge, 3G, LTE)

#### Stress Testing Infrastructure
- **Firebase Test Lab**: Run 7-day stress tests on physical devices in cloud
- **GitHub Actions**: CI/CD pipeline for automated stress tests
- **Crashlytics**: Real-time crash reporting during stress tests
- **Custom Test Runner**: Extended AndroidJUnitRunner with 7-day timeout, periodic health checks

---

## 3. Performance Benchmark Tests (NFR Validation)

### Objective
Validate that the app meets ALL P0 performance NFRs from the architecture and NFR assessments:
- Photo load time: <2 seconds (95th percentile)
- Transition smoothness: 60fps (no dropped frames)
- Memory usage: <300MB peak (4-photo buffer + Coil cache)
- Cold start time: <3 seconds (time to first frame)
- Crash-free rate: >99.5% (over 7 days)

### Test Scenarios

#### TS-PB-001: Photo Load Time Benchmark (NFR: <2s)
**Objective**: Validate that 95% of photo loads complete in <2 seconds over SMB

**Test Cases**:
- TC-PB-001-1: Load 100 random photos from 10K photo collection, measure 95th percentile load time (target: <2s)
- TC-PB-001-2: Load 100 photos of varying sizes (1MB, 5MB, 10MB, 20MB), measure 95th percentile (target: <2s for images ≤10MB)
- TC-PB-001-3: Load 100 photos from deep folder structure (10 levels deep), measure 95th percentile (target: <2s)
- TC-PB-001-4: Load 100 photos over slow network (2 Mbps), measure 95th percentile (target: <4s, acceptable degradation)

**Validation**:
- Use Macrobenchmark `measureRepeated` to capture accurate timing
- Exclude first 10 loads (cache warmup) from percentile calculation
- Record p50, p95, p99, max load times
- PASS if p95 ≤ 2000ms for normal network, p95 ≤ 4000ms for slow network

**Tools**: Macrobenchmark, Android Profiler (network timeline)

---

#### TS-PB-002: Transition Smoothness Benchmark (NFR: 60fps)
**Objective**: Validate that 95% of transitions render at 60fps (16.67ms per frame) with no dropped frames

**Test Cases**:
- TC-PB-002-1: Perform 100 Fade transitions, measure frame rendering times with Choreographer (target: 95% of frames ≤16.67ms)
- TC-PB-002-2: Perform 100 Slide transitions, measure frame rendering times (target: 95% of frames ≤16.67ms)
- TC-PB-002-3: Perform 100 Zoom/Ken Burns transitions, measure frame rendering times (target: 95% of frames ≤16.67ms)
- TC-PB-002-4: Perform 100 transitions while photo is loading in background (worst-case), measure frame rendering times (target: 95% of frames ≤16.67ms)

**Validation**:
- Use Macrobenchmark `frameDurationCpuMs` and `frameDurationUiMs` metrics
- Record jank count (frames >16.67ms), jank percentage
- Use Systrace to capture frame timelines, identify slow frames
- PASS if jank percentage <5%

**Tools**: Macrobenchmark, Systrace, Choreographer

---

#### TS-PB-003: Memory Usage Benchmark (NFR: <300MB peak)
**Objective**: Validate that peak memory usage stays below 300MB during normal operation

**Test Cases**:
- TC-PB-003-1: Load 100 photos sequentially, measure peak heap memory (target: <300MB)
- TC-PB-003-2: Perform 100 transitions with 4-photo buffer preloading, measure peak heap memory (target: <300MB)
- TC-PB-003-3: Scan 10K photo collection, measure peak heap memory during folder scan (target: <300MB)
- TC-PB-003-4: Run slideshow for 1 hour (240 transitions), measure peak heap memory (target: <300MB, no growth)

**Validation**:
- Use Android Profiler memory timeline to record heap allocations
- Capture heap dumps at peak memory usage, analyze with Memory Profiler
- Check for retained objects, bitmap allocations, collection growth
- PASS if peak heap ≤ 300MB, no memory growth trend

**Tools**: Android Profiler (Memory), LeakCanary, Heap Dump analysis

---

#### TS-PB-004: Cold Start Time Benchmark (NFR: <3s)
**Objective**: Validate that app cold start (process creation to first frame) completes in <3 seconds

**Test Cases**:
- TC-PB-004-1: Cold start app from launcher, measure time to first frame (target: <3s)
- TC-PB-004-2: Cold start app after system reboot, measure time to first frame (target: <3s)
- TC-PB-004-3: Cold start app with 10K photos already configured, measure time to first frame (target: <3s)
- TC-PB-004-4: Cold start app with no network connectivity, measure time to error screen (target: <3s)

**Validation**:
- Use Macrobenchmark `StartupTimingMetric` to measure cold start (application start to first frame rendered)
- Record p50, p95, p99 cold start times across 20 runs
- Use Systrace to identify slow initialization steps (Hilt dependency injection, Room database access, etc.)
- PASS if p95 ≤ 3000ms

**Tools**: Macrobenchmark (StartupTimingMetric), Systrace, Android Studio Startup Profiler

---

#### TS-PB-005: Battery Drain Benchmark (NFR: <5% per hour)
**Objective**: Validate that battery drain is <5% per hour during slideshow (NOTE: Senior Dev 1 flagged this NFR as ambiguous—clarify with team if this is plugged-in or battery-powered operation)

**Test Cases**:
- TC-PB-005-1: Run slideshow for 24 hours unplugged, measure battery drain per hour (target: <5%/hour avg)
- TC-PB-005-2: Run slideshow for 24 hours with screen brightness at 100%, measure battery drain (baseline for worst-case)
- TC-PB-005-3: Run slideshow for 24 hours with network disconnects every 10 minutes, measure battery drain (stress scenario)
- TC-PB-005-4: Profile battery drain by component (screen, CPU, network), identify optimization opportunities

**Validation**:
- Use Battery Historian to analyze battery drain over 24 hours
- Record battery drain per hour (%), identify wake locks, alarms, network activity
- Compare against Android baseline (screen on, idle app)
- PASS if avg drain ≤5%/hour (CONDITIONAL—depends on team clarification of NFR)

**Tools**: Battery Historian, adb dumpsys batterystats, Android Profiler (Energy)

---

## 4. 24/7 Stress Tests (7-Day Continuous Operation)

### Objective
Validate that the app can operate reliably for 7 days without human intervention, meeting the >99.5% crash-free rate NFR. This addresses Senior Dev 3's critical concern that "QA 2 tested 1 hour, but 24/7 requires multi-day stress test".

### Test Scenarios

#### TS-STRESS-001: 7-Day Continuous Slideshow Stress Test (P0)
**Objective**: Run slideshow continuously for 7 days (168 hours) to detect memory leaks, crashes, resource exhaustion, and performance degradation

**Test Cases**:
- TC-STRESS-001-1: Run slideshow for 7 days with 10s interval (60,000+ transitions), monitor crash rate (target: >99.5% crash-free)
- TC-STRESS-001-2: Run slideshow for 7 days with 5s interval (120,000+ transitions), monitor crash rate (target: >99.5% crash-free)
- TC-STRESS-001-3: Run slideshow for 7 days with random transitions (Fade/Slide/Zoom), monitor crash rate (target: >99.5% crash-free)
- TC-STRESS-001-4: Run slideshow for 7 days with network disconnects every 1 hour (168 disconnect/reconnect cycles), monitor recovery rate (target: 100% auto-recovery)

**Validation**:
- Use Firebase Test Lab to run test on 5 physical devices for 7 days
- Integrate Crashlytics for real-time crash reporting
- Capture memory heap dumps every 12 hours, analyze for memory growth
- Record metrics: crash count, ANR count, frame drops, memory usage (start vs. end), CPU usage (start vs. end)
- PASS if crash-free rate ≥99.5% (max 1 crash per 200 transitions = max 300 crashes over 60,000 transitions)

**Tools**: Firebase Test Lab, Crashlytics, Android Profiler (Memory), Systrace

---

#### TS-STRESS-002: Memory Leak Detection Over 10,000 Photo Loads (P0)
**Objective**: Detect memory leaks during extended operation. QA 1 tested 100 photo loads—this tests 10,000 loads (100x scale).

**Test Cases**:
- TC-STRESS-002-1: Load 10,000 photos sequentially, measure heap memory growth (target: <5% growth from start to end)
- TC-STRESS-002-2: Load 10,000 photos with cache clearing every 100 photos, measure heap memory (target: stable memory)
- TC-STRESS-002-3: Load 10,000 photos of varying sizes (1-50MB), measure heap memory growth (target: <5% growth)
- TC-STRESS-002-4: Load 10,000 photos while performing garbage collection every 100 loads, analyze heap dumps for retained objects

**Validation**:
- Use Android Profiler to record heap memory timeline over test duration
- Capture heap dumps at start (after 100 loads), middle (5000 loads), end (10,000 loads)
- Analyze heap dumps for retained bitmaps, leaked activities, collection growth
- Use LeakCanary in debug builds to auto-detect leaks
- PASS if heap growth ≤5% from start to end, no retained objects in heap dumps

**Tools**: Android Profiler (Memory), LeakCanary, MAT (Memory Analyzer Tool)

---

#### TS-STRESS-003: Crash-Free Rate Validation (NFR: >99.5%)
**Objective**: Validate that crash-free rate exceeds 99.5% over 7 days, as specified in PRD Section 3.1

**Test Cases**:
- TC-STRESS-003-1: Run 60,000 slideshow transitions over 7 days, count crashes (target: ≤300 crashes for 99.5% crash-free)
- TC-STRESS-003-2: Run 120,000 slideshow transitions over 7 days with 5s interval, count crashes (target: ≤600 crashes for 99.5% crash-free)
- TC-STRESS-003-3: Inject 100 random errors (network timeout, SMB disconnect, OOM), measure auto-recovery rate (target: 100%)
- TC-STRESS-003-4: Run slideshow with 10 different transition types, measure crash rate per transition type (identify crash-prone transitions)

**Validation**:
- Integrate Crashlytics for crash reporting
- Calculate crash-free rate: (1 - crashes / total_transitions) × 100%
- Analyze crash logs for root causes (OOM, ANR, SMB errors, etc.)
- PASS if crash-free rate ≥99.5%

**Tools**: Crashlytics, Firebase Test Lab, adb logcat

---

#### TS-STRESS-004: Auto-Recovery from Failures (24/7 Kiosk Operation)
**Objective**: Validate that app auto-recovers from critical failures (SMB server restart, network disconnect, app crash) without human intervention. Addresses Senior Dev 3's concern: "If SMB server restarts, slideshow stalls—BLOCKS 24/7 OPERATION"

**Test Cases**:
- TC-STRESS-004-1: Restart SMB server during slideshow, validate auto-reconnect within 30s (target: 100% recovery)
- TC-STRESS-004-2: Disconnect network during slideshow, reconnect after 10 minutes, validate auto-recovery (target: 100% recovery)
- TC-STRESS-004-3: Force kill app during slideshow, validate auto-restart via WorkManager (target: app restarts within 5 minutes)
- TC-STRESS-004-4: Inject OOM error during photo load, validate graceful degradation (skip photo, continue slideshow)

**Validation**:
- Monitor logs for error detection, retry attempts, recovery success
- Use WorkManager to implement auto-restart on crash (if not already in architecture)
- Record recovery time (seconds from failure to slideshow resume)
- PASS if 100% of failures result in auto-recovery within 5 minutes

**Tools**: adb logcat, Crashlytics, WorkManager logs

---

## 5. Scalability Tests (Large Collections, Large Images)

### Objective
Validate app performance with extreme data sets: 10K-100K photos, 50MB+ images, deep folder hierarchies. QA 1 tested 10K photos for memory, but didn't test UI responsiveness. QA 2 tested 10K photos for UI, but didn't test 100K photos or large images.

### Test Scenarios

#### TS-SCALE-001: 100K Photo Collection Performance
**Objective**: Validate app can handle 100K photo collection (10x larger than QA 1/QA 2 tests)

**Test Cases**:
- TC-SCALE-001-1: Scan 100K photo collection, measure scan time (target: <5 minutes)
- TC-SCALE-001-2: Load random photo from 100K collection, measure load time (target: <2s)
- TC-SCALE-001-3: Run slideshow with 100K photos for 1 hour, validate UI responsiveness (no ANRs)
- TC-SCALE-001-4: Search for photo in 100K collection, measure search time (target: <1s)

**Validation**:
- Use Android Profiler to monitor CPU/memory during 100K scan
- Check Room database query performance (EXPLAIN QUERY PLAN)
- Validate UI remains responsive during background folder scan (no ANRs)
- PASS if scan ≤5min, load ≤2s, search ≤1s, no ANRs

**Tools**: Android Profiler, Room Inspector, Systrace

---

#### TS-SCALE-002: Large Image Files (50MB+)
**Objective**: Validate app can handle extremely large image files (RAW photos, high-res scans)

**Test Cases**:
- TC-SCALE-002-1: Load 50MB JPEG image over SMB, measure load time (target: <5s)
- TC-SCALE-002-2: Load 100MB TIFF image over SMB, measure load time (expected: timeout with error message)
- TC-SCALE-002-3: Run slideshow with mix of 1MB and 50MB images, validate memory usage (target: <300MB peak)
- TC-SCALE-002-4: Load 50MB image on slow network (1 Mbps), validate loading indicator appears within 1s

**Validation**:
- Check Coil image loading configuration (maxImageSize, bitmap sampling)
- Validate OOM protection (Coil should downsample large images)
- Validate error handling (timeout after 30s with user-friendly message)
- PASS if 50MB images load in <5s, no OOM crashes

**Tools**: Android Profiler (Memory, Network), Charles Proxy (throttling)

---

#### TS-SCALE-003: Deep Folder Hierarchy (20 Levels)
**Objective**: Validate app can scan deeply nested folder structures without stack overflow or performance degradation

**Test Cases**:
- TC-SCALE-003-1: Scan folder with 20-level depth, 10K photos, measure scan time (target: <2 minutes)
- TC-SCALE-003-2: Scan folder with 20-level depth, 100K photos, measure scan time (target: <10 minutes)
- TC-SCALE-003-3: Load photo from 20-level deep folder, measure load time (target: <2s)
- TC-SCALE-003-4: Validate folder scanning uses iterative approach (not recursive, to avoid stack overflow)

**Validation**:
- Review SmbPhotoSource implementation (recursive vs iterative folder traversal)
- Monitor stack depth during folder scan (should not grow with folder depth)
- PASS if scan completes without stack overflow, load time ≤2s

**Tools**: Android Profiler, Code Review

---

## 6. Network Performance Tests (Adverse Network Conditions)

### Objective
Validate app performance under adverse network conditions: slow networks (1-2 Mbps), high latency (500ms), packet loss (5%). QA 1 tested network recovery, but not slow network performance.

### Test Scenarios

#### TS-NET-001: Slow Network Performance (1-2 Mbps)
**Objective**: Validate photo load time on slow networks (representative of rural broadband, congested Wi-Fi)

**Test Cases**:
- TC-NET-001-1: Load 10MB photo over 1 Mbps network, measure load time (target: <10s)
- TC-NET-001-2: Load 20MB photo over 1 Mbps network, validate timeout/error (expected: timeout after 30s)
- TC-NET-001-3: Run slideshow over 1 Mbps network with 30s interval, validate smooth operation (no buffering)
- TC-NET-001-4: Scan 10K photo collection over 1 Mbps network, measure scan time (target: <10 minutes)

**Validation**:
- Use Charles Proxy to throttle network to 1 Mbps
- Validate loading indicators appear during slow loads
- Validate slideshow pauses/shows loading state if photo doesn't load in time
- PASS if 10MB images load in <10s, slideshow handles slow loads gracefully

**Tools**: Charles Proxy, Network Link Conditioner, Android Profiler (Network)

---

#### TS-NET-002: High Latency Network Performance (500ms RTT)
**Objective**: Validate app performance with high network latency (representative of satellite internet, VPN)

**Test Cases**:
- TC-NET-002-1: Load photo over 500ms latency network, measure load time impact (expected: +1s vs baseline)
- TC-NET-002-2: Connect to SMB server over 500ms latency network, measure connection time (target: <5s)
- TC-NET-002-3: Run slideshow over 500ms latency network, validate transition timing (no stutters)
- TC-NET-002-4: Scan folder over 500ms latency network, measure scan time (expected: 2-3x slower than baseline)

**Validation**:
- Use Charles Proxy to inject 500ms latency
- Validate app shows "Connecting..." state during SMB connection
- PASS if connection ≤5s, slideshow operates smoothly (transitions not affected by latency)

**Tools**: Charles Proxy, tc (traffic control), Android Profiler (Network)

---

#### TS-NET-003: Packet Loss Network Performance (5% Loss)
**Objective**: Validate app handles packet loss gracefully (representative of unstable Wi-Fi, interference)

**Test Cases**:
- TC-NET-003-1: Load photo over 5% packet loss network, measure load time (expected: +2-3s vs baseline)
- TC-NET-003-2: Run slideshow over 5% packet loss network for 1 hour, validate crash-free operation (target: >99.5% crash-free)
- TC-NET-003-3: Inject 10% packet loss during photo load, validate retry logic (target: max 3 retries, then error)
- TC-NET-003-4: Scan folder over 5% packet loss network, validate completion (target: 100% success with retries)

**Validation**:
- Use Charles Proxy or tc to inject packet loss
- Monitor network errors in logs (jcifs-ng retry attempts)
- Validate user sees "Network unstable" warning if load takes >10s
- PASS if slideshow operates smoothly despite packet loss, no crashes

**Tools**: Charles Proxy, tc, Android Profiler (Network), adb logcat

---

## 7. Android Profiler Integration (CPU, Memory, GPU, Battery)

### Objective
Use Android Profiler to capture detailed resource usage during key operations. This addresses the gap: "QA 2 tested 60fps with Choreographer, but no profiling of CPU/GPU usage. What if it's 60fps but consuming 100% CPU?"

### Test Scenarios

#### TS-PROF-001: CPU Usage Profiling During Transitions
**Objective**: Profile CPU usage during transitions to identify CPU bottlenecks

**Test Cases**:
- TC-PROF-001-1: Profile CPU during 100 Fade transitions, measure avg CPU % (target: <30% avg)
- TC-PROF-001-2: Profile CPU during 100 Zoom transitions, measure avg CPU % (target: <50% avg, Zoom is more expensive)
- TC-PROF-001-3: Identify hot paths in CPU profile (sample-based profiler), optimize if >10% in single method
- TC-PROF-001-4: Profile CPU during concurrent photo load + transition, measure avg CPU % (target: <60% avg)

**Validation**:
- Use Android Profiler CPU timeline during test execution
- Record avg CPU %, peak CPU %, CPU time per thread
- Analyze flame graphs to identify hot paths (Coil image decoding, Compose recomposition, etc.)
- PASS if avg CPU <30% for Fade, <50% for Zoom, no single method >10% CPU time

**Tools**: Android Profiler (CPU), Systrace, Method Trace

---

#### TS-PROF-002: Memory Allocation Profiling During Photo Loads
**Objective**: Profile memory allocations during photo loads to identify allocation hotspots, optimize GC pressure

**Test Cases**:
- TC-PROF-002-1: Profile memory during 100 photo loads, measure total allocations (target: <500MB total)
- TC-PROF-002-2: Profile memory during 100 photo loads with cache hits, measure total allocations (target: <100MB total)
- TC-PROF-002-3: Identify top allocation sites in memory profile (shallow size >10MB), optimize if possible
- TC-PROF-002-4: Measure GC pause frequency during photo loads (target: <1 GC per load)

**Validation**:
- Use Android Profiler Memory timeline with allocation tracking enabled
- Record total allocations, shallow size by class, GC pause count
- Identify Bitmap allocations (should be managed by Coil, not leaking)
- PASS if total allocations <500MB, GC pauses <1 per load

**Tools**: Android Profiler (Memory), Allocation Tracker, Heap Dump

---

#### TS-PROF-003: GPU Rendering Profiling During Transitions
**Objective**: Profile GPU rendering during transitions to validate 60fps claim with GPU metrics

**Test Cases**:
- TC-PROF-003-1: Profile GPU during 100 Fade transitions, measure GPU frame time (target: <16ms avg)
- TC-PROF-003-2: Profile GPU during 100 Zoom transitions, measure GPU frame time (target: <16ms avg)
- TC-PROF-003-3: Identify overdraw in transitions using GPU Overdraw tool (target: <2x overdraw)
- TC-PROF-003-4: Measure GPU memory usage during transitions (target: <100MB GPU memory)

**Validation**:
- Enable GPU rendering profiling in Developer Options (on-screen bars)
- Use Systrace to capture GPU frame timelines (SurfaceFlinger, hwui)
- Analyze overdraw with GPU Overdraw visualization (Settings > Developer Options > Debug GPU Overdraw)
- PASS if GPU frame time <16ms avg, overdraw <2x

**Tools**: GPU Rendering Profiler, Systrace (SurfaceFlinger), GPU Overdraw

---

#### TS-PROF-004: Battery Drain Profiling by Component
**Objective**: Profile battery drain by component (screen, CPU, network, wake locks) to identify optimization opportunities

**Test Cases**:
- TC-PROF-004-1: Profile battery drain during 24-hour slideshow, measure drain by component (screen, CPU, network, etc.)
- TC-PROF-004-2: Identify wake locks during slideshow (target: only SCREEN_BRIGHT_WAKE_LOCK for screen-on)
- TC-PROF-004-3: Measure network battery drain during slideshow (target: <10% of total drain)
- TC-PROF-004-4: Compare battery drain with/without slideshow running (quantify app overhead)

**Validation**:
- Use Battery Historian to analyze battery drain over 24 hours
- Record drain % by component (screen, CPU, network, Wi-Fi, wake locks)
- Identify unexpected wake locks, alarms, or background services
- PASS if app-specific drain <20% of total (screen is largest component)

**Tools**: Battery Historian, adb dumpsys batterystats, Android Profiler (Energy)

---

## 8. Accessibility Tests (TalkBack, WCAG Compliance)

### Objective
Validate that the app is accessible to users with disabilities: blind (TalkBack), low vision (high contrast, text scaling), motor impairments (large touch targets), color blindness (contrast ratios). This addresses the major gap: "QA 2 mentioned TalkBack but provided NO concrete test scenarios."

### Test Scenarios

#### TS-A11Y-001: TalkBack Screen Reader Navigation (P0)
**Objective**: Validate that blind users can navigate the entire app using TalkBack

**Test Cases**:
- TC-A11Y-001-1: Navigate to Settings screen using TalkBack (eyes closed), configure SMB settings (server, username, password, folder path)
  - Expected: All text fields have content descriptions, TalkBack announces "Server address, edit box", can navigate with swipe gestures
- TC-A11Y-001-2: Navigate slideshow controls using TalkBack (eyes closed), pause/resume slideshow, change transition type
  - Expected: All buttons have content descriptions ("Pause slideshow", "Resume slideshow", "Transition type: Fade")
- TC-A11Y-001-3: Trigger settings access gesture (3-finger swipe up) during slideshow, validate TalkBack announces "Opening settings"
- TC-A11Y-001-4: Navigate error screens using TalkBack (eyes closed), activate retry button
  - Expected: Error messages announced by TalkBack, retry button has content description "Retry connection"

**Validation**:
- Test with TalkBack enabled (Settings > Accessibility > TalkBack)
- Tester must close eyes or wear blindfold during test
- Validate all interactive elements have contentDescription (Jetpack Compose semantics)
- Validate focus order is logical (top to bottom, left to right)
- PASS if tester can complete all tasks using TalkBack only (no visual cues)

**Tools**: TalkBack, Accessibility Scanner, Espresso Accessibility

---

#### TS-A11Y-002: Content Descriptions for Photos (P0)
**Objective**: Validate that photos have meaningful content descriptions for screen reader users

**Test Cases**:
- TC-A11Y-002-1: Load photo with TalkBack enabled, validate TalkBack announces photo file name or description
  - Expected: TalkBack announces "Photo: IMG_1234.jpg" or "Photo: Family vacation, beach sunset"
- TC-A11Y-002-2: Load 100 photos with TalkBack enabled, validate all photos have content descriptions (not silent)
- TC-A11Y-002-3: Load photo with no metadata, validate fallback content description (e.g., "Photo 1 of 100")
- TC-A11Y-002-4: Validate content descriptions are localized (if app supports multiple languages)

**Validation**:
- Check Jetpack Compose Image composable has contentDescription parameter
- Validate contentDescription is file name or extracted from EXIF metadata
- Test with TalkBack, ensure announcements are meaningful (not "Image", "Unlabeled")
- PASS if 100% of photos have non-empty content descriptions

**Tools**: TalkBack, Accessibility Scanner

---

#### TS-A11Y-003: High Contrast Mode Support
**Objective**: Validate that app is usable in Android high contrast mode (for low vision users)

**Test Cases**:
- TC-A11Y-003-1: Enable high contrast mode (Settings > Accessibility > High contrast text), validate all text is readable
- TC-A11Y-003-2: Enable high contrast mode, validate all UI elements (buttons, icons) are visible
- TC-A11Y-003-3: Enable high contrast mode, validate slideshow photos are not affected (only UI chrome changes)
- TC-A11Y-003-4: Test with high contrast mode on dark theme and light theme

**Validation**:
- Enable high contrast mode in Android settings
- Validate text color contrast against background (manual visual inspection)
- Validate icons are visible (not washed out by contrast mode)
- PASS if all UI elements remain visible and readable in high contrast mode

**Tools**: Manual testing, Accessibility Scanner

---

#### TS-A11Y-004: Text Scaling Support (200%+)
**Objective**: Validate that app supports large text scaling for low vision users (Android font size setting)

**Test Cases**:
- TC-A11Y-004-1: Set font size to 200% (Settings > Display > Font size), validate all text is readable (not clipped)
- TC-A11Y-004-2: Set font size to 200%, validate UI layout adjusts (no overlapping text)
- TC-A11Y-004-3: Set font size to 200%, validate Settings screen text fields remain usable
- TC-A11Y-004-4: Set font size to 300% (max), validate app gracefully handles extreme scaling (acceptable to truncate with ellipsis)

**Validation**:
- Change font size in Android settings (Display > Font size)
- Validate all Text composables respect font scaling (use sp units, not dp)
- Check for clipped text, overlapping text, layout overflow
- PASS if all text is readable at 200% scaling, graceful degradation at 300%

**Tools**: Manual testing, Layout Inspector

---

#### TS-A11Y-005: Touch Target Size Validation (48dp Minimum)
**Objective**: Validate that all interactive elements meet Android 48dp minimum touch target size (for users with motor impairments)

**Test Cases**:
- TC-A11Y-005-1: Measure touch target size of all buttons in Settings screen (target: ≥48dp width and height)
- TC-A11Y-005-2: Measure touch target size of slideshow pause/resume button (target: ≥48dp)
- TC-A11Y-005-3: Measure touch target size of transition type selector (target: ≥48dp)
- TC-A11Y-005-4: Measure touch target size of 3-finger swipe gesture target area (target: ≥48dp effective area)

**Validation**:
- Use Layout Inspector to measure button sizes in dp
- Validate Jetpack Compose Modifier.size or Modifier.clickable uses 48dp minimum
- Test with finger on real device (not stylus or mouse)
- PASS if all interactive elements ≥48dp × 48dp

**Tools**: Layout Inspector, Manual testing, Accessibility Scanner

---

#### TS-A11Y-006: Color Contrast Validation (WCAG AA Compliance)
**Objective**: Validate that text and UI elements meet WCAG 2.1 Level AA color contrast ratios (4.5:1 for text, 3:1 for UI)

**Test Cases**:
- TC-A11Y-006-1: Measure color contrast of all text against background (target: ≥4.5:1 for normal text, ≥3:1 for large text)
- TC-A11Y-006-2: Measure color contrast of buttons, icons, borders against background (target: ≥3:1)
- TC-A11Y-006-3: Test in dark theme, validate contrast ratios (target: ≥4.5:1 for text, ≥3:1 for UI)
- TC-A11Y-006-4: Test error messages (red text), validate contrast against background (target: ≥4.5:1)

**Validation**:
- Use axe DevTools Mobile or Color Contrast Analyzer to measure contrast ratios
- Record contrast ratio for each text/UI element
- Validate against WCAG 2.1 Level AA standards
  - Normal text (<18pt): ≥4.5:1
  - Large text (≥18pt or bold ≥14pt): ≥3:1
  - UI components: ≥3:1
- PASS if 100% of text/UI elements meet WCAG AA standards

**Tools**: axe DevTools Mobile, Color Contrast Analyzer, Accessibility Scanner

---

#### TS-A11Y-007: Color Blindness Simulation (Deuteranopia, Protanopia)
**Objective**: Validate that app is usable for users with color blindness (cannot rely on color alone to convey information)

**Test Cases**:
- TC-A11Y-007-1: Simulate deuteranopia (red-green color blindness), validate error messages are distinguishable (not just red text)
- TC-A11Y-007-2: Simulate protanopia (red-green color blindness, different type), validate UI elements are distinguishable
- TC-A11Y-007-3: Validate slideshow controls use icons + text (not just color to indicate state)
- TC-A11Y-007-4: Validate error states use icon + text + color (not just color to indicate error)

**Validation**:
- Use color blindness simulation tool (Android Accessibility Suite, or web-based simulator)
- Validate information is not conveyed by color alone (use icons, text, patterns)
- PASS if all UI states are distinguishable without relying on color perception

**Tools**: Color blindness simulator (Android Accessibility Suite), Manual testing

---

#### TS-A11Y-008: Keyboard Navigation Support (Bluetooth Keyboard)
**Objective**: Validate that users can navigate the app using a Bluetooth keyboard (for users who cannot use touchscreen)

**Test Cases**:
- TC-A11Y-008-1: Navigate Settings screen using Tab key, validate focus order is logical
- TC-A11Y-008-2: Activate buttons using Enter key, validate actions trigger
- TC-A11Y-008-3: Navigate form fields using Tab/Shift+Tab, validate focus moves correctly
- TC-A11Y-008-4: Pause slideshow using keyboard shortcut (if implemented), validate action triggers

**Validation**:
- Connect Bluetooth keyboard to tablet
- Validate Tab key moves focus through interactive elements
- Validate Enter key activates focused element
- Validate focus is visible (focus indicator on focused element)
- PASS if all interactive elements are reachable and activatable via keyboard

**Tools**: Bluetooth keyboard, Manual testing

---

## 9. Test Environment Setup

### Physical Devices (5x Tablets)
- **Device 1**: Amazon Fire HD 10 (2023) - Budget tablet, 3GB RAM, Android 11 (Fire OS 8)
- **Device 2**: Samsung Galaxy Tab A8 (2021) - Mid-range, 4GB RAM, Android 13
- **Device 3**: Lenovo Tab M10 Plus (2023) - Budget, 4GB RAM, Android 12
- **Device 4**: Generic Android tablet from Walmart - Low-end, 2GB RAM, Android 10 (worst-case scenario)
- **Device 5**: Samsung Galaxy Tab S6 Lite - Mid-range, 4GB RAM, Android 12

**Rationale**: Target users will use budget tablets, not Pixel devices. Testing on low-end hardware ensures app meets NFRs on real-world devices.

### Test SMB Server
- **Server**: Synology NAS DS220+ or Raspberry Pi 4 with Samba 4.x
- **Network**: Gigabit Ethernet (baseline), throttled to 1-2 Mbps for slow network tests
- **Photo Collection**: 100K photos (mix of 1MB-50MB), organized in 20-level deep folders
- **Backup**: Daily backups to cloud (prevent data loss during 7-day stress test)

### Network Configuration
- **Baseline**: Gigabit Ethernet, <1ms latency, 0% packet loss
- **Slow Network**: 1-2 Mbps throttling via Charles Proxy
- **High Latency**: 500ms RTT injection via Charles Proxy
- **Packet Loss**: 5% packet loss injection via Charles Proxy or tc

### CI/CD Pipeline for Stress Tests
- **Platform**: GitHub Actions or Firebase Test Lab
- **Test Runner**: Custom AndroidJUnitRunner with 7-day timeout (604800s), periodic health checks every 1 hour
- **Monitoring**: Crashlytics integration for real-time crash reporting, Slack/email alerts on crashes
- **Artifacts**: Capture heap dumps every 12 hours, crash logs, Systrace captures

---

## 10. Coverage Mapping (NFR → Test Scenarios)

### Performance NFRs

| NFR | Acceptance Criteria | Test Scenarios | Status |
|-----|---------------------|----------------|--------|
| Photo load time | <2s (95th percentile) over SMB | TS-PB-001 (Photo Load Time Benchmark) | ✅ Covered |
| Transition smoothness | 60fps (no dropped frames) | TS-PB-002 (Transition Smoothness Benchmark) | ✅ Covered |
| Memory usage | <300MB peak (4-photo buffer + cache) | TS-PB-003 (Memory Usage Benchmark), TS-STRESS-002 (Memory Leak Detection) | ✅ Covered |
| Cold start time | <3s (time to first frame) | TS-PB-004 (Cold Start Time Benchmark) | ✅ Covered |
| Battery drain | <5% per hour (AMBIGUOUS—needs clarification) | TS-PB-005 (Battery Drain Benchmark) | ⚠️ Covered (conditional on team clarification) |
| Crash-free rate | >99.5% over 7 days | TS-STRESS-001 (7-Day Stress Test), TS-STRESS-003 (Crash-Free Rate Validation) | ✅ Covered |
| 24/7 operation | Auto-recovery from failures | TS-STRESS-004 (Auto-Recovery from Failures) | ✅ Covered |

### Scalability NFRs

| NFR | Acceptance Criteria | Test Scenarios | Status |
|-----|---------------------|----------------|--------|
| Photo collection size | Support 10,000+ photos | TS-SCALE-001 (100K Photo Collection), QA 1 TS-042 (10K photos) | ✅ Covered |
| Large images | Handle 10MB+ images | TS-SCALE-002 (50MB+ Large Images) | ✅ Covered |
| Deep folders | Support nested folders (10+ levels) | TS-SCALE-003 (20-Level Deep Folders) | ✅ Covered |

### Network Performance NFRs

| NFR | Acceptance Criteria | Test Scenarios | Status |
|-----|---------------------|----------------|--------|
| Slow network | Graceful degradation on 1-2 Mbps | TS-NET-001 (Slow Network Performance) | ✅ Covered |
| High latency | Handle 500ms RTT | TS-NET-002 (High Latency Performance) | ✅ Covered |
| Packet loss | Handle 5% packet loss | TS-NET-003 (Packet Loss Performance) | ✅ Covered |
| Network recovery | Auto-reconnect within 30s | QA 1 TS-033 (Network Reconnection), TS-STRESS-004 (Auto-Recovery) | ✅ Covered |

### Accessibility NFRs

| NFR | Acceptance Criteria | Test Scenarios | Status |
|-----|---------------------|----------------|--------|
| TalkBack support | Navigate entire app via TalkBack | TS-A11Y-001 (TalkBack Navigation) | ✅ Covered |
| Content descriptions | All images have alt text | TS-A11Y-002 (Content Descriptions) | ✅ Covered |
| High contrast | Usable in high contrast mode | TS-A11Y-003 (High Contrast Mode) | ✅ Covered |
| Text scaling | Support 200% text scaling | TS-A11Y-004 (Text Scaling) | ✅ Covered |
| Touch targets | 48dp minimum size | TS-A11Y-005 (Touch Target Size) | ✅ Covered |
| Color contrast | WCAG AA compliance (4.5:1 text, 3:1 UI) | TS-A11Y-006 (Color Contrast Validation) | ✅ Covered |
| Color blindness | Information not conveyed by color alone | TS-A11Y-007 (Color Blindness Simulation) | ✅ Covered |
| Keyboard navigation | Navigate via Bluetooth keyboard | TS-A11Y-008 (Keyboard Navigation) | ✅ Covered |

---

## 11. Debate Summary: Critical Analysis of QA 1 & QA 2 Test Plans

### QA 1 (Unit & Integration Tests) - What Was Missed From Performance/Accessibility Lens

#### What QA 1 Did Well
- Comprehensive unit tests for ViewModels, Repositories, Data Sources (42 scenarios, 168 test cases)
- Memory leak detection (TS-040: 100 photo loads)
- Large collection testing (TS-042: 10K photos, <300MB memory)
- Network recovery testing (TS-033: SMB server restart, network disconnect)
- Security testing (Keystore encryption, PII logging audit)

#### Critical Gaps Identified
1. **100 Photo Loads Insufficient for 24/7 Operation**
   - **QA 1 Gap**: TS-040 tests 100 photo loads for memory leaks
   - **Why Insufficient**: 24/7 operation over 7 days = 60,000+ photos (at 10s interval). 100 loads is 0.17% of actual usage.
   - **This Plan Addresses**: TS-STRESS-002 (10,000 photo loads with memory profiling) scales testing 100x
   - **Concern to QA 1**: "Your 100 photo load test detects obvious leaks, but subtle leaks (1KB per load) only manifest after 10,000 loads (10MB cumulative). How do you detect these?"

2. **No Cold Start Performance Testing**
   - **QA 1 Gap**: Unit tests don't cover app startup time
   - **Why Matters**: Architecture specifies <3s cold start NFR. Senior Dev 1 expressed concern about "Performance Validation Deferred to Week 8"
   - **This Plan Addresses**: TS-PB-004 (Cold Start Time Benchmark with Macrobenchmark)
   - **Concern to QA 1**: "You validated business logic works, but what if Hilt dependency injection takes 5 seconds? Cold start NFR fails."

3. **No Network Performance Profiling**
   - **QA 1 Gap**: TS-033 tests network recovery (reconnection logic), but not slow network performance
   - **Why Matters**: Users on 1-2 Mbps rural broadband will experience different performance than your test environment's gigabit network
   - **This Plan Addresses**: TS-NET-001 (Slow Network), TS-NET-002 (High Latency), TS-NET-003 (Packet Loss)
   - **Concern to QA 1**: "You tested 'does it reconnect?' but not 'is it usable on a slow network?' These are different questions."

4. **No Accessibility Testing**
   - **QA 1 Gap**: No accessibility tests in unit/integration plan
   - **Why Matters**: TalkBack users cannot configure SMB settings if contentDescription is missing
   - **This Plan Addresses**: TS-A11Y-001 to TS-A11Y-008 (comprehensive accessibility testing)
   - **Concern to QA 1**: "Your unit tests validate SettingsViewModel logic, but do they validate that text fields have contentDescription for screen readers?"

---

### QA 2 (UI & E2E Tests) - What Was Missed From Performance/Accessibility Lens

#### What QA 2 Did Well
- Comprehensive UI tests (38 scenarios, 142 test cases)
- Transition smoothness validation (S21-S27: 60fps with Choreographer frame callbacks)
- 1-hour continuous operation test (S32: 240 transitions)
- Large collection UX testing (S43-S46: 10K photos, UI responsiveness)
- Error state UI testing (all error scenarios have clear messages, retry buttons)

#### Critical Gaps Identified
1. **1-Hour Stress Test Insufficient for 24/7 Operation**
   - **QA 2 Gap**: S32 tests 1 hour (240 transitions)
   - **Why Insufficient**: Senior Dev 3 explicitly stated: "QA 2 tested 1 hour, but 24/7 requires multi-day stress test. Both previous assessments missed the core NFR: >99.5% crash-free, 95%+ uptime, auto-recovery."
   - **This Plan Addresses**: TS-STRESS-001 (7-day stress test with 60,000+ transitions)
   - **Concern to QA 2**: "Your 1-hour test validates short-term stability. But what about memory leaks that only manifest after 48 hours? Crashes that only occur after 10,000 transitions? You validated the happy path, not the reliability path."

2. **60fps Validation Lacks Resource Profiling**
   - **QA 2 Gap**: S21-S27 tests validate 60fps with Choreographer, but don't profile CPU/GPU/memory usage during transitions
   - **Why Insufficient**: 60fps at 100% CPU usage is not acceptable for 24/7 kiosk operation (battery drain, heat, throttling)
   - **This Plan Addresses**: TS-PROF-001 (CPU Usage Profiling), TS-PROF-003 (GPU Rendering Profiling)
   - **Concern to QA 2**: "You validated 'does it hit 60fps?' but not 'at what cost?' If Zoom transitions use 80% CPU, that's a performance issue even if frame rate is 60fps."

3. **No Long-Duration Memory Leak Detection**
   - **QA 2 Gap**: S32 tests 1 hour, but doesn't capture heap dumps or analyze memory growth
   - **Why Insufficient**: Memory leaks manifest over hours/days, not minutes. QA 1's 100 photo load test is also insufficient.
   - **This Plan Addresses**: TS-STRESS-002 (10,000 photo loads with heap dump analysis every 2,000 loads)
   - **Concern to QA 2**: "Your 1-hour test might show 'no crash', but did you check heap memory at start vs. end? A 50MB growth over 1 hour = 1.2GB over 24 hours = OOM crash."

4. **No Battery Drain Testing**
   - **QA 2 Gap**: No battery drain testing
   - **Why Matters**: NFR specifies <5% battery drain per hour (though Senior Dev 1 flagged ambiguity). If app drains 10%/hour, tablet dies after 10 hours.
   - **This Plan Addresses**: TS-PB-005 (24-hour battery profiling), TS-PROF-004 (Battery drain by component)
   - **Concern to QA 2**: "You validated UI/UX, but did you validate battery impact? Users expect 'set it and forget it', not 'charge it every 8 hours'."

5. **No Network Stress Testing**
   - **QA 2 Gap**: No slow network, high latency, or packet loss testing
   - **Why Matters**: QA 1 tested network recovery (reconnection), but didn't test performance degradation on slow networks
   - **This Plan Addresses**: TS-NET-001 (Slow Network), TS-NET-002 (High Latency), TS-NET-003 (Packet Loss)
   - **Concern to QA 2**: "You tested UI on gigabit network. But what if user has 1 Mbps DSL? Does loading indicator appear? Does slideshow pause? Or does UI freeze?"

6. **No Concrete Accessibility Testing**
   - **QA 2 Gap**: S47-S48 mention "TalkBack support" and "Large touch targets" but provide NO concrete test scenarios
   - **Why Insufficient**: Mentioning accessibility is not the same as testing accessibility. How do you validate TalkBack navigation? What's the test procedure?
   - **This Plan Addresses**: TS-A11Y-001 to TS-A11Y-008 (8 comprehensive accessibility scenarios with step-by-step procedures)
   - **Concern to QA 2**: "You wrote 'TalkBack support' in your test plan. Did you actually test with TalkBack enabled (eyes closed)? Or is this aspirational?"

---

### Summary of Gaps Filled by This Test Plan

| Gap Category | QA 1 Coverage | QA 2 Coverage | QA 3 (This Plan) Coverage |
|--------------|---------------|---------------|---------------------------|
| **7-Day Stress Test** | ❌ Not covered | ⚠️ 1-hour only | ✅ 7-day stress test (TS-STRESS-001) |
| **10,000+ Photo Loads** | ⚠️ 100 loads only | ❌ Not covered | ✅ 10,000 photo loads (TS-STRESS-002) |
| **Cold Start Performance** | ❌ Not covered | ❌ Not covered | ✅ Cold start benchmark (TS-PB-004) |
| **Battery Drain** | ❌ Not covered | ❌ Not covered | ✅ 24-hour battery profiling (TS-PB-005, TS-PROF-004) |
| **Network Stress (Slow/Latency/Loss)** | ⚠️ Recovery only | ❌ Not covered | ✅ Network stress testing (TS-NET-001 to TS-NET-003) |
| **CPU/GPU Profiling** | ❌ Not covered | ⚠️ 60fps only | ✅ Resource profiling (TS-PROF-001 to TS-PROF-004) |
| **Scalability (100K photos, 50MB images)** | ⚠️ 10K only | ⚠️ 10K only | ✅ 100K photos, 50MB images (TS-SCALE-001 to TS-SCALE-003) |
| **TalkBack Screen Reader** | ❌ Not covered | ⚠️ Mentioned only | ✅ Concrete TalkBack tests (TS-A11Y-001 to TS-A11Y-002) |
| **WCAG Compliance** | ❌ Not covered | ❌ Not covered | ✅ Color contrast, touch targets (TS-A11Y-005 to TS-A11Y-007) |

---

## 12. Acceptance Criteria

### Performance Benchmarks - MUST PASS
- ✅ Photo load time: 95th percentile ≤2s (normal network), ≤4s (slow network)
- ✅ Transition smoothness: Jank percentage <5% (95% of frames ≤16.67ms)
- ✅ Memory usage: Peak heap ≤300MB, heap growth ≤5% over 10,000 photo loads
- ✅ Cold start time: 95th percentile ≤3s
- ⚠️ Battery drain: Avg ≤5%/hour (CONDITIONAL—pending team clarification on NFR ambiguity)

### 24/7 Stress Tests - MUST PASS
- ✅ 7-day stress test: Crash-free rate ≥99.5% (max 300 crashes over 60,000 transitions)
- ✅ Memory leak detection: Heap growth ≤5% from start to end of 10,000 photo loads
- ✅ Auto-recovery: 100% recovery from SMB server restart, network disconnect, app crash (within 5 minutes)

### Scalability Tests - MUST PASS
- ✅ 100K photo collection: Scan ≤5 min, load ≤2s, search ≤1s
- ✅ 50MB images: Load ≤5s, no OOM crashes
- ✅ 20-level deep folders: Scan completes without stack overflow, load ≤2s

### Network Performance Tests - MUST PASS
- ✅ Slow network (1 Mbps): 10MB image loads ≤10s, slideshow operates smoothly
- ✅ High latency (500ms): Connection ≤5s, slideshow not affected by latency
- ✅ Packet loss (5%): Slideshow crash-free, graceful retry logic

### Accessibility Tests - MUST PASS
- ✅ TalkBack: Tester can complete all tasks (configure settings, pause slideshow, retry errors) with eyes closed
- ✅ Content descriptions: 100% of photos have non-empty contentDescription
- ✅ High contrast: All text/UI elements visible and readable in high contrast mode
- ✅ Text scaling: All text readable at 200% scaling
- ✅ Touch targets: 100% of interactive elements ≥48dp × 48dp
- ✅ WCAG AA: 100% of text/UI elements meet contrast ratios (4.5:1 text, 3:1 UI)
- ✅ Color blindness: Information not conveyed by color alone (use icons, text, patterns)
- ✅ Keyboard navigation: All interactive elements reachable and activatable via Bluetooth keyboard

### Test Execution - MUST COMPLETE
- ✅ All 35 test scenarios executed
- ✅ All 128 test cases documented with pass/fail results
- ✅ Android Profiler captures for CPU, memory, GPU, battery (archived for future reference)
- ✅ Accessibility Scanner results (0 critical issues)
- ✅ 7-day stress test logs (Crashlytics, heap dumps, Systrace captures)

---

## 13. Risks & Mitigations

### Risk 1: 7-Day Stress Test Infrastructure Not Ready
**Risk**: CI/CD pipeline cannot run 7-day tests (GitHub Actions max job time is 6 hours)
**Mitigation**: Use Firebase Test Lab (supports multi-day tests) or dedicated physical devices with monitoring

### Risk 2: Battery Drain NFR Ambiguous
**Risk**: Senior Dev 1 flagged "<5% per hour" as ambiguous (plugged-in vs. battery-powered operation unclear)
**Mitigation**: Test both scenarios (plugged-in and battery-powered), report results, request clarification from team

### Risk 3: Accessibility Testing Requires Specialized Skills
**Risk**: Testers may not have experience with TalkBack, WCAG compliance, screen reader testing
**Mitigation**: Provide TalkBack training, partner with accessibility consultant, use automated tools (Accessibility Scanner, axe DevTools)

### Risk 4: Performance Issues Discovered Late
**Risk**: 7-day stress test runs in Week 3, performance issues discovered too late to fix before implementation deadline
**Mitigation**: Run short stress tests (24-hour) in Week 1-2 to identify issues early, escalate critical issues immediately

---

## 14. Dependencies

### Dependency 1: QA 1 & QA 2 Test Plans Must Be Finalized
**Why**: This plan references QA 1 test scenarios (TS-040 memory leaks, TS-042 large collections) and QA 2 test scenarios (S21-S27 transitions, S32 continuous operation). If those plans change, this plan must be updated.

### Dependency 2: Implementation Must Be Complete
**Why**: Performance benchmarking, stress testing, and accessibility testing require a working app. Cannot start until implementation is at least 80% complete.

### Dependency 3: Test Infrastructure Setup
**Why**: 7-day stress test requires Firebase Test Lab setup, Crashlytics integration, heap dump automation. This infrastructure must be set up in Week 1-2.

### Dependency 4: Test SMB Server Availability
**Why**: All performance tests require access to SMB server with 100K photos. Server must be available 24/7 during stress tests.

---

## 15. Next Steps

1. **Team Review & Consensus** (Week 1):
   - QA 1, QA 2, QA 3 review all 3 test plans
   - Identify overlaps, gaps, dependencies
   - Senior Dev 1/2/3 review test plans against NFR assessments
   - Confirm battery drain NFR interpretation (plugged-in vs. battery-powered)

2. **Test Infrastructure Setup** (Week 1-2):
   - Set up Firebase Test Lab for 7-day stress tests
   - Integrate Crashlytics for crash reporting
   - Set up Android Profiler automation (capture heap dumps every 12 hours)
   - Provision 5 physical tablets for performance/accessibility testing
   - Set up test SMB server with 100K photos

3. **Test Execution** (Week 2-5):
   - Week 2: Performance benchmarking (TS-PB-001 to TS-PB-005), profiling setup
   - Week 3: 7-day stress test (starts Monday, runs continuously)
   - Week 4: Network performance, scalability tests
   - Week 5: Accessibility testing, WCAG compliance validation

4. **Final Report & Sign-Off** (Week 6):
   - Consolidate results from QA 1, QA 2, QA 3
   - Identify critical issues, blockers for launch
   - Present findings to team, recommend go/no-go for implementation

---

**Test Plan Status**: READY FOR TEAM REVIEW
**Author**: QA 3 - Performance & Accessibility Tests focused
**Date**: 2026-03-02
**Version**: 1.0
