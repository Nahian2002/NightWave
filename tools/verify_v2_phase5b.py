#!/usr/bin/env python3
"""NightWave Alpha5B source gate. Not a substitute for Android Gradle/device QA."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
code = root / "app/src/main/java/com/nightwave/player"
read = lambda p: p.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
studio = read(code / "v2/ui/NightWaveStudioActivity.kt")
bridge = read(code / "v2/playback/StudioPlaybackBridge.kt")
service = read(code / "playback/PlaybackService.kt")
saved = read(code / "playback/QueueStateStore.kt")
gateway = read(code / "net/YouTubeGateway.kt")
browser = read(code / "ui/YouTubeSearchBrowser.kt")
classic = read(code / "ui/MainActivity.kt")
offline = read(code / "offline/OfflineAudioLibrary.kt")
manifest = read(root / "app/src/main/AndroidManifest.xml")

assert 'versionCode = 33' in gradle
assert 'versionName = "2.0.0-alpha5q"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert "private data class StudioSearchSnapshot(" in studio
assert "private var nativeSearchState by mutableStateOf" in studio
assert "withContext(Dispatchers.IO)" in studio
assert "nativeGateway.search(clean, mode)" in studio
assert "nativeSearchJob?.cancel()" in studio
assert "StudioTab.SEARCH -> StudioSearch(" in studio
assert "items(state.videos, key = { it.url })" in studio
assert "onClick = { onPlay(song, state.videos) }" in studio
assert "Open YouTube search" in studio
assert "YouTubeSearchBrowser(this).show(query)" in studio
assert "SearchMode.SONGS" in studio and "SearchMode.VIDEOS" in studio
assert "playerActions.retry()" in studio

# We do NOT remove the old, known-working official YouTube browser or public
# search alternate. Neither a native query nor a browser click guarantees a
# playable public stream.
assert "CommunityVideoSearch()" in gateway
assert "searchWithFilter" in gateway
assert "YouTubeSearchBrowser(this).show(clean)" in classic
assert "WebSearchRoute.selectedVideo" in browser

assert 'ACTION_RETRY_CURRENT' in service
assert 'private fun retryCurrent()' in service
assert 'autoRetriedVideo' in service
assert 'recoveryAttempt: Boolean = false' in service
assert 'ERROR_CODE_IO_BAD_HTTP_STATUS' in service
assert 'if (autoRetriedVideo == video)' in service
assert 'main.post {' in service
assert 'player.currentPosition > 3_000L' in service
assert 'clearMediaItems()' in service
assert 'savedQueue.recordFailure(entry.url' in service
assert 'savedQueue.clearFailure()' in service

assert "fun recordFailure(publicUrl: String, label: String)" in saved
assert "fun failure(expectedUrl: String?): String?" in saved
assert "fun clearFailure()" in saved
assert "prefs.edit().putString" in saved
assert "error = if (player.playerError != null)" in bridge
assert "persistence.failure(savedTrack?.url)" in bridge
assert "override fun retry()" in bridge
assert "val offline: Boolean get()" in bridge
assert 'offlineId ?: media?.mediaId.orEmpty()' in bridge
assert 'if (now.offline)' in studio
assert 'if (playerState.hasTrack && !playerState.offline)' in studio
assert 'DefaultDataSource.Factory(this, networkSource)' in service
assert 'OfflineAudioLibrary(this)' in service
assert 'if (offlinePlaying)' in service
assert 'advanceOffline(forward = true)' in service

# The Phase 5A crash fixes must remain intact: no eager YouTube resolution,
# no background network work just because a controller connected.
creation = service.split("override fun onCreate()", 1)[1].split(
    "override fun onStartCommand", 1
)[0]
assert "savedQueue.restore(queue)" in creation
assert "resolveAndPlay(" not in creation
assert "main.postDelayed(progressWriter" not in creation
assert "NightWaveCrashRecorder.install(this)" in read(code / "NightWaveApp.kt")
assert "NightWaveCrashRecorder.report(this)" in classic

ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count("android.intent.category.LAUNCHER") == 1
assert '<activity android:name=".v2.ui.NightWaveStudioActivity"' in manifest
assert 'fallbackToDestructiveMigration(' not in read(code / "v2/data/room/NightWaveDatabase.kt")

assert read(root / "app/src/test/java/com/nightwave/player/playback/PlaybackQueueTest.kt").count("@Test") >= 10
assert read(root / "app/src/androidTest/java/com/nightwave/player/playback/QueueStateBookmarkInstrumentedTest.kt").count("@Test") >= 9

for name, value in (
    ("StudioActivity", studio), ("Bridge", bridge), ("Media3 service", service),
    ("Queue storage", saved), ("Search gateway", gateway)
):
    assert value.count("{") == value.count("}"), name + " unbalanced braces"
    imports = [line for line in value.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"

print("PASS: premium native results, browser fallback, queue selection, bounded")
print("      stream retry, offline state, and Phase 5A startup/crash safety")
print("PENDING: Gradle compilation, unit/instrumented execution, device playback QA")
