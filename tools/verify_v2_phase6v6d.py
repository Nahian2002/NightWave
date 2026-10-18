#!/usr/bin/env python3
"""Read-only 6V6D lyrics export, accessibility and lifecycle source gate.

A static integration audit, NOT a Kotlin compiler or an Android/SAF test.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
P = "app/src/main/java/com/nightwave/player/v2/"
NEEDED = {
    P + "lyrics/LyricsExportPolicy.kt": (
        "data class PreparedLyricsExport(",
        "object LyricsExportPolicy",
        "fun suggestedFileName(",
        "fun prepare(",
        "LyricsEditorPolicy.serialize(current)",
        "TimedLyricsCodec.parse(bytes)",
        "require(verified.cues == current.cues)",
        "LyricsEditorPolicy.changed(baseline, current)",
    ),
    P + "lyrics/LyricsEditorNavigationPolicy.kt": (
        "object LyricsEditorNavigationPolicy",
        "fun clamp(", "fun step(", "fun follow(",
        "fun followOrKeep(", "fun afterEdit(",
        "change.toLong()", "indexOf(changedCue)",
    ),
    P + "ui/NightWaveLyricsEditorDialog.kt": (
        "onExport: () -> Unit",
        "onClick = onExport",
        "lyrics-export-lrc",
        "Export unsaved draft .lrc",
        "it does not save this draft in NightWave",
        "Modifier.heightIn(min = 48.dp)",
        ".semantics {",
        "stateDescription =",
        "contentDescription =",
        "LyricsEditorNavigationPolicy.step(",
        "LyricsEditorNavigationPolicy.afterEdit(",
        "Discard unsaved lyric edits?",
        "onSave", "onDiscard",
    ),
    P + "ui/NightWaveStudioActivity.kt": (
        "internal class LyricsEditorRetainedState : ViewModel()",
        "ViewModelProvider(this)[LyricsEditorRetainedState::class.java]",
        "var session by mutableStateOf<LyricsEditorSession?>(null)",
        "var pendingExport: PendingLyricsExport? = null",
        "current.isNotBlank()",
        "private val exportLyricsFile = registerForActivityResult(",
        'ActivityResultContracts.CreateDocument("text/plain")',
        "contentResolver.openOutputStream(uri,",
        "pending.requestId != lyricsEditorEpoch",
        'playback.state.videoUrl != "offline:" + pending.songId',
        "private fun exportLyricsEditorDraft()",
        "LyricsExportPolicy.prepare(",
        "exportLyricsFile.launch(prepared.suggestedName)",
        "onExport = { exportLyricsEditorDraft() }",
        "lyricsStore.saveReviewed(",
    ),
    "app/build.gradle.kts": ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7",),
    "tools/build-windows.ps1": (
        "check-phase6v6c-integration.ps1",
        "check-phase6v6d-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": ("check-phase6v6d-integration.ps1",),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v6d.py --self-test",
        "python3 tools/verify_v2_phase6v6d.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}
TESTS = {
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsExportPolicyTest.kt":
        (10, ("unsavedDraftExportsWithoutChangingSource",
              "unsafeManuallyConstructedDraftCannotBeExported",
              "timeCorrectionsArePreservedExactlyInExport")),
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorNavigationPolicyTest.kt":
        (10, ("hugeNavigationStepsDoNotOverflow",
              "reorderKeepsMovedCueSelected")),
    "app/src/androidTest/java/com/nightwave/player/v2/ui/LyricsEditorRetainedStateInstrumentedTest.kt":
        (2, ("rotationStyleRecreationPreservesUnsavedDraftOnlyInMemory",
             "explicitDiscardClearsOnlyRetainedDraftNotBaseline")),
    "app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorPolicyTest.kt":
        (14, ("serializedEditorDraftIsStrictLrcAndIdenticalAfterParse",)),
}


def audit(root: Path) -> list[str]:
    errors: list[str] = []
    for rel, tokens in NEEDED.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing: " + rel)
            continue
        s = file.read_text(encoding="utf-8")
        for token in tokens:
            if token not in s:
                errors.append(rel + ": missing " + repr(token))
    for rel, (minimum, methods) in TESTS.items():
        file = root / rel
        if not file.is_file():
            errors.append("Missing regression file " + rel)
            continue
        s = file.read_text(encoding="utf-8")
        if s.count("@Test fun ") < minimum:
            errors.append(rel + f": less than {minimum} regressions")
        for name in methods:
            if f"fun {name}(" not in s:
                errors.append(rel + ": missing " + name)
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-6v6d-gate-") as tmp:
        root = Path(tmp)
        for rel, tokens in NEEDED.items():
            file = root / rel
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(tokens), encoding="utf-8")
        for rel, (minimum, methods) in TESTS.items():
            file = root / rel
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(
                [f"@Test fun test{i}() {{}}" for i in range(minimum)] +
                [f"fun {name}() {{}}" for name in methods]),
                encoding="utf-8")
        assert not audit(root), audit(root)
        policy = root / (P + "lyrics/LyricsExportPolicy.kt")
        policy.write_text(policy.read_text(encoding="utf-8").replace(
            "require(verified.cues == current.cues)",
            "unchecked_export()"), encoding="utf-8")
        assert any("verified.cues" in issue for issue in audit(root))
        activity = root / (P + "ui/NightWaveStudioActivity.kt")
        activity.write_text(activity.read_text(encoding="utf-8").replace(
            "pending.requestId != lyricsEditorEpoch",
            "unscoped_export_session()"), encoding="utf-8")
        assert any("pending.requestId" in issue for issue in audit(root))
    print("PASS: synthetic 6V6D gate rejects unverified and stale LRC export")


def main() -> int:
    cli = argparse.ArgumentParser()
    cli.add_argument("--root", type=Path, default=ROOT)
    cli.add_argument("--self-test", action="store_true")
    args = cli.parse_args()
    if args.self_test:
        self_test()
        return 0
    problems = audit(args.root)
    for problem in problems:
        print("FAIL:", problem)
    if problems:
        return 1
    print("PASS: 6V6D exported LRC, accessible navigation and retained editor sources wired")
    print("UNVERIFIED: Kotlin/JUnit, Compose, SAF provider, emulator, signed APK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
