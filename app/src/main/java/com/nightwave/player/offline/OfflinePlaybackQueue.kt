package com.nightwave.player.offline

import com.nightwave.player.playback.PlaybackQueue
import kotlin.random.Random

/**
 * App-private offline IDs only. No file paths, content URIs, signed URLs or
 * library mutations. Media3 owns prepared timeline playback; this also provides
 * bounded legacy fallback navigation for queues that cannot be preloaded.
 */
class OfflinePlaybackQueue(private val pick: (Int) -> Int = { Random.nextInt(it) }) {
    private val ids = mutableListOf<String>()
    private val visited = ArrayDeque<Int>()
    // NW69: when Shuffle Previous retraces a song, Next should retrace the
    // departed song before picking fresh candidates. Session-only, bounded.
    private val forwardVisits = ArrayDeque<Int>()
    private val unplayed = mutableListOf<Int>()
    var index: Int = -1
        private set

    var shuffle: Boolean = false
        private set
    var repeat: PlaybackQueue.Repeat = PlaybackQueue.Repeat.OFF
        private set

    val current: String? get() = ids.getOrNull(index)
    val size: Int get() = ids.size

    fun setShuffle(enabled: Boolean) {
        if (shuffle == enabled) return
        shuffle = enabled
        visited.clear()
        forwardVisits.clear()
        refillUnplayed()
    }

    fun setRepeat(mode: PlaybackQueue.Repeat) {
        repeat = mode
    }

    fun select(candidates: List<String>, selectedId: String): String? {
        val clean = candidates.filter { it.isNotBlank() }.distinct().take(MAX_ITEMS)
        val selected = clean.indexOf(selectedId)
        if (selected < 0) return null
        ids.clear()
        ids.addAll(clean)
        index = selected
        visited.clear()
        forwardVisits.clear()
        refillUnplayed()
        return current
    }

    private fun refillUnplayed() {
        unplayed.clear()
        if (shuffle) unplayed.addAll(ids.indices.filter { it != index })
    }

    /**
     * NW100: read-only availability probe for media notification command
     * receipts. Previous/Forward history and the unplayed pool are untouched.
     */
    fun hasNext(): Boolean {
        if (current == null) return false
        if (!shuffle) return index < ids.lastIndex ||
            repeat == PlaybackQueue.Repeat.ALL
        if (ids.size == 1) return repeat == PlaybackQueue.Repeat.ALL
        if (forwardVisits.lastOrNull()?.let { it in ids.indices && it != index } == true)
            return true
        return unplayed.any { it in ids.indices && it != index } ||
            repeat == PlaybackQueue.Repeat.ALL
    }

    /** Automatic Repeat One replays this ID; manual Next advances normally. */
    fun next(automatic: Boolean = true): String? {
        if (index < 0) return null
        if (automatic && repeat == PlaybackQueue.Repeat.ONE) return current
        // Shuffle has no other candidate for a one-song queue. Repeat All
        // must replay that song instead of incorrectly signalling end of queue.
        if (shuffle && ids.size == 1 && repeat == PlaybackQueue.Repeat.ALL)
            return current
        val target = if (shuffle) {
            // A shuffled Previous builds an exact forward redo trail, not a
            // fresh random candidate. Never reroll an already heard track.
            val forward = forwardVisits.lastOrNull()
                ?.takeIf { it in ids.indices && it != index }
            if (forward != null) {
                forwardVisits.removeLast()
                unplayed.remove(forward)
                forward
            } else {
                forwardVisits.clear() // defensive if the queue changed
                if (unplayed.isEmpty() && repeat == PlaybackQueue.Repeat.ALL) refillUnplayed()
                if (unplayed.isEmpty()) return null
                unplayed.removeAt(pick(unplayed.size).coerceIn(0, unplayed.lastIndex))
            }
        } else {
            when {
                index < ids.lastIndex -> index + 1
                repeat == PlaybackQueue.Repeat.ALL -> 0
                else -> return null
            }
        }
        // Only shuffled playback requires a visit stack. In linear mode,
        // Previous always follows the visible queue, including after a jump.
        if (shuffle && target != index) pushHistory(index)
        index = target
        return current
    }

    fun previous(): String? {
        if (index < 0) return null
        val target = when {
            shuffle && visited.isNotEmpty() -> visited.removeLast()
            shuffle -> return null
            index > 0 -> index - 1
            repeat == PlaybackQueue.Repeat.ALL -> ids.lastIndex
            else -> return null
        }
        if (shuffle && target != index) {
            if (index in ids.indices) {
                forwardVisits.add(index)
                if (forwardVisits.size > MAX_ITEMS) forwardVisits.removeFirst()
            }
            // Both songs have already been played in this shuffle cycle.
            // Forward may retrace the departed song, but Previous must never
            // return it to random candidates after a different Jump branch.
            unplayed.remove(target)
        }
        index = target
        return current
    }

    fun snapshot(): List<String> = ids.toList()

    /** Remove unavailable IDs without rerolling shuffle or starting another song. */
    fun retain(available: Set<String>) {
        val selected = current
        if (selected == null || selected !in available) { reset(); return }
        val old = ids.toList()
        val kept = old.filter(available::contains)
        val positions = kept.withIndex().associate { it.value to it.index }
        fun remap(values: Collection<Int>): List<Int> = values.mapNotNull {
            old.getOrNull(it)?.let(positions::get)
        }
        val previous = remap(visited)
        val forward = remap(forwardVisits)
        val pending = remap(unplayed)
        ids.clear(); ids.addAll(kept)
        index = positions.getValue(selected)
        visited.clear(); visited.addAll(previous)
        forwardVisits.clear(); forwardVisits.addAll(forward)
        unplayed.clear(); unplayed.addAll(pending)
    }

    fun jump(to: Int): String? {
        if (to !in ids.indices) return null
        if (to != index) {
            forwardVisits.clear() // explicit new branch, not a Next redo
            if (shuffle) {
                // In shuffle mode, Previous retraces the actual listening order.
                if (index >= 0) pushHistory(index)
                unplayed.remove(to)
            } else {
                // No linear history is recorded; clear any previous shuffle
                // state defensively before following the visible queue.
                visited.clear()
            }
            index = to
        }
        return current
    }

    private fun pushHistory(from: Int) {
        visited.add(from)
        if (visited.size > MAX_ITEMS) visited.removeFirst()
    }

    fun reset() {
        ids.clear()
        visited.clear()
        forwardVisits.clear()
        unplayed.clear()
        index = -1
        // Keep the user's playback preferences across new offline selections.
    }

    companion object { const val MAX_ITEMS = 400 }
}
