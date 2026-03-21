package com.photoframe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photoframe.app.ui.settings.SettingsScreen
import com.photoframe.app.ui.slideshow.SlideshowScreen
import com.photoframe.app.ui.sources.SourcesScreen
import com.photoframe.app.ui.theme.PhotoFrameTheme
import com.photoframe.core.data.PhotoSourcesManager
import com.photoframe.core.model.Result
import com.photoframe.core.observer.MediaStoreObserver
import com.photoframe.core.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import javax.inject.Inject

// Type-safe navigation routes
@Serializable object LoadingRoute
@Serializable object SlideshowRoute
@Serializable object SettingsRoute
@Serializable object SourcesRoute

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
    val navController = rememberNavController()

    // slideshowReloadTrigger lives outside NavHost so it survives navigation.
    // Incrementing it forces SlideshowScreen to re-collect settings on return from Settings.
    var slideshowReloadTrigger by remember { mutableStateOf(0) }

    // Check for configured sources and navigate appropriately
    LaunchedEffect(Unit) {
        val sourcesResult = photoSourcesManager.getSources()
        val sources = (sourcesResult as? Result.Success)?.data ?: emptyList()

        // If first launch, mark it complete (skip the useless wizard)
        val firstLaunchResult = settingsRepository.isFirstLaunch()
        val isFirstLaunch = (firstLaunchResult as? Result.Success)?.data == true
        if (isFirstLaunch) {
            settingsRepository.markFirstLaunchComplete()
        }

        val startRoute = if (sources.isEmpty()) SourcesRoute else SlideshowRoute
        // Navigate from Loading to the actual start screen, removing Loading from the back stack
        navController.navigate(startRoute) {
            popUpTo<LoadingRoute> { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = LoadingRoute,
        // Disable NavHost's default fadeIn/fadeOut transitions to avoid interfering
        // with SlideshowScreen's own AnimatedContent photo transitions.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<LoadingRoute> {
            // Show nothing while loading
        }

        composable<SlideshowRoute> {
            SlideshowScreen(
                onNavigateToSettings = {
                    // Push Settings on top of Slideshow (Slideshow stays in back stack)
                    navController.navigate(SettingsRoute)
                },
                reloadTrigger = slideshowReloadTrigger
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = {
                    // Settings changed: pop Settings + Slideshow, then navigate fresh to Slideshow.
                    // This recreates the SlideshowViewModel so it re-reads updated settings.
                    slideshowReloadTrigger++
                    navController.navigate(SlideshowRoute) {
                        popUpTo<SlideshowRoute> { inclusive = true }
                    }
                },
                onNavigateToSources = {
                    // Push Sources on top of Settings
                    navController.navigate(SourcesRoute)
                }
            )
        }

        composable<SourcesRoute> {
            SourcesScreen(
                onNavigateBack = {
                    // Pop Sources, return to previous screen (Settings or Loading)
                    navController.popBackStack()
                }
            )
        }
    }
}
