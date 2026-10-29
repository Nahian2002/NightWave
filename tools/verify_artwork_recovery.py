"""ART02 source/caller fences; Android cache and audible acceptance stay separate."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player'
REQUIRED={
 'offline/OfflineTrackIdentityStore.kt': ['OfflineArtworkRecoveryPolicy.uniqueLinks',
  'OfflineArtworkRecoveryPolicy.batch(songs, emptySet())','SystemClock.elapsedRealtime() - started',
  'raw != expectedMetadata', 'val fence = OfflineArtworkCommitFence',
  'synchronized(OfflineLibraryIndexLock.monitor)', 'library.fileFor(song) == null',
  'linkPrefs.getString("v_${fence.videoId}"', 'fence.allows(current, record(song.id)',
  'fun remove(id: String) = synchronized(OfflineLibraryIndexLock.monitor)',
  'cache.openSnapshot(candidate)', 'NightWaveArtworkPolicy.isUnavailableLargeStill',
  'MAX_ART_BYTES = 1024L * 1024L', '64L * 1024 * 1024', 'Files.isSymbolicLink',
  'StandardCopyOption.ATOMIC_MOVE', 'finally { staged?.delete() }', 'output.fd.sync()',
  '.putLong(REVISION_KEY', 'if (!active()) return@snapshotUse'],
 'offline/OfflineLibraryActivity.kt': ['OfflineArtworkRecoveryPolicy.batch(songs, artworkRecoveryAttempted)',
  'recoverKnown(batch) { !artworkRecoveryClosed }', 'artworkRecoveryAttempted.addAll',
  'requestArtworkRecovery((listOfNotNull(resumeSong) + pageSongs)',
  '.filter { it.id in playableIds }', 'artworkRecoveryClosed = true'],
 'playback/PlaybackService.kt': ['offlineArtworkWorker',
  'offlineArtworkPrefs.registerOnSharedPreferenceChangeListener(offlineArtworkChanged)',
  'offlineArtworkPrefs.unregisterOnSharedPreferenceChangeListener(offlineArtworkChanged)',
  'offlineArtworkWorker.shutdownNow()', 'OfflineArtworkPlaybackFence.allows(',
  'expectedGeneration, generation, expectedIndex, offlineIndexPrefs.getString("items", null)',
  'answer.second, OfflineTrackIdentityStore.revision(this), offlinePlaying)',
  'identities.recoverKnown(listOf(song)) { !offlineArtworkClosed }',
  'item.buildUpon().setMediaMetadata(metadata).build()',
  'override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {\n            if (crossfadeHandoffInProgress) return\n            scheduleOfflineArtwork()'],
 'v2/playback/StudioPlaybackBridge.kt': ['artworkRevision != cachedOfflineArtworkRevision',
  'cachedOfflineArtworkRevision = artworkRevision'],
}
def verify(source):
 for path,markers in REQUIRED.items():
  for marker in markers: assert marker in source[path],(path,marker)
 identity=source['offline/OfflineTrackIdentityStore.kt']
 for forbidden in ['newCall(', 'HttpURLConnection','enqueue(', 'contentResolver.delete(', 'download(']:
  assert forbidden not in identity,forbidden
 publish=identity[identity.index('private fun publishKnown'):identity.index('@OptIn(coil.annotation.ExperimentalCoilApi::class)')]
 assert publish.index('copyCachedArtwork(')<publish.index('synchronized(OfflineLibraryIndexLock.monitor)')
 copy=identity[identity.index('private fun copyCachedArtwork'):identity.index('fun remove(id: String)')]
 assert 'OfflineLibraryIndexLock' not in copy
 service=source['playback/PlaybackService.kt']
 update=service[service.index('private val recoverOfflineArtwork'):service.index('private lateinit var sleepStatus')]
 for forbidden in ['.prepare(', '.seekTo(', '.play()', '.pause()', '.setMediaItems(', '.setUri(']:
  assert forbidden not in update,forbidden
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 source={n:(BASE/n).read_text(encoding='utf-8') for n in REQUIRED};verify(source)
 if args.self_test:
  for path,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(source);broken[path]=broken[path].replace(marker,'BROKEN_FIXTURE')
    try:verify(broken)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted broken caller: '+path+': '+marker)
 print('PASS ART02 bounded cache-only recovery, no-copy index lock, deletion/source/lifecycle fences, metadata-only player and revision queue refresh; device NOT_RUN')
