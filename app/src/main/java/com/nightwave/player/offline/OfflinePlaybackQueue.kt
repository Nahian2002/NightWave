package com.nightwave.player.offline

/**
 * Ephemeral queue of app-private audio IDs. Never stores file paths, content URIs,
 * streaming signatures or user credentials. Selection is deliberately bounded.
 *
 * This is separate from the saved public-video queue and must not be persisted
 * into QueueStateStore or written into YouTube playlists.
 */
class OfflinePlaybackQueue {
    private val ids = mutableListOf<String>()
    var index: Int = -1
        private set

    val current: String? get() = ids.getOrNull(index)
    val size: Int get() = ids.size

    fun select(candidates: List<String>, selectedId: String): String? {
        val clean = candidates.filter { it.isNotBlank() }.distinct().take(MAX_ITEMS)
        val selected = clean.indexOf(selectedId)
        if (selected < 0) return null
        ids.clear()
        ids.addAll(clean)
        index = selected
        return current
    }

    fun next(): String? {
        if (index < 0 || index >= ids.lastIndex) return null
        index += 1
        return current
    }

    fun previous(): String? {
        if (index <= 0) return null
        index -= 1
        return current
    }

    fun snapshot(): List<String> = ids.toList()

    fun jump(to: Int): String? {
        if (to !in ids.indices) return null
        index = to
        return current
    }

    fun reset() {
        ids.clear()
        index = -1
    }

    companion object { const val MAX_ITEMS = 400 }
}
