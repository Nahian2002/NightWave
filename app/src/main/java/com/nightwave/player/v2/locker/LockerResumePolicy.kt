package com.nightwave.player.v2.locker

/** Validate a Range response before appending even a single byte to private audio. */
object LockerResumePolicy {
    const val MAX_ATTEMPTS = 3
    const val CHECKPOINT_STEP = 1024L * 1024L
    private val contentRange = Regex("""bytes (\d+)-(\d+)/(\d+)""")

    fun verifiedSpan(
        offset: Long, total: Long, code: Int, range: String?, declaredBytes: Long
    ): Long {
        require(total in 1..LockerAudioPolicy.MAX_FILE_BYTES && offset in 0 until total)
        if (offset == 0L && code == 200) {
            require(declaredBytes == -1L || declaredBytes == total) {
                "Full Drive download content length changed"
            }
            return total
        }
        require(code == 206) { "Drive did not honor resume byte range" }
        val match = contentRange.matchEntire(range.orEmpty())
            ?: error("Missing or invalid Drive Content-Range")
        val (first, last, length) = match.destructured
        val start = first.toLong()
        val end = last.toLong()
        require(start == offset && end >= start && end < total &&
            length.toLong() == total) { "Drive returned a different byte range" }
        val count = end - start + 1
        require(declaredBytes == -1L || declaredBytes == count) {
            "Drive resume payload length inconsistent"
        }
        return count
    }

    fun percent(bytes: Long, total: Long): Int {
        require(total > 0 && bytes in 0..total)
        return (bytes * 100L / total).toInt().coerceAtMost(99)
    }

    fun shouldRetry(attempt: Int, exception: Exception): Boolean =
        attempt < MAX_ATTEMPTS && exception is java.io.IOException
}
