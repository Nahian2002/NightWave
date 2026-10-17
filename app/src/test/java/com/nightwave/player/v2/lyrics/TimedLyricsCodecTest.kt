package com.nightwave.player.v2.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedLyricsCodecTest {
    private fun parse(text: String) = TimedLyricsCodec.parse(text.toByteArray(Charsets.UTF_8))

    @Test fun importsTimestampedLinesInTimeOrder() {
        val lyrics = parse("[00:08.50] Second line\n[00:01.25] First line")
        assertEquals(listOf("First line", "Second line"), lyrics.cues.map { it.text })
        assertEquals(listOf(1250L, 8500L), lyrics.cues.map { it.timeMs })
    }
    @Test fun highlightsTheCorrectLineWithoutLeakingTheNext() {
        val lyrics = parse("[00:01.00] one\n[00:02.00] two\n[00:04.00] three")
        assertEquals(-1, lyrics.activeIndex(999L))
        assertEquals(0, lyrics.activeIndex(1000L))
        assertEquals(1, lyrics.activeIndex(3750L))
        assertEquals(2, lyrics.activeIndex(4000L))
    }
    @Test fun multipleTimestampsCreateDistinctCues() {
        val lyrics = parse("[00:01.2][00:03.20] Repeat")
        assertEquals(listOf(1200L, 3200L), lyrics.cues.map { it.timeMs })
    }
    @Test fun metadataIsIgnoredButValidTimeRemains() {
        val lyrics = parse("[ar:Artist]\n[ti:Name]\n[00:01.500]hello")
        assertEquals("hello", lyrics.cues.single().text)
    }
    @Test fun emptyAndUntimedFilesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { parse("a lyric without time") }
        assertThrows(IllegalArgumentException::class.java) { parse("") }
    }
    @Test fun sizeBoundPreventsUntrustedLargeFiles() {
        assertThrows(IllegalArgumentException::class.java) {
            TimedLyricsCodec.parse(ByteArray(TimedLyricsCodec.MAX_BYTES + 1) { 65 })
        }
    }
    @Test fun badUtf8IsRejected() {
        assertThrows(Exception::class.java) {
            TimedLyricsCodec.parse(byteArrayOf(0xC3.toByte(), 0x28))
        }
    }
    @Test fun malformedTimestampDoesNotCreateInvalidLyrics() {
        assertThrows(IllegalArgumentException::class.java) {
            parse("[00:78.20] invalid")
        }
    }
    @Test fun duplicateCuesAreRemoved() {
        val lyrics = parse("[00:01.00] same\n[00:01.00] same")
        assertEquals(1, lyrics.cues.size)
    }
    @Test fun cueCountFailsClosed() {
        val input = (1..TimedLyricsCodec.MAX_LINES+1).joinToString("\n") { "[00:01.00] line $it" }
        assertThrows(IllegalArgumentException::class.java) { parse(input) }
    }
}
