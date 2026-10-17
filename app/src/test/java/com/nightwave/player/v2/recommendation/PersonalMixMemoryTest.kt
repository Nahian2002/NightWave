package com.nightwave.player.v2.recommendation

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.insights.RecordedTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalMixMemoryTest {
    private fun video(id: String): VideoPreview =
        VideoPreview("Song $id", "Artist", "https://www.youtube.com/watch?v=$id", "", 150L)

    private val a = video("dQw4w9WgXcQ")
    private val b = video("9bZkp7q19f0")
    private val c = video("JGwWNGJdvx8")

    private fun signal(track: VideoPreview, ms: Long, plays: Int) =
        RecordedTrack(track.url, track.title, track.author, ms, plays, 12345L)

    @Test fun historyIsCompletelyIgnoredWhenNotOptedIn() {
        val inputs = listOf(a, b, c)
        val learned = listOf(signal(b, 6_000_000L, 200))
        val baseline = PersonalMixEngine.make(listOf(a), listOf(c), inputs, 100L)
        val denied = PersonalMixEngine.make(listOf(a), listOf(c), inputs, 100L,
            learned, memoryEnabled = false)
        assertEquals(baseline, denied)
        assertFalse(denied.any { it.key == "memory" })
    }

    @Test fun realMemoryCanReorderOnlyExistingSavedCatalogTracks() {
        val inputs = listOf(a, b, c)
        val learned = listOf(signal(b, 1_800_000L, 5))
        val mixes = PersonalMixEngine.make(listOf(a), listOf(c), inputs, 100L,
            learned, memoryEnabled = true)
        assertEquals(b.url, mixes.first().tracks.first().url)
        val allowed = inputs.map { it.url }.toSet()
        assertTrue(mixes.flatMap { it.tracks }.all { it.url in allowed })
        assertTrue(mixes.any { it.key == "memory" })
    }

    @Test fun unsavedHistoryCannotIntroduceASecretOrInventedSong() {
        val alien = video("CevxZvSJLk8")
        val mixes = PersonalMixEngine.make(listOf(a), listOf(c), listOf(a, c),
            101L, listOf(signal(alien, 9_999_000L, 900)), memoryEnabled = true)
        assertTrue(mixes.flatMap { it.tracks }.all { it.url in setOf(a.url, c.url) })
        assertFalse(mixes.any { it.tracks.any { song -> song.url == alien.url } })
    }

    @Test fun optInWithoutPriorRecordedSessionsMatchesOriginalMixes() {
        val baseline = PersonalMixEngine.make(listOf(a, b), listOf(c),
            listOf(a, b, c), 11L)
        val opted = PersonalMixEngine.make(listOf(a, b), listOf(c),
            listOf(a, b, c), 11L, emptyList(), true)
        assertEquals(baseline, opted)
    }

    @Test fun deterministicMemoryRankForSameDayAndInputs() {
        val saved = listOf(a, b, c)
        val history = listOf(signal(b, 600_000L, 3), signal(c, 125_000L, 1))
        val first = PersonalMixEngine.make(saved, saved, saved, 25L, history, true)
        val second = PersonalMixEngine.make(saved, saved, saved, 25L, history, true)
        assertEquals(first, second)
    }
}
