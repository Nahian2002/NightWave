package com.nightwave.player.playback

import kotlin.random.Random

data class QueueEntry(val url: String, val title: String, val creator: String, val artwork: String)

/** A URL-only queue. Streams are resolved just-in-time so signed links aren't stored or reused. */
class PlaybackQueue(private val pick: (Int) -> Int = { Random.nextInt(it) }) {
    enum class Repeat { OFF, ALL, ONE }
    private val entries = mutableListOf<QueueEntry>()
    private val visited = ArrayDeque<Int>()
    private val shuffledSeen = mutableSetOf<Int>()
    /** Explicit Play Next always outranks shuffle candidates in this session. */
    private val manualNext = ArrayDeque<String>()
    var index: Int = -1
        private set
    var shuffle: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                // A previous shuffled listening session must not leak into
                // linear Previous or a new Shuffle session.
                visited.clear()
                shuffledSeen.clear()
                manualNext.clear()
            }
        }
    var repeat: Repeat = Repeat.OFF

    /**
     * A user-initiated Library Play list request selects an explicit order.
     * Forget the previous shuffle cycle; normal song taps and restored queues
     * keep their existing Shuffle setting.
     */
    fun useExplicitDisplayOrder() {
        shuffle = false
        visited.clear()
        shuffledSeen.clear()
        manualNext.clear()
    }
    val size: Int get() = entries.size
    val current: QueueEntry? get() = entries.getOrNull(index)
    fun snapshot(): List<QueueEntry> = entries.toList()

    fun replace(items: List<QueueEntry>, selected: Int): QueueEntry? {
        // Resolve the requested identity before deduplicating URLs. Otherwise,
        // duplicate rows earlier in the list shift the selected track index.
        val requestedUrl = items.getOrNull(selected)?.url
        entries.clear()
        entries.addAll(items.distinctBy { it.url }.take(100))
        visited.clear()
        shuffledSeen.clear()
        manualNext.clear()
        index = if (entries.isEmpty()) -1 else {
            val exactIndex = entries.indexOfFirst { it.url == requestedUrl }
            if (exactIndex >= 0) exactIndex else selected.coerceIn(0, entries.lastIndex)
        }
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
        manualNext.remove(removed.url)
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

    /**
     * Play Next differs from Add to Queue: it moves an already queued URL or
     * inserts a valid new item directly after the current song.
     * For shuffle, manual priority is consumed by next() before random picks.
     * Only the service is permitted to persist/start the resulting queue.
     */
    fun insertNext(entry: QueueEntry): Boolean {
        if (entry.url.isBlank() || current?.url == entry.url) return false
        if (current == null) {
            replace(listOf(entry), 0)
            return true
        }
        val prior = entries.indexOfFirst { it.url == entry.url }
        if (prior >= 0 && prior <= index) return false // Don't rewrite history.
        if (prior < 0 && entries.size >= 100) return false
        val toInsert = if (prior >= 0) {
            val existing = entries.removeAt(prior)
            remapForRemoval(prior)
            existing // Keep previously saved exact title/artwork metadata.
        } else entry
        val target = (index + 1).coerceAtMost(entries.size)
        entries.add(target, toInsert)
        remapForInsertion(target)
        // Only queue contents and order are persisted, never a private stream URL.
        manualNext.remove(entry.url)
        manualNext.addFirst(entry.url)
        return true
    }

    /** Public video IDs only; QueueStateStore validates them again before disk. */
    fun priorityNextUrls(): List<String> = manualNext.filter { wanted ->
        entries.indexOfFirst { it.url == wanted } > index
    }.distinct().take(100)

    /** Restore only existing upcoming URLs; old/corrupt snapshots yield no priority. */
    fun restoreNextPriorities(urls: List<String>) {
        manualNext.clear()
        urls.distinct().take(100).forEach { wanted ->
            if (entries.indexOfFirst { it.url == wanted } > index)
                manualNext.addLast(wanted)
        }
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
        // Manually chosen Play Next still counts the current track as visited
        // in a shuffle cycle, so it cannot be replayed as an unseen candidate.
        if (shuffle && index >= 0) shuffledSeen.add(index)
        val queuedNext = manualNext.firstOrNull()?.let { wanted ->
            entries.indexOfFirst { it.url == wanted && it.url != current?.url }
        }?.takeIf { it >= 0 }
        val target = when {
            queuedNext != null -> {
                manualNext.removeFirst()
                queuedNext
            }
            shuffle && entries.size > 1 -> {
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
        if (shuffle && index >= 0 && index != target) {
            visited.add(index)
            if (visited.size > 100) visited.removeFirst()
        }
        index = target
        // Linear playback or explicit jump can reach pending priority itself.
        manualNext.remove(current?.url)
        return current
    }

    fun jump(to: Int): QueueEntry? {
        if (to !in entries.indices) return null
        if (shuffle && index >= 0 && index != to) {
            visited.add(index)
            if (visited.size > 100) visited.removeFirst()
        }
        index = to
        manualNext.remove(current?.url)
        return current
    }

    fun previous(): QueueEntry? {
        if (index < 0) return null
        // Unshuffled Previous follows the visible queue, not past jumps.
        // Shuffled Previous retraces the actual listening sequence.
        if (shuffle) {
            // Without a real prior shuffled selection there is no meaningful
            // "previous" track; never guess using linear index ordering.
            if (visited.isEmpty()) return null
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
        manualNext.clear()
        index = -1
    }
}
