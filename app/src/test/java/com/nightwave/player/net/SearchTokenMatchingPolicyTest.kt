package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTokenMatchingPolicyTest {
    private val p = SearchTokenMatchingPolicy

    @Test fun aloIsNotAloneEvenWhenSongQueryRepeatsIt() {
        val words = p.tokens(SearchRelevancePolicy.normalize("Alone"))
        assertEquals(0, p.evidence("alo", words, allowTypo = true))
        assertFalse(p.allCovered(listOf("alo", "alo"), words, allowTypo = true))
        assertTrue(p.allCovered(listOf("alone"), words))
        assertEquals(3, p.evidence("alo", p.tokens("alo"), allowTypo = true))
    }

    @Test fun matchingHasStrictEvidenceBeforeTypoCandidates() {
        assertEquals(3, p.evidence("moonlight", p.tokens("moonlight")))
        assertEquals(2, p.evidence("moon", p.tokens("moonlight")))
        assertEquals(1, p.evidence("moonligt", p.tokens("moonlight"), true))
        assertEquals(1, p.evidence("moolight", p.tokens("moonlight"), true))
        assertEquals(1, p.evidence("nigth", p.tokens("night"), true))
        assertEquals(0, p.evidence("moonligt", p.tokens("moonlight"), false))
        assertEquals(0, p.evidence("nightcore", p.tokens("night"), true))
    }

    @Test fun shortAndNonLatinNamesAreNeverTypoMerged() {
        assertEquals(0, p.evidence("alo", p.tokens("alone"), true))
        assertEquals(0, p.evidence("মালা", p.tokens("মেলা"), true))
        assertEquals(0, p.evidence("ভলবস", p.tokens("ভালোবাসা"), true))
        assertEquals(3, p.evidence("ভালোবাসা", p.tokens("ভালোবাসা"), true))
        assertEquals(0, p.evidence("123", p.tokens("1234"), true))
    }

    @Test fun onlyOneAsciiEditAndNoBlankTerms() {
        assertEquals(0, p.evidence("", p.tokens("night"), true))
        assertEquals(0, p.evidence("night", emptyList(), true))
        assertEquals(1, p.evidence("nightt", p.tokens("night"), true))
        assertEquals(1, p.evidence("nigh", p.tokens("night"), true))
        assertEquals(0, p.evidence("nghit", p.tokens("night"), true))
    }
}
