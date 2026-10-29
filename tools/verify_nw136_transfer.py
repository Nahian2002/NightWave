"""NW136 caller/contract guard; actual HTTP and checkpoint behavior runs in JUnit."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player/offline'
REQUIRED={
 'AudioRangeTransfer.kt': ['AudioRangePolicy.validator(response.headers)',
  'range.start == offset && range.end <= end', 'state!!.validator != validator',
  'file.fd.sync()', 'persist(meta, temp, state!!)', 'hex(digest.digest()) == hash',
  'file.setLength(state?.offset ?: 0L)', 'StandardCopyOption.ATOMIC_MOVE',
  'stopped() && !keepOnStop()', 'Files.isSymbolicLink', 'MAX_CHECKPOINTS = 8',
  'private const val QUOTA = 512L * 1024 * 1024', 'active.remove(key)'],
 'PermittedAudioDownloader.kt': ['.header("Accept-Encoding", "identity")',
  '.header("Range", "bytes=$offset-$end")', 'header("If-Range", validator)',
  'resolved.all(::isPublicAddress)', '.followRedirects(false)', '.followSslRedirects(false)',
  'engine.transfer(key, pending, retainOnStop)', 'canonicalTrack + "\\n" + representation',
  'inspectAudioOnly(pending)', 'FreeAudioIntegrity.sourceMatches',
  'OfflineSaveStoragePolicy.canStart(free + staged, total)'],
 'SearchTrackSaveWorker.kt': ['canonicalTrack = canonical',
  'resolveAudioOnlyForOffline(canonical)', 'SearchOfflinePauseState.isMarked(applicationContext, id)',
  'stopReason != WorkInfo.STOP_REASON_CANCELLED_BY_APP', 'AudioTransferData.DETAILS'],
 'DirectAudioDownloadWorker.kt': ['stopReason != WorkInfo.STOP_REASON_CANCELLED_BY_APP', 'AudioTransferData.DETAILS'],
 'CatalogDownloadWorker.kt': ['stopReason != WorkInfo.STOP_REASON_CANCELLED_BY_APP', 'AudioTransferData.DETAILS'],
 'CatalogDownloadsActivity.kt': ['previousSample.first < resumedStart', 'OfflineTransferRatePolicy.preciseDisplay(rate)',
  'Show transfer timing and resume details', 'Checked bytes resume when the source supports it'],
}
def verify(source):
 for name, markers in REQUIRED.items():
  for marker in markers: assert marker in source[name],(name,marker)
 engine=source['AudioRangeTransfer.kt']
 assert engine.index('file.fd.sync()') < engine.index('persist(meta, temp, state!!)')
 for queue in ('SearchTrackSaveQueue.kt','DirectAudioDownloadQueue.kt','CatalogDownloadQueue.kt'):
  assert '.addTag(AudioTransferData.TAG)' in source[queue]
 metrics=source['AudioTransferMetrics.kt']
 for forbidden in ('request.url', 'response.body', 'inetAddressList.toString', 'domainName.toString', 'Log.'):
  assert forbidden not in metrics,forbidden
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 names=set(REQUIRED)|{'AudioTransferMetrics.kt','SearchTrackSaveQueue.kt','DirectAudioDownloadQueue.kt','CatalogDownloadQueue.kt'}
 source={n:(BASE/n).read_text(encoding='utf-8') for n in names};verify(source)
 if args.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    bad=dict(source);bad[name]=bad[name].replace(marker,'BROKEN_FIXTURE')
    try:verify(bad)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted broken caller '+name+': '+marker)
 print('PASS NW136 guarded ranges, durable source-bound recovery, worker wiring and safe diagnostics; physical throughput NOT_RUN')
