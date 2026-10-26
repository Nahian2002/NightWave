#!/usr/bin/env python3
"""NW105 offline original-source regression wiring, not Android certification."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
script=(root/"tools/nw105_offline_junit_gate.py").read_text()
adapter=(root/"tools/NW105OfflineRepeatAdapter.kt").read_text()
runner=(root/"tools/NW105OriginalOfflineJUnitRunner.kt").read_text()
ci=(root/".github/workflows/android.yml").read_text()
assert "OFFLINE_JVM_ONLY_PASS" in script
assert "NOT_TESTED_ENUM_ADAPTER_ONLY" in script
assert "NW105_REAL_OFFLINE_JUNIT_SOURCE_PASS cases=14" in runner
assert "method.invoke(instance)" in runner
assert "class PlaybackQueue { enum class Repeat { OFF, ALL, ONE } }" in adapter
assert "NW105 offline production/test files missing" in script
assert "hashes(paths) != before" in script
assert "tools/nw105_offline_junit_gate.py --self-test" in ci
assert "tools/nw105_offline_junit_gate.py --report" in ci
assert "tools/verify_nw105_offline_junit.py" in ci
assert "NW102 compile and run original Kotlin JUnit source tests" in ci
print("NW105_REAL_OFFLINE_TEST_SOURCE_WIRING_PASS checks=11")
