package com.nightwave.player.offline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DownloadHistoryVisibilityInstrumentedTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test fun hiddenDownloadCardCanBeRestoredWithoutTouchingMediaFiles() {
        val fake = UUID.randomUUID()
        try {
            assertFalse(fake.toString() in DownloadHistoryVisibility.hiddenIds(app))
            assertTrue(DownloadHistoryVisibility.hide(app, fake))
            assertTrue(fake.toString() in DownloadHistoryVisibility.hiddenIds(app))
            assertTrue(DownloadHistoryVisibility.restore(app, fake))
            assertFalse(fake.toString() in DownloadHistoryVisibility.hiddenIds(app))
        } finally {
            DownloadHistoryVisibility.restore(app, fake)
        }
    }
}
