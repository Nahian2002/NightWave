package com.nightwave.player.offline

import android.content.Context
import java.util.UUID

/**
 * Tracks user-initiated Pause of a searched audio WorkRequest. NW136 retains
 * separately validated private byte checkpoints only for eligible servers.
 * No streaming URL, credentials, file paths or song titles are stored.
 */
object SearchOfflinePauseState {
    private const val PREFS = "nightwave_search_pause_v1"

    fun mark(context: Context, id: UUID): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(id.toString(), true).commit()

    fun isMarked(context: Context, id: UUID): Boolean = runCatching {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(id.toString(), false)
    }.getOrDefault(false)

    fun clear(context: Context, id: UUID): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(id.toString()).commit()
}
