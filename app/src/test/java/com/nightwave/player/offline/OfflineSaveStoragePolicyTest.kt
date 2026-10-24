package com.nightwave.player.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSaveStoragePolicyTest {
    private val mb = 1024L * 1024L

    @Test fun refusesAlmostFullDevice() {
        assertFalse(OfflineSaveStoragePolicy.canStart(23L * mb, null))
        assertTrue(OfflineSaveStoragePolicy.canStart(25L * mb, null))
    }

    @Test fun needsSpaceForStagingAndVerifiedPrivateCopy() {
        assertFalse(OfflineSaveStoragePolicy.canStart(35L * mb, 10L * mb))
        assertTrue(OfflineSaveStoragePolicy.canStart(36L * mb, 10L * mb))
        assertFalse(OfflineSaveStoragePolicy.canStart(120L * mb, 100L * mb))
        assertTrue(OfflineSaveStoragePolicy.canStart(216L * mb, 100L * mb))
    }

    @Test fun unknownSizeTransfersMustPreserveSpaceForTheVerifiedCopy() {
        assertFalse(OfflineSaveStoragePolicy.canContinue(30L * mb, 10L * mb))
        assertTrue(OfflineSaveStoragePolicy.canContinue(34L * mb, 10L * mb))
        assertFalse(OfflineSaveStoragePolicy.canContinue(0L, 1L * mb))
        assertFalse(OfflineSaveStoragePolicy.canContinue(-1L, 1L * mb))
        assertFalse(OfflineSaveStoragePolicy.canContinue(1000L * mb, 0L))
        assertFalse(OfflineSaveStoragePolicy.canContinue(1000L * mb, 251L * mb))
        assertTrue(OfflineSaveStoragePolicy.canContinue(274L * mb, 250L * mb))
    }

    @Test fun rejectsInvalidDeclaredSizesWithoutOverflow() {
        assertFalse(OfflineSaveStoragePolicy.canStart(1000L * mb, 0L))
        assertFalse(OfflineSaveStoragePolicy.canStart(1000L * mb, -1L))
        assertFalse(OfflineSaveStoragePolicy.canStart(1000L * mb, 251L * mb))
        assertFalse(OfflineSaveStoragePolicy.canStart(-1L, null))
    }
}
