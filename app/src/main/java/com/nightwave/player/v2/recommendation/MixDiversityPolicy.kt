package com.nightwave.player.v2.recommendation

import com.nightwave.player.net.VideoPreview

/**
 * Local-only deterministic curator. This does not fetch music or infer genres,
 * moods, identities or user dislikes. Callers provide validated saved sources.
 */
internal object MixDiversityPolicy {
    private const val MAX_POOL = 500
    private const val MAX_OUTPUT = 12
    private const val REPEATED_CREATOR_PENALTY = 3

    fun select(
        source: List<VideoPreview>,
        dayNumber: Long,
        limit: Int = MAX_OUTPUT,
        score: (VideoPreview) -> Int
    ): List<VideoPreview> {
        if (limit <= 0) return emptyList()
        val remaining = source.take(MAX_POOL).distinctBy { it.url }.toMutableList()
        val result = ArrayList<VideoPreview>(minOf(limit, MAX_OUTPUT))
        val creatorCounts = HashMap<String, Int>()
        while (result.size < minOf(limit, MAX_OUTPUT) && remaining.isNotEmpty()) {
            val next = remaining.maxWithOrNull(
                compareBy<VideoPreview> { track ->
                    val creator = creatorKey(track.author)
                    val penalty = if (creator == null) 0
                        else (creatorCounts[creator] ?: 0) * REPEATED_CREATOR_PENALTY
                    score(track).coerceIn(-1000, 1000) - penalty
                }.thenBy { dailyKey(it.url, dayNumber) }
                    .thenBy { it.url }
            ) ?: break
            remaining.remove(next)
            result.add(next)
            creatorKey(next.author)?.let { key ->
                creatorCounts[key] = (creatorCounts[key] ?: 0) + 1
            }
        }
        return result
    }

    private fun creatorKey(value: String): String? {
        val key = value.trim().lowercase()
        return key.takeIf {
            it.isNotBlank() && it !in setOf("unknown", "unknown artist", "youtube creator")
        }
    }

    // SplitMix64 finalizer: the day changes the entire tie key, not merely its
    // low bits. Inputs never leave the device and do not require a random seed.
    private fun dailyKey(url: String, dayNumber: Long): Long {
        var x = url.hashCode().toLong() xor (dayNumber * -7046029254386353131L)
        x = (x xor (x ushr 30)) * -4658895280553007687L
        x = (x xor (x ushr 27)) * -7723592293110705685L
        return x xor (x ushr 31)
    }
}
