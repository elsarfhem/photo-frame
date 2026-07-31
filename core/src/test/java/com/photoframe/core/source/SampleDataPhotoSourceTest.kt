package com.photoframe.core.source

import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleDataPhotoSourceTest {

    private fun buildSource() = SampleDataPhotoSource(
        id = "sample-1",
        displayName = "Sample Photos",
        isEnabled = true
    )

    @Test
    fun `type is SAMPLE`() {
        assertEquals(PhotoSourceType.SAMPLE, buildSource().type)
    }

    @Test
    fun `validate always succeeds without I O`() = runTest {
        val result = buildSource().validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `estimatePhotoCount returns bundled count`() = runTest {
        assertEquals(6, buildSource().estimatePhotoCount())
    }

    @Test
    fun `scanPhotos returns 6 photos with asset paths`() = runTest {
        val result = buildSource().scanPhotos()
        assertTrue(result is Result.Success)
        val photos = (result as Result.Success).data
        assertEquals(6, photos.size)
        photos.forEach { photo ->
            assertTrue(photo.path.startsWith("file:///android_asset/sample_photos/"))
            assertEquals("image/jpeg", photo.mimeType)
        }
    }

    @Test
    fun `scanPhotos respects maxPhotos`() = runTest {
        val result = buildSource().scanPhotos(maxPhotos = 2)
        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.size)
    }
}
