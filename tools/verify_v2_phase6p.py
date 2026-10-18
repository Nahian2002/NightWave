#!/usr/bin/env python3
"""6P integration source preservation; not a Kotlin compiler or APK check."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(p): return (root / p).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
service = read(base + "playback/PlaybackService.kt")
queue = read(base + "playback/PlaybackQueue.kt")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
ui = read(base + "v2/ui/NightWaveStudioActivity.kt")
quick = read(base + "v2/library/LibraryQuickPlayPolicy.kt")
search = read(base + "net/SearchRelevancePolicy.kt")
creators = read(base + "net/SavedCreatorBrowser.kt")
library = read(base + "library/MusicLibrary.kt")
gradle = read("app/build.gradle.kts")
windows = read("tools/build-windows.ps1")
local_gate = read("tools/check-phase6p-integration.ps1")
syntax = read("tools/check-builder-syntax.ps1")
workflow = read(".github/workflows/android.yml")
tests = read("app/src/test/java/com/nightwave/player/playback/PlaybackQueueDisplayOrderTest.kt")

assert 'EXTRA_QUEUE_DISPLAY_ORDER = "queue_display_order"' in service
assert "val explicitDisplayOrder = intent.action == ACTION_PLAY_QUEUE" in service
assert "intent.getBooleanExtra(EXTRA_QUEUE_DISPLAY_ORDER, false)" in service
assert service.count("queue.useExplicitDisplayOrder()") == 2
assert "if (sameQueue && (loaded || resolving))" in service
assert "if (explicitDisplayOrder) {" in service
assert "if (explicitDisplayOrder) queue.useExplicitDisplayOrder()" in service
assert "fun useExplicitDisplayOrder()" in queue
assert "shuffle = false" in queue and "shuffledSeen.clear()" in queue
assert "fun playLibraryCollection(collection: List<VideoPreview>)" in bridge
assert "explicitDisplayOrder = false" in bridge
assert "explicitDisplayOrder = true" in bridge
assert ".putExtra(PlaybackService.EXTRA_QUEUE_DISPLAY_ORDER, explicitDisplayOrder)" in bridge
assert "QueueSelectionRules.plan(video, collection)" in bridge
assert "onLibraryQuickPlay = { collection -> playback.playLibraryCollection(collection) }" in ui
assert "onQuickPlay = onLibraryQuickPlay" in ui
assert ui.count("plan.first?.let { onQuickPlay(plan.songs) }") == 2
assert ui.count("enabled = quickPlayableTracks.isNotEmpty()") == 2
assert "onPlay(song, if (selectedPlaylist == null) tracks else orderedTracks)" in ui
assert "MAX_QUEUE = 100" in quick and "ordered.take(MAX_QUEUE)" in quick
assert "VideoLink.normalize(it.url) == it.url" in quick
assert "Normalizer.Form.NFKD" in search
assert "VideoLink.normalize(video.url) != video.url" in creators
assert "nightwave_library_v1" in library
assert service.count("ExoPlayer.Builder(") == 1
assert "playWhenReady = false" in service
assert "PlaybackTransitionPolicy" in service
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert "check-phase6p-integration.ps1" in windows
assert "check-phase6p-integration.ps1" in syntax
assert "Phase 6P integration source preflight" in windows
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in windows
assert "verify-apk-signature.ps1" in windows
assert "getSharedPreferences" not in search and "HttpURLConnection" not in search
assert "tools/verify_v2_phase6p.py" in workflow
assert "feature/nightwave-v2-phase6o4-library-quick-actions" in workflow
assert tests.count("@Test") >= 5
for name, content in (("service", service), ("queue", queue),
                      ("bridge", bridge), ("ui", ui), ("tests", tests)):
    assert content.count("{") == content.count("}"), name
    imports = [ln for ln in content.splitlines() if ln.startswith("import ")]
    assert len(imports) == len(set(imports)), name
print("PASS: 6P integration source contracts, ordered quick play and Windows build guard")
print("PENDING: Gradle compiler/JVM, Android device, signer and GitHub runner availability")
