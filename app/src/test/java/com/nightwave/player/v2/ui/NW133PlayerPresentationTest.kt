package com.nightwave.player.v2.ui

import com.nightwave.player.v2.playback.PlaybackHealthPolicy
import org.junit.Assert.*
import org.junit.Test

class NW133PlayerPresentationTest {
    private val id = "00000000001"
    private fun still(file: String) = "https://i.ytimg.com/vi/$id/$file"

    @Test fun largeArtworkTriesSameSourceInBoundedDescendingQualityOrder() {
        val original = still("default.jpg")
        val candidates = NightWaveArtworkPolicy.requestCandidates(original, true)
        assertEquals(listOf(still("maxresdefault.jpg"), still("sddefault.jpg"), still("hqdefault.jpg"), original), candidates)
        assertTrue(candidates.all { it.contains("/vi/$id/") })
        assertEquals(4, candidates.size)
    }
    @Test fun existingHighResolutionSourceDoesNotLoopOnFallback() {
        for (name in listOf("maxresdefault.jpg", "sddefault.jpg", "hqdefault.jpg")) {
            val candidates = NightWaveArtworkPolicy.requestCandidates(still(name), true)
            assertEquals(3, candidates.size)
            assertEquals(candidates.size, candidates.distinct().size)
            assertTrue(still(name) in candidates)
        }
    }
    @Test fun smallArtworkDoesNotProbeLargeImages() {
        assertEquals(listOf(still("hqdefault.jpg"), still("default.jpg")),
            NightWaveArtworkPolicy.requestCandidates(still("default.jpg"), false))
        assertEquals(listOf(still("maxresdefault.jpg")),
            NightWaveArtworkPolicy.requestCandidates(still("maxresdefault.jpg"), false))
    }
    @Test fun preservesSignedAndAuthenticatedSourceAddressesExactly() {
        for (url in listOf(still("default.jpg") + "?token=fixture", still("default.jpg") + "#fixture",
            "https://user:fixture@i.ytimg.com/vi/$id/default.jpg", "https://i.ytimg.com:8443/vi/$id/default.jpg")) {
            assertEquals(listOf(url), NightWaveArtworkPolicy.requestCandidates(url, true))
            assertEquals(listOf(url), NightWaveArtworkPolicy.requestCandidates(url, false))
        }
    }
    @Test fun rejectsSpoofedUnknownEncodedOrNonHttpsThumbnailTransforms() {
        for (url in listOf("https://i.ytimg.com.attacker.invalid/vi/$id/default.jpg",
            "http://i.ytimg.com/vi/$id/default.jpg", "https://i.ytimg.com/vi/invalid/default.jpg",
            "https://i.ytimg.com/vi/$id/%64efault.jpg", "https://i.ytimg.com/vi/$id/unrecognized.jpg",
            "https://i.ytimg.com/vi/$id/default.jpg/", "https://i.ytimg.com/vi_webp/$id/default.webp")) {
            assertEquals(listOf(url), NightWaveArtworkPolicy.requestCandidates(url, true))
        }
    }
    @Test fun thirdPartyAndOwnedArtworkNeverGainAnInventedSource() {
        for (url in listOf("content://media/external/audio/27", "file:///owned/cover.png",
            "https://art.example.org/cover.jpg", "not-a-uri"))
            assertEquals(listOf(url), NightWaveArtworkPolicy.requestCandidates(url, true))
        assertTrue(NightWaveArtworkPolicy.requestCandidates("", true).isEmpty())
    }
    @Test fun decodedLargePlaceholderAdvancesButActualSmallAndUnknownImagesRemainValid() {
        assertTrue(NightWaveArtworkPolicy.isUnavailableLargeStill(still("maxresdefault.jpg"), 120, 90))
        assertTrue(NightWaveArtworkPolicy.isUnavailableLargeStill(still("sddefault.jpg"), 120, 90))
        assertFalse(NightWaveArtworkPolicy.isUnavailableLargeStill(still("maxresdefault.jpg"), 1280, 720))
        assertFalse(NightWaveArtworkPolicy.isUnavailableLargeStill(still("hqdefault.jpg"), 120, 90))
        assertFalse(NightWaveArtworkPolicy.isUnavailableLargeStill("https://art.example.org/maxresdefault.jpg", 120, 90))
        assertFalse(NightWaveArtworkPolicy.isUnavailableLargeStill(still("maxresdefault.jpg"), 0, -1))
    }
    @Test fun decodeRequestUsesActualBoundsAndNeverExceedsSourceCap() {
        assertEquals(96, NightWaveArtworkPolicy.requestPixels(96, 120))
        assertEquals(48, NightWaveArtworkPolicy.requestPixels(8, 12))
        assertEquals(1280, NightWaveArtworkPolicy.requestPixels(8000, 4000))
        assertEquals(0, NightWaveArtworkPolicy.requestPixels(0, 300))
        assertEquals(0, NightWaveArtworkPolicy.requestPixels(300, -1))
    }
    @Test fun changingArtworkKeepsIndependentOriginalIdentityAndFallbackPlans() {
        val first = NightWaveArtworkPolicy.requestCandidates(still("default.jpg"), true)
        val other = still("default.jpg").replace(id, "00000000002")
        val second = NightWaveArtworkPolicy.requestCandidates(other, true)
        assertTrue(first.none { it in second })
        assertEquals(still("default.jpg"), first.last())
        assertEquals(other, second.last())
    }
    private fun status(playing: Boolean = false, buffering: Boolean = false, resolving: Boolean = false,
        connected: Boolean = true, error: String? = null, offline: Boolean = false) =
        NightWaveNowPlayingPresentationPolicy.statusLabel(PlaybackHealthPolicy.classify(true,
            connected, offline, playing, buffering, resolving, error))
    @Test fun playerAndMiniShareTruthfulPlayingPausedResolvingAndBufferingLabels() {
        assertEquals("Playing", status(playing = true))
        assertEquals("Paused", status())
        assertEquals("Finding audio", status(playing = true, resolving = true))
        assertEquals("Buffering audio", status(playing = true, buffering = true))
        assertEquals("Playback unavailable", status(playing = true, error = "Fixture failure"))
        assertEquals("Player disconnected", status(connected = false, playing = true))
    }
    @Test fun failureAndDisconnectTakePriorityOverAnOldPlayingFlag() {
        assertEquals("Player disconnected", status(connected = false, error = "Fixture failure", buffering = true))
        assertEquals("Playback unavailable", status(error = "Fixture failure", resolving = true))
    }
    @Test fun seekKeepsExactSelectedIdentityAndClampsKnownDuration() {
        fun target(value: Float) = NightWaveNowPlayingPresentationPolicy.seekPosition("offline:one", "offline:one",
            true, 120_000L, false, null, value)
        assertEquals(30_000L, target(.25f))
        assertEquals(0L, target(-2f))
        assertEquals(120_000L, target(2f))
    }
    @Test fun staleTrackAndReconnectionCannotCommitToNewSelection() {
        assertNull(NightWaveNowPlayingPresentationPolicy.seekPosition("offline:old", "offline:new", true, 120_000L, false, null, .5f))
        assertNull(NightWaveNowPlayingPresentationPolicy.seekPosition(id, id, false, 120_000L, false, null, .5f))
        assertNull(NightWaveNowPlayingPresentationPolicy.seekPosition(id, id, true, 120_000L, true, null, .5f))
    }
    @Test fun errorsUnknownDurationAndInvalidProgressNeverBecomeSeekCommands() {
        for (duration in listOf(0L, -1L)) assertNull(NightWaveNowPlayingPresentationPolicy.seekPosition(id, id, true, duration, false, null, .5f))
        assertNull(NightWaveNowPlayingPresentationPolicy.seekPosition(id, id, true, 1000L, false, "Failure", .5f))
        assertNull(NightWaveNowPlayingPresentationPolicy.seekPosition(id, id, true, 1000L, false, null, Float.NaN))
        assertFalse(NightWaveNowPlayingPresentationPolicy.canSeek("", true, 1000L, false, null))
    }
}
