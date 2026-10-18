#!/usr/bin/env python3
"""6O3 playlist browsing source preservation. This is NOT an Android build."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path): return (root/path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
policy = read(base+"v2/library/LibraryBrowsePolicy.kt")
activity = read(base+"v2/ui/NightWaveStudioActivity.kt")
facade = read(base+"library/MusicLibrary.kt")
mutator = read(base+"library/LibraryMutationPlan.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/library/LibraryBrowsePolicyTest.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

assert "data class LibraryPlaylistPreview(" in policy
assert "val size: Int get() = songs.size" in policy
assert "val cover: VideoPreview? get() = songs.firstOrNull()" in policy
assert "fun playlists(" in policy and "fun songs(" in policy
assert "SORT_SAVED" in policy
assert "SORT_NAME" in policy and "SORT_NAME_DESC" in policy
assert "SORT_MOST_TRACKS" in policy
assert "SORT_TITLE" in policy and "SORT_TITLE_DESC" in policy
assert "SORT_CREATOR" in policy
assert "SearchRelevancePolicy.prepare(query.take(80))" in policy
assert "SearchRelevancePolicy.savedMatches(search, song)" in policy
assert "MusicIdentity.display(it.value.title, it.value.author)" in policy
assert "thenBy { it.index }" in policy
assert "return ordered.map { it.value }" in policy
assert "PlaylistPreview" not in mutator
assert "LibraryBrowsePolicy" not in mutator
assert "LibraryBrowsePolicy" not in facade
assert 'getSharedPreferences("nightwave_library_v1"' in facade
assert "LegacyLibraryTransaction.mutate(app," in facade

library = activity.split("private fun StudioLibrary(", 1)[1].split("private fun StudioProfile(", 1)[0]
assert "libraryRevision: Int, roomPreviewActive: Boolean" in library
assert "remember(names, libraryRevision, roomPreviewActive)" in library
assert "LibraryPlaylistPreview(name, getPlaylist(name))" in library
assert "LibraryBrowsePolicy.playlists(playlistPreviews," in library
assert "LibraryBrowsePolicy.songs(tracks, localFilter, sortMode)" in library
assert 'Text("Sort & filter playlists")' not in library or "Sort & filter playlists" in library
assert "playlistToolsOpen" in library
assert "playlistFilter = it.take(80)" in library
assert "selected = showFilledOnly" in library
assert "playlistSort = LibraryBrowsePolicy.SORT_MOST_TRACKS" in library
assert "playlistSort = LibraryBrowsePolicy.SORT_NAME_DESC" in library
assert 'items(visiblePlaylists, key = { it.name })' in library
assert "onSelectPlaylist(collection.name)" in library
assert "collection.cover" in library
assert "onPlay(song, if (selectedPlaylist == null) tracks else orderedTracks)" in library
assert "Clear playlist filters" in library
assert "Title Z–A" in library
assert "MusicCollectionTile(" in library
assert 'MusicCollectionTile("Offline music"' in library
assert 'MusicCollectionTile("Liked songs"' in library
assert 'Text("Import shared playlist")' in library
assert "onSharePlaylist(selectedPlaylist, tracks)" in library
assert "onManage" in library and "onOffline" in library
assert "roomPreviewActive = false" in activity
assert "libraryRevision++" in activity

assert "tools/verify_v2_phase6o3.py" in workflow
assert "feature/nightwave-v2-phase6o2-saved-creator-browser" in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle
assert tests.count("@Test") >= 19
for name, source in (("policy", policy), ("activity", activity), ("tests", tests)):
    assert source.count("{") == source.count("}"), name
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name

print("PASS: 6O3 UI-only playlist/track browsing, legacy data writes unchanged")
print("PENDING: Android/Compose compilation, JVM execution and physical device QA")
