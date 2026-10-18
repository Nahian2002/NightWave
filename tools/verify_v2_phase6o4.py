#!/usr/bin/env python3
"""Phase 6O4 source-only quick action preservation; not an Android test."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(p):return (root/p).read_text(encoding="utf-8")
base="app/src/main/java/com/nightwave/player/"
planner=src(base+"v2/library/LibraryQuickPlayPolicy.kt")
activity=src(base+"v2/ui/NightWaveStudioActivity.kt")
selection=src(base+"v2/playback/QueueSelection.kt")
service=src(base+"playback/PlaybackService.kt")
library=src(base+"library/MusicLibrary.kt")
tests=src("app/src/test/java/com/nightwave/player/v2/library/LibraryQuickPlayPolicyTest.kt")
workflow=src(".github/workflows/android.yml")
gradle=src("app/build.gradle.kts")
assert "object LibraryQuickPlayPolicy" in planner
assert "MAX_QUEUE = 100" in planner
assert "VideoLink.normalize(it.url) == it.url" in planner
assert ".distinctBy { it.url }" in planner
assert "safe.shuffled(Random(seed))" in planner
assert "ordered.take(MAX_QUEUE)" in planner
assert "val first: VideoPreview? get() = songs.firstOrNull()" in planner
assert "totalPlayable > songs.size" in planner
assert "QueueSelectionRules" in selection
assert "val plan = QueueSelectionRules.plan(video, collection)" in src(base+"v2/playback/StudioPlaybackBridge.kt")
assert "urls.take(100)" in service
assert "LibraryQuickPlayPolicy.plan(" in activity
assert "shuffle = true, seed = System.nanoTime()" in activity
assert "shuffle = false, seed = 0L" in activity
assert "plan.first?.let { onQuickPlay(plan.songs) }" in activity
assert "onLibraryQuickPlay = { collection -> playback.playLibraryCollection(collection) }" in activity
assert "LibraryQuickPlayPolicy.plan(orderedTracks, shuffle = false, seed = 0L).songs" in activity
assert "enabled = quickPlayableTracks.isNotEmpty()" in activity
assert "onCreatePlaylist = onCreatePlaylist" in activity
assert "promptPlaylistName { name ->" in activity
assert "library.createPlaylist(name)" in activity
assert "onSearch = { navigateToTab(StudioTab.SEARCH) }" in activity
assert "Text("+'"New playlist"'+", maxLines = 1)" in activity
assert "Text("+'"Find music"'+", maxLines = 1)" in activity
assert "Text("+'"Shuffle play"'+", maxLines = 1)" in activity
assert "Text("+'"Play list"'+", maxLines = 1)" in activity
assert "selectedPlaylist != LIKED_COLLECTION && tracks.isNotEmpty()" in activity
assert "onSharePlaylist(selectedPlaylist, tracks)" in activity
assert "MusicCollectionTile("+'"Offline music"'+"" in activity
assert "MusicCollectionTile("+'"Liked songs"'+"" in activity
assert 'getSharedPreferences("nightwave_library_v1"' in library
assert "tools/verify_v2_phase6o4.py" in workflow
assert "feature/nightwave-v2-phase6o3-premium-library-browsing" in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert tests.count("@Test") >= 16
for name,s in (("planner",planner),("activity",activity),("tests",tests)):
    assert s.count("{")==s.count("}"),name
    imports=[line for line in s.splitlines() if line.startswith("import ")]
    assert len(imports)==len(set(imports)),name
print("PASS: 6O4 bounded, explicit quick play/shuffle and original library data")
print("PENDING: Kotlin Gradle/JVM execution, Compose dimensions, device QA")
