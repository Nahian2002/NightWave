package com.nightwave.player.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFileVerificationPolicyTest {
    private val p = DownloadFileVerificationPolicy

    @Test fun repeatedProgressDoesNotRescanUnchangedSavedFileIds() {
        val ids = setOf("saved-a", "saved-b")
        assertTrue(p.needsRead(ids, null))
        assertFalse(p.needsRead(ids, ids))
        assertFalse(p.needsRead(emptySet(), emptySet()))
        assertTrue(p.needsRead(ids, setOf("saved-a")))
        assertTrue(p.needsRead(emptySet(), ids))
    }

    @Test fun staleAndDestroyedActivitiesCannotApplyFileResults() {
        assertTrue(p.canPublish(4, 4, false, false))
        assertFalse(p.canPublish(4, 5, false, false))
        assertFalse(p.canPublish(-1, -1, false, false))
        assertFalse(p.canPublish(4, 4, true, false))
        assertFalse(p.canPublish(4, 4, false, true))
    }

    @Test fun succeededIsNotReadyUntilRealFilesHaveBeenVerified() {
        val saved = setOf("saved-a")
        assertFalse(p.ready(true, "saved-a", saved, true, false))
        assertFalse(p.ready(true, "saved-a", saved, false, true))
        assertTrue(p.ready(true, "saved-a", saved, false, false))
        assertFalse(p.ready(false, "saved-a", saved, false, false))
        assertFalse(p.ready(true, "missing", saved, false, false))
        assertFalse(p.ready(true, null, saved, false, false))
    }

    @Test fun unknownFileStatusIsNotFalselyClassifiedMissingOrBroken() {
        assertFalse(p.needsAttention("SUCCEEDED", "saved-a", emptySet(), true, false))
        assertFalse(p.needsAttention("SUCCEEDED", "saved-a", emptySet(), false, true))
        assertTrue(p.needsAttention("SUCCEEDED", "missing", emptySet(), false, false))
        assertFalse(p.needsAttention("SUCCEEDED", "saved-a", setOf("saved-a"), false, false))
        assertTrue(p.needsAttention("FAILED", null, emptySet(), false, false))
        assertTrue(p.needsAttention("CANCELLED", null, emptySet(), true, false))
        assertFalse(p.needsAttention("RUNNING", null, emptySet(), false, false))
    }
}
