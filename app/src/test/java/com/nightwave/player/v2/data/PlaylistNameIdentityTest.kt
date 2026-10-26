package com.nightwave.player.v2.data

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistNameIdentityTest {
    @Test fun canonicalCaseKeyIsIndependentOfDeviceLanguage() {
        val original = Locale.getDefault()
        try {
            for (language in listOf("en-US", "tr-TR", "az-AZ", "lt-LT", "bn-BD")) {
                Locale.setDefault(Locale.forLanguageTag(language))
                assertEquals("i", PlaylistNameIdentity.key("I"))
                assertEquals("i", PlaylistNameIdentity.key("i"))
                assertTrue(PlaylistNameIdentity.same("I", "i"))
                assertFalse(PlaylistNameIdentity.allDistinct(listOf("I", "i")))
            }
        } finally { Locale.setDefault(original) }
    }

    @Test fun driveMergeIdentityMatchesBackupValidation() {
        assertTrue(PlaylistNameIdentity.same("MIX", "mix"))
        assertFalse(PlaylistNameIdentity.allDistinct(listOf("MIX", "mix")))
        assertTrue(PlaylistNameIdentity.allDistinct(listOf("MIX", "Focus")))
        assertFalse(PlaylistNameIdentity.same("MIX", "Focus"))
    }

    @Test fun dottedUnicodeIIsNeverSilentlyCoalescedWithAsciiI() {
        assertEquals("i\u0307", PlaylistNameIdentity.key("İ"))
        assertEquals("i", PlaylistNameIdentity.key("I"))
        assertTrue(PlaylistNameIdentity.allDistinct(listOf("İ", "i")))
        assertFalse(PlaylistNameIdentity.same("I", "ı"))
    }

    @Test fun canonicalizationDoesNotChangeDisplayNames() {
        val names = listOf("My Mix", "  हिन्दी ", "Bangla গান")
        val before = names.toList()
        assertTrue(PlaylistNameIdentity.allDistinct(names))
        assertEquals(before, names)
        assertEquals("My Mix", names.first())
    }

    @Test fun duplicateAtMaximumCollectionSizeIsStillRejected() {
        val names = (1..50).map { "Playlist $it" }
        assertTrue(PlaylistNameIdentity.allDistinct(names))
        assertFalse(PlaylistNameIdentity.allDistinct(
            names.dropLast(1) + "pLAYLIST 1"))
    }

    @Test fun emptyAndSingleItemCollectionsRemainValid() {
        assertTrue(PlaylistNameIdentity.allDistinct(emptyList()))
        assertTrue(PlaylistNameIdentity.allDistinct(listOf("Ambient")))
        assertEquals(PlaylistNameIdentity.key("Ambient"),
            PlaylistNameIdentity.key("AMBIENT"))
    }
}
