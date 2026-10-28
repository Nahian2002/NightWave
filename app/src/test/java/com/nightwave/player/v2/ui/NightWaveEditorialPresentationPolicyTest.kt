package com.nightwave.player.v2.ui

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.recommendation.PersonalMix
import org.junit.Assert.*
import org.junit.Test

class NightWaveEditorialPresentationPolicyTest {
    private val policy = NightWaveEditorialPresentationPolicy
    @Test fun promotionalPipeCaptionBecomesAConciseDisplayHeadline() {
        val full = "Amake Amar Moto Thakte Dao | Autograph | Prosenjit Chatterjee | Bengali Movie Song"
        val display = policy.identity(full, "SVF")
        assertEquals("Amake Amar Moto Thakte Dao", display.title)
        assertEquals("SVF", display.artistOrCreator)
    }
    @Test fun recordingVersionsRemainVisible() {
        assertEquals("River · Live Acoustic Remix", policy.identity(
            "River | Film credits | Live Acoustic Remix | Official Video", "Channel").title)
    }
    @Test fun ambiguousGenuinePipeTitlesAreNotGuessed() {
        assertEquals("Love | Hate | Hope", policy.identity("Love | Hate | Hope", "Channel").title)
    }
    @Test fun banglaMarksAndEditionLabelsSurviveDisplayFormatting() {
        val title = "আমাকে আমার মতো থাকতে দাও"
        assertEquals(title, policy.identity("$title | অটোগ্রাফ | Bengali Movie Song", "SVF").title)
        assertEquals("$title | লাইভ", policy.identity("$title | লাইভ | Official Video", "SVF").title)
    }
    @Test fun aSimpleTwoPartTitleKeepsTheExistingConservativeIdentity() {
        assertEquals("Song | Film", policy.identity("Song | Film", "Channel").title)
    }
    @Test fun longHeadlinesAndUnspacedPipesAreNotBlindlySplit() {
        val title = "a".repeat(97) + " | Film | Official Video"
        assertTrue(policy.identity(title, "Channel").title.startsWith("a".repeat(97)))
        assertEquals("Love|Hate | Film", policy.identity("Love|Hate | Film | Official Video", "Channel").title)
    }
    private fun track(n: Int, seconds: Long, creator: String = "Creator $n") =
        VideoPreview("Song $n", creator, "url:$n", "", seconds)
    @Test fun mixFactsUseUniqueTracksAndOnlyCompleteKnownDurations() {
        val a = track(1, 60); val b = track(2, 120)
        assertEquals("≈3 min · 2 creators", policy.mixFacts(PersonalMix("x", "x", "", "", listOf(a, b, a))))
        assertEquals("2 creators", policy.mixFacts(PersonalMix("x", "x", "", "", listOf(a, track(2, 0)))))
    }
    @Test fun unknownCreatorsAndInvalidDurationsDoNotInventFacts() {
        assertEquals("", policy.mixFacts(PersonalMix("x", "x", "", "", listOf(track(1, Long.MAX_VALUE, "Unknown creator")))))
        assertEquals("30 sec · 1 creator", policy.mixFacts(PersonalMix("x", "x", "", "", listOf(track(1, 30)))))
    }
}
