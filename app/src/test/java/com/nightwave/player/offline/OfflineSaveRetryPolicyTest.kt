package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSaveRetryPolicyTest {
    @Test fun usesSameCanonicalVideoIdForExplicitRetry() {
        val canonical = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val tag = OfflineSaveRetryPolicy.sourceTag(canonical)
        assertEquals(canonical, OfflineSaveRetryPolicy.retryUrl(setOf(tag)))
        assertTrue(OfflineSaveRetryPolicy.canOfferRetry(true, false, setOf(tag)))
    }

    @Test fun invalidOrAmbiguousVideoTagsAreRejected() {
        val tag = OfflineSaveRetryPolicy.SOURCE_TAG_PREFIX
        assertNull(OfflineSaveRetryPolicy.retryUrl(emptySet()))
        assertNull(OfflineSaveRetryPolicy.retryUrl(setOf(tag + "too-short")))
        assertNull(OfflineSaveRetryPolicy.retryUrl(setOf(tag + "https://x")))
        assertNull(OfflineSaveRetryPolicy.retryUrl(setOf(
            tag + "dQw4w9WgXcQ", tag + "aaaaaaaaaaa")))
        assertFalse(OfflineSaveRetryPolicy.canOfferRetry(
            true, false, setOf(tag + "too-short")))
    }

    @Test fun activeAndAvailableAudioNeverOfferRetry() {
        val tags = setOf(OfflineSaveRetryPolicy.SOURCE_TAG_PREFIX + "dQw4w9WgXcQ")
        assertFalse(OfflineSaveRetryPolicy.canOfferRetry(false, false, tags))
        assertFalse(OfflineSaveRetryPolicy.canOfferRetry(true, true, tags))
        assertTrue(OfflineSaveRetryPolicy.canOfferRetry(true, false, tags))
    }

    @Test fun newestJobTimestampIsReadSafely() {
        val prefix = OfflineSaveRetryPolicy.CREATED_TAG_PREFIX
        assertEquals(0L, OfflineSaveRetryPolicy.requestedAt(emptySet()))
        assertEquals(0L, OfflineSaveRetryPolicy.requestedAt(setOf(prefix + "oops")))
        assertEquals(0L, OfflineSaveRetryPolicy.requestedAt(setOf(prefix + "-1")))
        assertEquals(2000L, OfflineSaveRetryPolicy.requestedAt(
            setOf(prefix + "1000", prefix + "2000", prefix + "too-large-amount")))
    }
}
