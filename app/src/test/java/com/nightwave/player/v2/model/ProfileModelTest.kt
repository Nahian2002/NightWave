package com.nightwave.player.v2.model

import org.junit.Assert.*
import org.junit.Test

class ProfileModelTest {
    @Test fun invalidGuestProfileIsNormalized() {
        val guest = ProfileSettings(
            localId = "local-123",
            displayName = "  " + "a".repeat(55) + "   ",
            avatarStyle = "unknown",
            crossfadeSeconds = 200
        ).normalized()
        assertEquals(40, guest.displayName.length)
        assertEquals("orbit", guest.avatarStyle)
        assertEquals(12, guest.crossfadeSeconds)
        assertEquals("local-123", guest.localId)
    }

    @Test fun blankNameGetsSafeDefault() {
        assertEquals("NightWave Listener", ProfileSettings(localId = "x", displayName = "   ").normalized().displayName)
    }

    @Test fun noImplicitGuestCloudOverwrite() {
        assertTrue(BackupPlan(1, 2, 3, true).requiresUserChoice())
        assertFalse(BackupPlan(1, 0, null, false).requiresUserChoice())
        assertFalse(BackupPlan(1, 2, 3, true, RestoreChoice.KEEP_LOCAL).requiresUserChoice())
    }

    @Test fun backupWhitelistRejectsSecrets() {
        listOf("access_token", "googleIdToken", "password", "session_cookie", "stream_url")
            .forEach { assertFalse(it, ProfilePolicy.isBackupFieldAllowed(it)) }
        listOf("display_name", "theme", "playlist_order", "avatar_style")
            .forEach { assertTrue(it, ProfilePolicy.isBackupFieldAllowed(it)) }
    }

    @Test fun palettesHaveStableContrastAndSpacing() {
        for (theme in NightWaveTheme.entries) {
            val palette = NightWaveDesign.palette(theme)
            assertNotEquals(palette.background, palette.foreground)
            assertNotEquals(palette.accent, palette.surface)
        }
        assertTrue(NightWaveDesign.spacing.toList().zipWithNext().all { it.first < it.second })
        assertTrue(NightWaveDesign.TOUCH_TARGET_DP >= 48)
    }
}
