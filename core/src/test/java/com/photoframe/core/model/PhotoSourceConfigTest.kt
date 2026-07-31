package com.photoframe.core.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoSourceConfigTest {

    @Test
    fun `createSmb wires SMB type and config`() {
        val config = PhotoSourceConfig.createSmb(
            id = "smb-1",
            displayName = "nas",
            server = "192.168.1.2",
            share = "foto",
            path = "/",
            domain = "WORKGROUP",
            username = "admin"
        )
        assertEquals(PhotoSourceType.SMB, config.type)
        assertTrue(config.isEnabled)
        assertTrue(config.config is SourceConfig.SmbConfig)
        assertEquals("192.168.1.2", (config.config as SourceConfig.SmbConfig).server)
    }

    @Test
    fun `createLocal wires LOCAL type and folder list`() {
        val uris = listOf("content://foo", "content://bar")
        val config = PhotoSourceConfig.createLocal(
            id = "local-1",
            displayName = "Local",
            folderUris = uris
        )
        assertEquals(PhotoSourceType.LOCAL, config.type)
        assertTrue(config.config is SourceConfig.LocalConfig)
        assertEquals(uris, (config.config as SourceConfig.LocalConfig).folderUris)
    }

    @Test
    fun `isEnabled defaults to true, can be disabled`() {
        val disabled = PhotoSourceConfig.createLocal(
            id = "local-2",
            displayName = "off",
            folderUris = emptyList(),
            isEnabled = false
        )
        assertEquals(false, disabled.isEnabled)
    }

    @Test
    fun `createSample wires SAMPLE type and SampleConfig`() {
        val config = PhotoSourceConfig.createSample(
            id = "sample-1",
            displayName = "Sample Photos"
        )
        assertEquals(PhotoSourceType.SAMPLE, config.type)
        assertTrue(config.isEnabled)
        assertTrue(config.config is SourceConfig.SampleConfig)
    }
}
