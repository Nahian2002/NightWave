package com.nightwave.player.v2.insights

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Destructive to THIS app's listening-memory test prefs. Emulator only.
 * The assumption runs before any preference access; never run on a real phone.
 */
@RunWith(AndroidJUnit4::class)
class ListeningMemoryTrendInstrumentedTest {
    private lateinit var memory: ListeningMemoryStore
    private var prepared = false
    private val song = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    @Before fun setUp() {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val product = Build.PRODUCT.lowercase()
        assumeTrue(fingerprint.contains("generic") || fingerprint.contains("emulator") ||
            product.contains("sdk") || product.contains("emulator"))
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        memory = ListeningMemoryStore(ctx)
        assertTrue(memory.eraseAndDisable())
        prepared = true
    }

    @After fun tearDown() {
        if (prepared) memory.eraseAndDisable()
    }

    private fun pulse(day: Long? = null) = ListeningPulse(
        trackId = song, title = "A real recorded track", artist = "Artist",
        listenedMs = 10_000L, qualifyingPlay = false,
        recordedAtMs = day ?: System.currentTimeMillis()
    )

    @Test fun disabledRecorderDoesNotStoreDailyHistory() {
        val revision = memory.consentRevision()
        assertFalse(memory.record(pulse(), revision))
        val summary = memory.snapshot()
        assertFalse(summary.enabled)
        assertEquals(0, summary.daily.size)
        assertTrue(summary.recommendationTracks.isEmpty())
    }

    @Test fun acceptedPulseIsAtomicAcrossTrackAndDayTotals() {
        assertTrue(memory.setEnabled(true))
        val revision = memory.consentRevision()
        assertTrue(memory.record(pulse(), revision))
        val summary = memory.snapshot()
        assertEquals(10_000L, summary.listenedMs)
        assertEquals(10_000L, summary.daily.sumOf { it.observedMs })
        assertEquals(1, summary.recommendationTracks.size)
    }

    @Test fun eraseAndRapidReenableCannotResurrectQueuedOldPulse() {
        assertTrue(memory.setEnabled(true))
        val before = memory.consentRevision()
        assertTrue(memory.record(pulse(), before))
        assertTrue(memory.eraseAndDisable())
        assertEquals(0, memory.snapshot().daily.size)
        assertFalse(memory.record(pulse(), before))
        assertTrue(memory.setEnabled(true))
        assertFalse(memory.record(pulse(), before))
        assertEquals(0, memory.snapshot().listenedMs)
        assertTrue(memory.record(pulse(), memory.consentRevision()))
        assertEquals(10_000L, memory.snapshot().listenedMs)
    }

    @Test fun oldDaysCannotReenterRollingRetention() {
        assertTrue(memory.setEnabled(true))
        val revision = memory.consentRevision()
        val today = ListeningTrendEngine.utcEpochDay(System.currentTimeMillis())
        assertFalse(memory.record(
            pulse((today - 31L) * ListeningTrendEngine.MS_PER_DAY), revision))
        assertTrue(memory.record(pulse(), revision))
        assertEquals(1, memory.snapshot().daily.size)
    }

    @Test fun disabledConsentRetainsPastHistoryButHidesRankingSignals() {
        assertTrue(memory.setEnabled(true))
        val revision = memory.consentRevision()
        assertTrue(memory.record(pulse(), revision))
        assertTrue(memory.setEnabled(false))
        val summary = memory.snapshot()
        assertFalse(summary.enabled)
        assertEquals(1, summary.daily.size)
        assertTrue(summary.recommendationTracks.isEmpty())
    }
}
