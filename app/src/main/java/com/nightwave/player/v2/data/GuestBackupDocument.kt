package com.nightwave.player.v2.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** One confirmed picker owns one in-memory snapshot; never persist private archive bytes in a Bundle. */
internal class GuestBackupExportSlot {
    private var pending: ByteArray? = null
    val hasPending: Boolean get() = pending != null

    fun prepare(bytes: ByteArray) {
        check(!hasPending)
        require(bytes.size in 1..LibraryBackupCodec.MAX_BYTES)
        pending = bytes.copyOf()
    }

    fun complete(hasDestination: Boolean): ByteArray? {
        val snapshot = pending
        pending = null
        return if (hasDestination) snapshot else null
    }
}

internal object GuestBackupDocument {
    fun read(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val byte = input.read()
                if (byte < 0) break
                require(output.size() < LibraryBackupCodec.MAX_BYTES)
                output.write(byte)
            } else {
                require(output.size().toLong() + count <= LibraryBackupCodec.MAX_BYTES)
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }
}
