package com.nightwave.player.offline

import org.junit.Assert.*
import org.junit.Test

class NightWaveOfflineNavigationPolicyTest {
    private val p = NightWaveOfflineNavigationPolicy
    private val id = "a1234567-1234-1234-1234-123456789abc"

    @Test fun onlyExactPrivateComponentsCanReuseOfflineRoutes() {
        NightWaveOfflineRoute.entries.forEach { assertEquals(it, p.route(it.componentName)) }
        for (bad in listOf(null, "OfflineLibraryActivity", "other.app.OfflineLibraryActivity",
            "com.nightwave.player.offline.OfflineLibraryActivity/extra", "")) assertNull(p.route(bad))
    }

    @Test fun everyDestinationHasItsOwnStableComponent() {
        assertEquals(4, NightWaveOfflineRoute.entries.map { it.componentName }.toSet().size)
        assertTrue(NightWaveOfflineRoute.entries.all { it.label.isNotBlank() })
    }

    @Test fun searchDraftsAreBoundedWithoutChangingInternalUnicode() {
        assertEquals("আলো  আলো", p.query("  আলো  আলো  "))
        assertEquals(160, p.query("x".repeat(10_000)).length)
        assertEquals("", p.query(null))
    }

    @Test fun corruptSavedPositionsCannotBecomeNegativeOrUnbounded() {
        assertEquals(0, p.page(Int.MIN_VALUE)); assertEquals(100_000, p.page(Int.MAX_VALUE))
        assertEquals(0, p.scroll(-1)); assertEquals(1_000_000, p.scroll(Int.MAX_VALUE))
        assertEquals(75, p.page(75)); assertEquals(4321, p.scroll(4321))
    }

    @Test fun corruptSortRestoresTheSafeExistingDefault() {
        assertEquals(0, p.sort(-1)); assertEquals(0, p.sort(999))
        (0..3).forEach { assertEquals(it, p.sort(it)) }
    }

    @Test fun freshNavigationCarriesDestinationWithoutAnyPlaybackCommand() {
        for (tab in 0..3) {
            val request = p.mainRequest(id, tab, false, null)!!
            assertEquals(tab, request.tab); assertFalse(request.openPlayer)
        }
    }

    @Test fun openPlayerIsAnExplicitViewRequest() {
        assertTrue(p.mainRequest(id, 2, true, null)!!.openPlayer)
        assertFalse(p.mainRequest(id, 2, false, null)!!.openPlayer)
    }

    @Test fun appliedRequestCannotReplayAfterRecreation() {
        assertNull(p.mainRequest(id, 2, true, id))
        assertNotNull(p.mainRequest(id, 2, true, "b1234567-1234-1234-1234-123456789abc"))
    }

    @Test fun malformedOrMissingRequestCannotOverrideRestoredMainState() {
        for (bad in listOf(null, "", "old-request", "x".repeat(10_000), id.uppercase())) {
            assertNull(p.mainRequest(bad, 2, true, null))
        }
    }

    @Test fun invalidMainDestinationIsRejectedRatherThanClampedToAnotherTab() {
        for (tab in listOf(-1, 4, Int.MIN_VALUE, Int.MAX_VALUE)) assertNull(p.mainRequest(id, tab, false, null))
    }

    @Test fun legacyRootsCannotLeaveAnOfflineLoopBelowANewMainShell() {
        assertTrue(p.needsMainRoot("com.nightwave.player.ui.MainActivity"))
        NightWaveOfflineRoute.entries.forEach { assertTrue(p.needsMainRoot(it.componentName)) }
        assertFalse(p.needsMainRoot("com.nightwave.player.v2.ui.NightWaveStudioActivity"))
        assertFalse(p.needsMainRoot(null)) // unknown task must not authorize clearing it
        assertFalse(p.needsMainRoot("other.app.MainActivity"))
    }

    @Test fun editedCatalogQueryDoesNotRewriteItsSourceIdentityOnRecreation() {
        val draft = p.catalogDraft("acoustic version", "Original song", "Original artist", "new intent", "new artist")
        assertEquals("acoustic version", draft.query)
        assertEquals("Original song", draft.originTitle)
        assertEquals("Original artist", draft.originArtist)
    }

    @Test fun deliberatelyClearedCatalogDraftDoesNotReviveAnOldIntentQuery() {
        val draft = p.catalogDraft("", "", "", "Old song", "Old artist")
        assertEquals(NightWaveCatalogDraft("", "", ""), draft)
        val fresh = p.catalogDraft(null, null, null, " নতুন গান ", " Singer ")
        assertEquals(NightWaveCatalogDraft("নতুন গান", "নতুন গান", "Singer"), fresh)
    }
}
