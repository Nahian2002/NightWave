package com.nightwave.player.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The UI may filter bad entries but must never erase, recover or rewrite them. */
class OfflineLibraryViewPolicyTest {
    @Test fun onlyAvailableFilesCanAppearAsReady() {
        for (health in OfflineFileHealth.entries) {
            assertTrue(OfflineLibraryViewPolicy.includes(
                OfflineLibraryViewPolicy.Filter.ALL, health))
            assertTrue(OfflineLibraryViewPolicy.includes(
                OfflineLibraryViewPolicy.Filter.READY, health) ==
                (health == OfflineFileHealth.AVAILABLE))
            assertTrue(OfflineLibraryViewPolicy.includes(
                OfflineLibraryViewPolicy.Filter.ATTENTION, health) ==
                (health != OfflineFileHealth.AVAILABLE))
        }
    }

    @Test fun emptyMessagesDistinguishLostFilesFromNoSavedSongs() {
        val p = OfflineLibraryViewPolicy
        assertTrue(p.emptyMessage(OfflineLibraryViewPolicy.Filter.ALL, false, false).contains("Import audio"))
        assertTrue(p.emptyMessage(OfflineLibraryViewPolicy.Filter.READY, true, false).contains("No verified"))
        assertTrue(p.emptyMessage(OfflineLibraryViewPolicy.Filter.ATTENTION, true, false).contains("All indexed"))
        assertTrue(p.emptyMessage(OfflineLibraryViewPolicy.Filter.ATTENTION, true, true).contains("search and filter"))
        assertFalse(p.emptyMessage(OfflineLibraryViewPolicy.Filter.ATTENTION, true, false).contains("delete"))
    }
}
