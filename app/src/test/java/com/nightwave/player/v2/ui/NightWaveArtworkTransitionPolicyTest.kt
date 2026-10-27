package com.nightwave.player.v2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW116 transition state and TalkBack semantics; no Compose or media needed. */
class NightWaveArtworkTransitionPolicyTest {
    private val p = NightWaveArtworkTransitionPolicy
    private val a = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val b = "https://www.youtube.com/watch?v=9bZkp7q19f0"

    @Test fun changingTrackWithIdenticalThumbnailIsStillANewFrame() {
        val outgoing = p.frame(a, "https://images.example/album.jpg")
        val incoming = p.frame(b, outgoing.thumbnail)
        assertFalse(outgoing == incoming)
        assertTrue(p.shouldAnimate(outgoing, incoming, reduceMotion = false))
    }

    @Test fun identicalTrackAndThumbnailDoNotRestartCoverAnimation() {
        val current = p.frame(a, "https://images.example/song-a.jpg")
        assertEquals(current, p.frame(a, current.thumbnail))
        assertFalse(p.shouldAnimate(current, current, reduceMotion = false))
    }

    @Test fun updatedCoverForSameSongIsNewVisualState() {
        val previous = p.frame(a, "https://images.example/low-res.jpg")
        val updated = p.frame(a, "https://images.example/high-res.jpg")
        assertTrue(p.shouldAnimate(previous, updated, reduceMotion = false))
        assertEquals(a, updated.trackId)
    }

    @Test fun outgoingCoverIsNeverAnnouncedAsNewSong() {
        val old = p.frame(a, "https://images.example/a.jpg")
        val next = p.frame(b, "https://images.example/b.jpg")
        assertNull(p.accessibilityDescription(old, next, "ঝুম ভালোবাসা"))
        assertEquals("Artwork for ঝুম ভালোবাসা",
            p.accessibilityDescription(next, next, "ঝুম ভালোবাসা"))
    }

    @Test fun currentCoverUsesAccurateAndBoundedFallbackLabel() {
        val empty = p.frame("", "")
        assertEquals("Song artwork", p.accessibilityDescription(empty, empty, ""))
        assertEquals("Song artwork", p.accessibilityDescription(empty, empty, "  ".trim()))
        val other = p.frame(a, "")
        assertNull(p.accessibilityDescription(empty, other, "Song A"))
    }

    @Test fun reduceMotionNeverRequestsArtworkAnimation() {
        val old = p.frame(a, "cover-a")
        val next = p.frame(b, "cover-b")
        assertFalse(p.shouldAnimate(old, next, reduceMotion = true))
        assertFalse(p.shouldAnimate(next, old, reduceMotion = true))
        assertTrue(p.shouldAnimate(old, next, reduceMotion = false))
    }

    @Test fun previousArtworkRemainsInaccessibleDuringRapidSkips() {
        val frames = listOf(
            p.frame(a, "a.jpg"), p.frame(b, "b.jpg"),
            p.frame("offline:owned", "offline.jpg")
        )
        for (target in frames) {
            val labels = frames.map {
                p.accessibilityDescription(it, target, "Current song")
            }
            assertEquals(1, labels.count { it != null })
            assertEquals("Artwork for Current song", labels[frames.indexOf(target)])
        }
    }

    @Test fun arbitrarySignedThumbnailValuesNeverBecomeTrackIdentity() {
        val signed = "https://cdn.example/?sig=private"
        val first = p.frame(a, signed)
        val second = p.frame(b, signed)
        assertFalse(first == second)
        assertEquals(signed, first.thumbnail)
        assertNull(p.accessibilityDescription(first, second, "Song B"))
    }
}
