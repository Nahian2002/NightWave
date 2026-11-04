"""NW143 actual callers and retained recovery boundaries; no device certification."""
from pathlib import Path
import argparse
ROOT = Path(__file__).resolve().parents[1]
MARKERS = ['strings.get(R.string.nw143_first_listen) else entry.primary?.detail',
 'if (state.error.isNotBlank()) Text(state.error)',
 'if (state.details.isNotBlank()) Text(state.details)',
 'Text(strings.get(R.string.nw143_search_failed)',
 'Text(strings.get(R.string.nw143_previous_results)',
 'onNativeSearch(query.trim(), mode)', 'onWebSearch(query.trim())',
 'onEmptyAction = if (entry.primary != null) openPrimary else null',
 '"nw47-home-offline", openOffline',
 'var showTrustHelp by rememberSaveable', 'onDismissRequest = { showTrustHelp = false }',
 'R.string.nw143_report_help', 'R.string.nw143_backup_help',
 'R.string.nw143_permission_cancelled', 'R.string.nw143_import_failed',
 'Icons.Outlined.Settings, onShareDiagnostics', 'onClick = onCheckStorage',
 'onClick = if (roomPreviewActive) onUseLegacyReads', 'onOpenClassic)']
def verify(s):
    for marker in MARKERS:
        assert marker in s, marker
    assert 'showSearchDetails && responseMatchesQuery && (state.details.isNotBlank() || state.error.isNotBlank())' in s
    assert 'Text(state.error + " Showing previous results."' not in s
if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--self-test', action='store_true'); args = p.parse_args()
    source = (ROOT/'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt').read_text(encoding='utf-8')
    verify(source)
    if args.self_test:
        for marker in MARKERS:
            try: verify(source.replace(marker, 'BROKEN_CALLER'))
            except AssertionError: continue
            raise AssertionError('Accepted missing caller: '+marker)
    print('PASS NW143 bilingual first-use and recovery caller fences; device NOT_RUN')
