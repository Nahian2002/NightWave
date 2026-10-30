"""Actual adaptive caller and retained input/data boundaries; physical profiles NOT_RUN."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'app/src/main/java/com/nightwave/player/v2/ui'
REQUIRED = {
    'StudioPlayerScreens.kt': ['NightWaveAudioDetailsHeader(advancedAudioOpen)',
        '.heightIn(min = miniLayout.rowHeightDp.dp)', 'item(key = "queue-summary")',
        'testTag("nw142-queue-content")', 'onMoveUpcoming?.invoke(from, to, editSnapshot)',
        'onClearUpcoming?.invoke(reviewed)', 'requestRemove(index, video.url)',
        'if (result == SnackbarResult.ActionPerformed) onUndoRemoval(token)'],
    'NightWaveSoundStudio.kt': ['NightWaveAdaptiveViewportPolicy.DIALOG_MAX_WIDTH_DP.dp',
        'headerMaxHeightDp(maxHeight.value)', 'footerMaxHeightDp(maxHeight.value)',
        'Modifier.heightIn(max = headerLimit).verticalScroll(rememberScrollState())',
        'modifier = Modifier.heightIn(max = footerLimit).verticalScroll(rememberScrollState())',
        'if (advanced) SoundStudioApplyBar(', 'onDiscard = { applySnapshot(resetDraft = true) }'],
    'NightWaveAccountHub.kt': ['headerMaxHeightDp(maxHeight.value)',
        'Modifier.heightIn(max = headerLimit).verticalScroll(rememberScrollState())',
        'openExternalFlow(onPreviewRestore)', 'openExternalFlow(onCompare)'],
    'NightWaveStudioActivity.kt': ['LaunchedEffect(tab) {', 'focus.clearFocus()', 'keyboard?.hide()',
        'windowInsetsPadding(WindowInsets.safeDrawing)', 'contentWindowInsets = WindowInsets(0, 0, 0, 0)',
        'KeyboardActions(onSearch = { submit() })', 'lyricsImportRetained.review',
        'var selectedPlaylist by rememberSaveable', 'var expanded by rememberSaveable'],
    'NightWaveAudioDetailsHeader.kt': ['stackDetail(maxWidth.value, LocalDensity.current.fontScale)',
        'heightIn(min = 50.dp)', 'if (stack) Column(', 'Column(Modifier.weight(1f)',
        'Surface(onClick = onToggle,'],
    'SoundStudioApplyBar.kt': ['modifier: Modifier = Modifier', 'Column(modifier.fillMaxWidth()',
        'canApply && !busy', 'onClick = onApply', 'onClick = onDiscard']
}

def verify(source):
    for name, markers in REQUIRED.items():
        for marker in markers: assert marker in source[name], (name, marker)
    queue = source['StudioPlayerScreens.kt'].split('fun StudioQueueSheet(', 1)[1].split('private fun SwipeUpcomingRow', 1)[0]
    assert queue.count('LazyColumn(') == 1
    assert queue.index('LazyColumn(') < queue.index('item(key = "queue-summary")') < queue.index('item(key = "queue-playing-header")')
    activity = source['NightWaveStudioActivity.kt']
    focus = activity.split('LaunchedEffect(tab) {', 1)[1].split('    }', 1)[0]
    assert 'query =' not in focus and 'onPrepareSearch' not in focus and 'searchHistoryStore' not in focus
    assert '.imePadding()' not in activity, 'Do not double apply consumed safeDrawing IME insets'

if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--self-test', action='store_true'); args = p.parse_args()
    source = {name: (BASE/name).read_text(encoding='utf-8') for name in REQUIRED}; verify(source)
    if args.self_test:
        for name, markers in REQUIRED.items():
            for marker in markers:
                broken = dict(source); broken[name] = broken[name].replace(marker, 'BROKEN_FIXTURE')
                try: verify(broken)
                except (AssertionError, ValueError): continue
                raise AssertionError('Accepted broken actual adaptive caller: ' + name + ': ' + marker)
    print('PASS NW142 measured dialog budgets, scrollable queue summary, growing actions and retained input/identity callers; physical NOT_RUN')
