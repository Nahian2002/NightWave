package com.nightwave.player.v2.recommendation

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.insights.RecordedTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDailyMixEngineTest {
    private fun video(id: Int, author: String = "Creator ${id % 5}") =
        VideoPreview("Song $id", author,
            "https://www.youtube.com/watch?v=" + id.toString().padStart(11, '0'),
            "", 180L)

    @Test fun noSavedMusicNeverInventsSongs() {
        assertTrue(LocalDailyMixEngine.make(
            emptyList(), emptyList(), emptyList(), 10L).isEmpty())
    }

    @Test fun tinyLibraryDoesNotCreateFakeDailyMixes() {
        assertTrue(LocalDailyMixEngine.make(
            listOf(video(1), video(2)), emptyList(), emptyList(), 10L).isEmpty())
        val one = LocalDailyMixEngine.make(
            (1..3).map(::video), emptyList(), emptyList(), 10L)
        assertEquals(1, one.size)
        assertEquals(3, one.first().tracks.size)
    }

    @Test fun sixRealTracksYieldTwoNonOverlappingDailyMixes() {
        val saved = (1..6).map(::video)
        val mixes = LocalDailyMixEngine.make(saved, emptyList(), emptyList(), 10L)
        assertEquals(listOf("Daily Mix 1", "Daily Mix 2"), mixes.map { it.title })
        assertEquals(6, mixes.flatMap { it.tracks }.map { it.url }.distinct().size)
    }

    @Test fun nineTracksYieldThreeDisjointMixedCollections() {
        val saved = (1..9).map(::video)
        val mixes = LocalDailyMixEngine.make(saved, saved.take(3), emptyList(), 10L)
        assertEquals(3, mixes.size)
        assertEquals(9, mixes.flatMap { it.tracks }.map { it.url }.distinct().size)
        assertTrue(mixes.all { it.tracks.size == 3 })
    }

    @Test fun allRecommendationsAreRealInputVideosAndNeverDuplicatedWithinDailyMixes() {
        val saved = (1..32).map(::video)
        val mixes = LocalDailyMixEngine.make(
            saved.take(15), saved.drop(10).take(10), saved.drop(20), 4444L)
        val known = saved.mapTo(hashSetOf()) { it.url }
        val urls = mixes.flatMap { it.tracks }.map { it.url }
        assertEquals(urls.distinct().size, urls.size)
        assertTrue(urls.all { it in known })
        assertTrue(mixes.all { it.tracks.size <= LocalDailyMixEngine.MAX_PER_MIX })
    }

    @Test fun sameDayAndSameInputsYieldIdenticalMixes() {
        val saved = (1..27).map(::video)
        val first = LocalDailyMixEngine.make(saved, saved.take(5), saved.drop(10), 3000L)
        val second = LocalDailyMixEngine.make(saved, saved.take(5), saved.drop(10), 3000L)
        assertEquals(first, second)
    }

    @Test fun daysRotateEquallyRankedSelections() {
        val saved = (1..24).map { video(it, "Same Creator") }
        val positions = (1000L..1010L).map { day ->
            LocalDailyMixEngine.make(saved, emptyList(), emptyList(), day)
                .first().tracks.map { it.url }
        }.toSet()
        assertTrue(positions.size > 1)
    }

    @Test fun disabledListeningMemoryIsNeverUsedAsSignal() {
        val saved = (1..30).map(::video)
        val track = saved.last()
        val privateTrack = RecordedTrack(track.url, track.title, track.author,
            3_600_000L, 150, 600_000L)
        val noMemory = LocalDailyMixEngine.make(saved, emptyList(),
            emptyList(), 12345L)
        val disabled = LocalDailyMixEngine.make(saved, emptyList(),
            emptyList(), 12345L, listOf(privateTrack), memoryEnabled = false)
        assertEquals(noMemory, disabled)
        assertFalse(disabled.any { it.reason.contains("Playback history enabled") })
    }

    @Test fun optedInHistoryMayReorderOnlyKnownCatalogSongs() {
        val saved = (1..30).map(::video)
        val signal = RecordedTrack(saved.last().url, saved.last().title,
            saved.last().author, 6_000_000L, 30, 500_000L)
        val outside = RecordedTrack(video(999).url, "Unknown", "Unknown",
            99_000_000L, 1_000, 500_000L)
        val enabled = LocalDailyMixEngine.make(saved, emptyList(),
            emptyList(), 333L, listOf(signal, outside), memoryEnabled = true)
        assertTrue(enabled.isNotEmpty())
        assertTrue(enabled.any { it.reason.contains("Playback history enabled") })
        assertTrue(enabled.flatMap { it.tracks }.all { it.url in saved.map(VideoPreview::url) })
    }

    @Test fun fakeVideoLinksAndDuplicateInputsAreRejected() {
        val valid = (1..12).map(::video)
        val invalid = VideoPreview("Fake", "Unknown", "https://invalid.invalid/nope", "", 0L)
        val mixes = LocalDailyMixEngine.make(valid + invalid + valid,
            listOf(invalid), listOf(valid, valid), 333L)
        val output = mixes.flatMap { it.tracks }
        assertTrue(output.isNotEmpty())
        assertFalse(output.any { it.url == invalid.url })
        assertEquals(output.size, output.distinctBy { it.url }.size)
    }

    @Test fun sourceListsRemainUnchangedAfterGeneration() {
        val favorites = (1..12).map(::video).toMutableList()
        val recent = favorites.take(5).toMutableList()
        val beforeFavorites = favorites.toList()
        val beforeRecent = recent.toList()
        LocalDailyMixEngine.make(favorites, recent, listOf(favorites), 500L)
        assertEquals(beforeFavorites, favorites)
        assertEquals(beforeRecent, recent)
    }
}
