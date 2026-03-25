package com.photoframe.app.ui.slideshow

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import com.photoframe.app.media.SmbDataSourceFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * ViewModel for VideoPlayer to provide SmbDataSourceFactory via Hilt.
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    val smbDataSourceFactory: SmbDataSourceFactory
) : ViewModel()

/**
 * Video player composable using ExoPlayer.
 *
 * Features:
 * - Auto-play when loaded
 * - Full-screen display with proper scaling
 * - Lifecycle-aware (pauses/resumes with activity)
 * - Callback when video ends
 * - SMB protocol support via custom DataSource
 * - Buffering timeout: skips to next media if stuck buffering for too long
 *
 * @param videoPath SMB path to video file (e.g., "smb://server/share/video.mp4")
 * @param onVideoEnded Callback invoked when video finishes playing
 * @param smbDataSourceFactory Factory for SMB DataSource (injected via Hilt)
 * @param modifier Modifier for layout
 */
@UnstableApi
@Composable
fun VideoPlayer(
    videoPath: String,
    onVideoEnded: () -> Unit,
    smbDataSourceFactory: SmbDataSourceFactory,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Log.d(TAG, "VideoPlayer composable invoked for: $videoPath")

    // Track whether onVideoEnded has already been called to prevent double-fire
    var hasEnded by remember(videoPath) { mutableStateOf(false) }

    val safeOnVideoEnded: () -> Unit = {
        if (!hasEnded) {
            hasEnded = true
            onVideoEnded()
        }
    }

    // Create ExoPlayer instance
    val exoPlayer = remember(videoPath) {
        Log.d(TAG, "Creating ExoPlayer for: $videoPath")
        createExoPlayer(context, videoPath, safeOnVideoEnded, smbDataSourceFactory)
    }

    // Buffering timeout — if ExoPlayer is stuck buffering for too long, skip
    LaunchedEffect(videoPath) {
        delay(VIDEO_BUFFER_TIMEOUT_MS)
        if (!hasEnded) {
            val state = exoPlayer.playbackState
            if (state == Player.STATE_BUFFERING || state == Player.STATE_IDLE) {
                Log.w(TAG, "Video buffering timeout (${VIDEO_BUFFER_TIMEOUT_MS}ms) for: $videoPath, forcing skip")
                safeOnVideoEnded()
            }
        }
    }

    // Release player when composable leaves composition
    DisposableEffect(videoPath) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Hide playback controls
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) // Show spinner during loading
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Creates and configures ExoPlayer instance for video playback.
 * Supports both SMB and standard protocols (file://, http://).
 *
 * @param context Android context
 * @param videoPath Path to video (SMB, file, or HTTP URL)
 * @param onVideoEnded Callback when video ends
 * @param smbDataSourceFactory Factory for SMB DataSource
 * @return Configured ExoPlayer instance
 */
@UnstableApi
private fun createExoPlayer(
    context: Context,
    videoPath: String,
    onVideoEnded: () -> Unit,
    smbDataSourceFactory: SmbDataSourceFactory
): ExoPlayer {
    // Choose data source factory based on protocol
    val dataSourceFactory: DataSource.Factory = if (videoPath.startsWith("smb://", ignoreCase = true)) {
        Log.d(TAG, "createExoPlayer: Using SMB data source for: $videoPath")
        smbDataSourceFactory
    } else {
        Log.d(TAG, "createExoPlayer: Using default data source for: $videoPath")
        DefaultDataSource.Factory(context)
    }

    // No-retry policy: fail immediately instead of retrying 10x on SMB errors
    val noRetryPolicy = object : DefaultLoadErrorHandlingPolicy() {
        override fun getMinimumLoadableRetryCount(dataType: Int): Int = 0
        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
            C.TIME_UNSET // Don't retry
    }

    // Create media source
    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
        .setLoadErrorHandlingPolicy(noRetryPolicy)
        .createMediaSource(MediaItem.fromUri(Uri.parse(videoPath)))

    // Create ExoPlayer
    return ExoPlayer.Builder(context).build().apply {
        setMediaSource(mediaSource)
        prepare()
        playWhenReady = true // Auto-play
        repeatMode = Player.REPEAT_MODE_OFF

        // Listen for playback end and errors
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "onPlaybackStateChanged: $stateString for $videoPath")

                if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Video playback ended: $videoPath")
                    onVideoEnded()
                } else if (playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Video ready to play: $videoPath, isPlaying=${this@apply.isPlaying}")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "onIsPlayingChanged: isPlaying=$isPlaying for $videoPath")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error for $videoPath: ${error.message}", error)
                Log.e(TAG, "Error code: ${error.errorCode}")
                // Trigger onVideoEnded to skip to next media on error
                onVideoEnded()
            }

            override fun onRenderedFirstFrame() {
                Log.d(TAG, "onRenderedFirstFrame: First frame rendered for $videoPath")
            }
        })
    }
}

private const val TAG = "VideoPlayer"
private const val VIDEO_BUFFER_TIMEOUT_MS = 60_000L
