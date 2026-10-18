#!/usr/bin/env python3
"""Phase 6I UI density/source contract only, not a Compose or Android build."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(p):
    return (root / p).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/v2/ui/"
studio = read(base + "NightWaveStudioActivity.kt")
mini = read(base + "StudioPlayerScreens.kt")
visual = read(base + "NightWaveVisualLanguage.kt")
components = read(base + "StudioComponents.kt")
previous = read("tools/verify_v2_phase6a2.py")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")
home = studio.split("private fun StudioHome(", 1)[1].split("private fun MoodTile(", 1)[0]
library = studio.split("private fun StudioLibrary(", 1)[1].split("private fun StudioProfile(", 1)[0]
profile = studio.split("private fun StudioProfile(", 1)[1].split("private fun StudioProfileGroup(", 1)[0]
root_ui = studio.split("private fun StudioRoot(", 1)[1].split("private fun StudioHome(", 1)[0]
player = mini.split("fun StudioMiniPlayer(", 1)[1].split("private fun CoverArt(", 1)[0]

# Smaller chrome with full 48dp close/play touch targets, gesture surface
# and explicit hide-without-pausing semantics unchanged.
assert 'Box(Modifier.fillMaxWidth().height(9.dp)' in player
assert 'modifier = Modifier.fillMaxWidth().height(54.dp)' in player
assert 'CoverArt(state.thumbnail, Modifier.size(40.dp), 9)' in player
assert 'IconButton(onClick = onHide, modifier = Modifier.size(48.dp))' in player
assert 'IconButton(onClick = actions::playPause, modifier = Modifier.size(48.dp))' in player
assert 'PlayerVerticalSwipe.MINIMIZE -> onHide()' in player
assert 'PlayerVerticalSwipe.EXPAND -> onExpand()' in player
assert 'contentDescription = "Hide mini-player; music keeps playing"' in player
assert 'onHide = { compactHidden = true }' in root_ui
assert 'onClick = { compactHidden = false }' in root_ui
assert 'LaunchedEffect(playerState.videoUrl) { compactHidden = false }' in root_ui
assert 'Modifier.fillMaxWidth().height(58.dp)' in root_ui
assert '.testTag("nightwave-tab-" + item.name.lowercase())' in root_ui
assert 'horizontalArrangement = Arrangement.Center' in root_ui
assert '.semantics { this.selected = active }' in root_ui
assert 'windowInsetsPadding(WindowInsets.safeDrawing)' in root_ui

# No fake catalog or newly autoplayed mood recommendations. Every tile maps
# to the original search action. A horizontal shelf restores vertical space.
assert 'item(key = "discovery-shelf")' in home
assert 'Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())' in home
assert 'DiscoveryShortcuts.picks.forEachIndexed' in home
assert 'palettes[index % palettes.size]' in home
assert 'Modifier.width(178.dp)' in home
assert 'onSearch(shortcut.query)' in home
assert 'SectionHeading("Made for you"' in home
assert 'PersonalMixEngine.make(' in home
assert 'onResumeSpotlight(it, recent)' in home
assert 'Recently played' in home
assert 'Your collection' in home
assert 'NightWaveMoodArt(title, subtitle, gradient, modifier, onClick)' in studio
for label in ('"after hours"', '"deep focus"', '"desi pulse"',
              '"unplugged"', '"momentum"'):
    assert label in visual, label + " has no distinct visual motif"
assert 'when (title.lowercase())' in visual
assert 'Card(onClick = onClick' in visual
assert 'MoodTile(shortcut.title, shortcut.subtitle' in home

# A real Library remains the authority; remove only duplicated counters,
# NOT clickable favorites, playlists, offline library or filters.
assert '${favorites.size} liked tracks  ·  ${names.size} playlists' in library
assert 'MusicCollectionTile("Offline music"' in library
assert 'MusicCollectionTile("Liked songs"' in library
assert 'if (names.isNotEmpty())' in library
# Phase 6O3 passes a pure view-only sorted snapshot to collection tiles.
assert 'items(visiblePlaylists, key = { "playlist:" + it.name })' in library
assert 'MusicCollectionTile(collection.name, collection.size.toString()' in library
assert 'onSelectPlaylist(collection.name)' in library
assert 'onManage' in library and 'localFilter' in library

# The You page exposes privacy/diagnostics through visible disclosure rows.
# Opt-in, erase and backup controls cannot be silently dropped.
assert 'var showSnapshot by rememberSaveable' in profile
assert 'var showListeningMemory by rememberSaveable' in profile
assert 'var showPlaybackHealth by rememberSaveable' in profile
assert 'StudioProfileRow("Recent listening"' in profile
assert 'StudioProfileRow("Listening statistics"' in profile
assert 'if (memory.enabled) "On · Stored privately on this device"' in profile
assert 'if (showListeningMemory)' in profile
assert 'ListeningMemoryPanel(' in profile
assert 'onMemoryEnabledChange, onMemoryErase, onMemoryRefresh' in profile
assert 'PlaybackHealthPanel(playbackState, onRecoverPlayback)' in profile
assert '!playbackState.connected' in profile
assert 'StudioProfileRow("Manage backups"' in profile
assert 'StudioProfileRow("Compare & merge devices"' in profile
assert 'StudioProfileRow("Open classic interface"' in profile
assert 'contentPadding = PaddingValues' in studio
assert 'bottom = 150.dp' not in studio
assert 'BoxWithConstraints' in components

# The earlier safe swipe contract still applies to the new handle height.
assert 'Box(Modifier.fillMaxWidth().height(9.dp)' in previous
assert 'IconButton(onClick = onHide' in previous
assert 'tools/verify_v2_phase6i.py' in workflow
assert 'applicationId = "com.nightwave.player"' in gradle
assert 'versionCode = 35' in gradle

for name, source in (("studio", studio), ("mini", mini),
                     ("mood visual", visual), ("components", components)):
    assert source.count("{") == source.count("}"), name + " unmatched braces"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"

print("PASS: Phase 6I compact chrome, 6 unique mood visuals, swipe shelf and user-controlled profile disclosures")
print("UNVERIFIED: full Compose compile, Android touch/a11y, device artwork and playback")
