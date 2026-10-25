package com.nightwave.player.v2.library

/**
 * Presentation metadata only. Pinned playlists always appear first while the
 * ordering chosen by the user remains stable within pinned/unpinned groups.
 * Neither underlying playlist tracks nor saved playlist order are rewritten.
 */
object PlaylistPinPolicy {
    const val MAX_PINS = 50
    fun normalize(stored: List<String>, known: List<String>): List<String> {
        val allowed = known.toSet()
        return stored.asSequence().filter { it in allowed }.distinct()
            .take(MAX_PINS).toList()
    }

    fun toggle(current: List<String>, name: String, known: List<String>): List<String>? {
        if (name !in known) return null
        val before = normalize(current, known)
        return if (name in before) before - name
            else if (before.size >= MAX_PINS) null else before + name
    }

    fun <T> pinnedFirst(items: List<T>, pinned: List<String>, name: (T) -> String): List<T> {
        val set = pinned.toSet()
        return items.filter { name(it) in set } + items.filterNot { name(it) in set }
    }

    fun renamed(current: List<String>, oldName: String, updated: String): List<String> =
        current.map { if (it == oldName) updated else it }.distinct().take(MAX_PINS)

    fun deleted(current: List<String>, name: String): List<String> = current - name
}
