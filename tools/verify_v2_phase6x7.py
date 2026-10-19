#!/usr/bin/env python3
"""Phase 6X7 synchronized-lyrics UX integrity gate. Static source checks only."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = "app/src/main/java/com/nightwave/player/v2/"
UNIT = "app/src/test/java/com/nightwave/player/v2/lyrics/"

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

sync = read(MAIN + "lyrics/LyricsSyncPolicy.kt")
progress = read(MAIN + "lyrics/LyricsPresentationPolicy.kt")
editor = read(MAIN + "lyrics/LyricsEditorPolicy.kt")
player = read(MAIN + "ui/StudioPlayerScreens.kt")
editor_ui = read(MAIN + "ui/NightWaveLyricsEditorDialog.kt")
activity = read(MAIN + "ui/NightWaveStudioActivity.kt")
offset_store = read(MAIN + "lyrics/PrivateLyricsOffsetStore.kt")
original_lyrics = read(MAIN + "lyrics/PrivateLyricsStore.kt")
tests = read(UNIT + "LyricsPresentationPolicyTest.kt")
timing_tests = read(UNIT + "LyricsSyncPolicyTest.kt")
editor_tests = read(UNIT + "LyricsEditorPolicyTest.kt")
workflow = read(".github/workflows/android.yml")

assert "STEP_MS = 250" in sync
assert "FINE_STEP_MS = 50" in sync
assert "MAX_OFFSET_MS = 10_000" in sync
assert "ms % FINE_STEP_MS == 0" in sync
assert "fun fineAdjust(" in sync
assert "fun activeIndex(" in sync and "fun seekPosition(" in sync
assert "fun lineProgress(" in progress
assert "fun scrollTarget(" in progress
assert "val nextTime = next ?: return 1f" in progress
assert "LyricsSyncPolicy.validOffset(offsetMs)" in progress
assert "NUDGE_MS = 250L" in editor and "FINE_NUDGE_MS = 50L" in editor
assert "stepMs: Long = NUDGE_MS" in editor
assert "stepMs == NUDGE_MS || stepMs == FINE_NUDGE_MS" in editor
assert "collectIsDraggedAsState()" in player
assert "if (lyricsDragged) lyricsAutoFollow = false" in player
assert "LaunchedEffect(activeCue, lyricsAutoFollow," in player
assert "if (lyricsAutoFollow && activeCue >= 0)" in player
assert "LyricsPresentationPolicy.scrollTarget(activeCue)" in player
assert "LyricsPresentationPolicy.lineProgress(" in player
assert "lyrics-follow-toggle" in player
assert "lyrics-line-progress" in player
assert "lyrics-fine-timing-toggle" in player
assert "LyricsSyncPolicy.fineAdjust(" in player
assert "LyricsSyncPolicy.seekPosition(" in player
assert "onLyricsOffsetChange(" in player
assert "Your imported LRC" in player
assert "if (reduceMotion) lyricListState.scrollToItem(" in player
assert "var fineNudge by rememberSaveable(songId)" in editor_ui
assert "LyricsEditorPolicy.nudge(draft, index, -1, nudgeStep)" in editor_ui
assert "LyricsEditorPolicy.nudge(draft, index, 1, nudgeStep)" in editor_ui
assert "lyrics-editor-fine-nudge" in editor_ui
assert "onApply(updated)" in editor_ui and "Save changes" in editor_ui
assert "Discard unsaved lyric edits?" in editor_ui
assert "lyricsStore.saveReviewed(" in activity
assert "lyricsOffsetStore.save(id, desiredOffset)" in activity
assert "nightwave_private_lrc_offset_v1" in offset_store
assert "nightwave_private_lrc_v1" in original_lyrics
assert tests.count("@Test fun") >= 8
assert timing_tests.count("@Test fun") >= 13
assert editor_tests.count("@Test fun") >= 16
assert "python3 tools/verify_v2_phase6x7.py" in workflow
assert "python3 tools/verify_v2_phase6x6.py" in workflow
assert "feature/nightwave-v2-phase6x7-lyrics-follow-fine-sync" in workflow
for name, source in (
    ("sync", sync), ("progress", progress), ("editor", editor),
    ("player", player), ("editor_ui", editor_ui), ("tests", tests),
    ("timing_tests", timing_tests), ("editor_tests", editor_tests),
):
    assert source.count("{") == source.count("}"), name + " brace imbalance"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: Phase 6X7 precise LRC synchronization, manual follow, editor and storage hooks")
print("PENDING: Android Gradle compilation, JUnit execution and physical audio/scroll audition")
