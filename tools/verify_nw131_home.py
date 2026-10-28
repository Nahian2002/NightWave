"""Structural Home contracts with broken fixtures; JVM/device verification stays separate."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2"
NAMES = ("ui/NightWaveStudioActivity.kt", "ui/NightWaveHomePresentationPolicy.kt",
         "ui/NightWaveVisualLanguage.kt", "ui/NightWavePremiumChrome.kt",
         "recommendation/NightWaveHomeCurationPolicy.kt")


def verify(src):
    home = src[NAMES[0]].split("internal fun StudioHome(", 1)[1].split("private fun MoodTile(", 1)[0]
    for marker in ("NightWaveHomePresentationPolicy.entry(", "NightWaveHomePresentationPolicy.mix(",
                   "onResumeSpotlight(last, recentShelf)", "onPlay(song, recentShelf)",
                   "onPlay(it, chosenMix.tracks)", "onRemoveRecent(song)",
                   "onClick = openRecentHistory", "confirmClearHistory = true", "onClearRecent()",
                   "if (recentShelf.isNotEmpty())", "presentation = mixPresentations[mix.key]",
                   "onSearch(shortcut.query)", "emptyTitle = entry.primary?.title",
                   "if (entry.showLikedShortcut)", "if (entry.showSearchShortcut)"):
        assert marker in home, marker
    assert "EmptyMusicCard(" not in home
    assert "mix(it, favorites, recent, playlistTracks, memory.enabled)" in home
    for marker in ("ids(favorites)", "ids(recent)", "ids(playlists)", "Kind.HISTORY_ONLY",
                   '"RECENT MUSIC"', '"SAVED MUSIC"', '"Creator blend"',
                   "tracks.count { it.url in liked }", "tracks.count { it.url in history }"):
        assert marker in src[NAMES[1]], marker
    assert "Int.MAX_VALUE" in src[NAMES[2]]
    assert "compact && fontScale < 1.3f" in src[NAMES[2]]
    assert "onClick = onEmptyAction" in src[NAMES[3]]
    assert "onClick = onContinue" in src[NAMES[3]]
    assert "MAX_HOME_RECENT = 100" in src[NAMES[4]]
    assert "ids.count { it in nextUrls } * 5 >= ids.size * 4" in src[NAMES[4]]
    for name in (NAMES[1], NAMES[4]):
        for forbidden in (".edit()", "Http", "enqueue(", "recordPlay(", "android.content"):
            assert forbidden not in src[name], (name, forbidden)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    src = {name: (BASE / name).read_text(encoding="utf-8") for name in NAMES}
    verify(src)
    if args.self_test:
        for name, marker in ((NAMES[0], "onResumeSpotlight(last, recentShelf)"),
                             (NAMES[0], "onPlay(it, chosenMix.tracks)"),
                             (NAMES[0], "onClick = openRecentHistory"),
                             (NAMES[0], "onRemoveRecent(song)"),
                             (NAMES[0], "presentation = mixPresentations[mix.key]"),
                             (NAMES[0], "if (entry.showSearchShortcut)"),
                             (NAMES[4], "ids.count { it in nextUrls } * 5 >= ids.size * 4")):
            broken = dict(src)
            broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try:
                verify(broken)
            except AssertionError:
                continue
            raise AssertionError("Accepted broken fixture: " + marker)
    print("PASS NW131 Home wiring and negative fixtures; device/UI acceptance NOT_RUN")
