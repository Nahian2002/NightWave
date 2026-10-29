package com.nightwave.player.net

import java.io.BufferedOutputStream
import java.io.OutputStream

/** Batches small socket reads into sequential disk writes without retaining a whole song. */
internal object MediaTransferIo {
    const val BUFFER_BYTES = 256 * 1024

    /** Caller owns the stream and fsync. Flush must finish before its durable sync/index commit. */
    fun <T> bufferedWrite(output: OutputStream, write: (OutputStream) -> T): T {
        val buffered = BufferedOutputStream(output, BUFFER_BYTES)
        val result = write(buffered)
        buffered.flush()
        return result
    }
}
