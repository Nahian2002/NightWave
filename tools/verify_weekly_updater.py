"""Weekly updater real caller/scope fences; installation remains a device acceptance gate."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
FILES={
 'update':'app/src/main/java/com/nightwave/player/updates/WeeklyUpdates.kt',
 'ui':'app/src/main/java/com/nightwave/player/updates/WeeklyUpdateActivity.kt',
 'launcher':'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt',
 'entry':'app/src/main/java/com/nightwave/player/v2/ui/NightWaveBuildDetailsDialog.kt'}
REQUIRED={
 'update':['WeeklyUpdatePolicy.due(now, p.getLong("checked", 0))','WeeklyUpdatePolicy.RETRY_MS',
  'WeeklyUpdatePolicy.verify(payload, signature,', 'if (!m.getBoolean("enabled"))',
  'update.takeIf { it.code > code(c) }','require(received <= u.bytes)', 'received == u.bytes',
  'candidate.packageName == c.packageName', 'version(candidate) == u.code',
  'certificates(candidate) == certificates(installed)', 'minSdkVersion <= Build.VERSION.SDK_INT',
  'currentCoroutineContext().ensureActive()', 'part.delete(); apk.delete(); throw e',
  'connection.instanceFollowRedirects = false'],
 'ui':['ViewModelProvider(this)[WeeklyUpdateModel::class.java]', 'state.collectAsState()',
  'if (!packageManager.canRequestPackageInstalls())', 'Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES',
  'FileProvider.getUriForFile(this, "$packageName.updates"', 'Intent.FLAG_GRANT_READ_URI_PERMISSION',
  'state.file != null -> TextButton(onClick =', 'onClick = model::download'],
 'launcher':['com.nightwave.player.updates.WeeklyUpdates.onOpen(this)'],
 'entry':['com.nightwave.player.updates.WeeklyUpdateActivity::class.java']}
def verify(source):
 for name, markers in REQUIRED.items():
  for marker in markers:assert marker in source[name], (name,marker)
 assert 'startActivity' not in source['update'].split('suspend fun download(',1)[1]
 assert 'ACTION_DELETE' not in source['ui'] and 'INSTALL_PACKAGES_WITHOUT' not in source['ui']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');a=p.parse_args()
 source={k:(ROOT/v).read_text(encoding='utf-8') for k,v in FILES.items()};verify(source)
 if a.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(source);broken[name]=broken[name].replace(marker,'BROKEN_FIXTURE')
    try:verify(broken)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted missing updater fence: '+marker)
 print('PASS weekly latest-only signed feed and bounded verified APK with explicit OS handoff; device NOT_RUN')
