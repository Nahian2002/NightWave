package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSearchRankingTest {
    @Test fun latinAccentsAndCasingAreSearchable() {
        val keys = OfflineSearchRanking.terms("beyonce halo")
        assertTrue(OfflineSearchRanking.score(keys, "Beyoncé — Halo", "mp3") > 0)
        assertEquals("beyonce halo",
            OfflineSearchRanking.normalized("Beyoncé - Halo"))
    }

    @Test fun bengaliVowelsArePreserved() {
        val query = OfflineSearchRanking.terms("ফিরে দেখা")
        assertTrue(OfflineSearchRanking.score(query, "ফিরে দেখা - Live", "mp3") > 0)
        assertTrue(OfflineSearchRanking.normalized("ফিরে দেখা").contains("ফিরে"))
    }

    @Test fun exactAndPrefixOutrankSubstring() {
        val query = OfflineSearchRanking.terms("morning")
        val exact = OfflineSearchRanking.score(query, "Morning", "mp3")
        val prefix = OfflineSearchRanking.score(query, "Morning Chill", "mp3")
        val containing = OfflineSearchRanking.score(query, "A Beautiful Morning", "mp3")
        assertTrue(exact > prefix)
        assertTrue(prefix > containing)
    }

    @Test fun extensionOnlyAndUnrelatedQueriesAreSafe() {
        assertTrue(OfflineSearchRanking.score(OfflineSearchRanking.terms("flac"),
            "My Track", "flac") > 0)
        assertEquals(-1, OfflineSearchRanking.score(
            OfflineSearchRanking.terms("unrelated"), "My Track", "mp3"))
        assertFalse(OfflineSearchRanking.terms("!!!").isNotEmpty())
    }
}
