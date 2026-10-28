package com.nightwave.player.v2.ui

import org.junit.Assert.*
import org.junit.Test

class SearchDraftPolicyTest {
    @Test fun providerResponseWithoutExplicitIntentCannotReplaceDraft() {
        assertFalse(SearchDraftPolicy.shouldApply("", "request-1"))
    }
    @Test fun initialEmptyStateDoesNotPrefill() {
        assertFalse(SearchDraftPolicy.shouldApply("", ""))
    }
    @Test fun consumedShortcutCannotResurrectClearedTextAfterPageRecreation() {
        assertFalse(SearchDraftPolicy.shouldApply("request-1", "request-1"))
    }
    @Test fun freshExplicitShortcutCanPrefillTheSameSearchAgain() {
        assertTrue(SearchDraftPolicy.shouldApply("request-2", "request-1"))
    }
    @Test fun unconsumedSharedTextIsApplied() {
        assertTrue(SearchDraftPolicy.shouldApply("shared-1", ""))
    }
}
