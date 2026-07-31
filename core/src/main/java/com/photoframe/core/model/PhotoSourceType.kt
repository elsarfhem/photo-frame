package com.photoframe.core.model

/**
 * Type of photo source.
 *
 * Defines the different source types supported by the photo frame app.
 * Each type has a unique identifier and display name for UI presentation.
 *
 * Thread Safety: Enum, immutable, safe to share across threads.
 *
 * @property displayName Human-readable name for UI
 */
enum class PhotoSourceType(val displayName: String) {
    /**
     * SMB/Samba network share source.
     * Requires network connection and credentials.
     */
    SMB("Network Share (SMB)"),

    /**
     * Local device storage source.
     * Uses MediaStore API with scoped storage.
     */
    LOCAL("Local Storage"),

    /**
     * Sample/demo source.
     * Bundled sample photos for demo/testing without real network setup.
     */
    SAMPLE("Sample Photos")

    // Future: GOOGLE_DRIVE, DROPBOX, FTP, etc.
}
