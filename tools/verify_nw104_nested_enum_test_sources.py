#!/usr/bin/env python3
"""NW104 checks actual Kotlin JUnit enum references; source check != compiler."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SUITES = {
    "playback/AudioRouteTransportPolicyTest.kt": 6,
    "playback/EqualizerSelectionPolicyTest.kt": 6,
    "offline/OfflineShuffleCycleIntegrityTest.kt": 8,
    "playback/PlaybackResolutionPolicyTest.kt": 8,
    "widget/WidgetPlaybackIntentPolicyTest.kt": 5,
    "playback/MediaSessionQueueControlPolicyTest.kt": 8,
    "playback/PlaybackQueueManualNextAvailabilityTest.kt": 6,
    "offline/OfflineManualNextAvailabilityTest.kt": 6,
}
BAD = re.compile(r"\bp\.(?:Action|Key|Command)\b")

def no_invalid_enum(s: str):
    match = BAD.search(s)
    if match:
        raise AssertionError("Kotlin nested enum cannot use instance receiver: " + match.group())

def self_test():
    for invalid in ("p.Key.PLAY", "p.Action.DELEGATE", "p.Command.PAUSE"):
        try:
            no_invalid_enum(invalid)
        except AssertionError:
            pass
        else:
            raise AssertionError("Invalid Kotlin accepted: " + invalid)
    for valid in ("AudioRouteTransportPolicy.Key.PLAY",
                  "AudioRouteTransportPolicy.Action.DELEGATE",
                  "WidgetPlaybackIntentPolicy.Command.PAUSE", "p.command(true)"):
        no_invalid_enum(valid)
    print("NW104_ENUM_REFERENCE_SELFTEST_PASS cases=7")

def verify():
    root = ROOT / "app/src/test/java/com/nightwave/player"
    count = 0
    for rel, expected in SUITES.items():
        content = (root / rel).read_text(encoding="utf-8")
        no_invalid_enum(content)
        actual = len(re.findall(r"@Test\b", content))
        assert actual == expected, (rel, actual, expected)
        assert "import org.junit" in content, rel
        count += actual
    audio = (root / "playback/AudioRouteTransportPolicyTest.kt").read_text()
    widget = (root / "widget/WidgetPlaybackIntentPolicyTest.kt").read_text()
    assert "AudioRouteTransportPolicy.Key." in audio
    assert "AudioRouteTransportPolicy.Action." in audio
    assert "WidgetPlaybackIntentPolicy.Command." in widget
    assert count == 53
    print("NW104_REAL_TEST_SOURCE_GUARD_PASS classes=8 methods=53")

if __name__ == "__main__":
    if "--self-test" in sys.argv:
        self_test()
    else:
        verify()
