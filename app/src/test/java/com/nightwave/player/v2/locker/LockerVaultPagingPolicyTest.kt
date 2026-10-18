package com.nightwave.player.v2.locker

import org.junit.Assert.*
import org.junit.Test

class LockerVaultPagingPolicyTest {
    private val sha = "a".repeat(64)
    private val md5 = "b".repeat(32)
    private val folder = "NightWaveFolder123"
    private val remote = LockerRemoteTrack(
        "VaultFile123", "Owned.mp3", "audio/mpeg", 1024L,
        md5, sha
    )

    @Test fun firstOpaqueDriveCursorAccepted() {
        val seen = mutableSetOf<String>()
        assertEquals("opaque-token", LockerVaultPagingPolicy.acceptNextToken(
            "opaque-token", seen))
        assertEquals(setOf("opaque-token"), seen)
    }

    @Test fun repeatedCursorFailsClosedInsteadOfLooping() {
        val seen = mutableSetOf("opaque-token")
        assertThrows(IllegalArgumentException::class.java) {
            LockerVaultPagingPolicy.acceptNextToken("opaque-token", seen)
        }
    }

    @Test fun absentAndOverlongCursorRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerVaultPagingPolicy.acceptNextToken("", mutableSetOf())
        }
        assertThrows(IllegalArgumentException::class.java) {
            LockerVaultPagingPolicy.acceptNextToken("A".repeat(2049), mutableSetOf())
        }
    }

    @Test fun checksumQueryIsScopedToFolderAndPrivateShaKey() {
        val query = LockerVaultPagingPolicy.uploadChecksumQuery(folder, sha)
        assertTrue(query.contains("'$folder' in parents"))
        assertTrue(query.contains("trashed = false"))
        assertTrue(query.contains("key='nightwave_sha256'"))
        assertTrue(query.contains("value='$sha'"))
    }

    @Test fun unexpectedQueryValuesRejectedBeforeNetwork() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerVaultPagingPolicy.uploadChecksumQuery("bad' or trashed=true", sha)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LockerVaultPagingPolicy.uploadChecksumQuery(folder, sha + "'")
        }
    }

    @Test fun identicalChecksumAndMetadataMatchesVerifiedCloudCopy() {
        assertTrue(LockerVaultPagingPolicy.hasUploadMatch(
            remote, sha, 1024L, "audio/mpeg", md5))
        assertTrue(LockerVaultPagingPolicy.hasUploadMatch(
            remote.copy(md5 = md5.uppercase()), sha,
            1024L, "audio/mpeg", md5))
    }

    @Test fun nonidenticalContentCannotBeTreatedAsUploaded() {
        assertFalse(LockerVaultPagingPolicy.hasUploadMatch(
            remote, sha, 4096L, "audio/mpeg", md5))
        assertFalse(LockerVaultPagingPolicy.hasUploadMatch(
            remote, sha, 1024L, "audio/flac", md5))
        assertFalse(LockerVaultPagingPolicy.hasUploadMatch(
            remote, sha, 1024L, "audio/mpeg", "c".repeat(32)))
    }

    @Test fun mismatchedTargetedChecksumFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerVaultPagingPolicy.hasUploadMatch(
                remote.copy(sha256 = "c".repeat(64)), sha, 1024L, "audio/mpeg", md5)
        }
    }

    @Test fun incompleteInventoryCanContinueOnlyWithSafeInMemoryCursor() {
        val pending = LockerCloudInventory(
            tracks = listOf(remote), hasMore = true, pagesRead = 5,
            nextPageToken = "opaque-token", folderId = folder,
            seenTokens = setOf("opaque-token"))
        assertTrue(LockerVaultBrowsePolicy.canLoadMore(pending))
        assertFalse(LockerVaultBrowsePolicy.canLoadMore(
            pending.copy(nextPageToken = null)))
        assertFalse(LockerVaultBrowsePolicy.canLoadMore(
            pending.copy(pagesRead = LockerVaultBrowsePolicy.MAX_TOTAL_PAGES)))
        assertFalse(LockerVaultBrowsePolicy.canLoadMore(
            pending.copy(folderId = "invalid'folder")))
        assertFalse(LockerVaultBrowsePolicy.canLoadMore(
            pending.copy(hasMore = false)))
    }
}
