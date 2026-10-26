package com.nightwave.player.v2.data

/**
 * Archive/Drive playlist identity must match the existing local transaction
 * engine's case-insensitive comparisons, regardless of device language.
 * Bounded collections contain at most 50 names; pairwise validation avoids
 * lossy Unicode folding keys that disagree with String.equals(ignoreCase).
 */
object PlaylistNameIdentity {
    fun same(first: String, second: String): Boolean =
        first.equals(second, ignoreCase = true)

    fun contains(names: Iterable<String>, candidate: String): Boolean =
        names.any { same(it, candidate) }

    fun allDistinct(names: List<String>): Boolean =
        names.indices.all { i ->
            (0 until i).none { j -> same(names[i], names[j]) }
        }
}
