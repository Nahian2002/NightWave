package com.nightwave.player.v2.library

import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.SearchRelevancePolicy
import com.nightwave.player.net.SearchTokenMatchingPolicy
import com.nightwave.player.net.VideoPreview

/** UI-only collection snapshot. No source index, playlist order or URL is modified. */
data class LibraryPlaylistPreview(val name: String, val songs: List<VideoPreview>) {
    val size: Int get() = songs.size
    val cover: VideoPreview? get() = songs.firstOrNull()
}

/**
 * Pure bounded library browsing rules. Existing legacy saved order is preserved
 * unless the user explicitly chooses a temporary view sort.
 */
object LibraryBrowsePolicy {
    const val SORT_SAVED = "saved"
    const val SORT_NAME = "name"
    const val SORT_NAME_DESC = "name_desc"
    const val SORT_MOST_TRACKS = "most_tracks"
    const val SORT_TITLE = "title"
    const val SORT_TITLE_DESC = "title_desc"
    const val SORT_CREATOR = "artist"

    fun playlists(
        source: List<LibraryPlaylistPreview>,
        query: String,
        sort: String,
        withTracksOnly: Boolean,
        matchContainedSongs: Boolean = false
    ): List<LibraryPlaylistPreview> {
        val search = SearchRelevancePolicy.prepare(query.take(80))
        val filtered = source.withIndex().filter { (_, playlist) ->
            if (withTracksOnly && playlist.size == 0) return@filter false
            if (!search.valid) return@filter true
            val name = SearchRelevancePolicy.normalize(playlist.name)
            if (SearchTokenMatchingPolicy.allCovered(
                    search.terms, SearchTokenMatchingPolicy.tokens(name)
                )) return@filter true
            // Optional NW71 playlist discovery: find the containing playlist
            // by one song's title/creator, not by joining unrelated songs.
            // Bounded scan, view-only, no change to playlist membership/order.
            matchContainedSongs && playlist.songs.asSequence().take(100)
                .any { SearchRelevancePolicy.savedMatches(search, it) }
        }
        val ordered = when (sort) {
            SORT_NAME -> filtered.sortedWith(
                compareBy<IndexedValue<LibraryPlaylistPreview>> {
                    SearchRelevancePolicy.normalize(it.value.name)
                }.thenBy { it.index })
            SORT_NAME_DESC -> filtered.sortedWith(
                compareByDescending<IndexedValue<LibraryPlaylistPreview>> {
                    SearchRelevancePolicy.normalize(it.value.name)
                }.thenBy { it.index })
            SORT_MOST_TRACKS -> filtered.sortedWith(
                compareByDescending<IndexedValue<LibraryPlaylistPreview>> {
                    it.value.size
                }.thenBy { SearchRelevancePolicy.normalize(it.value.name) }
                    .thenBy { it.index })
            else -> filtered
        }
        return ordered.map { it.value }
    }

    fun songs(
        source: List<VideoPreview>,
        query: String,
        sort: String
    ): List<VideoPreview> {
        val search = SearchRelevancePolicy.prepare(query.take(80))
        val matched = source.withIndex().filter { (_, song) ->
            !search.valid || SearchRelevancePolicy.savedMatches(search, song)
        }
        val ordered = when (sort) {
            SORT_TITLE, SORT_TITLE_DESC -> {
                val compare = compareBy<IndexedValue<VideoPreview>> {
                    SearchRelevancePolicy.normalize(
                        MusicIdentity.display(it.value.title, it.value.author).title)
                }
                val stable = if (sort == SORT_TITLE_DESC) compare.reversed() else compare
                matched.sortedWith(stable.thenBy { it.index })
            }
            SORT_CREATOR -> matched.sortedWith(
                compareBy<IndexedValue<VideoPreview>> {
                    SearchRelevancePolicy.normalize(
                        MusicIdentity.display(it.value.title, it.value.author).artistOrCreator)
                }.thenBy {
                    SearchRelevancePolicy.normalize(
                        MusicIdentity.display(it.value.title, it.value.author).title)
                }.thenBy { it.index })
            else -> matched
        }
        return ordered.map { it.value }
    }
}
