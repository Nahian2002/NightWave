"""Current theme/motion caller and read-only lifecycle fences; not rendered acceptance."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player/v2/ui'
MARKERS={
 'NightWaveTheme.kt':['rememberNightWaveReducedMotion(reduceMotion, followSystemMotion)',
  'inversePrimary = paletteColor(NightWaveThemeMotionPolicy.inverseAction(palette))',
  'LocalNightWaveReduceMotion provides effectiveReduceMotion'],
 'NightWaveSystemMotion.kt':['if (userReduced || !followSystem) return userReduced',
  'Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)',
  'resolver.registerContentObserver(', 'resolver.unregisterContentObserver(observer)',
  'override fun onChange(selfChange: Boolean) { observation.refresh() }',
  'Lifecycle.Event.ON_START -> observation.start()',
  'Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> observation.stop()',
  'lifecycle.removeObserver(listener)', 'onDispose {', 'Lifecycle.State.STARTED',
  'LocalLifecycleOwner.current.lifecycle'],
 'NightWaveThemeMotionPolicy.kt':['palette.accentSoft',
  'systemScale == null || !systemScale.isFinite() || systemScale <= 0f',
  'if (active) return', 'if (active) publish(runCatching { read() }.getOrNull())',
  'if (!active) return', 'active = false', 'runCatching { unsubscribe() }'],
 'NightWaveStudioActivity.kt':['followSystemMotion = profile.motion != MotionPreference.FULL',
  'val reducedMotion = LocalNightWaveReduceMotion.current',
  'StudioTab.entries.size, reducedMotion))', 'reducedMotion = reducedMotion,',
  'reduceMotion = LocalNightWaveReduceMotion.current,', 'reduceMotion = reducedMotion',
  'focus.clearFocus()', 'keyboard?.hide()', 'NightWavePagerRecovery(pagerState, shelfGestures.active)']}
def verify(src):
 for name,markers in MARKERS.items():
  for marker in markers:assert marker in src[name],(name,marker)
 observer=src['NightWaveSystemMotion.kt']
 for bad in ('putFloat(', 'putInt(', 'putString(', '.edit()', 'startService(', 'Http', 'delay('):
  assert bad not in observer,bad
 root=src['NightWaveStudioActivity.kt'].split('private fun StudioRoot(',1)[1].split('internal fun StudioHome(',1)[0]
 assert 'profile.motion == MotionPreference.REDUCED' not in root
 assert root.count('reduceMotion = reducedMotion')==2
 # Never lose lifecycle cleanup while keeping the marker in an unrelated branch.
 assert 'lifecycle.removeObserver(listener)\n            observation.stop()' in observer
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 src={f:(BASE/f).read_text(encoding='utf-8') for f in MARKERS};verify(src)
 if args.self_test:
  for name,markers in MARKERS.items():
   for marker in markers:
    broken=dict(src);broken[name]=broken[name].replace(marker,'BROKEN_CALLER')
    try:verify(broken)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted missing theme/motion boundary: '+marker)
 print('PASS NW149 selected inverse role, effective motion callers and read-only lifecycle cleanup; device separate')
