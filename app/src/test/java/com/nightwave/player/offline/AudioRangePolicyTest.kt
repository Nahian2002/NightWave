package com.nightwave.player.offline

import okhttp3.Headers
import org.junit.Assert.*
import org.junit.Test

class AudioRangePolicyTest {
    @Test fun onlyExactSingleBoundedRangesAreAccepted() {
        assertEquals(AudioRangePolicy.Range(0, 9, 10), AudioRangePolicy.parse("bytes 0-9/10"))
        listOf(null, "bytes */10", "bytes 0-9/*", "bytes 9-0/10", "bytes 0-10/10",
            "bytes -1-9/10", "bytes 0-999999999999/1000000000000", "items 0-9/10",
            "bytes 0-9/10,20-29/30", "bytes 0-9/0").forEach { assertNull(AudioRangePolicy.parse(it)) }
    }
    @Test fun strongEtagWinsAndWeakEtagCannotBeReplacedWithDate() {
        assertEquals("\"strong\"", AudioRangePolicy.validator(Headers.headersOf("ETag", "\"strong\"")))
        assertNull(AudioRangePolicy.validator(Headers.headersOf("ETag", "W/\"weak\"",
            "Last-Modified", "Wed, 28 Oct 2026 10:00:00 GMT", "Date", "Wed, 28 Oct 2026 11:00:00 GMT")))
    }
    @Test fun dateRequiresNoEtagAndConservativeSeparation() {
        val modified = "Wed, 28 Oct 2026 10:00:00 GMT"
        assertEquals(modified, AudioRangePolicy.validator(Headers.headersOf("Last-Modified", modified,
            "Date", "Wed, 28 Oct 2026 10:01:00 GMT")))
        assertNull(AudioRangePolicy.validator(Headers.headersOf("Last-Modified", modified,
            "Date", "Wed, 28 Oct 2026 10:00:59 GMT")))
        assertNull(AudioRangePolicy.validator(Headers.headersOf("Last-Modified", modified)))
        assertNull(AudioRangePolicy.validator(Headers.headersOf("Last-Modified", "invalid", "Date", modified)))
    }
    @Test fun identityIsStableButDifferentRepresentationsAreSeparate() {
        assertEquals(AudioRangePolicy.key("x"), AudioRangePolicy.key("x"))
        assertNotEquals(AudioRangePolicy.key("x"), AudioRangePolicy.key("y"))
        assertTrue(Regex("[a-f0-9]{64}").matches(AudioRangePolicy.key("https://private.example/token")))
    }
    @Test fun diagnosticsDoNotContainEndpointOrAccount() {
        val metrics = AudioTransferMetrics()
        metrics.mode = "Bounded ranges"
        metrics.wireBytes = 125000
        val details = metrics.summary()
        assertTrue(details.contains("125000 bytes"))
        listOf("https:", "token", "@", "Cookie", "Authorization").forEach { assertFalse(details.contains(it)) }
        assertEquals("122 KiB/s · 1.00 Mbps", OfflineTransferRatePolicy.preciseDisplay(125000))
    }
}
