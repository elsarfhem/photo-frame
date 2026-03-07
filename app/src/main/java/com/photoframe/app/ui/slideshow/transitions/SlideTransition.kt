package com.photoframe.app.ui.slideshow.transitions

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale

/**
 * Slide transition effect for photo slideshow.
 *
 * Animation: Horizontal slide between photos
 * - Previous photo slides out to the left
 * - Next photo slides in from the right
 *
 * Performance: Hardware-accelerated using graphicsLayer
 * Duration: 400ms (configurable)
 * Target: 60fps
 *
 * @param bitmap Current photo bitmap
 * @param photoIndex Current photo index (used as key for AnimatedContent)
 * @param contentDescription Accessibility description
 * @param durationMillis Slide duration in milliseconds (default 400ms)
 * @param modifier Modifier for the container
 */
@Composable
fun SlideTransition(
    bitmap: Bitmap?,
    photoIndex: Int,
    contentDescription: String,
    durationMillis: Int = DEFAULT_SLIDE_DURATION_MS,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = photoIndex,
            transitionSpec = {
                // Slide in from right
                slideInHorizontally(
                    animationSpec = tween(durationMillis = durationMillis),
                    initialOffsetX = { fullWidth -> fullWidth }
                ) togetherWith
                // Slide out to left
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = durationMillis),
                    targetOffsetX = { fullWidth -> -fullWidth }
                )
            },
            label = "slide_transition"
        ) { _ ->
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Hardware acceleration hint
                            translationX = 0f
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Default slide duration: 400ms.
 */
private const val DEFAULT_SLIDE_DURATION_MS = 400
