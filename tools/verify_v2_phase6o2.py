#!/usr/bin/env python3
"""6O2 bounded saved-creator browser source assertions. NOT runtime tests."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
def src(path): return (root/path).read_text(encoding="utf-8")
base="app/src/main/java/com/nightwave/player/"
group=src(base+"net/SavedCreatorBrowser.kt")
identity=src(base+"net/MusicIdentity.kt")
search=src(base+"v2/ui/NightWaveStudioActivity.kt")
tests=src("app/src/test/java/com/nightwave/player/net/SavedCreatorBrowserTest.kt")
ci=src(".github/workflows/android.yml")
gradle=src("app/build.gradle.kts")

assert "object SavedCreatorBrowser" in group
assert "MAX_SOURCE_TRACKS = 350" in group
assert "MAX_GROUPS = 48" in group
assert "MAX_QUEUE_TRACKS = 100" in group
assert "source.take(MAX_SOURCE_TRACKS)" in group
assert "seenUrls.add(video.url)" in group
assert "VideoLink.normalize(video.url) != video.url" in group
assert "MusicIdentity.display(video.title, video.author)" in group
assert "SearchRelevancePolicy.normalize(label)" in group
assert "key in generic" in group
assert "it.name.lowercase(Locale.ROOT)" in group
assert "SavedCreatorGroup(it.name, it.count, it.tracks.toList())" in group
assert "fun creatorsFrom(tracks: List<VideoPreview>, limit: Int = 8)" in identity
ui=search.split("private fun StudioSearch(",1)[1].split("private fun StudioLibrary(",1)[0]
assert "SavedCreatorBrowser.groups(savedTracks)" in ui
assert "MusicIdentity.creatorsFrom(savedTracks, limit = 8)" in ui
assert "selectedCreator = creator.name" in ui
assert "selectedCreatorGroup != null && query.isBlank()" in ui
assert "onPlay(song, selectedCreatorGroup.tracks)" in ui
assert "onSongOptions(song)" in ui
assert "Back to discovery" in ui
assert "Browse all saved creators" in ui
assert "creatorGroups.chunked(2)" in ui
assert "showAllCreators = false" in ui
assert "onPrimarySearch(selectedCreatorGroup.name, SearchMode.SONGS)" in ui
assert "A local creator shortcut based on video metadata" in ui
assert "SavedTrackSearch.match(query, savedTracks)" in ui
assert "onPrimarySearch(creator.name" not in ui
assert "onNativeSearch(creator.name" not in ui
assert "selectedCreator = null" in ui
assert "tools/verify_v2_phase6o2.py" in ci
assert "feature/nightwave-v2-phase6o1-relevant-music-search" in ci
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert tests.count("@Test") >= 16
for name,text in (("model",group),("search",search),("tests",tests)):
    assert text.count("{")==text.count("}"), name
    imports=[ln for ln in text.splitlines() if ln.startswith("import ")]
    assert len(imports)==len(set(imports)), name
for forbidden in ("HttpURLConnection","NewPipe","MediaController","StreamInfo.getInfo","SharedPreferences"):
    assert forbidden not in group, forbidden
print("PASS: 6O2 saved-only creator identities, local scoped browsing and 100-item queue bound")
print("PENDING: Android build/JVM execution, UI dimensions and device QA")
