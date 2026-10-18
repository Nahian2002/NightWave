package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

/** Pure Kotlin UI policy tests: no accounts, music service or device required. */
class NightWaveChromePolicyTest {
    @Test fun morningStartsAtFiveAndEndsBeforeNoon() {
        assertEquals("Good evening", NightWaveChromePolicy.greeting(4))
        assertEquals("Good morning", NightWaveChromePolicy.greeting(5))
        assertEquals("Good morning", NightWaveChromePolicy.greeting(11))
    }

    @Test fun afternoonAndEveningBoundariesAreDeterministic() {
        assertEquals("Good afternoon", NightWaveChromePolicy.greeting(12))
        assertEquals("Good afternoon", NightWaveChromePolicy.greeting(17))
        assertEquals("Good evening", NightWaveChromePolicy.greeting(18))
        assertEquals("Good evening", NightWaveChromePolicy.greeting(23))
    }

    @Test fun invalidClockInputFailsClearly() {
        assertThrows(IllegalArgumentException::class.java) {
            NightWaveChromePolicy.greeting(-1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NightWaveChromePolicy.greeting(24)
        }
    }

    @Test fun emptyAndWhitespaceProfileNamesRemainReadable() {
        assertEquals("your space", NightWaveChromePolicy.displayFirstName(""))
        assertEquals("your space", NightWaveChromePolicy.displayFirstName("  \n  "))
        assertEquals("Demo", NightWaveChromePolicy.displayFirstName("  Demo  Listener "))
    }

    @Test fun largeProfileNameIsBoundedForNarrowHero() {
        assertTrue(NightWaveChromePolicy.displayFirstName("x".repeat(100)).length <= 22)
    }

    @Test fun onlyShowNextForMultipleTracksAndEnoughRoom() {
        assertFalse(NightWaveChromePolicy.showMiniNext(1, 500f))
        assertFalse(NightWaveChromePolicy.showMiniNext(2, 320f))
        assertFalse(NightWaveChromePolicy.showMiniNext(2, 364.99f))
        assertTrue(NightWaveChromePolicy.showMiniNext(2, 365f))
        assertTrue(NightWaveChromePolicy.showMiniNext(20, 450f))
    }

    @Test fun undefinedOrInfiniteWidthNeverEnablesNext() {
        assertFalse(NightWaveChromePolicy.showMiniNext(2, Float.NaN))
        assertFalse(NightWaveChromePolicy.showMiniNext(2, Float.POSITIVE_INFINITY))
        assertFalse(NightWaveChromePolicy.showMiniNext(2, -100f))
    }

    @Test fun navigationIndexesClampToFourKnownTabs() {
        assertEquals(4, NightWaveChromePolicy.NAV_ITEM_COUNT)
        assertEquals(0, NightWaveChromePolicy.selectedTab(-1))
        assertEquals(0, NightWaveChromePolicy.selectedTab(0))
        assertEquals(3, NightWaveChromePolicy.selectedTab(3))
        assertEquals(3, NightWaveChromePolicy.selectedTab(999))
    }

    @Test fun navigationHasAdequateTouchHeight() {
        assertTrue(NightWaveChromePolicy.NAV_MIN_TOUCH_DP >= 48)
        assertTrue(NightWaveChromePolicy.NAV_HEIGHT_DP >=
            NightWaveChromePolicy.NAV_MIN_TOUCH_DP)
    }
}
