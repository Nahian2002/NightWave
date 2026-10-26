#!/usr/bin/env python3
"""NW81 read-only, source-only contract check. Not an Android build or UI test."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
identity = read(base + "net/MusicIdentity.kt")
tile = read(base + "v2/ui/StudioComponents.kt")
activity = read(base + "v2/ui/NightWaveStudioActivity.kt")
queue = read(base + "v2/ui/StudioPlayerScreens.kt")
tests = read("app/src/test/java/com/nightwave/player/net/MusicIdentityTest.kt")
smoke = read("tools/NW81TitleSmoke.kt")

# All display-only transformations: identity and URL persistence unchanged.
assert "fun display(title: String, uploader: String)" in identity
assert "fun videoDisplay(title: String, uploader: String)" in identity
assert "original" in identity and "return MusicDisplayIdentity(clean, source)" in identity
assert "Character.COMBINING_SPACING_MARK.toInt()" in identity
assert "Character.NON_SPACING_MARK.toInt()" in identity
assert "orphanedBadgeSeparator" in identity
assert "lyric(?:s)?\\s+video" in identity
assert "visuali[sz]er" in identity
assert "creatorsFrom(" in identity and ".distinctBy { it.url }" in identity

# Raw Videos are still rendered with the provider's original title.
assert "rawVideoCaption: Boolean = false" in tile
assert "MusicIdentity.videoDisplay(video.title, video.author)" in tile
assert "MusicIdentity.display(video.title, video.author)" in tile
assert activity.count("rawVideoCaption = mode == SearchMode.VIDEOS") == 2

# Up Next uses one consistent display-only identity; queue URLs/index stay raw.
assert "MusicIdentity.display(title, subtitle)" in queue
assert "Text(display.title," in queue
assert "Text(display.artistOrCreator," in queue
assert "onRemove = { requestRemove(index, video.url) }" in queue
assert "onSelect = { onSelect(index) }" in queue

for case in (
    "nw81RemovesOnlyExplicitTrailingVideoPackaging",
    "nw81NeverErasesMeaningfulRecordingOrFeaturedArtistNames",
    "nw81BanglaVowelsMustNotFalselyCorroborateArtist",
    "nw81VideoModeNeverPretendsProviderCaptionIsMusicMetadata",
    "nw81PresentationNeverMutatesSongMetadataOrIdentity",
):
    assert case in tests, case

assert "NW81_TITLE_PRESENTATION_SMOKE_PASS" in smoke
assert "data class VideoPreview(" in smoke  # stub only within tools directory
print("NW81_TITLE_SOURCE_CONTRACT_PASS")
print("UNVERIFIED: Kotlin compilation, Gradle, Android rendering, real-device QA")
