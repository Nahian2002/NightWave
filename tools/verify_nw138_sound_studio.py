"""NW138 caller contracts and negative fixtures, not an acoustic or device result."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
BASE = 'app/src/main/java/com/nightwave/player/'
PATHS = {
 'ui': BASE+'v2/ui/NightWaveSoundStudio.kt',
 'bar': BASE+'v2/ui/SoundStudioApplyBar.kt',
 'draft': BASE+'playback/SoundStudioDraftPolicy.kt',
 'service': BASE+'playback/PlaybackService.kt',
 'caps': BASE+'playback/EqualizerCapabilities.kt',
}
REQUIRED = {
 'ui': ['val displayedToken = capabilities.commandToken', 'val requestedToken = displayedToken',
  'putExtra(PlaybackService.EXTRA_EQ_SESSION_TOKEN, requestedToken)',
  'latest.commandId == requestId', 'SoundStudioDraftPolicy.adoptManualReadback(requestedToken,',
  'requestedLevels, readback.levelsMb,', 'savedChoice.levelsMb == requestedLevels', 'savedChoice.centersHz == readback.centersHz', 'savedChoice?.preset == requestedPreset && latest.selected == requestedPreset &&', 'EqualizerCommandPolicy.shouldResetDraft(',
  'SoundStudioDraftPolicy.activeLabel(', 'selected = verifiedActive &&',
  'if (advanced) {', 'if (advanced) SoundStudioApplyBar(', 'edited = draftEdited, canApply = validManual, busy = busy',
  'onDiscard = { applySnapshot(resetDraft = true) }', 'issueCommand(PlaybackService.ACTION_EQ_BANDS_APPLY)',
  'SoundStudioPolicy.updateLevel(', 'displayedToken, capabilities.commandToken)) return@Slider',
  'contentDescription = "Equalizer "', 'stateDescription = SoundStudioPolicy.gainLabel(draftLevels[index])',
  'SoundStudioDraftPolicy.adjust(draftLevels[index], delta,', 'SoundStudioTuningPolicy.Shape.entries',
  'AudioTransitionUiPolicy.dialogDetail(selectedCrossfade)', 'not ReplayGain or streaming normalization',
  'repeatOnLifecycle(Lifecycle.State.STARTED)', 'refreshFromEngine(resetDraft = false)'],
 'bar': ['canApply && !busy', 'enabled = !busy', 'heightIn(min = 48.dp)',
  'Draft changes · not applied', 'Manual EQ unavailable · check audio compatibility',
  'Apply manual EQ', 'Discard draft changes', 'liveRegion = LiveRegionMode.Polite'],
 'draft': ['EqualizerCommandPolicy.accepts(requestedToken, currentToken)', 'committedRequested && active && selected == 21',
  'EqualizerReadbackPolicy.inspect(requested, actual)', 'EqualizerReadbackPolicy.Result.DEVICE_QUANTIZED',
  '!fresh -> "Checking current equalizer"', '!active -> if (selected == 0)',
  'SoundStudioPolicy.safeManualRange(minimum, maximum)', 'delta !in setOf(-100, 100)'],
 'service': ['eqCapabilities.publish(probe, session)',
  'EqualizerCommandPolicy.accepts(expected, eqCapabilities.read().commandToken)',
  'val sessionMatches = preset == 0 || eqCommandMatches(intent, sessionId)',
  'val sessionMatches = eqCommandMatches(intent, session)',
  'hasVerifiedActiveEqualizer()', 'equalizerReadback(effect)', 'effect.hasControl()'],
 'caps': ['if (!saved) clear()', 'effect.hasControl()', 'EqualizerCommandPolicy.token(sessionId, centers,'],
}

def verify(s):
 for key, markers in REQUIRED.items():
  for marker in markers:
   assert marker in s[key], (key, marker)
 ui=s['ui']
 assert ui.index('Text("Device presets"') < ui.index('if (advanced) {')
 assert ui.index('} // Advanced draft content') < ui.index('item(key = "nw88-local-audio-transitions")')
 assert ui.index('if (advanced) SoundStudioApplyBar(') > ui.index('Text("Refresh audio device")')
 # No in-scroll copy of the primary manual action; it lives in the sibling bar.
 assert 'Text("Apply manual EQ")' not in ui
 assert 'draftLevels = SoundStudioTuningPolicy.draft(' not in ui
 for forbidden in ['startService', 'setBandLevel', 'getSharedPreferences', 'HttpURLConnection']:
  assert forbidden not in s['bar'] and forbidden not in s['draft']

if __name__ == '__main__':
 p=argparse.ArgumentParser(); p.add_argument('--self-test', action='store_true'); a=p.parse_args()
 source={k:(ROOT/v).read_text(encoding='utf-8') for k,v in PATHS.items()}; verify(source)
 if a.self_test:
  for key,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(source);broken[key]=broken[key].replace(marker,'BROKEN_FIXTURE')
    try: verify(broken)
    except (AssertionError,ValueError): continue
    raise AssertionError('Accepted broken caller: '+key+': '+marker)
 print('PASS NW138 exact readback/draft, simple presets, explicit advanced, persistent Apply, labels/steps, original engine ownership; physical NOT_RUN')
