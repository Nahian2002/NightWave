package com.nightwave.player.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Read-only Android process-exit reasons (API 30+).
 * Avoid getDescription(), traceInputStream, process name, UIDs and memory paths:
 * descriptions/traces can contain private application and device information.
 */
object NightWaveExitHistory {
    private const val MAX_EXITS = 5

    fun report(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "Android process exit history requires Android 11 or newer."
        }
        return try {
            val manager = context.applicationContext
                .getSystemService(ActivityManager::class.java)
                ?: return "Android process exit history is unavailable on this device."
            val exits = manager.getHistoricalProcessExitReasons(
                context.packageName, 0, MAX_EXITS
            )
            if (exits.isEmpty()) return "Android reports no recent NightWave process exits."
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            "Android NightWave process exits (latest first, up to 5):\n" +
                exits.take(MAX_EXITS).joinToString("\n") { entry ->
                    formatter.format(Date(entry.timestamp)) + " · " +
                        reasonLabel(entry.reason)
                }
        } catch (_: Exception) {
            "Android process exit history could not be accessed. No device data was modified."
        }
    }

    private fun reasonLabel(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH -> "Java/Kotlin crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash"
        ApplicationExitInfo.REASON_ANR -> "App not responding (ANR)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "System low-memory termination"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "Startup initialization failure"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Excessive resource usage"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "App dependency terminated"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "User-requested stop"
        ApplicationExitInfo.REASON_EXIT_SELF -> "Process exited itself"
        ApplicationExitInfo.REASON_SIGNALED -> "Process received a signal"
        else -> "Other or unknown process exit"
    }
}
