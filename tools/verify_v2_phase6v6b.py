#!/usr/bin/env python3
"""NightWave 6V6B imported LRC review source and regression-discovery gate.

A string contract audit, NOT JVM test execution, an Android build, an LRC
catalog, or proof that arbitrary user-supplied lyrics belong to a song.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
P = "app/src/main/java/com/nightwave/player/v2/"
REQUIRED = {
    P + "lyrics/TimedLyrics.kt": (
        "object TimedLyricsCodec", "const val MAX_BYTES = 64 * 1024",
        "const val MAX_LINES = 1200", "require(cues.isNotEmpty())",
    ),
    P + "lyrics/LyricsImportReview.kt": (
        "object LyricsImportReviewPolicy", "LyricsImportMatch",
        "fun inspect(", "TimedLyricsCodec.parse(bytes)",
        "fun compare(", "declaredTitle.find(text)", "fun failureHint(",
        "ignoredTimestampRows", "repeatedCues", "outOfOrderCues",
        "trackDurationMs + 5_000L", "Filename/title matching is advisory.",
    ),
    P + "lyrics/PrivateLyricsStore.kt": (
        "nightwave_private_lrc_v1", "fun fingerprint(id: String): String?",
        "MessageDigest.getInstance(\"SHA-256\")",
        "fun saveReviewed(id: String, bytes: ByteArray, expectedFingerprint: String?): Boolean",
        "if (fingerprint(id) != expectedFingerprint) return false",
        "save(id, bytes)", "fun exportCloud(): List<CloudLyricEntry>",
        "fun restoreCloudMissing(entries: List<CloudLyricEntry>)",
    ),
    P + "ui/NightWaveStudioActivity.kt": (
        "private data class PendingLyricsReview(",
        "LyricsImportReviewPolicy.inspect(",
        "lyricsStore.fingerprint(id)", "showLyricsImportReview(",
        "setPositiveButton(if (replacing) \"Replace LRC\" else \"Import LRC\")",
        "lyricsStore.saveReviewed(", "lyricsRevision++",
        "requestEpoch != lyricsImportEpoch",
        'playback.state.videoUrl != "offline:$id"',
        "LyricsImportReviewPolicy.failureHint(error)",
        "contentResolver.openInputStream(uri)",
        "TimedLyricsCodec.MAX_BYTES",
    ),
    P + "ui/StudioPlayerScreens.kt": (
        "var lyricsGuideOpen by remember(state.videoUrl)",
        "How to import synchronized lyrics",
        "[00:12.50] First lyric line",
        "Matching filenames do not prove matching lyrics.",
        "Open full-screen synchronized lyrics",
        "LyricsSyncPolicy.seekPosition(",
    ),
    "tools/build-windows.ps1": (
        "check-phase6v6b-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": ("check-phase6v6b-integration.ps1",),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v6b.py --self-test",
        "python3 tools/verify_v2_phase6v6b.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}
TESTS = {
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsImportReviewPolicyTest.kt": (
        12, ("matchingFilenameOnlyAdvisesAndPreservesOriginalCues",
            "conflictingExplicitTitleTagWarnsEvenIfFilenameLooksMatching",
            "malformedRowsHaveActionableCountsButOtherCuesSurvive",
            "duplicateAndOutOfOrderCuesReportedWithoutChangingOriginalParser",
            "importedLrcParserStillRejectsMalformedUtf8AndOversizedFiles",
            "rejectedFilesGetSpecificSafeErrorsWithoutRawProviderDetails")),
    "app/src/test/java/com/nightwave/player/v2/lyrics/TimedLyricsCodecTest.kt":
        (10, ("badUtf8IsRejected", "cueCountFailsClosed")),
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicyTest.kt":
        (11, ("positiveOffsetDelaysLineHighlight", "invalidOffsetsAreRejectedBeforeUse")),
}


def audit(root: Path) -> list[str]:
    errors = []
    for rel, terms in REQUIRED.items():
        path = root / rel
        if not path.is_file():
            errors.append("Missing: " + rel)
            continue
        body = path.read_text(encoding="utf-8")
        for term in terms:
            if term not in body:
                errors.append(rel + ": missing " + repr(term))
    for rel, (minimum, methods) in TESTS.items():
        path = root / rel
        if not path.is_file():
            errors.append("Missing tests: " + rel)
            continue
        body = path.read_text(encoding="utf-8")
        if body.count("@Test fun ") < minimum:
            errors.append(rel + f": fewer than {minimum} regression tests")
        for method in methods:
            if "fun " + method + "(" not in body:
                errors.append(rel + ": missing " + method)
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-v6b-gate-") as tmp:
        root = Path(tmp)
        for rel, tokens in REQUIRED.items():
            p = root / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("\n".join(tokens), encoding="utf-8")
        for rel, (min_count, methods) in TESTS.items():
            p = root / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text("\n".join(
                [f"@Test fun case{i}() {{}}" for i in range(min_count)] +
                [f"fun {name}() {{}}" for name in methods]), encoding="utf-8")
        assert not audit(root), audit(root)
        store = root / (P + "lyrics/PrivateLyricsStore.kt")
        store.write_text(store.read_text(encoding="utf-8").replace(
            "if (fingerprint(id) != expectedFingerprint) return false",
            "unsafe_overwrite()"), encoding="utf-8")
        assert any("fingerprint(id)" in e for e in audit(root))
        ui = root / (P + "ui/NightWaveStudioActivity.kt")
        ui.write_text(ui.read_text(encoding="utf-8").replace(
            "lyricsStore.saveReviewed(", "lyricsStore.save("), encoding="utf-8")
        assert any("saveReviewed" in e for e in audit(root))
    print("PASS: synthetic 6V6B gate rejects unsafe replacement and missing reviewed import")


def main() -> int:
    arg = argparse.ArgumentParser()
    arg.add_argument("--root", type=Path, default=ROOT)
    arg.add_argument("--self-test", action="store_true")
    args = arg.parse_args()
    if args.self_test:
        self_test()
        return 0
    issues = audit(args.root)
    for item in issues:
        print("FAIL: " + item)
    if issues:
        return 1
    print("PASS: 6V6B LRC preview, existing-text guard and regression files wired")
    print("UNVERIFIED: Kotlin compile, JVM test execution, Android UI and signed APK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
