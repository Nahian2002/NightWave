"""Measurement harness caller/privacy checks; no actual device performance claim."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
PATHS={"android":"app/src/androidTest/java/com/nightwave/player/v2/ui/NW140PerformanceDiagnostic.kt",
       "evidence":"tools/nw140_measurements.py"}
REQUIRED={
 "android":["getString(\"nw140Measure\") == \"true\"", '"nw140-fixture-" + UUID.randomUUID()',
  'base.getSharedPreferences("$suffix-$name", mode)', 'base.deleteSharedPreferences("$suffix-$it")',
  'MusicLibrary(isolated).searchSnapshot()', 'OfflineAudioLibrary(isolated).songs()',
  'rememberNightWaveLocalCorpus(isolated, active = true, refresh = 1)', 'StudioSearch(Modifier, StudioSearchSnapshot()',
  'runBlocking { LocalMusicQueryRunner.run(index, query(target)) }', 'assertEquals(listOf(url(target)), result.saved.map { it.url })',
  'repeat(24)', 'if (iteration >= 4)', 'System.nanoTime() - started',
  'hasTestTag("nightwave-track-search") and hasText(title(target))', 'performScrollToNode(row)',
  'compose.onNode(row).assertIsDisplayed()', 'COMPOSE_AUTO_ADVANCE_DIAGNOSTIC',
  'REAL_MONOTONIC_RUN_BLOCKING', 'FrameMetrics(supplied)', 'frames.length() >= 10_000',
  'Build.VERSION.SDK_INT >= 31', 'FrameMetrics.DEADLINE', 'removeOnFrameMetricsAvailableListener(listener)',
  'if (listenerAttached)', 'ConcurrentHashMap.newKeySet<String>()',
  'WHOLE_FIXTURE_INCLUDING_SETUP_WARMUPS', 'assertEquals(0, actions.get())',
  'fileSha(File(base.applicationInfo.sourceDir))', 'fileSha(File(instrumentation.context.applicationInfo.sourceDir))',
  'base.assets.open("nightwave-build.json")', 'File(base.cacheDir, "nw140-diagnostics")'],
 "evidence":['math.ceil(len(ordered) * fraction)', 'receipt[key] == identity[key]',
  'len(samples) == 40', 'len(values)', 'receipt["fixture_sha256"] == fixture_digest()',
  'receipt["external_actions"] == 0', 'integer(item["elapsed_ns"], 1, 120_000_000_000)',
  'receipt["frame_callbacks_dropped"] == 0', 'not receipt["frames_truncated"]', 'len(usable) >= 100',
  'f["total_ns"] >= f["deadline_ns"]', 'diagnostic_deadline_miss_percent', 'else None',
  'STRUCTURALLY_VALID_DIAGNOSTIC_ONLY', 'DIAGNOSTIC_COMPARISON_ONLY', 'NOT_CERTIFIED',
  '"audible_next": "NOT_RUN"', '"startup_ttid_ttfd": "NOT_RUN"', '"rating": "NOT_ASSESSED"',
  'not target.is_relative_to(ROOT.resolve())', 'object_pairs_hook=unique', 'before[key] == after[key]',
  '"harness_sha256"', 'PREPARED_NOT_EXECUTED']}

def verify(source):
 for name,markers in REQUIRED.items():
  for marker in markers:assert marker in source[name],(name,marker)
 for forbidden in ['Identity.getAuthorizationClient','PlaybackService::class','NightWaveDriveClient(', 'WorkManager.getInstance',
                   'getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE).clear', 'Thread.sleep(']:
  assert forbidden not in source['android'], forbidden
 for forbidden in ['subprocess', 'os.system(', 'adb shell', 'urllib', 'requests.', 'force-stop', 'uninstall']:
  assert forbidden not in source['evidence'], forbidden

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 source={k:(ROOT/v).read_text(encoding='utf-8') for k,v in PATHS.items()};verify(source)
 if a.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(source);broken[name]=broken[name].replace(marker,'BROKEN_FIXTURE')
    try:verify(broken)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted missing measurement fence: '+name+': '+marker)
 print('PASS NW140 opt-in isolated real callers, bounded frames, exact artifact identities and no fabricated performance acceptance; device NOT_RUN')
