package com.photoframe.app.ui.slideshow.transitions

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random

/**
 * Zoom (Ken Burns) transition effect for photo slideshow.
 *
 * Animation: Slow zoom with pan (Ken Burns effect)
 * - Fills screen completely using ContentScale.Crop (no black bands on any aspect ratio)
 * - Gradually zooms in or out on top of the cropped image
 * - Pans horizontally and/or vertically
 * - Creates cinematic motion
 *
 * Performance: Hardware-accelerated using graphicsLayer
 * Duration: Full display interval (e.g., 10 seconds)
 * Target: 60fps
 *
 * Ken Burns Effect:
 * - Random zoom direction (in or out)
 * - Start/end scale range: 1.0 - 1.3
 * - Random pan direction scaled to screen dimensions
 * - Smooth linear interpolation
 *
 * @param bitmap Current photo bitmap
 * @param photoIndex Current photo index (used to trigger new animation)
 * @param contentDescription Accessibility description
 * @param durationMillis Zoom duration in milliseconds (default 10 seconds)
 * @param modifier Modifier for the container
 */
@Composable
fun ZoomTransition(
    bitmap: Bitmap?,
    photoIndex: Int,
    contentDescription: String,
    durationMillis: Int = DEFAULT_ZOOM_DURATION_MS,
    modifier: Modifier = Modifier
) {
    // Animation parameters (randomized per photo)
    var startScale by remember { mutableStateOf(1f) }
    var endScale by remember { mutableStateOf(1.1f) }
    var startTranslationX by remember { mutableStateOf(0f) }
    var endTranslationX by remember { mutableStateOf(0f) }
    var startTranslationY by remember { mutableStateOf(0f) }
    var endTranslationY by remember { mutableStateOf(0f) }

    // Animation progress (0.0 to 1.0)
    var animationTarget by remember { mutableStateOf(0f) }

    // Animated values
    val animatedProgress by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = LinearEasing
        ),
        label = "zoom_progress"
    )

    // Calculate current scale and translation
    val currentScale = lerp(startScale, endScale, animatedProgress)
    val currentTranslationX = lerp(startTranslationX, endTranslationX, animatedProgress)
    val currentTranslationY = lerp(startTranslationY, endTranslationY, animatedProgress)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

        // Scale pan range proportional to screen size for natural-looking motion
        val panRangeX = screenWidthPx * 0.03f  // 3% of screen width
        val panRangeY = screenHeightPx * 0.03f  // 3% of screen height

        // Start new animation when photo changes
        LaunchedEffect(photoIndex) {
            // Reset animation
            animationTarget = 0f

            // Randomize zoom direction
            if (Random.nextBoolean()) {
                // Zoom in
                startScale = 1.0f
                endScale = Random.nextFloat() * 0.15f + 1.15f // 1.15 - 1.3
            } else {
                // Zoom out
                startScale = Random.nextFloat() * 0.15f + 1.15f // 1.15 - 1.3
                endScale = 1.0f
            }

            // Randomize pan direction (proportional to screen size)
            startTranslationX = Random.nextFloat() * panRangeX * 2 - panRangeX
            endTranslationX = Random.nextFloat() * panRangeX * 2 - panRangeX
            startTranslationY = Random.nextFloat() * panRangeY * 2 - panRangeY
            endTranslationY = Random.nextFloat() * panRangeY * 2 - panRangeY

            // Start animation
            animationTarget = 1f
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Hardware-accelerated zoom and pan
                            scaleX = currentScale
                            scaleY = currentScale
                            translationX = currentTranslationX
                            translationY = currentTranslationY
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * Linear interpolation between two floats.
 *
 * @param start Start value
 * @param end End value
 * @param fraction Progress (0.0 to 1.0)
 * @return Interpolated value
 */
private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

/**
 * Default zoom duration: 10 seconds.
 * Should match display interval for smooth continuous motion.
 */
private const val DEFAULT_ZOOM_DURATION_MS = 10_000
