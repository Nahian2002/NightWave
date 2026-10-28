package com.nightwave.player.v2.ui

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.recommendation.PersonalMix

/** Presentation of actual bounded Home inputs; no new songs, writes, requests or taste inference. */
object NightWaveHomePresentationPolicy {
    enum class Kind { NEW, HISTORY_ONLY, SAVED_ONLY, MIXED }
    data class Entry(
        val kind: Kind,
        val primary: NightWaveHomeLayoutPolicy.EmptyRecentState?,
        val showLikedShortcut: Boolean,
        val showSearchShortcut: Boolean
    )
    data class MixPresentation(val title: String, val caption: String, val startsWith: String, val reason: String, val purpose: String = "")

    private fun ids(tracks: List<VideoPreview>): Set<String> = tracks.asSequence()
        .map { it.url }.filter { VideoLink.normalize(it) == it }.toSet()

    fun entry(favorites: List<VideoPreview>, recent: List<VideoPreview>, playlists: List<VideoPreview>): Entry {
        val liked = ids(favorites).isNotEmpty()
        val saved = liked || ids(playlists).isNotEmpty()
        val history = ids(recent).isNotEmpty()
        val kind = when {
            history && saved -> Kind.MIXED
            history -> Kind.HISTORY_ONLY
            saved -> Kind.SAVED_ONLY
            else -> Kind.NEW
        }
        val primary = if (history) null else when {
            liked -> NightWaveHomeLayoutPolicy.EmptyRecentState("Your liked songs", "Pick something you chose to save.",
                "Open Liked Songs", NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIKED_SONGS)
            saved -> NightWaveHomeLayoutPolicy.EmptyRecentState("Your playlists, together", "Choose a collection from your library.",
                "Open Library", NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIBRARY)
            else -> NightWaveHomeLayoutPolicy.EmptyRecentState("Your soundtrack starts here", "Find a song or bring your own audio.",
                "Find music", NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH)
        }
        return Entry(kind, primary,
            showLikedShortcut = liked && primary?.destination != NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIKED_SONGS,
            showSearchShortcut = primary?.destination != NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH)
    }

    fun mix(mix: PersonalMix, favorites: List<VideoPreview>, recent: List<VideoPreview>,
        playlists: List<VideoPreview>, memoryEnabled: Boolean): MixPresentation {
        val liked = ids(favorites); val history = ids(recent); val lists = ids(playlists)
        val tracks = mix.tracks.distinctBy { it.url }
        val likedCount = tracks.count { it.url in liked }
        val recentCount = tracks.count { it.url in history }
        val playlistCount = tracks.count { it.url in lists }
        val outsideRecent = tracks.count { it.url !in history }
        val knownUploaders = tracks.map { it.author.trim() }.filter { it.isNotBlank() &&
            it.lowercase(java.util.Locale.ROOT) !in setOf("unknown", "unknown creator", "unknown artist", "youtube creator", "various artists") }
            .map { it.lowercase(java.util.Locale.ROOT) }.distinct().size
        val title = when (mix.key) {
            "daily_mix_1" -> when { likedCount > 0 -> "Familiar picks"; recentCount > 0 -> "Daily selection"; else -> "Playlist picks" }
            "daily_mix_2" -> if (outsideRecent > 0) "Beyond recent plays" else "Another direction"
            "daily_mix_3" -> if (knownUploaders >= 2) "Creator blend" else "Another way to listen"
            "memory" -> "From your listening"
            "artists" -> "Familiar creators"
            "replay" -> if (tracks.isNotEmpty() && recentCount == tracks.size) "Listening history" else mix.title
            else -> mix.title
        }
        val membership = buildList {
            if (likedCount > 0) add("$likedCount liked")
            if (playlistCount > 0) add("$playlistCount in playlists")
            if (recentCount > 0) add("$recentCount recent")
        }.joinToString(" · ")
        val saved = liked + lists
        val caption = when {
            tracks.isNotEmpty() && tracks.all { it.url in saved } -> "SAVED MUSIC"
            tracks.isNotEmpty() && tracks.all { it.url in history && it.url !in saved } -> "RECENT MUSIC"
            else -> "YOUR MUSIC"
        }
        val first = tracks.firstOrNull()?.let { MusicIdentity.display(it.title, it.author).title }
        val purpose = when {
            mix.key == "daily_mix_1" && likedCount > 0 -> "Start with songs you liked"
            mix.key == "daily_mix_1" -> "A familiar starting point"
            mix.key == "daily_mix_2" && outsideRecent > 0 -> "Beyond your recent plays"
            mix.key == "daily_mix_2" -> "A different order to explore"
            mix.key == "daily_mix_3" && knownUploaders >= 2 -> "Across $knownUploaders creators"
            mix.key == "replay" && tracks.isNotEmpty() && recentCount == tracks.size -> "In the order you listened"
            tracks.isNotEmpty() && recentCount == 0 && tracks.all { it.url in saved } -> "Revisit music you saved"
            else -> "More from your music"
        }
        return MixPresentation(title, caption, first?.let { "Starts with $it" }.orEmpty(),
            "On this device" + (if (membership.isNotEmpty()) " · $membership" else "") +
                (if (memoryEnabled) " · Listening memory enabled" else ""), purpose)
    }
}
