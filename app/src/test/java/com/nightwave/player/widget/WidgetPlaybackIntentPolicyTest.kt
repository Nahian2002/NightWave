package com.nightwave.player.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetPlaybackIntentPolicyTest {
    private val p = WidgetPlaybackIntentPolicy

    @Test fun bufferingLoadedSongRequiresPause() {
        assertEquals(WidgetPlaybackIntentPolicy.Command.PAUSE, p.command(true, true, true))
        assertTrue(p.showsPauseIcon(true, true))
    }
    @Test fun pausedLoadedSongRequiresPlay() {
        assertEquals(WidgetPlaybackIntentPolicy.Command.PLAY, p.command(true, false, true))
        assertFalse(p.showsPauseIcon(true, false))
    }
    @Test fun coldServiceMustNeverAutoplayFromWidget() {
        assertEquals(WidgetPlaybackIntentPolicy.Command.NONE, p.command(false, false, true))
        assertEquals(WidgetPlaybackIntentPolicy.Command.NONE, p.command(false, true, true))
        assertFalse(p.showsPauseIcon(false, true))
    }
    @Test fun missingControllerCommandCannotControlPlayback() {
        assertEquals(WidgetPlaybackIntentPolicy.Command.NONE, p.command(true, true, false))
        assertEquals(WidgetPlaybackIntentPolicy.Command.NONE, p.command(true, false, false))
    }
    @Test fun WidgetIconAndActualActionAgree() {
        for (loaded in listOf(false, true)) for (intent in listOf(false, true)) {
            assertEquals(p.command(loaded, intent, true) == WidgetPlaybackIntentPolicy.Command.PAUSE,
                p.showsPauseIcon(loaded, intent))
        }
    }
}
