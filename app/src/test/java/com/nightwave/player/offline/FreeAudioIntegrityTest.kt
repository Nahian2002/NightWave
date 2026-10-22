package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

class FreeAudioIntegrityTest {
    @Test fun genuineSourceHashMatchesFullFile() {
        val song = File.createTempFile("nightwave-hash", ".ogg")
        try {
            song.writeBytes(ByteArray(65537) { (it % 227).toByte() })
            val expected = BigInteger(1,
                MessageDigest.getInstance("SHA-1").digest(song.readBytes()))
                .toString(36).padStart(31, '0')
            assertTrue(FreeAudioIntegrity.isValidSourceHash(expected))
            assertEquals(expected, FreeAudioIntegrity.sourceSha1Base36(song))
            assertTrue(FreeAudioIntegrity.sourceMatches(song, expected))
        } finally {
            song.delete()
        }
    }

    @Test fun changedAudioCannotBeIndexedAsCatalogOriginal() {
        val song = File.createTempFile("nightwave-changed", ".mp3")
        try {
            song.writeBytes("original audio fixture".toByteArray())
            val expected = FreeAudioIntegrity.sourceSha1Base36(song)
            song.appendBytes(byteArrayOf(1))
            assertFalse(FreeAudioIntegrity.sourceMatches(song, expected))
        } finally {
            song.delete()
        }
    }

    @Test fun malformedOrMissingHashFailsClosed() {
        assertFalse(FreeAudioIntegrity.isValidSourceHash("bad"))
        assertFalse(FreeAudioIntegrity.isValidSourceHash("0".repeat(32)))
        assertFalse(FreeAudioIntegrity.isValidSourceHash("z".repeat(30) + "!"))
        assertTrue(FreeAudioIntegrity.isValidSourceHash("0".repeat(31)))
    }
}
