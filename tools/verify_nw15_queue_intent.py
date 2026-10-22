#!/usr/bin/env python3
"""NW15 queue Intent source contracts, NOT JUnit or Android compilation."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def read(path: str) -> str:
    return (root / path).read_text(encoding="utf-8")

service = read("app/src/main/java/com/nightwave/player/playback/PlaybackService.kt")
policy = read("app/src/main/java/com/nightwave/player/playback/PublicQueueIntentPolicy.kt")
tests = read("app/src/test/java/com/nightwave/player/playback/PublicQueueIntentPolicyTest.kt")
restore = read("app/src/main/java/com/nightwave/player/playback/QueueRestorePolicy.kt")
workflow = read(".github/workflows/android.yml")

assert "QueueRestorePolicy.isCanonicalPublicWatchUrl(url)" in service
assert "PublicQueueIntentPolicy.retainedSelectionIndex(" in service
assert "urls.take(100), originalSelection, entries)" in service
assert "?: return result" in service
assert "PublicQueueSelectionPolicy.selectedIndex(entries, proposedQueue, selected)" in service
assert "queue.replace(entries, selected)" in service
assert "if (queue.add(QueueEntry(url," in service
assert "QueueRestorePolicy.isCanonicalPublicWatchUrl(requested)" in policy
assert "submittedUrls.getOrNull(originalIndex)" in policy
assert "validatedEntries.indexOfFirst { it.url == requested }" in policy
assert "fun isCanonicalPublicWatchUrl(url: String)" in restore
assert tests.count("@Test") == 6
for test in (
    "invalidRowBeforeSelectionDoesNotShiftToDifferentSong",
    "duplicatedRawRowsStillSelectTheIntendedIdentity",
    "invalidSelectedUrlCannotPlayUnrelatedValidEntry",
    "outOfBoundsSelectionIsRejectedInsteadOfClampedToWrongSong",
    "selectedRowDroppedByLimitIsNotSubstituted",
    "canonicalSelectionIsUnchanged"
):
    assert test in tests
assert "fix/nightwave-v2-nw15-queue-intent-identity" in workflow
assert "python3 tools/verify_nw15_queue_intent.py" in workflow
for name in ("nw01_recovery", "nw02_sound_studio", "nw03_account_hub",
             "nw04_premium_player", "nw05_integrated_stability",
             "nw06_discovery_search", "nw07_release_gate", "nw08_attestation",
             "nw09_device_reliability", "nw11_junit_source", "nw12_integration",
             "nw13_queue_navigation", "nw14_queue_window"):
    assert f"python3 tools/verify_{name}.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
for name, content in [("service", service), ("policy", policy)]:
    imports = [line for line in content.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
    assert content.count("{") == content.count("}"), name + " unbalanced braces"
print("PASS: NW15 selected canonical watch URL is preserved through service Intent filtering")
print("UNVERIFIED: full Gradle compilation, JVM tests and Android device behavior")
