"""DESIGN06 caller checks; rendering, gestures and data acceptance require independent device evidence."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
FILES = ("StudioPlayerScreens.kt", "ListeningMemoryPanel.kt", "ListeningTrendPanel.kt",
         "NightWaveStatisticsCards.kt", "NightWaveListeningPresentationPolicy.kt",
         "NightWaveVisualLanguage.kt", "NightWaveEditorialPresentationPolicy.kt", "NightWaveStudioActivity.kt")


def verify(src):
    player, memory, trend, cards, labels, visual, art, activity = (src[n] for n in FILES)
    overlay = player.split("fun StudioNowPlayingOverlay(", 1)[1].split("val colors =", 1)[0]
    for marker in ('updateTransition(targetState = expanded, label = "player-overlay")',
                   "overlayVisibility.AnimatedVisibility(", "overlayVisibility.isRunning", "visible = { it }", "NightWavePlayerOverlayPolicy.retainBackdrop("):
        assert marker in overlay, marker
    for forbidden in ("fadeIn(", "fadeOut(", "LaunchedEffect(expanded)", "MutableTransitionState(false)"):
        assert forbidden not in overlay, forbidden
    for marker in ("onCheckedChange = onEnabledChange", "onClick = onRefresh", "onClick = onErase",
                   "ListeningTrendPanel(", "NightWaveStatisticsCards(listOf(", "NightWaveEditorialPresentationPolicy.identity(track.title, track.artist)"):
        assert marker in memory, marker
    for marker in ("ListeningTrendEngine.rolling(", "ListeningTrendEngine.groupedByWeek(points)",
                   "point.labelUtc()", "clearAndSetSemantics", "NightWaveListeningPresentationPolicy.dateLabel(point)",
                   "drawRoundRect(colors.primary", 'testTag("listening-window-30")', 'testTag("listening-window-7")'):
        assert marker in trend, marker
    assert "LinearProgressIndicator" not in trend
    assert "columns(maxWidth.value, LocalDensity.current.fontScale)" in cards
    assert "if (columns == 1) Column" in cards
    assert "DateTimeFormatter.ofPattern" in labels and '" · UTC"' in labels
    assert "NightWaveEditorialPresentationPolicy.mixArtwork(mix)" in visual
    for marker in ("tracks.distinctBy { it.url }", "track.thumbnail.isNotBlank()", "creators.add(creator)", "covers.size == 4"):
        assert marker in art, marker
    for forbidden in (".edit()", "Http", "recordPlay(", "android.content"):
        assert forbidden not in labels and forbidden not in art, forbidden
    assert '"Check storage without changing your music"' in activity
    assert "modifier.fillMaxWidth().heightIn(min = 64.dp)" in activity


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(); src = {n: (BASE / n).read_text(encoding="utf-8") for n in FILES}; verify(src)
    if args.self_test:
        for name, marker in ((FILES[0], 'updateTransition(targetState = expanded, label = "player-overlay")'),
                             (FILES[0], "NightWavePlayerOverlayPolicy.retainBackdrop("),
                             (FILES[1], "onCheckedChange = onEnabledChange"),
                             (FILES[1], "onClick = onErase"),
                             (FILES[2], "ListeningTrendEngine.rolling("),
                             (FILES[2], "clearAndSetSemantics"),
                             (FILES[2], "drawRoundRect(colors.primary"),
                             (FILES[3], "columns(maxWidth.value, LocalDensity.current.fontScale)"),
                             (FILES[5], "NightWaveEditorialPresentationPolicy.mixArtwork(mix)"),
                             (FILES[6], "creators.add(creator)")):
            broken = dict(src); assert marker in broken[name]
            broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try: verify(broken)
            except AssertionError: continue
            raise AssertionError("Accepted broken continuity caller: " + marker)
    print("PASS nonblank transition wiring, readable actual listening totals, UTC/accessibility, consent and source-only covers; device NOT_RUN")
