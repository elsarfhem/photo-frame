package com.photoframe.core.scheduling

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.photoframe.core.model.SlideshowSettings
import com.photoframe.core.model.TransitionType
import io.mockk.*
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.util.concurrent.ExecutionException
import com.google.common.util.concurrent.ListenableFuture

/**
 * Unit tests for ScheduleManager.
 *
 * Tests:
 * - Schedule creation
 * - Schedule cancellation
 * - Schedule status checking
 * - Delay calculation
 *
 * Phase 5: Settings & Scheduling
 *
 * Note: These tests mock WorkManager to avoid Android dependencies.
 */
class ScheduleManagerTest {

    private lateinit var scheduleManager: ScheduleManager
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        // Mock Context
        context = mockk(relaxed = true)

        // Mock WorkManager
        workManager = mockk(relaxed = true)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager
    }

    @Test
    fun `scheduleDaily creates start and stop work when schedule is enabled`() {
        scheduleManager = ScheduleManager(context)

        val settings = SlideshowSettings(
            displayIntervalSeconds = 10,
            transitionType = TransitionType.FADE,
            shuffleEnabled = false,
            scheduleEnabled = true,
            scheduleStartTime = LocalTime.of(8, 0),
            scheduleEndTime = LocalTime.of(22, 0)
        )

        val result = scheduleManager.scheduleDaily(settings)

        assert(result)
        verify(exactly = 2) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    @Test
    fun `scheduleDaily returns false when schedule is disabled`() {
        scheduleManager = ScheduleManager(context)

        val settings = SlideshowSettings(
            displayIntervalSeconds = 10,
            transitionType = TransitionType.FADE,
            shuffleEnabled = false,
            scheduleEnabled = false, // Disabled
            scheduleStartTime = LocalTime.of(8, 0),
            scheduleEndTime = LocalTime.of(22, 0)
        )

        val result = scheduleManager.scheduleDaily(settings)

        assert(!result)
        verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    @Test
    fun `cancelSchedule cancels both start and stop work`() {
        scheduleManager = ScheduleManager(context)

        scheduleManager.cancelSchedule()

        verify(exactly = 2) { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `isScheduleActive returns true when work is enqueued`() {
        scheduleManager = ScheduleManager(context)

        // Mock WorkInfo for enqueued work
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.ENQUEUED

        // Mock ListenableFuture
        val future = mockk<ListenableFuture<List<WorkInfo>>>()
        every { future.get() } returns listOf(workInfo)
        every { workManager.getWorkInfosForUniqueWork(any()) } returns future

        val result = scheduleManager.isScheduleActive()

        assert(result)
    }

    @Test
    fun `isScheduleActive returns false when work is cancelled`() {
        scheduleManager = ScheduleManager(context)

        // Mock WorkInfo for cancelled work
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.CANCELLED

        // Mock ListenableFuture
        val future = mockk<ListenableFuture<List<WorkInfo>>>()
        every { future.get() } returns listOf(workInfo)
        every { workManager.getWorkInfosForUniqueWork(any()) } returns future

        val result = scheduleManager.isScheduleActive()

        assert(!result)
    }

    @Test
    fun `isScheduleActive returns false when no work is found`() {
        scheduleManager = ScheduleManager(context)

        // Mock ListenableFuture with empty list
        val future = mockk<ListenableFuture<List<WorkInfo>>>()
        every { future.get() } returns emptyList()
        every { workManager.getWorkInfosForUniqueWork(any()) } returns future

        val result = scheduleManager.isScheduleActive()

        assert(!result)
    }

    @Test
    fun `scheduleDaily cancels existing schedule before creating new one`() {
        scheduleManager = ScheduleManager(context)

        val settings = SlideshowSettings(
            displayIntervalSeconds = 10,
            transitionType = TransitionType.FADE,
            shuffleEnabled = false,
            scheduleEnabled = true,
            scheduleStartTime = LocalTime.of(8, 0),
            scheduleEndTime = LocalTime.of(22, 0)
        )

        scheduleManager.scheduleDaily(settings)

        // Verify cancel was called before enqueue
        verifyOrder {
            workManager.cancelUniqueWork(any())
            workManager.cancelUniqueWork(any())
            workManager.enqueueUniquePeriodicWork(any(), any(), any())
            workManager.enqueueUniquePeriodicWork(any(), any(), any())
        }
    }

    @Test
    fun `scheduleDaily handles exceptions gracefully`() {
        scheduleManager = ScheduleManager(context)

        // Mock WorkManager to throw exception
        every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } throws RuntimeException("Test exception")

        val settings = SlideshowSettings(
            displayIntervalSeconds = 10,
            transitionType = TransitionType.FADE,
            shuffleEnabled = false,
            scheduleEnabled = true,
            scheduleStartTime = LocalTime.of(8, 0),
            scheduleEndTime = LocalTime.of(22, 0)
        )

        val result = scheduleManager.scheduleDaily(settings)

        assert(!result)
    }
}
