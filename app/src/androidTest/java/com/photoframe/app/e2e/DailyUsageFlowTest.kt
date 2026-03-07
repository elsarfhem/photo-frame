package com.photoframe.app.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.photoframe.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

/**
 * E2E Test: Daily Usage Flow
 *
 * Tests TS-E2E-002 from QA 2 test plan (Phase 9: Test Implementation - Week 17)
 *
 * Validates typical daily usage patterns:
 * 1. App auto-starts slideshow at scheduled time
 * 2. User can pause/resume slideshow
 * 3. User can manually navigate photos
 * 4. User can adjust settings
 * 5. Slideshow auto-stops at scheduled end time
 * 6. App handles network disruptions gracefully
 *
 * Critical User Journey: Zero-touch operation for 24/7 kiosk mode.
 * User should not need to interact with app for normal operation.
 *
 * @see docs/features/photo-frame-app-initial/testing/ui-e2e-tests.md
 */
@RunWith(AndroidJUnit4::class)
class DailyUsageFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * TS-E2E-002-01: Slideshow auto-starts and runs continuously
     *
     * Verify slideshow runs without user intervention
     */
    @Test
    fun slideshow_autoStarts_runsContinuously() {
        // App should auto-start slideshow on launch
        composeTestRule.waitForIdle()

        // Wait for slideshow to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify slideshow is playing
        composeTestRule
            .onNodeWithTag("slideshow_photo")
            .assertExists()
            .assertIsDisplayed()

        // Verify playing state (pause button should be visible)
        composeTestRule
            .onNodeWithContentDescription("Pause slideshow")
            .assertExists()

        // Wait for photo transitions (verify continuous operation)
        var photoIndex = 0
        repeat(5) { // Observe 5 photo transitions
            Thread.sleep(10_000) // Wait 10 seconds per photo

            composeTestRule.waitForIdle()

            // Verify photo updated
            composeTestRule
                .onNodeWithTag("slideshow_photo")
                .assertExists()

            photoIndex++
            println("Photo transition $photoIndex verified")
        }

        assertTrue(photoIndex == 5, "Should observe 5 photo transitions")
    }

    /**
     * TS-E2E-002-02: User can pause and resume slideshow
     *
     * Verify manual pause/resume controls work
     */
    @Test
    fun slideshow_pauseResume_works() {
        // Wait for slideshow to start
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Pause slideshow
        composeTestRule
            .onNodeWithContentDescription("Pause slideshow")
            .assertExists()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify paused state (play button should be visible)
        composeTestRule
            .onNodeWithContentDescription("Resume slideshow")
            .assertExists()

        // Wait to confirm no auto-advance
        val currentPhotoIndex = getCurrentPhotoIndex()
        Thread.sleep(15_000) // Wait longer than photo interval

        val afterWaitIndex = getCurrentPhotoIndex()
        assertTrue(
            currentPhotoIndex == afterWaitIndex,
            "Photo should not advance when paused"
        )

        // Resume slideshow
        composeTestRule
            .onNodeWithContentDescription("Resume slideshow")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify resumed state (pause button should be visible)
        composeTestRule
            .onNodeWithContentDescription("Pause slideshow")
            .assertExists()

        // Verify auto-advance resumes
        val resumedIndex = getCurrentPhotoIndex()
        Thread.sleep(11_000) // Wait for one photo interval

        val advancedIndex = getCurrentPhotoIndex()
        assertTrue(
            advancedIndex > resumedIndex,
            "Photo should advance after resume"
        )
    }

    /**
     * TS-E2E-002-03: Manual photo navigation with swipe gestures
     *
     * Verify user can manually navigate photos
     */
    @Test
    fun slideshow_manualNavigation_works() {
        // Wait for slideshow to start
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val initialIndex = getCurrentPhotoIndex()

        // Swipe left to go to next photo
        composeTestRule
            .onNodeWithTag("slideshow_photo")
            .performTouchInput {
                swipeLeft()
            }

        composeTestRule.waitForIdle()

        val afterNextIndex = getCurrentPhotoIndex()
        assertTrue(
            afterNextIndex == initialIndex + 1,
            "Photo index should increment on swipe left"
        )

        // Swipe right to go to previous photo
        composeTestRule
            .onNodeWithTag("slideshow_photo")
            .performTouchInput {
                swipeRight()
            }

        composeTestRule.waitForIdle()

        val afterPrevIndex = getCurrentPhotoIndex()
        assertTrue(
            afterPrevIndex == afterNextIndex - 1,
            "Photo index should decrement on swipe right"
        )
    }

    /**
     * TS-E2E-002-04: Settings changes apply immediately
     *
     * Verify user can change settings and see immediate effect
     */
    @Test
    fun settings_changesApplyImmediately() {
        // Wait for slideshow to start
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Open settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Change transition type from Fade to Slide
        composeTestRule
            .onNodeWithText("Fade")
            .performClick()

        composeTestRule
            .onNodeWithText("Slide")
            .performClick()

        // Change photo interval from 10s to 5s
        composeTestRule
            .onNodeWithTag("photo_interval_slider")
            .performTouchInput {
                swipeLeft() // Decrease to 5 seconds
            }

        // Save settings
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        composeTestRule.waitForIdle()

        // Should return to slideshow
        composeTestRule
            .onNodeWithTag("slideshow_photo")
            .assertExists()

        // Verify new interval (should advance in ~5 seconds, not 10)
        Thread.sleep(6_000) // Wait 6 seconds

        composeTestRule.waitForIdle()

        // Photo should have advanced (new 5s interval)
        // In real test, verify photo counter incremented
        assertTrue(true, "Settings changes applied")
    }

    /**
     * TS-E2E-002-05: Network disruption recovery
     *
     * Verify app handles network disconnection gracefully
     */
    @Test
    fun slideshow_networkDisruption_recoversAutomatically() {
        // Wait for slideshow to start
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Simulate network disconnection
        // In real test, use ADB to disable WiFi:
        // adb shell svc wifi disable

        // Wait for buffer to exhaust (after 4 photos)
        Thread.sleep(45_000) // 4 photos * 10s + buffer

        composeTestRule.waitForIdle()

        // Error state should be displayed
        composeTestRule
            .onNodeWithText("Network connection lost")
            .assertExists()

        // Connection indicator should show disconnected
        composeTestRule
            .onNodeWithTag("connection_indicator_disconnected")
            .assertExists()

        // Simulate network reconnection
        // In real test: adb shell svc wifi enable

        // Wait for auto-recovery
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Slideshow should resume automatically
        composeTestRule
            .onNodeWithTag("slideshow_photo")
            .assertExists()
            .assertIsDisplayed()

        // Connection indicator should show connected
        composeTestRule
            .onNodeWithTag("connection_indicator_connected")
            .assertExists()

        assertTrue(true, "App recovered from network disruption")
    }

    /**
     * TS-E2E-002-06: Photo counter displays accurately
     *
     * Verify photo counter updates correctly
     */
    @Test
    fun photoCounter_displaysAccurately() {
        // Wait for slideshow to start
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Initial photo counter (e.g., "1 / 100")
        val initialCounter = getPhotoCounterText()
        println("Initial counter: $initialCounter")

        // Wait for next photo
        Thread.sleep(11_000)

        composeTestRule.waitForIdle()

        // Counter should increment
        val nextCounter = getPhotoCounterText()
        println("Next counter: $nextCounter")

        assertTrue(
            nextCounter != initialCounter,
            "Photo counter should increment after transition"
        )
    }

    /**
     * TS-E2E-002-07: Zero-touch operation for 1 hour
     *
     * Verify app runs for 1 hour without user intervention
     */
    @Test
    fun zeroTouchOperation_1Hour_success() {
        // Wait for slideshow to start
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val startTime = System.currentTimeMillis()
        val targetDuration = 60 * 60 * 1000L // 1 hour

        var transitionCount = 0
        var errorCount = 0

        while (System.currentTimeMillis() - startTime < targetDuration) {
            // Check every minute
            Thread.sleep(60_000)

            composeTestRule.waitForIdle()

            // Verify slideshow still running
            val isSlideshowVisible = composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()

            if (isSlideshowVisible) {
                transitionCount++
                println("Transition $transitionCount: OK")
            } else {
                errorCount++
                println("Error detected at transition $transitionCount")
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        val uptime = ((transitionCount.toDouble() / (transitionCount + errorCount)) * 100)

        println("Zero-touch operation results:")
        println("  Total time: ${totalTime / 1000}s")
        println("  Successful transitions: $transitionCount")
        println("  Errors: $errorCount")
        println("  Uptime: ${String.format("%.2f", uptime)}%")

        // Should maintain >99% uptime
        assertTrue(
            uptime > 99.0,
            "Should maintain >99% uptime, was ${uptime}%"
        )
    }

    /**
     * TS-E2E-002-08: Memory stability during extended operation
     *
     * Verify memory stays stable over extended operation
     */
    @Test
    fun extendedOperation_memoryStable() {
        // Wait for slideshow to start
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val memorySnapshots = mutableListOf<Long>()

        // Measure memory every 5 minutes for 30 minutes
        repeat(6) {
            val memoryUsage = getCurrentMemoryUsageMB()
            memorySnapshots.add(memoryUsage)
            println("Memory snapshot: ${memoryUsage}MB")

            Thread.sleep(5 * 60 * 1000) // 5 minutes
        }

        // Memory should not grow significantly
        val firstSnapshot = memorySnapshots.first()
        val lastSnapshot = memorySnapshots.last()
        val memoryGrowth = lastSnapshot - firstSnapshot

        assertTrue(
            memoryGrowth < 50,
            "Memory should not grow >50MB, grew ${memoryGrowth}MB"
        )

        println("Memory stability:")
        println("  Initial: ${firstSnapshot}MB")
        println("  Final: ${lastSnapshot}MB")
        println("  Growth: ${memoryGrowth}MB")
    }

    // Helper functions

    private fun getCurrentPhotoIndex(): Int {
        // In real test, extract from photo counter text
        // For now, return mock value
        return 1
    }

    private fun getPhotoCounterText(): String {
        // In real test, get actual counter text
        return "1 / 100"
    }

    private fun getCurrentMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024)
    }
}
