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
     * Most RAW files contain multiple embedded JPEGs (thumbnail, preview, full-size preview).
     * This finds the largest one for best quality display.
     */
    private fun extractEmbeddedJpeg(bytes: ByteArray, extension: String): Result<Bitmap> {
        try {
            // Find ALL embedded JPEGs in the RAW file
            val jpegRegions = findAllJpegs(bytes)

            if (jpegRegions.isEmpty()) {
                Log.w(TAG, "No JPEG preview found in $extension file")
                return Result.error(
                    IllegalStateException("No JPEG preview found"),
                    "RAW file does not contain embedded JPEG preview"
                )
            }

            // Try each JPEG, starting with the largest
            for (region in jpegRegions.sortedByDescending { it.size }) {
                try {
                    val jpegBytes = bytes.copyOfRange(region.start, region.end + 2)

                    // Check if this JPEG is decodable and reasonably sized
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)

                    // Skip tiny thumbnails (< 200px on either dimension)
                    if (options.outWidth < 200 || options.outHeight < 200) {
                        Log.d(TAG, "Skipping tiny thumbnail (${options.outWidth}x${options.outHeight})")
                        continue
                    }

                    // Decode the JPEG
                    val maxSize = 2560
                    options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
                    options.inJustDecodeBounds = false

                    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options)

                    if (bitmap != null) {
                        Log.d(TAG, "Extracted JPEG preview from $extension (${bitmap.width}x${bitmap.height})")
                        return Result.success(bitmap)
                    }
                } catch (e: Exception) {
                    // Try next JPEG region
                    Log.d(TAG, "Failed to decode JPEG region, trying next: ${e.message}")
                    continue
                }
            }

            Log.w(TAG, "No usable JPEG preview found in $extension file")
            return Result.error(
                IllegalStateException("No usable JPEG preview found"),
                "RAW file previews are too small or corrupted"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract JPEG from $extension: ${e.message}", e)
            return Result.error(e, "Failed to extract preview: ${e.message}")
        }
    }

    /**
     * Data class representing a JPEG region within a RAW file.
     */
    private data class JpegRegion(val start: Int, val end: Int) {
        val size: Int get() = end - start
    }

    /**
     * Finds ALL embedded JPEGs in the RAW file.
     * RAW files typically contain multiple JPEGs (thumbnail, preview, full preview).
     */
    private fun findAllJpegs(bytes: ByteArray): List<JpegRegion> {
        val regions = mutableListOf<JpegRegion>()
        var searchPos = 0
        var jpegStartsFound = 0

        while (searchPos < bytes.size - 1) {
            // Find next JPEG start marker (FF D8)
            val start = findJpegStart(bytes, searchPos)
            if (start < 0) break

            jpegStartsFound++

            // Find corresponding end marker (FF D9)
            val end = findJpegEnd(bytes, start)
            if (end > start) {
                val size = end - start + 2
                Log.d(TAG, "Found JPEG region at offset $start, size: ${size / 1024}KB")
                regions.add(JpegRegion(start, end))
                searchPos = end + 2
            } else {
                // No valid end found, move past this start marker
                Log.d(TAG, "Found JPEG start at $start but no valid end marker")
                searchPos = start + 2
            }
        }

        Log.d(TAG, "Found ${regions.size} complete JPEG regions out of $jpegStartsFound JPEG starts (file size: ${bytes.size / 1024}KB)")
        return regions
    }

    /**
     * Finds the start of JPEG data (FF D8 marker) starting from given position.
     */
    private fun findJpegStart(bytes: ByteArray, startPos: Int = 0): Int {
        for (i in startPos until bytes.size - 1) {
            if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD8.toByte()) {
                return i
            }
        }
        return -1
    }

    /**
     * Finds the end of JPEG data (FF D9 marker) starting from given position.
     * Also enforces maximum search distance to avoid false positives.
     */
    private fun findJpegEnd(bytes: ByteArray, startPos: Int): Int {
        // Limit search to 50MB to avoid scanning entire RAW file for corrupted JPEGs
        val maxSearchDistance = 50 * 1024 * 1024
        val searchLimit = minOf(startPos + maxSearchDistance, bytes.size - 1)

        for (i in startPos until searchLimit) {
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
