package com.photoframe.app.ui.slideshow.transitions

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity

/**
 * Pan transition effect for photo slideshow.
 *
 * Uses BiasAlignment to animate the crop window within ContentScale.Crop.
 * This approach is structurally impossible to produce black borders because
 * BiasAlignment operates within the crop coordinate system.
 *
 * Animation: Slow pan to show full photo without black bands
 * - Fills screen completely using ContentScale.Crop
 * - Intelligently pans based on aspect ratio:
 *   - Horizontal pan for landscape photos (wider than screen)
 *   - Vertical pan for portrait photos (taller than screen)
 *   - No pan for near-square photos
 * - Smooth one-way pan by animating alignment bias from -1 (start edge) to +1 (end edge)
 * - Duration matches display interval
 *
 * Architecture: Animates Image alignment parameter instead of graphicsLayer translation.
 * This keeps the crop window within valid bounds - no pixel offset calculations needed.
 *
 * @param bitmap Current photo bitmap
 * @param photoIndex Current photo index (used to trigger new animation)
 * @param contentDescription Accessibility description
 * @param durationMillis Pan duration in milliseconds (default 10 seconds)
 * @param modifier Modifier for the container
 */
@Composable
fun PanTransition(
    bitmap: Bitmap?,
    photoIndex: Int,
    contentDescription: String,
    durationMillis: Int = DEFAULT_PAN_DURATION_MS,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

        // Calculate pan direction based on aspect ratio comparison
        val panDirection = remember(bitmap, screenWidthPx, screenHeightPx) {
            bitmap?.let {
                calculatePanDirection(
                    it.width, it.height,
                    screenWidthPx.toInt(), screenHeightPx.toInt()
                )
            } ?: PanDirection.NONE
        }

        // Animatable for smooth bias animation with proper reset
        val alignmentBias = remember { Animatable(START_BIAS) }

        // Calculate current alignment based on pan direction and animated bias
        val alignment = when (panDirection) {
            PanDirection.HORIZONTAL -> BiasAlignment(
                horizontalBias = alignmentBias.value,
                verticalBias = 0f
            )
            PanDirection.VERTICAL -> BiasAlignment(
                horizontalBias = 0f,
                verticalBias = alignmentBias.value
            )
            PanDirection.NONE -> Alignment.Center
        }

        // Animate alignment bias when photo changes
        LaunchedEffect(photoIndex, panDirection) {
            if (panDirection != PanDirection.NONE) {
                alignmentBias.snapTo(START_BIAS)  // Instant reset to start
                alignmentBias.animateTo(
                    targetValue = END_BIAS,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing
                    )
                )
            } else {
                // No panning - keep centered
                alignmentBias.snapTo(0f)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = alignment  // Animated alignment shifts crop window
                )
            }
        }
    }
}

/**
 * Direction of pan animation.
 */
enum class PanDirection {
    HORIZONTAL,  // Pan left-to-right for landscape photos
    VERTICAL,    // Pan top-to-bottom for portrait photos
    NONE         // No panning for photos that fit viewport
}

/**
 * Calculates pan direction based on image and screen aspect ratios.
 *
 * Algorithm:
 * 1. Calculate scale factor for ContentScale.Crop
 * 2. Determine which dimension will overflow (be cropped)
 * 3. Return pan direction for the overflowing dimension
 *
 * @param bitmapWidth Image width in pixels
 * @param bitmapHeight Image height in pixels
 * @param screenWidth Screen width in pixels
 * @param screenHeight Screen height in pixels
 * @return Pan direction (HORIZONTAL, VERTICAL, or NONE)
 */
fun calculatePanDirection(
    bitmapWidth: Int,
    bitmapHeight: Int,
    screenWidth: Int,
    screenHeight: Int
): PanDirection {
    if (bitmapWidth <= 0 || bitmapHeight <= 0) {
        return PanDirection.NONE
    }

    // Calculate scale factor for ContentScale.Crop (fills container, crops excess)
    val scaleX = screenWidth.toFloat() / bitmapWidth
    val scaleY = screenHeight.toFloat() / bitmapHeight
    val scale = maxOf(scaleX, scaleY)  // Larger scale fills both dimensions

    // Calculate scaled image dimensions
    val scaledWidth = bitmapWidth * scale
    val scaledHeight = bitmapHeight * scale

    // Calculate overflow (how much is cropped on each side)
    val overflowX = scaledWidth - screenWidth
    val overflowY = scaledHeight - screenHeight

    // Determine pan direction based on which dimension has more overflow
    return when {
        overflowX > CROP_THRESHOLD && overflowX > overflowY -> {
            // Image is wider than screen - pan horizontally
            PanDirection.HORIZONTAL
        }
        overflowY > CROP_THRESHOLD -> {
            // Image is taller than screen - pan vertically
            PanDirection.VERTICAL
        }
        else -> {
            // Image fits perfectly or nearly perfectly - no panning needed
            PanDirection.NONE
        }
    }
}

/**
 * Default pan duration: 10 seconds.
 * Should match display interval for smooth continuous motion.
 */
private const val DEFAULT_PAN_DURATION_MS = 10_000

/**
 * Crop threshold in pixels.
 * If overflow is below this threshold, image fits viewport and no panning is applied.
 */
private const val CROP_THRESHOLD = 20f

/**
 * Start bias for alignment animation.
 * -0.95 shows the start edge (left/top) with 5% safety margin.
 */
private const val START_BIAS = -0.95f

/**
 * End bias for alignment animation.
 * +0.95 shows the end edge (right/bottom) with 5% safety margin.
 */
private const val END_BIAS = 0.95f
