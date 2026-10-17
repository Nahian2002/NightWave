#!/usr/bin/env python3
"""NightWave 6J source contract; not a substitute for Kotlin/Android Gradle."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
identity = read(base + "net/MusicIdentity.kt")
saved = read(base + "net/SavedTrackSearch.kt")
model = read(base + "net/YouTubeGateway.kt")
studio = read(base + "v2/ui/NightWaveStudioActivity.kt")
components = read(base + "v2/ui/StudioComponents.kt")
player = read(base + "v2/ui/StudioPlayerScreens.kt")
test = read("app/src/test/java/com/nightwave/player/net/MusicIdentityTest.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

# Formatting is display-only and grounded in public source metadata.
assert "object MusicIdentity" in identity
assert "data class MusicDisplayIdentity(" in identity
assert "fun display(title: String, uploader: String)" in identity
assert "private val bracketedPromotion = Regex(" in identity
assert "private val creditedTitle = Regex(" in identity
assert "key(prefix) == key(normalizedSource)" in identity
assert "return MusicDisplayIdentity(clean, source)" in identity
assert "var clean = original" in identity
assert "val source = uploader.trim()" in identity
assert 'source.replace(creatorSuffix, "")' in identity
assert "fun creatorsFrom(tracks: List<VideoPreview>, limit: Int = 8)" in identity
assert "distinctBy { it.url }" in identity
assert "limit.coerceAtMost(12)" in identity
assert "normalized in genericCreators" in identity
for forbidden in ("HttpURLConnection", "NewPipe", "Retrofit", "MediaController", "StreamInfo.getInfo"):
    assert forbidden not in identity, forbidden + " must not be used to label a saved track"
assert "data class VideoPreview(val title: String, val author: String, val url:" in model
assert "fun resolve(url: String): PlayableAudio" in model
# Original metadata storage model and playback remain unchanged.
assert "VideoPreview(" not in identity
assert "MusicIdentity" not in read(base + "playback/PlaybackService.kt")
assert "MusicIdentity" not in read(base + "v2/data/GuestProfileStore.kt")

# UI labels use derived identities; original VideoPreview is passed unchanged
# to playback and playlist commands.
assert "MusicIdentity.display(video.title, video.author)" in components
assert "Text(identity.title" in components
assert "identity.artistOrCreator" in components
assert 'contentDescription = "Options for " + video.title' in components
assert "MusicIdentity.display(state.title, state.artist)" in player
assert "MusicIdentity.display(it.title, it.author).title" in components
assert "MusicIdentity.display(it.title, it.author).artistOrCreator" in components
assert 'onClick = { onPlay(song, savedTracks) }' in studio
assert 'onClick = { onPlay(song, savedMatches) }' in studio
assert "SavedTrackSearch.match(query, savedTracks)" in studio

# Local-only discovery from genuinely saved/recent source URLs.
search = studio.split("private fun StudioSearch(", 1)[1].split("private fun StudioLibrary(", 1)[0]
assert "MusicIdentity.creatorsFrom(savedTracks, limit = 8)" in search
assert 'item(key = "local-creators")' in search
assert 'SectionHeading("Artists & creators from your music"' in search
assert "Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())" in search
assert "knownCreators.forEach { creator ->" in search
assert "query = creator.name" in search
assert "quickOptionsExpanded = false" in search
assert "onPrimarySearch(creator.name" not in search
assert "onNativeSearch(creator.name" not in search
assert "Saved entries stay in your library. Public video audio still needs internet." in search
assert "Saved entries · public videos require internet to play" in search
assert "val label = MusicIdentity.display(track.title, track.author)" in saved
assert "val label = MusicIdentity.display(it.value.title, it.value.author)" in saved

# Bounded authored tests, no new permission or app identity change.
assert test.count("@Test") >= 10
assert "mismatchedPublisherCannotCreateUnverifiedArtistAttribution" in test
assert "metadataFormattingDoesNotMutateTheActualPlayableVideo" in test
assert "searchMatchesDisplayArtistWithSpacingEvenWhenUploaderHasNone" in test
assert "tools/verify_v2_phase6j.py" in workflow
assert ":app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin" in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert "versionCode = 35" in gradle

for label, source in (("identity", identity), ("saved search", saved),
                      ("studio", studio), ("components", components),
                      ("player", player), ("test", test)):
    assert source.count("{") == source.count("}"), label + " unbalanced braces"
    imports = [ln for ln in source.splitlines() if ln.startswith("import ")]
    assert len(imports) == len(set(imports)), label + " duplicate imports"

print("PASS: Phase 6J verified-source title formatting, honest local creator shelf and unchanged playback identity")
print("PENDING: full Kotlin/Gradle/JUnit, Android UI/large-font, singer attribution and signer QA")
