package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryPolicyTest {
    @Test fun typingOrBlankInputsCannotCreateHistory() {
        assertEquals(emptyList<String>(), SearchHistoryPolicy.record(emptyList(), "   "))
        assertEquals(emptyList<String>(), SearchHistoryPolicy.record(emptyList(), "bad\u0000query"))
    }

    @Test fun aRepeatSearchMovesToTopWithoutDuplication() {
        val first = SearchHistoryPolicy.record(emptyList(), " Midnight    City ")
        val second = SearchHistoryPolicy.record(first, "Dreams")
        val third = SearchHistoryPolicy.record(second, "midnight city")
        assertEquals(listOf("midnight city", "Dreams"), third)
    }

    @Test fun boundedAndSafeAgainstCorruptedStorage() {
        val many = (1..30).fold(emptyList<String>()) { list, i ->
            SearchHistoryPolicy.record(list, "Song $i")
        }
        assertEquals(SearchHistoryPolicy.MAX_ENTRIES, many.size)
        assertEquals("Song 30", many.first())
        assertEquals("Song 19", many.last())
        assertEquals(listOf("hello", "World"),
            SearchHistoryPolicy.sanitize(listOf("hello", "HELLO", "World")))
        assertEquals(160, SearchHistoryPolicy.normalize("a".repeat(999))!!.length)
        assertFalse(many.any { it == "Song 1" })
    }

    @Test fun removeAffectsOnlySelectedHistoryEntry() {
        val saved = listOf("First", "Second", "Third")
        assertEquals(listOf("First", "Third"), SearchHistoryPolicy.remove(saved, "SECOND"))
        assertEquals(saved, SearchHistoryPolicy.remove(saved, "Missing"))
        assertTrue(SearchHistoryPolicy.remove(emptyList(), "First").isEmpty())
    }
}
