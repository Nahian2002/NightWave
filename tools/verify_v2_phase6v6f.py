#!/usr/bin/env python3
"""6V6F build-stabilization and privacy-safe lyrics QA source contract.

Read-only verification: this checks wiring, not real Android compilation.
Real Gradle/JVM/Compose/APK and Google Drive tests are separate mandatory gates.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
MAIN = "app/src/main/java/com/nightwave/player/v2/"
UNIT = "app/src/test/java/com/nightwave/player/v2/lyrics/"
ANDROID = "app/src/androidTest/java/com/nightwave/player/v2/ui/"
EXPECTED = {
    MAIN + "lyrics/TimedLyrics.kt": (
        "const val MAX_BYTES = 64 * 1024",
        "const val MAX_LINES = 1200",
        "object TimedLyricsCodec",
    ),
    MAIN + "lyrics/LyricsSyncPolicy.kt": (
        "MAX_OFFSET_MS = 10_000", "STEP_MS = 250",
        "fun seekPosition(", "fun activeIndex(",
    ),
    MAIN + "lyrics/PrivateLyricsStore.kt": (
        "nightwave_private_lrc_v1",
        "fun saveReviewed(",
        "if (fingerprint(id) != expectedFingerprint) return false",
        "fun restoreCloudMissing(",
    ),
    MAIN + "lyrics/LyricsEditorPolicy.kt": (
        "fun serialize(", "require(parsed.cues == ordered)",
        "MAX_UNDO = 15", "TimedLyricsCodec.parse(bytes)",
    ),
    MAIN + "lyrics/LyricsExportPolicy.kt": (
        "fun prepare(",
        "LyricsEditorPolicy.serialize(current)",
        "require(verified.cues == current.cues)",
    ),
    MAIN + "lyrics/LyricsEditorSearchPolicy.kt": (
        "MAX_QUERY_LENGTH = 80", "fun matches(",
        "fun nearest(", "fun ordinal(", "Normalizer.Form.NFC",
    ),
    MAIN + "ui/NightWaveLyricsEditorDialog.kt": (
        "lyrics-search-toggle", "lyrics-search-input",
        "lyrics-match-previous", "lyrics-match-next",
        "lyrics-export-lrc", "lyrics-save-edits",
        "onDiscard", "songId: String",
    ),
    MAIN + "ui/NightWaveStudioActivity.kt": (
        "internal class LyricsEditorRetainedState : ViewModel()",
        "lyricsStore.saveReviewed(", "pending.requestId != lyricsEditorEpoch",
        "exportLyricsFile.launch(prepared.suggestedName)",
    ),
    "tools/triage_github_actions.py": (
        "def classify(payload: dict)", "RUNNER_UNASSIGNED",
        "NOT_ATTEMPTED", "def self_test()", "runner_id",
        "runner = job.get(",
    ),
    "tools/smoke-lyrics-kotlin.ps1": (
        "param([switch]$RequireCompiler)",
        "TimedLyrics.kt", "LyricsSyncPolicy.kt",
        "LyricsImportReview.kt", "LyricsEditorSearchPolicy.kt",
        "LyricsEditorPolicy.kt", "LyricsExportPolicy.kt",
        "LyricsEditorNavigationPolicy.kt",
        "Get-Command 'kotlinc'", "NOT VERIFIED: Android Compose/Gradle",
    ),
    "tools/check-builder-syntax.ps1": (
        "check-phase6v6f-integration.ps1",
        "smoke-lyrics-kotlin.ps1",
    ),
    "tools/build-windows.ps1": (
        "check-phase6v6f-integration.ps1",
        "testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
        "NightWave-build-status.txt",
    ),
    ".github/workflows/android.yml": (
        "python3 tools/triage_github_actions.py --self-test",
        "python3 tools/verify_v2_phase6v6f.py --self-test",
        "python3 tools/verify_v2_phase6v6f.py",
        "gradle --no-daemon",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}
TEST_FILES = {
    UNIT + "LyricsEditorSearchPolicyTest.kt": 16,
    UNIT + "LyricsEditorPolicyTest.kt": 14,
    UNIT + "LyricsSyncPolicyTest.kt": 11,
    UNIT + "LyricsImportReviewPolicyTest.kt": 14,
    UNIT + "LyricsExportPolicyTest.kt": 10,
    UNIT + "LyricsEditorNavigationPolicyTest.kt": 10,
    ANDROID + "NightWaveLyricsSearchUiInstrumentedTest.kt": 3,
    ANDROID + "LyricsEditorRetainedStateInstrumentedTest.kt": 2,
}


def audit(root: Path) -> list[str]:
    errors = []
    for relative, terms in EXPECTED.items():
        file = root / relative
        if not file.is_file():
            errors.append("Missing: " + relative)
            continue
        source = file.read_text(encoding="utf-8")
        for term in terms:
            if term not in source:
                errors.append(relative + ": absent " + repr(term))
    for relative, minimum in TEST_FILES.items():
        file = root / relative
        if not file.is_file():
            errors.append("Missing regression file: " + relative)
            continue
        count = file.read_text(encoding="utf-8").count("@Test fun ")
        if count < minimum:
            errors.append(relative + ": only " + str(count) + " tests; need " +
                          str(minimum))
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nw-6v6f-contract-") as temp:
        root = Path(temp)
        for relative, values in EXPECTED.items():
            f = root / relative
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("\n".join(values), encoding="utf-8")
        for relative, minimum in TEST_FILES.items():
            f = root / relative
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text("@Test fun regression() {}\n" * minimum, encoding="utf-8")
        assert not audit(root), audit(root)
        file = root / "tools/triage_github_actions.py"
        file.write_text(file.read_text(encoding="utf-8").replace(
            "NOT_ATTEMPTED", "UNVERIFIED_COMPILE"), encoding="utf-8")
        assert any("NOT_ATTEMPTED" in e for e in audit(root))
        file = root / (MAIN + "lyrics/PrivateLyricsStore.kt")
        file.write_text(file.read_text(encoding="utf-8").replace(
            "if (fingerprint(id) != expectedFingerprint) return false",
            "unsafe_write()"), encoding="utf-8")
        assert any("fingerprint(id)" in e for e in audit(root))
    print("PASS: 6V6F synthetic guard finds fake CI compile claims and unsafe LRC writes")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", type=Path, default=ROOT)
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return 0
    failures = audit(args.root)
    for issue in failures:
        print("FAIL:", issue)
    if failures:
        return 1
    print("PASS: 6V6F build diagnostics, offline Kotlin smoke, lyrics source gates wired")
    print("NOT VERIFIED: Android Gradle/JUnit, Compose, Google Drive, signer or on-device upgrade")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
