package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class NightWaveCollectionChromeTest {
    @Test fun compactPhonesKeepSingleLineLayouts() {
        assertEquals(1, NightWaveCollectionChrome.lineCount(320f))
        assertEquals(1, NightWaveCollectionChrome.lineCount(429.99f))
    }

    @Test fun largerDisplaysMayShowMoreMetadata() {
        assertEquals(2, NightWaveCollectionChrome.lineCount(430f))
        assertEquals(2, NightWaveCollectionChrome.lineCount(740f))
    }

    @Test fun malformedWidthsFailClosedToCompactMode() {
        assertEquals(1, NightWaveCollectionChrome.lineCount(Float.NaN))
        assertEquals(1, NightWaveCollectionChrome.lineCount(Float.POSITIVE_INFINITY))
        assertEquals(1, NightWaveCollectionChrome.lineCount(-1f))
    }

    @Test fun collectionSubtitlesUseSingularAndPlural() {
        assertEquals("1 saved track", NightWaveCollectionChrome.sectionSubtitle(1, "saved track", "saved tracks"))
        assertEquals("2 saved tracks", NightWaveCollectionChrome.sectionSubtitle(2, "saved track", "saved tracks"))
        assertEquals("0 saved tracks", NightWaveCollectionChrome.sectionSubtitle(0, "saved track", "saved tracks"))
    }

    @Test fun impossibleNegativeCountsNeverReachVisibleUi() {
        assertEquals("0 tracks", NightWaveCollectionChrome.sectionSubtitle(-100, "track", "tracks"))
    }

    @Test fun sourceLabelsDoNotMisrepresentPublicResults() {
        assertEquals("Search result", NightWaveCollectionChrome.sourceLabel(NightWaveTrackSurface.SEARCH))
        assertEquals("Saved in your library", NightWaveCollectionChrome.sourceLabel(NightWaveTrackSurface.LIBRARY))
    }

    @Test fun sourceLabelsDistinguishHomeFromLibrary() {
        assertEquals("Recently heard", NightWaveCollectionChrome.sourceLabel(NightWaveTrackSurface.HOME))
    }

    @Test fun touchAndArtworkSizingStayWithinCompactCards() {
        assertTrue(NightWaveCollectionChrome.TRACK_MIN_HEIGHT_DP >= 64)
        assertTrue(NightWaveCollectionChrome.COLLECTION_MIN_HEIGHT_DP >= 72)
        assertTrue(NightWaveCollectionChrome.TRACK_ARTWORK_DP < NightWaveCollectionChrome.TRACK_MIN_HEIGHT_DP)
        assertTrue(NightWaveCollectionChrome.COLLECTION_ARTWORK_DP < NightWaveCollectionChrome.COLLECTION_MIN_HEIGHT_DP)
    }
}
