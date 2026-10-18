package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsEditorPolicyTest {
    private val original = TimedLyricsCodec.parse(
        "[00:01.000]One\n[00:02.500]Two\n[00:04.000]Three".toByteArray()
    )
    private fun draft() = LyricsEditorPolicy.begin(original)

    @Test fun openingEditorDoesNotMutateOriginalLyrics() {
        val before = original.cues.toList()
        val edited = LyricsEditorPolicy.updateText(draft(), 1, "Updated")
        assertEquals(before, original.cues)
        assertEquals("Updated", edited.cues[1].text)
        assertTrue(LyricsEditorPolicy.changed(draft(), edited))
    }

    @Test fun nudgeTimestampInQuarterSecondIncrements() {
        val after = LyricsEditorPolicy.nudge(draft(), 1, -2)
        assertEquals(2000L, after.cues[1].timeMs)
        assertEquals("Two", after.cues[1].text)
    }

    @Test fun directTimestampInputSupportsOneToThreeFractionDigits() {
        assertEquals(62000L, LyricsEditorPolicy.parseTime("01:02.0"))
        assertEquals(62250L, LyricsEditorPolicy.parseTime("01:02.25"))
        assertEquals(62255L, LyricsEditorPolicy.parseTime("01:02.255"))
        assertEquals("01:02.255", LyricsEditorPolicy.timeLabel(62255L))
    }

    @Test fun editorNeverGeneratesTimestampOutsideSupportedMinutes() {
        val first = LyricsEditorPolicy.nudge(draft(), 0, Int.MIN_VALUE)
        assertEquals(0L, first.cues[0].timeMs)
        val last = LyricsEditorPolicy.nudge(draft(), 2, Int.MAX_VALUE)
        assertEquals(LyricsEditorPolicy.MAX_TIME_MS, last.cues.last().timeMs)
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.parseTime("100:00.000")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.parseTime("01:75.000")
        }
    }

    @Test fun changingTimeReordersCueSafely() {
        val moved = LyricsEditorPolicy.updateTime(draft(), 2, "00:00.500")
        assertEquals("Three", moved.cues[0].text)
        assertEquals(500L, moved.cues[0].timeMs)
    }

    @Test fun addingAtPlaybackDoesNotPersistUntilExplicitSave() {
        val originalDraft = draft()
        val withNew = LyricsEditorPolicy.add(originalDraft, 3000L)
        assertEquals(3, originalDraft.cues.size)
        assertEquals(4, withNew.cues.size)
        assertEquals("New lyric line", withNew.cues[2].text)
    }

    @Test fun duplicateNewCueAtSamePositionFailsClosed() {
        val once = LyricsEditorPolicy.add(draft(), 3000L)
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.add(once, 3000L)
        }
    }

    @Test fun deletingSingleCueIsNotAllowed() {
        val single = LyricsEditorPolicy.begin(
            TimedLyricsCodec.parse("[00:01.000]Only".toByteArray()))
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.delete(single, 0)
        }
        assertEquals(2, LyricsEditorPolicy.delete(draft(), 1).cues.size)
    }

    @Test fun noSilentTextTruncationOrLossOfUnicode() {
        val withUnicode = LyricsEditorPolicy.updateText(draft(), 0, "  বৃষ্টি  ")
        assertEquals("বৃষ্টি", withUnicode.cues[0].text)
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.updateText(draft(), 0, "X".repeat(221))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.updateText(draft(), 0, "   ")
        }
    }

    @Test fun bracketedTimestampsInsideLyricsCannotCreateHiddenExtraCues() {
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.updateText(draft(), 0, "One [00:03.000] hidden")
        }
    }

    @Test fun serializedEditorDraftIsStrictLrcAndIdenticalAfterParse() {
        val updated = LyricsEditorPolicy.nudge(
            LyricsEditorPolicy.updateText(draft(), 1, "Different words"), 1, 2)
        val encoded = LyricsEditorPolicy.serialize(updated)
        assertEquals(updated.cues, TimedLyricsCodec.parse(encoded).cues)
        assertFalse(LyricsEditorPolicy.changed(updated, updated))
    }

    @Test fun invalidIndexFailsWithoutTouchingDraft() {
        val initial = draft()
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.delete(initial, 8)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsEditorPolicy.updateTime(initial, -1, "00:02.000")
        }
        assertEquals(original.cues, initial.cues)
    }

    @Test fun serializedLrcByteLimitRemainsMandatory() {
        val many = TimedLyricsCodec.parse(
            (0 until 300).joinToString("\n") { i ->
                "[00:00.000] ${i.toString().padStart(3, '0')}" +
                    "x".repeat(150)
            }.toByteArray()
        )
        val baseline = LyricsEditorPolicy.begin(many)
        val bigger = "z".repeat(220)
        var changed = baseline
        var failed = false
        for (i in 0 until changed.cues.size) {
            try { changed = LyricsEditorPolicy.updateText(changed, i, bigger) }
            catch (_: IllegalArgumentException) { failed = true; break }
        }
        assertTrue(failed)
        assertTrue(LyricsEditorPolicy.serialize(changed).size <= TimedLyricsCodec.MAX_BYTES)
    }

    @Test fun nonAsciiBoundaryTimestampFormatsCorrectly() {
        assertEquals("99:59.999",
            LyricsEditorPolicy.timeLabel(LyricsEditorPolicy.MAX_TIME_MS))
        assertEquals(5999999L, LyricsEditorPolicy.parseTime("99:59.999"))
    }
}
