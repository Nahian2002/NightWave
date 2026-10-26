#!/usr/bin/env python3
"""NW90 targeted regression source audit: no runtime/device claims."""
from pathlib import Path
import csv
ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

offline = read("app/src/main/java/com/nightwave/player/offline/OfflineLibraryActivity.kt")
service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
cert = read("tools/nw90_certification.py")
ci = read(".github/workflows/android.yml")
runner = read(".github/workflows/nightwave-runner-canary.yml")

# No synchronous full-disk scan from onResume, search/filter redraw, import
# result, or delete result. Only a worker may call healthReport in this UI.
resume = offline.split("override fun onResume()", 1)[1].split(
    "private fun requestHealthCheck(", 1)[0]
redraw = offline.split("private fun refresh()", 1)[1].split(
    "private fun", 1)[0]
scan = offline.split("private fun requestHealthCheck(", 1)[1].split(
    "private fun refresh()", 1)[0]
assert "requestHealthCheck()" in resume
assert "refresh()" not in resume
assert "library.healthReport()" not in redraw
assert "worker.execute {" in scan
assert "runCatching { library.healthReport() }" in scan
assert "if (healthCheckPending) return" in scan
assert "generation != healthCheckGeneration" in scan
assert "cachedReport = result.getOrNull()" in scan
assert "cachedReport?.indexReadable == true" in scan

# Manual EQ readback must reflect an actually complete current-session curve.
apply = service.split("private fun applyEqualizer(", 1)[1].split(
    "private var handledEndGeneration", 1)[0]
assert "manualLevels.size != bandCount" in apply
assert "(0 until bandCount).any { it !in manualLevels }" in apply
assert "candidate.getBandLevel(band.toShort()).toInt()" in apply
assert "EqualizerReadbackPolicy.Result.MISMATCH" in apply
legacy = service.split("ACTION_EQ_BAND_SET -> {", 1)[1].split(
    "ACTION_NORMALIZATION_SET -> {", 1)[0]
assert "caps.levelsMb.size == caps.centersHz.size" in legacy
assert "caps.levelsMb.forEachIndexed" in legacy
assert "if (manualLevels.size != caps.centersHz.size)" in legacy

# Fresh/current JVM proof is not granted by old XML counters alone.
assert 'parser.add_argument("--build-log")' in cert
assert 'if xml_gate["status"] == "PASS" and not log_ok:' in cert
assert '"NOT_VERIFIED"' in cert
assert "BUILD SUCCESSFUL" in cert
assert "--build-log build/nightwave-gradle.log" in ci
assert "feature/nightwave-nw90-final-integration-certification" in ci
assert "feature/nightwave-nw90-final-integration-certification" in runner
assert "nw90_certification.py --self-test" in ci

with (ROOT / "docs/NW90_EXPANDED_139_CASE_MATRIX.csv").open(
    encoding="utf-8", newline=""
) as fh:
    cases = list(csv.DictReader(fh))
assert len(cases) == 139
assert {c["case_id"] for c in cases} == {
    f"NW90-{n:03}" for n in range(1, 140)
}
assert all(c["status"] in ("NOT_RUN", "PASS", "FAIL", "BLOCKED") for c in cases)
print("NW90_TARGETED_FINAL_AUDIT_SOURCE_CONTRACT_PASS")
print("NOT A SUBSTITUTE FOR ANDROID BUILD, JUNIT, PLAYBACK OR DEVICE QA")
