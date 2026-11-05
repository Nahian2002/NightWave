package com.nightwave.player.v2.data

import com.nightwave.player.net.VideoPreview
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Test

class NW152GuestDocumentTest {
    private val song = VideoPreview("গান", "Fixture", "https://www.youtube.com/watch?v=abcdefghijk", "", 90L)
    private val archive = LibraryArchive("Listener", "AMOLED", "orbit", listOf(song), listOf("বাংলা" to listOf(song)))

    @Test fun pickerUsesExactPreparedBytesAndClaimsOnlyOnce() {
        val bytes = LibraryBackupCodec.encode(archive)
        val original = bytes.copyOf()
        val slot = GuestBackupExportSlot()
        slot.prepare(bytes)
        bytes.fill(0)
        try { slot.prepare(original); fail() } catch (_: IllegalStateException) { }
        val saved = slot.complete(true)!!
        assertArrayEquals(original, saved)
        assertEquals(archive, LibraryBackupCodec.decode(GuestBackupDocument.read(saved.inputStream())))
        assertFalse(slot.hasPending)
        assertNull(slot.complete(true))
    }

    @Test fun cancelAndLostProcessNeverProvideReplacementBytes() {
        val slot = GuestBackupExportSlot()
        slot.prepare(LibraryBackupCodec.encode(archive))
        assertNull(slot.complete(false))
        assertNull(slot.complete(true))
        assertNull(GuestBackupExportSlot().complete(true))
    }

    @Test fun emptyAndOversizedSnapshotsNeverBecomePending() {
        for (size in listOf(0, LibraryBackupCodec.MAX_BYTES + 1)) {
            val slot = GuestBackupExportSlot()
            try { slot.prepare(ByteArray(size)); fail() } catch (_: IllegalArgumentException) { }
            assertFalse(slot.hasPending)
        }
    }

    @Test fun readerChecksActualBytesWithoutTrustingProviderLength() {
        val data = ByteArray(LibraryBackupCodec.MAX_BYTES) { (it % 113).toByte() }
        assertArrayEquals(data, GuestBackupDocument.read(data.inputStream()))
        try { GuestBackupDocument.read(ByteArray(data.size + 1).inputStream()); fail() }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun emptyBulkReadCannotSpinOrLoseTheNextByte() {
        val provider = object : InputStream() {
            var next = 65
            override fun read(buffer: ByteArray, offset: Int, count: Int) = 0
            override fun read(): Int = if (next < 68) next++ else -1
        }
        assertArrayEquals("ABC".toByteArray(), GuestBackupDocument.read(provider))
    }
}
