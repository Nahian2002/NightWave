package com.nightwave.player.v2.locker

import org.junit.Assert.*
import org.junit.Test

class LockerVaultBrowsePolicyTest {
    private val sha = "a".repeat(64)
    private val md5 = "b".repeat(32)

    private fun song(id: String, name: String, size: Long,
        verified: Boolean = true) = LockerRemoteTrack(
        id, name, "audio/mpeg", size,
        md5 = if (verified) md5 else "",
        sha256 = if (verified) sha else ""
    )

    private val songs = listOf(
        song("VaultFile001", "Zenith.mp3", 4096L),
        song("VaultFile002", "alpha.mp3", 1024L),
        song("VaultFile003", "Beta.mp3", 3072L, verified = false)
    )

    @Test fun caseInsensitiveSearchNeverMutatesCloudInventory() {
        val inventory = LockerCloudInventory(songs)
        val matched = LockerVaultBrowsePolicy.browse(inventory, "  ALPHA  ")
        assertEquals(listOf("alpha.mp3"), matched.map { it.name })
        assertEquals(songs, inventory.tracks)
    }

    @Test fun verifiedFilterRequiresBothHashes() {
        val inventory = LockerCloudInventory(songs)
        assertEquals(2, LockerVaultBrowsePolicy.browse(inventory, "",
            LockerVaultFilter.VERIFIED).size)
        assertEquals(listOf("Beta.mp3"), LockerVaultBrowsePolicy.browse(
            inventory, "", LockerVaultFilter.NEEDS_ATTENTION).map { it.name })
    }

    @Test fun sortingIsStableAndUsesNumericSize() {
        val inventory = LockerCloudInventory(songs)
        assertEquals(listOf("alpha.mp3", "Beta.mp3", "Zenith.mp3"),
            LockerVaultBrowsePolicy.browse(inventory, "").map { it.name })
        assertEquals(listOf(4096L, 3072L, 1024L),
            LockerVaultBrowsePolicy.browse(inventory, "",
                sort = LockerVaultSort.LARGEST).map { it.bytes })
        assertEquals(listOf(1024L, 3072L, 4096L),
            LockerVaultBrowsePolicy.browse(inventory, "",
                sort = LockerVaultSort.SMALLEST).map { it.bytes })
    }

    @Test fun summaryReportsOnlyListedFilesNotFullDriveCapacity() {
        val report = LockerVaultBrowsePolicy.summary(
            LockerCloudInventory(songs, hasMore = true, pagesRead = 5))
        assertEquals(3, report.listedCount)
        assertEquals(2, report.verifiedCount)
        assertEquals(1, report.needsAttentionCount)
        assertEquals(8192L, report.listedBytes)
        assertTrue(report.partialListing)
    }

    @Test fun fullInventoryDoesNotClaimMorePages() {
        val report = LockerVaultBrowsePolicy.summary(
            LockerCloudInventory(songs, hasMore = false, pagesRead = 1))
        assertFalse(report.partialListing)
    }

    @Test fun visiblePageIsBoundedEvenWithHugeRequest() {
        val many = List(4500) { index ->
            song("VaultFile${index.toString().padStart(4, '0')}",
                "song${index}.mp3", 100L)
        }
        assertEquals(40, LockerVaultBrowsePolicy.visible(many, 40).size)
        assertEquals(4000, LockerVaultBrowsePolicy.visible(many, 9000).size)
        assertTrue(LockerVaultBrowsePolicy.visible(many, -1).isEmpty())
    }

    @Test fun byteUnitsAreHonestAndBounded() {
        assertEquals("0 KiB", LockerVaultBrowsePolicy.bytesLabel(0))
        assertEquals("1 KiB", LockerVaultBrowsePolicy.bytesLabel(1))
        assertEquals("1 MiB", LockerVaultBrowsePolicy.bytesLabel(1024L * 1024L))
    }

    @Test fun emptyInventoryHasNoPhantomDownloads() {
        val empty = LockerCloudInventory(emptyList())
        assertTrue(LockerVaultBrowsePolicy.browse(empty, "any").isEmpty())
        assertEquals(0L, LockerVaultBrowsePolicy.summary(empty).listedBytes)
    }
}
