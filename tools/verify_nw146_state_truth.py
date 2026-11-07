"""Actual state callers; physical phase coverage remains separate."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
FILES={
 'player':'app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt',
 'update':'app/src/main/java/com/nightwave/player/updates/WeeklyUpdates.kt',
 'ui':'app/src/main/java/com/nightwave/player/updates/WeeklyUpdateActivity.kt',
 'transport':'app/src/main/java/com/nightwave/player/updates/WeeklyFeedTransport.kt'}
MARKERS={
 'player':['val currentStatus = strings.playbackStatus(PlaybackHealthPolicy.classify(state.hasTrack,',
           'R.string.nw146_current_track','onRemove = {}, currentStatus = currentStatus',
           'isPlaying && currentStatus == null', 'stateDescription = currentStatus ?:'],
 'update':['WeeklyFeedTransport.read(connection, authenticatedGithub = token != null)',
           'UpdateCheckResult(UpdateCheckStatus.DISABLED)',
           'UpdateCheckPolicy.authenticated(true, code(c), update.code, Build.VERSION.SDK_INT, update.minSdk)',
           'network.activeNetwork == null'],
 'ui':['state.checkStatus == UpdateCheckStatus.DISABLED','state.checkStatus == UpdateCheckStatus.OFFLINE',
       'state.checkStatus == UpdateCheckStatus.UNAVAILABLE','state.checkStatus == UpdateCheckStatus.INVALID_METADATA',
       'catch (_: java.io.IOException)', 'if (state.failed && state.update != null)'],
 'transport':['status != 200','UpdateCheckStatus.UNAVAILABLE','UpdateCheckStatus.PRIVATE_ACCESS','24 * 1024',
              'UpdateCheckStatus.INVALID_METADATA','currentCoroutineContext().ensureActive()', 'connection.disconnect()']}
def verify(src):
 for key,markers in MARKERS.items():
  for marker in markers:assert marker in src[key],(key,marker)
 assert src['update'].index('WeeklyUpdatePolicy.verify(payload, signature,') < src['update'].index('UpdateCheckResult(UpdateCheckStatus.DISABLED)')
 assert 'DISABLED' not in src['transport']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 src={k:(ROOT/v).read_text(encoding='utf-8') for k,v in FILES.items()};verify(src)
 if a.self_test:
  for key,markers in MARKERS.items():
   for marker in markers:
    broken=dict(src);broken[key]=broken[key].replace(marker,'BROKEN_CALLER')
    try:verify(broken)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted missing state boundary: '+marker)
 print('PASS NW146 selected/playing separation and authenticated update status callers; device NOT_RUN')
