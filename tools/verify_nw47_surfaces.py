#!/usr/bin/env python3
"""NW47 source contract gate. Does not claim Android compilation or phone QA."""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt"
SURFACES = ROOT / "app/src/main/java/com/nightwave/player/v2/ui/NightWaveEverydaySurfaces.kt"
TEST = ROOT / "app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveEverydaySurfacesInstrumentedTest.kt"


def verify(activity: str, surfaces: str, tests: str) -> None:
    required_activity = (
        "import androidx.compose.foundation.lazy.LazyRow",
        "nw47-home-quick-access",
        "nw47-home-continue",
        "nw47-home-offline",
        "nw47-home-liked",
        "nw47-home-search",
        "nw47-recent-shelf",
        "NightWaveRecentShelfTile(",
        "onRemove = { onRemoveRecent(song) }",
        "onOptions = { onSongOptions(song) }",
        "onPlay = { onPlay(song, recent) }",
        "openLikedSongs = {",
        "selectedPlaylist = LIKED_COLLECTION",
        "openOffline = onOfflineLibrary",
        "nw47-library-quick-access",
        "nw47-library-offline",
        "nw47-library-liked",
        "nw47-library-history",
        "nw47-library-new-playlist",
        "onSelectPlaylist(RECENT_COLLECTION)",
        "onCreatePlaylist",
        "nw47-you-quick-access",
        "nw47-you-account",
        "nw47-you-sound",
        "nw47-you-history",
        "Local profile · Your music stays on this device",
        'CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP -> "CONNECTED"',
        "onUndoPlaylistEdit",
        "onBulkRemove",
        "onCloudBackup",
        "onCloudPreviewRestore",
        "onManageHistory",
        "PlaylistPinPolicy.pinnedFirst",
        "LibraryBrowsePolicy.playlists",
    )
    required_surface = (
        "fun NightWaveQuickActionPill(",
        "fun NightWaveRecentShelfTile(",
        "Modifier.heightIn(min = 48.dp)",
        "NightWaveArtworkPolicy.preferredArtwork(song.thumbnail)",
        "NightWaveArtworkPolicy.allowOriginalFallback(",
        "MusicIdentity.display(song.title, song.author)",
        "onClick = onPlay",
        "onClick = onOptions",
        "onClick = onRemove",
        "nw47-recent-options",
        "nw47-recent-remove",
        "nw47-recent-play",
        "contentDescription = \"Remove \"",
        "AsyncImage(",
    )
    required_tests = (
        "quickActionHasARealTapOnCompactPhone",
        "recentlyPlayedActionsRemainIndependentFromPlayback",
        "assertEquals(1, plays)",
        "assertEquals(1, options)",
        "assertEquals(1, removals)",
    )
    for name, source, markers in (
        ("activity", activity, required_activity),
        ("surfaces", surfaces, required_surface),
        ("tests", tests, required_tests),
    ):
        missing = [mark for mark in markers if mark not in source]
        if missing:
            raise AssertionError(name + " missing: " + ", ".join(missing))
    if "song.artist" in surfaces:
        raise AssertionError("VideoPreview has author, not artist")
    if activity.find("nw47-home-quick-access") > activity.find("nw47-recent-shelf"):
        raise AssertionError("Home shortcuts moved below long recent history")
    if activity.find("nw47-library-quick-access") > activity.find(
            'item(key = "playlist-section")'):
        raise AssertionError("Library shortcuts buried below playlist rows")
    if activity.find("nw47-you-quick-access") > activity.find(
            'StudioProfileGroup("Google Drive · optional")'):
        raise AssertionError("You shortcuts buried in settings")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    a, s, t = (
        ACTIVITY.read_text(encoding="utf-8"),
        SURFACES.read_text(encoding="utf-8"),
        TEST.read_text(encoding="utf-8"),
    )
    verify(a, s, t)
    if args.self_test:
        def negative(a0: str, s0: str, t0: str) -> None:
            try:
                verify(a0, s0, t0)
            except AssertionError:
                return
            raise AssertionError("NW47 guard accepted a broken fixture")
        negative(a.replace("openOffline = onOfflineLibrary",
                           "openOffline = {}"), s, t)
        negative(a, s.replace("song.author", "song.artist"), t)
        negative(a, s.replace("onClick = onRemove", "onClick = onPlay"), t)
        negative(a, s, t.replace("assertEquals(1, removals)", "no assertion"))
        print("NW47 negative fixtures: PASS")
    print("NW47 navigation, metadata, history and backup source contracts: PASS")
    print("NOTE: source gate only, not a Gradle/Android build.")


if __name__ == "__main__":
    main()
