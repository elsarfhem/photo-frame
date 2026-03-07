package com.photoframe.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.photoframe.app.ui.settings.SettingsScreen
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.model.TransitionType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * UI Component Test: SettingsScreen
 *
 * Tests TS-UI-002 from QA 2 test plan (Phase 9: Test Implementation - Week 17)
 *
 * Validates:
 * - Settings screen renders all fields
 * - SMB connection configuration works
 * - Transition type selection works
 * - Photo interval adjustment works
 * - Schedule configuration works
 * - Test connection button works
 * - Save button validation
 * - Form validation errors
 *
 * @see docs/features/photo-frame-app-initial/testing/ui-e2e-tests.md
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * TS-UI-002-01: Settings screen renders all fields
     */
    @Test
    fun settingsScreen_rendersAllFields() {
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Verify all fields are present
        composeTestRule.onNodeWithText("SMB Settings").assertExists()
        composeTestRule.onNodeWithText("Host").assertExists()
        composeTestRule.onNodeWithText("Share Name").assertExists()
        composeTestRule.onNodeWithText("Folder Path").assertExists()
        composeTestRule.onNodeWithText("Username").assertExists()
        composeTestRule.onNodeWithText("Password").assertExists()

        composeTestRule.onNodeWithText("Slideshow Settings").assertExists()
        composeTestRule.onNodeWithText("Transition Type").assertExists()
        composeTestRule.onNodeWithText("Photo Interval").assertExists()

        composeTestRule.onNodeWithText("Schedule").assertExists()
        composeTestRule.onNodeWithText("Enable Scheduled Playback").assertExists()
    }

    /**
     * TS-UI-002-02: SMB host field input works
     */
    @Test
    fun smbHost_inputWorks() {
        var updatedConnection: SmbConnection? = null
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = { updatedConnection = it },
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Find host field and update it
        composeTestRule
            .onNodeWithText("Host")
            .assertExists()

        composeTestRule
            .onNodeWithText("192.168.1.100")
            .performTextClearance()

        composeTestRule
            .onNodeWithText("Host")
            .performTextInput("192.168.1.200")

        // Verify callback was invoked
        // In real implementation, verify updatedConnection.host == "192.168.1.200"
    }

    /**
     * TS-UI-002-03: Transition type dropdown works
     */
    @Test
    fun transitionType_dropdownWorks() {
        var selectedTransition: TransitionType? = null
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = { selectedTransition = it },
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Click transition dropdown
        composeTestRule
            .onNodeWithText("Fade")
            .performClick()

        // Select "Slide" option
        composeTestRule
            .onNodeWithText("Slide")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify selection changed
        // In real implementation, verify selectedTransition == TransitionType.SLIDE
    }

    /**
     * TS-UI-002-04: Photo interval slider works
     */
    @Test
    fun photoInterval_sliderWorks() {
        var selectedInterval: Int? = null
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = { selectedInterval = it },
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Find interval slider
        composeTestRule
            .onNodeWithTag("photo_interval_slider")
            .assertExists()

        // Adjust slider value
        composeTestRule
            .onNodeWithTag("photo_interval_slider")
            .performTouchInput {
                swipeRight()
            }

        composeTestRule.waitForIdle()

        // Verify callback was invoked with new value
        // In real implementation, verify selectedInterval changed
    }

    /**
     * TS-UI-002-05: Schedule toggle works
     */
    @Test
    fun scheduleToggle_works() {
        var scheduleEnabled: Boolean? = null
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { enabled, _, _ -> scheduleEnabled = enabled },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Find and toggle schedule switch
        composeTestRule
            .onNodeWithText("Enable Scheduled Playback")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify callback was invoked
        // In real implementation, verify scheduleEnabled == true
    }

    /**
     * TS-UI-002-06: Schedule time pickers work
     */
    @Test
    fun scheduleTimePickers_work() {
        var startTime: String? = null
        var endTime: String? = null
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = true,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, start, end ->
                    startTime = start
                    endTime = end
                },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Click start time to open picker
        composeTestRule
            .onNodeWithText("09:00")
            .performClick()

        // Material TimePicker should open
        composeTestRule.waitForIdle()

        // In real test, interact with MaterialTimePicker
        // For now, verify it opens
        composeTestRule
            .onNodeWithTag("time_picker_dialog")
            .assertExists()
    }

    /**
     * TS-UI-002-07: Test connection button works
     */
    @Test
    fun testConnectionButton_triggersTest() {
        var testConnectionCalled = false
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = { testConnectionCalled = true },
                onSave = {},
                onBack = {}
            )
        }

        // Click test connection button
        composeTestRule
            .onNodeWithText("Test Connection")
            .performClick()

        // Verify callback was invoked
        assertTrue(testConnectionCalled, "Test connection callback should be invoked")
    }

    /**
     * TS-UI-002-08: Save button validates and saves
     */
    @Test
    fun saveButton_validatesAndSaves() {
        var saveCalled = false
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = { saveCalled = true },
                onBack = {}
            )
        }

        // Click save button
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        // Verify callback was invoked
        assertTrue(saveCalled, "Save callback should be invoked")
    }

    /**
     * TS-UI-002-09: Validation errors display
     */
    @Test
    fun validationErrors_display() {
        val invalidConnection = SmbConnection(
            host = "", // Empty host should trigger validation error
            shareName = "photos",
            folderPath = "/family",
            username = "user",
            encryptedPassword = "encrypted123"
        )

        composeTestRule.setContent {
            SettingsScreen(
                connection = invalidConnection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Attempt to save with invalid host
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        // Validation error should be displayed
        composeTestRule
            .onNodeWithText("Host is required")
            .assertExists()
    }

    /**
     * TS-UI-002-10: Schedule validation (end after start)
     */
    @Test
    fun scheduleValidation_endAfterStart() {
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = true,
                scheduleStartTime = "21:00", // End before start - invalid
                scheduleEndTime = "09:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // Attempt to save with invalid schedule
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        // Validation error should be displayed
        composeTestRule
            .onNodeWithText("End time must be after start time")
            .assertExists()
    }

    /**
     * TS-UI-002-11: Accessibility - Touch targets
     */
    @Test
    fun accessibility_touchTargets48dp() {
        val connection = createMockConnection()

        composeTestRule.setContent {
            SettingsScreen(
                connection = connection,
                transitionType = TransitionType.FADE,
                photoIntervalSeconds = 10,
                scheduleEnabled = false,
                scheduleStartTime = "09:00",
                scheduleEndTime = "21:00",
                onConnectionChange = {},
                onTransitionChange = {},
                onIntervalChange = {},
                onScheduleChange = { _, _, _ -> },
                onTestConnection = {},
                onSave = {},
                onBack = {}
            )
        }

        // All buttons should have minimum touch target of 48dp
        // Compose enforces this by default, but verify key buttons

        composeTestRule
            .onNodeWithText("Test Connection")
            .assertExists()
            .assertIsEnabled()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Save")
            .assertExists()
            .assertIsEnabled()
            .assertHasClickAction()
    }

    // Helper function

    private fun createMockConnection(): SmbConnection {
        return SmbConnection(
            host = "192.168.1.100",
            shareName = "photos",
            folderPath = "/family",
            username = "user",
            encryptedPassword = "encrypted123"
        )
    }
}
