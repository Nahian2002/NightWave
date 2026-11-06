package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedSearchScopeTest {
    @Test fun onlyExplicitExternalScopesHaveProviderRequests() {
        for (scope in listOf(UnifiedSearchScope.MUSIC, UnifiedSearchScope.LOCAL, UnifiedSearchScope.SAVED)) {
            assertNull(UnifiedSourceRequest.create(scope, "piano"))
        }
        assertEquals(setOf(DiscoverySource.AUDIUS, DiscoverySource.PEERTUBE, DiscoverySource.RADIO),
            UnifiedSearchScope.ALL.providers)
        assertTrue(UnifiedSearchScope.ALL.showsLocal)
        assertEquals(setOf(DiscoverySource.PEERTUBE), UnifiedSearchScope.PEERTUBE.providers)
        assertNull(UnifiedSourceRequest.create(UnifiedSearchScope.RADIO, "  "))
    }

    @Test fun resultIdentityRequiresTheSameFilterAndExactSubmittedQuery() {
        val request = UnifiedSourceRequest.create(UnifiedSearchScope.PEERTUBE, "  বাংলা গান  ")!!
        assertTrue(request.matches(UnifiedSearchScope.PEERTUBE, "বাংলা গান"))
        assertFalse(request.matches(UnifiedSearchScope.PEERTUBE, "বাংলা"))
        assertFalse(request.matches(UnifiedSearchScope.ALL, "বাংলা গান"))
        assertNotEquals(request, UnifiedSourceRequest.create(UnifiedSearchScope.PEERTUBE, "বাংলা গান"))
    }

    @Test fun referencesKeepProviderIdentityAndExactAddressAcrossStorageRoundTrips() {
        val address = "https://peertube.example.org/w/piano1"
        val peer = SavedSourceLink.from(DiscoverySource.PEERTUBE, address)!!
        val radio = SavedSourceLink.from(DiscoverySource.RADIO, address)!!
        assertNotEquals(peer, radio)
        val encoded = SavedSourceLinksCodec.encode(listOf(peer, radio))
        assertEquals(listOf(peer, radio), SavedSourceLinksCodec.decode(encoded))
        assertFalse(encoded.contains("duration"))
        assertFalse(encoded.contains("stream"))
    }

    @Test fun corruptOrUnsupportedBookmarksNeverBecomePlayableOrReplaceStoredLinks() {
        assertNull(SavedSourceLinksCodec.decode("[broken"))
        assertNull(SavedSourceLinksCodec.decode("""[{"source":"PEERTUBE","url":"https://127.0.0.1/w/abc"}]"""))
        assertNull(SavedSourceLink.from(DiscoverySource.PEERTUBE, "https://peer.example.org/"))
        assertNull(SavedSourceLink.from(DiscoverySource.AUDIUS, "https://audius.co/artist/piano"))
        val link = SavedSourceLink.from(DiscoverySource.RADIO, "https://radio.example.org/")!!
        assertNull(SavedSourceLinksCodec.decode(SavedSourceLinksCodec.encode(listOf(link, link))))
    }
}
