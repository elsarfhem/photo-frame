package com.photoframe.tests.functional

import com.photoframe.core.model.PhotoSourceConfig
import com.photoframe.core.model.PhotoSourceType
import com.photoframe.core.model.SourceConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Functional tests for source-list scenarios driven by the slideshow
 * reload logic: mixed SMB + local, SMB-only offline gate, and
 * enable/disable filtering. These mirror the conditions the
 * SlideshowViewModel evaluates at startup and on source-change events.
 */
class SourceConfigFunctionalTest {

    private val smb = PhotoSourceConfig.createSmb(
        id = "smb-1",
        displayName = "nas",
        server = "192.168.1.2",
        share = "foto",
        path = "/",
        domain = "WORKGROUP",
        username = "admin"
    )

    private val local = PhotoSourceConfig.createLocal(
        id = "local-1",
        displayName = "Local",
        folderUris = listOf("content://com.android.externalstorage.documents/tree/primary%3APictures")
    )

    @Test
    fun `smb-only setup is detected for network gate`() {
        val sources = listOf(smb)
        val smbOnly = sources.isNotEmpty() && sources.all { it.type == PhotoSourceType.SMB }
        assertTrue(smbOnly)
    }

    @Test
    fun `mixed local+SMB setup skips network gate`() {
        val sources = listOf(smb, local)
        val smbOnly = sources.isNotEmpty() && sources.all { it.type == PhotoSourceType.SMB }
        assertFalse(smbOnly)
    }

    @Test
    fun `disabled sources excluded from enabled list`() {
        val disabledSmb = smb.copy(isEnabled = false)
        val enabled = listOf(disabledSmb, local).filter { it.isEnabled }
        assertEquals(1, enabled.size)
        assertEquals(PhotoSourceType.LOCAL, enabled.first().type)
    }

    @Test
    fun `signature detects enable flag change`() {
        fun signature(list: List<PhotoSourceConfig>) = list
            .sortedBy { it.id }
            .joinToString("|") { "${it.id}:${it.isEnabled}:${it.config.hashCode()}" }

        val before = listOf(smb, local)
        val after = listOf(smb.copy(isEnabled = false), local)
        assertFalse(signature(before) == signature(after))
    }

    @Test
    fun `signature stable when list unchanged`() {
        fun signature(list: List<PhotoSourceConfig>) = list
            .sortedBy { it.id }
            .joinToString("|") { "${it.id}:${it.isEnabled}:${it.config.hashCode()}" }

        val list1 = listOf(smb, local)
        val list2 = listOf(local, smb) // different order, same content
        assertEquals(signature(list1), signature(list2))
    }

    @Test
    fun `local config folder URIs preserved`() {
        val cfg = local.config as SourceConfig.LocalConfig
        assertEquals(1, cfg.folderUris.size)
        assertTrue(cfg.folderUris.first().startsWith("content://"))
    }
}
