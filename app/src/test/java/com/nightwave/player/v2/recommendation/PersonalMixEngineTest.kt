package com.nightwave.player.v2.recommendation

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalMixEngineTest {
    private fun video(id: String, author: String = "Artist") =
        VideoPreview(id, author, "https://www.youtube.com/watch?v=$id", "", 180L)
    private val a = video("dQw4w9WgXcQ")
    private val b = video("9bZkp7q19f0")
    private val c = video("JGwWNGJdvx8", "Another Artist")
    private val d = video("CevxZvSJLk8")

    @Test fun noUserDataNeverInventsSongs() {
        assertTrue(PersonalMixEngine.make(emptyList(), emptyList(), emptyList(), 123L).isEmpty())
    }

    @Test fun allSuggestionsComeFromActualSavedSources() {
        val mixes = PersonalMixEngine.make(listOf(a, b), listOf(b, c), listOf(d, a), 10L)
        val known = setOf(a.url, b.url, c.url, d.url)
        assertTrue(mixes.isNotEmpty())
        assertTrue(mixes.all { mix -> mix.tracks.isNotEmpty() &&
            mix.tracks.all { it.url in known } })
    }

    @Test fun sameInputsAndDayProduceIdenticalMixes() {
        val first = PersonalMixEngine.make(listOf(a, b), listOf(c, a), listOf(d), 200L)
        val second = PersonalMixEngine.make(listOf(a, b), listOf(c, a), listOf(d), 200L)
        assertEquals(first, second)
    }

    @Test fun noDuplicatesWithinAnyMix() {
        val mixes = PersonalMixEngine.make(listOf(a, a, b), listOf(a, c, c), listOf(b, d, d), 100L)
        assertTrue(mixes.all { mix ->
            mix.tracks.map { it.url }.distinct().size == mix.tracks.size
        })
    }

    @Test fun recentReplayMaintainsListeningOrder() {
        // Distinct pool contents ensure Replay is shown, not deduplicated away.
        val mixes = PersonalMixEngine.make(listOf(a, d), listOf(c, b),
            listOf(a, d), 1L)
        val replay = mixes.single { it.key == "replay" }
        assertEquals(listOf(c, b), replay.tracks)
    }

    @Test fun invalidSourcesAreFiltered() {
        val fake = video("not-valid!")
        val mixes = PersonalMixEngine.make(listOf(fake, a), listOf(fake), emptyList(), 4L)
        assertTrue(mixes.flatMap { it.tracks }.all { it.url == a.url })
        assertFalse(mixes.isEmpty())
    }

    @Test fun boundedCollectionsAndMixes() {
        val input = (1..40).map {
            video("abCD1234" + it.toString().padStart(3, '0'))
        }
        val mixes = PersonalMixEngine.make(input, input, input, 55L)
        assertTrue(mixes.size <= 3)
        assertTrue(mixes.all { it.tracks.size <= 12 })
    }
}
