package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLibraryRecoveryGuideTest {
    private val guide = OfflineLibraryRecoveryGuide

    @Test fun everyIndexedFileHealthHasDistinctHonestGuidance() {
        val titles = OfflineFileHealth.entries.map(guide::title)
        assertEquals(OfflineFileHealth.entries.size, titles.toSet().size)
        for (health in OfflineFileHealth.entries) {
            assertTrue(guide.title(health).isNotBlank())
            assertTrue(guide.help(health).length > 30)
        }
    }

    @Test fun damagedAudioIsNeverAutoDeletedOrForcedToPlay() {
        for (health in OfflineFileHealth.entries.filter {
            it != OfflineFileHealth.AVAILABLE
        }) {
            val help = guide.help(health)
            assertFalse(help.contains("we deleted"))
            assertFalse(help.contains("play anyway"))
            assertFalse(help.contains("auto-repair"))
        }
        assertTrue(guide.help(OfflineFileHealth.SIZE_CHANGED).contains("Preserve"))
        assertTrue(guide.help(OfflineFileHealth.MISSING).contains("index"))
        assertTrue(guide.help(OfflineFileHealth.UNSAFE_METADATA).contains("backup"))
        assertTrue(guide.help(OfflineFileHealth.UNREADABLE).contains("accessibility"))
    }

    @Test fun issueSummaryRemainsAccurateAtZeroOneOrLargeCounts() {
        assertTrue(guide.emptyIssuesMessage(0).contains("No indexed audio issues"))
        assertTrue(guide.emptyIssuesMessage(1).contains("1 indexed audio entry needs"))
        assertTrue(guide.emptyIssuesMessage(8).contains("8 indexed audio entries need"))
        assertTrue(guide.emptyIssuesMessage(8).contains("options button"))
        assertEquals("Recheck saved files", guide.recheckActionLabel())
    }
}
