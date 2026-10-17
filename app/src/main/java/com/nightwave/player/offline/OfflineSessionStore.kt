package com.nightwave.player.offline

import android.content.Context

/**
 * Stores an optional, explicit-resume checkpoint for imported audio.
 * Never writes into the public YouTube queue or modifies the offline file index.
 * Malformed snapshots are ignored, not destroyed on read.
 */
class OfflineSessionStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("nightwave_offline_session_v1", Context.MODE_PRIVATE)

    fun read(): OfflineSession? {
        val saved = OfflineSessionCodec.decode(
            runCatching { prefs.getString("snapshot", null) }.getOrNull()
        ) ?: return null
        val id = runCatching { prefs.getString("position_id", null) }.getOrNull()
        val position = runCatching { prefs.getLong("position_ms", saved.positionMs) }
            .getOrDefault(saved.positionMs)
        return if (id == saved.selectedId && position in 0L..OfflineSessionCodec.MAX_POSITION_MS)
            saved.copy(positionMs = position) else saved
    }

    /** Synchronous checkpoint at track selection / transition. Never erases imported audio. */
    @Synchronized
    fun saveSelection(ids: List<String>, selectedId: String, positionMs: Long = 0L): Boolean {
        val raw = runCatching {
            OfflineSessionCodec.encode(OfflineSession(ids, selectedId, positionMs))
        }.getOrNull() ?: return false
        return prefs.edit().putString("snapshot", raw)
            .putString("position_id", selectedId).putLong("position_ms", positionMs)
            .commit()
    }

    /** Bounded seek checkpoint; flush synchronously on pause / service destroy. */
    @Synchronized
    fun savePosition(id: String, positionMs: Long, flush: Boolean = false): Boolean {
        if (positionMs !in 0L..OfflineSessionCodec.MAX_POSITION_MS ||
            read()?.selectedId != id) return false
        val update = prefs.edit().putString("position_id", id)
            .putLong("position_ms", positionMs)
        return if (flush) update.commit() else { update.apply(); true }
    }
}
