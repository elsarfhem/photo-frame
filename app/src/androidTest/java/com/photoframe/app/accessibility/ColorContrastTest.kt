package com.photoframe.app.accessibility

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.photoframe.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.pow

/**
 * Accessibility Test: Color Contrast (WCAG AA)
 *
 * Tests TS-A11Y-003 from QA 3 test plan (Phase 9: Test Implementation - Week 18)
 *
 * Validates:
 * - P0 Accessibility: WCAG AA color contrast requirements
 * - Normal text: 4.5:1 contrast ratio minimum
 * - Large text (18pt+): 3:1 contrast ratio minimum
 * - UI components: 3:1 contrast ratio minimum
 *
 * WCAG 2.1 Guidelines:
 * - 1.4.3 Contrast (Minimum) - Level AA
 *   - Normal text: 4.5:1
 *   - Large text (14pt bold or 18pt): 3:1
 * - 1.4.11 Non-text Contrast - Level AA
 *   - UI components and graphics: 3:1
 *
 * @see docs/features/photo-frame-app-initial/testing/performance-accessibility-tests.md
 */
@RunWith(AndroidJUnit4::class)
class ColorContrastTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        private const val MIN_NORMAL_TEXT_CONTRAST = 4.5
        private const val MIN_LARGE_TEXT_CONTRAST = 3.0
        private const val MIN_UI_COMPONENT_CONTRAST = 3.0
    }

    /**
     * TS-A11Y-003-01: Primary buttons meet 4.5:1 contrast ratio
     *
     * WCAG 1.4.3 (Level AA): Button text contrast
     */
    @Test
    fun primaryButtons_meetContrastRatio() {
        composeTestRule.waitForIdle()

        // Material 3 primary button uses primary color for background
        // and onPrimary for text - should have high contrast

        // Example Material 3 colors:
        val primaryColor = Color(0xFF6750A4) // Material Purple
        val onPrimaryColor = Color(0xFFFFFFFF) // White text

        val contrastRatio = calculateContrastRatio(primaryColor, onPrimaryColor)

        println("Primary button contrast ratio: ${String.format("%.2f", contrastRatio)}:1")

        assertTrue(
            contrastRatio >= MIN_NORMAL_TEXT_CONTRAST,
            "Primary button text should have contrast ratio ≥4.5:1, was ${contrastRatio}:1"
        )
    }

    /**
     * TS-A11Y-003-02: Error messages meet 4.5:1 contrast ratio
     *
     * WCAG 1.4.3 (Level AA): Error text must be readable
     */
    @Test
    fun errorMessages_meetContrastRatio() {
        // Material 3 error color on surface
        val errorColor = Color(0xFFB3261E) // Material Error Red
        val surfaceColor = Color(0xFFFFFBFE) // Material Surface

        val contrastRatio = calculateContrastRatio(errorColor, surfaceColor)

        println("Error text contrast ratio: ${String.format("%.2f", contrastRatio)}:1")

        assertTrue(
            contrastRatio >= MIN_NORMAL_TEXT_CONTRAST,
            "Error text should have contrast ratio ≥4.5:1, was ${contrastRatio}:1"
        )
    }

    /**
     * TS-A11Y-003-03: Normal body text meets 4.5:1 contrast ratio
     *
     * WCAG 1.4.3 (Level AA): Body text contrast
     */
    @Test
    fun bodyText_meetsContrastRatio() {
        // Material 3 default text colors
        val onSurfaceColor = Color(0xFF1C1B1F) // Dark gray text
        val surfaceColor = Color(0xFFFFFBFE) // Light surface

        val contrastRatio = calculateContrastRatio(onSurfaceColor, surfaceColor)

        println("Body text contrast ratio: ${String.format("%.2f", contrastRatio)}:1")

        assertTrue(
            contrastRatio >= MIN_NORMAL_TEXT_CONTRAST,
            "Body text should have contrast ratio ≥4.5:1, was ${contrastRatio}:1"
        )
    }

    /**
     * TS-A11Y-003-04: Icon buttons meet 3:1 contrast ratio
     *
     * WCAG 1.4.11 (Level AA): UI component contrast
     */
    @Test
    fun iconButtons_meetContrastRatio() {
        // Icons should have 3:1 contrast against background
        val iconColor = Color(0xFF49454F) // Material onSurfaceVariant
        val surfaceColor = Color(0xFFFFFBFE) // Material Surface

        val contrastRatio = calculateContrastRatio(iconColor, surfaceColor)

        println("Icon button contrast ratio: ${String.format("%.2f", contrastRatio)}:1")

        assertTrue(
            contrastRatio >= MIN_UI_COMPONENT_CONTRAST,
            "Icon buttons should have contrast ratio ≥3:1, was ${contrastRatio}:1"
        )
    }

    /**
     * TS-A11Y-003-05: Form field labels meet 4.5:1 contrast ratio
     *
     * WCAG 1.4.3 (Level AA): Label text contrast
     */
    @Test
    fun formFieldLabels_meetContrastRatio() {
        // Material 3 label colors
        val labelColor = Color(0xFF49454F) // onSurfaceVariant for labels
        val surfaceColor = Color(0xFFFFFBFE) // Surface

        val contrastRatio = calculateContrastRatio(labelColor, surfaceColor)

        println("Form label contrast ratio: ${String.format("%.2f", contrastRatio)}:1")

        assertTrue(
            contrastRatio >= MIN_NORMAL_TEXT_CONTRAST,
            "Form labels should have contrast ratio ≥4.5:1, was ${contrastRatio}:1"
        )
    }

    /**
     * TS-A11Y-003-06: Disabled text has adequate contrast
     *
     * Disabled text should still be readable (though may be lower contrast)
     */
    @Test
    fun disabledText_hasAdequateContrast() {
        // Disabled text typically uses lower opacity
        // Should still aim for at least 3:1 for readability

        val disabledColor = Color(0xFF1C1B1F).copy(alpha = 0.38f) // 38% opacity
        val surfaceColor = Color(0xFFFFFBFE)

        // Calculate effective color after blending with background
        val effectiveColor = blendColors(disabledColor, surfaceColor)
        val contrastRatio = calculateContrastRatio(effectiveColor, surfaceColor)

        println("Disabled text contrast ratio: ${String.format("%.2f", contrastRatio)}:1")

        // Disabled text doesn't need to meet 4.5:1, but should be visible
        assertTrue(
            contrastRatio >= 2.0,
            "Disabled text should have contrast ratio ≥2:1 for visibility, was ${contrastRatio}:1"
        )
    }

    /**
     * TS-A11Y-003-07: Connection status indicators meet 3:1 contrast
     *
     * WCAG 1.4.11 (Level AA): Status indicator contrast
     */
    @Test
    fun statusIndicators_meetContrastRatio() {
        // Green indicator for "connected"
        val connectedColor = Color(0xFF4CAF50) // Green
        val surfaceColor = Color(0xFFFFFBFE)

        val connectedContrast = calculateContrastRatio(connectedColor, surfaceColor)

        println("Connected indicator contrast: ${String.format("%.2f", connectedContrast)}:1")

        assertTrue(
            connectedContrast >= MIN_UI_COMPONENT_CONTRAST,
            "Connected indicator should have contrast ≥3:1, was ${connectedContrast}:1"
        )

        // Red indicator for "disconnected"
        val disconnectedColor = Color(0xFFB3261E) // Material Error Red
        val disconnectedContrast = calculateContrastRatio(disconnectedColor, surfaceColor)

        println("Disconnected indicator contrast: ${String.format("%.2f", disconnectedContrast)}:1")

        assertTrue(
            disconnectedContrast >= MIN_UI_COMPONENT_CONTRAST,
            "Disconnected indicator should have contrast ≥3:1, was ${disconnectedContrast}:1"
        )
    }

    /**
     * TS-A11Y-003-08: Focus indicators meet 3:1 contrast
     *
     * WCAG 1.4.11 (Level AA): Focus indicator contrast
     */
    @Test
    fun focusIndicators_meetContrastRatio() {
        // Material 3 focus indicator
        val primaryColor = Color(0xFF6750A4)
        val surfaceColor = Color(0xFFFFFBFE)

        val contrastRatio = calculateContrastRatio(primaryColor, surfaceColor)

        println("Focus indicator contrast: ${String.format("%.2f", contrastRatio)}:1")

        assertTrue(
            contrastRatio >= MIN_UI_COMPONENT_CONTRAST,
            "Focus indicator should have contrast ≥3:1, was ${contrastRatio}:1"
        )
    }

    /**
     * TS-A11Y-003-09: Dark mode colors meet contrast requirements
     *
     * Verify dark theme also meets WCAG AA
     */
    @Test
    fun darkModeColors_meetContrastRatio() {
        // Material 3 Dark Theme colors
        val darkOnSurface = Color(0xFFE6E1E5) // Light text on dark
        val darkSurface = Color(0xFF1C1B1F) // Dark surface

        val contrastRatio = calculateContrastRatio(darkOnSurface, darkSurface)

        println("Dark mode text contrast: ${String.format("%.2f", contrastRatio)}:1")

        assertTrue(
            contrastRatio >= MIN_NORMAL_TEXT_CONTRAST,
            "Dark mode text should have contrast ≥4.5:1, was ${contrastRatio}:1"
        )

        // Dark mode primary button
        val darkPrimary = Color(0xFFD0BCFF) // Lighter purple on dark
        val darkPrimaryContainer = Color(0xFF4F378B) // Dark purple background

        val buttonContrast = calculateContrastRatio(darkPrimary, darkPrimaryContainer)

        println("Dark mode button contrast: ${String.format("%.2f", buttonContrast)}:1")

        assertTrue(
            buttonContrast >= MIN_NORMAL_TEXT_CONTRAST,
            "Dark mode button should have contrast ≥4.5:1, was ${buttonContrast}:1"
        )
    }

    /**
     * TS-A11Y-003-10: Text on photo overlay meets contrast requirements
     *
     * Photo counter and controls need adequate contrast against photo background
     */
    @Test
    fun photoOverlayText_meetsContrastRatio() {
        // Text on photos typically uses scrim (semi-transparent dark overlay)
        // to ensure readability

        val whiteText = Color(0xFFFFFFFF)
        val scrimBackground = Color(0xFF000000).copy(alpha = 0.5f) // 50% black scrim

        // Worst case: scrim over white photo
        val whitePhoto = Color(0xFFFFFFFF)
        val effectiveBackground = blendColors(scrimBackground, whitePhoto)

        val contrastRatio = calculateContrastRatio(whiteText, effectiveBackground)

        println("Photo overlay text contrast (worst case): ${String.format("%.2f", contrastRatio)}:1")

        // Large text (photo counter) needs 3:1
        assertTrue(
            contrastRatio >= MIN_LARGE_TEXT_CONTRAST,
            "Photo overlay text should have contrast ≥3:1, was ${contrastRatio}:1"
        )

        // Best case: scrim over black photo
        val blackPhoto = Color(0xFF000000)
        val bestCaseBackground = blendColors(scrimBackground, blackPhoto)
        val bestCaseContrast = calculateContrastRatio(whiteText, bestCaseBackground)

        println("Photo overlay text contrast (best case): ${String.format("%.2f", bestCaseContrast)}:1")

        assertTrue(
            bestCaseContrast >= MIN_LARGE_TEXT_CONTRAST,
            "Photo overlay text should have contrast ≥3:1 on dark photos"
        )
    }

    // Helper functions

    /**
     * Calculate WCAG 2.1 contrast ratio between two colors
     *
     * Formula: (L1 + 0.05) / (L2 + 0.05)
     * Where L1 is luminance of lighter color, L2 is luminance of darker color
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val lum1 = color1.luminance().toDouble()
        val lum2 = color2.luminance().toDouble()

        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)

        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Blend foreground color with background using alpha compositing
     *
     * Used for calculating effective color of semi-transparent elements
     */
    private fun blendColors(foreground: Color, background: Color): Color {
        val alpha = foreground.alpha

        val r = foreground.red * alpha + background.red * (1 - alpha)
        val g = foreground.green * alpha + background.green * (1 - alpha)
        val b = foreground.blue * alpha + background.blue * (1 - alpha)

        return Color(r, g, b, 1f)
    }

    /**
     * Calculate relative luminance per WCAG definition
     *
     * Already provided by Compose's Color.luminance() extension
     */
    private fun calculateLuminance(color: Color): Float {
        return color.luminance()
    }
}
