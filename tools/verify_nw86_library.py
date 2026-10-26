#!/usr/bin/env python3
"""NW86 read-only source contracts: Library, folders and compact You.
This is NOT an Android/Compose build and does not execute user data edits.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def source(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
browse = source(base + "v2/library/LibraryBrowsePolicy.kt")
selection = source(base + "v2/library/LibraryBulkSelectionPolicy.kt")
folders = source(base + "v2/library/PlaylistFolderStore.kt")
pin = source(base + "v2/library/PlaylistPinPolicy.kt")
library = source(base + "library/MusicLibrary.kt")
mutations = source(base + "library/LibraryMutationPlan.kt")
ui = source(base + "v2/ui/NightWaveStudioActivity.kt")
selection_tests = source("app/src/test/java/com/nightwave/player/v2/library/LibraryBulkSelectionPolicyTest.kt")
browse_tests = source("app/src/test/java/com/nightwave/player/v2/library/LibraryBrowsePolicyTest.kt")
smoke = source("tools/NW86LibrarySmoke.kt")

# NW82 exact-token semantics reach collection names; keep original saved order.
assert "SearchTokenMatchingPolicy.allCovered(" in browse
assert "SearchTokenMatchingPolicy.tokens(name)" in browse
assert 'const val SORT_OLDEST = "oldest"' in browse
assert "SORT_OLDEST -> matched.reversed()" in browse

# Never silently expand single-writer bulk edit beyond its matching 20-track cap.
assert "const val MAX_SELECTED = 20" in selection
assert "operation.urls.size !in 1..20" in mutations
assert "operation.tracks.size !in 1..20" in mutations
assert "fun toggleVisible(" in selection
assert "fun hiddenSelectionCount(" in selection
assert "fun visibleActionLabel(" in selection
assert "val allowed = source.map { it.url }.toSet()" in selection
assert "safe.size >= MAX_SELECTED" in selection

# Confirm destructive history-only mutation, without deleting saved audio.
library_ui = ui.split("private fun StudioLibrary(", 1)[1].split(
    "@Composable\nprivate fun StudioProfile(", 1)[0]
assert "confirmClearHistory by rememberSaveable(selectedPlaylist)" in library_ui
assert 'testTag("nw86-library-clear-recent-request")' in library_ui
assert 'testTag("nw86-library-clear-recent-confirm")' in library_ui
assert "onDismissRequest = { confirmClearHistory = false }" in library_ui
assert "onClearRecent()" in library_ui
assert "Liked songs" in library_ui and "offline audio stay saved" in library_ui
assert "onRemove = if (selectedPlaylist == RECENT_COLLECTION)" in library_ui
assert "selectedTracks = LibraryBulkSelectionPolicy.selected(tracks, selectedTrackUrls)" in library_ui
assert "LibraryBulkSelectionPolicy.hiddenSelectionCount(" in library_ui
assert "LibraryBulkSelectionPolicy.toggleVisible(" in library_ui
assert 'testTag("nw86-select-visible")' in library_ui
assert 'testTag("nw86-clear-selection")' in library_ui
assert "selectedTrackUrls = emptySet()" in library_ui
assert '"Oldest first"' in library_ui and '"Newest first"' in library_ui
assert "selectedFolder = null" in library_ui
assert '"Show all playlists"' in library_ui


home = ui.split("private fun StudioHome(", 1)[1].split(
    "@Composable\nprivate fun MoodTile(", 1)[0]
assert "var confirmClearHistory by rememberSaveable" in home
assert 'testTag("nw86-clear-recent-request")' in home
assert 'testTag("nw86-clear-recent-confirm")' in home
assert 'onClearRecent()' in home

# Folder/pin management remains metadata-only and playlist order is untouched.
assert "PlaylistFolderPolicy.assign(" in folders
assert "PlaylistFolderPolicy.rename(" in folders
assert '"nightwave_playlist_folders_v1"' in folders
assert "fun <T> pinnedFirst(" in pin
assert "MusicLibrary(" not in folders
assert "fun removeRecent(url: String)" in library
assert "fun clearRecent(): Boolean" in library

# Compact You retains the real account, profile and recovery actions.
you = ui.split("private fun StudioProfile(", 1)[1]
assert '"nw86-you-drive-status"' in you
assert '"Backup connection"' in you
assert "CloudAccountPresentation.stage(cloud)" in you
assert "onOpenAccount" in you
assert 'StudioProfileGroup("Library & backup")' in you
assert 'StudioProfileRow("Manage backups"' in you
assert 'StudioProfileRow("Back up now"' in you
assert 'StudioProfileRow("Preview cloud restore"' in you
assert 'StudioProfileRow("Manage Recently Played"' in you
assert '"Sound Studio", "Equalizer' in you

for case in (
    "nw86SelectVisibleAddsToOtherFilterSelectionsWithoutLosingThem",
    "nw86VisibleSelectionCannotExceedLegacyAtomicTwentySongLimit",
    "nw86UnknownRowsCannotBeSelectedThroughStaleFilteredViews",
    "nw86SelectingVisibleDoesNotModifyOriginalPlaylistOrOrdering",
):
    assert case in selection_tests, case
for case in (
    "nw86ShortPlaylistQueryNeverMatchesLongerUnrelatedWord",
    "nw86PlaylistQueryRequiresEachRepeatedWord",
    "nw86BanglaVowelMarksStayDistinctInPlaylistNames",
    "nw86RecentlyPlayedOldestViewOnlyReversesVisibleReadOrder",
):
    assert case in browse_tests, case
assert "NW86_LIBRARY_SELECTION_POLICY_SMOKE_PASS" in smoke
print("NW86_LIBRARY_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: Android build, Compose UI, full JVM tests and live data")
