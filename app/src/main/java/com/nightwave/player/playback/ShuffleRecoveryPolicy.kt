package com.nightwave.player.playback

/** Public-ID-only history, never a resolved media URL or playback token. */
data class ShuffleRecoverySnapshot(
    val visited: List<String> = emptyList(),
    val forward: List<String> = emptyList(),
    val seen: List<String> = emptyList()
)

/** Reject entire untrusted shuffle history without erasing a valid selected queue. */
internal object ShuffleRecoveryPolicy {
    const val MAX_HISTORY = 100

    fun validated(
        stored: ShuffleRecoverySnapshot?,
        available: Set<String>,
        selectedUrl: String?
    ): ShuffleRecoverySnapshot? {
        if (stored == null || selectedUrl == null || selectedUrl !in available) return null
        val portions = listOf(stored.visited, stored.forward, stored.seen)
        if (portions.any { it.size > MAX_HISTORY || it.any { url -> url !in available } }) return null
        if (stored.seen.size != stored.seen.distinct().size) return null
        return stored
    }
}
