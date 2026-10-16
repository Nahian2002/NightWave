package com.nightwave.player.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchInputResolverTest {
    @Test fun keywordsAreNotInterpretedAsLinks() {
        listOf("shape of you", "lofi hip hop", "আকাশ", "Adele hello", "BLACKPINK - how you like that").forEach { query ->
            assertEquals(SearchInput.Keywords(query), SearchInputResolver.resolve(query))
        }
        assertEquals(SearchInput.Keywords("song name"), SearchInputResolver.resolve("  song name  "))
    }

    @Test fun directVideoLinksStillWork() {
        assertEquals(
            SearchInput.VideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            SearchInputResolver.resolve("https://youtu.be/dQw4w9WgXcQ")
        )
    }

    @Test fun invalidLinksAndEmptyInputProduceErrors() {
        assertEquals(SearchInput.Blank, SearchInputResolver.resolve("   "))
        assertEquals(SearchInput.BadLink, SearchInputResolver.resolve("https://youtube.com/watch?v=bad"))
    }
}
