package com.nightwave.player.v2.insights

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningInsightsTest {
    private fun song(id: String, artist: String) =
        VideoPreview(id, artist, "https://www.youtube.com/watch?v=$id", "", 100L)

    @Test fun noListeningHistoryMeansNoFakePlaysOrTime() {
        val data = ListeningInsightsEngine.calculate(emptyList(), emptyList(), emptyList())
        assertEquals(0, data.recentEntries)
        assertEquals(0, data.uniqueRecentTracks)
        assertTrue(data.creators.isEmpty())
    }
    @Test fun countsUniqueTracksAndDistinctCreators() {
        val a = song("dQw4w9WgXcQ", "Artist A")
        val b = song("9bZkp7q19f0", "Artist B")
        val stats = ListeningInsightsEngine.calculate(listOf(a, a, b),
            listOf(a, a), listOf("Mix", "Mix"))
        assertEquals(3, stats.recentEntries)
        assertEquals(2, stats.uniqueRecentTracks)
        assertEquals(2, stats.distinctRecentCreators)
        assertEquals(1, stats.likedTracks)
        assertEquals(1, stats.playlists)
    }
    @Test fun unknownArtistsNotPresentedAsMeaningfulCreators() {
        val a = song("dQw4w9WgXcQ", "Unknown")
        val result = ListeningInsightsEngine.calculate(listOf(a), emptyList(), emptyList())
        assertEquals(0, result.distinctRecentCreators)
        assertTrue(result.creators.isEmpty())
    }
    @Test fun repeatedCreatorListsDistinctSavedTracksNotPlays() {
        val a = song("dQw4w9WgXcQ", "A")
        val b = song("9bZkp7q19f0", "A")
        val insights = ListeningInsightsEngine.calculate(listOf(a, b, a),
            emptyList(), emptyList())
        assertEquals(2, insights.creators.first().distinctRecentTracks)
    }
    @Test fun topCreatorListIsLimited() {
        val tracks = (1..20).map { song("abcde" + it.toString().padStart(6, '0'), "Artist $it") }
        assertEquals(3, ListeningInsightsEngine.calculate(tracks, emptyList(), emptyList()).creators.size)
    }
}
