"""NW150 current destination/stop/follow caller fences; actual phone acceptance is separate."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player'
REQUIRED={
 'v2/ui/NightWaveStudioActivity.kt':[
  'onOfflineSearch(offline.title, offline.id)', 'onOfflineSearch(song.title, song.id)',
  '.putExtra(OfflineLibraryActivity.EXTRA_SEARCH_ENTRY_ID, entryId)',
  'nativeSearchState.stop(SearchStopReason.CANCELLED)',
  'nativeSearchState.stop(SearchStopReason.BACKGROUNDED)',
  'NightWaveSearchStoppedCard(state.stopped, state.videos.isNotEmpty(),',
  'onWeb = { dismissSearchInput(); onWebSearch(query.trim()) }',
  'onNativeSearch(query.trim(), mode)', 'request != nativeSearchGeneration',
  'SearchResultPresentationPolicy.matches('],
 'offline/OfflineLibraryActivity.kt':[
  'savedInstanceState?.containsKey("nw150-entry-id") == true',
  'outState.putString("nw150-entry-id", selectedEntryId)',
  'if (!applyingSearchDestination) selectedEntryId = null',
  'availabilityFilter = OfflineLibraryViewPolicy.Filter.ALL\n            applyingSearchDestination = true',
  'finally { applyingSearchDestination = false }',
  'OfflineSearchDestinationPolicy.select(searchText, all, selectedEntryId)',
  'all.none { it.id == selectedEntryId }', 'getString(R.string.nw150_entry_missing)',
  'report.entries.filter {', 'OfflineFileHealth.AVAILABLE'],
 'offline/OfflineSearchDestinationPolicy.kt':[
  'if (selectedId != null) songs.filter { it.id == selectedId }',
  'else OfflineTrackSearch.match(query, songs)', 'it.length <= 128'],
 'v2/ui/SearchStoppedState.kt':['if (!loading) this else copy(loading = false, error = "", stopped = reason)'],
 'v2/ui/NightWaveSearchStoppedCard.kt':[
  'liveRegion = LiveRegionMode.Polite', 'testTag("nw150-search-retry")', 'testTag("nw150-search-web")', 'heightIn(min = 48.dp)',
  'R.string.nw150_results_kept', 'R.string.nw150_search_stopped_hint'],
 'v2/ui/StudioPlayerScreens.kt':[
  'FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)',
  'R.string.nw150_pause_follow else R.string.nw141_follow',
  'testTag("lyrics-follow-toggle")', 'LyricsReadingPolicy.toggledFollow(lyricsAutoFollow)',
  'onClick = onEditLyrics', 'if (lyricsDragged) lyricsAutoFollow = false']}

def verify(src):
 for name,markers in REQUIRED.items():
  for marker in markers: assert marker in src[name],(name,marker)
 for name in ('offline/OfflineSearchDestinationPolicy.kt','v2/ui/SearchStoppedState.kt','v2/ui/NightWaveSearchStoppedCard.kt'):
  for bad in ('.edit()', 'startService(', 'FileOutputStream', 'newCall(', 'playback.play('):
   assert bad not in src[name],(name,bad)
 assert 'onOfflineSearch(song.title)' not in src['v2/ui/NightWaveStudioActivity.kt']
 assert 'onOfflineSearch(offline.title)' not in src['v2/ui/NightWaveStudioActivity.kt']

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 src={name:(BASE/name).read_text(encoding='utf-8') for name in REQUIRED};verify(src)
 if args.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(src);broken[name]=broken[name].replace(marker,'BROKEN_CALLER')
    try:verify(broken)
    except AssertionError:continue
    raise AssertionError('Accepted removed boundary '+name+': '+marker)
 print('PASS NW150 exact offline entry, retained/edit-cleared selection, neutral stopped search and wrapping follow controls; device separate')
