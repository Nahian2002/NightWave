package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDiscoveryReviewPolicyTest {
    private fun candidate(name: String, id: String) = FreeAudioCandidate(
        name, "https://upload.wikimedia.org/wikipedia/commons/$id.mp3",
        "https://commons.wikimedia.org/wiki/File:$id.mp3",
        "CC0", 1000L, "$id.mp3", "abc"
    )

    @Test fun exactTitleWordsRiseWithoutDeletingUnrelatedEntries() {
        val other = candidate("Piano exercise", "a")
        val partial = candidate("Sonata for violin", "b")
        val full = candidate("Moonlight Sonata piano performance", "c")
        val source = listOf(other, partial, full)
        val ordered = OfflineDiscoveryReviewPolicy.rank("Moonlight Sonata", source)
        assertEquals(listOf(full, partial, other), ordered)
        assertEquals(listOf(other, partial, full), source)
    }

    @Test fun tiesStayInOriginalOrderAndKeepObjectIdentity() {
        val a = candidate("Moonlight live", "a")
        val b = candidate("Moonlight recording", "b")
        val c = candidate("Orchestra", "c")
        val result = OfflineDiscoveryReviewPolicy.rank("Moonlight", listOf(c, b, a))
        assertEquals(listOf(b, a, c), result)
        assertTrue(result[0] === b)
        assertTrue(result[1] === a)
    }

    @Test fun noTokensNeverReordersProviderResults() {
        val a = candidate("A", "a")
        val b = candidate("B", "b")
        assertEquals(listOf(a, b),
            OfflineDiscoveryReviewPolicy.rank("!!!", listOf(a, b)))
    }

    @Test fun banglaVowelsRemainDistinctTitleWords() {
        val exact = candidate("ভালোবাসি ঝুম", "a")
        val other = candidate("ভালবাসি ঝুম", "b")
        assertEquals(listOf(exact, other),
            OfflineDiscoveryReviewPolicy.rank("ভালোবাসি ঝুম", listOf(other, exact)))
    }

    @Test fun hintsNeverCallAListingTheSameRecording() {
        assertEquals("1 of 2 title words appear. This may be a different recording.",
            OfflineDiscoveryReviewPolicy.reviewHint("Moonlight Sonata", "Sonata study"))
        assertEquals("Different listing title. Review this recording and its license.",
            OfflineDiscoveryReviewPolicy.reviewHint("Moonlight Sonata", "Jazz improvisation"))
        assertEquals("Independent recording. Review title, license and source.",
            OfflineDiscoveryReviewPolicy.reviewHint("!@#", "Jazz improvisation"))
    }

    @Test fun longOrNonsenseRequestsAreBoundedAndDoNotCrash() {
        val sample = listOf(candidate("Simple", "one"), candidate("Song", "two"))
        val result = OfflineDiscoveryReviewPolicy.rank("x".repeat(100_000), sample)
        assertEquals(sample, result)
    }
}
