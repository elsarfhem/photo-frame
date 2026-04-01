package com.photoframe.core.logging

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.photoframe.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the export file (state snapshot + event log) and creates a share intent.
 *
 * Thread Safety: Safe to call from main thread — IO work is dispatched.
 */
@Singleton
class LogExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLogger: AppLogger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val exportDir: File by lazy {
        File(context.cacheDir, "log-exports").apply { mkdirs() }
    }

    /**
     * Builds the full export file with device info, app state, and event log.
     * Returns a share Intent ready to launch.
     */
    suspend fun buildShareIntent(): Intent = withContext(ioDispatcher) {
        val exportFile = buildExportFile()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Photo Frame Logs - ${formatDate()}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun buildExportFile(): File {
        // Clean up previous exports
        exportDir.listFiles()?.forEach { it.delete() }

        val exportFile = File(exportDir, "photoframe-export-${formatDate()}.txt")

        val sb = StringBuilder()

        // Device info section
        sb.appendLine("== Device Info ==")
        sb.appendLine("Model: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine("App: ${packageInfo.versionName} (build ${packageInfo.longVersionCode})")
        } catch (_: Exception) {
            sb.appendLine("App: unknown version")
        }
        sb.appendLine()

        // Memory section
        sb.appendLine("== Memory ==")
        val runtime = Runtime.getRuntime()
        val maxMB = runtime.maxMemory() / (1024 * 1024)
        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        sb.appendLine("Heap: ${usedMB}MB / ${maxMB}MB")
        sb.appendLine()

        // Log content
        sb.appendLine("== Diagnostic Event Log ==")
        sb.appendLine(appLogger.readLog())

        exportFile.writeText(sb.toString())
        return exportFile
    }

    private fun formatDate(): String {
        return SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    }
}
