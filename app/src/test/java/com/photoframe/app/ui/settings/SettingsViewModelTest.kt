package com.photoframe.app.ui.settings

import com.photoframe.core.model.Result
import com.photoframe.core.model.SlideshowSettings
import com.photoframe.core.model.SmbConnection
import com.photoframe.core.model.TransitionType
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.smb.SmbClient
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsViewModel.
 *
 * Tests:
 * - Loading settings from repository
 * - Form validation
 * - Saving settings
 * - Testing SMB connection
 * - Resetting to defaults
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var smbClient: SmbClient
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock dependencies
        settingsRepository = mockk(relaxed = true)
        smbClient = mockk(relaxed = true)

        // Mock StateFlows
        every { settingsRepository.smbConnection } returns MutableStateFlow(null)
        every { settingsRepository.slideshowSettings } returns MutableStateFlow(SlideshowSettings.DEFAULT)

        // Mock suspend functions
        coEvery { settingsRepository.loadSmbConnection() } returns Result.Success(null)
        coEvery { settingsRepository.loadSlideshowSettings() } returns Result.Success(SlideshowSettings.DEFAULT)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assert(state.smbServer.isEmpty())
        assert(state.smbShare.isEmpty())
        assert(state.smbUsername.isEmpty())
        assert(state.smbPassword.isEmpty())
    }

    @Test
    fun `loads SMB connection from repository`() = runTest {
        val connection = SmbConnection(
            serverUrl = "192.168.1.100",
            sharePath = "photos",
            username = "user",
            domain = null
        )
        coEvery { settingsRepository.loadSmbConnection() } returns Result.Success(connection)
        coEvery { settingsRepository.getSmbPassword() } returns Result.Success("password123")

        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assert(state.smbServer == "192.168.1.100")
        assert(state.smbShare == "photos")
        assert(state.smbUsername == "user")
        assert(state.smbPassword == "password123")
    }

    @Test
    fun `loads slideshow settings from repository`() = runTest {
        val settings = SlideshowSettings(
            displayIntervalSeconds = 30,
            transitionType = TransitionType.FADE,
            shuffleEnabled = true
        )
        coEvery { settingsRepository.loadSlideshowSettings() } returns Result.Success(settings)

        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assert(state.displayInterval == 30)
        assert(state.transitionType == TransitionType.FADE)
        assert(state.shuffleEnabled)
    }

    @Test
    fun `updateSmbServer marks state as modified`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSmbServer("192.168.1.100")

        val state = viewModel.state.value
        assert(state.smbServer == "192.168.1.100")
        assert(state.isModified)
    }

    @Test
    fun `validation error when server is empty`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSmbServer("")

        val state = viewModel.state.value
        assert(state.validationErrors.containsKey("smbServer"))
    }

    @Test
    fun `validation error when share is empty`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSmbShare("")

        val state = viewModel.state.value
        assert(state.validationErrors.containsKey("smbShare"))
    }

    @Test
    fun `validation error when username is empty`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSmbUsername("")

        val state = viewModel.state.value
        assert(state.validationErrors.containsKey("smbUsername"))
    }

    @Test
    fun `validation error when password is empty`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSmbPassword("")

        val state = viewModel.state.value
        assert(state.validationErrors.containsKey("smbPassword"))
    }

    @Test
    fun `testConnection succeeds with valid credentials`() = runTest {
        coEvery { smbClient.testConnection(any(), any()) } returns Result.Success(Unit)

        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set valid credentials
        viewModel.updateSmbServer("192.168.1.100")
        viewModel.updateSmbShare("photos")
        viewModel.updateSmbUsername("user")
        viewModel.updateSmbPassword("password")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.testConnection()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assert(state.connectionTestResult is ConnectionTestResult.Success)
    }

    @Test
    fun `testConnection fails with invalid credentials`() = runTest {
        coEvery { smbClient.testConnection(any(), any()) } returns Result.Error(Exception("Connection failed"))

        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set valid credentials
        viewModel.updateSmbServer("192.168.1.100")
        viewModel.updateSmbShare("photos")
        viewModel.updateSmbUsername("user")
        viewModel.updateSmbPassword("password")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.testConnection()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assert(state.connectionTestResult is ConnectionTestResult.Failure)
    }

    @Test
    fun `saveSettings succeeds with valid data`() = runTest {
        coEvery { settingsRepository.saveSmbConnection(any(), any()) } returns Result.Success(Unit)
        coEvery { settingsRepository.saveSlideshowSettings(any()) } returns Result.Success(Unit)

        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set valid data
        viewModel.updateSmbServer("192.168.1.100")
        viewModel.updateSmbShare("photos")
        viewModel.updateSmbUsername("user")
        viewModel.updateSmbPassword("password")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assert(state.saveResult is SaveResult.Success)
        assert(!state.isModified)
    }

    @Test
    fun `saveSettings fails with validation errors`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        // Don't set any data (all fields empty)
        viewModel.saveSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assert(state.saveResult is SaveResult.Failure)
    }

    @Test
    fun `resetToDefaults sets default values`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetToDefaults()

        val state = viewModel.state.value
        assert(state.displayInterval == SlideshowSettings.DEFAULT_DISPLAY_INTERVAL_SECONDS)
        assert(state.transitionType == TransitionType.DEFAULT)
        assert(!state.shuffleEnabled)
        assert(state.isModified)
    }

    @Test
    fun `toggleShuffle changes shuffle state`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleShuffle(true)

        val state = viewModel.state.value
        assert(state.shuffleEnabled)
        assert(state.isModified)
    }

    @Test
    fun `updateDisplayInterval changes interval`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateDisplayInterval(30)

        val state = viewModel.state.value
        assert(state.displayInterval == 30)
        assert(state.isModified)
    }

    @Test
    fun `updateTransitionType changes transition`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTransitionType(TransitionType.SLIDE)

        val state = viewModel.state.value
        assert(state.transitionType == TransitionType.SLIDE)
        assert(state.isModified)
    }

    @Test
    fun `clearSaveResult clears save result`() = runTest {
        viewModel = SettingsViewModel(settingsRepository, smbClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearSaveResult()

        val state = viewModel.state.value
        assert(state.saveResult == null)
    }
}
