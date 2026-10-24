package com.nightwave.player.v2.cloud

/** Pure bounded read policy, shared by runtime and executable JVM tests. */
object DriveResponseReadPolicy {
    const val MAX_CONSECUTIVE_EMPTY_READS = 8

    fun canAppend(currentBytes: Int, incomingBytes: Int, maxBytes: Int): Boolean =
        maxBytes >= 0 && currentBytes in 0..maxBytes &&
            incomingBytes > 0 && incomingBytes <= maxBytes - currentBytes

    fun stalled(emptyReadCount: Int): Boolean =
        emptyReadCount > MAX_CONSECUTIVE_EMPTY_READS
}
