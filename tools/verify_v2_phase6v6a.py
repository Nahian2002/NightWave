#!/usr/bin/env python3
"""Phase 6V6A static lyric integration contract and regression discovery.

Only checks source wiring. Not a Kotlin compiler, Android instrumentation,
licensed lyric source, Google Drive operation, or APK release approval.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
P = "app/src/main/java/com/nightwave/player/v2/"
REQ = {
    P + "lyrics/TimedLyrics.kt": [
        "fun activeIndex(", "object TimedLyricsCodec",
        "const val MAX_BYTES = 64 * 1024",
        "const val MAX_LINES = 1200",
        "require(cues.isNotEmpty())",
    ],
    P + "lyrics/PrivateLyricsStore.kt": [
        'nightwave_private_lrc_v1',
        "fun save(id: String, bytes: ByteArray)",
        "TimedLyricsCodec.parse(bytes)",
        "fun exportCloud(): List<CloudLyricEntry>",
        "fun restoreCloudMissing(entries: List<CloudLyricEntry>)",
    ],
    P + "lyrics/LyricsSyncPolicy.kt": [
        "object LyricsSyncPolicy", "MAX_OFFSET_MS = 10_000",
        "STEP_MS = 250", "fun validOffset(",
        "fun activeIndex(", "fun seekPosition(", "fun adjust(", "fun label(",
        "lyrics.activeIndex(", "candidate.coerceAtMost(durationMs)",
    ],
    P + "lyrics/PrivateLyricsOffsetStore.kt": [
        "class PrivateLyricsOffsetStore", "nightwave_private_lrc_offset_v1",
        "fun read(id: String): Int", "fun save(id: String, offsetMs: Int): Boolean",
        "offline.find(id) != null", "LyricsSyncPolicy.validOffset",
        "prefs.edit()", "edit.commit()",
    ],
    P + "ui/StudioPlayerScreens.kt": [
        "fun StudioNowPlayingOverlay(",
        "lyricsOffsetMs: Int = 0",
        "onLyricsOffsetChange: (Int) -> Unit",
        "var lyricsImmersive by remember(state.videoUrl)",
        "rememberLazyListState()", "LaunchedEffect(activeCue",
        "LyricsSyncPolicy.activeIndex(",
        "LyricsSyncPolicy.seekPosition(",
        "actions.seekTo(",
        "onLyricsOffsetChange(", "Open full-screen synchronized lyrics",
        "Your imported LRC", "if (reduceMotion) lyricListState.scrollToItem",
        "onImportLyrics", "onRemoveLyrics",
    ],
    P + "ui/NightWaveStudioActivity.kt": [
        "lyricsOffsetStore = PrivateLyricsOffsetStore(this)",
        "currentLyricsOffsetMs = 0",
        "lyricsOffsetStore.read(id)",
        "lyricsOffsetStore.save(id, desiredOffset)",
        "playback.state.videoUrl == current",
        "loadedLyrics?.first == id",
        "lyricsOffsetMs = currentLyricsOffsetMs",
        "onLyricsOffsetChange = { desiredOffset ->",
        "lyricsOffsetMs = lyricsOffsetMs",
        "onLyricsOffsetChange = onLyricsOffsetChange",
    ],
    "tools/build-windows.ps1": [
        "check-phase6v6a-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ],
    "tools/check-builder-syntax.ps1": [
        "check-phase6v6a-integration.ps1",
    ],
    ".github/workflows/android.yml": [
        "python3 tools/verify_v2_phase6v6a.py --self-test",
        "python3 tools/verify_v2_phase6v6a.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ],
}
TESTS = {
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicyTest.kt":
        (10, ["positiveOffsetDelaysLineHighlight",
              "negativeOffsetAdvancesHighlight",
              "tappingCueSeeksWithOffsetAndClampsToDuration",
              "hugeStepArgumentDoesNotOverflow",
              "invalidOffsetsAreRejectedBeforeUse"]),
    "app/src/test/java/com/nightwave/player/v2/lyrics/TimedLyricsCodecTest.kt":
        (10, ["badUtf8IsRejected", "duplicateCuesAreRemoved",
              "sizeBoundPreventsUntrustedLargeFiles"]),
}


def audit(root: Path) -> list[str]:
    errors = []
    for rel, tokens in REQ.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing: " + rel)
            continue
        s = file.read_text(encoding="utf-8")
        for token in tokens:
            if token not in s:
                errors.append(rel + ": missing " + repr(token))
    for rel, (minimum, cases) in TESTS.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing tests: " + rel)
            continue
        s = file.read_text(encoding="utf-8")
        if s.count("@Test fun ") < minimum:
            errors.append(rel + f": fewer than {minimum} tests")
        for case in cases:
            if "fun " + case + "(" not in s:
                errors.append(rel + ": missing " + case)
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-v6a-gate-") as tmp:
        root = Path(tmp)
        for rel, terms in REQ.items():
            file = root / rel
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(terms), encoding="utf-8")
        for rel, (minimum, cases) in TESTS.items():
            file = root / rel
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(
                [f"@Test fun example{i}() {{}}" for i in range(minimum)] +
                [f"fun {case}() {{}}" for case in cases]), encoding="utf-8")
        assert not audit(root), audit(root)
        target = root / (P + "ui/StudioPlayerScreens.kt")
        target.write_text(target.read_text(encoding="utf-8").replace(
            "LyricsSyncPolicy.seekPosition(", "unsafe_seek_old_index("),
            encoding="utf-8")
        assert any("LyricsSyncPolicy.seekPosition" in e for e in audit(root))
        target = root / (P + "lyrics/PrivateLyricsOffsetStore.kt")
        target.write_text(target.read_text(encoding="utf-8").replace(
            "offline.find(id) != null", "true"), encoding="utf-8")
        assert any("offline.find(id)" in e for e in audit(root))
    print("PASS: 6V6A synthetic checker rejects unsafe seek and unbound lyric offsets")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    issues = audit(args.root)
    for issue in issues:
        print("FAIL: " + issue)
    if issues:
        return 1
    print("PASS: 6V6A imported LRC sync, immutable offset and UI hooks present")
    print("UNVERIFIED: Gradle, UI rendering, Media3 seek, Google OAuth, signed APK")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
