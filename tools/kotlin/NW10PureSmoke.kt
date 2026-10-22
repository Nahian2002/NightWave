package com.nightwave.nw10

import com.nightwave.player.offline.OfflinePlaybackQueue
import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.playback.QueueEntry
import com.nightwave.player.playback.PublicQueueSelectionPolicy
import com.nightwave.player.playback.ShuffleModePolicy
import com.nightwave.player.playback.RepeatModePolicy
import com.nightwave.player.playback.SoundStudioPolicy
import com.nightwave.player.v2.ui.NightWaveArtworkPolicy
import com.nightwave.player.v2.ui.NightWaveSeekGesturePolicy
import com.nightwave.player.v2.playback.PlaybackSeekIdentityPolicy
import com.nightwave.player.v2.cloud.CloudAuthorizationRecoveryPolicy

/** Runs actual repository Kotlin classes with the real Kotlin compiler, no Android mocks. */
fun main() {
    var cases = 0
    fun verify(result: Boolean, description: String) {
        check(result) { "FAIL: $description" }
        cases++
    }

    val offline = OfflinePlaybackQueue(pick = { 0 })
    verify(offline.select(listOf("a", "b", "c"), "a") == "a", "offline selection")
    verify(offline.jump(2) == "c", "offline jump")
    verify(offline.previous() == "b", "previous after manual jump")
    verify(offline.jump(-1) == null && offline.current == "b", "invalid jump retains selection")
    verify(offline.previous() == "a" && offline.previous() == null,
        "previous follows actual visible queue")
    val duplicate = listOf(
        QueueEntry("https://www.youtube.com/watch?v=firstfirst1", "A", "A", ""),
        QueueEntry("https://www.youtube.com/watch?v=firstfirst1", "A", "A", ""),
        QueueEntry("https://www.youtube.com/watch?v=second22222", "B", "B", "")
    )
    val unique = duplicate.distinctBy { it.url }
    verify(PublicQueueSelectionPolicy.selectedIndex(duplicate, unique, 2) == 1,
        "deduplicated service selection uses clicked identity")
    val queue = PlaybackQueue(pick = { 0 })
    verify(queue.replace(duplicate, 2)?.url == unique[1].url, "public queue replacement")
    verify(ShuffleModePolicy.resolve(false, true, true), "explicit shuffle ON")
    verify(!ShuffleModePolicy.resolve(true, true, false), "explicit shuffle OFF")
    verify(RepeatModePolicy.next(PlaybackQueue.Repeat.OFF) == PlaybackQueue.Repeat.ALL,
        "repeat cycle")
    verify(SoundStudioPolicy.safeManualRange(-2000, 2000) == -1500..1500,
        "manual EQ bounds")
    verify(SoundStudioPolicy.safeManualRange(1700, 2200) == null,
        "unavailable EQ band range")
    verify(NightWaveSeekGesturePolicy.touchFraction(50f, 100f, 10f) == .5f,
        "seek center")
    verify(NightWaveSeekGesturePolicy.touchFraction(-100f, 100f, 10f) == 0f,
        "seek out-of-bounds")
    verify(PlaybackSeekIdentityPolicy.accepts("offline:x", "offline:x", "offline:x"),
        "current song accepted")
    verify(!PlaybackSeekIdentityPolicy.accepts("offline:x", "offline:y", "offline:y"),
        "stale song rejected")
    val low = "https://i.ytimg.com/vi/dQw4w9WgXcQ/default.jpg"
    verify(NightWaveArtworkPolicy.preferredArtwork(low) ==
        "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", "art upgrade")
    verify(NightWaveArtworkPolicy.preferredArtwork("content://media/27") ==
        "content://media/27", "local art untouched")
    verify(CloudAuthorizationRecoveryPolicy.shouldRestore("connect", true),
        "Drive consent preserved across Activity recreation")
    verify(!CloudAuthorizationRecoveryPolicy.shouldRestore("delete", true),
        "unexpected Drive operation rejected")
    println("PASS: NW-10 compiled Kotlin smoke — $cases behavioral assertions")
}
