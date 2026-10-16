package com.nightwave.player.playback

import kotlin.random.Random

data class QueueEntry(val url: String, val title: String, val creator: String, val artwork: String)

/** A URL-only queue. Streams are resolved just-in-time so signed links aren't stored or reused. */
class PlaybackQueue(private val pick: (Int) -> Int = { Random.nextInt(it) }) {
    enum class Repeat { OFF, ALL, ONE }
    private val entries = mutableListOf<QueueEntry>()
    private val visited = ArrayDeque<Int>()
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
        index = if (entries.isEmpty()) -1 else selected.coerceIn(0, entries.lastIndex)
        return current
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
                val candidates = entries.indices.filter { it != index }
                candidates[pick(candidates.size).coerceIn(0, candidates.lastIndex)]
            }
            index < entries.lastIndex -> index + 1
            repeat == Repeat.ALL -> 0
            else -> return null
        }
        if (index >= 0) visited.addLast(index)
        if (visited.size > 100) visited.removeFirst()
        index = target
        return current
    }

    fun jump(to: Int): QueueEntry? {
        if (to !in entries.indices) return null
        if (index >= 0 && index != to) visited.addLast(index)
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
        index = -1
    }
}
