#!/usr/bin/env python3
"""NightWave 5Y static safety gate. Does not run Gradle or phone playback."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding="utf-8")
base = "app/src/main/java/com/nightwave/player/"
home = read(base + "v2/ui/NightWaveStudioActivity.kt")
component = read(base + "v2/ui/StudioComponents.kt")
bridge = read(base + "v2/playback/StudioPlaybackBridge.kt")
service = read(base + "playback/PlaybackService.kt")
mixes = read(base + "v2/recommendation/PersonalMixEngine.kt")
policy = read(base + "v2/playback/SpotlightResumePolicy.kt")
test_mix = read("app/src/test/java/com/nightwave/player/v2/recommendation/PersonalMixEngineTest.kt")
test_resume = read("app/src/test/java/com/nightwave/player/v2/playback/SpotlightResumePolicyTest.kt")
cloud = read(base + "v2/cloud/NightWaveCloudWorker.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

# Real local-only recommendations, not Spotify or fictitious catalog data.
assert "object PersonalMixEngine" in mixes
assert "fun make(" in mixes
assert "VideoLink.normalize(it.url) == it.url" in mixes
assert "(likes + plays + lists).distinctBy { it.url }" in mixes
assert "if (pool.isEmpty()) return emptyList()" in mixes
assert "dayNumber" in mixes
assert "return outputs.take(3)" in mixes
assert "tracks = songs.take(12)" in mixes
assert "if (!signatures.add(signature)) return" in mixes
assert "fun rank(track: VideoPreview)" in mixes
assert "only if" not in mixes.lower()
for banned in ("Retrofit", "HttpUrl", "OkHttp", "NewPipe", "fetch(", "apiKey",
               "Spotify", "https://api.", "randomTrack", "generateUrl"):
    assert banned not in mixes, banned
assert test_mix.count("@Test") >= 7

# A mix tap expands authentic suggestions. Playback begins only on track tap.
assert "PersonalMixEngine.make(favorites, recent, playlistTracks, dayNumber," in home
assert 'SectionHeading("Made for your listening"' in home
assert "chosenMixKey = if (chosenMixKey == mix.key)" in home
assert 'onClick = { onPlay(song, chosenMix.tracks) }' in home
assert "onSearch(shortcut.query)" in home

# Spotlight same-track request must never clear/recreate an existing stream.
assert "object SpotlightResumePolicy" in policy
assert "SpotlightAction.KEEP_CURRENT" in policy
assert "SpotlightAction.RESUME_CURRENT" in policy
assert "SpotlightAction.RETRY_CURRENT" in policy
assert "SpotlightAction.START_SELECTED" in policy
assert "onResumeSpotlight(it, recent)" in home
assert "SpotlightResumePolicy.choose(" in home
assert "SpotlightAction.KEEP_CURRENT -> Unit" in home
assert "SpotlightAction.RESUME_CURRENT -> playerActions.resumeCurrent()" in home
assert "SpotlightAction.RETRY_CURRENT -> playerActions.retry()" in home
assert "fun resumeCurrent()" in bridge
assert "active.isPlaying -> Unit" in bridge
assert "active.currentMediaItem != null -> {" in bridge
assert "active.play()" in bridge
assert "if (sameQueue && (loaded || resolving))" in service
assert "if (loaded) exoPlayer?.apply {" in service
assert "playbackState == Player.STATE_ENDED" in service
assert "resolvingPublicUrl == queue.current?.url" in service
assert "resolvingPublicUrl = null" in service
assert test_resume.count("@Test") >= 8

# Existing media ownership, cloud backups, app identity and signing untouched.
assert "ACTION_RESUME_OFFLINE" in service
assert "private fun resolveAndPlay(" in service
assert "restoreWithConfirmation(" not in service
assert "class NightWaveCloudWorker" in cloud
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert "tools/verify_v2_phase5y.py" in workflow
assert ":app:compileDebugAndroidTestKotlin" in workflow
for name, content in (("home", home), ("service", service), ("bridge", bridge),
                      ("mix", mixes), ("resume", policy)):
    assert content.count("{") == content.count("}"), name + " unbalanced braces"
    imports = [s for s in content.splitlines() if s.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " repeated imports"
print("PASS: 5Y local-only mixes, preview-before-play and non-destructive current-track resume")
print("PENDING: Android Gradle/JUnit, device stream extraction timing and media service QA")
