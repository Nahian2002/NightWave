"""Theme-only expression contracts and broken caller fixtures; never a rendered-quality score."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
FILES = ("NightWaveMattePolicy.kt", "NightWavePremiumChrome.kt", "StudioPlayerScreens.kt",
         "NightWaveStudioActivity.kt", "NightWaveVisualLanguage.kt", "NightWaveTheme.kt", "NightWaveNativeMatte.kt",
         "NightWaveMatteSystemBars.kt")


def verify(src):
    policy, chrome, player, home, visual, theme, native, bars = (src[name] for name in FILES)
    for marker in ("fun transportFill(palette: Palette): Long = palette.accent",
                   "fun transportInk(palette: Palette): Long = palette.contrastOnAccent()"):
        assert marker in policy, marker
    assert "targetValue = if (active) colors.primaryContainer" in chrome
    assert 'listOf(colors.surface, colors.primaryContainer)' in chrome
    mini = player.split("fun StudioMiniPlayer(", 1)[1].split("internal fun CoverArt(", 1)[0]
    assert ".background(colors.surfaceVariant)" in mini
    assert "PlayerProgressRules.fraction(state.positionMs, state.durationMs)" in mini
    assert "featured = NightWaveThemeExpressionPolicy.featuredMixCell(pair.size)" in home
    assert "NightWaveThemeExpressionPolicy.sectionSurface(palette, secondary)" in visual
    assert "tint = accent, modifier = Modifier.size(23.dp)" in visual
    for body in (theme, native):
        for marker in ("Theme_NightWave_Aurora", "Theme_NightWave_Pearl", "Theme_NightWave_AMOLED"):
            assert marker in body, marker
    for role in ("surfaceDim", "surfaceBright", "surfaceContainerLowest", "surfaceContainerLow",
                 "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest"):
        assert role + " = paletteColor(" in theme, role
    assert "if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false" in bars
    assert "LaunchedEffect(state.prefillToken)" in home
    assert "SearchDraftPolicy.shouldApply(state.prefillToken, consumedPrefill)" in home
    assert 'if (!queryFocused) Text("Search songs, artists or links"' in home
    assert "LaunchedEffect(state.query)" not in home
    for marker in ("tertiary = paletteColor(palette.secondaryAccent)",
                   "tertiaryContainer = paletteColor(palette.secondarySoft)"):
        assert marker in theme, marker


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(); src = {name: (UI / name).read_text(encoding="utf-8") for name in FILES}; verify(src)
    if args.self_test:
        for name, marker in ((FILES[0], "Long = palette.accent"), (FILES[1], "colors.primaryContainer"),
                             (FILES[2], ".background(colors.surfaceVariant)"), (FILES[3], "featuredMixCell(pair.size)"),
                             (FILES[4], "sectionSurface(palette, secondary)"), (FILES[4], "tint = accent, modifier"),
                             (FILES[5], "Theme_NightWave_Aurora"), (FILES[6], "Theme_NightWave_AMOLED"),
                             (FILES[3], "SearchDraftPolicy.shouldApply(state.prefillToken, consumedPrefill)"),
                             (FILES[3], 'if (!queryFocused) Text("Search songs, artists or links"'),
                             (FILES[5], "surfaceContainerHigh = paletteColor("),
                             (FILES[7], "if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false")):
            broken = dict(src); broken[name] = broken[name].replace(marker, "BROKEN_FIXTURE")
            try:
                verify(broken)
            except AssertionError:
                continue
            raise AssertionError("Accepted broken theme wiring: " + marker)
    print("PASS expressive theme roles, native selection, colored controls and bounded collection caller; device NOT_RUN")
