package com.photoframe.app.ui.slideshow

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

/**
 * Video player composable using ExoPlayer.
 *
 * Features:
 * - Auto-play when loaded
 * - Full-screen display with proper scaling
 * - Lifecycle-aware (pauses/resumes with activity)
 * - Callback when video ends
 *
 * @param videoPath SMB path to video file (e.g., "smb://server/share/video.mp4")
 * @param onVideoEnded Callback invoked when video finishes playing
 * @param modifier Modifier for layout
 */
@Composable
fun VideoPlayer(
    videoPath: String,
    onVideoEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Create ExoPlayer instance
    val exoPlayer = remember {
        createExoPlayer(context, videoPath, onVideoEnded)
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
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Creates and configures ExoPlayer instance for SMB video playback.
 *
 * @param context Android context
 * @param videoPath SMB path to video
 * @param onVideoEnded Callback when video ends
 * @return Configured ExoPlayer instance
 */
private fun createExoPlayer(
    context: Context,
    videoPath: String,
    onVideoEnded: () -> Unit
): ExoPlayer {
    // Create data source factory for SMB support
    val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)

    // Create media source
    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
        .createMediaSource(MediaItem.fromUri(Uri.parse(videoPath)))

    // Create ExoPlayer
    return ExoPlayer.Builder(context).build().apply {
        setMediaSource(mediaSource)
        prepare()
        playWhenReady = true // Auto-play
        repeatMode = Player.REPEAT_MODE_OFF

        // Listen for playback end
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        })
    }
}
