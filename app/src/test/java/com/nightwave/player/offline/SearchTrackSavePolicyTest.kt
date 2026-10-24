package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTrackSavePolicyTest {
    @Test fun canonicalTrackRequiresRealVideoIdentity() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            SearchTrackSavePolicy.canonicalTrack("https://youtu.be/dQw4w9WgXcQ"))
        assertNull(SearchTrackSavePolicy.canonicalTrack("https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ"))
    }

    @Test fun signedAudioEndpointNeverAllowsPrivateOrUntrustedHosts() {
        assertTrue(SearchTrackSavePolicy.safeAudioEndpoint(
            "https://r2---sn-example.googlevideo.com/videoplayback?expire=12345"))
        assertFalse(SearchTrackSavePolicy.safeAudioEndpoint(
            "http://r2---sn-example.googlevideo.com/videoplayback"))
        assertFalse(SearchTrackSavePolicy.safeAudioEndpoint(
            "https://googlevideo.com.attacker.test/videoplayback"))
        assertFalse(SearchTrackSavePolicy.safeAudioEndpoint(
            "https://localhost/videoplayback"))
        assertFalse(SearchTrackSavePolicy.safeAudioEndpoint(
            "https://user:pass@r2---sn-example.googlevideo.com/videoplayback"))
        assertFalse(SearchTrackSavePolicy.safeAudioEndpoint(
            "https://r2---sn-example.googlevideo.com:8443/videoplayback"))
        assertFalse(SearchTrackSavePolicy.safeAudioEndpoint(
            "https://r2---sn-example.googlevideo.com/videoplayback#fragment"))
    }

    @Test fun namesRemainUnicodeButNeverContainPathsOrControls() {
        assertEquals("বাংলা গান 2026", SearchTrackSavePolicy.safeTitle("বাংলা গান / 2026"))
        assertEquals("Saved audio", SearchTrackSavePolicy.safeTitle("../"))
        assertTrue(SearchTrackSavePolicy.safeTitle("a".repeat(250)).length <= 105)
    }

    @Test fun aBookmarkMustNeverBeLabelledReadyOffline() {
        assertEquals("NOT_AVAILABLE_OFFLINE", SearchTrackSavePolicy.resultStatus(null, true))
        assertEquals("NOT_AVAILABLE_OFFLINE", SearchTrackSavePolicy.resultStatus("id", false))
        assertEquals("READY_OFFLINE", SearchTrackSavePolicy.resultStatus("id", true))
    }
}
