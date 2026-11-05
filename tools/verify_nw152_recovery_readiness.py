"""Actual guest document and installer-provider fences. Runtime/account acceptance is separate."""
from pathlib import Path
import argparse, base64, json, xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'app/src/main/java/com/nightwave/player'
MARKERS = {
    'v2/ui/NightWaveStudioActivity.kt': [
        'ViewModelProvider(this)[GuestBackupRetainedState::class.java]',
        'withContext(Dispatchers.IO) { runCatching { archiveManager.export() } }',
        'slot.prepare(bytes)', 'slot.complete(false)', 'guestBackupRetained.export.complete(uri != null)',
        'if (snapshot == null)', 'contentResolver.openOutputStream(uri, "wt")', 'out.write(snapshot)',
        'archiveManager.preview(GuestBackupDocument.read(input))', 'archive to archiveManager.guestRestoreSnapshot()',
        'archiveManager.restoreGuestIfUnchanged(expected, archive)', 'R.string.nw152_restore_stale'],
    'v2/data/GuestBackupDocument.kt': ['check(!hasPending)', 'pending = bytes.copyOf()',
        'pending = null', 'if (hasDestination) snapshot else null',
        'require(output.size().toLong() + count <= LibraryBackupCodec.MAX_BYTES)'],
    'v2/data/LibraryArchiveManager.kt': ['guestRestoreSnapshot(): GuestRestoreSnapshot = LegacyLibraryTransaction.exclusive',
        'if (guestRestoreSnapshot() != expected) CloudRestoreApplyResult.STALE_PREVIEW',
        'else if (restoreWithConfirmation(archive)) CloudRestoreApplyResult.APPLIED']}

def verify(source):
    for name, markers in MARKERS.items():
        for marker in markers:
            assert marker in source[name], (name, marker)
    callback = source['v2/ui/NightWaveStudioActivity.kt'].split('private val exportFile =', 1)[1].split('private val importFile =', 1)[0]
    assert 'archiveManager.export()' not in callback
    assert callback.index('if (snapshot == null)') < callback.index('openOutputStream')

if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--self-test', action='store_true'); args = p.parse_args()
    source = {name: (BASE / name).read_text(encoding='utf-8') for name in MARKERS}
    verify(source)
    android = '{http://schemas.android.com/apk/res/android}'
    manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    provider = next(n for n in manifest.findall('./application/provider') if n.get(android+'authorities') == '${applicationId}.updates')
    assert provider.get(android+'exported') == 'false' and provider.get(android+'grantUriPermissions') == 'true'
    paths = list(ET.parse(ROOT / 'app/src/main/res/xml/weekly_update_paths.xml').getroot())
    assert len(paths) == 1 and paths[0].tag == 'cache-path' and paths[0].get('path') == 'approved-updates/'
    envelope = json.loads((ROOT/'updates/stable.json').read_text(encoding='utf-8-sig'))
    assert json.loads(base64.b64decode(envelope['payload'], validate=True))['enabled'] is False
    en, bn = [{n.get('name') for n in ET.parse(ROOT/f'app/src/main/res/{folder}/nw152.xml').getroot()} for folder in ('values','values-bn')]
    assert en == bn and len(en) == 11
    if args.self_test:
        for name, markers in MARKERS.items():
            for marker in markers:
                broken = dict(source); broken[name] = broken[name].replace(marker, 'BROKEN_CALLER')
                try: verify(broken)
                except (AssertionError, ValueError, IndexError): continue
                raise AssertionError('Accepted missing document fence '+marker)
    print('PASS NW152 one-shot guest snapshot, stale replacement fence, private provider root and disabled feed; runtime NOT_RUN')
