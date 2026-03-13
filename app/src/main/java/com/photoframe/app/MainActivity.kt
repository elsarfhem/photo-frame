package com.photoframe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.photoframe.app.ui.settings.SettingsScreen
import com.photoframe.app.ui.slideshow.SlideshowScreen
import com.photoframe.app.ui.sources.SourcesScreen
import com.photoframe.app.ui.theme.PhotoFrameTheme
import com.photoframe.core.data.PhotoSourcesManager
import com.photoframe.core.model.Result
import com.photoframe.core.observer.MediaStoreObserver
import com.photoframe.core.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for the Photo Frame app.
 *
 * Features:
 * - Locked to landscape orientation (configured in AndroidManifest.xml)
 * - Navigation between slideshow and settings
 * - Auto-start slideshow if sources are configured
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var mediaStoreObserver: MediaStoreObserver

    @Inject
    lateinit var photoSourcesManager: PhotoSourcesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start MediaStore observer for real-time photo detection
        mediaStoreObserver.start()

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

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Loading -> {
                // Show nothing while loading
            }
            is Screen.Slideshow -> {
                SlideshowScreen(
                    shuffleEnabled = true,
                    autoPlay = true,
                    onNavigateToSettings = {
                        currentScreen = Screen.Settings
                    }
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
