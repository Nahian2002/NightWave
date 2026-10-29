#!/usr/bin/env python3
"""6V6C read-only source and regression-discovery gate.

Source checks are NOT the Gradle compiler, executed JUnit, Compose runtime,
an emulator, or a signed APK.
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
    P + "lyrics/LyricsEditorPolicy.kt": (
        "data class LyricsEditorDraft(", "object LyricsEditorPolicy",
        "MAX_TIME_MS = 99L * 60_000L + 59_999L",
        "MAX_UNDO = 15", "NUDGE_MS = 250L",
        "fun begin(", "fun updateText(", "fun updateTime(",
        "fun nudge(", "fun add(", "fun delete(", "fun serialize(",
        "fun changed(", "TimedLyricsCodec.parse(bytes)",
        "require(parsed.cues == ordered)",
        "require(bytes.size in 1..TimedLyricsCodec.MAX_BYTES)",
    ),
    P + "lyrics/PrivateLyricsStore.kt": (
        'nightwave_private_lrc_v1',
        "fun saveReviewed(id: String, bytes: ByteArray, expectedFingerprint: String?): Boolean",
        "if (fingerprint(id) != expectedFingerprint) return false",
        "save(id, bytes)", "fun restoreCloudMissing(",
    ),
    P + "ui/NightWaveLyricsEditorDialog.kt": (
        "fun NightWaveLyricsEditorDialog(",
        'testTag("nightwave-lrc-editor")',
        "LazyColumn(", "rememberLazyListState()",
        "LyricsEditorPolicy.updateText(",
        "LyricsEditorPolicy.updateTime(",
        "LyricsEditorPolicy.nudge(",
        "LyricsEditorPolicy.add(",
        "LyricsEditorPolicy.delete(",
        "onApply(", "onUndo", "onSave", "onDiscard",
        "lyrics-save-edits",
        "Discard unsaved lyric edits?",
        "No pending edits",
        "Playing", "Previous", "Next",
    ),
    P + "ui/StudioPlayerScreens.kt": (
        "onEditLyrics: () -> Unit = {}",
        "Edit lyric lines & timestamps",
        "onClick = onEditLyrics",
        "Open full-screen synchronized lyrics",
        "LyricsSyncPolicy.seekPosition(",
    ),
    P + "ui/NightWaveStudioActivity.kt": (
        "internal data class LyricsEditorSession(",
        "internal class LyricsEditorRetainedState : ViewModel()",
        "var session by mutableStateOf<LyricsEditorSession?>(null)",
        "private var lyricsEditorSession: LyricsEditorSession?",
        "get() = lyricsEditorRetained.session",
        "set(value) { lyricsEditorRetained.session = value }",
        "fun openLyricsEditor()",
        "LyricsEditorPolicy.begin(imported)",
        "fun onLyricsEditorDraft(",
        "fun undoLyricsEditorDraft()",
        "fun discardLyricsEditorDraft()",
        "fun saveLyricsEditorDraft()",
        "LyricsEditorPolicy.serialize(editor.draft)",
        "boundLyrics.saveReviewed(",
        "editor.expectedFingerprint",
        "editor.requestId == lyricsEditorEpoch",
        "onEditLyrics = { openLyricsEditor() }",
        "NightWaveLyricsEditorDialog(",
        "onUndo = { undoLyricsEditorDraft() }",
        "onSave = { saveLyricsEditorDraft() }",
        "onDiscard = { discardLyricsEditorDraft() }",
        "onEditLyrics = onEditLyrics",
    ),
    "tools/build-windows.ps1": (
        "check-phase6v6c-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": ("check-phase6v6c-integration.ps1",),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v6c.py --self-test",
        "python3 tools/verify_v2_phase6v6c.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}
TESTS = {
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorPolicyTest.kt": (
        14, ("openingEditorDoesNotMutateOriginalLyrics",
             "nudgeTimestampInQuarterSecondIncrements",
             "changingTimeReordersCueSafely",
             "serializedEditorDraftIsStrictLrcAndIdenticalAfterParse",
             "serializedLrcByteLimitRemainsMandatory",
             "bracketedTimestampsInsideLyricsCannotCreateHiddenExtraCues")),
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsImportReviewPolicyTest.kt": (
        14, ("conflictingExplicitTitleTagWarnsEvenIfFilenameLooksMatching",)),
    "app/src/test/java/com/nightwave/player/v2/lyrics/TimedLyricsCodecTest.kt": (
        10, ("sizeBoundPreventsUntrustedLargeFiles",)),
}


def audit(root: Path) -> list[str]:
    errors: list[str] = []
    for relative, terms in REQUIRED.items():
        file = root / relative
        if not file.is_file():
            errors.append("Missing: " + relative)
            continue
        text = file.read_text(encoding="utf-8")
        for token in terms:
            if token not in text:
                errors.append(relative + ": missing source contract " + repr(token))
    for relative, (minimum, cases) in TESTS.items():
        file = root / relative
        if not file.is_file():
            errors.append("Missing test: " + relative)
            continue
        text = file.read_text(encoding="utf-8")
        if text.count("@Test fun ") < minimum:
            errors.append(relative + f": fewer than {minimum} tests")
        for case in cases:
            if f"fun {case}(" not in text:
                errors.append(relative + ": missing " + case)
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-v6c-gate-") as tmp:
        root = Path(tmp)
        for relative, terms in REQUIRED.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("\n".join(terms), encoding="utf-8")
        for relative, (count, cases) in TESTS.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("\n".join(
                [f"@Test fun case{i}() {{}}" for i in range(count)] +
                [f"fun {name}() {{}}" for name in cases]), encoding="utf-8")
        assert not audit(root), audit(root)
        target = root / (P + "ui/NightWaveStudioActivity.kt")
        target.write_text(target.read_text(encoding="utf-8").replace(
            "boundLyrics.saveReviewed(", "unsafe_overwrite_without_review("),
            encoding="utf-8")
        assert any("saveReviewed" in message for message in audit(root))
        target = root / (P + "lyrics/LyricsEditorPolicy.kt")
        target.write_text(target.read_text(encoding="utf-8").replace(
            "require(parsed.cues == ordered)", "silently_truncate_lyrics()"),
            encoding="utf-8")
        assert any("parsed.cues" in message for message in audit(root))
    print("PASS: synthetic 6V6C source gate rejects unreviewed saves and cue truncation")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    problems = audit(args.root)
    for problem in problems:
        print("FAIL: " + problem)
    if problems:
        return 1
    print("PASS: 6V6C in-memory editor, explicit safe save and regression sources wired")
    print("UNVERIFIED: Kotlin/JUnit, Compose rendering, device testing, APK signer")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
