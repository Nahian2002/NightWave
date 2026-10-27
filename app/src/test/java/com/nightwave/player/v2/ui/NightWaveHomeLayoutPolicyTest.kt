package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWaveHomeLayoutPolicyTest {
    private val policy = NightWaveHomeLayoutPolicy

    @Test fun standardAndWidePhonesGetBalancedMusicCards() {
        assertEquals(1, policy.mixColumns(320f, 1f))
        assertEquals(1, policy.mixColumns(389.9f, 1f))
        assertEquals(2, policy.mixColumns(390f, 1f))
        assertEquals(2, policy.mixColumns(430f, 1f))
        assertEquals(2, policy.mixColumns(1000f, 1f))
    }

    @Test fun largerTextGetsReadableFullWidthMixCards() {
        assertEquals(1, policy.mixColumns(430f, 1.3f))
        assertEquals(1, policy.mixColumns(1000f, 2f))
        assertEquals(2, policy.mixColumns(430f, 1.29f))
        assertTrue(policy.mixCardCompact(1))
        assertFalse(policy.mixCardCompact(2))
    }

    @Test fun invalidDisplayMeasurementsDoNotBreakHome() {
        assertEquals(1, policy.mixColumns(Float.NaN, 1f))
        assertEquals(1, policy.mixColumns(Float.POSITIVE_INFINITY, 1f))
        assertEquals(1, policy.mixColumns(-1f, 1f))
        assertEquals(2, policy.mixColumns(400f, Float.NaN))
    }

    @Test fun emptyNewLibraryOffersSearchWithoutInventingSavedSongs() {
        val state = policy.recentEmptyState(0, 0)
        assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH,
            state.destination)
        assertEquals("Find music", state.actionLabel)
        assertEquals("Your soundtrack starts here", state.title)
        assertFalse(state.detail.contains("recommended", ignoreCase = true))
    }

    @Test fun likedSongsAreTheBestEmptyHistoryDestination() {
        val state = policy.recentEmptyState(1, 0)
        assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIKED_SONGS,
            state.destination)
        assertEquals("Open Liked Songs", state.actionLabel)
        assertTrue(state.detail.contains("liked songs", ignoreCase = true))
        assertFalse(state.detail.contains("recent plays", ignoreCase = true))
    }

    @Test fun playlistOnlyLibraryOpensExistingMusicNotGenericSearch() {
        val state = policy.recentEmptyState(0, 5)
        assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIBRARY,
            state.destination)
        assertEquals("Open Library", state.actionLabel)
        assertTrue(state.detail.contains("playlists", ignoreCase = true))
    }

    @Test fun whenLikesAndPlaylistsExistPreferDirectLikedSongs() {
        for (favorites in listOf(1, 2, 100, Int.MAX_VALUE)) {
            val state = policy.recentEmptyState(favorites, 1000)
            assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIKED_SONGS,
                state.destination)
            assertEquals("Open Liked Songs", state.actionLabel)
        }
    }

    @Test fun corruptOrNegativeCountsCannotClaimUserOwnsSavedMusic() {
        val invalid = listOf(Int.MIN_VALUE, -100, -1, 0)
        for (favorites in invalid) for (playlist in invalid) {
            val state = policy.recentEmptyState(favorites, playlist)
            assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH,
                state.destination)
            assertFalse(state.detail.contains("your saved playlists", ignoreCase = true))
        }
        assertEquals(NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIBRARY,
            policy.recentEmptyState(Int.MIN_VALUE, 1).destination)
    }

    @Test fun repeatedEmptyStateDecisionsArePureAndStable() {
        for (favorites in listOf(0, 1, 40)) for (playlists in listOf(0, 1, 80)) {
            val first = policy.recentEmptyState(favorites, playlists)
            val second = policy.recentEmptyState(favorites, playlists)
            assertEquals(first, second)
            assertTrue(first.title.isNotBlank())
            assertTrue(first.detail.isNotBlank())
            assertTrue(first.actionLabel.isNotBlank())
        }
    }

    @Test fun shortRecentShelfNeedsNeitherExpansionNorFullHistoryLink() {
        for (count in listOf(0, 1, 5, 6)) {
            val actions = policy.recentBrowseActions(count, false, false)
            assertFalse(actions.showExpansion)
            assertEquals(null, actions.expansionLabel)
            assertFalse(actions.showFullHistory)
        }
    }

    @Test fun completeSevenSongShelfCanTruthfullySaySeeAll() {
        val collapsed = policy.recentBrowseActions(7, false, false)
        assertTrue(collapsed.showExpansion)
        assertEquals("See all", collapsed.expansionLabel)
        assertFalse(collapsed.showFullHistory)
        val expanded = policy.recentBrowseActions(7, false, true)
        assertTrue(expanded.showExpansion)
        assertEquals("Show less", expanded.expansionLabel)
        assertFalse(expanded.showFullHistory)
    }

    @Test fun cappedHistoryNeverMislabelsHundredAsAll() {
        val collapsed = policy.recentBrowseActions(100, true, false)
        assertTrue(collapsed.showExpansion)
        assertEquals("See first 100", collapsed.expansionLabel)
        assertTrue(collapsed.showFullHistory)
        val expanded = policy.recentBrowseActions(100, true, true)
        assertEquals("Show less", expanded.expansionLabel)
        assertTrue(expanded.showFullHistory)
    }

    @Test fun fullHistoryRouteIsIndependentOfShowAllToggle() {
        for (expanded in listOf(true, false)) {
            val state = policy.recentBrowseActions(100, true, expanded)
            assertTrue(state.showFullHistory)
            assertTrue(state.showExpansion)
        }
    }

    @Test fun incompleteWindowUsesTrueOverflowSignalEvenIfOnlyFiveAreVisible() {
        val actions = policy.recentBrowseActions(5, true, false)
        assertFalse(actions.showExpansion)
        assertEquals(null, actions.expansionLabel)
        assertTrue(actions.showFullHistory)
    }

    @Test fun negativeOrCorruptCountsCannotCauseMisleadingExpansion() {
        for (count in listOf(Int.MIN_VALUE, -100, -1, 0)) {
            val actions = policy.recentBrowseActions(count, false, true)
            assertFalse(actions.showExpansion)
            assertEquals(null, actions.expansionLabel)
            assertFalse(actions.showFullHistory)
        }
        val capped = policy.recentBrowseActions(Int.MAX_VALUE, true, false)
        assertEquals("See first 2147483647", capped.expansionLabel)
        assertTrue(capped.showFullHistory)
    }

    @Test fun recentBrowsePolicyCannotModifyListeningOrCollectionState() {
        val original = listOf("song-a", "song-b", "song-c")
        val copy = original.toList()
        val a = policy.recentBrowseActions(100, true, false)
        val b = policy.recentBrowseActions(100, true, false)
        assertEquals(a, b)
        assertEquals(copy, original)
        assertTrue(a.showFullHistory)
    }
}
