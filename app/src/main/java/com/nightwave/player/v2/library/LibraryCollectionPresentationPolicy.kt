package com.nightwave.player.v2.library

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview

/** Read-only labels from real membership. History and derived groups never imply a deliberate save. */
object LibraryCollectionPresentationPolicy {
    data class Overview(val likedCount: Int, val playlistCount: Int, val historySongs: Int,
        val historyEntries: Int, val savedSongs: Int) {
        val showRecentlyLiked: Boolean get() = likedCount > 0
        val showSaveHint: Boolean get() = savedSongs == 0
        val summary: String get() = "$likedCount liked songs · $playlistCount playlists"
    }
    private fun ids(songs: List<VideoPreview>) = songs.asSequence().map { it.url }
        .filter { VideoLink.normalize(it) == it }.toSet()

    fun displaySongs(songs: List<VideoPreview>): List<VideoPreview> = songs
        .filter { VideoLink.normalize(it.url) == it.url }.distinctBy { it.url }

    fun overview(liked: List<VideoPreview>, recent: List<VideoPreview>, playlists: List<LibraryPlaylistPreview>): Overview {
        val likes = ids(liked)
        return Overview(likes.size, playlists.size, ids(recent).size, recent.size,
            (likes + playlists.flatMap { ids(it.songs) }).size)
    }

    fun automaticDetail(songs: List<VideoPreview>, liked: List<VideoPreview>, recent: List<VideoPreview>,
        playlists: List<LibraryPlaylistPreview>): String = detail(songs, ids(liked), ids(recent),
            playlists.flatMap { ids(it.songs) }.toSet())

    /** Prepare memberships once per library change, rather than rescanning every playlist per card. */
    fun automaticDetails(collections: List<SmartLibraryCollection>, liked: List<VideoPreview>,
        recent: List<VideoPreview>, playlists: List<LibraryPlaylistPreview>): Map<String, String> {
        val likes = ids(liked); val history = ids(recent)
        val saved = playlists.flatMap { ids(it.songs) }.toSet()
        return collections.associate { it.id to detail(it.songs, likes, history, saved) }
    }

    private fun detail(songs: List<VideoPreview>, likes: Set<String>, history: Set<String>, saved: Set<String>): String {
        val selected = ids(songs)
        val membership = buildList {
            val likedCount = selected.count { it in likes }
            val playlistCount = selected.count { it in saved }
            val recentCount = selected.count { it in history }
            if (likedCount > 0) add("$likedCount liked")
            if (playlistCount > 0) add("$playlistCount in playlists")
            if (recentCount > 0) add("$recentCount in history")
        }
        return (listOf("${selected.size} songs", "Automatic collection") + membership).joinToString(" · ")
    }

    fun songSection(overview: Boolean, history: Boolean, automatic: Boolean, liked: Boolean): String = when {
        overview -> "Recently liked"
        history -> "Listening history"
        automatic -> "Collection songs"
        liked -> "Liked songs"
        else -> "Playlist songs"
    }
}
