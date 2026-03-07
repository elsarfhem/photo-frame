package com.photoframe.app.ui.slideshow

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.photoframe.core.model.Photo

/**
 * UI state for the slideshow screen.
 *
 * Thread Safety: Immutable data class, safe to share across threads.
 *
 * @property currentPhoto Current photo bitmap being displayed
 * @property currentPhotoMetadata Metadata for the current photo
 * @property photoIndex Current photo index (0-based)
 * @property totalPhotos Total number of photos in slideshow
 * @property isPlaying True if slideshow is auto-advancing
 * @property isLoading True if photos are being loaded
 * @property error Error message if loading failed, null otherwise
 * @property bufferedPhotosCount Number of photos currently in buffer (for debugging)
 */
@Immutable
data class SlideshowState(
    val currentPhoto: Bitmap? = null,
    val currentPhotoMetadata: Photo? = null,
    val photoIndex: Int = 0,
    val totalPhotos: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bufferedPhotosCount: Int = 0
) {
    /**
     * Returns true if slideshow is ready to display photos.
     */
    val isReady: Boolean
        get() = currentPhoto != null && !isLoading && error == null

    /**
     * Returns true if slideshow has photos but none are displayed.
     */
    val isEmpty: Boolean
        get() = totalPhotos == 0 && !isLoading && error == null

    /**
     * Returns true if slideshow encountered an error.
     */
    val hasError: Boolean
        get() = error != null

    companion object {
        /**
         * Initial empty state.
         */
        val EMPTY = SlideshowState()
    }
}
