package com.nightwave.player.v2.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAccountPresentationTest {
    @Test fun unconnectedLibraryIsAlwaysLocal() {
        assertEquals(CloudAccountPresentation.BackupStage.LOCAL_ONLY,
            CloudAccountPresentation.stage(false, true, 12345L))
    }

    @Test fun authorizingDriveDoesNotPretendABackupExists() {
        assertEquals(CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP,
            CloudAccountPresentation.stage(true, false, 0L))
        assertEquals(CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP,
            CloudAccountPresentation.stage(true, true, 0L))
        assertEquals(CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP,
            CloudAccountPresentation.stage(true, false, 12345L))
    }

    @Test fun onlyVerifiedSuccessCountsAsBackup() {
        assertEquals(CloudAccountPresentation.BackupStage.VERIFIED_BACKUP,
            CloudAccountPresentation.stage(true, true, 12345L))
        assertEquals("Drive backup verified", CloudAccountPresentation.title(
            CloudAccountPresentation.BackupStage.VERIFIED_BACKUP))
    }

    @Test fun masksOnlyDisplayAndKeepsDomain() {
        assertEquals("j•••@gmail.com",
            CloudAccountPresentation.maskedAccount("jayed@gmail.com"))
        assertEquals("Google account",
            CloudAccountPresentation.maskedAccount("invalid"))
        assertEquals("a•••@example.com",
            CloudAccountPresentation.maskedAccount("a@example.com"))
    }

    @Test fun backupActionRequiresConnectionAndNoCurrentOperation() {
        assertFalse(CloudAccountPresentation.canBackUp(CloudBackupStatus(), false))
        assertFalse(CloudAccountPresentation.canBackUp(
            CloudBackupStatus(account = "a@example.com"), true))
        assertTrue(CloudAccountPresentation.canBackUp(
            CloudBackupStatus(account = "a@example.com"), false))
    }
}
