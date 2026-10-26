package com.nightwave.player.net

/**
 * Presentation-only alternative-version grouping. A "version" is still its
 * own independently selectable public video; never collapse URLs or pretend
 * an uploader's label proves official recording identity.
 */
internal object SearchVersionGroupingPolicy {
    private val versionCues = setOf(
        "remix", "cover", "live", "acoustic", "instrumental",
        "karaoke", "nightcore", "slowed", "sped", "reverb"
    )

    fun isOtherVersion(query: String, video: VideoPreview): Boolean {
        val wanted = SearchMusicIntentPolicy.canonical(query)
        if (wanted.isBlank()) return false
        val terms = SearchTokenMatchingPolicy.tokens(wanted)
        if (terms.any { it in versionCues }) return false
        return SearchMusicIntentPolicy.titleCandidates(video).any { candidate ->
            if (!candidate.startsWith("$wanted ")) return@any false
            val suffix = candidate.removePrefix("$wanted ")
            val first = suffix.substringBefore(' ')
            first in versionCues &&
                (first != "sped" || suffix.startsWith("sped up"))
        }
    }
}
