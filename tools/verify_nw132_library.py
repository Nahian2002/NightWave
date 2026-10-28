"""NW132 caller/presentation/edit/document boundaries; compilation/device proof is separate."""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player"
NAMES = ("v2/ui/NightWaveStudioActivity.kt", "v2/library/LibraryCollectionPresentationPolicy.kt",
         "v2/library/LibraryBrowsePolicy.kt", "v2/library/LibraryBulkSelectionPolicy.kt", "v2/ui/NightWaveLibraryOptions.kt")


def verify(src):
    activity = src[NAMES[0]]
    screen = activity.split("internal fun StudioLibrary(", 1)[1].split("private fun StudioProfile(", 1)[0]
    for marker in ("LibraryCollectionPresentationPolicy.overview(", "LibraryCollectionPresentationPolicy.automaticDetails(",
                   "LibraryCollectionPresentationPolicy.songSection(", "overview.showRecentlyLiked", "overview.showSaveHint",
                   '"nw47-library-new-playlist"', '"nw132-library-options"', '"nw132-playlist-options"',
                   "onExportPlaylist(name, tracks)", "onTogglePinPlaylist(name)", "onMovePlaylist(name)",
                   "onManageFolders", "onImportSharedPlaylist", "onRemoveRecent(song)", "onClearRecent()",
                   "LibraryBulkSelectionPolicy.sanitize(", "hiddenSelectedCount", "onBulkRemove(it, selectedTracks)",
                   "onMovePlaylistSong(name, song.url, steps, tracks.map { it.url })"):
        assert marker in screen, marker
    assert "rememberSaveable(selectedPlaylist," in screen
    assert "state = libraryListState" in screen
    assert '"Saved tracks"' not in screen
    assert 'if (visibleTracks.isEmpty() && selectedPlaylist != null)' in screen
    for marker in ("library.restorePlaylistIfUnchanged(", "before.map { it.url }", ".setNegativeButton(\"Keep\", null)",
                   "ActivityResultContracts.OpenDocument()", "ActivityResultContracts.CreateDocument(\"text/plain\")",
                   "PlaylistDocumentPolicy.read(input)", "showSharedPlaylistPreview(parsed)",
                   '.setPositiveButton("Import")', "library.importSharedPlaylist(incoming.name, incoming.tracks)"):
        assert marker in activity, marker
    assert "MAX_PLAYLIST_SONGS = 300" in src[NAMES[2]]
    assert ".take(MAX_PLAYLIST_SONGS)" in src[NAMES[2]]
    assert "VideoLink.normalize(url) == url" in src[NAMES[3]]
    assert "filter(::eligible)" in src[NAMES[3]]
    assert "MAX_SELECTED = 20" in src[NAMES[3]]
    assert "expanded = false\n                    action()" in src[NAMES[4]]
    for name in (NAMES[1], NAMES[2], NAMES[3]):
        for bad in (".edit()", "android.content", "Http", "enqueue("):
            assert bad not in src[name], (name, bad)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    src = {name: (BASE / name).read_text(encoding="utf-8") for name in NAMES}
    verify(src)
    if args.self_test:
        for name, marker in ((NAMES[0], "LibraryCollectionPresentationPolicy.songSection("),
                             (NAMES[0], "overview.showRecentlyLiked"), (NAMES[0], "state = libraryListState"), (NAMES[0], "onExportPlaylist(name, tracks)"),
                             (NAMES[0], "library.restorePlaylistIfUnchanged("), (NAMES[0], "PlaylistDocumentPolicy.read(input)"),
                             (NAMES[2], ".take(MAX_PLAYLIST_SONGS)"), (NAMES[3], "VideoLink.normalize(url) == url")):
            broken = dict(src); assert marker in broken[name]
            broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try:
                verify(broken)
            except AssertionError:
                continue
            raise AssertionError("Accepted broken fixture: " + marker)
    print("PASS NW132 Library identity, complete bounded browse, valid bulk selection and retained document/recovery wiring; device NOT_RUN")
