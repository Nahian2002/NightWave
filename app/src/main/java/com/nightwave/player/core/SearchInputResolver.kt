package com.nightwave.player.core

/** Keep user-entered song names separate from YouTube video URLs. */
sealed interface SearchInput {
    data class Keywords(val query: String) : SearchInput
    data class VideoUrl(val url: String) : SearchInput
    data object Blank : SearchInput
    data object BadLink : SearchInput
}

object SearchInputResolver {
    fun resolve(raw: String): SearchInput {
        val input = raw.trim()
        if (input.isEmpty()) return SearchInput.Blank
        val url = VideoLink.normalize(input)
        if (url != null) return SearchInput.VideoUrl(url)
        return if (VideoLink.resemblesLink(input)) SearchInput.BadLink else SearchInput.Keywords(input)
    }
}
