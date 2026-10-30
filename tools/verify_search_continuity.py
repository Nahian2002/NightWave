"""SEARCH02 bounded public cache, source continuity, refresh and request-owned phase fences."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player'
REQUIRED={
 'net/PublicSearchResultCache.kt':['MAX_ENTRIES = 12','MAX_ENTRY_UNITS = 50_000','MAX_TEXT_UNITS = 250_000',
  'FRESH_MS = 120_000L','STALE_MS = 600_000L','result.videos.size !in 1..55',
  'VideoLink.normalizeSearchResult(it.url) != it.url','if (units > MAX_ENTRY_UNITS) return false',
  'now - it.at !in 0 until STALE_MS','entries.size > MAX_ENTRIES || textUnits > MAX_TEXT_UNITS',
  'result.videos.toList()', 'private data class Key(val query: String, val mode: SearchMode)'],
 'net/YouTubeGateway.kt':['private val memoryCache = PublicSearchResultCache()',
  'if (!forceRefresh) cachedSearch(text, mode)?.takeIf { it.fresh }',
  'request.commitIfActive { memoryCache.put(query, mode, result) }',
  'request?.beginPhase(PublicSearchPhase.DIRECT)', 'request?.beginPhase(PublicSearchPhase.COMMUNITY)',
  'remember(text, mode, result, request)'],
 'net/PublicMusicSearchRunner.kt':['directBudgetMillis: Long = 8_000L',
  'minOf(deadline, phaseDeadline)', 'throw PublicSearchPhaseDeadlineException()',
  'diagnostics.httpCompleted(System.nanoTime() - started)',
  'commitIfActive(block: () -> T): T = synchronized(calls) { checkActive(); block() }',
  'TIMEOUT_MS = 20_000L', 'finally { request.cancel() }', 'calls.toList().forEach { it.cancel() }'],
 'v2/ui/NightWaveStudioActivity.kt':['val recent = nativeGateway.cachedSearch(clean, mode)',
  'val forceRefresh = previous?.videos?.isNotEmpty() == true',
  'nativeGateway.search(clean, mode, forceRefresh = forceRefresh)',
  'finally { searchDetails = searchRequest.details() }',
  'videos = carriedVideos, provider = carriedProvider',
  'request != nativeSearchGeneration', '}\n        if (responseMatchesQuery && state.videos.isNotEmpty())',
  'R.string.nw141_recent_results_updating_0c5f7', 'testTag("search02-refresh")',
  'if (showSearchDetails && responseMatchesQuery && state.details.isNotBlank())',
  'fromCache = response.fromCache, details = searchDetails'],
}
def verify(source):
 for name,markers in REQUIRED.items():
  for marker in markers:assert marker in source[name],(name,marker)
 cache=source['net/PublicSearchResultCache.kt']
 for forbidden in ['SharedPreferences','FileOutputStream','getSharedPreferences','newCall(','.edit()','object PublicSearchResultCache']:
  assert forbidden not in cache,forbidden
 metrics=source['net/PublicSearchDiagnostics.kt']
 for forbidden in ['VideoPreview','Request)', '.url', 'query:', 'headers', 'body.string', 'Log.']:
  assert forbidden not in metrics,forbidden
 assert '.cancelAll(' not in source['net/PublicMusicSearchRunner.kt']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 source={n:(BASE/n).read_text(encoding='utf-8') for n in set(REQUIRED)|{'net/PublicSearchDiagnostics.kt'}};verify(source)
 if a.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(source);broken[name]=broken[name].replace(marker,'BROKEN_FIXTURE')
    try:verify(broken)
    except AssertionError:continue
    raise AssertionError('Accepted broken caller: '+name+': '+marker)
 print('PASS SEARCH02 bounded activity/public-only cache, visible matching refresh, owned phase deadlines, guarded commits and numeric diagnostics; device NOT_RUN')
