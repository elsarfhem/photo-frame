package com.photoframe.core.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoTest {

    private fun photo(fileName: String, mime: String = "image/jpeg"): Photo = Photo(
        path = "smb://host/share/$fileName",
        fileName = fileName,
        fileSize = 1_000L,
        lastModified = 0L,
        mimeType = mime
    )

    @Test
    fun `extension extracts final suffix`() {
        assertEquals("jpg", photo("foo.bar.jpg").extension)
        assertEquals("", photo("noextension").extension)
    }

    @Test
    fun `isJpeg matches mime or extension case insensitively`() {
        assertTrue(photo("a.JPG", mime = "application/octet-stream").isJpeg)
        assertTrue(photo("a.jpeg", mime = "image/jpeg").isJpeg)
        assertFalse(photo("a.png", mime = "image/png").isJpeg)
    }

    @Test
    fun `isVideo detects mp4 mov m4v`() {
        assertTrue(photo("clip.mp4", mime = "").isVideo)
        assertTrue(photo("clip.MOV", mime = "").isVideo)
        assertTrue(photo("clip.m4v", mime = "video/mp4").isVideo)
        assertFalse(photo("pic.jpg").isVideo)
    }

    @Test
    fun `isRaw detects dng cr2 nef rw2 arw`() {
        listOf("dng", "cr2", "nef", "rw2", "arw").forEach { ext ->
            assertTrue(photo("file.$ext", mime = "").isRaw, "Expected $ext to be RAW")
        }
        assertFalse(photo("file.jpg").isRaw)
    }
}
