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
        refillUnplayed()
        return current
    }

    private fun refillUnplayed() {
        unplayed.clear()
        if (shuffle) unplayed.addAll(ids.indices.filter { it != index })
    }

    /** Automatic Repeat One replays this ID; manual Next advances normally. */
    fun next(automatic: Boolean = true): String? {
        if (index < 0) return null
        if (automatic && repeat == PlaybackQueue.Repeat.ONE) return current
        val target = if (shuffle) {
            if (unplayed.isEmpty() && repeat == PlaybackQueue.Repeat.ALL) refillUnplayed()
            if (unplayed.isEmpty()) return null
            unplayed.removeAt(pick(unplayed.size).coerceIn(0, unplayed.lastIndex))
        } else {
            when {
                index < ids.lastIndex -> index + 1
                repeat == PlaybackQueue.Repeat.ALL -> 0
                else -> return null
            }
        }
        if (target != index) pushHistory(index)
        index = target
        return current
    }

    fun previous(): String? {
        if (index < 0) return null
        val target = when {
            visited.isNotEmpty() -> visited.removeLast()
            shuffle -> return null
            index > 0 -> index - 1
            repeat == PlaybackQueue.Repeat.ALL -> ids.lastIndex
            else -> return null
        }
        if (shuffle && target != index) {
            unplayed.remove(target)
            if (index !in unplayed) unplayed.add(index)
        }
        index = target
        return current
    }

    fun snapshot(): List<String> = ids.toList()

    fun jump(to: Int): String? {
        if (to !in ids.indices) return null
        if (to != index) {
            if (shuffle) {
                // In shuffle mode, Previous retraces the actual listening order.
                if (index >= 0) pushHistory(index)
                unplayed.remove(to)
            } else {
                // An explicit linear jump starts a new position: Previous should
                // select the preceding visible track, not a stale visit.
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
        unplayed.clear()
        index = -1
        // Keep the user's playback preferences across new offline selections.
    }

    companion object { const val MAX_ITEMS = 400 }
}
