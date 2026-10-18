package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsExportPolicyTest {
    private val original = LyricsEditorPolicy.begin(TimedLyricsCodec.parse(
        "[00:01.000]First\n[00:03.250]Second".toByteArray(Charsets.UTF_8)
    ))

    @Test fun originalLyricsExportAsStandardLrc() {
        val ready = LyricsExportPolicy.prepare("My Song", original, original)
        assertEquals("My Song.lrc", ready.suggestedName)
        assertEquals(2, ready.cueCount)
        assertFalse(ready.unsavedDraft)
        assertEquals(original.cues, TimedLyricsCodec.parse(ready.bytes).cues)
    }

    @Test fun unsavedDraftExportsWithoutChangingSource() {
        val modified = LyricsEditorPolicy.updateText(original, 0, "Edited First")
        val ready = LyricsExportPolicy.prepare("My Song", original, modified)
        assertTrue(ready.unsavedDraft)
        assertEquals("First", original.cues.first().text)
        assertEquals("Edited First", TimedLyricsCodec.parse(ready.bytes).cues.first().text)
    }

    @Test fun suggestedFilenameRemovesSeparatorsAndControlCharacters() {
        val name = LyricsExportPolicy.suggestedFileName("Audio/Private\\Name:\nTitle\t?.lrc")
        assertFalse(name.contains("/"))
        assertFalse(name.contains("\\"))
        assertFalse(name.contains("\n"))
        assertFalse(name.contains("?"))
        assertTrue(name.endsWith(".lrc"))
    }

    @Test fun emptyNameFallsBackWithoutWritingAnywhere() {
        assertEquals("NightWave Lyrics.lrc", LyricsExportPolicy.suggestedFileName("   ..   "))
    }

    @Test fun filenameLengthIsBoundedAndNotDoubleSuffixed() {
        val result = LyricsExportPolicy.suggestedFileName("a".repeat(200) + ".LRC")
        assertTrue(result.length <= LyricsExportPolicy.MAX_BASENAME_LENGTH + 4)
        assertEquals("Example.lrc", LyricsExportPolicy.suggestedFileName("Example.lrc"))
    }

    @Test fun nonAsciiSongTitlesArePreserved() {
        assertEquals("বৃষ্টি.lrc", LyricsExportPolicy.suggestedFileName("বৃষ্টি"))
    }

    @Test fun timeCorrectionsArePreservedExactlyInExport() {
        val updated = LyricsEditorPolicy.updateTime(original, 1, "00:04.125")
        val ready = LyricsExportPolicy.prepare("Song", original, updated)
        assertEquals(4125L, TimedLyricsCodec.parse(ready.bytes).cues[1].timeMs)
    }

    @Test fun unsafeManuallyConstructedDraftCannotBeExported() {
        val bad = LyricsEditorDraft(listOf(LyricCue(1000L, "Fake [00:02.000]extra")))
        assertThrows(IllegalArgumentException::class.java) {
            LyricsExportPolicy.prepare("song", original, bad)
        }
    }

    @Test fun oversizedDraftCannotPassExportValidation() {
        val cues = (0 until 400).map { i ->
            LyricCue(i * 1000L, (i.toString() + "Z".repeat(213)).take(220))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsExportPolicy.prepare("song", original, LyricsEditorDraft(cues))
        }
    }

    @Test fun exportDoesNotMutateBaselineOrCurrentDraft() {
        val changed = LyricsEditorPolicy.nudge(original, 0, 2)
        val before = changed.cues.toList()
        val ready = LyricsExportPolicy.prepare("Song", original, changed)
        assertEquals(before, changed.cues)
        assertEquals(1500L, TimedLyricsCodec.parse(ready.bytes).cues.first().timeMs)
        assertEquals(1000L, original.cues.first().timeMs)
    }
}
