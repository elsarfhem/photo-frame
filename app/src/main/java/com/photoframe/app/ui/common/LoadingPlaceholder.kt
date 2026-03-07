package com.photoframe.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer loading placeholder with animated gradient.
 *
 * Provides a polished loading experience with smooth animation.
 * Used for skeleton screens while data is loading.
 *
 * Phase 6: Polish & Bug Fixes
 *
 * @param modifier Modifier for the placeholder
 * @param shape Shape of the placeholder (default: rounded rectangle)
 */
@Composable
fun ShimmerLoadingPlaceholder(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 1000f, translateAnim - 1000f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Form field skeleton loading placeholder.
 */
@Composable
fun FormFieldPlaceholder(modifier: Modifier = Modifier) {
    ShimmerLoadingPlaceholder(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(4.dp)
    )
}

/**
 * Button skeleton loading placeholder.
 */
@Composable
fun ButtonPlaceholder(modifier: Modifier = Modifier) {
    ShimmerLoadingPlaceholder(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Text skeleton loading placeholder.
 */
@Composable
fun TextPlaceholder(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 120.dp
) {
    ShimmerLoadingPlaceholder(
        modifier = modifier
            .width(width)
            .height(20.dp),
        shape = RoundedCornerShape(4.dp)
    )
}
