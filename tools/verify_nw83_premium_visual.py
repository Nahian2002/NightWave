#!/usr/bin/env python3
"""NW83 read-only source contract. Not a Compose screenshot or Android build."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "app/src/main/java/com/nightwave/player/v2/ui/"
def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

tokens = read(BASE + "NightWavePremiumDesignTokens.kt")
theme = read(BASE + "NightWaveTheme.kt")
tracks = read(BASE + "StudioComponents.kt")
home = read(BASE + "NightWavePremiumChrome.kt")
search = read(BASE + "NightWaveDiscoveryChrome.kt")
activity = read(BASE + "NightWaveStudioActivity.kt")
player = read(BASE + "StudioPlayerScreens.kt")
ambient = read(BASE + "NightWaveVisualLanguage.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/ui/NightWavePremiumDesignTokensTest.kt")
contrast_tests = read("app/src/test/java/com/nightwave/player/v2/model/NightWaveContrastPolicyTest.kt")
smoke = read("tools/NW83VisualTokensSmoke.kt")

# Every Material3 typography role is explicit: no default text styles bleed in.
for role in ("displayLarge", "displayMedium", "displaySmall",
             "headlineLarge", "headlineMedium", "headlineSmall",
             "titleLarge", "titleMedium", "titleSmall",
             "bodyLarge", "bodyMedium", "bodySmall",
             "labelLarge", "labelMedium", "labelSmall"):
    assert role + " = TextStyle(" in theme, role
for role in ("primaryContainer", "onPrimaryContainer",
             "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
             "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
             "surfaceTint", "outlineVariant", "inverseSurface", "inverseOnSurface",
             "onError", "errorContainer", "onErrorContainer"):
    assert theme.count(role + " = ") == 2, role

# Shared sizing is actually consumed by all main pages, not only declared.
assert activity.count("NightWavePremiumDesignTokens.pageGutterDp(") == 4
assert activity.count("NightWavePremiumDesignTokens.MAX_CONTENT_WIDTH_DP.dp") == 4
assert "NightWavePremiumDesignTokens.rowMinHeightDp(fontScale)" in tracks
assert "NightWavePremiumDesignTokens.titleLines(fontScale" in tracks
assert "NightWavePremiumDesignTokens.rowBorderAlpha(" in tracks
assert "NightWavePremiumDesignTokens.ROW_ARTWORK_DP.dp" in tracks
assert "NightWavePremiumDesignTokens.NAV_CORNER_DP.dp" in home
assert "NightWavePremiumDesignTokens.HERO_CORNER_DP.dp" in home
assert "NightWavePremiumDesignTokens.SEARCH_CARD_CORNER_DP.dp" in search
assert "NightWavePremiumDesignTokens.rowMinHeightDp(fontScale)" in player
assert "val miniShape = RoundedCornerShape(" in player
assert ".shadow(7.dp, miniShape)" in player
assert ".border(1.dp, colors.outline.copy(alpha = .26f), miniShape)" in player
assert "NightWavePremiumDesignTokens.titleLines(fontScale" in player

# Decorative accents remain in the cool NightWave spectrum; stored song IDs
# and original metadata must not be passed to UI-only design tokens for edits.
assert "0xFF83D9D5" in ambient
assert "0xFF7FBAD3" in ambient
assert "const val MIN_TOUCH_TARGET_DP = 48" in tokens
assert "fun controlTouchTargetDp(fontScale: Float)" in tokens
assert "rowMinHeightDp(1.8f)" in tests
assert "NW83_PREMIUM_VISUAL_TOKENS_SMOKE_PASS" in smoke
assert theme.count("errorContainer = paletteColor(palette.surface)") == 2
assert "nw83MaterialContainerRolesMeetNormalTextContrastInAllPalettes" in contrast_tests
assert "ratio(p.danger, p.surface) >= 4.5" in contrast_tests
assert "Palette" not in tokens and "VideoPreview" not in tokens
print("NW83_PREMIUM_VISUAL_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: Android compilation, rendered screenshots, font clipping and real-device UX")
