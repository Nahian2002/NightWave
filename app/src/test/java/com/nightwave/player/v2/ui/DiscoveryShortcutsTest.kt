package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class DiscoveryShortcutsTest {
    @Test fun allSearchShortcutsAreUniqueAndValid() {
        assertTrue(DiscoveryShortcuts.isValid())
        assertEquals(6, DiscoveryShortcuts.picks.size)
        assertEquals(6, DiscoveryShortcuts.picks.map { it.query }.distinct().size)
    }

    @Test fun knownShortcutKeepsItsPublicQuery() {
        assertEquals("night drive synthwave music", DiscoveryShortcuts.find("late")?.query)
        assertEquals("new bangla hindi music", DiscoveryShortcuts.find("desi")?.query)
    }

    @Test fun unknownShortcutNeverSearchesUnexpectedContent() {
        assertNull(DiscoveryShortcuts.find(""))
        assertNull(DiscoveryShortcuts.find("../../unknown"))
    }
}
