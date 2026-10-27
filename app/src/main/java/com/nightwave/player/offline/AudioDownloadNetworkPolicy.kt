package com.nightwave.player.offline

import java.io.IOException

/**
 * NW79: responses that must be retried using WorkManager's persisted
 * exponential backoff, rather than issuing immediate requests in a tight loop.
 * No URL, header, cookie or server response body is included in the exception.
 */
internal object AudioDownloadNetworkPolicy {
    // A 250 MiB authorized recording over ~35 KiB/s takes over two hours.
    // The idle read timeout, byte cap, foreground cancellation and storage
    // checks remain bounded independently of this total transfer window.
    const val MAX_TRANSFER_MINUTES = 180L

    fun shouldBackOff(statusCode: Int): Boolean =
        statusCode == 408 || statusCode == 425 || statusCode == 429 ||
            statusCode == 500 || statusCode == 502 ||
            statusCode == 503 || statusCode == 504

    /**
     * NW114: requests always start at byte zero and never include Range.
     * A bare 206 or a contradictory Content-Range on 200 MUST NOT be
     * appended, trusted as a full recording, or committed to Offline Library.
     * Accept-Ranges advertising support is not a resumable checkpoint.
     */
    fun isFullStartResponse(statusCode: Int, contentRange: String?): Boolean =
        statusCode == 200 && contentRange.isNullOrBlank()
}

internal class RetryableAudioSourceResponse(val statusCode: Int) :
    IOException("HTTP $statusCode")
