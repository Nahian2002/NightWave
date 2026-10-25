package com.nightwave.player.v2.ui

import android.content.Context
import org.json.JSONArray

/**
 * Explicit searches live only on this device, independently of playlists,
 * downloads, Drive backup and listening history. No tracking or network calls.
 */
internal class SearchHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nightwave_search_history_v1", Context.MODE_PRIVATE)
    private val key = "submitted_queries"

    fun read(): List<String> = runCatching {
        val serialized = preferences.getString(key, "[]") ?: "[]"
        // Guard against oversized/corrupt preferences without crashing Search.
        if (serialized.length > 8_192) return@runCatching emptyList()
        val json = JSONArray(serialized)
        SearchHistoryPolicy.sanitize((0 until minOf(json.length(), 100))
            .mapNotNull { json.optString(it, "").takeIf(String::isNotBlank) })
    }.getOrDefault(emptyList())

    private fun write(entries: List<String>): Boolean = runCatching {
        val array = JSONArray()
        SearchHistoryPolicy.sanitize(entries).forEach(array::put)
        preferences.edit().putString(key, array.toString()).commit()
    }.getOrDefault(false)

    fun record(query: String): List<String> {
        val current = read()
        val next = SearchHistoryPolicy.record(current, query)
        return if (next == current || write(next)) next else current
    }

    fun remove(query: String): List<String> {
        val current = read()
        val next = SearchHistoryPolicy.remove(current, query)
        return if (next == current || write(next)) next else current
    }

    fun clear(): List<String> {
        val current = read()
        return if (current.isEmpty() || write(emptyList())) emptyList() else current
    }

    /** Undo only if no intervening search changed the same preference. */
    fun restoreIfUnchanged(expected: List<String>, before: List<String>): List<String> {
        val current = read()
        return if (current == expected && write(before)) SearchHistoryPolicy.sanitize(before)
        else current
    }
}
