package com.nightwave.player.offline

import androidx.work.Data
import com.nightwave.player.net.MusicIdentity
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class EverydayOfflineClarityTest {
    @Test fun recordedSubsecondTransferUsesBitsAndWallTime() {
        assertEquals("Transfer 0.51 s · 42.68 Mbps avg",
            CompletedAudioTransfer(2_704_719, 507).label())
        assertEquals("Transfer 8.00 s · 1.00 Mbps avg",
            CompletedAudioTransfer(1_000_000, 8000).label())
    }
    @Test fun corruptAndAbsentReceiptsCannotManufactureSpeed() {
        for (bytes in listOf(-1L, 0L, Long.MAX_VALUE))
            assertNull(CompletedAudioTransfer(bytes, 1000).label())
        for (elapsed in listOf(-1L, 0L, Long.MAX_VALUE))
            assertNull(CompletedAudioTransfer(1000, elapsed).label())
        assertNull(AudioTransferData.completion(Data.EMPTY))
        val builder = Data.Builder()
        AudioTransferData.putCompletion(builder, CompletedAudioTransfer(1, 0))
        assertNull(AudioTransferData.completion(builder.build()))
    }
    @Test fun completedReceiptSurvivesWorkManagerSerialization() {
        val expected = CompletedAudioTransfer(125_000, 1000)
        val builder = Data.Builder()
        AudioTransferData.putCompletion(builder, expected)
        assertEquals(expected, AudioTransferData.completion(builder.build()))
    }
    @Test fun retainedBytesAndLaterImportTimeNeverInflateCompletion() {
        var now = 1_000_000_000L
        val metrics = AudioTransferMetrics { now }
        metrics.resumedBytes = 2_000_000
        metrics.addReceived(125_000)
        assertNull(metrics.completedTransfer())
        now += 1_000_000_000L
        metrics.markTransferComplete()
        now += 90_000_000_000L // Import/verification or a later dialog.
        metrics.markTransferComplete()
        assertEquals(CompletedAudioTransfer(125_000, 1000), metrics.completedTransfer())
        assertTrue(metrics.completedTransfer()!!.label()!!.endsWith("1.00 Mbps avg"))
    }
    @Test fun formattingRemainsUnambiguousAcrossDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("Transfer 1.00 s · 1.00 Mbps avg",
                CompletedAudioTransfer(125_000, 1000).label())
        } finally { Locale.setDefault(original) }
    }
    @Test fun filteredEmptyRecoveryOnlyAppearsWhenSongsCanBeRevealed() {
        for (filter in OfflineLibraryViewPolicy.Filter.entries) {
            assertFalse(OfflineLibraryViewPolicy.canShowAll(filter, false, true))
            assertTrue(OfflineLibraryViewPolicy.canShowAll(filter, true, true))
        }
        assertFalse(OfflineLibraryViewPolicy.canShowAll(OfflineLibraryViewPolicy.Filter.ALL, true, false))
        assertTrue(OfflineLibraryViewPolicy.canShowAll(OfflineLibraryViewPolicy.Filter.ATTENTION, true, false))
        assertTrue(OfflineLibraryViewPolicy.canShowAll(OfflineLibraryViewPolicy.Filter.READY, true, false))
    }
    @Test fun displayKeepsEditionAndUncorroboratedArtistWhileCleaningBadges() {
        assertEquals("Blue (Live)", MusicIdentity.display("Yung Kai - Blue (Live) [Official Audio]", "Yung Kai").title)
        assertEquals("Other Artist - Blue (Remix)",
            MusicIdentity.display("Other Artist - Blue (Remix) [Official Audio]", "Unknown creator").title)
        assertEquals("বাংলা গান", MusicIdentity.display("বাংলা গান [Official Audio]", "শিল্পী").title)
    }
}
