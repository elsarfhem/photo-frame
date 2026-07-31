package com.photoframe.app.ui.sources

import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result
import com.photoframe.core.model.SourceConfig
import com.photoframe.core.repository.MultiSourcePhotoRepository
import com.photoframe.core.repository.SettingsRepository
import com.photoframe.core.security.CredentialStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourcesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var multiSourceRepository: MultiSourcePhotoRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var credentialStore: CredentialStore
    private lateinit var viewModel: SourcesViewModel

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        multiSourceRepository = mockk<MultiSourcePhotoRepository>()
        settingsRepository = mockk<SettingsRepository>()
        credentialStore = mockk<CredentialStore>()

        every { multiSourceRepository.photoSources } returns MutableStateFlow(emptyList())
        coEvery { multiSourceRepository.addPhotoSource(any()) } returns Result.success(Unit)

        viewModel = SourcesViewModel(multiSourceRepository, settingsRepository, credentialStore)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `addSmbSource with server demo creates SAMPLE source and skips credentials`() = runTest {
        viewModel.addSmbSource(
            displayName = "",
            server = " Demo ",
            share = "",
            path = "",
            domain = "",
            username = "",
            password = ""
        )
        dispatcher.scheduler.advanceUntilIdle()

        val captured = mutableListOf<PhotoSourceConfig>()
        coVerify { multiSourceRepository.addPhotoSource(capture(captured)) }
        assertEquals(1, captured.size)
        assertEquals(PhotoSourceType.SAMPLE, captured[0].type)
        assertTrue(captured[0].config is SourceConfig.SampleConfig)

        coVerify(exactly = 0) { credentialStore.storePassword(any(), any()) }
        assertEquals(null, viewModel.state.value.error)
        assertTrue(!viewModel.state.value.showAddDialog)
    }

    @Test
    fun `addSmbSource with real server still validates required fields`() = runTest {
        viewModel.addSmbSource(
            displayName = "",
            server = "192.168.1.2",
            share = "",
            path = "",
            domain = "",
            username = "",
            password = ""
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Share is required", viewModel.state.value.error)
        coVerify(exactly = 0) { multiSourceRepository.addPhotoSource(any()) }
    }
}
