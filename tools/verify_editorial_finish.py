"""DESIGN04 caller contracts; source checks never certify phone rendering or gesture quality."""
from pathlib import Path
import argparse
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
FILES = ("StudioPlayerScreens.kt", "NightWaveStudioActivity.kt", "NightWavePagerRecovery.kt",
         "NightWaveEditorialPresentationPolicy.kt", "NightWaveTrackDetails.kt", "NightWaveVisualLanguage.kt")
def verify(src):
    player, home, recovery, policy, details, visual = (src[n] for n in FILES)
    for marker in ("NightWaveArtworkFramePolicy.heightDp(", "resolvedArtwork?.intrinsicWidth", "resolvedArtwork?.intrinsicHeight",
                   "Modifier.size(width = art, height = artHeight)", "NightWaveTrackDetails(state.title, state.artist, state.videoUrl)",
                   'testTag("editorial-track-details")', "NightWaveEditorialPresentationPolicy.identity(state.title, state.artist)"):
        assert marker in player, marker
    for marker in ("LocalNightWaveShelfGestures provides shelfGestures", "!shelfGestures.active",
                   "NightWavePagerRecovery(pagerState, shelfGestures.active)", "featured = true, emphasized = true"):
        assert marker in home, marker
    for marker in ("PointerEventPass.Initial", "gestures.begin(owner)", "finally { gestures.end(owner) }",
                   "onDispose { gestures?.end(owner) }", "dragging || shelfTouch"):
        assert marker in recovery, marker
    for marker in ("MusicIdentity.display(title, creator)", "!promotion.containsMatchIn(title)", "edition.containsMatchIn(it)",
                   "durationSeconds in 1..604800"):
        assert marker in policy, marker
    for bad in ("Http", ".edit()", "startActivity", "recordPlay"):
        assert bad not in policy, bad
    assert 'Text(title.ifBlank' in details and 'testTag("editorial-original-title")' in details
    assert 'testTag("editorial-original-source")' in details and "SelectionContainer" in details
    assert "if (selected || emphasized)" in visual and "NightWaveEditorialPresentationPolicy.mixFacts(mix)" in visual
if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(); src = {n: (BASE / n).read_text(encoding="utf-8") for n in FILES}; verify(src)
    if args.self_test:
        for name, marker in ((FILES[0], "NightWaveArtworkFramePolicy.heightDp("),
                             (FILES[0], "Modifier.size(width = art, height = artHeight)"),
                             (FILES[0], "NightWaveTrackDetails(state.title, state.artist, state.videoUrl)"),
                             (FILES[1], "LocalNightWaveShelfGestures provides shelfGestures"),
                             (FILES[1], "!shelfGestures.active"), (FILES[1], "featured = true, emphasized = true"),
                             (FILES[2], "finally { gestures.end(owner) }"),
                             (FILES[3], "!promotion.containsMatchIn(title)"),
                             (FILES[4], 'testTag("editorial-original-title")'),
                             (FILES[5], "NightWaveEditorialPresentationPolicy.mixFacts(mix)")):
            broken = dict(src); broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try: verify(broken)
            except AssertionError: continue
            raise AssertionError("Accepted broken editorial finish: " + marker)
    print("PASS editorial framing, original-caption access, mix hierarchy and shelf-touch ownership; device NOT_RUN")
