package com.nightwave.player.v2.lyrics

import org.junit.Assert.*
import org.junit.Test

class LyricsImportReviewPolicyTest {
    private fun inspect(
        text: String = "[ti:Blue Skies]\n[00:02.00]First\n[00:04.50]Second",
        filename: String = "Blue Skies.lrc",
        title: String = "Blue Skies",
        durationMs: Long = 9_000L
    ) = LyricsImportReviewPolicy.inspect(
        text.toByteArray(Charsets.UTF_8), filename, title, durationMs
    )

    @Test fun matchingFilenameOnlyAdvisesAndPreservesOriginalCues() {
        val review = inspect()
        assertEquals(LyricsImportMatch.LIKELY, review.match)
        assertEquals(2, review.cueCount)
        assertEquals(4500L, review.lastCueMs)
        assertEquals("First", review.lyrics.cues.first().text)
    }

    @Test fun conflictingExplicitTitleTagWarnsEvenIfFilenameLooksMatching() {
        assertEquals(LyricsImportMatch.DIFFERENT,
            inspect(text = "[ti:Different Song]\n[00:02.00]one").match)
    }

    @Test fun artistPrefixProvidesPossibleMatchNotAutomaticProof() {
        assertEquals(LyricsImportMatch.POSSIBLE,
            inspect(filename = "Artist - Blue Skies (Official Lyrics).lrc",
                text = "[00:01.00]one").match)
    }

    @Test fun unrelatedSongIsNotSilentlyAssociated() {
        val preview = inspect(filename = "Unrelated.lrc", text = "[00:01.00]one")
        assertEquals(LyricsImportMatch.DIFFERENT, preview.match)
        assertTrue(preview.warnings.any { it.contains("does not match") })
    }

    @Test fun malformedRowsHaveActionableCountsButOtherCuesSurvive() {
        val review = inspect(text =
            "[00:61.00]too late\n[00:01.00]valid\n[00:invalid]bad\n[00:02.00]next",
            filename = "Blue Skies.lrc")
        assertEquals(2, review.cueCount)
        assertEquals(2, review.ignoredTimestampRows)
        assertTrue(review.warnings.any { it.contains("malformed") })
    }

    @Test fun duplicateAndOutOfOrderCuesReportedWithoutChangingOriginalParser() {
        val report = inspect(text =
            "[00:04.00]late\n[00:02.00]early\n[00:02.00]early")
        assertEquals(2, report.cueCount)
        assertEquals(1, report.repeatedCues)
        assertEquals(1, report.outOfOrderCues)
        assertEquals(listOf("early", "late"), report.lyrics.cues.map { it.text })
    }

    @Test fun durationMismatchWarnsWithoutGuessingAnOffset() {
        val preview = inspect(text = "[00:14.00]too late", durationMs = 8_000L)
        assertTrue(preview.durationWarning)
        assertTrue(preview.warnings.any { it.contains("duration") })
    }

    @Test fun validSlightlyBeyondDurationIsNotSpuriouslyFlagged() {
        assertFalse(inspect(text = "[00:12.00]last", durationMs = 8_000L)
            .durationWarning)
    }

    @Test fun importedLrcParserStillRejectsMalformedUtf8AndOversizedFiles() {
        assertThrows(Exception::class.java) {
            LyricsImportReviewPolicy.inspect(byteArrayOf(0xC3.toByte(), 0x28),
                "Blue Skies.lrc", "Blue Skies", 9000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsImportReviewPolicy.inspect(
                ByteArray(TimedLyricsCodec.MAX_BYTES + 1) { 65 },
                "Blue Skies.lrc", "Blue Skies", 9000)
        }
    }

    @Test fun filenameIsAdvisoryAndNormalizationDoesNotTreatAllTitlesAsMatches() {
        assertEquals("blue skies", LyricsImportReviewPolicy.normalizedName("Blue-Skies"))
        assertEquals(LyricsImportMatch.DIFFERENT,
            LyricsImportReviewPolicy.compare("Wrong.lrc", "Blue Skies", null))
        assertEquals(LyricsImportMatch.UNKNOWN,
            LyricsImportReviewPolicy.compare("", "", null))
    }

    @Test fun rejectedFilesGetSpecificSafeErrorsWithoutRawProviderDetails() {
        assertTrue(LyricsImportReviewPolicy.failureHint(
            IllegalArgumentException("No synchronized LRC timestamps were found"))
            .contains("[mm:ss.xx]"))
        assertTrue(LyricsImportReviewPolicy.failureHint(
            IllegalArgumentException("Lyrics file exceeds 64 KB")).contains("64 KB"))
        assertTrue(LyricsImportReviewPolicy.failureHint(
            IllegalArgumentException("Lyrics file has too many timed lines")).contains("1,200"))
        assertFalse(LyricsImportReviewPolicy.failureHint(
            IllegalStateException("content://private/stolen-token")).contains("stolen-token"))
    }

    @Test fun nonLrcTextFilenameIsWarnedButValidTimingStillAccepted() {
        val preview = inspect(filename = "Blue Skies.txt", text = "[00:01.00]one")
        assertEquals(1, preview.cueCount)
        assertTrue(preview.warnings.any { it.contains("not .lrc") })
    }

    @Test fun sourceNameCannotInsertControlCharactersIntoConfirmation() {
        val preview = inspect(filename = "Blue\nSkies\t.lrc")
        assertFalse(preview.sourceLabel.contains("\n"))
        assertFalse(preview.sourceLabel.contains("\t"))
    }

    @Test fun reviewNeverIncludesRawLyricTextInWarningMessages() {
        val report = inspect(text = "[00:01.00]SECRET-WORDS")
        assertFalse(report.warnings.joinToString(" ").contains("SECRET-WORDS"))
    }
}
