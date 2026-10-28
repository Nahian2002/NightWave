"""Regression guard for tiny-art and idle split-page corrections; device verification stays separate."""
from pathlib import Path
import argparse
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
FILES = ("NightWaveStudioActivity.kt", "NightWavePagerRecovery.kt", "StudioPlayerScreens.kt", "NightWaveVisualLanguage.kt")
def verify(src):
    root, recovery, player, visual = (src[n] for n in FILES)
    for marker in ("NightWavePagerRecovery(pagerState)", "StudioTab.entries[pagerState.settledPage]",
                   "NightWavePagerRecoveryPolicy.needsNavigation(", "onReselect = { navigateToTab(tab) }",
                   "userScrollEnabled = !expanded && !showQueue", "pagerState.animateScrollToPage(destination.ordinal,"):
        assert marker in root, marker
    assert root.count("nightWaveShelfBoundary()") >= root.count("horizontalScroll(") + 4
    for marker in ("Offset(available.x, 0f)", "Velocity(available.x, 0f)", "snapshotFlow", "collectLatest",
                   "state.isScrollInProgress, dragging, state.pageCount", "delay(100L)", "state.scrollToPage(page)"):
        assert marker in recovery, marker
    assert "ContentScale.Inside" not in player
    assert "contentScale = if (large) ContentScale.Fit else ContentScale.Crop" in player
    assert "requestCandidates(url, large)" in player and "candidateIndex == attempt" in player
    assert "Modifier.fillMaxWidth().aspectRatio(1.6f)" in visual
    assert "if (featured) 88.dp else 64.dp" in visual
if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(); src = {n: (BASE / n).read_text(encoding="utf-8") for n in FILES}; verify(src)
    if args.self_test:
        for name, marker in ((FILES[0], "NightWavePagerRecovery(pagerState)"),
                             (FILES[0], "StudioTab.entries[pagerState.settledPage]"),
                             (FILES[0], "onReselect = { navigateToTab(tab) }"),
                             (FILES[0], "NightWavePagerRecoveryPolicy.needsNavigation("),
                             (FILES[0], "nightWaveShelfBoundary()"),
                             (FILES[1], "Offset(available.x, 0f)"), (FILES[1], "Velocity(available.x, 0f)"),
                             (FILES[1], "state.isScrollInProgress, dragging, state.pageCount"),
                             (FILES[2], "ContentScale.Fit"), (FILES[3], "Modifier.fillMaxWidth().aspectRatio(1.6f)")):
            broken = dict(src); broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try: verify(broken)
            except AssertionError: continue
            raise AssertionError("Accepted broken recovery: " + marker)
    print("PASS fitted source artwork, shelf isolation, settled navigation and idle pager recovery; device NOT_RUN")
