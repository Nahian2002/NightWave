#!/usr/bin/env python3
"""NW-04 source contracts; real Android build and device interaction remain necessary."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(name: str) -> str:
    return (ROOT / name).read_text(encoding="utf-8")

prefix = "app/src/main/java/com/nightwave/player/v2/ui/"
unit = "app/src/test/java/com/nightwave/player/v2/ui/"
player = read(prefix + "StudioPlayerScreens.kt")
seek = read(prefix + "NightWaveSeekTimeline.kt")
seek_policy = read(prefix + "NightWaveSeekGesturePolicy.kt")
seek_tests = read(unit + "NightWaveSeekGesturePolicyTest.kt")
artwork = read(prefix + "NightWaveArtworkPolicy.kt")
art_tests = read(unit + "NightWaveArtworkPolicyTest.kt")
chrome = read(prefix + "NightWaveChromePolicy.kt")
navigation = read(prefix + "NightWavePremiumChrome.kt")
chrome_gate = read("tools/verify_v2_phase6i.py")
nav_gate = read("tools/verify_v2_phase6v7a.py")
workflow = read(".github/workflows/android.yml")

# Real artwork is loaded, with an upgrade only for known low-resolution YouTube stills.
assert "NightWaveArtworkPolicy.preferredArtwork(url)" in player
assert "NightWaveArtworkPolicy.allowOriginalFallback(" in player
assert "onError = {" in player
assert 'host !in setOf("i.ytimg.com", "img.youtube.com")' in artwork
assert '"https://i.ytimg.com/vi/' in artwork
assert art_tests.count("@Test") >= 4
assert "refusesExternalHostSpoofingAndInvalidIds" in art_tests

# The UI has a single slim seek timeline, not the tall default Material3 thumb.
assert "NightWaveSeekTimeline(" in player
assert "dragFraction = null" in player
assert "onCancel = { dragFraction = null }" in player
assert 'testTag("nightwave-player-seek")' in seek
assert "ProgressBarRangeInfo(shown, 0f..1f)" in seek
assert "setProgress { requested ->" in seek
assert "detectDragGestures(" in seek and "detectTapGestures" in seek
assert "NightWaveSeekGesturePolicy.touchFraction(" in seek
assert "NightWaveSeekGesturePolicy.thumbX(" in seek
assert "fun touchFraction(" in seek_policy and "fun thumbX(" in seek_policy
assert seek_tests.count("@Test") >= 4
assert "narrowAndInvalidTracksStayBounded" in seek_tests

# Keep audio functionality present but disclosure collapsed by default.
assert "var advancedAudioOpen by remember" in player
assert "if (advancedAudioOpen)" in player
assert 'Text("Sound & playback"' in player
assert 'Text("Lyrics"' in player
assert "onCrossfade(crossfadeDraft)" in player
assert "onListeningLevel(percent)" in player
assert "onSleepTimer(minutes)" in player
assert "onOfflineSpeed(percent)" in player
assert "actions::toggleShuffle" in player
assert "actions::cycleRepeat" in player
assert "PlayerArtworkSwipe.NEXT -> actions.next()" in player
assert "PlayerArtworkSwipe.PREVIOUS -> actions.previous()" in player

# Compact shell retains full controls, explicit close behavior and tap accessibility.
assert "const val NAV_HEIGHT_DP = 60" in chrome
assert "const val NAV_MIN_TOUCH_DP = 56" in chrome
assert "NightWaveChromePolicy.NAV_HEIGHT_DP.dp" in navigation
assert 'testTag("nightwave-mini-player")' in player
assert 'CoverArt(state.thumbnail, Modifier.size(38.dp), 9)' in player
assert 'IconButton(onClick = onHide, modifier = Modifier.size(48.dp))' in player
assert 'IconButton(onClick = actions::playPause, modifier = Modifier.size(48.dp))' in player
assert "NAV_HEIGHT_DP = 60" in nav_gate
assert "height(5.dp)" in chrome_gate and "height(52.dp)" in chrome_gate

# Compilation still required; this source gate is not an Android-release claim.
assert "feature/nightwave-v2-nw04-premium-player" in workflow
assert "python3 tools/verify_nw04_premium_player.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
for label, src in (("player", player), ("seek", seek), ("navigation", navigation)):
    imports = [x for x in src.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), label + ": duplicate imports"
    assert src.count("{") == src.count("}"), label + ": unbalanced braces"

print("PASS: NW04 premium player art, seek, transport, gestures and compact navigation source contracts")
print("UNVERIFIED: Gradle compile, JVM execution, visual QA, real artwork, playback, device accessibility")
