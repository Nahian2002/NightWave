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
}
