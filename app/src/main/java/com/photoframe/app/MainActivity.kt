package com.photoframe.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.lifecycleScope
import com.photoframe.app.ui.settings.SettingsScreen
import com.photoframe.app.ui.slideshow.SlideshowScreen
import com.photoframe.app.ui.sources.SourcesScreen
import com.photoframe.app.ui.theme.PhotoFrameTheme
import com.photoframe.core.data.PhotoSourcesManager
import com.photoframe.core.model.Result
import com.photoframe.core.observer.MediaStoreObserver
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.scheduling.ScheduleWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the Photo Frame app.
 *
 * Features:
 * - Locked to landscape orientation (configured in AndroidManifest.xml)
 * - Navigation between slideshow and settings
 * - Auto-start slideshow if SMB is configured
 * - Handles scheduled start/stop broadcasts from ScheduleWorker
 *
 * Phase 5: Settings & Scheduling
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var mediaStoreObserver: MediaStoreObserver

    @Inject
    lateinit var photoSourcesManager: PhotoSourcesManager

    private var scheduleReceiver: ScheduleBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start MediaStore observer for real-time photo detection
        mediaStoreObserver.start()

        // Register broadcast receiver for scheduled actions
        registerScheduleReceiver()

        setContent {
            PhotoFrameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoFrameApp(settingsRepository, photoSourcesManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaStoreObserver.stop()
        unregisterScheduleReceiver()
    }

    /**
     * Registers broadcast receiver for scheduled start/stop events.
     */
    private fun registerScheduleReceiver() {
        scheduleReceiver = ScheduleBroadcastReceiver { action ->
            when (action) {
                ScheduleWorker.ACTION_SCHEDULE_START_SLIDESHOW -> {
                    android.util.Log.i("MainActivity", "Scheduled start received")
                    // Screen is already displayed, just ensure we're on slideshow
                }
                ScheduleWorker.ACTION_SCHEDULE_STOP_SLIDESHOW -> {
                    android.util.Log.i("MainActivity", "Scheduled stop received")
                    // In production, you might finish() the activity or pause slideshow
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(ScheduleWorker.ACTION_SCHEDULE_START_SLIDESHOW)
            addAction(ScheduleWorker.ACTION_SCHEDULE_STOP_SLIDESHOW)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scheduleReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(scheduleReceiver, filter)
        }
    }

    /**
     * Unregisters broadcast receiver.
     */
    private fun unregisterScheduleReceiver() {
        scheduleReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered
            }
        }
        scheduleReceiver = null
    }

    /**
     * Broadcast receiver for scheduled slideshow events.
     */
    private class ScheduleBroadcastReceiver(
        private val onAction: (String) -> Unit
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { onAction(it) }
        }
    }
}

/**
 * Main app composable with navigation.
 */
@Composable
fun PhotoFrameApp(
    settingsRepository: SettingsRepository,
    photoSourcesManager: PhotoSourcesManager
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }

    // Check for configured sources and navigate appropriately
    LaunchedEffect(Unit) {
        // Check if any photo sources are configured
        val sourcesResult = photoSourcesManager.getSources()
        val sources = (sourcesResult as? Result.Success)?.data ?: emptyList()

        // If first launch, mark it complete (skip the useless wizard)
        val firstLaunchResult = settingsRepository.isFirstLaunch()
        val isFirstLaunch = (firstLaunchResult as? Result.Success)?.data == true
        if (isFirstLaunch) {
            settingsRepository.markFirstLaunchComplete()
        }

        currentScreen = if (sources.isEmpty()) {
            // No sources configured, go to Sources screen to set up
            Screen.Sources
        } else {
            // Sources configured, go to slideshow
            Screen.Slideshow
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Triple-tap to navigate to settings from slideshow
                var tapCount = 0
                var lastTapTime = 0L

                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 500) {
                            tapCount++
                            if (tapCount >= 2 && currentScreen is Screen.Slideshow) {
                                // Triple tap detected
                                currentScreen = Screen.Settings
                                tapCount = 0
                            }
                        } else {
                            tapCount = 1
                        }
                        lastTapTime = currentTime
                    }
                )
            }
    ) {
        when (val screen = currentScreen) {
            is Screen.Loading -> {
                // Show nothing while loading
            }
            is Screen.Slideshow -> {
                SlideshowScreen(
                    shuffleEnabled = true,
                    autoPlay = true
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    onNavigateBack = {
                        // After saving settings, return to slideshow
                        currentScreen = Screen.Slideshow
                    },
                    onNavigateToSources = {
                        currentScreen = Screen.Sources
                    }
                )
            }
            is Screen.Sources -> {
                SourcesScreen(
                    onNavigateBack = {
                        currentScreen = Screen.Settings
                    }
                )
            }
        }
    }
}

/**
 * App navigation screens.
 */
sealed class Screen {
    object Loading : Screen()
    object Slideshow : Screen()
    object Settings : Screen()
    object Sources : Screen()
}
