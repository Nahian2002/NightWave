package com.nightwave.player.v2.cloud

import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.data.LibraryArchive
import com.nightwave.player.v2.data.LibraryBackupCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudRecoveryPolicyTest {
    private fun track(id: String) = VideoPreview(
        "Track $id", "Creator", "https://www.youtube.com/watch?v=$id", "", 100L)
    private val a = track("dQw4w9WgXcQ")
    private val b = track("9bZkp7q19f0")
    private val c = track("aqz-KE-bpKQ")

    private fun archive(
        likes: List<VideoPreview> = listOf(a),
        playlists: List<Pair<String, List<VideoPreview>>> = listOf("My mix" to listOf(a)),
        name: String = "Listener",
        theme: String = "MIDNIGHT"
    ) = LibraryArchive(name, theme, "orbit", likes, playlists)

    private fun local(a: LibraryArchive) = LibraryBackupCodec.encode(a)
    private fun remote(a: LibraryArchive, history: List<VideoPreview> = emptyList(),
                       enabled: Boolean = false): CloudBackupPreview =
        CloudBackupCodec.decode(CloudBackupCodec.encode(
            LibraryBackupCodec.encode(a), history, enabled))

    @Test fun identicalBackupReportsNoReplaceLoss() {
        val data = archive()
        val result = CloudRecoveryPolicy.inspect(local(data), remote(data))
        assertEquals(0, result.localLikedNotInCloud)
        assertEquals(0, result.localPlaylistsNotInCloud)
        assertEquals(0, result.localPlaylistsWithChangedContent)
        assertFalse(result.profileWouldChange)
    }

    @Test fun lostLikedSongsAreCountedBeforeConfirmation() {
        val report = CloudRecoveryPolicy.inspect(
            local(archive(likes = listOf(a,b,c))), remote(archive(likes = listOf(a))))
        assertEquals(2, report.localLikedNotInCloud)
        assertEquals(3, report.localLiked)
        assertEquals(1, report.cloudLiked)
    }

    @Test fun missingLocalPlaylistsCountedWithoutTouchingStorage() {
        val report = CloudRecoveryPolicy.inspect(
            local(archive(playlists = listOf("Mix" to listOf(a), "Hidden" to listOf(b)))),
            remote(archive(playlists = listOf("Mix" to listOf(a)))))
        assertEquals(1, report.localPlaylistsNotInCloud)
        assertEquals(2, report.localPlaylistCount)
        assertEquals(1, report.cloudPlaylistCount)
    }

    @Test fun replacedSongOrderCountsAsChangedPlaylist() {
        val report = CloudRecoveryPolicy.inspect(
            local(archive(playlists = listOf("Mix" to listOf(a,b)))),
            remote(archive(playlists = listOf("Mix" to listOf(b,a)))))
        assertEquals(1, report.localPlaylistsWithChangedContent)
    }

    @Test fun changedPlaylistSongCountCountsAsChanged() {
        val report = CloudRecoveryPolicy.inspect(
            local(archive(playlists = listOf("Mix" to listOf(a)))),
            remote(archive(playlists = listOf("Mix" to listOf(a,b,c)))))
        assertEquals(1, report.localPlaylistsWithChangedContent)
        assertEquals(3, report.cloudPlaylistTracks)
    }

    @Test fun samePlaylistCaseChangedWarnsUser() {
        val report = CloudRecoveryPolicy.inspect(
            local(archive(playlists = listOf("Mix" to listOf(a)))),
            remote(archive(playlists = listOf("mix" to listOf(a)))))
        assertEquals(1, report.localPlaylistsWithChangedContent)
    }

    @Test fun replacementProfileIsReported() {
        val report = CloudRecoveryPolicy.inspect(
            local(archive(name = "On phone")), remote(archive(name = "In Drive")))
        assertTrue(report.profileWouldChange)
    }

    @Test fun sameProfileNoWarning() {
        val report = CloudRecoveryPolicy.inspect(
            local(archive()), remote(archive()))
        assertFalse(report.profileWouldChange)
    }

    @Test fun optedInHistoryShownSeparately() {
        val report = CloudRecoveryPolicy.inspect(local(archive()),
            remote(archive(), listOf(b), true))
        assertEquals(1, report.optionalHistoryCount)
    }

    @Test fun optedOutHistoryIsAbsent() {
        val report = CloudRecoveryPolicy.inspect(local(archive()), remote(archive()))
        assertEquals(0, report.optionalHistoryCount)
    }

    @Test fun expectedLocalBytesAreDefensivelyCopied() {
        val bytes = local(archive())
        val report = CloudRecoveryPolicy.inspect(bytes, remote(archive()))
        assertTrue(bytes.contentEquals(report.expectedLocalBackup))
        assertFalse(bytes === report.expectedLocalBackup)
    }

    @Test fun emptyLocalArchiveCannotBeCalledSafe() {
        assertThrows(Exception::class.java) {
            CloudRecoveryPolicy.inspect(byteArrayOf(), remote(archive()))
        }
    }

    @Test fun invalidRemotePlaylistFailsBeforeAnyRestore() {
        val invalid = archive(playlists = listOf("Bad" to listOf(
            a.copy(url = "https://example.invalid/not-a-song"))))
        assertThrows(Exception::class.java) {
            CloudRecoveryPolicy.inspect(local(archive()),
                CloudBackupPreview(invalid, emptyList(), false))
        }
    }

    @Test fun remoteWithHistoryButNoPermissionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudRecoveryPolicy.inspect(local(archive()),
                CloudBackupPreview(archive(), listOf(a), false))
        }
    }

    @Test fun reportDoesNotModifyCallerLists() {
        val original = archive(playlists = listOf("A" to listOf(a), "B" to listOf(b)))
        val after = CloudRecoveryPolicy.inspect(local(original), remote(archive()))
        assertEquals(listOf("A", "B"), original.playlists.map { it.first })
        assertEquals(1, after.localPlaylistsNotInCloud)
    }
}
