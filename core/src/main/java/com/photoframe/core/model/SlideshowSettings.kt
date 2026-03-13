package com.photoframe.core.model

import androidx.compose.runtime.Immutable

/**
 * Settings for the slideshow display and behavior.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * @property displayIntervalSeconds Number of seconds to display each photo (3-300 seconds)
 * @property transitionType Type of transition effect between photos
 * @property shuffleEnabled If true, photos are displayed in random order
 * @property panAnimationEnabled If true, photos are displayed with pan animation
 */
@Immutable
data class SlideshowSettings(
    val displayIntervalSeconds: Int = DEFAULT_DISPLAY_INTERVAL_SECONDS,
    val transitionType: TransitionType = TransitionType.DEFAULT,
    val shuffleEnabled: Boolean = false,
    val panAnimationEnabled: Boolean = true
) {
    init {
        require(displayIntervalSeconds in MIN_DISPLAY_INTERVAL..MAX_DISPLAY_INTERVAL) {
            "Display interval must be between $MIN_DISPLAY_INTERVAL and $MAX_DISPLAY_INTERVAL seconds"
        }
    }

    /**
     * Returns the display interval in milliseconds.
     */
    val displayIntervalMillis: Long
        get() = displayIntervalSeconds * 1000L

    companion object {
        const val MIN_DISPLAY_INTERVAL = 3
        const val MAX_DISPLAY_INTERVAL = 300  // 5 minutes
        const val DEFAULT_DISPLAY_INTERVAL_SECONDS = 10

        /**
         * Default slideshow settings.
         */
        val DEFAULT = SlideshowSettings(
            displayIntervalSeconds = DEFAULT_DISPLAY_INTERVAL_SECONDS,
            transitionType = TransitionType.DEFAULT,
            shuffleEnabled = false,
            panAnimationEnabled = true
        )
    }
}
