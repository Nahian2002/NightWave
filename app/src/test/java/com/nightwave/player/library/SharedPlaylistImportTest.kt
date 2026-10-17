package com.nightwave.player.library

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPlaylistImportTest {
    private fun video(id: String) = VideoPreview("Track", "Creator",
        "https://www.youtube.com/watch?v=" + id, "", 0L)
    private val a = video("dQw4w9WgXcQ")
    private val b = video("9bZkp7q19f0")
    private fun empty() = LibraryCollections(emptyList(), emptyList(), emptyList())

    @Test fun createsOneWholeOrderedPlaylist() {
        val result = LibraryMutationPlan.apply(empty(),
            LibraryChange.ImportSharedPlaylist("Road", listOf(a, b)))
        assertTrue(result.accepted && result.changed)
        assertEquals("Road", result.updated.playlists.single().first)
        assertEquals(listOf(a.url, b.url),
            result.updated.playlists.single().second.map { it.url })
    }

    @Test fun repeatedImportDoesNotMultiplyPlaylists() {
        val first = LibraryMutationPlan.apply(empty(),
            LibraryChange.ImportSharedPlaylist("Road", listOf(a)))
        val next = LibraryMutationPlan.apply(first.updated,
            LibraryChange.ImportSharedPlaylist("Road", listOf(a)))
        assertTrue(next.accepted)
        assertFalse(next.changed)
        assertEquals(1, next.updated.playlists.size)
    }

    @Test fun nameCollisionKeepsOriginalAndImportsSharedCopy() {
        val old = empty().copy(playlists = listOf("Road" to listOf(b)))
        val result = LibraryMutationPlan.apply(old,
            LibraryChange.ImportSharedPlaylist("Road", listOf(a)))
        assertEquals("Road (Shared)", result.updated.playlists.last().first)
        assertEquals(listOf(b), result.updated.playlists.first().second)
    }

    @Test fun multipleCollisionsAllocateNewSuffix() {
        val old = empty().copy(playlists = listOf(
            "Road" to listOf(b), "Road (Shared)" to listOf(b)))
        val result = LibraryMutationPlan.apply(old,
            LibraryChange.ImportSharedPlaylist("Road", listOf(a)))
        assertEquals("Road (Shared 2)", result.updated.playlists.last().first)
        assertEquals(3, result.updated.playlists.size)
    }

    @Test fun sameSharedCopyIsIdempotentAfterCollision() {
        val old = empty().copy(playlists = listOf(
            "Road" to listOf(b), "Road (Shared)" to listOf(a)))
        val result = LibraryMutationPlan.apply(old,
            LibraryChange.ImportSharedPlaylist("Road", listOf(a)))
        assertTrue(result.accepted)
        assertFalse(result.changed)
    }

    @Test fun malformedBatchCannotPartiallyMutateCollection() {
        val old = empty()
        val inputs = listOf(listOf(a, a), listOf(a, a.copy(url = "offline:private")),
            emptyList())
        for (batch in inputs) {
            val result = LibraryMutationPlan.apply(old,
                LibraryChange.ImportSharedPlaylist("Road", batch))
            assertFalse(result.accepted)
            assertEquals(old, result.updated)
        }
    }

    @Test fun enforcesThirtySongLimit() {
        val input = (1..31).map { video("abCD1234" + it.toString().padStart(3, '0')) }
        val result = LibraryMutationPlan.apply(empty(),
            LibraryChange.ImportSharedPlaylist("Too many", input))
        assertFalse(result.accepted)
        assertTrue(result.updated.playlists.isEmpty())
    }

    @Test fun fullLibraryIsLeftUnchanged() {
        val old = empty().copy(playlists =
            (1..50).map { ("Saved" + it) to emptyList<VideoPreview>() })
        val result = LibraryMutationPlan.apply(old,
            LibraryChange.ImportSharedPlaylist("Road", listOf(a)))
        assertFalse(result.accepted)
        assertEquals(old, result.updated)
    }

    @Test fun favoritesAndRecentAndPreviousPlaylistsStayUnchanged() {
        val old = LibraryCollections(listOf(a), listOf(b), listOf("Existing" to listOf(a)))
        val result = LibraryMutationPlan.apply(old,
            LibraryChange.ImportSharedPlaylist("New", listOf(b)))
        assertTrue(result.accepted)
        assertEquals(old.favorites, result.updated.favorites)
        assertEquals(old.recent, result.updated.recent)
        assertEquals(old.playlists, result.updated.playlists.take(1))
    }

    @Test fun unsafeOrTooLongNamesAreRejected() {
        val before = empty()
        for (name in listOf("", " ", "bad\nname", "x".repeat(61))) {
            val result = LibraryMutationPlan.apply(before,
                LibraryChange.ImportSharedPlaylist(name, listOf(a)))
            assertFalse(result.accepted)
            assertEquals(before, result.updated)
        }
    }
}
