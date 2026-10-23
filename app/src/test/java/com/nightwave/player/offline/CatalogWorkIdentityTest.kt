package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM-only source identity tests: no network, user data or Android services. */
class CatalogWorkIdentityTest {
    private val audio = "https://upload.wikimedia.org/wikipedia/commons/a/ab/Song.ogg"
    private val hash = "0".repeat(31)

    @Test fun sameSourceIsAlwaysOneUniqueQueue() {
        assertEquals(CatalogWorkIdentity.uniqueName(audio, hash),
            CatalogWorkIdentity.uniqueName(audio, hash))
        val name = CatalogWorkIdentity.uniqueName(audio, hash)
        assertTrue(name.matches(Regex("nw23-[0-9a-f]{36}")))
        assertFalse(name.contains("wikimedia.org"))
        assertFalse(name.contains("Song"))
    }

    @Test fun differentOriginalOrChecksumMakesDifferentKey() {
        val other = "https://upload.wikimedia.org/wikipedia/commons/a/ab/Another.ogg"
        assertNotEquals(CatalogWorkIdentity.uniqueName(audio, hash),
            CatalogWorkIdentity.uniqueName(other, hash))
        assertNotEquals(CatalogWorkIdentity.uniqueName(audio, hash),
            CatalogWorkIdentity.uniqueName(audio, "1".repeat(31)))
    }

    @Test fun forgedAndMalformedSourcesNeverGetAQueueIdentity() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogWorkIdentity.uniqueName("https://upload.wikimedia.org.bad.invalid/file.ogg", hash)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CatalogWorkIdentity.uniqueName("http://upload.wikimedia.org/wikipedia/commons/f.ogg", hash)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CatalogWorkIdentity.uniqueName(audio, "no checksum")
        }
    }
}
