"""NW151 current callers and resource bounds; not runtime/translation certification."""
from pathlib import Path
import argparse,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player'
MARKERS={
 'ui/NightWaveClassicTitleView.kt':['ClassicTextSizePolicy.choose(16, 23)', 'StaticLayout.Builder.obtain', 'TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP','super.onMeasure(widthMeasureSpec, heightMeasureSpec)'],
 'ui/ThumbnailLoader.kt':['val request = Any()', 'closed || into.tag !== request','into.tag === request','NativeThumbnailPolicy.read(input)','NativeThumbnailPolicy.sample(bounds.outWidth, bounds.outHeight, target)','val key = "$target:$url"','declaredBytes > 3_000_000'],
 'ui/NativeThumbnailPolicy.kt':['require(output.size().toLong() + n <= MAX_BYTES)','1_048_576L','coerceIn(64, 1024)'],
 'v2/ui/NightWaveStudioActivity.kt':['val dismissSearchInput = { searchFocus.clearFocus(); searchKeyboard?.hide(); Unit }','Lifecycle.Event.ON_STOP) dismissSearchInput()','searchLifecycle.removeObserver(observer)','val submit = {\n        dismissSearchInput()','onWeb = if (YouTubeReleasePolicy.UNOFFICIAL_SEARCH_ENABLED) null'],
 'offline/PermittedAudioDownloadActivity.kt':['getString(R.string.nw151_rights)','getString(R.string.nw151_boundaries)','getString(R.string.nw151_ready)','minimumHeight = dp(48)','rights.isChecked = false']}
def verify(src,layout):
 for name,markers in MARKERS.items():
  for marker in markers:assert marker in src[name],(name,marker)
 assert 'NightWaveClassicTitleView android:id="@+id/brandText"' in layout
 assert 'android:autoSize' not in layout
 assert 'inSampleSize = 2' not in src['ui/ThumbnailLoader.kt']
 assert 'putInt(' not in src['ui/NightWaveClassicTitleView.kt']
 for bad in ('putString(','.edit()', 'Settings.Global.put','query = ""'):
  # The focus owner alone must not write data, clear drafts or change system settings.
  focus=src['v2/ui/NightWaveStudioActivity.kt'].split('val searchFocus =',1)[1].split('val submit =',1)[0]
  assert bad not in focus,bad
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 src={n:(BASE/n).read_text(encoding='utf-8') for n in MARKERS};layout=(ROOT/'app/src/main/res/layout/activity_main.xml').read_text(encoding='utf-8');verify(src,layout)
 en=ET.parse(ROOT/'app/src/main/res/values/nw151.xml').getroot();bn=ET.parse(ROOT/'app/src/main/res/values-bn/nw151.xml').getroot()
 assert {n.get('name') for n in en}=={n.get('name') for n in bn} and len(en)==19
 assert all(n.text and n.text.strip() for n in bn)
 if a.self_test:
  for name,markers in MARKERS.items():
   for marker in markers:
    broken=dict(src);broken[name]=broken[name].replace(marker,'BROKEN_CALLER')
    try:verify(broken,layout)
    except (AssertionError,IndexError):continue
    raise AssertionError('Accepted missing boundary '+marker)
 print('PASS NW151 classic API23 sizing, bounded original-source thumbnails, focus ownership and19 paired native strings; runtime NOT_RUN')
