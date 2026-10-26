#!/usr/bin/env python3
"""NW110 read-only, fail-closed final evidence inventory.

Portable Kotlin PASS never certifies a signed Android release. The only
automatic public-release result is NO_GO; manual approval cannot be inferred
from a developer-build artifact or self-reported JSON.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION = (
    "app/src/main/java/com/nightwave/player/playback/ShuffleRecoveryPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt",
    "app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt",
    "app/src/main/java/com/nightwave/player/offline/OfflineGaplessPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/AudioRouteTransportPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/EqualizerSelectionPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/PlaybackResolutionPolicy.kt",
    "app/src/main/java/com/nightwave/player/widget/WidgetPlaybackIntentPolicy.kt",
    "app/src/main/java/com/nightwave/player/playback/MediaSessionQueueControlPolicy.kt",
)
TESTS = (
    "app/src/test/java/com/nightwave/player/playback/AudioRouteTransportPolicyTest.kt",
    "app/src/test/java/com/nightwave/player/playback/EqualizerSelectionPolicyTest.kt",
    "app/src/test/java/com/nightwave/player/offline/OfflineShuffleCycleIntegrityTest.kt",
    "app/src/test/java/com/nightwave/player/playback/PlaybackResolutionPolicyTest.kt",
    "app/src/test/java/com/nightwave/player/widget/WidgetPlaybackIntentPolicyTest.kt",
    "app/src/test/java/com/nightwave/player/playback/MediaSessionQueueControlPolicyTest.kt",
    "app/src/test/java/com/nightwave/player/playback/PlaybackQueueManualNextAvailabilityTest.kt",
    "app/src/test/java/com/nightwave/player/offline/OfflineManualNextAvailabilityTest.kt",
)
INPUTS = PRODUCTION + TESTS
MATRIX = "docs/NW90_EXPANDED_139_CASE_MATRIX.csv"

def policy_receipt_status(root: Path, receipt: Path) -> str:
    """Verify current 17 source SHA-256 values, not just untrusted PASS text."""
    if not receipt.is_file() or receipt.is_symlink():
        return "NOT_VERIFIED"
    try:
        data = json.loads(receipt.read_text(encoding="utf-8"))
        hashes = data.get("source_sha256")
        if (data.get("status") != "POLICY_JVM_TESTS_PASS_ONLY" or
                data.get("executed_tests") != 53 or
                data.get("test_classes") != 8 or
                data.get("source_file_count") != 17 or
                not isinstance(hashes, dict) or set(hashes) != set(INPUTS) or
                data.get("android_compilation") != "NOT_VERIFIED" or
                data.get("official_gradle_junit") != "NOT_RUN"):
            return "NOT_VERIFIED"
        for name in INPUTS:
            path = root / name
            if not path.is_file() or path.is_symlink():
                return "NOT_VERIFIED"
            if hashlib.sha256(path.read_bytes()).hexdigest() != hashes[name]:
                return "NOT_VERIFIED"
    except (OSError, ValueError, TypeError, KeyError):
        return "NOT_VERIFIED"
    return "PASS_POLICY_JVM_ONLY"

def matrix_state(path: Path) -> dict:
    result = {k: 0 for k in ("PASS", "FAIL", "BLOCKED", "NOT_RUN")}
    result["total"] = 0
    result["status"] = "NOT_VERIFIED"
    if not path.is_file():
        return result
    try:
        with path.open(encoding="utf-8", newline="") as stream:
            reader = csv.DictReader(stream, strict=True)
            if reader.fieldnames != ["case_id", "area", "scenario", "expected",
                                     "severity", "status", "evidence"]:
                return result
            rows = list(reader)
        if len(rows) != 139 or {r.get("case_id") for r in rows} != {
                f"NW90-{i:03}" for i in range(1, 140)}:
            return result
        for row in rows:
            status = row.get("status")
            if (status not in result or status == "total" or
                    not row.get("scenario") or not row.get("expected") or
                    row.get("severity") not in ("P0", "P1", "P2") or
                    (status == "PASS" and not row.get("evidence", "").strip())):
                return result
            result[status] += 1
        result["total"] = 139
        result["status"] = "MATRIX_FORMAT_VALID_NOT_DEVICE_ATTESTED"
    except (OSError, UnicodeError, csv.Error, TypeError):
        pass
    return result

def source_config_status(root: Path) -> str:
    try:
        app = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
        plugin = (root / "build.gradle.kts").read_text(encoding="utf-8")
        manifest = (root / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        extraction = (root / "app/src/main/res/xml/nightwave_data_extraction_rules.xml").read_text(encoding="utf-8")
    except OSError:
        return "NOT_VERIFIED"
    required = (
        re.search(r"\bcompileSdk\s*=\s*36\b", app),
        re.search(r"\btargetSdk\s*=\s*36\b", app),
        'applicationId = "com.nightwave.player"' in app,
        'id("com.android.application") version "8.10.1"' in plugin,
        'android:allowBackup="false"' in manifest,
        'android:dataExtractionRules="@xml/nightwave_data_extraction_rules"' in manifest,
        "<cloud-backup>" in extraction,
        "<device-transfer>" in extraction,
    )
    return "CONFIG_SOURCE_PRESENT_UNCOMPILED" if all(required) else "NOT_VERIFIED"

def certify(root: Path, receipt: Path) -> dict:
    policy = policy_receipt_status(root, receipt)
    matrix = matrix_state(root / MATRIX)
    return {
        "milestone": "NW110",
        "release_decision": "NO_GO",
        "public_release_approved": False,
        "policy_jvm_53": policy,
        "source_android36_privacy": source_config_status(root),
        "device_matrix_139": matrix,
        "release_gates": {
            "full_android_build": "NOT_VERIFIED",
            "official_gradle_junit": "NOT_VERIFIED",
            "signed_apk_update_continuity": "NOT_VERIFIED",
            "real_device_playback_and_talkback": "NOT_VERIFIED",
            "real_google_drive_roundtrip": "NOT_VERIFIED",
            "play_store_submission": "NOT_VERIFIED",
            "content_rights_and_youtube_paths": "BLOCKED_REVIEW_REQUIRED",
            "published_privacy_and_data_safety": "NOT_VERIFIED",
            "full_git_history_secret_review": "NOT_VERIFIED",
        },
        "notice": "Source/JVM receipt is not Android, signing, OAuth, rights or device approval.",
    }

def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-nw110-") as directory:
        root = Path(directory)
        for name in INPUTS:
            file = root / name
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("test source: " + name, encoding="utf-8")
        receipt = root / "test.json"
        original = {
            "status": "POLICY_JVM_TESTS_PASS_ONLY", "executed_tests": 53,
            "test_classes": 8, "source_file_count": 17,
            "android_compilation": "NOT_VERIFIED", "official_gradle_junit": "NOT_RUN",
            "source_sha256": {name: hashlib.sha256((root / name).read_bytes()).hexdigest()
                              for name in INPUTS},
        }
        receipt.write_text(json.dumps(original), encoding="utf-8")
        assert policy_receipt_status(root, receipt) == "PASS_POLICY_JVM_ONLY"
        assert certify(root, receipt)["release_decision"] == "NO_GO"
        tests = 2
        for key, bad in (("executed_tests", 52), ("test_classes", 7),
                         ("source_file_count", 16), ("status", "PASS"),
                         ("android_compilation", "PASS")):
            changed = original | {key: bad}
            receipt.write_text(json.dumps(changed), encoding="utf-8")
            assert policy_receipt_status(root, receipt) == "NOT_VERIFIED"
            tests += 1
        receipt.write_text(json.dumps(original), encoding="utf-8")
        path = root / INPUTS[0]
        path.write_text("mutated test source", encoding="utf-8")
        assert policy_receipt_status(root, receipt) == "NOT_VERIFIED"
        tests += 1
        path.unlink()
        assert policy_receipt_status(root, receipt) == "NOT_VERIFIED"
        tests += 1
        receipt.unlink()
        assert policy_receipt_status(root, receipt) == "NOT_VERIFIED"
        tests += 1
        assert tests == 10
    print("NW110_FAIL_CLOSED_CERTIFICATION_SELFTEST_PASS cases=10")

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", type=Path, default=ROOT)
    parser.add_argument("--policy-receipt", type=Path,
                        default=Path("build/nightwave-nw102-jvm-source-tests.json"))
    parser.add_argument("--report", type=Path,
                        default=Path("build/nightwave-nw110-final-certification.json"))
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    root = args.project_root.resolve()
    input_receipt = args.policy_receipt if args.policy_receipt.is_absolute() else root / args.policy_receipt
    target = args.report if args.report.is_absolute() else root / args.report
    result = certify(root, input_receipt)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("NW110_POLICY_JVM:", result["policy_jvm_53"])
    print("NW110_RELEASE_DECISION:", result["release_decision"])
    print("NW110_REPORT:", target)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
