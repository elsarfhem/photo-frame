package com.photoframe.app.ui.slideshow

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs

/**
 * Slideshow screen composable.
 *
 * Features:
 * - Full-screen photo display
 * - Scale-to-fit with letterbox/pillarbox
 * - Loading indicator
 * - Error UI with retry button
 * - Immersive mode (hides system UI)
 * - Settings button for accessing configuration
 *
 * Architecture: Observes SlideshowViewModel state via StateFlow.
 *
 * @param viewModel Slideshow view model (injected via Hilt)
 * @param shuffleEnabled If true, shuffles photos on initialization
 * @param autoPlay If true, starts auto-advance on initialization
 * @param onNavigateToSettings Callback to navigate to settings screen
 */
@Composable
fun SlideshowScreen(
    viewModel: SlideshowViewModel = hiltViewModel(),
    shuffleEnabled: Boolean = false,
    autoPlay: Boolean = true,
    onNavigateToSettings: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Keep screen on during slideshow
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Hide system UI (immersive mode)
    HideSystemUI()

    // Initialize slideshow on first composition
    // autoPlay is passed to initialize() to avoid race condition
    LaunchedEffect(Unit) {
        viewModel.initialize(shuffleEnabled, autoPlay)
    }

    // Main container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            // Loading state
            state.isLoading -> {
                LoadingContent()
            }
            // Error state
            state.hasError -> {
                ErrorContent(
                    error = state.error ?: "Unknown error",
                    onRetry = { viewModel.retry() }
                )
            }
            // Empty state
            state.isEmpty -> {
                EmptyContent()
            }
            // Photo/Video display
            state.isReady -> {
                MediaContent(
                    bitmap = state.currentPhoto,
                    photoMetadata = state.currentPhotoMetadata,
                    photoIndex = state.photoIndex,
                    totalPhotos = state.totalPhotos,
                    transitionType = state.transitionType,
                    viewModel = viewModel,
                    onNavigateToSettings = onNavigateToSettings
                )

                // Connection status indicator (top-right corner)
                ConnectionStatusIndicator(
                    isConnected = !state.hasError,
                    isConnecting = state.isLoading,
                    error = state.error,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

/**
 * Hides system UI bars for immersive full-screen experience.
 */
@Composable
private fun HideSystemUI() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.apply {
                // Hide system bars
                hide(WindowInsetsCompat.Type.systemBars())
                // Keep system bars hidden
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose {
            // Restore system bars when leaving screen
            val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, view) }
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/**
 * Loading indicator displayed while photos are loading.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading photos...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

/**
 * Error UI displayed when photo loading fails.
 *
 * @param error Error message to display
 * @param onRetry Callback when retry button is clicked
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Empty state displayed when no photos are found.
 */
@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No Photos Found",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No photos found in the configured SMB share.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Displays the current media (photo or video) with scale-to-fit.
 * Includes swipe gesture detection for manual navigation and transition effects.
 *
 * @param bitmap Current photo bitmap (null for videos)
 * @param photoMetadata Current media metadata
 * @param photoIndex Current media index (0-based)
 * @param totalPhotos Total number of media items
 * @param transitionType Type of transition effect to apply
 * @param viewModel Slideshow view model
 * @param onNavigateToSettings Callback to navigate to settings screen
 */
@Composable
private fun MediaContent(
    bitmap: Bitmap?,
    photoMetadata: com.photoframe.core.model.Photo?,
    photoIndex: Int,
    totalPhotos: Int,
    transitionType: com.photoframe.core.model.TransitionType,
    viewModel: SlideshowViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val view = LocalView.current
    var dragOffset by remember { mutableStateOf(0f) }
    var showControls by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()

    // Get VideoPlayerViewModel for SmbDataSourceFactory injection
    val videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel()

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Combined gesture detector to avoid conflicts between tap and drag
                // Detects both taps (for navigation/controls) and horizontal drags (for swipe)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position
                    var totalDrag = 0f
                    var hasDragged = false

                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            val dragAmount = change.position.x - change.previousPosition.x
                            totalDrag += dragAmount
                            if (abs(totalDrag) > 10f) {
                                hasDragged = true
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    // Determine if this was a tap or drag based on movement
                    if (hasDragged && abs(totalDrag) > SWIPE_THRESHOLD) {
                        // Horizontal drag detected - handle as swipe
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                        if (totalDrag > 0) {
                            viewModel.previousPhoto(pauseAutoAdvance = true)
                        } else {
                            viewModel.nextPhoto(pauseAutoAdvance = true)
                        }
                    } else if (!hasDragged) {
                        // No significant drag - handle as tap
                        if (!showControls) {
                            showControls = true
                        } else {
                            // Handle zone-based navigation
                            val screenWidth = size.width
                            val tapX = downPosition.x

                            when {
                                tapX < screenWidth / 3 -> {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                    viewModel.previousPhoto(pauseAutoAdvance = true)
                                    showControls = false
                                }
                                tapX > screenWidth * 2 / 3 -> {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                    viewModel.nextPhoto(pauseAutoAdvance = true)
                                    showControls = false
                                }
                                else -> {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                    if (state.isPlaying) {
                                        viewModel.pause()
                                    } else {
                                        viewModel.play()
                                    }
                                    showControls = false
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Animated media display with transitions
        // Use fileName as key to prevent VideoPlayer disposal during transitions
        AnimatedContent(
            targetState = photoMetadata?.fileName ?: "empty_$photoIndex",
            transitionSpec = {
                // No transition for videos - instant switch to prevent player disposal
                if (photoMetadata?.isVideo == true) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    getTransitionSpec(transitionType)
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "media_transition"
        ) { fileName ->
            // Check if current media is a video
            android.util.Log.d("SlideshowScreen", "Rendering media: fileName=$fileName, isVideo=${photoMetadata?.isVideo}, hasBitmap=${bitmap != null}")

            if (photoMetadata?.isVideo == true) {
                // Display video player with SMB support
                android.util.Log.d("SlideshowScreen", "Showing VideoPlayer for: ${photoMetadata.path}")
                VideoPlayer(
                    videoPath = photoMetadata.path,
                    onVideoEnded = {
                        // Auto-advance to next media when video finishes
                        viewModel.nextPhoto(pauseAutoAdvance = false)
                    },
                    smbDataSourceFactory = videoPlayerViewModel.smbDataSourceFactory,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (bitmap != null) {
                // Apply Ken Burns effect for zoom transition
                if (transitionType == com.photoframe.core.model.TransitionType.ZOOM_KEN_BURNS) {
                    KenBurnsImage(
                        bitmap = bitmap,
                        contentDescription = "Photo ${photoIndex + 1} of $totalPhotos",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Display photo with standard scale
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo ${photoIndex + 1} of $totalPhotos",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit // Scale-to-fit, preserves aspect ratio
                    )
                }
            } else {
                // Neither video nor valid bitmap - black screen
                android.util.Log.w("SlideshowScreen", "BLACK SCREEN: No content to render. isVideo=${photoMetadata?.isVideo}, hasBitmap=${bitmap != null}")
            }
        }

        // Show control overlay when tapped
        if (showControls) {
            ControlOverlay(
                isPlaying = state.isPlaying,
                photoIndex = photoIndex,
                totalPhotos = totalPhotos,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

/**
 * Control overlay showing navigation zones and status.
 */
@Composable
private fun ControlOverlay(
    isPlaying: Boolean,
    photoIndex: Int,
    totalPhotos: Int,
    onNavigateToSettings: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        // Settings button (top-right corner)
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )
        }

        // Left zone indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(200.dp)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        // Center zone indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(200.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            val icon = if (isPlaying) {
                Icons.Filled.Pause
            } else {
                Icons.Filled.PlayArrow
            }
            Icon(
                imageVector = icon,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        // Right zone indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(200.dp)
                .align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        // Photo counter at bottom
        Text(
            text = "${photoIndex + 1} / $totalPhotos",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}

/**
 * Swipe threshold in pixels.
 * Drag must exceed this distance to trigger navigation.
 */
private const val SWIPE_THRESHOLD = 100f

/**
 * Returns the appropriate transition spec based on transition type.
 */
private fun getTransitionSpec(transitionType: com.photoframe.core.model.TransitionType): ContentTransform {
    val duration = 600 // milliseconds

    return when (transitionType) {
        com.photoframe.core.model.TransitionType.FADE -> {
            fadeIn(animationSpec = tween(duration)) togetherWith
                    fadeOut(animationSpec = tween(duration))
        }
        com.photoframe.core.model.TransitionType.SLIDE -> {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(duration)
            ) + fadeIn(animationSpec = tween(duration)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(duration)
                    ) + fadeOut(animationSpec = tween(duration))
        }
        com.photoframe.core.model.TransitionType.ZOOM_KEN_BURNS -> {
            // For Ken Burns, use fade transition (zoom is applied in KenBurnsImage)
            fadeIn(animationSpec = tween(duration)) togetherWith
                    fadeOut(animationSpec = tween(duration))
        }
    }
}

/**
 * Displays an image with Ken Burns effect (slow zoom and pan).
 */
@Composable
private fun KenBurnsImage(
    bitmap: Bitmap,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    // Animate scale from 1.0 to 1.15 over 10 seconds (or display interval)
    val scale by animateFloatAsState(
        targetValue = 1.15f,
        animationSpec = tween(durationMillis = 10000, easing = androidx.compose.animation.core.LinearEasing),
        label = "ken_burns_scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale),
            contentScale = ContentScale.Crop // Crop to fill for zoom effect
        )
    }
}
