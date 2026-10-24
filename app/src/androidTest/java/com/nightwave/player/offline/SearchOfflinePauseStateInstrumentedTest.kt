package com.nightwave.player.offline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Disposable UUID entry only; never modifies a real song or download. */
@RunWith(AndroidJUnit4::class)
class SearchOfflinePauseStateInstrumentedTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test fun pausedRequestSurvivesObjectRecreationAndCanBeCleared() {
        val id = UUID.randomUUID()
        try {
            assertFalse(SearchOfflinePauseState.isMarked(app, id))
            assertTrue(SearchOfflinePauseState.mark(app, id))
            assertTrue(SearchOfflinePauseState.isMarked(
                ApplicationProvider.getApplicationContext(), id))
        } finally {
            assertTrue(SearchOfflinePauseState.clear(app, id))
        }
        assertFalse(SearchOfflinePauseState.isMarked(app, id))
    }
}
