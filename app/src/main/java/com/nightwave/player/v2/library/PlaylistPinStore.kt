package com.nightwave.player.v2.library

import android.content.Context
import org.json.JSONArray

/** A separate preference key: playlist data, ordering and folder assignments are untouched. */
class PlaylistPinStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_playlist_pins_v1", Context.MODE_PRIVATE)

    companion object {
        private val writeLock = Any()
        private const val KEY = "pins"
    }

    private fun decode(): List<String>? = runCatching {
        val raw = prefs.getString(KEY, null) ?: return@runCatching emptyList()
        require(raw.length <= 8_192)
        val array = JSONArray(raw)
        require(array.length() <= PlaylistPinPolicy.MAX_PINS)
        val values = (0 until array.length()).map { array.getString(it) }
        require(values.all { it.length in 1..60 && it == it.trim() &&
            it.none(Character::isISOControl) })
        require(values.distinct().size == values.size)
        values
    }.getOrNull()

    fun snapshot(known: List<String>): List<String> = synchronized(writeLock) {
        PlaylistPinPolicy.normalize(decode() ?: emptyList(), known)
    }

    private fun write(values: List<String>): Boolean = runCatching {
        val output = JSONArray()
        values.forEach { output.put(it) }
        prefs.edit().putString(KEY, output.toString()).commit()
    }.getOrDefault(false)

    /** Returns null on invalid playlist, capacity, corrupt prefs or failed commit. */
    fun toggle(name: String, known: List<String>): List<String>? = synchronized(writeLock) {
        val current = decode() ?: return@synchronized null
        val next = PlaylistPinPolicy.toggle(current, name, known) ?: return@synchronized null
        if (!write(next)) return@synchronized null
        next
    }

    fun playlistRenamed(old: String, updated: String): Boolean = synchronized(writeLock) {
        val current = decode() ?: return@synchronized false
        val next = PlaylistPinPolicy.renamed(current, old, updated)
        current == next || write(next)
    }

    fun playlistDeleted(name: String): Boolean = synchronized(writeLock) {
        val current = decode() ?: return@synchronized false
        val next = PlaylistPinPolicy.deleted(current, name)
        current == next || write(next)
    }
}
