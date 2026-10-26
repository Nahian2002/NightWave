package com.nightwave.player.playback

import org.junit.Assert.*
import org.junit.Test

class MediaSessionQueueControlPolicyTest {
    private val p = MediaSessionQueueControlPolicy

    @Test fun appAndNotificationControllersMayReceiveQueueButtons() {
        assertTrue(p.trustedController(true, false))
        assertTrue(p.trustedController(false, true))
        assertTrue(p.trustedController(true, true))
    }

    @Test fun externalControllerCannotObtainPrivilegedCustomQueueButtons() {
        assertFalse(p.trustedController(false, false))
    }

    @Test fun commandNamesMustBeExactAndDistinct() {
        assertTrue(p.knownAction("next", "next", "previous"))
        assertTrue(p.knownAction("previous", "next", "previous"))
        assertFalse(p.knownAction("seek", "next", "previous"))
        assertFalse(p.knownAction(null, "next", "previous"))
    }

    @Test fun corruptedCommandIdentifiersCannotAuthoriseNavigation() {
        assertFalse(p.knownAction("next", "", "previous"))
        assertFalse(p.knownAction("next", "next", "next"))
        assertFalse(p.knownAction("previous", "next", ""))
    }

    @Test fun publicSelectionCanBeNavigatedWithoutLoadedMediaItem() {
        assertTrue(p.canNavigate(false, false, true))
    }

    @Test fun offlineSelectionSupportsGaplessAndLegacyFallback() {
        assertTrue(p.canNavigate(true, true, false))
    }

    @Test fun noSelectedTrackCannotAcknowledgeSuccessfulNavigation() {
        assertFalse(p.canNavigate(false, false, false))
        assertFalse(p.canNavigate(true, false, false))
    }

    @Test fun switchingModeNeverNavigatesAnOldSelectionFromOtherMode() {
        assertFalse(p.canNavigate(true, false, true))
        assertFalse(p.canNavigate(false, true, false))
    }
}
