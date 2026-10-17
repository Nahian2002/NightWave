#!/usr/bin/env python3
"""6D static source audit. Authored for later CI; NOT an Android build."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(p):
    return (root / p).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
service = read(base + "playback/PlaybackService.kt")
gate = read(base + "playback/PlaybackResolutionPolicy.kt")
queue = read(base + "playback/QueueStateStore.kt")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
studio = read(base + "v2/ui/NightWaveStudioActivity.kt")
test = read("app/src/test/java/com/nightwave/player/playback/PlaybackResolutionPolicyTest.kt")
android_test = read("app/src/androidTest/java/com/nightwave/player/playback/QueueLookupIndicatorInstrumentedTest.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

# Prevent stale network jobs becoming new music: at most two workers and one
# queued request, cancel superseded jobs, invalidating generation before install.
assert "ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS," in service
assert "ArrayBlockingQueue<Runnable>(1)" in service
assert "private var resolveFuture: Future<*>?" in service
assert "resolveFuture?.cancel(true)" in service
assert "resolver.queue.clear()" in service
assert "resolver.submit(Runnable {" in service
assert "if (Thread.currentThread().isInterrupted) return@Runnable" in service
assert "PlaybackResolutionPolicy.acceptsResult(request, generation," in service
assert "resolveDeadline?.let(main::removeCallbacks)" in service
assert "generation++ // future resolution is now invalid" in service
assert "PlaybackResolutionPolicy.timedOut(startElapsedMs, SystemClock.elapsedRealtime())" in service
assert "Audio lookup took too long. Tap Retry or choose another song." in service
assert "Music lookup is busy. Tap Retry in a moment." in service
assert "RESOLUTION_TIMEOUT_MS = 35_000L" in gate
assert "expectedUrl == selectedUrl" in gate
assert "requestGeneration == currentGeneration" in gate
assert "!offlineMode" in gate

# Real identity / bookmark protections, no automatic startup stream request.
assert "PlaybackResolutionPolicy.reusesLoadedTrack(" in service
assert "private fun resumeSavedQueue()" in service
assert "Never resume an offline/different cached item" in service
assert "savedQueue.bookmark(entry.url)?.positionMs ?: 0L" in service
assert "PlaybackResolutionPolicy.awaitsSameResolution(entry.url, resolvingPublicUrl)" in service
assert "if (sameQueue && (loaded || resolving))" in service
assert "savedQueue.restore(queue)" in service
assert "Do NOT play, seek, or access any offline file on service creation." in service
assert "if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)" in service

# Pending pause is respected when an asynchronous result finally returns.
assert "playWhenReady = autoPlay" in service
assert "val shouldPlay = autoPlay && playWhenReady" in service
assert "if (shouldPlay) play()" in service
assert "it.playWhenReady)) it.pause()" in bridge
assert "exoPlayer?.play() // resume intent only; do NOT re-extract" in service

# Repeat-one should seek loaded media first; expired media still has a single
# old onPlayerError fallback, not a runaway retry loop.
assert "queue.repeat == PlaybackQueue.Repeat.ONE" in service
assert "savedQueue.saveBookmark(repeating!!.url, 0L, 0L)" in service
assert "val repeating = queue.current" in service
assert "if (autoRetriedVideo == video)" in service
assert "Stream refresh failed. Tap Retry to try again." in service
assert "if (!advanceOffline(forward = true))" in service

# Stream pending state: never put signed URLs into prefs, never leave
# stale spinner after timeout, service restart, switch, or process death.
assert 'putString("resolving_url", publicUrl)' in queue
assert 'putLong("resolving_at_elapsed", SystemClock.elapsedRealtime())' in queue
assert "fun lookupPending(expectedUrl: String?): Boolean" in queue
assert "now - then <= PlaybackResolutionPolicy.RESOLUTION_TIMEOUT_MS" in queue
assert 'prefs.edit().remove("resolving_url").remove("resolving_at_elapsed").apply()' in queue
assert "savedQueue.clearLookup()" in service
assert "savedQueue.markLookup(entry.url)" in service
assert "persistence.lookupPending(savedTrack?.url)" in bridge
assert "player.playbackState == Player.STATE_BUFFERING || resolving" in bridge
assert "else if (offlineId != null || resolving) null" in bridge

# No new permission / network provider / hidden background player.
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'resolveAndPlay(' in service
assert 'StudioMiniPlayer(' in studio
assert 'onHide = { compactHidden = true }' in studio
assert "tools/verify_v2_phase6d.py" in workflow
assert ":app:compileDebugAndroidTestKotlin" in workflow
assert test.count("@Test") >= 5
assert android_test.count("@Test") >= 3
assert "assumeTrue(" in android_test
assert "Build.FINGERPRINT" in android_test

for name, content in (("service", service), ("queue", queue), ("bridge", bridge),
                      ("policy", gate), ("unit tests", test)):
    assert content.count("{") == content.count("}"), name + " unmatched braces"
    imports = [line for line in content.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"

print("PASS: 6D bounded/cancelable extractor, pending pause, ID-safe resume and repeat-one reuse")
print("PENDING: full Gradle compilation/JUnit, emulator, physical audio timing and APK signing")
