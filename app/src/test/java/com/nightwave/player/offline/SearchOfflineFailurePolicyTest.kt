package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchOfflineFailurePolicyTest {
    @Test fun refusedAndRateLimitedSourcesHaveDistinctMessages() {
        assertTrue(SearchOfflineFailurePolicy.message("Audio server returned HTTP 403.", false)
            .contains("denied access"))
        assertTrue(SearchOfflineFailurePolicy.message("Audio server returned HTTP 429.", false)
            .contains("rate-limited"))
    }

    @Test fun unsupportedAudioIsNotMistakenForNetworkOutage() {
        assertTrue(SearchOfflineFailurePolicy.message(
            "No independent audio-only stream is available", false)
            .contains("standalone audio"))
        assertTrue(SearchOfflineFailurePolicy.message(
            "The link returned invalid audio or a different format.", false)
            .contains("unsupported audio"))
        assertTrue(SearchOfflineFailurePolicy.message(
            "Not enough device storage for this offline audio file.", false)
            .contains("storage"))
    }

    @Test fun diskExhaustionDuringUnknownLengthTransferIsExplained() {
        assertTrue(SearchOfflineFailurePolicy.message(
            "Device storage is too low to finish saving audio safely.", false)
            .contains("Free space"))
    }

    @Test fun networkFailureIsRetryableByUser() {
        assertTrue(SearchOfflineFailurePolicy.message("Connection reset", true)
            .contains("Connection interrupted"))
    }

    @Test fun neverRevealsSignedStreamUrlsOrServerTokens() {
        val secret = "https://r2---sn-private.googlevideo.com/videoplayback?token=private_secret"
        val rendered = SearchOfflineFailurePolicy.message(secret, false)
        assertEquals("This recording could not be stored. It is not available offline.", rendered)
        assertFalse(rendered.contains("private_secret"))
        assertFalse(rendered.contains("googlevideo"))
    }

    @Test fun transientSourceStatusesUseRedactedRetryGuidance() {
        for (code in listOf(500, 502, 503, 504)) {
            val rendered = SearchOfflineFailurePolicy.message(
                "HTTP $code https://example.org/?secret=never-leak", true)
            assertEquals("Audio source is temporarily unavailable. Retry later.", rendered)
            assertFalse(rendered.contains("secret="))
        }
        for (code in listOf(408, 425)) {
            assertEquals("Connection interrupted before audio was saved. Retry when online.",
                SearchOfflineFailurePolicy.message("HTTP $code", true))
        }
    }

    @Test fun partialAudioRejectionIsSafeAndNotRepresentedAsSaved() {
        val explanation = SearchOfflineFailurePolicy.message(
            "Partial audio response; no trusted resume checkpoint exists " +
                "https://source.example/?token=secret", false)
        assertEquals("The audio source returned partial audio; no offline copy was saved.",
            explanation)
        assertFalse(explanation.contains("token"))
    }
}
