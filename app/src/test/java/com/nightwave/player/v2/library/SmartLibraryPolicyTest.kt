package com.nightwave.player.v2.library

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartLibraryPolicyTest {
    private fun track(name: String, creator: String, seconds: Long, id: Int) =
        VideoPreview(name, creator, "https://www.youtube.com/watch?v=" +
            id.toString().padStart(11, 'a'), "", seconds)

    @Test fun emptyLibraryCreatesNoFakeCollections() {
        assertTrue(SmartLibraryPolicy.generate(emptyList(), emptyList(), emptyList()).isEmpty())
    }

    @Test fun groupsOnlyActualSavedMetadataAndNoFakeGenres() {
        val a = track("One", "Creator A", 120L, 1)
        val b = track("Two", "Creator A", 290L, 2)
        val c = track("Three", "Creator B", 150L, 3)
        val smart = SmartLibraryPolicy.generate(listOf(a), listOf(b), listOf(listOf(c)))
        assertTrue(smart.any { it.title.startsWith("Creator mix") &&
            it.songs.map(VideoPreview::url).containsAll(listOf(a.url, b.url)) })
        assertFalse(smart.any { it.title.contains("Genre", ignoreCase = true) })
    }

    @Test fun respectsDeduplicationAndDoesNotMutateOriginalPlaylists() {
        val a = track("One", "Creator", 240L, 1)
        val source = mutableListOf(a, a)
        val before = source.toList()
        val smart = SmartLibraryPolicy.generate(source, listOf(a), listOf(source))
        assertEquals(before, source)
        assertTrue(smart.all { c -> c.songs.distinctBy { it.url }.size == c.songs.size })
    }

    @Test fun recentAndLongListenCollectionsRepresentActualData() {
        val a = track("Short", "A", 99L, 1)
        val b = track("Long", "B", 300L, 2)
        val c = track("Longer", "C", 540L, 3)
        val smart = SmartLibraryPolicy.generate(emptyList(), listOf(a, b, c), emptyList())
        assertEquals(3, smart.first { it.id.endsWith("/recent") }.songs.size)
        assertEquals(2, smart.first { it.id.endsWith("/long") }.songs.size)
    }

    @Test fun generatedCollectionCountRemainsBounded() {
        val many = (0..100).map { track("Song $it", "Creator ${it / 2}", 100L, it) }
        val smart = SmartLibraryPolicy.generate(many, many, listOf(many))
        assertTrue(smart.size <= SmartLibraryPolicy.MAX_COLLECTIONS)
        assertTrue(smart.all { SmartLibraryPolicy.belongs(it.id) })
    }

    @Test fun longListenUsesActualDurationSecondsFieldAndFourMinuteBoundary() {
        val before = track("Before threshold", "Artist", 239L, 80)
        val exact = track("At threshold", "Artist", 240L, 81)
        val longer = track("After threshold", "Artist", 241L, 82)
        val collections = SmartLibraryPolicy.generate(
            listOf(before, exact, longer), emptyList(), emptyList())
        val longListens = collections.single { it.id.endsWith("/long") }
        assertEquals(listOf(exact.url, longer.url), longListens.songs.map { it.url })
    }

    @Test fun unknownOrNegativeDurationCannotBeCalledLongMusic() {
        val unknown = track("Unknown", "A", 0L, 90)
        val invalid = track("Invalid", "A", -1L, 91)
        val valid = track("Real", "A", 240L, 92)
        val collections = SmartLibraryPolicy.generate(
            listOf(unknown, invalid, valid), emptyList(), emptyList())
        assertFalse(collections.any { it.id.endsWith("/long") })
    }

}
