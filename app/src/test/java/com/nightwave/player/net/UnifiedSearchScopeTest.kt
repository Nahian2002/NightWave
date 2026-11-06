package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.HttpUrl.Companion.toHttpUrl

class UnifiedSearchScopeTest {
    @Test fun onlyExplicitExternalScopesHaveProviderRequests() {
        for (scope in listOf(UnifiedSearchScope.MUSIC, UnifiedSearchScope.LOCAL, UnifiedSearchScope.SAVED)) {
            assertNull(UnifiedSourceRequest.create(scope, "piano"))
        }
        assertEquals(setOf(DiscoverySource.AUDIUS, DiscoverySource.PEERTUBE, DiscoverySource.RADIO, DiscoverySource.OPENVERSE),
            UnifiedSearchScope.ALL.providers)
        assertTrue(UnifiedSearchScope.ALL.showsLocal)
        assertEquals(setOf(DiscoverySource.PEERTUBE), UnifiedSearchScope.PEERTUBE.providers)
        assertEquals(setOf(DiscoverySource.OPENVERSE), UnifiedSearchScope.OPENVERSE.providers)
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

    @Test fun banglaExactMatchRisesWithoutCollapsingVersionsOrInventingIdentity() {
        fun item(id: String, title: String) = DiscoveryItem(DiscoveryItemId(DiscoverySource.AUDIUS, id),
            title, "Creator", "https://audius.co/artist/$id".toHttpUrl(), null, DiscoveryCapabilities())
        val version = item("live", "আমার গান live")
        val first = item("studio1", "আমার গান")
        val second = item("studio2", "আমার গান")
        val input = listOf(version, first, second)
        assertEquals(listOf(first, second, version), rankUnifiedSourceResults("আমার গান", input))
        assertEquals(input, rankUnifiedSourceResults("different", input))
    }
}
