#!/usr/bin/env python3
"""NW88 read-only audio safety and Sound Studio wiring contracts.

Checks changed sources and test presence; never claims acoustic effect,
Gradle success or Samsung device validation.
"""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def source(path):
    return (ROOT / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
service = source(base + "playback/PlaybackService.kt")
readback = source(base + "playback/EqualizerReadbackPolicy.kt")
ui = source(base + "v2/ui/NightWaveSoundStudio.kt")
crossfade = source(base + "playback/CrossfadePolicy.kt")
normalization = source(base + "playback/OfflineNormalizationPolicy.kt")
gapless = source(base + "offline/OfflineGaplessPolicy.kt")
junit = source("app/src/test/java/com/nightwave/player/playback/EqualizerReadbackPolicyTest.kt")
smoke = source("tools/NW88AudioSmoke.kt")

# Manual EQ may be labelled effective only after a same-session, owned
# Android effect returned matching band gains. Never promise route audibility.
for marker in (
    "EqualizerReadbackPolicy.inspect(",
    "EqualizerReadbackPolicy.Result.MISMATCH",
    "EqualizerReadbackPolicy.Result.INVALID",
    "candidate.getBandLevel(band.toShort()).toInt()",
    "candidate.enabled = true",
    "candidate.hasControl()",
    "equalizerSessionId = sessionId",
    "onAudioSessionIdChanged(audioSessionId: Int)",
):
    assert marker in service, marker
assert "Output route may bypass effects" in readback
assert "if (requestedMb.isEmpty()" in readback
assert "Result.DEVICE_QUANTIZED" in readback

# Only existing PlaybackService commands and persisted local-only stores
# control the UI. No new decoder, server, paid API or fabricated spectrum.
for marker in (
    "CrossfadeSettingsStore(context)",
    "OfflineNormalizationStore(context)",
    "PlaybackService.ACTION_CROSSFADE_SET",
    "PlaybackService.ACTION_NORMALIZATION_SET",
    "crossfadeStore.read()",
    "normalizationStore.enabled()",
    "transitionBusy",
    "AudioTransitionUiPolicy.dialogDetail(selectedCrossfade)",
    "NW88" if False else "Local playback quality",
    "Offline volume balancing",
    "Crossfade preference saved for eligible offline tracks",
    "not ReplayGain or streaming normalization",
):
    assert marker in ui, marker
assert "action == PlaybackService.ACTION_EQ_BANDS_APPLY" in ui
assert "if (pendingCommand != null || transitionBusy) return" in ui

# Fallback modes, crossfade ownership and attenuation remain intact.
assert "const val DEFAULT_SECONDS = 0" in crossfade
assert "fun canPrepare(" in crossfade
assert "fun gains(" in crossfade
assert "fun combined(" in normalization
assert "MAX_SAMPLED_MS = 12_000L" in normalization
assert "MAX_PREPARED_ITEMS = 64" in gapless
assert "CrossfadeHandoffPolicy.mayHandoff(" in service
assert "OfflineGaplessPolicy.transitionIndex(" in service

for test in (
    "nw88ExactManualBandReadbackCanBeReportedAsVerified",
    "nw88DeviceQuantizationIsNotFabricatedAsAnExactMatch",
    "nw88IgnoredBoostOrLargeMismatchCannotClaimManualEqActive",
    "nw88MalformedOrStaleBandReceiptsFailClosed",
):
    assert test in junit, test
assert "NW88_SOUND_STUDIO_AUDIO_POLICIES_SMOKE_PASS" in smoke
print("NW88_SOUND_STUDIO_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: full Gradle/JUnit, Android EQ audibility, routing and device playback")
