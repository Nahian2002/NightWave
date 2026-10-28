"""DESIGN03 actual caller contracts; Android rendering/interaction requires device evidence."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
FILES = ("NightWaveStudioActivity.kt", "StudioPlayerScreens.kt", "StudioComponents.kt",
         "NightWaveVisualLanguage.kt", "NightWaveTheme.kt")


def verify(src):
    home, player, components, visual, theme = (src[n] for n in FILES)
    search = home.split("internal fun StudioSearch(", 1)[1].split("internal fun StudioLibrary(", 1)[0]
    for marker in ('key = { "search-starter:" + it.url }', 'item(key = "search-offline-quick-access")',
                   'if (searchHistory.isNotEmpty())', 'savedMatches.take(savedVisible)',
                   'offlineMatches.take(offlineVisible)', 'onOfflineSearch("")', 'onPrimarySearch(shortcut.query, SearchMode.SONGS)'):
        assert marker in search, marker
    assert search.index('key = { "search-starter:" + it.url }') < search.index('if (searchHistory.isNotEmpty())')
    assert search.index('key = { "search-starter:" + it.url }') < search.index('item(key = "search-offline-quick-access")')
    for state in ("recentShelfState", "nextShelfState", "moodShelfState"):
        assert f"rememberSnapFlingBehavior({state}, SnapPosition.Start)" in home, state
    assert "itemsIndexed(DiscoveryShortcuts.picks" in home
    assert "contentScale = if (large) ContentScale.Inside else ContentScale.Crop" in player
    assert "requestCandidates(url, large)" in player and "candidateIndex == attempt" in player
    assert "CoverArt(video.thumbnail, modifier" in components
    assert "trackIdentity = video.url, large = large" in components
    assert "Text(name, color = colors.onSurface" in components
    assert "contentColor = colors.onSurface" in components
    assert theme.count("NightWaveContentPresentationPolicy.errorSurface(palette)") == 2
    assert "Modifier.padding(start = 12.dp, top = 12.dp).size(72.dp)" in visual


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(); src = {n: (BASE / n).read_text(encoding="utf-8") for n in FILES}; verify(src)
    if args.self_test:
        for name, marker in ((FILES[0], 'key = { "search-starter:" + it.url }'),
                             (FILES[0], "rememberSnapFlingBehavior(moodShelfState, SnapPosition.Start)"),
                             (FILES[0], "rememberSnapFlingBehavior(recentShelfState, SnapPosition.Start)"),
                             (FILES[0], "rememberSnapFlingBehavior(nextShelfState, SnapPosition.Start)"),
                             (FILES[1], "ContentScale.Inside"), (FILES[1], "candidateIndex == attempt"),
                             (FILES[2], "Text(name, color = colors.onSurface"),
                             (FILES[2], "trackIdentity = video.url, large = large"),
                             (FILES[3], "Modifier.padding(start = 12.dp, top = 12.dp).size(72.dp)"),
                             (FILES[4], "NightWaveContentPresentationPolicy.errorSurface(palette)")):
            broken = dict(src); broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try:
                verify(broken)
            except AssertionError:
                continue
            raise AssertionError("Accepted broken content finish: " + marker)
    print("PASS content hierarchy, snapping, source artwork, normal text and error roles; device NOT_RUN")
