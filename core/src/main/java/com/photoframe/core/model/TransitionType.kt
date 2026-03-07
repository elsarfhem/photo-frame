package com.photoframe.core.model

/**
 * Types of transitions between photos in the slideshow.
 */
enum class TransitionType {
    /**
     * Simple crossfade/dissolve transition.
     */
    FADE,

    /**
     * Horizontal slide transition (left to right).
     */
    SLIDE,

    /**
     * Zoom and pan transition (Ken Burns effect).
     */
    ZOOM_KEN_BURNS;

    companion object {
        /**
         * Default transition type for the slideshow.
         */
        val DEFAULT = FADE
    }
}
