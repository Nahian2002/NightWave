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

    /** Null indicates corrupted/unreadable storage; writes must fail closed. */
    private fun decode(): List<String>? = runCatching {
        val serialized = preferences.getString(key, null) ?: return@runCatching emptyList()
        // A malformed list is never silently replaced with an empty list.
        require(serialized.length <= 8_192) { "Search history is oversized" }
        val json = JSONArray(serialized)
        require(json.length() <= SearchHistoryPolicy.MAX_ENTRIES)
        val stored = (0 until json.length()).map { index ->
            // optString() silently repairs corrupted entries, so demand real strings.
            require(json.get(index) is String) { "Invalid search entry" }
            json.getString(index)
        }
        require(SearchHistoryPolicy.sanitize(stored) == stored) {
            "Search history needs explicit recovery"
        }
        stored
    }.getOrNull()

    fun read(): List<String> = decode() ?: emptyList()

    private fun write(entries: List<String>): Boolean = runCatching {
        val array = JSONArray()
        SearchHistoryPolicy.sanitize(entries).forEach { array.put(it) }
        preferences.edit().putString(key, array.toString()).commit()
    }.getOrDefault(false)

    fun record(query: String): List<String> {
        val current = decode() ?: return emptyList()
        val next = SearchHistoryPolicy.record(current, query)
        return if (next == current || write(next)) next else current
    }

    fun remove(query: String): List<String> {
        val current = decode() ?: return emptyList()
        val next = SearchHistoryPolicy.remove(current, query)
        return if (next == current || write(next)) next else current
    }

    fun clear(): List<String> {
        val current = decode() ?: return emptyList()
        return if (current.isEmpty() || write(emptyList())) emptyList() else current
    }

    /** Undo only if no intervening search changed the same preference. */
    fun restoreIfUnchanged(expected: List<String>, before: List<String>): List<String> {
        val current = decode() ?: return emptyList()
        return if (current == expected && write(before)) SearchHistoryPolicy.sanitize(before)
        else current
    }
}
