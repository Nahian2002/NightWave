package com.nightwave.player.v2.recommendation

import com.nightwave.player.net.VideoPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixDiversityPolicyTest {
    private fun track(index: Int, creator: String = "Creator"): VideoPreview {
        val id = "abCD1234" + index.toString().padStart(3, '0')
        return VideoPreview("Song $index", creator,
            "https://www.youtube.com/watch?v=$id", "", 180L)
    }

    @Test fun emptyAndNonPositiveLimitsReturnNothing() {
        assertTrue(MixDiversityPolicy.select(emptyList(), 1L) { 5 }.isEmpty())
        assertTrue(MixDiversityPolicy.select(listOf(track(1)), 1L, 0) { 5 }.isEmpty())
        assertTrue(MixDiversityPolicy.select(listOf(track(1)), 1L, -1) { 5 }.isEmpty())
    }

    @Test fun alwaysReturnsOnlyCandidatesWithoutDuplicates() {
        val a = track(1)
        val b = track(2)
        val chosen = MixDiversityPolicy.select(listOf(a, a, b, b), 5L) { 1 }
        assertEquals(setOf(a.url, b.url), chosen.map { it.url }.toSet())
        assertEquals(2, chosen.size)
    }

    @Test fun boundedToTwelveEvenWithHugeRequestedLimit() {
        val tracks = (1..100).map { track(it) }
        assertEquals(12, MixDiversityPolicy.select(tracks, 5L, 999) { 0 }.size)
    }

    @Test fun oneCreatorStillFillsTheMix() {
        val tracks = (1..20).map { track(it, "The same creator") }
        assertEquals(12, MixDiversityPolicy.select(tracks, 3L) { 4 }.size)
    }

    @Test fun creatorDiversityIsFavoredWhenSignalsAreSimilar() {
        val tracks = (1..8).map { track(it, "A") } +
            (9..12).map { track(it, "B") } +
            (13..16).map { track(it, "C") }
        val chosen = MixDiversityPolicy.select(tracks, 5L, 6) { 5 }
        assertTrue(chosen.take(3).map { it.author }.toSet().size >= 2)
    }

    @Test fun veryStrongAffinityStillBeatsDiversityPenalty() {
        val main = track(1, "A")
        val second = track(2, "A")
        val outsider = track(3, "B")
        val chosen = MixDiversityPolicy.select(listOf(main, second, outsider), 5L, 3) {
            if (it == main || it == second) 30 else 1
        }
        assertEquals(setOf(main.url, second.url), chosen.take(2).map { it.url }.toSet())
    }

    @Test fun identicalInputsAndDayGiveIdenticalOrder() {
        val tracks = (1..30).map { track(it, "Creator " + it % 4) }
        assertEquals(
            MixDiversityPolicy.select(tracks, 20000L) { 1 },
            MixDiversityPolicy.select(tracks, 20000L) { 1 }
        )
    }

    @Test fun dailyTieRotationActuallyChangesEqualSignalOrder() {
        val tracks = (1..18).map { track(it) }
        val orders = (20000L..20012L).map { day ->
            MixDiversityPolicy.select(tracks, day) { 1 }.map { it.url }
        }.toSet()
        assertTrue(orders.size > 1)
    }

    @Test fun unknownCreatorLabelsAreNotMistakenForOneArtist() {
        val tracks = (1..12).map { track(it, "Unknown") }
        assertEquals(12, MixDiversityPolicy.select(tracks, 5L) { 0 }.size)
    }

    @Test fun personalizationEngineHasAtMostFourUniqueGenuineMixes() {
        val favorites = (1..18).map { track(it, "Creator " + it % 3) }
        val mixes = PersonalMixEngine.make(favorites, listOf(track(1)), favorites, 20000L)
        assertTrue(mixes.size <= 4)
        assertTrue(mixes.all { it.tracks.size <= 12 })
        val allowed = favorites.map { it.url }.toSet()
        assertTrue(mixes.flatMap { it.tracks }.all { it.url in allowed })
        assertEquals(mixes.map { it.key }.toSet().size, mixes.size)
    }
}
