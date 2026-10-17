package com.nightwave.player.playback

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-only: uses only transient lookup keys, no saved queue or audio files.
 * Never run as an instruction to clear a phone's NightWave application data.
 */
@RunWith(AndroidJUnit4::class)
class QueueLookupIndicatorInstrumentedTest {
    private lateinit var store: QueueStateStore
    private var prepared = false
    private val song = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val other = "https://www.youtube.com/watch?v=9bZkp7q19f0"

    @Before fun setUp() {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val product = Build.PRODUCT.lowercase()
        assumeTrue(fingerprint.contains("generic") || fingerprint.contains("emulator") ||
            product.contains("sdk") || product.contains("emulator"))
        store = QueueStateStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.clearLookup()
        prepared = true
    }

    @After fun tearDown() {
        if (prepared) store.clearLookup()
    }

    @Test fun pendingLookupBelongsOnlyToSelectedTrack() {
        store.markLookup(song)
        assertTrue(store.lookupPending(song))
        assertFalse(store.lookupPending(other))
    }

    @Test fun canceledLookupImmediatelyRemovesLoadingState() {
        store.markLookup(song)
        store.clearLookup()
        assertFalse(store.lookupPending(song))
    }

    @Test fun signedOrOfflineStreamUriCannotBecomePublicLookup() {
        store.markLookup("offline:fake.mp3")
        assertFalse(store.lookupPending("offline:fake.mp3"))
        store.markLookup("https://example.com/signed-token")
        assertFalse(store.lookupPending("https://example.com/signed-token"))
    }
}
