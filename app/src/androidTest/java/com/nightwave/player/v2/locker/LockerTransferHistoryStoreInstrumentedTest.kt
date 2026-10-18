package com.nightwave.player.v2.locker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Each test uses a unique cache-file journal. Never modifies real user history,
 * owned songs, download checkpoints, Google Drive or account settings.
 */
@RunWith(AndroidJUnit4::class)
class LockerTransferHistoryStoreInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun withIsolatedHistory(test: (LockerTransferHistoryStore, File) -> Unit) {
        val file = File(context.cacheDir,
            "nightwave_history_test_" + UUID.randomUUID().toString() + ".json")
        try {
            test(LockerTransferHistoryStore(context, file), file)
        } finally {
            file.delete()
            File(file.path + ".bak").delete()
            File(file.path + ".new").delete()
        }
    }

    @Test fun finishedTransferSurvivesStoreRecreation() = withIsolatedHistory { history, file ->
        val id = history.start(LockerTransferKind.DOWNLOAD, "Owned Song.mp3", 1024L)
        assertEquals(LockerTransferState.STARTED, history.read().entries.single().state)
        history.finish(id, LockerTransferState.COMPLETED)
        val reopened = LockerTransferHistoryStore(context, file).read()
        assertFalse(reopened.unreadable)
        assertEquals(LockerTransferState.COMPLETED, reopened.entries.single().state)
        assertEquals("Owned Song.mp3", reopened.entries.single().title)
    }

    @Test fun unconfirmedTransferNeverBecomesImplicitSuccess() =
        withIsolatedHistory { history, file ->
            history.start(LockerTransferKind.UPLOAD, "Music.mp3", 512L)
            val reopened = LockerTransferHistoryStore(context, file).read()
            assertEquals(LockerTransferState.STARTED, reopened.entries.single().state)
        }

    @Test fun journalNeverPersistsAccountTokenOrPrivatePath() =
        withIsolatedHistory { history, file ->
            history.start(LockerTransferKind.UPLOAD, "Local music", 512L)
            val json = file.readText(Charsets.UTF_8)
            assertFalse(json.contains("owner@example.test"))
            assertFalse(json.contains("Bearer "))
            assertFalse(json.contains("access_token"))
            assertFalse(json.contains("locker_staging"))
            assertFalse(json.contains("DriveFile123"))
        }

    @Test fun corruptJournalRemainsUnmodifiedUntilExplicitClear() =
        withIsolatedHistory { history, file ->
            file.writeText("{invalid source bytes", Charsets.UTF_8)
            assertTrue(history.read().unreadable)
            assertThrows(IllegalArgumentException::class.java) {
                history.start(LockerTransferKind.DOWNLOAD, "New song", 400L)
            }
            assertEquals("{invalid source bytes", file.readText(Charsets.UTF_8))
        }

    @Test fun clearingHistoryDoesNotTouchIndependentDownloadFixture() =
        withIsolatedHistory { history, _ ->
            val marker = File(context.cacheDir,
                "nightwave_checkpoint_fixture_" + UUID.randomUUID().toString())
            try {
                assertTrue(marker.createNewFile())
                marker.writeText("saved checkpoint bytes", Charsets.UTF_8)
                history.start(LockerTransferKind.DOWNLOAD, "Music.mp3", 1000L)
                history.clear()
                assertTrue(history.read().entries.isEmpty())
                assertEquals("saved checkpoint bytes", marker.readText(Charsets.UTF_8))
            } finally {
                marker.delete()
            }
        }
}
