package com.nightwave.player.net

import com.nightwave.player.offline.OfflineSong
import com.nightwave.player.v2.data.room.LegacyLibrarySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchTypingQueryTest {
    @Test fun completesOneWordAtATimeAndRespectsAnExplicitWordBoundary() {
        val labels = listOf("Blue Moon Rising", "Blue Moon", "Blue Eyes", "Blueberry Sky")
        assertEquals(listOf("Blue"), SearchTypingQuery.parse("Bl")!!.completions(labels.take(3)))
        assertEquals(listOf("Blue Moon", "Blue Eyes"), SearchTypingQuery.parse("blue ")!!.completions(labels))
        assertEquals(listOf("Blue Moon"), SearchTypingQuery.parse("blue mo")!!.completions(labels))
        assertEquals(listOf("Blue Moon Rising"), SearchTypingQuery.parse("Blue Moon ")!!.completions(labels))
    }

    @Test fun preservesBanglaWordsAndUsesHistoryWithoutALoadedLibrary() = runBlocking {
        val preview = LocalMusicQueryRunner.preview(null, "নীল ", listOf("নীল আকাশ আজ"))
        assertEquals(listOf("নীল আকাশ"), preview!!.completions)
        assertEquals(emptyList<VideoPreview>(), preview.saved)
        assertNull(LocalMusicQueryRunner.preview(null, "https://youtu.be/dQw4w9WgXcQ", listOf("Blue")))
    }

    @Test fun shortPrefixesPreviewActualSavedAndImportedTracks() = runBlocking {
        val tracks = (0..4).map { NW129SearchFixture.track(it, "Blue Moon $it", "Moonlight") }
        val exact = tracks.last().copy(title = "Blue Moon")
        val unrelated = NW129SearchFixture.track(9, "Bumblebee")
        val offline = OfflineSong("a.mp3", "Blue Morning", "audio/mpeg", 100)
        val snapshot = LocalMusicSnapshot(LegacyLibrarySnapshot(
            tracks + exact + unrelated, emptyList(), emptyList(), "fixture"), listOf(offline))
        val index = LocalMusicSearchIndex.build(snapshot)
        val partial = LocalMusicQueryRunner.preview(index, "Blue Mo", emptyList())!!
        assertEquals("Blue Mo", partial.query)
        assertEquals(listOf(tracks[0], tracks[1], tracks[2]), partial.saved)
        assertEquals(listOf(offline), partial.offline)
        assertEquals(listOf("Blue Moon", "Blue Morning"), partial.completions)

        val completed = LocalMusicQueryRunner.preview(index, "Blue Moon", emptyList())!!
        assertEquals(exact, completed.saved.first())
        assertEquals(completed.saved.size, completed.saved.distinctBy { it.url }.size)
        assertEquals(emptyList<VideoPreview>(), index.search("Blue Mo").saved)
    }
}
