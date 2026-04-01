package com.photoframe.core.logging

import android.content.Context
import com.photoframe.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent file-based diagnostic logger for the Photo Frame app.
 *
 * Writes structured events to a rotating log file on internal storage.
 * Survives app kills/crashes so diagnostics are available on next startup.
 *
 * Thread Safety: All writes are dispatched to IO dispatcher via coroutine.
 * The coroutine scope uses SupervisorJob so individual log failures don't
 * cancel subsequent writes. The scope lives as long as the process (singleton).
 */
@Singleton
class AppLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val logFile: File by lazy {
        File(context.filesDir, LOG_FILE_NAME)
    }

    /** Creates a new SimpleDateFormat per call — SDF is NOT thread-safe. */
    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    /**
     * Logs a diagnostic event to the persistent log file.
     * Non-blocking — dispatches write to IO.
     */
    fun log(event: String, details: String? = null) {
        val timestamp = now()
        val line = if (details != null) {
            "[$timestamp] $event | $details\n"
        } else {
            "[$timestamp] $event\n"
        }
        scope.launch {
            appendSafely(line)
        }
    }

    /**
     * Captures remaining logcat from the previous session.
     * Should be called once on app startup.
     *
     * Reads logcat line-by-line to avoid loading the full buffer into memory.
     * Uses ProcessBuilder with redirectErrorStream to prevent stderr deadlock.
     */
    fun capturePreviousLogcat() {
        scope.launch {
            try {
                val separator = "\n--- LOGCAT CAPTURE (${now()}) ---\n"
                appendSafely(separator)

                val process = ProcessBuilder("logcat", "-d", "-t", "5000", "-v", "threadtime")
                    .redirectErrorStream(true)
                    .start()

                // Filter line-by-line to avoid holding full logcat in memory
                val sb = StringBuilder()
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.filter { line -> isRelevantLogLine(line) }
                        .forEach { line ->
                            // Sanitize SMB URLs to avoid leaking credentials
                            sb.appendLine(sanitize(line))
                        }
                }

                val completed = process.waitFor(10, TimeUnit.SECONDS)
                if (!completed) process.destroyForcibly()

                if (sb.isNotBlank()) {
                    appendSafely(sb.toString())
                } else {
                    appendSafely("[No relevant logcat entries found]\n")
                }

                appendSafely("--- END LOGCAT CAPTURE ---\n\n")
            } catch (e: Exception) {
                appendSafely("[Failed to capture logcat: ${e.message}]\n")
            }
        }
    }

    /**
     * Returns the log file for sharing/export.
     */
    fun logFile(): File = logFile

    /**
     * Reads the full log content. Should be called from IO thread.
     */
    fun readLog(): String {
        return try {
            if (logFile.exists()) logFile.readText() else ""
        } catch (e: Exception) {
            "[Error reading log: ${e.message}]"
        }
    }

    /**
     * Appends text to the log file, trimming only when well over max size.
     */
    private fun appendSafely(text: String) {
        try {
            logFile.appendText(text)
            // Only trim after exceeding the threshold — avoids read+rewrite on every append
            if (logFile.length() > MAX_LOG_SIZE_BYTES) {
                trimLog()
            }
        } catch (_: IOException) {
            // Fail silently — logging must never crash the app
        }
    }

    /**
     * Trims the log file by removing the oldest ~50%.
     * Called infrequently (only when file exceeds 2MB).
     * After trimming, the file is ~1MB, so the next trim won't happen
     * until another ~1MB of logs is written.
     */
    private fun trimLog() {
        try {
            val content = logFile.readText()
            val trimPoint = content.length / 2
            val newStart = content.indexOf('\n', trimPoint)
            if (newStart > 0) {
                val trimmed = "[... older entries trimmed ...]\n" + content.substring(newStart + 1)
                logFile.writeText(trimmed)
            }
        } catch (_: Exception) {
            // Fail silently
        }
    }

    private fun isRelevantLogLine(line: String): Boolean {
        return line.contains("photoframe", ignoreCase = true) ||
            line.contains("SlideshowVM", ignoreCase = true) ||
            line.contains("SlideshowWatchdog", ignoreCase = true) ||
            line.contains("ImageCache", ignoreCase = true) ||
            line.contains("NetworkMonitor", ignoreCase = true) ||
            line.contains("MemoryMonitor", ignoreCase = true) ||
            line.contains("PhotoFrame", ignoreCase = true) ||
            line.contains("AndroidRuntime", ignoreCase = true) ||
            line.contains("FATAL", ignoreCase = true) ||
            line.contains("lowmemorykiller", ignoreCase = true) ||
            line.contains("ActivityManager", ignoreCase = true)
    }

    /** Redact SMB URLs that may contain embedded credentials. */
    private fun sanitize(line: String): String {
        return line.replace(Regex("smb://[^@]*@"), "smb://***@")
    }

    companion object {
        private const val LOG_FILE_NAME = "photoframe-diagnostic.log"
        private const val MAX_LOG_SIZE_BYTES = 2 * 1024 * 1024L // 2MB
    }
}
