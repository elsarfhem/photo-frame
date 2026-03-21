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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.photoframe.app.ui.slideshow.transitions.PanTransition
import com.photoframe.app.ui.slideshow.transitions.ZoomTransition
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
 * @param onNavigateToSettings Callback to navigate to settings screen
 */
@Composable
fun SlideshowScreen(
    viewModel: SlideshowViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    reloadTrigger: Int = 0
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Reload photos when returning from settings (reloadTrigger > 0 means we navigated back)
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            viewModel.reload()
        }
    }

    // Hoist isPlaying as a Compose snapshot state for synchronous UI updates.
    // StateFlow → collectAsState is async, so the overlay could show a stale icon
    // when the gesture handler toggles play/pause and shows controls in the same frame.
    var isPlayingLocal by remember { mutableStateOf(state.isPlaying) }
    // Sync ViewModel → Compose state (for changes from auto-advance, watchdog, etc.)
    LaunchedEffect(state.isPlaying) { isPlayingLocal = state.isPlaying }

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

    // Note: Slideshow initialization moved to ViewModel's init block to eliminate
    // timing gap where combine flow could propagate buffer errors before initialization.
    // The ViewModel now auto-initializes based on settings (shuffle + auto-play).

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
                    isPlaying = isPlayingLocal,
                    displayIntervalMillis = state.displayIntervalMillis,
                    panAnimationEnabled = state.panAnimationEnabled,
                    navigationDirection = state.navigationDirection,
                    currentRotation = state.currentRotation,
                    viewModel = viewModel,
                    onPlayPause = { playing ->
                        isPlayingLocal = playing  // Immediate Compose snapshot update
                        if (playing) viewModel.play() else viewModel.pause()
                    },
                    onNavigateToSettings = onNavigateToSettings,
                    onRotateClockwise = viewModel::rotateClockwise,
                    onRotateCounterClockwise = viewModel::rotateCounterClockwise
                )

                // Connection status indicator (top-right corner)
                ConnectionStatusIndicator(
                    isConnected = !state.hasError,
                    isConnecting = state.isLoading,
                    error = state.error,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            // Defensive fallback for unexpected state gaps
            else -> {
                LoadingContent()
            }
        }

        // Settings button always accessible (except during ready state which has its own)
        if (!state.isReady) {
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
 * @param isPlaying Whether slideshow auto-advance is active
 * @param displayIntervalMillis Display interval for animations
 * @param panAnimationEnabled Whether pan animation is enabled
 * @param navigationDirection Direction of navigation (forward/backward)
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
    isPlaying: Boolean,
    displayIntervalMillis: Long,
    panAnimationEnabled: Boolean,
    navigationDirection: NavigationDirection,
    currentRotation: Int = 0,
    viewModel: SlideshowViewModel = hiltViewModel(),
    onPlayPause: (Boolean) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onRotateClockwise: () -> Unit = {},
    onRotateCounterClockwise: () -> Unit = {}
) {
    val view = LocalView.current
    var dragOffset by remember { mutableStateOf(0f) }
    var showControls by remember { mutableStateOf(false) }
    var controlsRevision by remember { mutableStateOf(0) }

    // Keep updated references for values read inside pointerInput (which captures at composition time)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentOnPlayPause by rememberUpdatedState(onPlayPause)
    val currentShowControls by rememberUpdatedState(showControls)

    // Get VideoPlayerViewModel for SmbDataSourceFactory injection
    val videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel()

    // Auto-hide controls after 3 seconds (revision resets timer)
    LaunchedEffect(showControls, controlsRevision) {
        if (showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Combined gesture detector: tap to show overlay, then tap zones to act
                // Swipes always navigate regardless of overlay state
                awaitEachGesture {
                    val down = awaitFirstDown()
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

                    // Swipes always work regardless of overlay state
                    if (hasDragged && abs(totalDrag) > SWIPE_THRESHOLD) {
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                        if (totalDrag > 0) {
                            viewModel.previousPhoto(pauseAutoAdvance = false)
                        } else {
                            viewModel.nextPhoto(pauseAutoAdvance = false)
                        }
                        return@awaitEachGesture
                    }

                    // Tap: first tap shows overlay, second tap performs action
                    if (!currentShowControls) {
                        showControls = true
                    } else {
                        val screenWidth = size.width
                        val tapX = downPosition.x
                        when {
                            tapX < screenWidth / 3 -> {
                                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                viewModel.previousPhoto(pauseAutoAdvance = false)
                            }
                            tapX > screenWidth * 2 / 3 -> {
                                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                viewModel.nextPhoto(pauseAutoAdvance = false)
                            }
                            else -> {
                                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                currentOnPlayPause(!currentIsPlaying)
                            }
                        }
                        // Reset auto-hide timer
                        controlsRevision++
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Apply user rotation to bitmap
        val rotatedBitmap = remember(bitmap, currentRotation) {
            if (bitmap == null || currentRotation % 360 == 0) bitmap
            else {
                val matrix = android.graphics.Matrix().apply { postRotate(currentRotation.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        }

        // Cache bitmaps per fileName so exiting AnimatedContent slots retain their old photo.
        // Without this, both entering and exiting slots read the same (new) rotatedBitmap
        // from the outer scope, causing the new photo to flash before the transition animates.
        val bitmapCache = remember { mutableMapOf<String, Bitmap?>() }
        val currentKey = photoMetadata?.fileName ?: "empty_$photoIndex"
        bitmapCache[currentKey] = rotatedBitmap

        // Animated media display with transitions
        // Use fileName as key to prevent VideoPlayer disposal during transitions
        AnimatedContent(
            targetState = currentKey,
            transitionSpec = {
                // No transition for videos - instant switch to prevent player disposal
                if (photoMetadata?.isVideo == true) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    getTransitionSpec(transitionType, navigationDirection)
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "media_transition"
        ) { fileName ->
            // Look up the bitmap for THIS animation slot (entering uses new, exiting uses old)
            val slotBitmap = bitmapCache[fileName]

            android.util.Log.d("SlideshowScreen", "Rendering media: fileName=$fileName, isVideo=${photoMetadata?.isVideo}, hasBitmap=${slotBitmap != null}")

            if (photoMetadata?.isVideo == true && fileName == currentKey) {
                // Display video player with SMB support (only for the entering slot)
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
            } else if (slotBitmap != null) {
                // Apply Ken Burns or Pan animation based on settings
                if (transitionType == com.photoframe.core.model.TransitionType.ZOOM_KEN_BURNS) {
                    ZoomTransition(
                        bitmap = slotBitmap,
                        photoIndex = photoIndex,
                        contentDescription = "Photo ${photoIndex + 1} of $totalPhotos",
                        durationMillis = displayIntervalMillis.toInt(),
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (panAnimationEnabled) {
                    // Apply pan animation with crop (no black bands)
                    PanTransition(
                        bitmap = slotBitmap,
                        photoIndex = photoIndex,
                        contentDescription = "Photo ${photoIndex + 1} of $totalPhotos",
                        durationMillis = displayIntervalMillis.toInt(),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Display photo with standard scale (letterbox/pillarbox)
                    Image(
                        bitmap = slotBitmap.asImageBitmap(),
                        contentDescription = "Photo ${photoIndex + 1} of $totalPhotos",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit // Scale-to-fit, preserves aspect ratio
                    )
                }

            } else {
                // Neither video nor valid bitmap - black screen
                android.util.Log.w("SlideshowScreen", "BLACK SCREEN: No content to render. isVideo=${photoMetadata?.isVideo}, hasBitmap=${slotBitmap != null}")
            }
        }

        // Show control overlay when tapped
        if (showControls) {
            ControlOverlay(
                isPlaying = isPlaying,
                photoIndex = photoIndex,
                totalPhotos = totalPhotos,
                isVideo = photoMetadata?.isVideo == true,
                onNavigateToSettings = onNavigateToSettings,
                onRotateClockwise = onRotateClockwise,
                onRotateCounterClockwise = onRotateCounterClockwise
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
    isVideo: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    onRotateClockwise: () -> Unit = {},
    onRotateCounterClockwise: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        // Top-right controls: rotate buttons + settings
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            if (!isVideo) {
                IconButton(onClick = onRotateCounterClockwise) {
                    Icon(
                        imageVector = Icons.Filled.RotateLeft,
                        contentDescription = "Rotate counter-clockwise",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
                IconButton(onClick = onRotateClockwise) {
                    Icon(
                        imageVector = Icons.Filled.RotateRight,
                        contentDescription = "Rotate clockwise",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
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
 * Returns the appropriate transition spec based on transition type and navigation direction.
 *
 * @param transitionType Type of transition effect
 * @param navigationDirection Direction of navigation (FORWARD=next, BACKWARD=previous)
 */
private fun getTransitionSpec(
    transitionType: com.photoframe.core.model.TransitionType,
    navigationDirection: NavigationDirection
): ContentTransform {
    val fadeDuration = 600 // milliseconds for fade transitions
    val slideDuration = 800 // milliseconds for slide transitions (longer for smoothness)

    return when (transitionType) {
        com.photoframe.core.model.TransitionType.FADE -> {
            fadeIn(animationSpec = tween(fadeDuration)) togetherWith
                    fadeOut(animationSpec = tween(fadeDuration))
        }
        com.photoframe.core.model.TransitionType.SLIDE -> {
            // Slide direction based on navigation:
            // FORWARD (next): Slide in from right, slide out to left
            // BACKWARD (previous): Slide in from left, slide out to right
            val (slideInOffset, slideOutOffset) = when (navigationDirection) {
                NavigationDirection.FORWARD -> {
                    { fullWidth: Int -> fullWidth } to { fullWidth: Int -> -fullWidth }
                }
                NavigationDirection.BACKWARD -> {
                    { fullWidth: Int -> -fullWidth } to { fullWidth: Int -> fullWidth }
                }
            }

            slideInHorizontally(
                initialOffsetX = slideInOffset,
                animationSpec = tween(slideDuration)
            ) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = slideOutOffset,
                        animationSpec = tween(slideDuration)
                    )
        }
        com.photoframe.core.model.TransitionType.ZOOM_KEN_BURNS -> {
            // For Ken Burns, use fade transition (zoom is applied in ZoomTransition)
            fadeIn(animationSpec = tween(fadeDuration)) togetherWith
                    fadeOut(animationSpec = tween(fadeDuration))
        }
    }
}

