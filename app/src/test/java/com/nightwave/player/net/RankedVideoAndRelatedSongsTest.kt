package com.nightwave.player.net

import com.nightwave.player.v2.recommendation.RelatedSongsPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class RankedVideoAndRelatedSongsTest {
    @Test fun viewsBreakTiesWithoutDisplacingRelevantResults() {
        val low = video(1, "Night Train", views = 31)
        val popular = video(2, "Night Train", views = 400_000)
        val unknown = video(3, "Night Train")
        val unrelated = video(4, "Cooking Tutorial", views = 5_000_000_000L)
        val expected = listOf(popular, low, unknown, unrelated)
        val input = listOf(low, unrelated, unknown, popular)

        assertEquals(expected, SearchRelevancePolicy.rankVideos("Night Train", input))
        assertEquals(expected, SearchRelevancePolicy.rankPublic("Night Train", input))
    }

    @Test fun gameplayDoesNotBeatTheSongInSongsMode() {
        val song = video(1, "Minecraft (Official Audio)", "C418", 40_000)
        val realLife = video(2, "Minecraft VS Real Life – What Minecraft Looks Like in Real Life!",
            "Gaming Channel", 800_000)
        val speedrun = video(3, "Minecraft Speedrunner VS 6 Hunters FINALE", "Creator", 900_000)
        val longGeneric = video(4, "Minecraft but You Can Equip Anything", "Gaming Channel", 2_000_000)
            .copy(durationSeconds = 840)
        val shortGeneric = video(5, "Crooked TNT Tunnel in Minecraft", "Peach Craft", 1_000_000)
            .copy(durationSeconds = 615)
        val groups = SearchResultPresentationPolicy.groupMusic("Minecraft", SearchMode.SONGS,
            listOf(realLife, speedrun, longGeneric, shortGeneric, song))

        assertEquals(listOf(song), groups.bestMatches)
        assertEquals(setOf(realLife, speedrun, longGeneric, shortGeneric), groups.moreResults.toSet())
        assertEquals(0, SearchMusicIntentPolicy.incidentalNonMusicPenalty(
            "Minecraft speedrunner", speedrun))
    }

    @Test fun exactLongSongsAndSoundtracksRemainEligibleForHighlights() {
        val exact = video(1, "Echoes", "Pink Floyd").copy(durationSeconds = 1411)
        val soundtrack = video(2, "Minecraft Original Soundtrack", "C418").copy(durationSeconds = 3000)

        assertEquals(true, SearchMusicIntentPolicy.canHighlightInSongs("Echoes", exact))
        assertEquals(true, SearchMusicIntentPolicy.canHighlightInSongs("Minecraft", soundtrack))
        val topicTrack = video(3, "Lilypad", "Minecraft - Topic")
        assertEquals(true, SearchMusicIntentPolicy.canHighlightInSongs("Minecraft", topicTrack))
    }

    @Test fun missingViewsKeepProviderTieOrder() {
        val input = listOf(video(2, "Night Train"), video(1, "Night Train"))
        assertEquals(input, SearchRelevancePolicy.rankVideos("Night Train", input))
        assertEquals(input, SearchRelevancePolicy.rankPublic("Night Train", input))
    }

    @Test fun relatedSongsPreferArtistExcludeDuplicatesAndRequireEvidence() {
        val current = video(1, "Night Train")
        val duplicateUrl = current.copy(url = "https://youtu.be/id000000001")
        val duplicateTitle = video(2, "Night Train (Official Video)")
        val artistSong = video(3, "Across Town", "The Band - Topic")
        val repeatedSong = video(4, "Across Town (Official Audio)")
        val sharedTitleWords = video(5, "Night Train Home", "Other")
        val tooLong = video(6, "Night Train Interview").copy(durationSeconds = 3600)
        val unrelated = video(7, "Night Drive", "Other", 1_000_000)
        assertEquals("The Band", RelatedSongsPolicy.artist(artistSong))
        assertEquals(listOf(artistSong, sharedTitleWords),
            RelatedSongsPolicy.select(current, listOf(duplicateUrl, duplicateTitle, artistSong,
                repeatedSong, sharedTitleWords, tooLong, unrelated)))
        assertEquals(emptyList<VideoPreview>(),
            RelatedSongsPolicy.select(current.copy(url = "file:///local.flac"), listOf(artistSong)))
    }

    private fun video(id: Int, title: String, author: String = "The Band", views: Long? = null) =
        VideoPreview(title, author, "https://www.youtube.com/watch?v=id" + id.toString().padStart(9, '0'),
            "", 180L, views)
}
