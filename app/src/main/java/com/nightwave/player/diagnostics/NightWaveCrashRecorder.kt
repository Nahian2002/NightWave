package com.nightwave.player.diagnostics

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

/** Local private summary: exception types and NightWave stack frames ONLY. */
object NightWaveCrashRecorder {
    private const val PREFS = "nightwave_crash_diagnostics_v1"
    private const val KEY = "recent"
    private const val MAX_RECORDS = 5
    private val handling = AtomicBoolean(false)

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler() ?: return
        if (previous is NightWaveHandler) return
        Thread.setDefaultUncaughtExceptionHandler(
            NightWaveHandler(context.applicationContext, previous)
        )
    }

    private class NightWaveHandler(
        val context: Context,
        val previous: Thread.UncaughtExceptionHandler
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                if (handling.compareAndSet(false, true)) {
                    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    val previousText = prefs.getString(KEY, "").orEmpty()
                    val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                    val issue = generateSequence(throwable) { it.cause }
                        .take(3).joinToString(" / ") { it.javaClass.name.take(110) }
                    val appFrames = generateSequence(throwable) { it.cause }
                        .take(3).flatMap { it.stackTrace.asSequence() }
                        .filter { it.className.startsWith("com.nightwave.player.") }
                        .take(12).map { element ->
                            element.className.removePrefix("com.nightwave.player.").take(130) +
                                "." + element.methodName.take(70) +
                                ":" + element.lineNumber
                        }.joinToString("\n")
                    val record = (
                        stamp + " · " + thread.name.take(32) + "\n" + issue + "\n" + appFrames
                    ).take(4000)
                    val history = (listOf(record) +
                        previousText.split("\n---\n").filter { it.isNotBlank() })
                        .take(MAX_RECORDS)
                    prefs.edit().putString(KEY, history.joinToString("\n---\n")).commit()
                }
            } catch (_: Throwable) {
                // Never hide or replace the real Android crash.
            } finally {
                previous.uncaughtException(thread, throwable)
            }
        }
    }

    /** No exception message, URL, original file path, user data or tokens. */
    fun report(context: Context): String = try {
        val saved = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "").orEmpty()
        if (saved.isBlank()) "NightWave: no locally recorded fatal exceptions yet."
        else "NightWave local crash summary:\n\n" + saved
    } catch (_: Exception) {
        "NightWave: unable to read local crash summary."
    }
}
