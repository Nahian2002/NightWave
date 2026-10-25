package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure Kotlin: no media, network, credentials or WorkManager dependency. */
class DownloadRecoveryPolicyTest {
    private val p = DownloadRecoveryPolicy

    @Test fun workFailureCannotDisplaySecretUrlsOrLocalPaths() {
        val secret = "Failed fetching https://cdn.example.org/path?token=super_secret at /data/user/0/private/song"
        val message = p.failedMessage(secret)
        assertEquals("Download failed. This recording is not available offline.", message)
        assertFalse(message.contains("super_secret"))
        assertFalse(message.contains("cdn.example"))
        assertFalse(message.contains("/data/"))
        assertFalse(p.failedMessage(null).contains("null"))
    }

    @Test fun actionableSafeFailureCategories() {
        assertTrue(p.failedMessage("Device storage is too low").contains("Free device space"))
        assertTrue(p.failedMessage("ENOSPC").contains("storage"))
        assertTrue(p.failedMessage("HTTP 429").contains("Retry later"))
        assertTrue(p.failedMessage("Audio source denied access").contains("permitted alternative"))
        assertTrue(p.failedMessage("HTTP 403").contains("denied access"))
        assertTrue(p.failedMessage("No supported public audio stream").contains("supported playable"))
        assertTrue(p.failedMessage("Invalid audio").contains("supported playable"))
        assertTrue(p.failedMessage("Audio failed a security or permission check").contains("permission"))
        assertTrue(p.failedMessage("HTTP 404").contains("no longer available"))
        assertTrue(p.failedMessage("Connection interrupted").contains("Check internet"))
        assertTrue(p.failedMessage("Audio was saved in Offline Library, but its search link was not recorded")
            .contains("already be in Offline Library"))
    }

    @Test fun pausedOnlyAfterWorkManagerReportsCancelled() {
        assertFalse(p.isPaused("RUNNING", true))
        assertFalse(p.isPaused("FAILED", true))
        assertFalse(p.isPaused("SUCCEEDED", true))
        assertTrue(p.isPaused("CANCELLED", true))
        assertFalse(p.isPaused("CANCELLED", false))
        assertTrue(p.isPausing("RUNNING", true))
        assertTrue(p.isPausing("BLOCKED", true))
        assertFalse(p.isPausing("CANCELLED", true))
        assertFalse(p.isPausing("RUNNING", false))
    }

    @Test fun pauseAndRetryActionsAreStrictAboutLifecycleState() {
        for (state in listOf("ENQUEUED", "RUNNING", "BLOCKED")) {
            assertTrue(p.mayRequestPause(state, false, true))
            assertFalse(p.mayRequestPause(state, true, true))
            assertFalse(p.mayRequestPause(state, false, false))
        }
        for (state in listOf("FAILED", "SUCCEEDED", "CANCELLED")) {
            assertFalse(p.mayRequestPause(state, false, true))
        }
        assertEquals("Resume (restarts from zero)", p.retryLabel("CANCELLED", true))
        assertEquals("Retry saving this song", p.retryLabel("FAILED", true))
        assertEquals("Retry saving this song", p.retryLabel("CANCELLED", false))
    }
}
