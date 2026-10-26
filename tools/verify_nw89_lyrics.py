#!/usr/bin/env python3
"""NW89 source-only lyrics/accessibility contracts. Not an Android compile."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
lyrics_dir = base + "v2/lyrics/"
codec = read(lyrics_dir + "TimedLyrics.kt")
review = read(lyrics_dir + "LyricsImportReview.kt")
presentation = read(lyrics_dir + "LyricsPresentationPolicy.kt")
reading = read(lyrics_dir + "LyricsReadingPolicy.kt")
editor = read(lyrics_dir + "LyricsEditorPolicy.kt")
now = read(base + "v2/ui/StudioPlayerScreens.kt")
editing = read(base + "v2/ui/NightWaveLyricsEditorDialog.kt")
tests = {
    "TimedLyricsCodecTest.kt": ["nw89SimultaneousCuesPreserveTheUsersWrittenOrder",
                                "nw89LargeBoundedMultilingualLrcKeepsStableOrder"],
    "LyricsImportReviewPolicyTest.kt": [
        "nw89BanglaVowelsAreNotRemovedFromSongIdentityHints",
        "nw89ExplicitTitleTagMustNotPassBecauseOfPartialWord"],
    "LyricsPresentationPolicyTest.kt": [
        "nw89LongLrcBinaryNextCueLookupPreservesProgressAccuracy",
        "nw89ManySimultaneousCuesFindNextDistinctTimestamp"],
    "LyricsEditorPolicyTest.kt": [
        "nw89EditorDoesNotAlphabetizeSimultaneousLyricCues"],
    "LyricsReadingPolicyTest.kt": [
        "nw89OnlyNearbyLinesAnimateAndReducedMotionAlwaysJumps",
        "nw89InvalidAndHugeIndexJumpsNeverOverflow",
        "nw89ScreenReaderUsesRealLineAndOffsetAdjustedSeekTime",
        "nw89AccessibleCurrentStateAndFollowHintTellTheTruth"],
}
assert "cues.distinct().sortedBy { it.timeMs }" in codec
assert "private val order = compareBy<LyricCue> { it.timeMs }" in editor
assert "thenBy { it.text }" not in editor
assert r"\p{M}" in review
assert '(" $declared ").contains(" $target ")' in review
assert '(" $fileName ").contains(targetPhrase)' in review
assert "while (low <= high)" in presentation
assert "var nextTime: Long? = null" in presentation
assert "for (index in activeIndex + 1" not in presentation

for marker in (
    "MAX_ANIMATED_SCROLL_ROWS = 12",
    "fun shouldAnimateScroll(",
    "fun cueDescription(",
    "LyricsSyncPolicy.seekPosition(",
    "fun currentLineState(",
    "fun followHint(",
):
    assert marker in reading, marker
assert "LyricsReadingPolicy.shouldAnimateScroll(" in now
assert "LyricsReadingPolicy.shouldAnimateScroll(" in editing
assert "lyricsDragged" in now and "lyricsAutoFollow = false" in now
assert 'testTag("lyrics-cue-$index")' in now
assert "heightIn(min = 56.dp)" in now
assert "contentDescription = LyricsReadingPolicy.cueDescription(" in now
assert "stateDescription =" in now
assert "LyricsReadingPolicy.currentLineState(current)" in now
assert 'testTag("lyrics-follow-toggle")' in now
assert "actions.seekToForTrack(cueOriginTrack," in now
assert "LyricsSyncPolicy.seekPosition(" in now
assert "PrivateLyricsStore" not in reading
assert "MAX_BYTES = 64 * 1024" in codec
assert "MAX_LINES = 1200" in codec

for file, case_names in tests.items():
    test_file = read("app/src/test/java/com/nightwave/player/v2/lyrics/" + file)
    for case in case_names:
        assert case in test_file, file + " missing " + case
smoke = read("tools/NW89LyricsSmoke.kt")
assert "NW89_LYRICS_ACCESSIBILITY_PERFORMANCE_SMOKE_PASS" in smoke
print("NW89_LYRICS_ACCESSIBILITY_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: Kotlin/JUnit execution, Android compilation and TalkBack/device QA")
