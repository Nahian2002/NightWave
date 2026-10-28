"""Matte design caller/negative contracts. Compilation and physical acceptance are separate."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
UI = "app/src/main/java/com/nightwave/player/v2/ui/"
NAMES = (UI + "NightWaveMatteComponents.kt", UI + "StudioPlayerScreens.kt",
         UI + "NightWaveSeekTimeline.kt", UI + "NightWaveStudioActivity.kt",
         UI + "NightWaveNativeMatte.kt", UI + "NightWaveTheme.kt")


def verify(src):
    components, player, seek, home, native, theme = (src[name] for name in NAMES)
    for marker in ("collectIsPressedAsState()", "interactionSource = source", "LinearOutSlowInEasing",
                   "LocalNightWaveReduceMotion.current", "LocalRippleConfiguration provides null",
                   "bentoColumns(maxWidth.value, LocalDensity.current.fontScale)",
                   "artwork?.let", "toBitmap(NightWaveMattePolicy.BACKDROP_BITMAP_PX",
                   "BlurredEdgeTreatment.Rectangle", "supportsBlur(android.os.Build.VERSION.SDK_INT)"):
        assert marker in components, marker
    for forbidden in ("pointerInput", "AsyncImage", "Http", ".edit()", "startActivity", "enqueue("):
        assert forbidden not in components, forbidden
    for marker in ("remember(state.videoUrl, state.thumbnail)", "NightWaveArtworkBackdrop(resolvedArtwork",
                   "artwork.trackId == state.videoUrl && artwork.thumbnail == state.thumbnail",
                   "onResolved?.invoke(loaded.result.drawable)", "NightWaveMattePlayButton(onClick = actions::playPause",
                   "actions.seekToForTrack(originatingTrack, it)"):
        assert marker in player, marker
    for marker in ("showSeekThumb(pressed || dragging, focused, touchExploration)", ".focusable(enabled)",
                   ".onPreviewKeyEvent", "Key.DirectionRight", "addTouchExplorationStateChangeListener(listener)",
                   "removeTouchExplorationStateChangeListener(listener)", "commitCallback(trackIdentity, requested"):
        assert marker in seek, marker
    for marker in ("mixes.firstOrNull()?.let", "mixes.drop(1).chunked(2)", "NightWaveBentoPair(first = firstCell",
                   "onPlay(it, chosenMix.tracks)", "onResumeSpotlight(last, recentShelf)", "onClearRecent()"):
        assert marker in home, marker
    assert '.getString("theme", null)' in native
    for bad in (".edit()", ".put", ".load()", "register", "Http", "account"):
        assert bad not in native, bad
    assert "surfaceTint = Color.Transparent" in theme
    assert "Theme_NightWave_Pearl" in theme


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    src = {name: (ROOT / name).read_text(encoding="utf-8") for name in NAMES}
    verify(src)
    if args.self_test:
        mutations = ((NAMES[0], "interactionSource = source"), (NAMES[0], "BlurredEdgeTreatment.Rectangle"),
                     (NAMES[1], "artwork.trackId == state.videoUrl && artwork.thumbnail == state.thumbnail"),
                     (NAMES[2], "removeTouchExplorationStateChangeListener(listener)"),
                     (NAMES[2], ".focusable(enabled)"), (NAMES[3], "mixes.drop(1).chunked(2)"),
                     (NAMES[4], '.getString("theme", null)'), (NAMES[5], "Theme_NightWave_Pearl"))
        for name, marker in mutations:
            broken = dict(src); broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try:
                verify(broken)
            except AssertionError:
                continue
            raise AssertionError("Accepted broken matte contract: " + marker)
    print("PASS matte theme/feedback/artwork/seek/caller contracts; device acceptance NOT_RUN")
