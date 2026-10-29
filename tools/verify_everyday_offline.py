"""FINISH02 production callers and negative fixtures, not phone acceptance."""
from pathlib import Path
import argparse
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'app/src/main/java/com/nightwave/player/offline'
REQUIRED = {
    'OfflineLibraryActivity.kt': ['identities.display(resumeSong).title', 'contentDescription = "Clear saved-song search"',
        'searchRow.addView(clearSearch, LinearLayout.LayoutParams(dp(48), dp(49)))',
        'OfflineLibraryViewPolicy.canShowAll(', '"Show all saved songs"',
        'pendingSearch?.let(uiHandler::removeCallbacks)', 'availabilityFilter = OfflineLibraryViewPolicy.Filter.ALL',
        'OfflineTrackIdentityStore(this).display(song).title', '.setTitle(identity.title)'],
    'CatalogDownloadsActivity.kt': ['snapshot to display', 'verifiedIdentities = answer.getOrNull()?.second.orEmpty()',
        'val savedIdentity = verifiedIdentities[savedId(info)]', 'AudioTransferData.completion(info.outputData)?.label()',
        'verifiedFileHealth[savedId(info)] == OfflineFileHealth.AVAILABLE', '!verifyingFiles && !fileCheckFailed'],
    'AudioTransferData.kt': ['data.getInt(RECEIPT_VERSION, 0) != 1', 'receipt?.label() == null', 'putLong(RECEIPT_BYTES, receipt.receivedBytes)'],
    'AudioTransferMetrics.kt': ['if (completed == null) completed = CompletedAudioTransfer(wireBytes,',
        'fun completedTransfer(): CompletedAudioTransfer? = completed'],
}
for worker, variable in [('SearchTrackSaveWorker.kt','downloader'), ('DirectAudioDownloadWorker.kt','downloader'), ('CatalogDownloadWorker.kt','transfer')]:
    REQUIRED[worker] = [f'AudioTransferData.putCompletion(this, {variable}.completedTransfer())', 'OfflineTransferCompletionPolicy.canCommit']

def verify(source):
    for name, markers in REQUIRED.items():
        for marker in markers:
            assert marker in source[name], (name, marker)
    downloader = source['PermittedAudioDownloader.kt']
    assert downloader.count('metrics.markTransferComplete()\n                onVerifying()') == 2
    assert 'Regex' not in source['CompletedAudioTransfer.kt']

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    source = {p: (BASE/p).read_text(encoding='utf-8') for p in set(REQUIRED) | {'PermittedAudioDownloader.kt','CompletedAudioTransfer.kt'}}
    verify(source)
    if args.self_test:
        for name, markers in REQUIRED.items():
            for marker in markers:
                broken = dict(source); broken[name] = broken[name].replace(marker,'BROKEN_FIXTURE')
                try: verify(broken)
                except AssertionError: continue
                raise AssertionError('Accepted broken caller: ' + name + ': ' + marker)
        broken = dict(source); broken['PermittedAudioDownloader.kt'] = broken['PermittedAudioDownloader.kt'].replace('metrics.markTransferComplete()', '')
        try: verify(broken)
        except AssertionError: pass
        else: raise AssertionError('Accepted missing transfer boundary')
    print('PASS FINISH02 clear filters, known display identity and verified frozen completion receipt; device NOT_RUN')
