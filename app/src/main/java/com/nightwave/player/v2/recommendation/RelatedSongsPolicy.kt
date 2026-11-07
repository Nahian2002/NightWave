package com.nightwave.player.v2.recommendation

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.SearchMusicIntentPolicy
import com.nightwave.player.net.SearchRelevancePolicy
import com.nightwave.player.net.VideoPreview

/** Related public songs from the visible search and liked songs, without fetching. */
internal object RelatedSongsPolicy {
    private val generic = setOf(
        "youtube", "youtube creator", "unknown creator", "unknown artist", "various artists"
    )
    private val topicSuffix = Regex("""\s*[-–—]\s*Topic$""", RegexOption.IGNORE_CASE)
    private val filler = setOf("official", "music", "video", "audio", "lyrics", "remix", "cover")

    private data class Match(val track: VideoPreview, val url: String, val identity: Pair<String, String>,
        val sameArtist: Boolean, val sharedWords: Int)

    fun artist(track: VideoPreview): String? =
        MusicIdentity.display(track.title, track.author).artistOrCreator.replace(topicSuffix, "").takeIf {
            val name = SearchRelevancePolicy.normalize(it)
            name.isNotBlank() && name !in generic
        }

    fun select(current: VideoPreview, candidates: List<VideoPreview>): List<VideoPreview> {
        val currentUrl = VideoLink.normalize(current.url) ?: return emptyList()
        val currentTitle = title(current)
        val currentArtist = artist(current)?.let(SearchRelevancePolicy::normalize)
        val currentWords = meaningfulWords(currentTitle)
        return candidates.mapNotNull { candidate ->
            val url = VideoLink.normalize(candidate.url) ?: return@mapNotNull null
            val title = title(candidate)
            if (url == currentUrl || title == currentTitle ||
                (candidate.durationSeconds != 0L && candidate.durationSeconds !in 50L..1200L)) return@mapNotNull null
            val artist = artist(candidate)?.let(SearchRelevancePolicy::normalize).orEmpty()
            val sameArtist = currentArtist != null && artist == currentArtist
            val sharedWords = currentWords.intersect(meaningfulWords(title)).size
            if ((!sameArtist && sharedWords < 2) ||
                SearchMusicIntentPolicy.incidentalNonMusicPenalty(currentTitle, candidate) <= -230)
                return@mapNotNull null
            Match(candidate, url, title to artist, sameArtist, sharedWords)
        }.sortedWith(compareByDescending<Match> { it.sameArtist }
            .thenByDescending { it.sharedWords }
            .thenByDescending { it.track.viewCount ?: -1L })
            .distinctBy { it.url }
            .distinctBy { it.identity }
            .take(3)
            .map { it.track }
    }

    private fun title(track: VideoPreview): String =
        SearchRelevancePolicy.normalize(MusicIdentity.display(track.title, track.author).title)

    private fun meaningfulWords(title: String): Set<String> =
        title.split(' ').filter { it.length >= 3 && it !in filler }.toSet()
}
