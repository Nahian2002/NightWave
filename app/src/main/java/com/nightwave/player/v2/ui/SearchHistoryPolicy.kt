package com.nightwave.player.v2.ui

import java.util.Locale

/**
 * Private, on-device history of explicit search submissions only.
 * Typing, library suggestions and playback never enter the history.
 */
internal object SearchHistoryPolicy {
    const val MAX_ENTRIES = 12
    private const val MAX_QUERY_CHARS = 160

    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isISOControl() }) return null
        return trimmed.replace(Regex("\\s+"), " ").take(MAX_QUERY_CHARS)
            .takeIf { it.isNotBlank() }
    }

    private fun key(query: String) = query.lowercase(Locale.ROOT)

    fun sanitize(stored: List<String>): List<String> {
        val seen = HashSet<String>()
        return stored.take(100).mapNotNull(::normalize)
            .filter { seen.add(key(it)) }.take(MAX_ENTRIES)
    }

    fun record(current: List<String>, raw: String): List<String> {
        val normalized = normalize(raw) ?: return sanitize(current)
        return (listOf(normalized) + sanitize(current)
            .filterNot { key(it) == key(normalized) }).take(MAX_ENTRIES)
    }

    fun remove(current: List<String>, raw: String): List<String> {
        val normalized = normalize(raw) ?: return sanitize(current)
        return sanitize(current).filterNot { key(it) == key(normalized) }
    }
}
