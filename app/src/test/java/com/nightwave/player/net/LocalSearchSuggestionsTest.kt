package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSearchSuggestionsTest {
    private fun item(id: String, title: String, author: String = "Test Channel") =
        VideoPreview(title, author, "https://www.youtube.com/watch?v=$id", "", 210L)

    @Test fun noSuggestionsWithoutQueryOrSavedTracks() {
        val songs = listOf(item("aaaaaaa0001", "Midnight Sun"))
        assertTrue(LocalSearchSuggestions.suggest("", songs).isEmpty())
        assertTrue(LocalSearchSuggestions.suggest("!!!", songs).isEmpty())
        assertTrue(LocalSearchSuggestions.suggest("midnight", emptyList()).isEmpty())
    }

    @Test fun suggestionsAreFromRealSavedMetadata() {
        val songs = listOf(item("aaaaaaa0001", "Midnight Sun"),
            item("aaaaaaa0002", "Moonlight"))
        val suggestions = LocalSearchSuggestions.suggest("midn", songs)
        assertEquals("Midnight Sun", suggestions.first().label)
        assertEquals(LocalSearchSuggestion.Kind.SONG, suggestions.first().kind)
        assertFalse(suggestions.any { "Moonlight" == it.query })
    }

    @Test fun duplicatesDoNotMultiplySuggestions() {
        val songs = listOf(item("aaaaaaa0001", "Blue Moon"),
            item("aaaaaaa0002", "Blue Moon"), item("aaaaaaa0003", "Blue Moon"))
        val results = LocalSearchSuggestions.suggest("Blue", songs)
        assertEquals(1, results.count { it.kind == LocalSearchSuggestion.Kind.SONG })
    }

    @Test fun limitsAreSafeEvenWhenCallerAsksForMore() {
        val songs = (0..50).map { item("id${it}track", "Skyline $it") }
        assertTrue(LocalSearchSuggestions.suggest("Skyline", songs, 200).size <= 8)
        assertEquals(emptyList<LocalSearchSuggestion>(),
            LocalSearchSuggestions.suggest("Skyline", songs, -1))
    }

    @Test fun creatorSuggestionRequiresActualSavedCreatorMatch() {
        val songs = listOf(item("aaaaaaa0001", "Dreams", "Bright Star"),
            item("aaaaaaa0002", "Starlight", "Bright Star"),
            item("aaaaaaa0003", "Some Video", "Another Creator"))
        val found = LocalSearchSuggestions.suggest("Bright Star", songs)
        assertTrue(found.any { it.kind == LocalSearchSuggestion.Kind.CREATOR &&
            it.label == "Bright Star" && it.detail.contains("saved videos") })
    }

    @Test fun titleAndCreatorAreNotSilentlyRewrittenOrUploaded() {
        val title = "Beyoncé - Halo (Official Audio)"
        val track = item("aaaaaaa0001", title, "Beyoncé")
        LocalSearchSuggestions.suggest("halo", listOf(track))
        assertEquals(title, track.title)
        assertEquals("Beyoncé", track.author)
    }
}
