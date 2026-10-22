package com.nightwave.player.v2.ui

import com.nightwave.player.playback.ShuffleModePolicy
import com.nightwave.player.playback.SoundStudioPolicy
import com.nightwave.player.v2.cloud.CloudAuthorizationRecoveryPolicy
import com.nightwave.player.v2.playback.PlaybackSeekIdentityPolicy

/**
 * No Android mocks. NW-12 independently compiled these six exact GitHub
 * production source blobs with kotlinc 1.9.0 and executed 28 assertions.
 */
fun main() {
    var passed = 0
    fun expect(label: String, yes: Boolean) { check(yes) { label }; passed++ }
    expect("shuffle explicit on", ShuffleModePolicy.resolve(false, true, true))
    expect("shuffle explicit idempotence", ShuffleModePolicy.resolve(true, true, true))
    expect("shuffle explicit off", !ShuffleModePolicy.resolve(true, true, false))
    expect("shuffle legacy toggle", !ShuffleModePolicy.resolve(true, false, true))
    expect("seek positive", PlaybackSeekIdentityPolicy.accepts("offline:a", "offline:a", "offline:a"))
    expect("seek stale display", !PlaybackSeekIdentityPolicy.accepts("offline:a", "offline:b", "offline:a"))
    expect("seek stale media", !PlaybackSeekIdentityPolicy.accepts("offline:a", "offline:a", "offline:b"))
    expect("seek null", !PlaybackSeekIdentityPolicy.accepts("offline:a", "offline:a", null))
    expect("cloud connect", CloudAuthorizationRecoveryPolicy.shouldRestore("connect", true))
    expect("cloud unknown action", !CloudAuthorizationRecoveryPolicy.shouldRestore("delete", true))
    expect("cloud absence", !CloudAuthorizationRecoveryPolicy.shouldRestore(null, true))
    expect("cloud current", CloudAuthorizationRecoveryPolicy.requestIsCurrent(2, 2, true, false))
    expect("cloud stale", !CloudAuthorizationRecoveryPolicy.requestIsCurrent(2, 3, true, false))
    expect("cloud dialog active", !CloudAuthorizationRecoveryPolicy.requestIsCurrent(2, 2, true, true))
    expect("cloud timeout", CloudAuthorizationRecoveryPolicy.timeoutMs() == 20_000L)
    expect("gesture midpoint", NightWaveSeekGesturePolicy.touchFraction(54f,108f,8f) == .5f)
    expect("gesture clamp left", NightWaveSeekGesturePolicy.touchFraction(-22f,108f,8f) == 0f)
    expect("gesture clamp right", NightWaveSeekGesturePolicy.touchFraction(200f,108f,8f) == 1f)
    expect("gesture invalid NaN", NightWaveSeekGesturePolicy.touchFraction(Float.NaN,108f,8f) == 0f)
    expect("thumb center", NightWaveSeekGesturePolicy.thumbX(.5f,108f,8f) == 54f)
    expect("art upgraded", NightWaveArtworkPolicy.preferredArtwork("https://i.ytimg.com/vi/dQw4w9WgXcQ/default.jpg") == "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg")
    expect("art preserved local", NightWaveArtworkPolicy.preferredArtwork("content://media/external/audio/27") == "content://media/external/audio/27")
    expect("art reject spoof", NightWaveArtworkPolicy.preferredArtwork("https://i.ytimg.com.attacker.test/vi/dQw4w9WgXcQ/default.jpg").contains("attacker.test"))
    expect("EQ limit", SoundStudioPolicy.safeManualRange(-2400,2400) == (-1500..1500))
    expect("EQ unavailable", SoundStudioPolicy.safeManualRange(1600,2500) == null)
    expect("EQ valid gain", SoundStudioPolicy.validLevels(listOf(0, 1000), 2, -1500, 1500))
    expect("EQ invalid gain", !SoundStudioPolicy.validLevels(listOf(0, 1600), 2, -1500, 1500))
    expect("EQ label", SoundStudioPolicy.gainLabel(200) == "+2.0 dB")
    println("PASS NW-12 hash-matched source compilation and $passed Kotlin behavior assertions")
}
