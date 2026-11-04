"""Actual preview/transport callers; physical session and external returns remain device gates."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
FILES={
 'ui':'v2/ui/NightWaveStudioActivity.kt',
 'widget':'widget/NightWaveWidgetController.kt',
 'service':'playback/PlaybackService.kt'}
MARKERS={
 'ui':['reviewPlaylistText(prepared) {', 'prepared.bytes.toString(Charsets.UTF_8)',
       'setNegativeButton(getString(R.string.nw144_cancel), null)',
       'setPositiveButton(getString(R.string.nw144_continue)) { _, _ -> proceed() }',
       'playlistDocumentRetained.pendingExport = prepared', 'exportPlaylistTextFile.launch(prepared.fileName)',
       'putExtra(Intent.EXTRA_TEXT, text)', 'else showSharedPlaylistPreview(parsed)',
       'if (savedInstanceState == null) handleIncomingShare(intent)',
       'library.importSharedPlaylist(incoming.name, incoming.tracks)'],
 'widget':['WidgetPlaybackIntentPolicy.command(', 'connected.currentMediaItem != null',
           'MediaController.releaseFuture(pendingFuture)', 'main.postDelayed(timeout, 7_000L)'],
 'service':['MediaSessionQueueControlPolicy.trustedController(', 'MediaSessionQueueControlPolicy.canAcknowledgeNext(',
            'event.repeatCount > 0']}
def verify(s):
 for key,markers in MARKERS.items():
  for marker in markers: assert marker in s[key], (key,marker)
 assert s['ui'].count('reviewPlaylistText(prepared) {') == 2
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 source={k:(ROOT/'app/src/main/java/com/nightwave/player'/v).read_text(encoding='utf-8') for k,v in FILES.items()};verify(source)
 if a.self_test:
  for key,markers in MARKERS.items():
   for marker in markers:
    broken=dict(source);broken[key]=broken[key].replace(marker,'BROKEN_CALLER')
    try:verify(broken)
    except AssertionError:continue
    raise AssertionError('Accepted missing caller: '+marker)
 print('PASS NW144 approved snapshot/preview and retained widget/session fences; device NOT_RUN')
