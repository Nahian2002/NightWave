#!/usr/bin/env python3
"""NW56 fail-closed source check for palette readability and card text."""
from pathlib import Path
import argparse
import re

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "app/src/main/java/com/nightwave/player/v2/model"
UI = ROOT / "app/src/main/java/com/nightwave/player/v2/ui"
TEST = ROOT / "app/src/test/java/com/nightwave/player/v2/model"


def contrast(a: int, b: int) -> float:
    def luminance(color):
        channels = [((color >> i) & 255) / 255.0 for i in (16, 8, 0)]
        linear = [x / 12.92 if x <= .04045 else ((x + .055) / 1.055) ** 2.4
                  for x in channels]
        return .2126 * linear[0] + .7152 * linear[1] + .0722 * linear[2]
    values = sorted((luminance(a), luminance(b)))
    return (values[1] + .05) / (values[0] + .05)


def check(design: str, contrast_policy: str, test: str, player: str) -> None:
    palettes = re.findall(r"NightWaveTheme\.(\w+) -> Palette\(\s*([^)]*)\)", design)
    if len(palettes) != 4:
        raise AssertionError("Expected the four existing NightWave themes")
    for name, content in palettes:
        colors = [int(x, 16) for x in re.findall(r"0x[0-9A-Fa-f]{8}", content)]
        if len(colors) != 9:
            raise AssertionError(name + ": unexpected color structure")
        bg, surface, raised, foreground, secondary, accent, soft, outline, danger = colors
        light = "false" in content
        on_accent = 0xFFF9F7F2 if light else 0xFF100D20
        for key, left, right in (
            ("foreground/surface", foreground, surface),
            ("foreground/raised", foreground, raised),
            ("secondary/raised", secondary, raised),
            ("accent/raised", accent, raised),
            ("on-primary", on_accent, accent),
        ):
            if contrast(left, right) < 4.5:
                raise AssertionError("%s %s contrast below 4.5" % (name, key))
    if "else 0xFFF9F7F2" not in design:
        raise AssertionError("Pearl action foreground must be readable")
    if "fun ratio(foreground: Long, background: Long)" not in contrast_policy:
        raise AssertionError("Contrast utility missing")
    if "allTextColorsAreReadableOnTheirConfiguredSurfaces" not in test:
        raise AssertionError("Numerical contrast unit coverage missing")
    start = player.index('Text("Sound & playback",')
    if 'color = colors.onSurface' not in player[start:start + 220]:
        raise AssertionError("Sound/Playback heading has inherited low-contrast color")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    src = [
        (MODEL / "NightWaveDesign.kt").read_text(encoding="utf-8"),
        (MODEL / "NightWaveContrastPolicy.kt").read_text(encoding="utf-8"),
        (TEST / "NightWaveContrastPolicyTest.kt").read_text(encoding="utf-8"),
        (UI / "StudioPlayerScreens.kt").read_text(encoding="utf-8"),
    ]
    check(*src)
    if args.self_test:
        for i, a, b in (
            (0, "0xFF815154", "0xFFB48883"),
            (0, "else 0xFFF9F7F2", "else 0xFF17111A"),
            (2, "allTextColorsAreReadableOnTheirConfiguredSurfaces", "testNoChecks"),
            (3, 'color = colors.onSurface)\n                            Text("Crossfade',
             'color = colors.outline)\n                            Text("Crossfade'),
        ):
            broken = src[:]
            assert a in broken[i], a
            broken[i] = broken[i].replace(a, b, 1)
            try:
                check(*broken)
            except AssertionError:
                continue
            raise AssertionError("NW56 contrast mutation not rejected")
        print("NW56 contrast negative fixtures: PASS")
    print("NW56 4-theme normal-text contrast and card hierarchy: PASS")
    print("UNVERIFIED: actual font rendering, dynamic colors and device screenshots.")


if __name__ == "__main__":
    main()
