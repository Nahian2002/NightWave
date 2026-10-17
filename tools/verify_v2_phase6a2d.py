#!/usr/bin/env python3
"""Phase 6A2D source contract only; intentionally no Gradle/device execution."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/v2/ui/"
studio = read(base + "NightWaveStudioActivity.kt")
visual = read(base + "NightWaveVisualLanguage.kt")
player = read(base + "StudioPlayerScreens.kt")
test = read("app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveMixDensityInstrumentedTest.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")

home = studio.split("private fun StudioHome(", 1)[1].split("private fun MoodTile(", 1)[0]
search = studio.split("private fun StudioSearch(", 1)[1].split("private fun StudioLibrary(", 1)[0]
mix_card = visual.split("fun NightWaveMixCard(", 1)[1].split("fun NightWaveMoodArt(", 1)[0]

# Recommendations are genuine and opening cards never starts audio.
assert "PersonalMixEngine.make(favorites, recent, playlistTracks, dayNumber," in home
assert 'SectionHeading("Made for you"' in home
assert 'verticalArrangement = Arrangement.spacedBy(12.dp)' in home
assert 'compact = pair.size == 1' in home
assert 'Modifier.fillMaxWidth()' in home
assert 'chosenMixKey = if (chosenMixKey == mix.key) null else mix.key' in home
assert 'showAllMixTracks = false' in home
assert 'OutlinedButton(onClick = {' in home and 'Text("Play mix")' in home
assert 'chosenMix.tracks.firstOrNull()?.let { onPlay(it, chosenMix.tracks) }' in home
assert 'chosenMix.tracks.take(if (showAllMixTracks) 12 else 3)' in home
assert 'showAllMixTracks = !showAllMixTracks' in home
assert 'onClick = { onPlay(song, chosenMix.tracks) }' in home
assert 'NightWaveMixCard(' in home
assert 'compact: Boolean = false' in visual
assert 'height(if (compact) 106.dp else 122.dp)' in mix_card
assert 'height(118.dp)' in visual

# Empty Search: meaningful local actions and real saved tracks; no fake catalog.
assert 'if (query.isBlank() && !searching)' in search
assert 'NightWaveMicroLabel("Explore by mood")' in search
assert 'if (savedTracks.isNotEmpty())' in search
assert 'items(savedTracks.take(3)' in search
assert 'onClick = { onPlay(song, savedTracks) }' in search
assert 'Your liked songs, playlists and recent music will appear here.' in search
assert 'if (query.isNotBlank()) {' in search
assert 'Text("Find music on YouTube")' in search
assert 'onPrimarySearch(shortcut.query, SearchMode.SONGS)' in search
assert 'var quickOptionsExpanded by rememberSaveable' in search
assert 'More search options' in search
assert 'if (quickOptionsExpanded || (responseMatchesQuery &&' in search
assert 'Text(if (searching) "Checking quick results…" else "Try quick in-app results")' in search
assert 'SavedTrackSearch.match(query, savedTracks)' in search
assert 'query.trim().equals(state.query.trim(), ignoreCase = true)' in search
assert 'onClick = { onPlay(song, savedMatches) }' in search
assert 'onClick = { onPlay(song, state.videos) }' in search
assert 'contentDescription = "Clear search"' in search
assert 'No matches yet. Try a different artist or open YouTube search.' not in search

# Existing playback, close mini, data store and private cloud behavior stay.
assert 'onResumeSpotlight(it, recent)' in home
assert 'contentDescription = "Hide mini-player; music keeps playing"' in player
assert 'onHide = { compactHidden = true }' in studio
assert 'StudioProfileRow("Compare & merge devices"' in studio
assert 'versionCode = 35' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert test.count('@Test') >= 2
assert 'tools/verify_v2_phase6a2d.py' in workflow
for label, src in (("studio", studio), ("visual", visual), ("player", player), ("test", test)):
    assert src.count("{") == src.count("}"), label + ": unmatched braces"
    imports = [x for x in src.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), label + ": duplicated imports"
print("PASS: 6A2D source layout, honest local Search and preview-first personalized mixes")
print("PENDING: Android Compose compile, instrumentation, real-device screen/a11y QA")
