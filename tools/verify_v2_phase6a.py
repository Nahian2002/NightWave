#!/usr/bin/env python3
"""Phase 6A UI source contract; deliberately NOT an Android or screenshot test."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
studio = read(base + "v2/ui/NightWaveStudioActivity.kt")
component = read(base + "v2/ui/StudioComponents.kt")
player = read(base + "v2/ui/StudioPlayerScreens.kt")
visual = read(base + "v2/ui/NightWaveVisualLanguage.kt")
queue = read(base + "playback/PlaybackService.kt")
cloud = read(base + "v2/cloud/NightWaveCloudWorker.kt")
planner = read(base + "v2/cloud/CloudLibraryMerger.kt")
gradle = read("app/build.gradle.kts")
workflow = read(".github/workflows/android.yml")

# All surfaces built from real saved data, fixed identifiers and Material colors.
assert 'object NightWaveVisualLanguage' in visual
assert 'fun accent(identity: String): Color' in visual
assert 'fun NightWaveBackdrop(identity: String' in visual
assert 'fun NightWaveMoodArt(' in visual
assert 'fun NightWaveMixCard(' in visual
assert 'fun NightWaveLibraryNumbers(' in visual
assert 'fun NightWaveMicroLabel(' in visual
assert 'Color.Transparent' in visual
assert 'Brush.radialGradient(' in visual
assert 'Brush.linearGradient(colors)' in visual
for unsafe in ('AsyncImage(', 'BitmapFactory', 'OkHttp', 'WebView', 'NewPipe',
               'Math.random(', 'Permission', 'getExternalStorageDirectory'):
    assert unsafe not in visual, unsafe

# Home: authentic, context-aware content and non-interruptive mix selection.
assert 'NightWaveLibraryNumbers(favorites.size, recent.size, onLibrary = openLibrary)' in studio
assert 'NightWaveMixCard(' in studio
assert 'chosenMixKey = if (chosenMixKey == mix.key)' in studio
assert 'onClick = { onPlay(song, chosenMix.tracks) }' in studio
assert 'NightWaveMoodArt(title, subtitle, gradient, modifier, onClick)' in studio
assert 'ListeningSpotlight(' in studio
assert 'onResumeSpotlight(it, recent)' in studio
assert 'offset(x = (-9).dp)' in component

# Search and Library remain useful, clearable and local-first.
assert 'SectionHeading("Explore sound"' in studio
assert 'contentDescription = "Clear search"' in studio
assert 'Text("Find music on YouTube")' in studio
assert 'Text(if (searching) "Checking quick results…" else "Try quick in-app results")' in studio
assert 'SavedTrackSearch.match(query, savedTracks)' in studio
assert 'contentDescription = "Clear library filter"' in studio
assert 'var sortMode by rememberSaveable(selectedPlaylist)' in studio
assert 'sortMode == "saved"' in studio
assert 'sortMode == "title"' in studio
assert 'sortMode == "artist"' in studio
assert 'onPlay(song, if (selectedPlaylist == null) tracks else orderedTracks)' in studio
assert 'Text("Manage playlists")' in studio

# Navigation and player visual edits must not alter player/media ownership.
assert 'testTag("nightwave-tab-" + item.name.lowercase())' in studio
assert 'this.selected = active' in studio
assert 'profile.motion == MotionPreference.REDUCED' in studio
assert 'userScrollEnabled = !expanded && !showQueue' in studio
assert 'NightWaveBackdrop(state.videoUrl.ifBlank' in player
assert 'NightWaveVisualLanguage.accent(state.videoUrl)' in player
assert 'state.buffering' in player
assert 'canSwipeArtwork' in player
assert 'actions::playPause' in player
assert 'actions::toggleShuffle' in player
assert 'actions::cycleRepeat' in player
assert 'onOpenQueue' in player
assert 'PlayerGestureRules.MIN_VERTICAL_SWIPE_DP.dp.toPx()' in player
assert 'PlayerGestureRules.MIN_SWIPE_DP.dp.toPx()' in player
assert 'NightWaveVisualLanguage.accent(state.videoUrl)' in player
assert 'state.offline' in player
assert 'NightWaveCloudWorker' in cloud
assert 'object CloudLibraryMerger' in planner
assert 'resolveAndPlay(' in queue
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle
assert 'versionName = "2.0.0-alpha5s"' in gradle
assert 'tools/verify_v2_phase6a.py' in workflow

for name, source in (("studio", studio), ("components", component),
                     ("player", player), ("visual", visual)):
    assert source.count("{") == source.count("}"), name + " unbalanced curly braces"
    imports = [line for line in source.splitlines() if line.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicated imports"

print("PASS: Phase 6A design source contract, retained playback, navigation and cloud boundaries")
print("PENDING: Compose compilation, Android screenshot/device QA and accessibility validation")
