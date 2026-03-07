package com.photoframe.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.photoframe.app.ui.slideshow.SlideshowScreen
import com.photoframe.core.model.Photo
import com.photoframe.core.model.SlideshowState
import com.photoframe.core.model.TransitionType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Component Test: SlideshowScreen
 *
 * Tests TS-UI-001 from QA 2 test plan (Phase 9: Test Implementation - Week 17)
 *
 * Validates:
 * - Slideshow screen renders correctly
 * - Photo displays properly
 * - Transition animations work
 * - Play/pause button functionality
 * - Settings button navigation
 * - Touch gestures (swipe)
 * - Connection status indicator
 *
 * @see docs/features/photo-frame-app-initial/testing/ui-e2e-tests.md
 */
@RunWith(AndroidJUnit4::class)
class SlideshowScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * TS-UI-001-01: Slideshow screen renders with photo
     */
    @Test
    fun slideshowScreen_rendersWithPhoto() {
        val photo = createMockPhoto(1)
        val state = SlideshowState.Playing(
            currentPhoto = photo,
            photoIndex = 0,
            totalPhotos = 10,
            isBuffering = false
        )

        composeTestRule.setContent {
            SlideshowScreen(
                state = state,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Verify photo is displayed
        composeTestRule
            .onNodeWithContentDescription("Current photo: photo_1.jpg")
            .assertExists()
    }

    /**
     * TS-UI-001-02: Play/pause button toggles correctly
     */
    @Test
    fun playPauseButton_togglesState() {
        var isPaused = false
        val photo = createMockPhoto(1)

        composeTestRule.setContent {
            SlideshowScreen(
                state = if (isPaused) {
                    SlideshowState.Paused(photo, 0, 10, false)
                } else {
                    SlideshowState.Playing(photo, 0, 10, false)
                },
                onPauseResume = { isPaused = !isPaused },
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Initially playing - should show pause icon
        composeTestRule
            .onNodeWithContentDescription("Pause slideshow")
            .assertExists()

        // Click to pause
        composeTestRule
            .onNodeWithContentDescription("Pause slideshow")
            .performClick()

        // Update state to paused
        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            SlideshowScreen(
                state = SlideshowState.Paused(photo, 0, 10, false),
                onPauseResume = { isPaused = !isPaused },
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Should now show play icon
        composeTestRule
            .onNodeWithContentDescription("Resume slideshow")
            .assertExists()
    }

    /**
     * TS-UI-001-03: Settings button navigates to settings
     */
    @Test
    fun settingsButton_triggersNavigation() {
        var settingsClicked = false
        val photo = createMockPhoto(1)
        val state = SlideshowState.Playing(photo, 0, 10, false)

        composeTestRule.setContent {
            SlideshowScreen(
                state = state,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = { settingsClicked = true }
            )
        }

        // Click settings button
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        // Verify callback was invoked
        assert(settingsClicked) { "Settings callback should be invoked" }
    }

    /**
     * TS-UI-001-04: Swipe gesture navigation works
     */
    @Test
    fun swipeGesture_triggersPhotoNavigation() {
        var nextClicked = false
        var prevClicked = false
        val photo = createMockPhoto(1)
        val state = SlideshowState.Playing(photo, 0, 10, false)

        composeTestRule.setContent {
            SlideshowScreen(
                state = state,
                onPauseResume = {},
                onNext = { nextClicked = true },
                onPrevious = { prevClicked = true },
                onSettings = {}
            )
        }

        // Swipe left to go to next photo
        composeTestRule
            .onNodeWithContentDescription("Current photo: photo_1.jpg")
            .performTouchInput {
                swipeLeft()
            }

        composeTestRule.waitForIdle()
        assert(nextClicked) { "Next photo callback should be invoked on swipe left" }

        // Swipe right to go to previous photo
        composeTestRule
            .onNodeWithContentDescription("Current photo: photo_1.jpg")
            .performTouchInput {
                swipeRight()
            }

        composeTestRule.waitForIdle()
        assert(prevClicked) { "Previous photo callback should be invoked on swipe right" }
    }

    /**
     * TS-UI-001-05: Connection status indicator displays correctly
     */
    @Test
    fun connectionStatus_displaysCorrectly() {
        val photo = createMockPhoto(1)
        val connectedState = SlideshowState.Playing(photo, 0, 10, false)
        val disconnectedState = SlideshowState.Error(
            message = "Network connection lost",
            canRetry = true
        )

        // Test connected state
        composeTestRule.setContent {
            SlideshowScreen(
                state = connectedState,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Connection indicator should be green/hidden when connected
        composeTestRule
            .onNodeWithTag("connection_indicator_connected")
            .assertExists()

        // Test disconnected state
        composeTestRule.setContent {
            SlideshowScreen(
                state = disconnectedState,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Connection indicator should be red when disconnected
        composeTestRule
            .onNodeWithTag("connection_indicator_disconnected")
            .assertExists()
    }

    /**
     * TS-UI-001-06: Loading/buffering state displays correctly
     */
    @Test
    fun bufferingState_showsLoadingIndicator() {
        val photo = createMockPhoto(1)
        val state = SlideshowState.Playing(
            currentPhoto = photo,
            photoIndex = 0,
            totalPhotos = 10,
            isBuffering = true
        )

        composeTestRule.setContent {
            SlideshowScreen(
                state = state,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Loading indicator should be visible when buffering
        composeTestRule
            .onNodeWithTag("buffering_indicator")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * TS-UI-001-07: Photo counter displays correctly
     */
    @Test
    fun photoCounter_displaysCorrectly() {
        val photo = createMockPhoto(1)
        val state = SlideshowState.Playing(
            currentPhoto = photo,
            photoIndex = 5,
            totalPhotos = 100,
            isBuffering = false
        )

        composeTestRule.setContent {
            SlideshowScreen(
                state = state,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Photo counter should show "6 / 100" (index + 1)
        composeTestRule
            .onNodeWithText("6 / 100")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * TS-UI-001-08: Transition animations execute
     */
    @Test
    fun transitionAnimations_execute() {
        val photo1 = createMockPhoto(1)
        val photo2 = createMockPhoto(2)

        composeTestRule.setContent {
            SlideshowScreen(
                state = SlideshowState.Playing(photo1, 0, 10, false),
                transitionType = TransitionType.FADE,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Take screenshot before transition
        // composeTestRule.onRoot().captureToImage()

        // Trigger transition
        composeTestRule.setContent {
            SlideshowScreen(
                state = SlideshowState.Playing(photo2, 1, 10, false),
                transitionType = TransitionType.FADE,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Wait for animation to complete
        composeTestRule.waitForIdle()

        // New photo should be visible
        composeTestRule
            .onNodeWithContentDescription("Current photo: photo_2.jpg")
            .assertExists()
    }

    /**
     * TS-UI-001-09: Error state displays correctly
     */
    @Test
    fun errorState_displaysErrorMessage() {
        val errorState = SlideshowState.Error(
            message = "Failed to load photos from SMB server",
            canRetry = true
        )

        composeTestRule.setContent {
            SlideshowScreen(
                state = errorState,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // Error message should be displayed
        composeTestRule
            .onNodeWithText("Failed to load photos from SMB server")
            .assertExists()
            .assertIsDisplayed()

        // Retry button should be visible
        composeTestRule
            .onNodeWithText("Retry")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * TS-UI-001-10: Accessibility - TalkBack support
     */
    @Test
    fun accessibility_talkBackSupport() {
        val photo = createMockPhoto(1)
        val state = SlideshowState.Playing(photo, 0, 10, false)

        composeTestRule.setContent {
            SlideshowScreen(
                state = state,
                onPauseResume = {},
                onNext = {},
                onPrevious = {},
                onSettings = {}
            )
        }

        // All interactive elements should have content descriptions
        composeTestRule
            .onNodeWithContentDescription("Pause slideshow")
            .assertExists()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Settings")
            .assertExists()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Current photo: photo_1.jpg")
            .assertExists()
    }

    // Helper function

    private fun createMockPhoto(index: Int): Photo {
        return Photo(
            path = "/test/photo_$index.jpg",
            filename = "photo_$index.jpg",
            fileSize = 5_000_000,
            lastModified = System.currentTimeMillis(),
            metadata = emptyMap()
        )
    }
}
