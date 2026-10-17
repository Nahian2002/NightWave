#!/usr/bin/env python3
"""NightWave Alpha 5E source-only design gate, not Android compilation."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
app = root / "app/src/main/java/com/nightwave/player"
read = lambda p: p.read_text(encoding="utf-8")
gradle = read(root / "app/build.gradle.kts")
studio = read(app / "v2/ui/NightWaveStudioActivity.kt")
components = read(app / "v2/ui/StudioComponents.kt")
discovery = read(app / "v2/ui/DiscoveryShortcuts.kt")
player = read(app / "v2/ui/StudioPlayerScreens.kt")
bridge = read(app / "v2/playback/StudioPlaybackBridge.kt")
service = read(app / "playback/PlaybackService.kt")
store = read(app / "offline/OfflineSessionStore.kt")
manifest = read(root / "app/src/main/AndroidManifest.xml")
tests = read(root / "app/src/test/java/com/nightwave/player/v2/ui/DiscoveryShortcutsTest.kt")

assert 'versionCode = 31' in gradle
assert 'versionName = "2.0.0-alpha5o"' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle

# Shortcuts are authored query helpers, not mock playable tracks.
assert 'data class DiscoveryShortcut(' in discovery
assert 'object DiscoveryShortcuts' in discovery
assert 'fun isValid(): Boolean' in discovery
assert 'fun find(key: String)' in discovery
assert len([line for line in discovery.splitlines() if 'DiscoveryShortcut("' in line]) == 6
assert tests.count('@Test') >= 3

# Real recent-playback spotlight, grounded artwork, and current track state.
assert 'fun ListeningSpotlight(' in components
assert 'lastTrack: VideoPreview?' in components
assert 'fun MusicCollectionTile(' in components
assert 'private fun TrackArtwork(' in components
assert 'if (video.thumbnail.isNotBlank())' in components
assert 'isPlaying: Boolean = false' in components
assert 'ListeningSpotlight(' in studio
assert 'recent.firstOrNull()?.let { onPlay(it, recent) }' in studio
assert 'MusicCollectionTile("Liked songs"' in studio
assert 'getPlaylist(name)' in studio
assert 'Filter this collection' in studio
assert 'isPlaying = playingUrl == song.url' in studio

# Search suggestions use the existing real public search method, no fabricated results.
assert 'DiscoveryShortcuts.picks.chunked(2)' in studio
assert 'onSearch(shortcut.query)' in studio
assert 'onNativeSearch(shortcut.query, SearchMode.SONGS)' in studio
assert 'items(state.videos, key = { it.url })' in studio
assert 'onClick = { onPlay(song, state.videos) }' in studio
assert 'Open YouTube search' in studio

# Navigation stays in-app and respects reduced-motion settings.
assert 'animateColorAsState(' in studio
assert 'MotionPreference.REDUCED' in studio
assert 'BackHandler(enabled = showQueue || expanded ||' in studio
assert 'tab == StudioTab.LIBRARY && selectedPlaylist != null -> selectedPlaylist = null' in studio
assert 'tab != StudioTab.HOME -> tab = StudioTab.HOME' in studio

# Preserve prior offline resume, network player and one launcher.
assert 'private fun resumeOfflineSession()' in service
assert 'ACTION_RESUME_OFFLINE' in studio
assert 'fun read(): OfflineSession?' in store
assert 'fun StudioNowPlayingOverlay(' in player
assert 'class StudioPlaybackBridge(' in bridge
creation = service.split('override fun onCreate()', 1)[1].split('override fun onStartCommand', 1)[0]
assert 'playOfflineId(' not in creation
assert 'resolveAndPlay(' not in creation
ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.count('android.intent.category.LAUNCHER') == 1

for name, source in (
    ('premium shell', studio), ('shared design', components),
    ('discovery', discovery), ('existing player UI', player)
):
    assert source.count('{') == source.count('}'), name + ' unbalanced braces'
    imports = [line for line in source.splitlines() if line.startswith('import ')]
    assert len(imports) == len(set(imports)), name + ' duplicate imports'

print('PASS: Phase 5E source wiring, real discovery, saved library and navigation protections')
print('PENDING: Android Gradle, Compose rendering and physical-device validation')
