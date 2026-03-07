package com.photoframe.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.photoframe.core.model.Result
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

/**
 * Decoder for RAW image formats.
 *
 * Supports:
 * - DNG: Android native decoder (API 24+)
 * - CR2, NEF, RW2, ARW: Extract embedded JPEG preview
 *
 * Strategy:
 * 1. DNG files → Use Android's DngDecoder
 * 2. Other RAW → Extract embedded JPEG thumbnail
 * 3. Fallback → Return error (auto-skip will handle)
 *
 * Note: This provides "good enough" RAW support without adding
 * large native libraries like libraw (~10MB).
 */
object RawImageDecoder {

    private const val TAG = "RawImageDecoder"

    /**
     * Decodes RAW image bytes to Bitmap.
     *
     * @param bytes RAW image bytes
     * @param extension File extension (e.g., "dng", "cr2", "nef")
     * @return Result with decoded Bitmap or error
     */
    suspend fun decode(bytes: ByteArray, extension: String): Result<Bitmap> {
        return try {
            when (extension.lowercase()) {
                "dng" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    decodeDng(bytes)
                } else {
                    extractEmbeddedJpeg(bytes, extension)
                }
                "cr2", "nef", "rw2", "arw" -> extractEmbeddedJpeg(bytes, extension)
                else -> Result.error(
                    UnsupportedOperationException("Unsupported RAW format: $extension"),
                    "RAW format $extension is not supported"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode RAW image: ${e.message}", e)
            Result.error(e, "Failed to decode RAW image: ${e.message}")
        }
    }

    /**
     * Decodes DNG file using Android's native decoder.
     * Only available on API 28+.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeDng(bytes: ByteArray): Result<Bitmap> {
        return try {
            // Android has native DNG support via ImageDecoder
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, source ->
                // Downsample if image is very large
                val maxSize = 2560
                if (info.size.width > maxSize || info.size.height > maxSize) {
                    val scale = maxOf(
                        info.size.width.toFloat() / maxSize,
                        info.size.height.toFloat() / maxSize
                    )
                    decoder.setTargetSampleSize(scale.toInt().coerceAtLeast(1))
                }
            }

            Log.d(TAG, "Successfully decoded DNG image (${bitmap.width}x${bitmap.height})")
            Result.success(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode DNG: ${e.message}", e)
            Result.error(e, "Failed to decode DNG: ${e.message}")
        }
    }

    /**
     * Extracts embedded JPEG preview from RAW file.
     *
     * Most RAW files contain an embedded JPEG preview that we can extract.
     * This is much faster than full RAW processing and doesn't require libraw.
     */
    private fun extractEmbeddedJpeg(bytes: ByteArray, extension: String): Result<Bitmap> {
        try {
            // Look for JPEG markers in the RAW file
            // JPEG starts with FF D8 and ends with FF D9
            val jpegStart = findJpegStart(bytes)
            val jpegEnd = findJpegEnd(bytes, jpegStart)

            if (jpegStart >= 0 && jpegEnd > jpegStart) {
                // Extract JPEG bytes
                val jpegBytes = bytes.copyOfRange(jpegStart, jpegEnd + 2)

                // Decode JPEG
                val options = BitmapFactory.Options().apply {
                    // Downsample if needed
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)

                // Calculate sample size
                val maxSize = 2560
                options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
                options.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)

                if (bitmap != null) {
                    Log.d(TAG, "Extracted JPEG preview from $extension (${bitmap.width}x${bitmap.height})")
                    return Result.success(bitmap)
                }
            }

            Log.w(TAG, "No JPEG preview found in $extension file")
            return Result.error(
                IllegalStateException("No JPEG preview found"),
                "RAW file does not contain embedded JPEG preview"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract JPEG from $extension: ${e.message}", e)
            return Result.error(e, "Failed to extract preview: ${e.message}")
        }
    }

    /**
     * Finds the start of JPEG data (FF D8 marker).
     */
    private fun findJpegStart(bytes: ByteArray): Int {
        for (i in 0 until bytes.size - 1) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD8.toByte()) {
                return i
            }
        }
        return -1
    }

    /**
     * Finds the end of JPEG data (FF D9 marker).
     */
    private fun findJpegEnd(bytes: ByteArray, startPos: Int): Int {
        for (i in startPos until bytes.size - 1) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD9.toByte()) {
                return i
            }
        }
        return -1
    }

    /**
     * Calculates sample size for downsampling large images.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Checks if file extension is a RAW format.
     */
    fun isRawFormat(extension: String): Boolean {
        return extension.lowercase() in setOf("dng", "cr2", "nef", "rw2", "arw")
    }
}
