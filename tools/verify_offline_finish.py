"""Recording-driven offline finish caller fences; phone acceptance is separate."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player'
REQUIRED={
 'offline/PermittedAudioDownloader.kt':['parallelism = 4','activeCalls.forEach { it.cancel() }',
  'resolved.all(::isPublicAddress)','SearchTrackSavePolicy.safeAudioEndpoint(url)',
  'inspectAudioOnly(pending)','expectedSourceSha1','metrics.addReceived(n.toLong())'],
 'offline/AudioRangeTransfer.kt':['require(parallelism in 1..4)','64 * 1024L',
  'Executors.newFixedThreadPool(parallelism)','completed.poll(250, TimeUnit.MILLISECONDS)',
  'range.start == start && range.end <= end','range.total != state.total','require(range.end == end)',
  'validator != state.validator || type != state.type','parts.sortedBy { it.end }',
  'abortRequests()','executor.shutdownNow()','file.fd.sync()','persist(meta, temp, state)'],
 'offline/CatalogDownloadsActivity.kt':['DownloadRateWindow()', 'canEstimate(now)', 'rateWindows.keys.retainAll(runningIds)',
  'recoveryOnly(destinations)','received >= total','Finishing save · Checking audio',
  'OfflineTransferRatePolicy.preciseDisplay(rate)'],
 'offline/OfflineLibraryActivity.kt':['recoveryOnly(downloadsShortcut)',
  'if (numberOfPages > 1) View.VISIBLE else View.GONE','identities.display(song)',
  'load(identity.artwork)','OfflineTrackIdentityStore(this).remove(song.id)'],
 'offline/OfflineTrackIdentityStore.kt':['cache.openSnapshot(candidate)',
  'BitmapFactory.decodeFile','MAX_ART_BYTES = 1024L * 1024L','64L * 1024 * 1024',
  'Files.isSymbolicLink','StandardCopyOption.ATOMIC_MOVE','OfflineTrackIdentityPolicy.decode'],
 'offline/SearchTrackSaveWorker.kt':['OfflineTrackIdentityStore(applicationContext).capture(saved, stream.video)',
  'SearchTrackOfflineIndex.linkVerified','OfflineTransferCompletionPolicy.canCommit'],
 'playback/PlaybackService.kt':['registerOnSharedPreferenceChangeListener(offlineIndexChanged)',
  'unregisterOnSharedPreferenceChangeListener(offlineIndexChanged)',
  'epoch != offlineIndexEpoch || generation != expectedGeneration',
  'offlineQueue.snapshot() != ids','player.clearMediaItems()', 'offlineSession.clearSelection()',
  'offlineQueue.retain(known)','player.removeMediaItem(i)','OfflineTrackIdentityStore(this).display(song)',
  '.setArtworkUri(identity.artwork'],
 'v2/playback/StudioPlaybackBridge.kt':['val canPause: Boolean', 'playRequested = wantsPlayback',
  'it.playbackState == Player.STATE_BUFFERING || state.resolving', 'identities.display(it)'],
 'v2/ui/StudioPlayerScreens.kt':['Icon(if (state.canPause) Icons.Filled.Pause'],
}
def verify(source):
 for name,markers in REQUIRED.items():
  for marker in markers:assert marker in source[name],(name,marker)
 identity=source['offline/OfflineTrackIdentityStore.kt']
 for forbidden in ('newCall(', 'HttpURLConnection', 'enqueue(', 'contentResolver.delete(', 'download('):
  assert forbidden not in identity,forbidden
 assert 'dispatcher.cancelAll' not in source['offline/PermittedAudioDownloader.kt']
 assert 'others restart safely' in source['offline/DownloadCenterDashboardPolicy.kt']
 assert 'Resume restarts from zero' not in source['offline/DownloadCenterDashboardPolicy.kt']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 source={n:(BASE/n).read_text(encoding='utf-8') for n in set(REQUIRED)|{'offline/DownloadCenterDashboardPolicy.kt'}}
 verify(source)
 if a.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    bad=dict(source);bad[name]=bad[name].replace(marker,'BROKEN_FIXTURE')
    try:verify(bad)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted broken caller: '+name+': '+marker)
 print('PASS bounded parallel guarded transfer, source identity/cache, removal/queue fences, truthful ETA and compact retained routes; device NOT_RUN')
