package com.photoframe.app.media

import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.photoframe.core.di.IoDispatcher
import com.photoframe.core.model.Result
import com.photoframe.core.smb.SmbClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer DataSource.Factory for SMB protocol support.
 *
 * Each DataSource downloads the video file once from SMB and caches it in memory.
 * Subsequent ExoPlayer seek operations (close/open with DataSpec.position) are
 * served from the cache without re-downloading.
 *
 * Limitations:
 * - Loads entire file into memory (not suitable for videos >100MB)
 *
 * @param smbClient SMB client for network file access
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
@Singleton
class SmbDataSourceFactory @Inject constructor(
    private val smbClient: SmbClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return SmbDataSource(smbClient, ioDispatcher)
    }
}

/**
 * ExoPlayer DataSource that reads video bytes from SMB shares.
 *
 * Caches the entire file in memory after the first download so that
 * ExoPlayer's repeated close()/open() cycles for seeking don't trigger
 * redundant SMB reads. Supports DataSpec.position for seek offsets.
 *
 * Thread Safety: Not thread-safe. ExoPlayer creates one instance per playback.
 */
private class SmbDataSource(
    private val smbClient: SmbClient,
    private val ioDispatcher: CoroutineDispatcher
) : DataSource {

    private var uri: android.net.Uri? = null
    private var bytesRemaining: Long = 0
    private var opened = false
    private var readPosition: Int = 0

    /** Cached file bytes — persists across close()/open() cycles for the same URI. */
    private var cachedBytes: ByteArray? = null
    private var cachedUri: String? = null

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val path = uri.toString()
        val position = dataSpec.position.toInt()

        // Reuse cached bytes if we already downloaded this file
        val bytes = cachedBytes
        if (bytes != null && cachedUri == path) {
            readPosition = position
            bytesRemaining = (bytes.size - position).toLong()
            opened = true
            Log.d(TAG, "open: Serving from cache (pos=$position, remaining=$bytesRemaining)")
            return bytesRemaining
        }

        Log.d(TAG, "open: Downloading SMB video: $path")

        val result = runBlocking(ioDispatcher) {
            try {
                withTimeout(OPEN_TIMEOUT_MS) {
                    if (!smbClient.isConnected()) {
                        Log.w(TAG, "open: SmbClient not connected")
                    }
                    smbClient.readFile(path)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "open: Timeout reading SMB video after ${OPEN_TIMEOUT_MS}ms")
                Result.error(e, "SMB video read timed out")
            }
        }

        when (result) {
            is Result.Success -> {
                cachedBytes = result.data
                cachedUri = path
                readPosition = position
                bytesRemaining = (result.data.size - position).toLong()
                opened = true
                Log.d(TAG, "open: Downloaded ${result.data.size} bytes, serving from pos=$position")
                return bytesRemaining
            }
            is Result.Error -> {
                Log.e(TAG, "open: Failed to read SMB video: ${result.message}")
                throw java.io.IOException("Failed to read SMB video: ${result.message}", result.exception)
            }
            is Result.Loading -> {
                throw java.io.IOException("Unexpected result state from SMB client")
            }
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return androidx.media3.common.C.RESULT_END_OF_INPUT

        val bytes = cachedBytes ?: return androidx.media3.common.C.RESULT_END_OF_INPUT
        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()

        System.arraycopy(bytes, readPosition, buffer, offset, bytesToRead)
        readPosition += bytesToRead
        bytesRemaining -= bytesToRead

        return bytesToRead
    }

    override fun getUri(): android.net.Uri? = uri

    override fun close() {
        // Keep cachedBytes/cachedUri so the next open() for the same video is instant
        readPosition = 0
        bytesRemaining = 0
        opened = false
    }

    companion object {
        private const val TAG = "SmbDataSource"
        /** Max time for the initial SMB download. Generous for large video files. */
        private const val OPEN_TIMEOUT_MS = 30_000L
    }
}
