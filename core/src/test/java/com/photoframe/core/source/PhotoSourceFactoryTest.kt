package com.photoframe.core.source

import com.photoframe.core.data.LocalPhotoDataSource
import com.photoframe.core.data.SmbPhotoDataSource
import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.Result
import com.photoframe.core.security.CredentialStore
import com.photoframe.core.smb.SmbClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

class PhotoSourceFactoryTest {

    private fun buildFactory() = PhotoSourceFactory(
        smbClient = mockk<SmbClient>(),
        smbPhotoDataSource = mockk<SmbPhotoDataSource>(),
        localPhotoDataSource = mockk<LocalPhotoDataSource>(),
        credentialStore = mockk<CredentialStore>()
    )

    @Test
    fun `createSource returns SampleDataPhotoSource for SAMPLE config`() = runTest {
        val config = PhotoSourceConfig.createSample(
            id = "sample-1",
            displayName = "Sample Photos"
        )

        val result = buildFactory().createSource(config)

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data is SampleDataPhotoSource)
    }
}
