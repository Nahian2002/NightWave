#!/usr/bin/env python3
"""6X5 library organization static integration checks; not a Gradle/device build."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def source(path): return (root/path).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
folders = source(base + "v2/library/PlaylistFolderStore.kt")
smart = source(base + "v2/library/SmartLibraryPolicy.kt")
ui = source(base + "v2/ui/NightWaveStudioActivity.kt")
library = source(base + "library/MusicLibrary.kt")
browse = source(base + "v2/library/LibraryBrowsePolicy.kt")
player = source(base + "v2/playback/StudioPlaybackBridge.kt")
folder_tests = source("app/src/test/java/com/nightwave/player/v2/library/PlaylistFolderPolicyTest.kt")
smart_tests = source("app/src/test/java/com/nightwave/player/v2/library/SmartLibraryPolicyTest.kt")
ci = source(".github/workflows/android.yml")
assert "nightwave_playlist_folders_v1" in folders
assert "PlaylistFolderSnapshot" in folders
assert "MAX_FOLDERS = 40" in folders
assert "MAX_PLACEMENTS = 300" in folders
assert "fun create(name: String)" in folders
assert "fun rename(old: String, updated: String)" in folders
assert "fun delete(folder: String)" in folders
assert "fun assign(playlist: String, folder: String?, known: List<String>)" in folders
assert "playlistRenamed(old: String, updated: String)" in folders
assert "playlistDeleted(name: String)" in folders
assert 'prefs.contains("snapshot")' in folders
assert "prefs.edit().putString(\"snapshot\", obj.toString()).commit()" in folders
assert "PlaylistFolderPolicy.delete" in folders
assert "placements = before.placements.filterValues { it != folder }" in folders
assert "fun generate(" in smart
assert "MAX_SOURCE_TRACKS = 600" in smart
assert "MAX_COLLECTIONS = 8" in smart
assert "Recent rotation" in smart
assert "Rediscover your likes" in smart
assert "Long listens" in smart
assert "val durationSeconds: Long" in source(base + "net/YouTubeGateway.kt")
assert "it.durationSeconds >= 240L" in smart
assert "it.duration >= 240L" not in smart
assert "Creator mix" in smart
assert "MusicIdentity.display(" in smart
assert ".distinctBy { it.url }" in smart
assert "SmartLibraryPolicy.generate(favorites, recent," in ui
assert "folderStore = PlaylistFolderStore(this)" in ui
assert "folderSnapshot = folderStore.snapshot()" in ui
assert "managePlaylistFolders()" in ui
assert "choosePlaylistFolder(name)" in ui
assert "folderStore.playlistRenamed(name, updated)" in ui
assert "folderStore.playlistDeleted(name)" in ui
assert "onManageFolders = onManageFolders" in ui
assert "onMovePlaylist = onMovePlaylist" in ui
assert "smart-collections-section" in ui
assert "playlist-folders-manager" in ui
assert "move-current-to-folder" in ui
assert "nightwave://folder/unfiled" in ui
assert "selectedSmart != null -> selectedSmart.songs" in ui
assert "selectedSmart == null" in ui
assert "LibraryBrowsePolicy.playlists(" in ui
assert "LibraryBrowsePolicy.songs(" in ui
assert "fun createPlaylist(" in library
assert "fun renamePlaylist(" in library
assert "fun deletePlaylist(" in library
assert "LibraryBrowsePolicy" in browse
assert "fun toggleShuffle()" in player
assert folder_tests.count("@Test") >= 6
assert smart_tests.count("@Test") >= 5
assert "python3 tools/verify_v2_phase6x5.py" in ci
assert "python3 tools/verify_v2_phase6x4.py" in ci
assert "feature/nightwave-v2-phase6x5-smart-library-folders" in ci
for key, value in (("folders",folders),("smart",smart),("ui",ui),
                   ("folder_tests",folder_tests),("smart_tests",smart_tests)):
    assert value.count("{") == value.count("}"), key + " braces"
    imports=[line for line in value.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), key + " duplicate imports"
print("PASS: 6X5 non-destructive playlist folder and smart collection source contracts")
print("PENDING: Android Gradle compilation, JUnit execution and real mobile UI tests")
