#!/usr/bin/env python3
"""NW107 source-only contract for reversible lyric-follow accessibility."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
read=lambda path:(root/path).read_text(encoding="utf-8")
policy=read("app/src/main/java/com/nightwave/player/v2/lyrics/LyricsReadingPolicy.kt")
ui=read("app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt")
tests=read("app/src/test/java/com/nightwave/player/v2/lyrics/LyricsReadingPolicyTest.kt")
ci=read(".github/workflows/android.yml")
assert "fun followActionLabel(following: Boolean)" in policy
assert "fun followActionDescription(following: Boolean)" in policy
assert "fun followStateDescription(following: Boolean)" in policy
assert "fun toggledFollow(following: Boolean): Boolean = !following" in policy
assert "lyricsAutoFollow =\n                                    LyricsReadingPolicy.toggledFollow(lyricsAutoFollow)" in ui
assert "LyricsReadingPolicy.followActionDescription(" in ui
assert "LyricsReadingPolicy.followStateDescription(" in ui
assert "LyricsReadingPolicy.followActionLabel(lyricsAutoFollow)" in ui
assert "enabled = !lyricsAutoFollow" not in ui
assert 'testTag("lyrics-follow-toggle")' in ui
assert "if (lyricsDragged) lyricsAutoFollow = false" in ui
assert "if (lyricsAutoFollow && activeCue >= 0)" in ui
assert len(tests.split("@Test fun ")) - 1 == 8
assert "nw107FollowButtonCanPauseWithoutSwiping" in tests
assert "nw107LongBanglaCueDescriptionStillIncludesSeekTime" in tests
assert "tools/NW107LyricsFollowSmoke.kt" in ci
assert "tools/nw107_lyrics_junit_gate.py" in ci
assert "tools/verify_nw107_lyrics_follow.py" in ci
print("NW107_LYRICS_FOLLOW_ACCESSIBILITY_SOURCE_PASS checks=18")
