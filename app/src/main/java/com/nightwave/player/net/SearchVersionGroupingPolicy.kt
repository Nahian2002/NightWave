package com.nightwave.player.net

/**
 * Presentation-only alternative-version grouping. A "version" is still its
 * own independently selectable public video; never collapse URLs or pretend
 * an uploader's label proves official recording identity.
 */
internal object SearchVersionGroupingPolicy {
    private val versionLabels = linkedMapOf(
        "remix" to "Remix", "cover" to "Cover", "live" to "Live", "acoustic" to "Acoustic",
        "instrumental" to "Instrumental", "karaoke" to "Karaoke", "nightcore" to "Nightcore",
        "slowed" to "Slowed", "sped up" to "Sped up", "reverb" to "Reverb", "8d" to "8D",
        "রিমিক্স" to "রিমিক্স", "কভার" to "কভার", "লাইভ" to "লাইভ"
    )

    private fun has(text: String, phrase: String): Boolean = " $phrase " in " $text "

    fun titleLabels(title: String): List<String> {
        val text = SearchMusicIntentPolicy.canonical(title)
        val versions = versionLabels.filterKeys { has(text, it) }.values.toList()
        val formatLabels = linkedMapOf("official audio" to "Official audio", "official music video" to "Official music video",
            "official video" to "Official video", "lyric video" to "Lyric video", "lyrics" to "Lyrics",
            "visualizer" to "Visualizer", "visualiser" to "Visualiser")
        return versions + formatLabels.filterKeys { has(text, it) }.values
    }

    fun isOtherVersion(query: String, video: VideoPreview): Boolean {
        val wanted = SearchMusicIntentPolicy.canonical(query)
        if (wanted.isBlank()) return false
        if (versionLabels.keys.any { has(wanted, it) }) return false
        val title = SearchMusicIntentPolicy.canonical(video.title)
        if (versionLabels.keys.none { has(title, it) }) return false
        val words = SearchTokenMatchingPolicy.tokens(title + " " + SearchMusicIntentPolicy.canonical(video.author))
        return (listOf(wanted) + SearchMultilingualAliasPolicy.alternatives(wanted)).any { form ->
            SearchTokenMatchingPolicy.allCovered(SearchTokenMatchingPolicy.tokens(form), words)
        }
    }
}
