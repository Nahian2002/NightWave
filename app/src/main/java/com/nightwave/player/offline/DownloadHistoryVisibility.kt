package com.nightwave.player.offline

import android.content.Context
import java.util.UUID

/**
 * Presentation only: hide old completed WorkManager cards without cancelling
 * any job or deleting app-private music, library rows, or resume metadata.
 */
object DownloadHistoryVisibility {
    private const val PREFS = "nightwave_downloads_hidden_v1"
    private const val KEY = "hidden"
    private const val MAX_HIDDEN = 200

    fun hiddenIds(context: Context): Set<String> =
        runCatching {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY, emptySet()).orEmpty().toSet()
        }.getOrDefault(emptySet())

    fun hide(context: Context, workId: UUID): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = hiddenIds(context)
        if (workId.toString() in current) return true
        // Fail safely at the cap rather than silently forgetting preferences.
        if (current.size >= MAX_HIDDEN) return false
        return prefs.edit().putStringSet(KEY, current + workId.toString()).commit()
    }

    fun restore(context: Context, workId: UUID): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.edit().putStringSet(KEY,
            hiddenIds(context) - workId.toString()).commit()
    }
}
