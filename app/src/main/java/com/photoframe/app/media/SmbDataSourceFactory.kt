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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer DataSource.Factory for SMB protocol support.
 *
 * Mirrors the SmbFetcher pattern used by Coil for image loading.
 * Enables ExoPlayer to load videos directly from SMB network shares.
 *
 * Architecture:
 * - Factory creates SmbDataSource instances on-demand
 * - Each DataSource loads entire video file into memory (suitable for small videos)
 * - Uses existing SmbClient for SMB protocol handling
 *
 * Limitations:
 * - Loads entire file into memory (not suitable for videos >100MB)
 * - No seek support optimization (full file read on every open)
 * - Consider chunked streaming for large videos in future
 *
 * Thread Safety: Factory is thread-safe. DataSource instances are single-use.
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
 * Implementation Notes:
 * - Loads entire video file on open() call
 * - Supports sequential read() operations for ExoPlayer
 * - Returns C.RESULT_END_OF_INPUT when all bytes consumed
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
    private var dataBytes: ByteArray? = null
    private var readPosition: Int = 0

    override fun addTransferListener(transferListener: TransferListener) {
        // TransferListener not needed for our simple implementation
        // Could be used for tracking download progress in future
    }

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val path = uri.toString()

        Log.d(TAG, "open: Opening SMB video: $path")

        // Ensure SMB client is connected before reading
        // Note: ExoPlayer calls open() on background thread, so blocking is acceptable
        val result = runBlocking(ioDispatcher) {
            // Check if connected, if not the connection should already be established
            // by the repository during initialization
            if (!smbClient.isConnected()) {
                Log.w(TAG, "open: SmbClient not connected - connection should be established by repository")
            }
            smbClient.readFile(path)
        }

        when (result) {
            is Result.Success -> {
                dataBytes = result.data
                readPosition = 0
                bytesRemaining = dataBytes!!.size.toLong()
                opened = true
                Log.d(TAG, "open: Successfully loaded ${bytesRemaining} bytes")
                return bytesRemaining
            }
            is Result.Error -> {
                Log.e(TAG, "open: Failed to read SMB video: ${result.message}", result.exception)
                throw java.io.IOException("Failed to read SMB video: ${result.message}", result.exception)
            }
            is Result.Loading -> {
                Log.e(TAG, "open: Unexpected loading state from SMB client")
                throw java.io.IOException("Unexpected result state from SMB client")
            }
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return androidx.media3.common.C.RESULT_END_OF_INPUT

        val bytes = dataBytes ?: return androidx.media3.common.C.RESULT_END_OF_INPUT
        val bytesToRead = minOf(length.toLong(), bytesRemaining).toInt()

        System.arraycopy(bytes, readPosition, buffer, offset, bytesToRead)
        readPosition += bytesToRead
        bytesRemaining -= bytesToRead

        return bytesToRead
    }

    override fun getUri(): android.net.Uri? = uri

    override fun close() {
        uri = null
        dataBytes = null
        readPosition = 0
        bytesRemaining = 0
        opened = false
        Log.d(TAG, "close: DataSource closed")
    }

    companion object {
        private const val TAG = "SmbDataSource"
    }
}
