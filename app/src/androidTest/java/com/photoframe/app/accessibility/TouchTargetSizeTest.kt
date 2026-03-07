package com.photoframe.app.accessibility

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.photoframe.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

/**
 * Accessibility Test: Touch Target Size
 *
 * Tests TS-A11Y-002 from QA 3 test plan (Phase 9: Test Implementation - Week 18)
 *
 * Validates:
 * - P0 Accessibility: Minimum touch target size of 48dp
 * - All interactive elements are easily tappable
 * - Adequate spacing between touch targets
 *
 * WCAG 2.1 Guidelines:
 * - 2.5.5 Target Size (Level AAA) - 44x44dp minimum
 * - Android Accessibility Guidelines: 48x48dp minimum
 *
 * Material Design Guidelines:
 * - Minimum touch target: 48x48dp
 * - Recommended spacing: 8dp between targets
 *
 * @see docs/features/photo-frame-app-initial/testing/performance-accessibility-tests.md
 */
@RunWith(AndroidJUnit4::class)
class TouchTargetSizeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        private const val MIN_TOUCH_TARGET_DP = 48
        private const val MIN_SPACING_DP = 8
    }

    /**
     * TS-A11Y-002-01: All buttons meet 48dp minimum size
     *
     * Android Accessibility: 48x48dp minimum touch target
     */
    @Test
    fun allButtons_meet48dpMinimum() {
        composeTestRule.waitForIdle()

        // Slideshow screen buttons
        val slideshowButtons = listOf(
            "Pause slideshow",
            "Settings"
        )

        slideshowButtons.forEach { buttonDescription ->
            val nodes = composeTestRule
                .onAllNodesWithContentDescription(buttonDescription, useUnmergedTree = true)
                .fetchSemanticsNodes()

            nodes.forEach { node ->
                val bounds = node.boundsInRoot
                val widthDp = bounds.width.value
                val heightDp = bounds.height.value

                println("Button '$buttonDescription': ${widthDp}dp x ${heightDp}dp")

                assertTrue(
                    widthDp >= MIN_TOUCH_TARGET_DP && heightDp >= MIN_TOUCH_TARGET_DP,
                    "Button '$buttonDescription' should be at least ${MIN_TOUCH_TARGET_DP}dp, " +
                            "was ${widthDp}dp x ${heightDp}dp"
                )
            }
        }

        // Navigate to settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Settings screen buttons
        val settingsButtons = listOf(
            "Test Connection",
            "Save",
            "Navigate back"
        )

        settingsButtons.forEach { buttonDescription ->
            val nodes = composeTestRule
                .onAllNodesWithContentDescription(buttonDescription, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()

            nodes.forEach { node ->
                val bounds = node.boundsInRoot
                val widthDp = bounds.width.value
                val heightDp = bounds.height.value

                println("Button '$buttonDescription': ${widthDp}dp x ${heightDp}dp")

                assertTrue(
                    widthDp >= MIN_TOUCH_TARGET_DP && heightDp >= MIN_TOUCH_TARGET_DP,
                    "Button '$buttonDescription' should be at least ${MIN_TOUCH_TARGET_DP}dp, " +
                            "was ${widthDp}dp x ${heightDp}dp"
                )
            }
        }
    }

    /**
     * TS-A11Y-002-02: Toggle switches meet 48dp minimum
     *
     * Switches and checkboxes are critical accessibility targets
     */
    @Test
    fun toggleSwitches_meet48dpMinimum() {
        // Navigate to settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Find schedule enable toggle
        val toggleNodes = composeTestRule
            .onAllNodes(hasClickAction() and hasText("Enable Scheduled Playback", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()

        toggleNodes.forEach { node ->
            val bounds = node.boundsInRoot
            val widthDp = bounds.width.value
            val heightDp = bounds.height.value

            println("Toggle switch: ${widthDp}dp x ${heightDp}dp")

            assertTrue(
                heightDp >= MIN_TOUCH_TARGET_DP,
                "Toggle switch height should be at least ${MIN_TOUCH_TARGET_DP}dp, was ${heightDp}dp"
            )
        }
    }

    /**
     * TS-A11Y-002-03: Text input fields meet 48dp minimum height
     *
     * Input fields should have adequate tap target height
     */
    @Test
    fun textInputFields_meet48dpMinimumHeight() {
        // Navigate to settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Test input fields
        val inputFields = listOf(
            "Host",
            "Share Name",
            "Username",
            "Password"
        )

        inputFields.forEach { fieldLabel ->
            val nodes = composeTestRule
                .onAllNodesWithText(fieldLabel, useUnmergedTree = true)
                .fetchSemanticsNodes()

            // Find the input field itself (not just the label)
            val inputNode = nodes.firstOrNull { node ->
                node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text) ||
                        node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.EditableText)
            }

            if (inputNode != null) {
                val bounds = inputNode.boundsInRoot
                val heightDp = bounds.height.value

                println("Input field '$fieldLabel': height ${heightDp}dp")

                assertTrue(
                    heightDp >= MIN_TOUCH_TARGET_DP,
                    "Input field '$fieldLabel' height should be at least ${MIN_TOUCH_TARGET_DP}dp, " +
                            "was ${heightDp}dp"
                )
            }
        }
    }

    /**
     * TS-A11Y-002-04: Dropdown/Spinner controls meet 48dp minimum
     *
     * Dropdown triggers should be easy to tap
     */
    @Test
    fun dropdownControls_meet48dpMinimum() {
        // Navigate to settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Transition type dropdown
        val dropdownNodes = composeTestRule
            .onAllNodesWithText("Fade", useUnmergedTree = true)
            .fetchSemanticsNodes()

        // Find the dropdown trigger (should be clickable)
        val dropdownNode = dropdownNodes.firstOrNull { node ->
                node.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        }

        if (dropdownNode != null) {
            val bounds = dropdownNode.boundsInRoot
            val heightDp = bounds.height.value

            println("Dropdown control: height ${heightDp}dp")

            assertTrue(
                heightDp >= MIN_TOUCH_TARGET_DP,
                "Dropdown control height should be at least ${MIN_TOUCH_TARGET_DP}dp, was ${heightDp}dp"
            )
        }
    }

    /**
     * TS-A11Y-002-05: Slider controls meet 48dp minimum touch target
     *
     * Slider thumb should be easy to grab
     */
    @Test
    fun sliderControls_meet48dpMinimum() {
        // Navigate to settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Photo interval slider
        val sliderNodes = composeTestRule
            .onAllNodesWithTag("photo_interval_slider", useUnmergedTree = true)
            .fetchSemanticsNodes()

        sliderNodes.forEach { node ->
            val bounds = node.boundsInRoot
            val heightDp = bounds.height.value

            println("Slider control: height ${heightDp}dp")

            // Slider track can be thinner, but overall touch target should be 48dp
            assertTrue(
                heightDp >= MIN_TOUCH_TARGET_DP,
                "Slider touch target height should be at least ${MIN_TOUCH_TARGET_DP}dp, was ${heightDp}dp"
            )
        }
    }

    /**
     * TS-A11Y-002-06: Adequate spacing between touch targets
     *
     * Minimum 8dp spacing prevents accidental taps
     */
    @Test
    fun touchTargets_haveAdequateSpacing() {
        composeTestRule.waitForIdle()

        // Get positions of adjacent buttons on slideshow screen
        val pauseButton = composeTestRule
            .onNodeWithContentDescription("Pause slideshow", useUnmergedTree = true)
            .fetchSemanticsNode()

        val settingsButton = composeTestRule
            .onNodeWithContentDescription("Settings", useUnmergedTree = true)
            .fetchSemanticsNode()

        // Calculate spacing between buttons
        val pauseBounds = pauseButton.boundsInRoot
        val settingsBounds = settingsButton.boundsInRoot

        // Buttons should have at least 8dp spacing (or be in different areas)
        val horizontalSpacing = if (pauseBounds.right < settingsBounds.left) {
            settingsBounds.left - pauseBounds.right
        } else if (settingsBounds.right < pauseBounds.left) {
            pauseBounds.left - settingsBounds.right
        } else {
            Float.MAX_VALUE // Vertically stacked, no horizontal spacing concern
        }

        val verticalSpacing = if (pauseBounds.bottom < settingsBounds.top) {
            settingsBounds.top - pauseBounds.bottom
        } else if (settingsBounds.bottom < pauseBounds.top) {
            pauseBounds.top - settingsBounds.bottom
        } else {
            Float.MAX_VALUE // Horizontally aligned, no vertical spacing concern
        }

        val actualSpacing = minOf(horizontalSpacing, verticalSpacing)

        println("Touch target spacing: ${actualSpacing}dp")

        // Either adequate spacing or buttons are far apart (in different screen regions)
        assertTrue(
            actualSpacing >= MIN_SPACING_DP || actualSpacing == Float.MAX_VALUE,
            "Touch targets should have at least ${MIN_SPACING_DP}dp spacing, had ${actualSpacing}dp"
        )
    }

    /**
     * TS-A11Y-002-07: Time picker touch targets are adequate
     *
     * Time picker buttons should be easy to tap
     */
    @Test
    fun timePickerTouchTargets_areAdequate() {
        // Navigate to settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Enable schedule to reveal time pickers
        composeTestRule
            .onNodeWithText("Enable Scheduled Playback")
            .performClick()

        composeTestRule.waitForIdle()

        // Find time picker trigger buttons (e.g., "09:00")
        val timePickerNodes = composeTestRule
            .onAllNodesWithText("09:00", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes()

        timePickerNodes.forEach { node ->
            val bounds = node.boundsInRoot
            val heightDp = bounds.height.value

            println("Time picker button: height ${heightDp}dp")

            assertTrue(
                heightDp >= MIN_TOUCH_TARGET_DP,
                "Time picker button should be at least ${MIN_TOUCH_TARGET_DP}dp, was ${heightDp}dp"
            )
        }
    }

    /**
     * TS-A11Y-002-08: Icon-only buttons have adequate size
     *
     * Icon buttons without text need sufficient size
     */
    @Test
    fun iconOnlyButtons_haveAdequateSize() {
        composeTestRule.waitForIdle()

        // Back button in settings (icon only)
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        val backButton = composeTestRule
            .onNodeWithContentDescription("Navigate back", useUnmergedTree = true)
            .fetchSemanticsNode()

        val bounds = backButton.boundsInRoot
        val widthDp = bounds.width.value
        val heightDp = bounds.height.value

        println("Icon-only back button: ${widthDp}dp x ${heightDp}dp")

        assertTrue(
            widthDp >= MIN_TOUCH_TARGET_DP && heightDp >= MIN_TOUCH_TARGET_DP,
            "Icon-only button should be at least ${MIN_TOUCH_TARGET_DP}dp, " +
                    "was ${widthDp}dp x ${heightDp}dp"
        )
    }

    /**
     * TS-A11Y-002-09: Touch targets are tappable on tablets
     *
     * Verify touch targets work on large screens (10" tablets)
     */
    @Test
    fun touchTargets_workOnTablets() {
        // This test validates that touch targets scale appropriately
        // on larger screens without becoming too small

        composeTestRule.waitForIdle()

        // Get screen density to validate scaling
        val density = composeTestRule.density

        println("Screen density: ${density.density}")

        // Verify primary buttons
        val primaryButtons = listOf(
            "Pause slideshow",
            "Settings"
        )

        primaryButtons.forEach { buttonDescription ->
            val nodes = composeTestRule
                .onAllNodesWithContentDescription(buttonDescription, useUnmergedTree = true)
                .fetchSemanticsNodes()

            nodes.forEach { node ->
                val bounds = node.boundsInRoot
                val widthDp = bounds.width.value
                val heightDp = bounds.height.value

                // On tablets, buttons should still be at least 48dp
                // (not shrunk proportionally)
                assertTrue(
                    widthDp >= MIN_TOUCH_TARGET_DP,
                    "Button '$buttonDescription' on tablet should be at least ${MIN_TOUCH_TARGET_DP}dp wide"
                )

                assertTrue(
                    heightDp >= MIN_TOUCH_TARGET_DP,
                    "Button '$buttonDescription' on tablet should be at least ${MIN_TOUCH_TARGET_DP}dp tall"
                )
            }
        }
    }

    /**
     * TS-A11Y-002-10: Touch targets are tappable with motor impairments
     *
     * Verify adequate size for users with reduced dexterity
     */
    @Test
    fun touchTargets_supportMotorImpairments() {
        // WCAG 2.1 Level AAA recommends 44x44dp minimum
        // Android recommends 48x48dp
        // We should meet or exceed these requirements

        composeTestRule.waitForIdle()

        val criticalActions = listOf(
            "Pause slideshow" to "Critical: User needs to stop slideshow",
            "Settings" to "Critical: User needs to configure app",
            "Save" to "Critical: User needs to save settings"
        )

        criticalActions.forEach { (action, rationale) ->
            val nodes = composeTestRule
                .onAllNodesWithContentDescription(action, substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes()

            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                val bounds = node.boundsInRoot
                val widthDp = bounds.width.value
                val heightDp = bounds.height.value

                println("$action ($rationale): ${widthDp}dp x ${heightDp}dp")

                // Should meet Android's 48dp recommendation
                assertTrue(
                    widthDp >= MIN_TOUCH_TARGET_DP && heightDp >= MIN_TOUCH_TARGET_DP,
                    "$action should be at least ${MIN_TOUCH_TARGET_DP}dp for motor accessibility"
                )
            }
        }
    }
}
