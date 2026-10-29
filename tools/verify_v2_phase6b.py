#!/usr/bin/env python3
"""Phase 6B source gate. Not Android compilation or executed tests."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path):
    return (root / path).read_text(encoding="utf-8")

base = "app/src/main/java/com/nightwave/player/"
activity = read(base + "v2/ui/NightWaveStudioActivity.kt")
now_playing = read(base + "v2/ui/StudioPlayerScreens.kt")
lyric_codec = read(base + "v2/lyrics/TimedLyrics.kt")
lyric_store = read(base + "v2/lyrics/PrivateLyricsStore.kt")
insights = read(base + "v2/insights/ListeningInsights.kt")
insights_ui = read(base + "v2/ui/ListeningInsightsPanel.kt")
backup = read(base + "v2/cloud/CloudBackupCodec.kt")
service = read(base + "playback/PlaybackService.kt")
workflow = read(".github/workflows/android.yml")
gradle = read("app/build.gradle.kts")
lyric_tests = read("app/src/test/java/com/nightwave/player/v2/lyrics/TimedLyricsCodecTest.kt")
insight_tests = read("app/src/test/java/com/nightwave/player/v2/insights/ListeningInsightsTest.kt")

# Lyrics are sourced solely from a user-opened local document for an existing
# imported offline audio track. Limit file size/line count and stored payload.
assert 'ActivityResultContracts.OpenDocument()' in activity
assert 'importLyricsFile.launch(arrayOf("text/*", "application/octet-stream"))' in activity
assert 'val id = pendingLyricsId' in activity
assert 'playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(id)' in activity
assert 'count <= TimedLyricsCodec.MAX_BYTES' in activity
assert 'lyricsStore.save(id, data.toByteArray())' in activity
assert 'onRemoveLyrics' in activity
assert 'lyricsRevision++' in activity
assert 'val current = playback.state.videoUrl' in activity
assert 'if (current.startsWith("offline:"))' in activity
assert 'loadedLyrics = null' in activity
assert 'loadedLyrics = id to result' in activity
assert 'MAX_BYTES = 64 * 1024' in lyric_codec
assert 'MAX_LINES = 1200' in lyric_codec
assert 'No synchronized LRC timestamps were found' in lyric_codec
assert 'onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)' in lyric_codec
assert 'cues.distinct().sortedWith(' in lyric_codec
assert 'fun activeIndex(positionMs: Long)' in lyric_codec
assert 'nightwave_private_lrc_v1' in lyric_store
assert 'getSharedPreferences(' in lyric_store
assert 'offline.find(id) != null' in lyric_store
assert 'lyricsStore' in activity
assert 'save(id: String, bytes: ByteArray)' in lyric_store
assert 'fun delete(id: String)' in lyric_store

# Now Playing never attempts to fetch copyrighted public-video lyrics.
assert 'lyrics: TimedLyrics?' in now_playing
assert 'var lyricsOpen by remember(state.videoUrl)' in now_playing
assert 'if (!state.offline)' in now_playing
assert 'does not copy lyrics from streaming sites' in now_playing
assert 'Import synchronized lyrics' in now_playing
assert 'val active = lyrics.activeIndex(state.positionMs)' in now_playing
assert 'lyrics.cues.drop(start).take(6)' in now_playing
assert 'Text("Remove lyrics")' in now_playing
assert 'onImportLyrics = onImportLyrics' in activity
assert 'onRemoveLyrics = onRemoveLyrics' in activity
assert 'StudioMiniPlayer(playerState, playerActions,' in activity
assert 'contentDescription = "Hide mini-player; music keeps playing"' in now_playing

# Listening facts are bounded deduplicated local snapshot, no fabricated
# listening minutes, rank by play count, remote analytics or extra tracking.
assert 'ListeningInsightsEngine.calculate(recent, favorites, playlistNames)' in activity
assert 'ListeningInsightsPanel(insights)' in activity
assert 'recent.take(100).distinctBy { it.url }' in insights
assert 'distinctRecentCreators' in insights
assert 'likedTracks = favorites.distinctBy { it.url }.size' in insights
assert 'playlists = playlistNames.distinct().size' in insights
assert 'not play counts' in insights_ui
assert 'top 3' not in insights_ui.lower()
for prohibited in ('Retrofit', 'OkHttp', 'https://api', 'Spotify', 'NewPipe', 'Network', 'API_KEY'):
    assert prohibited not in lyric_codec + lyric_store + insights, prohibited
assert 'guest_archive' in backup
assert 'LibraryBackupCodec.decode(manualBackup)' in backup
assert 'resolveAndPlay(' in service
assert 'versionCode = 35' in gradle
assert 'applicationId = "com.nightwave.player"' in gradle
assert lyric_tests.count("@Test") >= 10
assert insight_tests.count("@Test") >= 5
assert 'tools/verify_v2_phase6b.py' in workflow
assert ':app:compileDebugAndroidTestKotlin' in workflow

for name, source in (("Activity", activity), ("NowPlaying", now_playing),
                     ("LyricCodec", lyric_codec), ("LyricStore", lyric_store),
                     ("Insights", insights), ("InsightsUI", insights_ui)):
    assert source.count("{") == source.count("}"), name + " brace balance"
    imports = [x for x in source.splitlines() if x.startswith("import ")]
    assert len(imports) == len(set(imports)), name + " duplicate imports"
print("PASS: 6B user-imported private synced lyrics and honest bounded recent-listening insights")
print("PENDING: Android/Compose compile, JVM regression run, on-device UX and signing QA")
