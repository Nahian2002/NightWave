"""NW147 actual layout callers and preservation, not rendered acceptance."""
from pathlib import Path
import argparse
ROOT = Path(__file__).resolve().parents[1]
BASE = 'app/src/main/java/com/nightwave/player/v2/ui/'
FILES = {'settings': BASE+'NightWaveSecondaryComponents.kt', 'activity': BASE+'NightWaveStudioActivity.kt',
         'sound': BASE+'NightWaveSoundStudio.kt', 'queue': BASE+'StudioPlayerScreens.kt', 'home': BASE+'NightWaveVisualLanguage.kt'}
REQUIRED = {
 'settings': ['NightWaveSecondaryLayoutPolicy.SETTINGS_MIN_DP.dp', 'NightWaveSecondaryLayoutPolicy.SETTINGS_VERTICAL_DP.dp',
              'stackSettingAction(maxWidth.value, scale)', 'identity(Modifier.fillMaxWidth())', 'identity(Modifier.weight(1f))',
              'heightIn(min = 48.dp).widthIn(min = 48.dp)', 'rememberSaveable', 'if (expanded) Column',
              'stateDescription = strings.get(if (expanded)', 'Text(detail, style = MaterialTheme.typography.bodySmall'],
 'activity': ['NightWaveSettingsRow(title, detail, icon, onClick, modifier, trailing)', 'NightWaveSecondaryLayoutPolicy.GROUP_CAPTION_BOTTOM_DP.dp'],
 'sound': ['NightWaveSecondaryLayoutPolicy.SOUND_PADDING_DP.dp', 'NightWaveSecondaryLayoutPolicy.SOUND_GAP_DP.dp',
           'NightWaveSecondaryLayoutPolicy.STATUS_GAP_DP.dp', 'NightWaveTechnicalDetails(strings.get(R.string.nw141_playback_compatibility_2ec06))',
           'SoundStudioDraftPolicy.activeLabel(', 'if (liveConfirmed) status.message', 'Text(tuningStatus,',
           'if (notice.isNotBlank())', 'onClick = { discover() }', 'if (advanced) SoundStudioApplyBar('],
 'queue': ['NightWaveSecondaryLayoutPolicy.QUEUE_GAP_DP.dp', 'NightWaveSecondaryLayoutPolicy.QUEUE_SUMMARY_BOTTOM_DP.dp',
           'CoverArt(artwork, Modifier.size(45.dp), 10)', 'currentStatus = currentStatus', 'testTag("nw134-clear-upcoming")', 'testTag("nw134-edit-order")'],
 'home': ['NightWaveSecondaryLayoutPolicy.MIX_VERTICAL_DP.dp', 'NightWaveSecondaryLayoutPolicy.MIX_CAPTION_DP.dp', 'if (featured) 88.dp else 64.dp']}

def verify(src):
 for key, markers in REQUIRED.items():
  for marker in markers: assert marker in src[key], (key, marker)
 for bad in ('maxLines', 'Ellipsis', '.height(', 'SharedPreferences', 'startService', 'PlaybackService'):
  assert bad not in src['settings'], bad
 disclosure = src['sound'].split('NightWaveTechnicalDetails(', 1)[1].split('if (notice.isNotBlank())', 1)[0]
 assert 'capabilities.centersHz.size' in disclosure and 'Effects depend on Android' in disclosure
 for bad in ('issueCommand(', 'onDiscard', 'status.message', 'Text(tuningStatus'):
  assert bad not in disclosure, bad
 if (ROOT/'app/src/main/res/values/nw147.xml').read_text().count('<string ') != 2: raise AssertionError('English details states')
 if (ROOT/'app/src/main/res/values-bn/nw147.xml').read_text().count('<string ') != 2: raise AssertionError('Bangla details states')

if __name__ == '__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 src={k:(ROOT/f).read_text(encoding='utf-8') for k,f in FILES.items()};verify(src)
 if args.self_test:
  for key, markers in REQUIRED.items():
   for marker in markers:
    broken=dict(src);broken[key]=broken[key].replace(marker,'BROKEN_CALLER')
    try:verify(broken)
    except (AssertionError, IndexError):continue
    raise AssertionError('Accepted missing layout caller: '+marker)
 print('PASS NW147 compact callers, intrinsic text, preserved artwork/actions and details boundary; rendered acceptance NOT_RUN')
