#!/usr/bin/env python3
"""6V6E search/editor and cumulative 6V6A-E integrity source audit.

A static presence/invariant check, not an Android compiler, JUnit execution,
Compose UI test, filesystem export, or successful signed APK.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parents[1]
MAIN = "app/src/main/java/com/nightwave/player/v2/"
UNIT = "app/src/test/java/com/nightwave/player/v2/lyrics/"
ANDROID = "app/src/androidTest/java/com/nightwave/player/v2/ui/"

REQUIRED = {
    MAIN + "lyrics/TimedLyrics.kt": (
        "const val MAX_BYTES = 64 * 1024", "const val MAX_LINES = 1200",
        "fun activeIndex(", "object TimedLyricsCodec",
    ),
    MAIN + "lyrics/LyricsSyncPolicy.kt": (
        "MAX_OFFSET_MS = 10_000", "STEP_MS = 250",
        "fun activeIndex(", "fun seekPosition(",
    ),
    MAIN + "lyrics/LyricsImportReview.kt": (
        "fun inspect(", "TimedLyricsCodec.parse(bytes)", "fun failureHint(",
        "LyricsImportMatch.DIFFERENT",
    ),
    MAIN + "lyrics/PrivateLyricsStore.kt": (
        "nightwave_private_lrc_v1", "fun fingerprint(id: String): String?",
        "fun saveReviewed(", "if (fingerprint(id) != expectedFingerprint) return false",
        "fun exportCloud()", "fun restoreCloudMissing(",
    ),
    MAIN + "lyrics/LyricsEditorPolicy.kt": (
        "data class LyricsEditorDraft(", "MAX_UNDO = 15", "fun updateText(",
        "fun updateTime(", "fun nudge(", "fun add(", "fun delete(",
        "require(parsed.cues == ordered)", "TimedLyricsCodec.parse(bytes)",
    ),
    MAIN + "lyrics/LyricsExportPolicy.kt": (
        "fun suggestedFileName(", "fun prepare(",
        "LyricsEditorPolicy.serialize(current)",
        "require(verified.cues == current.cues)",
    ),
    MAIN + "lyrics/LyricsEditorNavigationPolicy.kt": (
        "fun clamp(", "fun step(", "fun followOrKeep(", "fun afterEdit(",
    ),
    MAIN + "lyrics/LyricsEditorSearchPolicy.kt": (
        "MAX_QUERY_LENGTH = 80",
        "Normalizer.normalize(value, Normalizer.Form.NFC)",
        "fun matches(", "fun nearest(", "fun ordinal(",
        "index.takeIf { found }", "indices.firstOrNull", "indices.lastOrNull",
    ),
    MAIN + "ui/NightWaveLyricsEditorDialog.kt": (
        "fun NightWaveLyricsEditorDialog(", "songId: String",
        "rememberSaveable(songId)", "var searchOpen by rememberSaveable(songId)",
        "if (searchOpen) Surface(", "lyrics-search-panel",
        "lyrics-search-input", "lyrics-search-toggle", "lyrics-search-status",
        "lyrics-match-previous", "lyrics-match-next",
        "LyricsEditorSearchPolicy.MAX_QUERY_LENGTH",
        "LyricsEditorSearchPolicy.matches(draft, effectiveQuery)",
        "LyricsEditorSearchPolicy.nearest(", "val searchHit = index in searchMatchSet",
        "searchHit -> colors.tertiaryContainer",
        "LyricsEditorNavigationPolicy.afterEdit(",
        "lyrics-export-lrc", "lyrics-save-edits", "Discard unsaved lyric edits?",
        "onSave", "onExport", "onDiscard",
    ),
    MAIN + "ui/NightWaveStudioActivity.kt": (
        "songId = session.songId",
        "internal class LyricsEditorRetainedState : ViewModel()",
        "ViewModelProvider(this)[LyricsEditorRetainedState::class.java]",
        'ActivityResultContracts.CreateDocument("text/plain")',
        "pending.requestId != lyricsEditorEpoch",
        "lyricsStore.saveReviewed(", "onExport = { exportLyricsEditorDraft() }",
        "lyricsRevision++",
    ),
    "tools/build-windows.ps1": (
        "check-phase6v6a-integration.ps1",
        "check-phase6v6b-integration.ps1",
        "check-phase6v6c-integration.ps1",
        "check-phase6v6d-integration.ps1",
        "check-phase6v6e-integration.ps1",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
        "verify-apk-signature.ps1",
    ),
    "tools/check-builder-syntax.ps1": ("check-phase6v6e-integration.ps1",),
    ".github/workflows/android.yml": (
        "python3 tools/verify_v2_phase6v6e.py --self-test",
        "python3 tools/verify_v2_phase6v6e.py",
        ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin",
    ),
}
TESTS = {
    UNIT + "LyricsEditorSearchPolicyTest.kt": (
        16, ("caseInsensitiveSearchReturnsOrderedCueIndices",
             "bengaliTextMatchesWithoutTransliteratingLyrics",
             "nextMatchWrapsAfterFinalResult",
             "cueReorderUsesCurrentIndexNotStaleOldPosition",
             "searchesAllAllowedTwelveHundredLines")),
    UNIT + "LyricsSyncPolicyTest.kt": (
        11, ("positiveOffsetDelaysLineHighlight", "negativeOffsetAdvancesHighlight")),
    UNIT + "LyricsImportReviewPolicyTest.kt": (
        14, ("malformedRowsHaveActionableCountsButOtherCuesSurvive",)),
    UNIT + "LyricsEditorPolicyTest.kt": (
        14, ("serializedEditorDraftIsStrictLrcAndIdenticalAfterParse",)),
    UNIT + "LyricsExportPolicyTest.kt": (
        10, ("unsavedDraftExportsWithoutChangingSource",)),
    UNIT + "LyricsEditorNavigationPolicyTest.kt": (
        10, ("reorderKeepsMovedCueSelected",)),
    ANDROID + "LyricsEditorRetainedStateInstrumentedTest.kt": (
        2, ("rotationStyleRecreationPreservesUnsavedDraftOnlyInMemory",)),
    ANDROID + "NightWaveLyricsSearchUiInstrumentedTest.kt": (
        3, ("searchingShowsMatchesWithoutTouchingStoredLyrics",
             "noMatchesDisablePreviousAndNext",
             "clearingSearchHidesMatchResults")),
}


def audit(root: Path) -> list[str]:
    errors = []
    for relative, tokens in REQUIRED.items():
        file = root / relative
        if not file.is_file():
            errors.append("Missing source: " + relative)
            continue
        content = file.read_text(encoding="utf-8")
        for token in tokens:
            if token not in content:
                errors.append(relative + ": missing " + repr(token))
    for relative, (at_least, methods) in TESTS.items():
        file = root / relative
        if not file.is_file():
            errors.append("Missing regression suite: " + relative)
            continue
        content = file.read_text(encoding="utf-8")
        count = content.count("@Test fun ")
        if count < at_least:
            errors.append(relative + f": expected {at_least} test declarations, found {count}")
        for method in methods:
            if "fun " + method + "(" not in content:
                errors.append(relative + ": missing test " + method)
    return errors


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="nightwave-v6e-audit-") as tmp:
        root = Path(tmp)
        for relative, tokens in REQUIRED.items():
            file = root / relative
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(tokens), encoding="utf-8")
        for relative, (n, methods) in TESTS.items():
            file = root / relative
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("\n".join(
                [f"@Test fun test{i}() {{}}" for i in range(n)] +
                [f"fun {m}() {{}}" for m in methods]), encoding="utf-8")
        assert not audit(root), audit(root)
        search = root / (MAIN + "lyrics/LyricsEditorSearchPolicy.kt")
        search.write_text(search.read_text(encoding="utf-8").replace(
            "MAX_QUERY_LENGTH = 80", "MAX_QUERY_LENGTH = 500000"),
            encoding="utf-8")
        assert any("MAX_QUERY_LENGTH = 80" in e for e in audit(root))
        editor = root / (MAIN + "ui/NightWaveLyricsEditorDialog.kt")
        editor.write_text(editor.read_text(encoding="utf-8").replace(
            "LyricsEditorSearchPolicy.nearest(", "unsafe_stale_index_navigation("),
            encoding="utf-8")
        assert any("LyricsEditorSearchPolicy.nearest" in e for e in audit(root))
    print("PASS: cumulative 6V6E audit detects unbounded search and stale navigation")


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
    print("PASS: cumulative 6V6A-E source hooks and regression declarations are present")
    print("NOT VERIFIED: Gradle/JUnit execution, Compose/emulator, Drive, SAF or APK signer")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
