package com.nightwave.player.v2.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchConsumerCopyPolicyTest {
    private val policy = SearchConsumerCopyPolicy

    @Test fun labelsStayReadableAndDoNotPresentSourceDomains() {
        assertEquals("Songs & music", policy.resultsTitle(SearchConsumerCopyPolicy.Mode.SONGS))
        assertEquals("Videos", policy.resultsTitle(SearchConsumerCopyPolicy.Mode.VIDEOS))
        assertEquals("No online matches yet", policy.resultCount(0))
        assertEquals("1 online result", policy.resultCount(1))
        assertEquals("24 online results", policy.resultCount(24))
        assertEquals("No online matches yet", policy.resultCount(-5))
        val all = listOf(policy.bestMatchesSubtitle(), policy.moreResultsSubtitle(),
            policy.loadingHint(true), policy.fallbackHint(false)).joinToString()
        assertFalse(all.contains("piped.video"))
        assertFalse(all.contains("provider"))
        assertFalse(all.contains("not verified artist identities"))
    }

    @Test fun localLibraryHintDoesNotClaimMissingSongsExist() {
        assertTrue(policy.loadingHint(true).contains("saved music"))
        assertFalse(policy.loadingHint(false).contains("saved music"))
        assertTrue(policy.fallbackHint(true).contains("saved tracks"))
        assertFalse(policy.fallbackHint(false).contains("saved tracks"))
    }

    @Test fun creatorMetadataCaveatRemainsHonestWithoutTechnicalLanguage() {
        assertEquals("2 saved tracks", policy.savedCreatorSubtitle(2))
        assertEquals("0 saved tracks", policy.savedCreatorSubtitle(-9))
        assertTrue(policy.savedCreatorNotice().contains("may differ"))
    }
}
