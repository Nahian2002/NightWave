package com.nightwave.player.net

import org.junit.Assert.*
import org.junit.Test

class InvidiousResultsTest {
    private val raw = """
        [
          {"type":"video","videoId":"dQw4w9WgXcQ","title":"Baby shark","author":"Pinkfong","lengthSeconds":95},
          {"type":"playlist","playlistId":"PL000","title":"ignore"},
          {"type":"video","videoId":"dQw4w9WgXcQ","title":"duplicate"},
          {"type":"video","videoId":"badID","title":"not playable"},
          {"type":"video","videoId":"J---aiyznGQ","title":"Long track","author":"Creator","lengthSeconds":-4}
        ]
    """.trimIndent()

    @Test fun readsOnlyPublicVideosAndRemovesDuplicates() {
        val items = InvidiousResults.parse(raw)
        assertEquals(2, items.size)
        assertEquals("Baby shark", items[0].title)
        assertEquals("Pinkfong", items[0].author)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", items[0].url)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", items[0].thumbnail)
        assertEquals(95L, items[0].durationSeconds)
        assertEquals(0L, items[1].durationSeconds)
    }

    @Test fun refusesInvalidJsonInsteadOfShowingFakeResults() {
        assertThrows(org.json.JSONException::class.java) { InvidiousResults.parse("<html>blocked</html>") }
    }

    @Test fun emptyResultsDoNotPretendToBeTracks() {
        assertTrue(InvidiousResults.parse("[]").isEmpty())
    }
}
