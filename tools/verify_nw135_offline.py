"""NW135 import/availability callers. Actual SAF, codec, playback and device acceptance are independent."""
from pathlib import Path
import argparse

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player'
FILES=('offline/OfflineAudioLibrary.kt','offline/OfflineAudioVerifier.kt',
       'offline/OfflineLibraryActivity.kt','v2/ui/NightWaveStudioActivity.kt',
       'offline/OfflineImportJourneyPolicy.kt','offline/OfflineLibraryViewPolicy.kt')

def verify(src):
    library,probe,ui,studio,policy,view=(src[n] for n in FILES)
    importer=library.split('fun import(uri: Uri,',1)[1].split('/** Stream SHA-256;',1)[0]
    assert importer.index('OfflineAudioVerifier.verify(pending)') < importer.index('pending.renameTo(finalFile)') < importer.index('prefs.edit()')
    for marker in ('pending.delete()','finalFile.delete()','audioSha256(pending)','output.fd.sync()'):
        assert marker in importer,marker
    check=library.split('fun verifyAudio(song:',1)[1].split('fun import(',1)[0]
    assert 'fileFor(song)' in check and 'OfflineAudioVerifier.verify(file)' in check
    for forbidden in ('.delete(','.edit()', 'renameTo('): assert forbidden not in check
    for marker in ('MediaExtractor()', 'OfflineAudioValidationPolicy.firstAudioTrack(mimes)',
                   'readSampleData(', 'hasReadableSample(', 'extractor.release()'):
        assert marker in probe,marker
    for forbidden in ('.delete(','.edit()', 'Http'): assert forbidden not in probe
    for marker in ('OfflineImportJourneyPolicy.empty(report.indexReadable, report.entries.size)',
                   'if (trulyEmpty) View.GONE else View.VISIBLE', 'OfflineImportJourneyPolicy.FORMAT_HELP',
                   'if (!report.indexReadable)', 'library.verifyAudio(song)', 'audioCheckFailures.add(song.id)',
                   'entry.copy(health = OfflineFileHealth.UNREADABLE)', 'OpenMultipleDocuments()',
                   'savedInstanceState == null && intent.getBooleanExtra(EXTRA_IMPORT_OWNED, false)',
                   'OfflineImportJourneyPolicy.progress(index + 1, batch.size, phase)', 'OfflineImportJourneyPolicy.result(imported, failures.size,',
                   'registerOnSharedPreferenceChangeListener(indexListener)', 'unregisterOnSharedPreferenceChangeListener(indexListener)',
                   'if (healthRecheckRequested)'):
        assert marker in ui,marker
    assert 'val rawReport = cachedReport ?: run {' in ui
    assert '.putExtra(OfflineLibraryActivity.EXTRA_IMPORT_OWNED, true)' in studio
    assert 'SearchTrackSaveQueue.enqueue(this, video)' in studio
    assert 'showOfflineSources(video)' in studio and 'Other audio sources' in studio
    assert 'indexReadable && indexedCount == 0' in policy
    assert '"Available local files (size checked)"' in view

if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('--self-test',action='store_true'); args=p.parse_args()
    src={n:(BASE/n).read_text(encoding='utf-8') for n in FILES}; verify(src)
    if args.self_test:
        for name,marker in ((FILES[0],'OfflineAudioVerifier.verify(pending)'),
                            (FILES[0],'OfflineAudioVerifier.verify(file)'),
                            (FILES[1],'readSampleData('),
                            (FILES[2],'if (!report.indexReadable)'),
                            (FILES[2],'audioCheckFailures.add(song.id)'),
                            (FILES[2],'registerOnSharedPreferenceChangeListener(indexListener)'),
                            (FILES[2],'val rawReport = cachedReport ?: run {'),
                            (FILES[3],'.putExtra(OfflineLibraryActivity.EXTRA_IMPORT_OWNED, true)'),
                            (FILES[5],'"Available local files (size checked)"')):
            broken=dict(src); broken[name]=broken[name].replace(marker,'BROKEN_FIXTURE')
            try: verify(broken)
            except (AssertionError,ValueError,IndexError): continue
            raise AssertionError('Accepted broken NW135 caller: '+marker)
    print('PASS NW135 pre-commit audio probe, bounded import stages, honest availability, empty/index distinction and retained sources; device NOT_RUN')
