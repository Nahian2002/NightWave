#!/usr/bin/env python3
"""NW96 source integration checks; do not substitute for Android compile/device EQ."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
svc = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
store = read("app/src/main/java/com/nightwave/player/playback/EqualizerSelectionStore.kt")
policy = read("app/src/main/java/com/nightwave/player/playback/EqualizerSelectionPolicy.kt")
ci = read(".github/workflows/android.yml")
tests = read("app/src/test/java/com/nightwave/player/playback/EqualizerSelectionPolicyTest.kt")
assert "val savedEq = eqSelectionStore.read()" in svc
assert "requestedEqPreset = savedEq?.preset ?: 0" in svc
assert "manualCentersHz = savedEq?.centersHz ?: emptyList()" in svc
assert "manualLevels.clear()" in svc
assert "EqualizerSelectionPolicy.matchesSession(savedCurve, liveCenters)" in svc
assert "candidate.getCenterFreq(it.toShort()) / 1000" in svc
assert "eqSelectionStore.save(EqualizerSelection(preset))" in svc
assert svc.count("eqSelectionStore.save(choice)") >= 2
assert "manualCentersHz == caps.centersHz" in svc
assert "val updated = baseline.toMutableList()" in svc
assert "nightwave_eq_selection_v1" in store
assert "prefs.edit().putString(\"choice\", raw).commit()" in store
assert "val serialized = prefs.getString(\"choice\", null)" in store
assert "require(EqualizerSelectionPolicy.valid(selection))" in store
assert "centersHz == selection.centersHz" in policy
assert "selection.centersHz.distinct().size" in policy
assert tests.count("@Test fun ") >= 6
assert "python3 tools/verify_nw96_equalizer.py" in ci
assert "NW96EqualizerSessionSmoke.kt" in ci
print("NW96_EQ_SESSION_SOURCE_WIRING_PASS")
