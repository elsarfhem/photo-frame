package com.photoframe.app.ui.slideshow.transitions

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import kotlin.random.Random

/**
 * Zoom (Ken Burns) transition effect for photo slideshow.
 *
 * Animation: Slow zoom with pan (Ken Burns effect)
 * - Gradually zooms in or out
 * - Pans horizontally and/or vertically
 * - Creates cinematic motion
 *
 * Performance: Hardware-accelerated using graphicsLayer
 * Duration: Full display interval (e.g., 10 seconds)
 * Target: 60fps
 *
 * Ken Burns Effect:
 * - Random start scale (1.0 - 1.2)
 * - Random end scale (1.0 - 1.3)
 * - Random pan direction (-50 to +50 pixels)
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

    // Start new animation when photo changes
    LaunchedEffect(photoIndex) {
        // Reset animation
        animationTarget = 0f

        // Randomize animation parameters
        startScale = Random.nextFloat() * 0.2f + 1.0f // 1.0 - 1.2
        endScale = Random.nextFloat() * 0.3f + 1.0f // 1.0 - 1.3

        // Ensure zoom direction (always zoom in or always zoom out)
        if (startScale > endScale) {
            // Zoom out: start larger, end smaller
            startScale = Random.nextFloat() * 0.3f + 1.2f // 1.2 - 1.5
            endScale = 1.0f
        } else {
            // Zoom in: start smaller, end larger
            startScale = 1.0f
            endScale = Random.nextFloat() * 0.3f + 1.2f // 1.2 - 1.5
        }

        // Randomize pan direction (-50 to +50 pixels)
        startTranslationX = Random.nextFloat() * 100f - 50f
        endTranslationX = Random.nextFloat() * 100f - 50f
        startTranslationY = Random.nextFloat() * 100f - 50f
        endTranslationY = Random.nextFloat() * 100f - 50f

        // Start animation
        animationTarget = 1f
    }

    Box(
        modifier = modifier.fillMaxSize(),
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
                contentScale = ContentScale.Fit
            )
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
