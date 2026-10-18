package com.nightwave.player.v2.locker

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LockerTransferHistoryPolicyTest {
    private fun entry(id: String = UUID.randomUUID().toString(),
        state: LockerTransferState = LockerTransferState.STARTED) =
        LockerTransferRecord(id, LockerTransferKind.DOWNLOAD,
            state, "Favorite track", 4096L, 1_700_000_000_000L)

    @Test fun restartedInProgressTransferIsNotDeclaredFailedOrSuccessful() {
        assertTrue(LockerTransferHistoryPolicy.description(
            LockerTransferState.STARTED).contains("Unconfirmed"))
        assertFalse(LockerTransferHistoryPolicy.description(
            LockerTransferState.STARTED).contains("Verified complete"))
    }

    @Test fun verifiedCompletionMayChangeOnlyExistingAttempt() {
        val first = entry()
        val second = entry()
        val changed = LockerTransferHistoryPolicy.finish(
            listOf(first, second), first.id, LockerTransferState.COMPLETED)
        assertEquals(LockerTransferState.COMPLETED, changed[0].state)
        assertEquals(LockerTransferState.STARTED, changed[1].state)
    }

    @Test fun missingJournalEntryNeverFabricatesSuccessfulTransfer() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerTransferHistoryPolicy.finish(listOf(entry()),
                UUID.randomUUID().toString(), LockerTransferState.COMPLETED)
        }
    }

    @Test fun cannotFinishAnAttemptAsStarted() {
        val row = entry()
        assertThrows(IllegalArgumentException::class.java) {
            LockerTransferHistoryPolicy.finish(listOf(row), row.id,
                LockerTransferState.STARTED)
        }
    }

    @Test fun previousTerminalStateIsNotSilentlyOverwritten() {
        val row = entry(state = LockerTransferState.COMPLETED)
        val changed = LockerTransferHistoryPolicy.finish(
            listOf(row), row.id, LockerTransferState.NOT_VERIFIED)
        assertEquals(LockerTransferState.COMPLETED, changed.single().state)
    }

    @Test fun historyIsCappedAndDuplicateIdsAreDeduplicated() {
        val many = (0 until 35).map { entry() }
        val capped = LockerTransferHistoryPolicy.capped(
            listOf(many[0]) + many)
        assertEquals(30, capped.size)
        assertEquals(many[0].id, capped[0].id)
        assertEquals(30, capped.map { it.id }.distinct().size)
    }

    @Test fun titlesAreBoundedAndDoNotRetainBasicUrls() {
        val title = LockerTransferHistoryPolicy.displayTitle(
            "a".repeat(90) + "/outside.mp3")
        assertTrue(title.length <= LockerTransferHistoryPolicy.MAX_TITLE)
        assertFalse(title.contains("/"))
        assertFalse(LockerTransferHistoryPolicy.displayTitle(
            "https://secret.example/signed").contains("https://"))
    }

    @Test fun malformedOrOversizedRecordsRejected() {
        assertFalse(LockerTransferHistoryPolicy.valid(
            entry().copy(id = "../../file")))
        assertFalse(LockerTransferHistoryPolicy.valid(entry().copy(bytes = 0L)))
        assertFalse(LockerTransferHistoryPolicy.valid(entry().copy(timeMs = 0L)))
        assertFalse(LockerTransferHistoryPolicy.valid(
            entry().copy(bytes = LockerAudioPolicy.MAX_FILE_BYTES + 1L)))
        assertTrue(LockerTransferHistoryPolicy.valid(entry()))
    }
}
