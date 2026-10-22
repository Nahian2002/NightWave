package com.nightwave.player.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicQueueIntentPolicyTest {
    private val a = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val b = "https://www.youtube.com/watch?v=9bZkp7q19f0"
    private val c = "https://www.youtube.com/watch?v=J---aiyznGQ"
    private fun song(url: String) = QueueEntry(url, "Title", "Creator", "")

    @Test fun invalidRowBeforeSelectionDoesNotShiftToDifferentSong() {
        val raw = listOf("https://evil.example/stream", a, b)
        val kept = listOf(song(a), song(b))
        assertEquals(1, PublicQueueIntentPolicy.retainedSelectionIndex(raw, 2, kept))
    }

    @Test fun duplicatedRawRowsStillSelectTheIntendedIdentity() {
        val raw = listOf(a, a, b)
        val kept = listOf(song(a), song(a), song(b))
        assertEquals(2, PublicQueueIntentPolicy.retainedSelectionIndex(raw, 2, kept))
        assertEquals(0, PublicQueueIntentPolicy.retainedSelectionIndex(raw, 1, kept))
    }

    @Test fun invalidSelectedUrlCannotPlayUnrelatedValidEntry() {
        val invalid = a + "&sig=secret"
        assertNull(PublicQueueIntentPolicy.retainedSelectionIndex(
            listOf(a, invalid, b), 1, listOf(song(a), song(b))))
        assertNull(PublicQueueIntentPolicy.retainedSelectionIndex(
            listOf(a, "https://www.youtube.com/watch?v=short"), 1, listOf(song(a))))
    }

    @Test fun outOfBoundsSelectionIsRejectedInsteadOfClampedToWrongSong() {
        val raw = listOf(a, b)
        val kept = listOf(song(a), song(b))
        assertNull(PublicQueueIntentPolicy.retainedSelectionIndex(raw, -1, kept))
        assertNull(PublicQueueIntentPolicy.retainedSelectionIndex(raw, 999, kept))
    }

    @Test fun selectedRowDroppedByLimitIsNotSubstituted() {
        val raw = listOf(a, b, c)
        assertNull(PublicQueueIntentPolicy.retainedSelectionIndex(raw, 2, listOf(song(a), song(b))))
    }

    @Test fun canonicalSelectionIsUnchanged() {
        val raw = listOf(a, b, c)
        assertEquals(2, PublicQueueIntentPolicy.retainedSelectionIndex(
            raw, 2, raw.map(::song)))
    }
}
