package com.nightwave.player.v2.locker

/**
 * Interprets Google's resumable-upload Range acknowledgement. The response
 * describes bytes that Drive has COMMITTED, not bytes this phone sent.
 * Never infer an offset from an unsuccessful chunk request.
 */
object LockerUploadResumePolicy {
    const val MAX_CONSECUTIVE_FAILURES = 3
    const val CHUNK_ALIGNMENT = 256 * 1024
    private val acceptedRange = Regex("""bytes=0-(\d+)""")

    fun acknowledged(range: String?, total: Long, previouslyConfirmed: Long): Long {
        require(total in 1..LockerAudioPolicy.MAX_FILE_BYTES)
        require(previouslyConfirmed in 0 until total)
        if (range.isNullOrBlank()) {
            require(previouslyConfirmed == 0L) {
                "Drive upload status unexpectedly lost confirmed progress"
            }
            return 0L
        }
        val value = acceptedRange.matchEntire(range)
            ?: throw IllegalArgumentException("Malformed Drive upload Range")
        val last = value.groupValues[1].toLongOrNull()
            ?: throw IllegalArgumentException("Unbounded Drive upload Range")
        require(last >= 0 && last < total)
        val received = last + 1
        require(received >= previouslyConfirmed) {
            "Drive upload status cannot move backwards"
        }
        return received
    }

    fun canSendNext(offset: Long, total: Long): Boolean {
        require(total in 1..LockerAudioPolicy.MAX_FILE_BYTES)
        require(offset in 0 until total)
        // Drive requires all non-final chunks to align to 256 KiB boundaries.
        return offset % CHUNK_ALIGNMENT == 0L ||
            total - offset <= LockerAudioPolicy.CHUNK_BYTES.toLong()
    }

    fun transientStatus(code: Int): Boolean =
        code == 408 || code == 429 || code in 500..599

    fun retryAllowed(failures: Int): Boolean =
        failures in 1 until MAX_CONSECUTIVE_FAILURES
}
