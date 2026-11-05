package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

/** Fixture-only import/review/edit/export/follow journey; no filesystem, service or provider access. */
class NW150LyricsJourneyTest {
    @Test fun reviewedOwnedLrcCanBeEditedAndRoundTrippedWithoutChangingTheBaseline() {
        val bytes = "[ti:মালা]\n[00:01.000]প্রথম লাইন\n[00:02.000]দ্বিতীয় লাইন".toByteArray(Charsets.UTF_8)
        val review = LyricsImportReviewPolicy.inspect(bytes, "মালা.lrc", "মালা", 60_000)
        assertEquals(LyricsImportMatch.LIKELY, review.match)
        val original = LyricsEditorPolicy.begin(review.lyrics)
        val draft = LyricsEditorPolicy.updateText(original, 1, "বদলানো লাইন")
        val encoded = LyricsEditorPolicy.serialize(draft)
        assertEquals(draft.cues, TimedLyricsCodec.parse(encoded).cues)
        assertEquals("দ্বিতীয় লাইন", original.cues[1].text)
        assertEquals(2_000L, draft.cues[1].timeMs)
    }
    @Test fun explicitDifferentRecordingRemainsAReviewWarning() {
        val review = LyricsImportReviewPolicy.inspect("[ti:Other recording]\n[00:01.00]Fixture".toByteArray(),
            "Same title.lrc", "Same title", 10_000)
        assertEquals(LyricsImportMatch.DIFFERENT, review.match)
        assertTrue(review.warnings.any { it.contains("does not match") })
    }
    @Test fun followCanPauseAndResumeWithoutChangingCuesOrSeekOffsets() {
        val cue = LyricCue(2_000L, "Fixture")
        var following = true
        following = LyricsReadingPolicy.toggledFollow(following); assertFalse(following)
        following = LyricsReadingPolicy.toggledFollow(following); assertTrue(following)
        assertEquals(1_500L, LyricsSyncPolicy.seekPosition(cue, 500, 60_000))
        assertEquals(LyricCue(2_000L, "Fixture"), cue)
    }
}
