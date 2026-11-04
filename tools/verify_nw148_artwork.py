"""Actual NW148 geometry, lifecycle and privacy callers; real decoding remains a device test."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player/v2/ui'
FILES=('StudioPlayerScreens.kt','NightWaveArtworkRequestPolicy.kt')
MARKERS={
 FILES[0]: ['NightWaveArtworkRequestPolicy.pixels(measured.width, measured.height)',
  '.size(requestSize.width, requestSize.height)', '.scale(if (large) Scale.FIT else Scale.FILL)',
  '.precision(Precision.INEXACT)', 'remember(displayUrl, requestSize, large, context)',
  'remember(trackIdentity, url, large) { ArtworkRequestSession() }', 'onDispose { session.close() }',
  'rememberUpdatedState(ticket)', 'rememberUpdatedState(onResolved)', 'rememberUpdatedState(onDecode)',
  'session.accepts(ticket, currentTicket) && candidateIndex == attempt', 'key(ticket)',
  '!loaded.result.isSampled && NightWaveArtworkPolicy.isUnavailableLargeStill(displayUrl',
  '.memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED)',
  'imageLoader ?: context.imageLoader', 'ApplicationInfo.FLAG_DEBUGGABLE', 'Log.d("NightWaveArt", report.toString())'],
 FILES[1]: ['MAX_EDGE = 1280', 'width <= 0 || height <= 0', 'MAX_EDGE.toDouble() / maxOf(width, height)',
  'active && expected.session === this && current.session === this && expected == current',
  'fun close() { active = false }', 'data class ArtworkDecodeReport']}
def verify(src):
 for name,markers in MARKERS.items():
  for marker in markers:assert marker in src[name],(name,marker)
 ui=src[FILES[0]].split('internal fun CoverArt(',1)[1].split('fun StudioNowPlayingOverlay(',1)[0]
 assert '.size(pixels, pixels)' not in ui
 assert ui.count('session.accepts(ticket, currentTicket)')==2
 assert ui.index('session.accepts(ticket, currentTicket)',ui.index('onSuccess =')) < ui.index('resolvedCallback?.invoke')
 report=src[FILES[1]].split('data class ArtworkDecodeReport',1)[1]
 for bad in ('trackIdentity','thumbnail','title','url','account'):assert bad not in report,bad
 for bad in ('memoryCacheKey(', 'diskCacheKey(', 'HttpURLConnection', '.edit()', 'startService('):assert bad not in ui,bad
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 src={f:(BASE/f).read_text(encoding='utf-8') for f in FILES};verify(src)
 if args.self_test:
  for name,markers in MARKERS.items():
   for marker in markers:
    broken=dict(src);broken[name]=broken[name].replace(marker,'BROKEN_CALLER')
    try:verify(broken)
    except (AssertionError,ValueError,IndexError):continue
    raise AssertionError('Accepted missing artwork boundary: '+marker)
 print('PASS NW148 rectangular requests, scale/cache/lifecycle fences and identifier-free debug receipts; device NOT_RUN')
