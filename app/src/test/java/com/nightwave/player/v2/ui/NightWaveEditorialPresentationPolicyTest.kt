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

    @Test fun ownerCaptionWithoutPromotionalBadgeUsesCorroboratedFinalCreator() {
        val title = "Amake Amar Moto Thakte Dao | Autograph | Prosenjit Chatterjee | Anupam Roy | Srijit Mukherji | SVF"
        val display = policy.identity(title, "SVF")
        assertEquals("Amake Amar Moto Thakte Dao", display.title)
        assertEquals("SVF", display.artistOrCreator)
    }
    @Test fun uncorroboratedMultiPartCaptionsAreNotGuessed() {
        val title = "Song name | Film name | Performer | Other publisher"
        assertEquals(title, policy.identity(title, "Channel").title)
        assertEquals(title, policy.identity(title, "Unknown creator").title)
    }
    @Test fun corroboratedCreditsKeepLiveRemixAndBanglaEditions() {
        assertEquals("River · Live Acoustic Remix", policy.identity(
            "River | Film | Live Acoustic Remix | Publisher", "Publisher").title)
        assertEquals("আমাকে আমার মতো থাকতে দাও · লাইভ", policy.identity(
            "আমাকে আমার মতো থাকতে দাও | অটোগ্রাফ | লাইভ | এসভিএফ", "এসভিএফ").title)
    }
    @Test fun genericFinalSourceDoesNotAuthorizeCaptionCollapse() {
        val title = "Song name | Film | Cast | Unknown creator"
        assertEquals(title, policy.identity(title, "Unknown creator").title)
    }
    @Test fun shortGenuinePipeTitlesRemainCompleteEvenWithCreatorSuffix() {
        val title = "Love | Hate | Channel"
        assertEquals(title, policy.identity(title, "Channel").title)
    }
    @Test fun banglaCreatorComparisonPreservesDependentVowels() {
        val title = "A song | Film | Cast | দিন"
        assertEquals(title, policy.identity(title, "দীন").title)
    }
    @Test fun compactMixSummaryIncludesCountAndCompleteDurationOnce() {
        val a = track(1, 60); val b = track(2, 120)
        assertEquals("2 tracks · ≈3 min", policy.mixSummary(PersonalMix("x", "x", "", "", listOf(a,b,a))))
        assertEquals("1 track · 30 sec", policy.mixSummary(PersonalMix("x", "x", "", "", listOf(track(1,30)))))
    }
    @Test fun incompleteOrEmptyMixNeverReportsAnInventedDuration() {
        assertEquals("2 tracks", policy.mixSummary(PersonalMix("x", "x", "", "", listOf(track(1,60),track(2,0)))))
        assertEquals("0 tracks", policy.mixSummary(PersonalMix("x", "x", "", "", emptyList())))
    }

    @Test fun representativeCoversAreStableActualTracksWithoutQueueChanges() {
        val tracks = (1..8).map { track(it,120).copy(thumbnail = "art:$it") }
        val mix = PersonalMix("replay", "History", "", "", tracks)
        val before = tracks.toList()
        val covers = policy.mixArtwork(mix)
        assertEquals(4, covers.size)
        assertEquals(covers, policy.mixArtwork(mix))
        assertTrue(covers.all { it in tracks })
        assertEquals(before, mix.tracks)
    }
    @Test fun representativeCoversPreferAvailableImagesAndDistinctCreators() {
        val tracks = listOf(track(1,60,"One"),track(2,60,"One"),track(3,60,"Two"),track(4,60,"Three"),track(5,60,"Four"))
            .map { it.copy(thumbnail = "art:" + it.url) }
        val mix = PersonalMix("daily", "Daily", "", "", tracks + track(6,60,"Five"))
        val covers = policy.mixArtwork(mix)
        assertEquals(4,covers.map { it.author }.distinct().size)
        assertTrue(covers.all { it.thumbnail.isNotBlank() })
    }
    @Test fun missingOrRepeatedArtworkSourcesStillUseRealUniqueTracks() {
        val a = track(1,60,"Unknown creator");val b = track(2,60,"Unknown creator")
        assertEquals(setOf(a,b),policy.mixArtwork(PersonalMix("x","x","","",listOf(a,a,b))).toSet())
        assertTrue(policy.mixArtwork(PersonalMix("x","x","","",emptyList())).isEmpty())
    }
    @Test fun representativeSelectionRetainsAllFourSlotsWithOneCreator() {
        val tracks = (1..6).map { track(it,120,"One").copy(thumbnail = "art:$it") }
        assertEquals(4,policy.mixArtwork(PersonalMix("x","x","","",tracks)).distinctBy { it.url }.size)
    }
}
