package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

/** No Android or network dependency; all labels describe real data sources. */
class NightWaveBrowseChromePolicyTest {
    @Test fun zeroOneAndPluralCollectionsAreAccurate() {
        assertEquals("0 tracks",
            NightWaveBrowseChromePolicy.countLabel(0, "track", "tracks"))
        assertEquals("1 track",
            NightWaveBrowseChromePolicy.countLabel(1, "track", "tracks"))
        assertEquals("28 tracks",
            NightWaveBrowseChromePolicy.countLabel(28, "track", "tracks"))
    }

    @Test fun negativeCountsFailInsteadOfShowingFabricatedStatistics() {
        assertThrows(IllegalArgumentException::class.java) {
            NightWaveBrowseChromePolicy.countLabel(-1, "track", "tracks")
        }
    }

    @Test fun librarySummaryTracksTwoIndependentCollections() {
        assertEquals("1 liked track  ·  0 playlists",
            NightWaveBrowseChromePolicy.libraryCounts(1, 0))
        assertEquals("0 liked tracks  ·  1 playlist",
            NightWaveBrowseChromePolicy.libraryCounts(0, 1))
    }

    @Test fun savedResultsNeverPretendToBePublicVideos() {
        assertEquals("Saved on this device", NightWaveBrowseChromePolicy.sourceDetail(
            NightWaveBrowseChromePolicy.Source.SAVED_LIBRARY))
    }

    @Test fun publicResultsDoNotClaimTheyAreOffline() {
        val message = NightWaveBrowseChromePolicy.sourceDetail(
            NightWaveBrowseChromePolicy.Source.PUBLIC_VIDEO)
        assertTrue(message.contains("internet required"))
        assertFalse(message.contains("Saved on this device"))
    }

    @Test fun titleStripsWhitespaceAndRespectsMaximumLength() {
        assertEquals("My Mix", NightWaveBrowseChromePolicy.title("  My Mix  ", "Backup"))
        assertEquals("Backup", NightWaveBrowseChromePolicy.title("   ", "Backup"))
        assertEquals(NightWaveBrowseChromePolicy.MAX_HEADER_TITLE,
            NightWaveBrowseChromePolicy.title("a".repeat(300), "Backup").length)
    }

    @Test fun narrowCollectionCardsStackVertically() {
        assertTrue(NightWaveBrowseChromePolicy.showCompactCollections(320f))
        assertTrue(NightWaveBrowseChromePolicy.showCompactCollections(439.99f))
        assertFalse(NightWaveBrowseChromePolicy.showCompactCollections(440f))
        assertFalse(NightWaveBrowseChromePolicy.showCompactCollections(740f))
    }

    @Test fun undefinedWidthDefaultsToSafeStackedLayout() {
        assertTrue(NightWaveBrowseChromePolicy.showCompactCollections(Float.NaN))
        assertTrue(NightWaveBrowseChromePolicy.showCompactCollections(Float.POSITIVE_INFINITY))
        assertTrue(NightWaveBrowseChromePolicy.showCompactCollections(-10f))
    }

    @Test fun artworkAndTileDimensionsRemainCompact() {
        assertTrue(NightWaveBrowseChromePolicy.MIN_TILE_HEIGHT_DP >= 64)
        assertTrue(NightWaveBrowseChromePolicy.TRACK_ART_DP <=
            NightWaveBrowseChromePolicy.MIN_TILE_HEIGHT_DP)
        assertTrue(NightWaveBrowseChromePolicy.COLLECTION_ART_DP <=
            NightWaveBrowseChromePolicy.MIN_TILE_HEIGHT_DP)
    }
}
