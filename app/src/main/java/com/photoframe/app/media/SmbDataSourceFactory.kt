package com.photoframe.app.media

import android.content.Context
import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Result
import com.photoframe.core.smb.SmbClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer DataSource.Factory for SMB protocol support.
 *
 * Each DataSource streams the video file from SMB to a temp file on disk.
 * Subsequent ExoPlayer seek operations (close/open with DataSpec.position) are
 * served from the cached temp file without re-downloading.
 *
 * Memory usage: Only a small read buffer (~64KB), regardless of video file size.
 * Previous implementation loaded the entire video into a ByteArray, which caused
 * OOM crashes and process kills for large videos (>50MB).
 *
 * @param smbClient SMB client for network file access
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 * @param context Application context for cache directory access
 */
@Singleton
class SmbDataSourceFactory @Inject constructor(
    private val smbClient: SmbClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) : DataSource.Factory {

    private val cacheDir: File by lazy {
        File(context.cacheDir, "smb_video_cache").also { it.mkdirs() }
    }

    override fun createDataSource(): DataSource {
        return SmbDataSource(smbClient, ioDispatcher, cacheDir)
    }
}

/**
 * ExoPlayer DataSource that reads video bytes from SMB shares via temp files.
 *
 * Streams the file from SMB to a temp file on disk after the first download so that
 * ExoPlayer's repeated close()/open() cycles for seeking don't trigger
 * redundant SMB reads. Supports DataSpec.position for seek offsets.
 *
 * Memory: Only a read buffer (~8KB) is held in memory. The video content stays on disk.
 *
 * Thread Safety: Not thread-safe. ExoPlayer creates one instance per playback.
 */
private class SmbDataSource(
    private val smbClient: SmbClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val cacheDir: File
) : DataSource {

    private var uri: android.net.Uri? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    /** Cached temp file — persists across close()/open() cycles for the same URI. */
    private var cachedFile: File? = null
    private var cachedUri: String? = null
    private var randomAccessFile: RandomAccessFile? = null

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val path = uri.toString()
        val position = dataSpec.position

        // Reuse cached temp file if we already downloaded this file
        val existingFile = cachedFile
        if (existingFile != null && existingFile.exists() && cachedUri == path) {
            val fileLength = existingFile.length()
            val raf = RandomAccessFile(existingFile, "r")
            raf.seek(position)
            randomAccessFile = raf
            bytesRemaining = fileLength - position
            opened = true
            Log.d(TAG, "open: Serving from cache file (pos=$position, remaining=$bytesRemaining)")
            return bytesRemaining
        }

        Log.d(TAG, "open: Downloading SMB video to temp file: $path")

        // Create temp file for this video
        val tempFile = File.createTempFile("smb_video_", ".tmp", cacheDir)

        val result = runBlocking(ioDispatcher) {
            try {
                withTimeout(OPEN_TIMEOUT_MS) {
                    if (!smbClient.isConnected()) {
                        Log.w(TAG, "open: SmbClient not connected")
                    }
                    smbClient.readFileToFile(path, tempFile)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "open: Timeout reading SMB video after ${OPEN_TIMEOUT_MS}ms")
                tempFile.delete()
                Result.error(e, "SMB video read timed out")
            }
        }

        when (result) {
            is Result.Success -> {
                cachedFile = tempFile
                cachedUri = path
                val fileLength = tempFile.length()
                val raf = RandomAccessFile(tempFile, "r")
                raf.seek(position)
                randomAccessFile = raf
                bytesRemaining = fileLength - position
                opened = true
                Log.d(TAG, "open: Downloaded $fileLength bytes to temp file, serving from pos=$position")
                return bytesRemaining
            }
            is Result.Error -> {
                tempFile.delete()
                Log.e(TAG, "open: Failed to read SMB video: ${result.message}")
                throw java.io.IOException("Failed to read SMB video: ${result.message}", result.exception)
            }
            is Result.Loading -> {
                tempFile.delete()
                throw java.io.IOException("Unexpected result state from SMB client")
            }
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return androidx.media3.common.C.RESULT_END_OF_INPUT

        val raf = randomAccessFile ?: return androidx.media3.common.C.RESULT_END_OF_INPUT
        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()

        val bytesRead = raf.read(buffer, offset, bytesToRead)
        if (bytesRead == -1) return androidx.media3.common.C.RESULT_END_OF_INPUT

        bytesRemaining -= bytesRead
        return bytesRead
    }

    override fun getUri(): android.net.Uri? = uri

    override fun close() {
        // Close the RandomAccessFile but keep the temp file for reuse on next open()
        try {
            randomAccessFile?.close()
        } catch (e: Exception) {
            Log.w(TAG, "close: Error closing RandomAccessFile", e)
        }
        randomAccessFile = null
        bytesRemaining = 0
        opened = false
    }

    /** Cleans up the temp file. Called implicitly when this DataSource is garbage collected. */
    protected fun finalize() {
        try {
            randomAccessFile?.close()
            cachedFile?.delete()
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    companion object {
        private const val TAG = "SmbDataSource"
        /** Max time for the initial SMB download. Generous for large video files. */
        private const val OPEN_TIMEOUT_MS = 60_000L
    }
}
