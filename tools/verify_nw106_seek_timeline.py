#!/usr/bin/env python3
"""NW106 source-gate: seek gesture geometry and accessibility wiring."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
p=(root/"app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekGesturePolicy.kt").read_text()
ui=(root/"app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekTimeline.kt").read_text()
tests=(root/"app/src/test/java/com/nightwave/player/v2/ui/NightWaveSeekGesturePolicyTest.kt").read_text()
ci=(root/".github/workflows/android.yml").read_text()
assert "fun touchFractionOrNull(" in p
assert "val usable = width - 2f * edge" in p
assert "if (!usable.isFinite() || usable <= 0f) return null" in p
assert "fun accessibilityFractionOrNull(" in p
assert "requested.isFinite()" in p
assert "touchFractionOrNull(" in ui
assert ui.count("touchFractionOrNull(") >= 3
assert "var validDrag = false" in ui
assert "if (validDrag) commitCallback(trackIdentity, dragged)" in ui
assert "accessibilityFractionOrNull(requested)" in ui
assert "if (mapped != null)" in ui
assert "else false" in ui
assert ui.count("NightWaveSeekGesturePolicy.touchFraction(") == 0
assert tests.count("@Test fun ") == 9
assert "tools/NW106SeekTimelineSmoke.kt" in ci
assert "tools/verify_nw106_seek_timeline.py" in ci
print("NW106_SEEK_TIMELINE_SOURCE_WIRING_PASS checks=16")
