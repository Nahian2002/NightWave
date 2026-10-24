package com.nightwave.player.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NightWaveCrashPrivacyPolicyTest {
    @Test fun customThreadsNeverRevealEmailsOrURLs() {
        assertEquals("background", NightWaveCrashPrivacyPolicy.threadLabel("user@host.com"))
        assertEquals("background", NightWaveCrashPrivacyPolicy.threadLabel("http://test/path"))
        assertEquals("main", NightWaveCrashPrivacyPolicy.threadLabel("main"))
        assertEquals("render", NightWaveCrashPrivacyPolicy.threadLabel("RenderThread"))
    }
    @Test fun stackFrameTextIsBoundedToCodeSymbols() {
        val result = NightWaveCrashPrivacyPolicy.safeSourceSymbol("Audio/path@email", 130)
        assertEquals("Audio_path_email", result)
        assertFalse(result.contains("@"))
        assertEquals(0, NightWaveCrashPrivacyPolicy.safeFrameLine(-1))
        assertEquals(8, NightWaveCrashPrivacyPolicy.safeFrameLine(8))
    }
}
