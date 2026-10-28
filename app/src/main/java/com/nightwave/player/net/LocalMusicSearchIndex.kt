package com.nightwave.player.net

import com.nightwave.player.offline.OfflineLibraryCapacity
import com.nightwave.player.offline.OfflineSong
import com.nightwave.player.offline.OfflineTrackSearch
import com.nightwave.player.v2.data.room.LegacyLibrarySnapshot

data class LocalMusicSnapshot(val publicLibrary: LegacyLibrarySnapshot, val offline: List<OfflineSong>)
data class LocalMusicOrigin(val liked: Boolean, val recent: Boolean, val playlists: Set<String>) {
    fun label(): String = buildList {
        if (liked) add("Liked")
        if (playlists.isNotEmpty()) add(if (playlists.size == 1) "In a playlist" else "In ${playlists.size} playlists")
        if (recent) add("Recent")
    }.joinToString(" · ")
}

/** Results carry their exact immutable index and query; stale rows fail closed
 * synchronously in composition, before a producer's cancellation can run.
 */
data class LocalMusicSearchResult(
    val index: LocalMusicSearchIndex, val query: String,
    val saved: List<VideoPreview>, val offline: List<OfflineSong>,
    val suggestions: List<LocalSearchSuggestion>
) {
    fun accepts(query: String, index: LocalMusicSearchIndex?): Boolean = this.query == query && this.index === index
}

class LocalMusicSearchIndex private constructor(
    private val saved: SavedTrackSearch.Prepared,
    private val audio: OfflineTrackSearch.Prepared,
    val previews: List<VideoPreview>, val creators: List<SavedCreatorShortcut>,
    val creatorGroups: List<SavedCreatorGroup>, private val origins: Map<String, LocalMusicOrigin>,
    private val preparedCreators: List<LocalSearchSuggestions.PreparedCreator>,
    val referenceCount: Int, val offlineCount: Int
) {
    val publicCount: Int get() = origins.size
    fun origin(url: String): LocalMusicOrigin? = origins[url]
    fun search(query: String, checkpoint: () -> Unit = {}): LocalMusicSearchResult {
        val matches = saved.all(query, checkpoint).tracks
        val offline = if (query.isBlank()) emptyList() else audio.match(query, checkpoint)
        val suggestions = LocalSearchSuggestions.fromPrepared(query, matches, preparedCreators,
            provenance = "local references · uploader/creator")
        checkpoint()
        return LocalMusicSearchResult(this, query, matches, offline, suggestions)
    }

    companion object {
        const val MAX_PUBLIC_REFERENCES = 15_600 // 500 likes + 100 recent + 50 × 300 playlist references.
        fun build(snapshot: LocalMusicSnapshot, checkpoint: () -> Unit = {}): LocalMusicSearchIndex {
            val library = snapshot.publicLibrary
            require(library.favorites.size <= 500 && library.recent.size <= 100 &&
                library.playlists.size <= 50 && library.playlists.all { it.second.size <= 300 }) {
                "Library exceeds supported metadata bounds"
            }
            require(snapshot.offline.size <= OfflineLibraryCapacity.MAX_ITEMS) { "Offline index exceeds supported bounds" }
            class Origin(var liked: Boolean = false, var recent: Boolean = false,
                val playlists: MutableSet<String> = linkedSetOf())
            val origins = linkedMapOf<String, Origin>()
            val all = ArrayList<VideoPreview>()
            val representatives = linkedMapOf<String, VideoPreview>()
            fun quality(video: VideoPreview): Int =
                (if (video.title.isNotBlank()) 2 else 0) +
                (if (video.author.isNotBlank() && !video.author.startsWith("Unknown", true)) 4 else 0) +
                (if (video.thumbnail.isNotBlank()) 1 else 0) + (if (video.durationSeconds > 0) 1 else 0)
            fun add(tracks: List<VideoPreview>, liked: Boolean = false, recent: Boolean = false, playlist: String? = null) {
                tracks.forEach { track ->
                    if (all.size % 128 == 0) checkpoint()
                    all.add(track) // ALL duplicate metadata variants reach the scorer.
                    val origin = origins.getOrPut(track.url) { Origin() }
                    origin.liked = origin.liked || liked
                    origin.recent = origin.recent || recent
                    if (playlist != null) origin.playlists.add(playlist)
                    val old = representatives[track.url]
                    if (old == null || quality(track) > quality(old)) representatives[track.url] = track
                }
            }
            add(library.favorites, liked = true)
            add(library.recent, recent = true)
            library.playlists.forEach { (name, tracks) -> add(tracks, playlist = name) }
            require(all.size <= MAX_PUBLIC_REFERENCES)
            val previews = representatives.values.toList()
            val creators = MusicIdentity.allCreatorsFrom(previews, checkpoint)
            val index = LocalMusicSearchIndex(SavedTrackSearch.prepare(all, checkpoint),
                OfflineTrackSearch.prepare(snapshot.offline, checkpoint), previews,
                creators,
                SavedCreatorBrowser.groups(previews, scanLimit = MAX_PUBLIC_REFERENCES),
                origins.mapValues { (_, v) -> LocalMusicOrigin(v.liked, v.recent, v.playlists.toSet()) },
                LocalSearchSuggestions.prepareCreators(creators),
                all.size, snapshot.offline.size)
            checkpoint()
            return index
        }
    }
}
