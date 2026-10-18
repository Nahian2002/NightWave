package com.nightwave.player.v2.library

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQuickPlayPolicyTest {
    private fun song(n: Int) = VideoPreview(
        "Saved $n", "Creator",
        "https://www.youtube.com/watch?v=" + "v" + n.toString().padStart(10, '0'),
        "art$n", 210L
    )

    @Test fun emptyCollectionDoesNotAutoplay() {
        val plan = LibraryQuickPlayPolicy.plan(emptyList(), false, 0L)
        assertNull(plan.first)
        assertTrue(plan.songs.isEmpty())
    }

    @Test fun savedOrderPlayStartsWithFirstVisibleEntry() {
        val source = listOf(song(8), song(2), song(4))
        val plan = LibraryQuickPlayPolicy.plan(source, false, 42L)
        assertEquals(source, plan.songs)
        assertEquals(source.first(), plan.first)
    }

    @Test fun sortedOrFilteredViewOrderIsNotRewritten() {
        val saved = listOf(song(3), song(1), song(2))
        val visible = saved.sortedBy { it.title }
        val plan = LibraryQuickPlayPolicy.plan(visible, false, 0L)
        assertEquals(visible, plan.songs)
        assertEquals(3, saved.first().title.substringAfterLast(" ").toInt())
    }

    @Test fun oneItemShuffleKeepsSameSong() {
        val original = song(1)
        assertEquals(listOf(original),
            LibraryQuickPlayPolicy.plan(listOf(original), true, 99L).songs)
    }

    @Test fun shuffleIsReproducibleForUnitTestSeed() {
        val input = (0..50).map(::song)
        val a = LibraryQuickPlayPolicy.plan(input, true, 2026L)
        val b = LibraryQuickPlayPolicy.plan(input, true, 2026L)
        assertEquals(a.songs, b.songs)
    }

    @Test fun shuffleReturnsOnlyRealOriginalEntries() {
        val input = (0..50).map(::song)
        val shuffled = LibraryQuickPlayPolicy.plan(input, true, 98L)
        assertEquals(input.toSet(), shuffled.songs.toSet())
        assertEquals(input.size, shuffled.songs.size)
    }

    @Test fun shuffleDoesNotModifySavedOrder() {
        val input = (0..50).map(::song)
        val first = input.first()
        LibraryQuickPlayPolicy.plan(input, true, 1L)
        assertEquals(first, input.first())
    }

    @Test fun queueNeverExceedsServiceCapacity() {
        val tracks = (0 until 300).map(::song)
        val plan = LibraryQuickPlayPolicy.plan(tracks, false, 1L)
        assertEquals(100, plan.songs.size)
        assertEquals(300, plan.totalPlayable)
        assertTrue(plan.truncated)
    }

    @Test fun shuffleQueueAlsoNeverExceedsServiceCapacity() {
        val plan = LibraryQuickPlayPolicy.plan((0 until 300).map(::song), true, 2L)
        assertEquals(100, plan.songs.size)
        assertEquals(300, plan.totalPlayable)
    }

    @Test fun exactlyOneHundredDoesNotClaimTruncation() {
        val plan = LibraryQuickPlayPolicy.plan((0 until 100).map(::song), false, 1L)
        assertFalse(plan.truncated)
    }

    @Test fun duplicateSavedUrlsAppearOnlyOnce() {
        val repeated = song(1)
        val plan = LibraryQuickPlayPolicy.plan(listOf(repeated, repeated, song(2)), false, 0L)
        assertEquals(listOf(repeated, song(2)), plan.songs)
        assertEquals(2, plan.totalPlayable)
    }

    @Test fun offlinePrivateFileIsNeverQueuedForPublicStream() {
        val local = VideoPreview("Local", "NightWave", "offline:123", "", 10L)
        assertEquals(listOf(song(1)),
            LibraryQuickPlayPolicy.plan(listOf(local, song(1)), false, 0L).songs)
    }

    @Test fun deceptiveHostIsNotTrustedAsPublicYouTube() {
        val fake = song(1).copy(url = "https://www.youtube.com.evil.invalid/watch?v=abc12345678")
        assertTrue(LibraryQuickPlayPolicy.plan(listOf(fake), false, 0L).songs.isEmpty())
    }

    @Test fun extraTrackingParametersAreNotPersistedOrQueued() {
        val changed = song(1).copy(url = song(1).url + "&t=1")
        assertTrue(LibraryQuickPlayPolicy.plan(listOf(changed), false, 0L).songs.isEmpty())
    }

    @Test fun retainsExactVideoObjectAndPlayUrl() {
        val original = song(5)
        val plan = LibraryQuickPlayPolicy.plan(listOf(original), false, 4L)
        assertEquals(original, plan.first)
        assertEquals(original.url, plan.first?.url)
    }

    @Test fun noSongIsInferredForInvalidInputs() {
        val junk = VideoPreview("", "", "", "", 0L)
        val plan = LibraryQuickPlayPolicy.plan(listOf(junk), true, 12L)
        assertTrue(plan.songs.isEmpty())
        assertNull(plan.first)
    }
}
