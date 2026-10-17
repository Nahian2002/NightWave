package com.nightwave.player.net

import java.util.Locale

/**
 * Device-only grouping of actual saved/recent public video references.
 * Uploaders are not verified recording artists: every group is labelled as
 * an artist/creator shortcut, never as an official artist profile or album.
 */
data class SavedCreatorGroup(
    val name: String,
    val trackCount: Int,
    val tracks: List<VideoPreview>
) {
    val hasMore: Boolean get() = trackCount > tracks.size
}

object SavedCreatorBrowser {
    const val MAX_SOURCE_TRACKS = 350
    const val MAX_GROUPS = 48
    const val MAX_QUEUE_TRACKS = 100

    private val generic = setOf(
        "youtubecreator", "unknowncreator", "unknownartist", "variousartists"
    )

    fun groups(source: List<VideoPreview>): List<SavedCreatorGroup> {
        class Bucket(
            val name: String,
            val tracks: MutableList<VideoPreview> = mutableListOf(),
            var count: Int = 0
        )
        val grouped = linkedMapOf<String, Bucket>()
        val seenUrls = hashSetOf<String>()
        source.take(MAX_SOURCE_TRACKS).forEach { video ->
            // These are local saved PUBLIC references, not imported file IDs.
            if (!video.url.startsWith("https://www.youtube.com/watch?v=") ||
                !seenUrls.add(video.url)) return@forEach
            val label = MusicIdentity.display(video.title, video.author)
                .artistOrCreator.trim()
            if (label.length !in 2..100) return@forEach
            val key = SearchRelevancePolicy.normalize(label)
                .filter { it.isLetterOrDigit() }
            if (key.length < 2 || key in generic) return@forEach
            val bucket = grouped.getOrPut(key) { Bucket(label) }
            bucket.count++
            // The playback service bounds new public queues to 100 entries.
            // Keep complete source-object identities and avoid presenting a
            // 350-item queue that silently truncates user-selected positions.
            if (bucket.tracks.size < MAX_QUEUE_TRACKS) bucket.tracks.add(video)
        }
        return grouped.values
            .sortedWith(compareByDescending<Bucket> { it.count }
                .thenBy { it.name.lowercase(Locale.ROOT) })
            .take(MAX_GROUPS)
            .map { SavedCreatorGroup(it.name, it.count, it.tracks.toList()) }
    }
}
