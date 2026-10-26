#!/usr/bin/env python3
"""NW84 read-only Home wiring and safety contract; NOT a Compose/Android build."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

BASE = "app/src/main/java/com/nightwave/player/"
policy = read(BASE + "v2/recommendation/NightWaveHomeCurationPolicy.kt")
activity = read(BASE + "v2/ui/NightWaveStudioActivity.kt")
premium = read(BASE + "v2/ui/NightWavePremiumChrome.kt")
surfaces = read(BASE + "v2/ui/NightWaveEverydaySurfaces.kt")
layout = read(BASE + "v2/ui/NightWaveHomeLayoutPolicy.kt")
tests = read("app/src/test/java/com/nightwave/player/v2/recommendation/NightWaveHomeCurationPolicyTest.kt")
ui_tests = read("app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveEverydaySurfacesInstrumentedTest.kt")

# Existing last-played playback policy remains the owner of resume/restore.
assert "onContinue: (() -> Unit)? = null" in premium
assert "modifier.clickable(role = Role.Button, onClick = onContinue)" in premium
assert 'if (identity == null) "Your listening space" else "Continue listening"' in premium
assert "MusicIdentity.display(it.title, it.author)" in premium

# Non-fabricated, deterministic selection from eligible device library metadata.
for marker in (
    "VideoLink.normalize(it.url) == it.url",
    ".filterNot { it.url in recentUrls }",
    "MixDiversityPolicy.select(",
    "val favoriteUrls",
    "val signatures",
    "overlap * 5 >= minOf(ids.size, other.size) * 4",
    "return Plan(next, chosen)",
):
    assert marker in policy, marker
assert "memoryTracks" not in policy and "ListeningMemory" not in policy

home = activity.split("private fun StudioHome(", 1)[1].split(
    "@Composable\nprivate fun MoodTile(", 1)[0]
for marker in (
    "NightWaveHomeCurationPolicy.curate(",
    "val mixes = homePlan.mixes",
    "onContinue = recent.firstOrNull()?.let { last ->",
    "onResumeSpotlight(last, recent)",
    'item(key = "nw84-next-listens-heading")',
    'item(key = "nw84-next-listens-shelf")',
    "NightWaveNextListenTile(",
    "provenance = NightWaveHomeCurationPolicy.sourceLabel(",
    "onPlay = { onPlay(song, homePlan.nextListens) }",
    "onOptions = { onSongOptions(song) }",
    'item(key = "nw47-recent-shelf")',
    'item(key = "nw47-home-library-stats")',
    "mixes.chunked(mixesPerRow)",
    "NightWaveHomeLayoutPolicy.mixCardCompact(mixesPerRow)",
):
    assert marker in home, marker

# Actual music appears above counters; original Recent and Continue remain.
positions = [home.index(key) for key in (
    'item(key = "nightwave-premium-home-header")',
    'item(key = "nw47-home-quick-access")',
    'item(key = "nw84-next-listens-heading")',
    'item(key = "recent-heading")',
    'item(key = "nw47-recent-shelf")',
    'item(key = "nw47-home-library-stats")',
)]
assert positions == sorted(positions)
assert home.index('if (mixes.isNotEmpty()) {') < home.index(
    'item(key = "nw47-home-library-stats")')
assert "onResumeSpotlight(it, recent)" in home

assert "fun NightWaveNextListenTile(" in surfaces
assert '"nw84-next-listen-play"' in surfaces
assert '"nw84-next-listen-options"' in surfaces
assert "NightWaveArtworkPolicy.preferredArtwork(song.thumbnail)" in surfaces
assert "NightWaveArtworkPolicy.allowOriginalFallback(" in surfaces
assert "MusicIdentity.display(song.title, song.author)" in surfaces
assert "NightWavePremiumDesignTokens.CARD_CORNER_DP.dp" in surfaces

for check in (
    "nextListensExcludeRecentAndKeepOriginalSourceMetadata",
    "invalidLinksAndDuplicateSavedReferencesNeverBecomeRecommendations",
    "nearDuplicateMixCardsDoNotRepeatSameCatalogOrRewriteTracks",
    "uniqueMixCardsAreCappedWithOriginalPriorityAndNoFabrication",
):
    assert check in tests, check
for check in (
    "nw84RealLastPlayedHeroIsAnIndependentContinueAction",
    "nw84NextListenTileHasSeparatePlayAndOptionsActions",
):
    assert check in ui_tests, check

assert "mixColumns(windowWidthDp: Float, fontScale: Float)" in layout
assert "return if (width < 390f || scale >= 1.3f) 1 else 2" in layout
print("NW84_MUSIC_FIRST_HOME_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: full Gradle, Compose instrumentation, device experience, playback")
