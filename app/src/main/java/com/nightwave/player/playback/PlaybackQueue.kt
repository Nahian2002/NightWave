package com.nightwave.player.playback

import kotlin.random.Random

data class QueueEntry(val url: String, val title: String, val creator: String, val artwork: String)

/** A URL-only queue. Streams are resolved just-in-time so signed links aren't stored or reused. */
class PlaybackQueue(private val pick: (Int) -> Int = { Random.nextInt(it) }) {
    enum class Repeat { OFF, ALL, ONE }
    private val entries = mutableListOf<QueueEntry>()
    private val visited = ArrayDeque<Int>()
    private val shuffledSeen = mutableSetOf<Int>()
    var index: Int = -1
        private set
    var shuffle: Boolean = false
    var repeat: Repeat = Repeat.OFF
    val size: Int get() = entries.size
    val current: QueueEntry? get() = entries.getOrNull(index)
    fun snapshot(): List<QueueEntry> = entries.toList()

    fun replace(items: List<QueueEntry>, selected: Int): QueueEntry? {
        entries.clear()
        entries.addAll(items.distinctBy { it.url }.take(100))
        visited.clear()
        shuffledSeen.clear()
        index = if (entries.isEmpty()) -1 else selected.coerceIn(0, entries.lastIndex)
        return current
    }

    /**
     * Queue-only, forward-item removal. Caller must supply both the expected
     * absolute index and public URL: a stale UI swipe may not remove a
     * different song after Next/Jump/replacement. Never remove the playing
     * item, a previously played item, or shuffled future entries.
     */
    data class RemovedUpcoming(val entry: QueueEntry, val at: Int, val playingUrl: String)

    fun removeUpcoming(at: Int, expectedUrl: String): RemovedUpcoming? {
        val playing = current?.url ?: return null
        if (shuffle || at <= index || at !in entries.indices ||
            entries[at].url != expectedUrl) return null
        val removed = entries.removeAt(at)
        remapForRemoval(at)
        return RemovedUpcoming(removed, at, playing)
    }

    /**
     * Undo is allowed only while the same song still plays and the original
     * insertion remains ahead of it. Service also guards a one-time token and
     * invalidates undo when another queue operation runs.
     */
    fun restoreUpcoming(removed: RemovedUpcoming): Boolean {
        if (shuffle || current?.url != removed.playingUrl ||
            removed.at <= index || removed.at !in 0..entries.size ||
            entries.size >= 100 || entries.any { it.url == removed.entry.url }) return false
        entries.add(removed.at, removed.entry)
        remapForInsertion(removed.at)
        return true
    }

    private fun remapForRemoval(at: Int) {
        val history = visited.toList()
        visited.clear()
        history.forEach { old ->
            if (old != at) visited.add(if (old > at) old - 1 else old)
        }
        val shuffleHistory = shuffledSeen.toList()
        shuffledSeen.clear()
        shuffleHistory.forEach { old ->
            if (old != at) shuffledSeen.add(if (old > at) old - 1 else old)
        }
    }

    private fun remapForInsertion(at: Int) {
        val history = visited.toList()
        visited.clear()
        history.forEach { old -> visited.add(if (old >= at) old + 1 else old) }
        val shuffleHistory = shuffledSeen.toList()
        shuffledSeen.clear()
        shuffleHistory.forEach { old -> shuffledSeen.add(if (old >= at) old + 1 else old) }
    }

    fun add(entry: QueueEntry): Boolean {
        if (entries.size >= 100 || entries.any { it.url == entry.url }) return false
        entries.add(entry)
        if (index == -1) index = 0
        return true
    }

    fun next(automatic: Boolean = false): QueueEntry? {
        if (entries.isEmpty()) return null
        if (automatic && repeat == Repeat.ONE) return current
        val target = when {
            shuffle && entries.size > 1 -> {
                if (index >= 0) shuffledSeen.add(index)
                var candidates = entries.indices.filter { it !in shuffledSeen }
                if (candidates.isEmpty() && repeat == Repeat.ALL) {
                    shuffledSeen.clear()
                    candidates = entries.indices.filter { it != index }
                }
                if (candidates.isEmpty()) return null
                candidates[pick(candidates.size).coerceIn(0, candidates.lastIndex)]
            }
            index < entries.lastIndex -> index + 1
            repeat == Repeat.ALL -> 0
            else -> return null
        }
        if (index >= 0) visited.add(index)
        if (visited.size > 100) visited.removeFirst()
        index = target
        return current
    }

    fun jump(to: Int): QueueEntry? {
        if (to !in entries.indices) return null
        if (index >= 0 && index != to) visited.add(index)
        index = to
        return current
    }

    fun previous(): QueueEntry? {
        if (visited.isNotEmpty()) {
            index = visited.removeLast()
            return current
        }
        if (index > 0) index--
        else if (repeat == Repeat.ALL && entries.isNotEmpty()) index = entries.lastIndex
        return current
    }

    fun reset() {
        entries.clear()
        visited.clear()
        shuffledSeen.clear()
        index = -1
    }
}
