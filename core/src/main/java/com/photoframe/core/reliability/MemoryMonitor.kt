package com.photoframe.core.reliability

import com.photoframe.core.di.DefaultDispatcher
import com.photoframe.core.image.ImageCache
import com.photoframe.core.slideshow.PhotoBufferManager
import com.photoframe.core.telemetry.TelemetryLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors memory usage and performs preemptive cache clearing to prevent OOM crashes.
 *
 * P0 BLOCKING: Addresses "Memory leaks virtually guaranteed for 24/7 operation" (Senior Dev 3).
 *
 * Strategy:
 * - Monitor memory every 60 seconds in background coroutine
 * - Preemptive cache clearing at 75% memory threshold
 * - Emergency cache clear + GC at 90% threshold
 * - Log memory metrics to Crashlytics
 *
 * Thread Safety: Coroutine-based, no shared mutable state except StateFlow (atomic).
 *
 * Integration:
 * - Started in PhotoFrameApplication.onCreate()
 * - Uses ImageCache and PhotoBufferManager for cache clearing
 *
 * @param imageCache Image cache to clear when memory is low
 * @param photoBufferManager Photo buffer to clear when memory is critical
 * @param telemetryLogger Telemetry logger for Crashlytics integration
 * @param dispatcher Coroutine dispatcher for background monitoring
 */
@Singleton
class MemoryMonitor @Inject constructor(
    private val imageCache: ImageCache,
    private val photoBufferManager: PhotoBufferManager,
    private val telemetryLogger: TelemetryLogger,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(dispatcher)
    private var monitoringJob: Job? = null

    // Memory state
    private val _memoryState = MutableStateFlow<MemoryState>(MemoryState.Normal)
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    // Memory statistics
    private val _memoryStats = MutableStateFlow(MemoryStats(0L, 0L, 0L, 0.0))
    val memoryStats: StateFlow<MemoryStats> = _memoryStats.asStateFlow()

    /**
     * Starts memory monitoring.
     * Monitors every 60 seconds and takes action at thresholds.
     *
     * Thread Safety: Safe to call multiple times (idempotent).
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return // Already monitoring

        monitoringJob = scope.launch {
            while (true) {
                try {
                    checkMemoryUsage()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    // Log error but continue monitoring
                    // TODO: Log to Crashlytics
                }
            }
        }
    }

    /**
     * Stops memory monitoring.
     *
     * Thread Safety: Safe to call multiple times.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Checks current memory usage and takes action if needed.
     *
     * Thresholds:
     * - < 75%: Normal operation
     * - 75-90%: Preemptive cache clearing (memory only)
     * - > 90%: Emergency cache clearing (memory + disk) + explicit GC
     */
    private suspend fun checkMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val usagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100.0

        // Update stats
        _memoryStats.value = MemoryStats(
            maxMemory = maxMemory,
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            usagePercent = usagePercent
        )

        when {
            usagePercent >= CRITICAL_THRESHOLD_PERCENT -> {
                handleCriticalMemory(usagePercent)
            }
            usagePercent >= WARNING_THRESHOLD_PERCENT -> {
                handleWarningMemory(usagePercent)
            }
            else -> {
                _memoryState.value = MemoryState.Normal
            }
        }
    }

    /**
     * Handles warning threshold (75%): Preemptive cache clearing.
     *
     * Actions:
     * - Clear ImageCache memory cache (disk cache preserved)
     * - Log memory warning event
     * - Update state to Warning
     */
    private suspend fun handleWarningMemory(usagePercent: Double) {
        if (_memoryState.value is MemoryState.Warning) return // Already handled

        _memoryState.value = MemoryState.Warning(usagePercent)

        // Log event to Crashlytics
        telemetryLogger.logMemoryWarning(usagePercent)

        // Clear memory cache (preserve disk cache for performance)
        imageCache.clearMemoryCache()
    }

    /**
     * Handles critical threshold (90%): Emergency cache clearing + GC.
     *
     * Actions:
     * - Clear PhotoBufferManager (recycles bitmaps)
     * - Clear ImageCache memory and disk caches
     * - Explicit System.gc() request
     * - Log critical memory event
     * - Update state to Critical
     */
    private suspend fun handleCriticalMemory(usagePercent: Double) {
        if (_memoryState.value is MemoryState.Critical) return // Already handled

        _memoryState.value = MemoryState.Critical(usagePercent)

        // Log critical memory event
        telemetryLogger.logMemoryCritical(usagePercent)

        // Emergency cache clearing
        photoBufferManager.clear() // Recycles bitmaps
        imageCache.clearAllCaches() // Clears memory + disk

        // Request garbage collection (hint only, JVM decides)
        System.gc()

        // Log recovery attempt
        telemetryLogger.logMemoryRecoveryAttempted()
    }

    /**
     * Gets current memory usage as a percentage.
     *
     * @return Memory usage percentage (0.0 - 100.0)
     */
    fun getMemoryUsagePercent(): Double {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        return (usedMemory.toDouble() / maxMemory.toDouble()) * 100.0
    }

    /**
     * Gets formatted memory usage string for debugging.
     *
     * @return Memory usage string (e.g., "512MB / 1024MB (50%)")
     */
    fun getMemoryUsageString(): String {
        val stats = _memoryStats.value
        val usedMB = stats.usedMemory / (1024 * 1024)
        val maxMB = stats.maxMemory / (1024 * 1024)
        return "$usedMB MB / $maxMB MB (${stats.usagePercent.toInt()}%)"
    }

    companion object {
        /**
         * Memory monitoring interval: 60 seconds.
         */
        private const val MONITORING_INTERVAL_MS = 60_000L

        /**
         * Warning threshold: 75% memory usage.
         * Triggers preemptive cache clearing.
         */
        private const val WARNING_THRESHOLD_PERCENT = 75.0

        /**
         * Critical threshold: 90% memory usage.
         * Triggers emergency cache clearing + GC.
         */
        private const val CRITICAL_THRESHOLD_PERCENT = 90.0
    }
}

/**
 * Represents the current memory state.
 *
 * Thread Safety: Immutable sealed class, safe to share across threads.
 */
sealed class MemoryState {
    /**
     * Normal memory usage (< 75%).
     */
    object Normal : MemoryState()

    /**
     * Warning memory usage (75-90%).
     * Preemptive cache clearing in progress.
     */
    data class Warning(val usagePercent: Double) : MemoryState()

    /**
     * Critical memory usage (> 90%).
     * Emergency cache clearing + GC in progress.
     */
    data class Critical(val usagePercent: Double) : MemoryState()
}

/**
 * Memory statistics.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * @param maxMemory Maximum memory available to JVM (bytes)
 * @param totalMemory Total memory allocated by JVM (bytes)
 * @param usedMemory Memory currently used (bytes)
 * @param usagePercent Memory usage percentage (0.0 - 100.0)
 */
data class MemoryStats(
    val maxMemory: Long,
    val totalMemory: Long,
    val usedMemory: Long,
    val usagePercent: Double
)
