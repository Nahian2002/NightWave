package com.nightwave.player.v2.cloud

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure cloud snapshot decision checks. No network, Google account, or phone. */
class CloudSnapshotPolicyTest {
    private fun video(id: String) = VideoPreview(
        "Track $id", "Creator", "https://www.youtube.com/watch?v=$id", "", 120L
    )
    private val a = video("dQw4w9WgXcQ")
    private val b = video("9bZkp7q19f0")

    private fun backup(
        displayName: String = "Local",
        favorites: List<VideoPreview> = listOf(a),
        history: List<VideoPreview> = emptyList(),
        includeHistory: Boolean = false,
        playlistTracks: List<VideoPreview> = listOf(a)
    ) = CloudBackupCodec.encode(
        LibraryBackupCodec.encode(LibraryArchive(
            displayName, "MIDNIGHT", "orbit",
            favorites, listOf("Saved mix" to playlistTracks)
        )),
        history, includeHistory
    )

    @Test fun samePayloadCanReuseRemoteVerifiedBackup() {
        val payload = backup()
        assertTrue(CloudSnapshotPolicy.sameVerifiedContent(payload, payload.copyOf()))
    }

    @Test fun anotherEquivalentBackupEncodesToTheSameLogicalContent() {
        assertTrue(CloudSnapshotPolicy.sameVerifiedContent(backup(), backup()))
    }

    @Test fun aNewFavoriteRequiresANewSnapshot() {
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(
            backup(favorites = listOf(a,b)), backup()))
    }

    @Test fun changedPlaylistContentRequiresSnapshot() {
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(
            backup(playlistTracks = listOf(a,b)), backup()))
    }

    @Test fun changedProfileRequiresSnapshot() {
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(
            backup(displayName = "Other"), backup()))
    }

    @Test fun listeningHistoryOptInIsPartOfBackupIdentity() {
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(
            backup(history = listOf(b), includeHistory = true),
            backup(includeHistory = false)))
    }

    @Test fun changedOptedInHistoryRequiresNewSnapshot() {
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(
            backup(history = listOf(a), includeHistory = true),
            backup(history = listOf(b), includeHistory = true)))
    }

    @Test fun optedOutHistoryNeverChangesBackupContent() {
        assertTrue(CloudSnapshotPolicy.sameVerifiedContent(
            backup(history = listOf(a), includeHistory = false),
            backup(history = listOf(b), includeHistory = false)))
    }

    @Test fun remoteCorruptionMustNotSilentlyClaimSuccess() {
        val old = backup()
        val corrupt = old.copyOf().also { it[0] = 0 }
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(old, corrupt))
    }

    @Test fun emptyRemoteCannotBeReused() {
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(backup(), byteArrayOf()))
    }

    @Test fun unrelatedRemoteJsonCannotBeReused() {
        assertFalse(CloudSnapshotPolicy.sameVerifiedContent(
            backup(), "{}".toByteArray()))
    }

    @Test fun invalidNewBackupFailsClosedRatherThanUploading() {
        assertThrows(Exception::class.java) {
            CloudSnapshotPolicy.sameVerifiedContent(byteArrayOf(), backup())
        }
    }

    @Test fun bothCorruptBackupsCannotBeEqual() {
        assertThrows(Exception::class.java) {
            CloudSnapshotPolicy.sameVerifiedContent(byteArrayOf(), byteArrayOf())
        }
    }

    @Test fun originalExpectedBackupIsNotRewritten() {
        val expected = backup(history = listOf(b), includeHistory = true)
        val before = expected.copyOf()
        CloudSnapshotPolicy.sameVerifiedContent(expected, backup())
        assertTrue(before.contentEquals(expected))
    }
}
