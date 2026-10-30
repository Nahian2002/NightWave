"""NW137 owned-LRC caller/data boundaries; host/source checks never certify phone sync."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player/v2'
REQUIRED={
 'lyrics/LyricsTrackBindingPolicy.kt':['VideoLink.normalize(mediaId) == mediaId','"youtube:" + mediaId.substringAfter("v=")',
  'mediaId.removePrefix("offline:").takeIf(::isLocal)', 'MAX_PUBLIC_TRACKS = 128', 'MAX_PUBLIC_BYTES = 4 * 1024 * 1024',
  'TimedLyricsCodec.parse(bytes)', 'LyricsEditorPolicy.serialize(', 'MessageDigest.getInstance("SHA-256")'],
 'lyrics/BoundLyricsStore.kt':['synchronized(writeLock)', 'synchronized(OfflineLibraryIndexLock.monitor)',
  'local.saveReviewed(id, normalized.toByteArray(Charsets.UTF_8), expectedFingerprint)',
  'if (fingerprint(id) != expectedFingerprint) return false', 'OwnedLyricsContentPolicy.withinQuota',
  'if (!active()) return false','if (!active()) false else prefs.edit().putString',
  'PREFS_NAME = "nightwave_public_lrc_v1"','offsets.save(id, offsetMs)','local.delete(id)',
  'LyricsSyncPolicy.validOffset(offsetMs)', 'local.read(id)'],
 'ui/NightWaveStudioActivity.kt':['boundLyrics = BoundLyricsStore(this, lyricsStore, lyricsOffsetStore)',
  'internal class LyricsImportRetainedState : ViewModel()', 'lyricsImportRetained.pendingId',
  'lyricsImportRetained.review = selection', 'kotlinx.coroutines.withTimeoutOrNull(3_000L)',
  'while (!playback.state.connected) delay(80L)', 'val selectedMedia = LyricsTrackBindingPolicy.mediaId(id)',
  'requestEpoch != lyricsImportEpoch', 'LyricsTrackBindingPolicy.sourceLabel(id)',
  'boundLyrics.fingerprint(id)', 'boundLyrics.saveReviewed(', 'selection.previousFingerprint)',
  'LyricsTrackBindingPolicy.matches(id, playback.state.videoUrl)',
  'playback.state.videoUrl == LyricsTrackBindingPolicy.mediaId(it.first)',
  'val lyricId = LyricsTrackBindingPolicy.storageId(current)', 'boundLyrics.readOffset(id)',
  'boundLyrics.saveOffset(id, desiredOffset)', 'lyricsReviewDialog?.dismiss()',
  'lyricsStore.exportCloud()', 'lyricsStore.restoreCloudMissing(',
  'playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(pending.songId)',
  'playback.state.videoUrl == LyricsTrackBindingPolicy.mediaId(pending.songId)'],
 'ui/StudioPlayerScreens.kt':['val lyricsSupported = LyricsTrackBindingPolicy.storageId(state.videoUrl) != null',
  'lyricsImmersive && lyricsSupported && lyrics != null', 'if (lyricsSupported)',
  'R.string.nw141_attach_your_own_timestamped_lrc_to_this_exact_vi_bf4e8', 'actions.seekToForTrack(cueOriginTrack,',
  'onEditLyrics', 'onImportLyrics', 'onRemoveLyrics', 'LyricsSyncPolicy.seekPosition('],
}
def verify(source):
 for name,markers in REQUIRED.items():
  for marker in markers:assert marker in source[name],(name,marker)
 bound=source['lyrics/BoundLyricsStore.kt']
 for forbidden in ['YouTubeGateway','HttpURLConnection','newCall(', 'exportCloud(', 'restoreCloudMissing(', 'contentResolver']:
  assert forbidden not in bound,forbidden
 activity=source['ui/NightWaveStudioActivity.kt']
 picker=activity[activity.index('private val importLyricsFile'):activity.index('/** Android asks the user where to save')]
 assert picker.index('TimedLyricsCodec.MAX_BYTES') < picker.index('lyricsImportRetained.review = selection')
 assert 'setPositiveButton(if (replacing) "Replace LRC" else "Import LRC")' in picker
 assert 'lyricsImportRetained.review = null' in picker
 assert 'boundLyrics.exportCloud' not in activity
 assert 'Synchronized lyrics are not available for public videos' not in source['ui/StudioPlayerScreens.kt']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 source={n:(BASE/n).read_text(encoding='utf-8') for n in REQUIRED};verify(source)
 if a.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(source);broken[name]=broken[name].replace(marker,'BROKEN_FIXTURE')
    try:verify(broken)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted broken caller: '+name+': '+marker)
 print('PASS NW137 exact owned-LRC binding, original local/cloud stores, bounded device-private public LRC, retained explicit review, guarded import/editor/export/offset and scoped cue seek; device NOT_RUN')
