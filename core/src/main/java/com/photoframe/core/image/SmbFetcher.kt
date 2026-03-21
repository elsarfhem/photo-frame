package com.photoframe.core.image

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.photoframe.core.model.Result
import com.photoframe.core.smb.SmbClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem

/**
 * Custom Coil Fetcher for loading images from SMB network shares.
 *
 * Integrates with SmbClient to fetch photo bytes from SMB shares.
 * Handles SMB URLs in the format: smb://server/share/path/photo.jpg
 *
 * Thread Safety: All methods are suspend functions running on ioDispatcher.
 *
 * @param smbClient SMB client for network operations
 * @param path SMB path to the image file
 * @param options Coil fetch options
 * @param ioDispatcher Coroutine dispatcher for I/O operations
 */
class SmbFetcher(
    private val smbClient: SmbClient,
    private val path: String,
    private val options: Options,
    private val ioDispatcher: CoroutineDispatcher
) : Fetcher {

    /**
     * Fetches the image from the SMB share.
     *
     * @return FetchResult with image data, or throws exception if fetch failed
     */
    override suspend fun fetch(): FetchResult = withContext(ioDispatcher) {
        android.util.Log.d("SmbFetcher", "fetch() called for path: $path")
        // Fetch bytes from SMB share
        val result = smbClient.readFile(path)

        when (result) {
            is Result.Success -> {
                android.util.Log.d("SmbFetcher", "Successfully read ${result.data.size} bytes from $path")
                // Convert bytes to Okio BufferedSource
                val buffer = Buffer().write(result.data)

                // Return as ImageSource for Coil to decode
                SourceFetchResult(
                    source = ImageSource(
                        source = buffer,
                        fileSystem = FileSystem.SYSTEM
                    ),
                    mimeType = getMimeTypeFromPath(path),
                    dataSource = DataSource.NETWORK
                )
            }
            is Result.Error -> {
                android.util.Log.e("SmbFetcher", "Failed to read from $path: ${result.message}", result.exception)
                throw result.exception
            }
            is Result.Loading -> {
                throw IllegalStateException("Unexpected loading state from SMB client")
            }
        }
    }

    /**
     * Determines MIME type from file extension.
     *
     * @param path File path
     * @return MIME type string
     */
    private fun getMimeTypeFromPath(path: String): String? {
        val extension = path.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "heic" -> "image/heic"
            else -> null
        }
    }

    /**
     * Factory for creating SmbFetcher instances.
     *
     * @param smbClient SMB client for network operations
     * @param ioDispatcher Coroutine dispatcher for I/O operations
     */
    class Factory(
        private val smbClient: SmbClient,
        private val ioDispatcher: CoroutineDispatcher
    ) : Fetcher.Factory<coil3.Uri> {

        /**
         * Creates a Fetcher if the data is an SMB URL.
         *
         * @param data Request data (URI)
         * @param options Coil fetch options
         * @param imageLoader ImageLoader instance
         * @return SmbFetcher if data is SMB URL, null otherwise
         */
        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            android.util.Log.d("SmbFetcher.Factory", "create() called with URI: $data (scheme: ${data.scheme})")
            // Only handle SMB URLs
            return if (data.scheme?.equals("smb", ignoreCase = true) == true) {
                val path = data.toString()
                android.util.Log.d("SmbFetcher.Factory", "Creating SmbFetcher for: $path")
                SmbFetcher(smbClient, path, options, ioDispatcher)
            } else {
                android.util.Log.d("SmbFetcher.Factory", "Not an SMB URL (scheme=${data.scheme}), returning null")
                null
            }
        }
    }
}
