package com.photoframe.tests.functional

import com.photoframe.core.model.Photo
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the Photo catalog: filtering, classification, and
 * mixed-extension handling. Validates the slideshow's media-type routing
 * (JPEG vs PNG vs RAW vs video) end-to-end without touching SMB or the disk.
 */
class PhotoCatalogFunctionalTest {

    private fun photo(name: String, mime: String = "application/octet-stream"): Photo = Photo(
        path = "smb://host/share/$name",
        fileName = name,
        fileSize = 0L,
        lastModified = 0L,
        mimeType = mime
    )

    @Test
    fun `mixed catalog splits cleanly into still and motion`() {
        val catalog = listOf(
            photo("a.jpg"),
            photo("b.mp4"),
            photo("c.png", mime = "image/png"),
            photo("d.mov"),
            photo("e.dng"),
            photo("README")
        )

        val stills = catalog.filter { !it.isVideo }
        val videos = catalog.filter { it.isVideo }

        assertEquals(4, stills.size)
        assertEquals(2, videos.size)
        assertTrue(stills.any { it.isRaw })
        assertTrue(stills.any { it.isJpeg })
        assertTrue(stills.any { it.isPng })
    }

    @Test
    fun `folder path derivation strips filename from SMB path`() {
        val p = photo("Belgio 2025/video/clip.mp4")
        val folder = p.path.substringBeforeLast('/').removePrefix("smb://")
        assertEquals("host/share/Belgio 2025/video", folder)
    }

    @Test
    fun `extension classification is case insensitive`() {
        assertTrue(photo("CAM.MP4").isVideo)
        assertTrue(photo("SHOT.JPEG").isJpeg)
        assertTrue(photo("RAW.CR2").isRaw)
    }
}
