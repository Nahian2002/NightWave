package com.nightwave.player.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NW68 pure JVM regressions for session ownership and crossfade deck fencing. */
class AudioEngineOwnershipPolicyTest {
    @Test fun eqCapabilitiesCannotAdvertiseUnownedOrInvalidSessions() {
        val p = EqualizerSessionPolicy
        assertTrue(p.canPublish(47, true, 5, -1500, 1500))
        assertFalse(p.canPublish(-1, true, 5, -1500, 1500))
        assertFalse(p.canPublish(0, true, 5, -1500, 1500))
        assertFalse(p.canPublish(47, false, 5, -1500, 1500))
        assertFalse(p.canPublish(47, true, 0, -1500, 1500))
        assertFalse(p.canPublish(47, true, 33, -1500, 1500))
        assertFalse(p.canPublish(47, true, 5, 1500, -1500))
    }

    @Test fun eqInstanceMustStillOwnSameLiveSession() {
        val p = EqualizerSessionPolicy
        assertTrue(p.canReuse(21, 100, 100, true, true))
        assertFalse(p.canReuse(0, 100, 100, true, true))
        assertFalse(p.canReuse(21, 100, 101, true, true))
        assertFalse(p.canReuse(21, 100, 100, false, true))
        assertFalse(p.canReuse(21, 100, 100, true, false))
        assertFalse(p.canReuse(21, -1, -1, true, true))
    }

    private fun handoff(
        offline: Boolean = true, gapless: Boolean = true,
        shuffle: Boolean = false, repeatOne: Boolean = false,
        paused: Boolean = false, oldPlaying: Boolean = true,
        newPlaying: Boolean = true, oldIndex: Int = 1,
        nextIndex: Int = 2, count: Int = 4,
        selected: String? = "song-b.mp3", next: String? = "song-c.mp3",
        oldMedia: String? = "offline:song-b.mp3",
        newMedia: String? = "offline:song-c.mp3"
    ): Boolean = CrossfadeHandoffPolicy.mayHandoff(
        offline, gapless, shuffle, repeatOne, paused,
        oldPlaying, newPlaying, oldIndex, nextIndex, count,
        selected, next, oldMedia, newMedia
    )

    @Test fun handoffAcceptsOnlyPlayingNextOwnedOfflineItem() {
        assertTrue(handoff())
        assertFalse(handoff(oldPlaying = false))
        assertFalse(handoff(newPlaying = false))
        assertFalse(handoff(offline = false))
        assertFalse(handoff(gapless = false))
        assertFalse(handoff(shuffle = true))
        assertFalse(handoff(repeatOne = true))
        assertFalse(handoff(paused = true))
    }

    @Test fun staleMediaOrSelectionNeverReceivesSession() {
        assertFalse(handoff(oldIndex = -1))
        assertFalse(handoff(oldIndex = 0))
        assertFalse(handoff(nextIndex = 3))
        assertFalse(handoff(count = 2))
        assertFalse(handoff(selected = "song-a.mp3"))
        assertFalse(handoff(selected = null))
        assertFalse(handoff(next = "song-d.mp3"))
        assertFalse(handoff(next = null))
        assertFalse(handoff(oldMedia = "https://example.com/stream"))
        assertFalse(handoff(newMedia = "offline:song-d.mp3"))
        assertFalse(handoff(newMedia = null))
        assertFalse(handoff(newMedia = "offline:song-b.mp3"))
    }
}
