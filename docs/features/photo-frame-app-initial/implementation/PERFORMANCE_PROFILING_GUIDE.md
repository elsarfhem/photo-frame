# Performance Profiling & Optimization Guide

**Phase 6: Polish & Bug Fixes**
**Date**: 2026-03-03

---

## Overview

This guide provides step-by-step instructions for profiling and optimizing the Photo Frame Android app to meet the performance NFRs defined in the PRD.

---

## Performance Targets (NFRs)

| Metric | Target | Tool |
|--------|--------|------|
| Photo load time | <2s (95th percentile) | Custom telemetry + Systrace |
| Transition smoothness | 60fps, <5% jank rate | Choreographer API + Systrace |
| Memory usage | <300MB peak | Android Profiler + LeakCanary |
| Cold start time | <3 seconds | Android Studio Profiler |
| Crash-free rate | >99.5% over 7 days | Crashlytics |

---

## Tools Required

1. **Android Studio Profiler** - CPU, Memory, Network profiling
2. **Systrace** - Frame rendering analysis
3. **LeakCanary** - Memory leak detection (already integrated)
4. **Crashlytics** - Crash tracking (already integrated)
5. **Battery Historian** - Battery usage analysis
6. **Compose Layout Inspector** - UI hierarchy and recomposition tracking

---

## Step-by-Step Profiling

### 1. Photo Load Time Profiling

**Goal**: Verify photo load time is <2s (95th percentile)

**Steps**:
1. Enable custom telemetry logging in `ImageCache.kt`:
   ```kotlin
   val startTime = System.currentTimeMillis()
   // ... load image
   val loadTime = System.currentTimeMillis() - startTime
   TelemetryLogger.logPhotoLoadTime(loadTime)
   ```

2. Run slideshow for 100 photo transitions
3. Extract telemetry logs:
   ```bash
   adb logcat | grep "PhotoLoadTime" > photo_load_times.log
   ```

4. Calculate 95th percentile:
   ```python
   import numpy as np
   times = [...]  # Parse from logs
   p95 = np.percentile(times, 95)
   print(f"95th percentile: {p95}ms")
   ```

**Expected Results**:
- Median: <1s
- 95th percentile: <2s

**Optimization if needed**:
- Reduce image dimensions in `ImageCache` (currently 2560x1600)
- Enable Coil disk cache: `diskCachePolicy(CachePolicy.ENABLED)`
- Preload more photos in `PhotoBufferManager` (increase from 4 to 6)

---

### 2. Transition Smoothness (60fps Target)

**Goal**: Verify 60fps with <5% jank rate

**Steps**:
1. Connect device via USB
2. Run slideshow with all 3 transition types
3. Record Systrace (15 seconds each):
   ```bash
   python systrace.py --time=15 -o trace_fade.html gfx view wm
   python systrace.py --time=15 -o trace_slide.html gfx view wm
   python systrace.py --time=15 -o trace_zoom.html gfx view wm
   ```

4. Analyze in Chrome (chrome://tracing):
   - Look for frame drops (red/yellow frames)
   - Check UI thread time (should be <16ms per frame)
   - Check RenderThread time

**Expected Results**:
- Frame time: 16ms (60fps)
- Jank rate: <5%

**Optimization if needed**:
- Reduce Compose recompositions:
  - Use `remember` for expensive calculations
  - Use `derivedStateOf` for computed state
  - Use `key()` to prevent unnecessary recompositions
- Offload work from UI thread:
  - Move image loading to `Dispatchers.IO`
  - Use `LaunchedEffect` for side effects
- Simplify transition animations:
  - Reduce animation duration
  - Use hardware-accelerated animations

---

### 3. Memory Usage (Peak <300MB)

**Goal**: Verify peak memory usage is <300MB

**Steps**:
1. Open Android Studio Profiler
2. Select "Memory" profiler
3. Start slideshow
4. Run for 1 hour with 10-second interval (360 photos)
5. Monitor:
   - Java Heap: Should be <200MB
   - Native Heap: Should be <100MB
   - Total: Should be <300MB peak

**Expected Results**:
- Stable memory usage (no continuous growth)
- No memory leaks detected by LeakCanary
- Bitmap allocation managed by Coil and `PhotoBufferManager`

**Optimization if needed**:
- Reduce buffer size in `PhotoBufferManager` (from 4 to 3 photos)
- Force garbage collection after buffer clear:
  ```kotlin
  bitmap.recycle()
  System.gc()
  ```
- Check for leaked coroutines (use `viewModelScope` correctly)

---

### 4. Cold Start Time (<3s)

**Goal**: Verify app cold start time is <3 seconds

**Steps**:
1. Force stop app:
   ```bash
   adb shell am force-stop com.photoframe.app
   ```

2. Launch with timing:
   ```bash
   adb shell am start -W com.photoframe.app/.MainActivity
   ```

3. Parse output:
   ```
   TotalTime: 2847  # Should be <3000ms
   ```

**Expected Results**:
- Cold start: <3 seconds
- Warm start: <1 second

**Optimization if needed**:
- Defer non-critical initialization (move to background thread)
- Use Hilt for lazy dependency injection
- Optimize Compose UI hierarchy (reduce nesting)
- Use `Modifier.drawWithCache()` for expensive drawing

---

### 5. Crash-Free Rate (>99.5%)

**Goal**: Verify crash-free rate >99.5% over 7 days

**Steps**:
1. Deploy to Firebase App Distribution (beta testing)
2. Run 7-day stress test on Firebase Test Lab:
   ```yaml
   # test_matrix.yaml
   - deviceModel: Pixel5
     androidVersion: 33
     orientation: landscape
     testScript: |
       - launch app
       - wait 1 minute
       - repeat 10080 times  # 7 days with 1-minute interval
   ```

3. Monitor Crashlytics dashboard:
   - Crash-free users: >99.5%
   - ANR rate: <0.1%
   - Fatal errors: 0

**Expected Results**:
- Crash-free sessions: >99.5%
- All P0 crashes fixed
- Auto-recovery working (CrashHandler)

**Optimization if needed**:
- Add try-catch for remaining crash-prone code paths
- Improve error handling in `SmbPhotoDataSource`
- Add telemetry for non-fatal errors

---

## Compose-Specific Profiling

### Recomposition Analysis

**Tool**: Compose Layout Inspector

**Steps**:
1. Open Android Studio > Layout Inspector
2. Enable "Show Recomposition Counts"
3. Navigate through app screens
4. Identify hot spots (high recomposition counts)

**Expected Results**:
- `SlideshowScreen`: Recompose only when photo changes
- `SettingsScreen`: Recompose only when form fields change

**Optimization**:
- Use `remember` for expensive calculations
- Use `derivedStateOf` for computed state
- Use `@Stable` and `@Immutable` on data classes (already done)
- Use `key()` for list items

---

## Battery Usage Analysis

**Tool**: Battery Historian

**Steps**:
1. Reset battery stats:
   ```bash
   adb shell dumpsys batterystats --reset
   ```

2. Run slideshow for 2 hours
3. Capture battery dump:
   ```bash
   adb bugreport > bugreport.zip
   ```

4. Upload to Battery Historian:
   ```
   https://bathist.ef.lc/
   ```

**Expected Results**:
- Wake locks released when slideshow paused
- CPU usage: Moderate (slideshow is photo display, not video)
- Network usage: Low (preload buffer minimizes requests)

**Optimization if needed**:
- Use WorkManager `setRequiresBatteryNotLow()` for scheduling
- Reduce polling frequency in `MemoryMonitor` (from 60s to 120s)
- Use `Dispatchers.Default` with limited parallelism

---

## Automated Performance Tests

### Unit Tests for Performance

```kotlin
@Test
fun `photo buffer manager - preload time under 5 seconds for 4 photos`() = runTest {
    val startTime = System.currentTimeMillis()
    bufferManager.preloadNext(photos, currentIndex = 0)
    val duration = System.currentTimeMillis() - startTime

    assertThat(duration).isLessThan(5000) // 5 seconds for 4 photos
}

@Test
fun `image cache - downsampling reduces memory by 90 percent`() {
    val original = loadBitmap("test_4k.jpg") // 4K image
    val downsampled = imageCache.load("test_4k.jpg", maxSize = 2560)

    val originalSize = original.byteCount
    val downsampledSize = downsampled.byteCount

    assertThat(downsampledSize).isLessThan(originalSize * 0.1) // 90% reduction
}
```

### UI Performance Tests

```kotlin
@Test
fun `slideshow transition - maintains 60fps`() {
    composeTestRule.setContent {
        SlideshowScreen(autoPlay = true)
    }

    val frameMetrics = captureFrameMetrics(duration = 10_000) // 10 seconds
    val jankRate = frameMetrics.filter { it > 16 }.size / frameMetrics.size.toFloat()

    assertThat(jankRate).isLessThan(0.05) // <5% jank rate
}
```

---

## Performance Checklist

Before releasing:

- [ ] Photo load time: <2s (95th percentile) verified
- [ ] Transition smoothness: 60fps, <5% jank verified
- [ ] Memory usage: <300MB peak verified
- [ ] Cold start time: <3s verified
- [ ] Crash-free rate: >99.5% over 7 days verified
- [ ] LeakCanary: No memory leaks detected
- [ ] Battery usage: Acceptable for 24/7 operation
- [ ] Compose recompositions: Optimized (no hot spots)
- [ ] Systrace analyzed: No UI thread blocking

---

## Known Performance Risks

### 1. Large Photo Collections (10,000+ photos)

**Risk**: Initial scan may timeout (30s limit)

**Mitigation**:
- `IncrementalPhotoLoader` loads first 100, then background loads rest
- Show progress indicator during initial load
- Consider implementing pagination or folder-based filtering

### 2. Very Large Photos (>4K)

**Risk**: OOM errors on low-memory devices

**Mitigation**:
- `ImageCache` downsamples to 2560x1600
- Coil automatically uses subsampling for very large images
- `MemoryMonitor` clears cache at 75% and 90% thresholds

### 3. Network Latency (High-latency SMB servers)

**Risk**: Photo load time exceeds 2s target

**Mitigation**:
- `PhotoBufferManager` preloads 4 photos (buffer absorbs latency)
- Exponential backoff retry in `SlideshowRepository`
- Connection pooling (potential future optimization)

### 4. Long-Running Operation (24/7)

**Risk**: Memory leaks over days/weeks

**Mitigation**:
- `SlideshowWatchdog` monitors for stalls
- `MemoryMonitor` preemptively clears cache
- `CrashHandler` auto-restarts on OOM
- LeakCanary enabled in debug builds

---

## Recommended Tools & Libraries

### Already Integrated
- ✅ Coil (efficient image loading with disk cache)
- ✅ LeakCanary (memory leak detection)
- ✅ Crashlytics (crash tracking)
- ✅ Custom telemetry (photo load time logging)

### Future Enhancements
- Firebase Performance Monitoring (automatic performance tracking)
- Macrobenchmark (startup and jank testing)
- Jetpack Benchmark (microbenchmarking critical paths)

---

## Summary

This guide provides comprehensive profiling steps to verify all performance NFRs are met. Run profiling at:
1. **Week 13** (after Phase 6 polish)
2. **Week 16** (mid-testing phase)
3. **Week 18** (before MVP release)

**Next Step**: Proceed to Phase 9 (Test Implementation & Execution) with confidence that performance monitoring is in place.

---

**Document Owner**: Phase 6 Implementation Team
**Last Updated**: 2026-03-03
**Status**: Ready for profiling
