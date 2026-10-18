package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsEditorSearchPolicyTest {
    private val original = LyricsEditorPolicy.begin(TimedLyricsCodec.parse(
        "[00:01.000]Hello World\n" +
            "[00:02.500]বৃষ্টি নামে\n" +
            "[00:03.000]HELLO again\n" +
            "[00:04.750]Last line".toByteArray(Charsets.UTF_8)
    ))

    @Test fun emptyAndWhitespaceQueriesShowNoMatches() {
        assertTrue(LyricsEditorSearchPolicy.matches(original, "").isEmpty())
        assertTrue(LyricsEditorSearchPolicy.matches(original, "   ").isEmpty())
    }

    @Test fun caseInsensitiveSearchReturnsOrderedCueIndices() {
        assertEquals(listOf(0, 2), LyricsEditorSearchPolicy.matches(original, "hello"))
        assertEquals(listOf(0, 2), LyricsEditorSearchPolicy.matches(original, "HELLO"))
    }

    @Test fun bengaliTextMatchesWithoutTransliteratingLyrics() {
        assertEquals(listOf(1), LyricsEditorSearchPolicy.matches(original, "বৃষ্টি"))
        assertEquals("বৃষ্টি নামে", original.cues[1].text)
    }

    @Test fun literalPunctuationNeverRunsAsRegularExpression() {
        assertEquals(emptyList<Int>(), LyricsEditorSearchPolicy.matches(original, "(.*)"))
        assertEquals(listOf(3), LyricsEditorSearchPolicy.matches(original, "last"))
    }

    @Test fun searchCanJumpToTimestampPrefixWhenRequested() {
        assertEquals(listOf(1), LyricsEditorSearchPolicy.matches(original, "00:02"))
        assertEquals(listOf(3), LyricsEditorSearchPolicy.matches(original, "04:750"))
    }

    @Test fun whitespaceRunsAreNormalizedForMatching() {
        val draft = LyricsEditorPolicy.updateText(original, 0, "Hello   World")
        assertEquals(listOf(0), LyricsEditorSearchPolicy.matches(draft, "  hello world "))
    }

    @Test fun oversizedQueryRejectedWithoutChangingTheDraft() {
        val before = original.cues.toList()
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorSearchPolicy.matches(original, "x".repeat(81))
        }
        assertEquals(before, original.cues)
    }

    @Test fun noMatchingResultsNeverCauseNavigationOrSeek() {
        assertNull(LyricsEditorSearchPolicy.nearest(emptyList(), 0, true))
        assertNull(LyricsEditorSearchPolicy.nearest(emptyList(), 0, false))
        assertNull(LyricsEditorSearchPolicy.ordinal(emptyList(), 0))
    }

    @Test fun nextMatchWrapsAfterFinalResult() {
        val hits = listOf(0, 2)
        assertEquals(2, LyricsEditorSearchPolicy.nearest(hits, 0, true))
        assertEquals(0, LyricsEditorSearchPolicy.nearest(hits, 2, true))
        assertEquals(0, LyricsEditorSearchPolicy.nearest(hits, -1, true))
    }

    @Test fun previousMatchWrapsBeforeInitialResult() {
        val hits = listOf(0, 2)
        assertEquals(2, LyricsEditorSearchPolicy.nearest(hits, 0, false))
        assertEquals(0, LyricsEditorSearchPolicy.nearest(hits, 2, false))
        assertEquals(2, LyricsEditorSearchPolicy.nearest(hits, 500, false))
    }

    @Test fun searchOrdinalOnlyExistsForSelectedMatchingCue() {
        val hits = listOf(0, 2)
        assertEquals(1, LyricsEditorSearchPolicy.ordinal(hits, 0))
        assertEquals(2, LyricsEditorSearchPolicy.ordinal(hits, 2))
        assertNull(LyricsEditorSearchPolicy.ordinal(hits, 1))
    }

    @Test fun malformedNonAscendingIndicesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorSearchPolicy.nearest(listOf(2, 2), 1, true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorSearchPolicy.nearest(listOf(3, 1), 1, false)
        }
    }

    @Test fun aTextEditImmediatelyChangesMatchingResults() {
        val edited = LyricsEditorPolicy.updateText(original, 2, "New words")
        assertEquals(listOf(0), LyricsEditorSearchPolicy.matches(edited, "hello"))
        assertEquals(listOf(0, 2), LyricsEditorSearchPolicy.matches(original, "hello"))
    }

    @Test fun cueReorderUsesCurrentIndexNotStaleOldPosition() {
        val moved = LyricsEditorPolicy.updateTime(original, 2, "00:00.500")
        assertEquals(listOf(0, 1), LyricsEditorSearchPolicy.matches(moved, "hello"))
        assertEquals("HELLO again", moved.cues[0].text)
    }

    @Test fun doesNotModifyOrPersistSensitiveLyrics() {
        val originalCues = original.cues.toList()
        val matches = LyricsEditorSearchPolicy.matches(original, "hello")
        assertEquals(listOf(0, 2), matches)
        assertEquals(originalCues, original.cues)
    }

    @Test fun searchesAllAllowedTwelveHundredLines() {
        val longLrc = (0 until TimedLyricsCodec.MAX_LINES).joinToString("\n") { i ->
            val min = i / 60
            val sec = i % 60
            "[%02d:%02d.000]".format(min, sec) +
                if (i == TimedLyricsCodec.MAX_LINES - 1) " Needle" else " Line"
        }.toByteArray(Charsets.UTF_8)
        val draft = LyricsEditorPolicy.begin(TimedLyricsCodec.parse(longLrc))
        assertEquals(listOf(1199), LyricsEditorSearchPolicy.matches(draft, "needle"))
    }
}
