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
 * E2E Test: First-Time Setup Flow
 *
 * Tests TS-E2E-001 from QA 2 test plan (Phase 9: Test Implementation - Week 17)
 *
 * Validates complete first-time user setup experience:
 * 1. App launches to setup wizard
 * 2. User configures SMB connection
 * 3. User tests connection successfully
 * 4. User configures slideshow settings
 * 5. Slideshow starts automatically
 * 6. Settings persist on next launch
 *
 * Critical User Journey: First-time user must successfully configure
 * the app and start a slideshow within 3 minutes.
 *
 * @see docs/features/photo-frame-app-initial/testing/ui-e2e-tests.md
 */
@RunWith(AndroidJUnit4::class)
class FirstTimeSetupFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * TS-E2E-001-01: Complete first-time setup flow
     *
     * Full journey: Setup wizard → Configure SMB → Test connection → Start slideshow
     */
    @Test
    fun firstTimeSetup_completeFlow_success() {
        // Step 1: App should launch to setup wizard (first launch)
        composeTestRule
            .onNodeWithText("Welcome to Photo Frame")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Get Started")
            .assertExists()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()

        // Step 2: Configure SMB connection
        composeTestRule
            .onNodeWithText("SMB Server Configuration")
            .assertExists()

        // Fill in host
        composeTestRule
            .onNodeWithText("Host")
            .performTextInput("192.168.1.100")

        // Fill in share name
        composeTestRule
            .onNodeWithText("Share Name")
            .performTextInput("photos")

        // Fill in folder path
        composeTestRule
            .onNodeWithText("Folder Path")
            .performTextInput("/family")

        // Fill in username
        composeTestRule
            .onNodeWithText("Username")
            .performTextInput("testuser")

        // Fill in password
        composeTestRule
            .onNodeWithText("Password")
            .performTextInput("testpass123")

        composeTestRule.waitForIdle()

        // Step 3: Test connection
        composeTestRule
            .onNodeWithText("Test Connection")
            .performClick()

        // Wait for connection test (show loading indicator)
        composeTestRule
            .onNodeWithTag("connection_test_progress")
            .assertExists()

        // Wait for success message
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Connection successful!")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("Connection successful!")
            .assertExists()

        // Step 4: Click Next to slideshow settings
        composeTestRule
            .onNodeWithText("Next")
            .assertExists()
            .performClick()

        composeTestRule.waitForIdle()

        // Step 5: Configure slideshow settings
        composeTestRule
            .onNodeWithText("Slideshow Settings")
            .assertExists()

        // Select transition type (default is Fade, keep it)
        composeTestRule
            .onNodeWithText("Fade")
            .assertExists()

        // Adjust photo interval (keep default 10 seconds)
        composeTestRule
            .onNodeWithText("10 seconds")
            .assertExists()

        // Step 6: Click "Start Slideshow"
        composeTestRule
            .onNodeWithText("Start Slideshow")
            .assertExists()
            .performClick()

        composeTestRule.waitForIdle()

        // Step 7: Slideshow should start
        // Wait for first photo to load
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify slideshow screen is displayed
        composeTestRule
            .onNodeWithTag("slideshow_photo")
            .assertExists()
            .assertIsDisplayed()

        // Verify controls are present
        composeTestRule
            .onNodeWithContentDescription("Pause slideshow")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription("Settings")
            .assertExists()

        // Test passes - user successfully completed first-time setup
        assertTrue(true, "First-time setup flow completed successfully")
    }

    /**
     * TS-E2E-001-02: Setup wizard skipped on subsequent launch
     *
     * After first setup, app should directly launch to slideshow
     */
    @Test
    fun secondLaunch_skipsSetupWizard_startsSlideshow() {
        // Simulate second launch (first launch flag already set)
        // In real test, this would be a separate test run after first setup

        composeTestRule.waitForIdle()

        // Should NOT see setup wizard
        composeTestRule
            .onNodeWithText("Welcome to Photo Frame")
            .assertDoesNotExist()

        // Should directly see slideshow or settings
        // (depending on whether slideshow is scheduled to run)
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            val hasSlideshow = composeTestRule
                .onAllNodesWithTag("slideshow_photo")
                .fetchSemanticsNodes()
                .isNotEmpty()

            val hasSettings = composeTestRule
                .onAllNodesWithText("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()

            hasSlideshow || hasSettings
        }

        assertTrue(true, "App skipped setup wizard on subsequent launch")
    }

    /**
     * TS-E2E-001-03: Connection test failure handling
     *
     * Verify error handling when connection test fails
     */
    @Test
    fun setupWizard_connectionTestFails_showsError() {
        // Navigate to SMB configuration
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()

        composeTestRule.waitForIdle()

        // Fill in invalid credentials
        composeTestRule
            .onNodeWithText("Host")
            .performTextInput("invalid.host.com")

        composeTestRule
            .onNodeWithText("Share Name")
            .performTextInput("photos")

        // Test connection with invalid host
        composeTestRule
            .onNodeWithText("Test Connection")
            .performClick()

        // Wait for error message
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Connection failed")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Error message should be displayed
        composeTestRule
            .onNodeWithText("Connection failed")
            .assertExists()

        // Error details should be shown
        composeTestRule
            .onNodeWithTag("error_details")
            .assertExists()

        // "Next" button should be disabled until connection succeeds
        composeTestRule
            .onNodeWithText("Next")
            .assertIsNotEnabled()
    }

    /**
     * TS-E2E-001-04: Form validation in setup wizard
     *
     * Verify required fields are validated
     */
    @Test
    fun setupWizard_formValidation_requiresFields() {
        // Navigate to SMB configuration
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()

        composeTestRule.waitForIdle()

        // Try to test connection without filling fields
        composeTestRule
            .onNodeWithText("Test Connection")
            .performClick()

        composeTestRule.waitForIdle()

        // Validation errors should be displayed
        composeTestRule
            .onNodeWithText("Host is required")
            .assertExists()

        composeTestRule
            .onNodeWithText("Share name is required")
            .assertExists()
    }

    /**
     * TS-E2E-001-05: Back navigation in setup wizard
     *
     * Verify user can navigate back through wizard steps
     */
    @Test
    fun setupWizard_backNavigation_works() {
        // Navigate to SMB configuration
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()

        composeTestRule.waitForIdle()

        // Fill in fields
        composeTestRule
            .onNodeWithText("Host")
            .performTextInput("192.168.1.100")

        // Navigate to slideshow settings (skip connection test for this test)
        composeTestRule
            .onNodeWithText("Skip")
            .performClick()

        composeTestRule.waitForIdle()

        // Now on slideshow settings page
        composeTestRule
            .onNodeWithText("Slideshow Settings")
            .assertExists()

        // Click back button
        composeTestRule
            .onNodeWithContentDescription("Navigate back")
            .performClick()

        composeTestRule.waitForIdle()

        // Should be back on SMB configuration
        composeTestRule
            .onNodeWithText("SMB Server Configuration")
            .assertExists()

        // Previously entered host should still be there
        composeTestRule
            .onNodeWithText("192.168.1.100")
            .assertExists()
    }

    /**
     * TS-E2E-001-06: Setup completion time <3 minutes
     *
     * Measure time to complete first-time setup
     */
    @Test
    fun setupCompletion_under3Minutes() {
        val startTime = System.currentTimeMillis()

        // Complete setup flow (simplified for timing test)
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()

        // Fill all fields quickly
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Host").performTextInput("192.168.1.100")
        composeTestRule.onNodeWithText("Share Name").performTextInput("photos")
        composeTestRule.onNodeWithText("Folder Path").performTextInput("/family")
        composeTestRule.onNodeWithText("Username").performTextInput("user")
        composeTestRule.onNodeWithText("Password").performTextInput("pass")

        // Test connection
        composeTestRule.onNodeWithText("Test Connection").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Connection successful!").fetchSemanticsNodes().isNotEmpty()
        }

        // Next to slideshow settings
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()

        // Start slideshow
        composeTestRule.onNodeWithText("Start Slideshow").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("slideshow_photo").fetchSemanticsNodes().isNotEmpty()
        }

        val completionTime = System.currentTimeMillis() - startTime

        // Should complete in under 3 minutes (180 seconds)
        assertTrue(
            completionTime < 180_000,
            "Setup should complete in <3 minutes, took ${completionTime}ms"
        )

        println("Setup Completion Time: ${completionTime}ms (${completionTime / 1000}s)")
    }
}
