package com.nightwave.player.v2.playback

import org.junit.Assert.*
import org.junit.Test

class PlayerSnapshotPolicyTest {
    @Test fun normalPlayheadTickNeverRequiresDiskSnapshot() {
        assertFalse(PlayerSnapshotPolicy.needsFullRefresh("offline:id.mp3", "offline:id.mp3", 0, true))
        assertFalse(PlayerSnapshotPolicy.needsFullRefresh("https://www.youtube.com/watch?v=abc",
            "https://www.youtube.com/watch?v=abc", 6, true))
    }

    @Test fun periodicReconciliationStillSeesQueueChanges() {
        assertTrue(PlayerSnapshotPolicy.needsFullRefresh(null, "", 7, false))
        assertTrue(PlayerSnapshotPolicy.needsFullRefresh("offline:a.mp3", "offline:a.mp3", 7, false))
    }

    @Test fun mediaTransitionTriggersImmediateFullRefresh() {
        assertTrue(PlayerSnapshotPolicy.needsFullRefresh("offline:new.mp3", "offline:old.mp3", 0, true))
        assertTrue(PlayerSnapshotPolicy.needsFullRefresh("https://www.youtube.com/watch?v=new",
            "https://www.youtube.com/watch?v=old", 0, true))
    }

    @Test fun emptyUnpreparedPlayerCanShowRestoredMetadata() {
        assertFalse(PlayerSnapshotPolicy.needsFullRefresh(null,
            "https://www.youtube.com/watch?v=restored", 0, false))
    }

    @Test fun mediaLossAfterActivePlaybackMustReconcile() {
        assertTrue(PlayerSnapshotPolicy.needsFullRefresh(null,
            "https://www.youtube.com/watch?v=old", 0, true))
    }
}
