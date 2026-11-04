"""COMPAT01 API23 caller fences; does not certify device compatibility."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[1]
REQUIRED={
 'app/build.gradle.kts':['minSdk = 23','targetSdk = 36','isCoreLibraryDesugaringEnabled = true','desugar_jdk_libs_nio:2.1.5'],
 'app/src/main/java/com/nightwave/player/updates/WeeklyUpdateActivity.kt':['android.os.Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()'],
 'app/src/main/java/com/nightwave/player/updates/WeeklyUpdates.kt':['require(Build.VERSION.SDK_INT >= 24 || update.minSdk != null)',
     'val archiveMinimum = if (Build.VERSION.SDK_INT >= 24)',
     'WeeklyUpdatePolicy.minimumSdkAccepted(Build.VERSION.SDK_INT, archiveMinimum, u.minSdk)'],
 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStrings.kt':['ConfigurationCompat.getLocales(resources.configuration)'],
 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveTheme.kt':['ConfigurationCompat.getLocales(LocalConfiguration.current)'],
 'app/src/main/java/com/nightwave/player/ui/ThumbnailLoader.kt':['getHeaderField("Content-Length")?.trim()?.toLongOrNull()', 'declaredBytes > 3_000_000'],
 'tools/sign-weekly-update.ps1':['aapt.exe', '$payload.minSdk = [int]$Matches[1]'],
 'tools/verify-apk-signature.ps1':['verify --verbose --print-certs', "sdkVersion:'23'", 'Candidate must declare the reviewed Android6/API23 minimum.']}
def verify(src):
 for name,markers in REQUIRED.items():
  for marker in markers:assert marker in src[name],(name,marker)
 assert '--min-sdk-version 26' not in src['tools/verify-apk-signature.ps1']
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 src={name:(ROOT/name).read_text(encoding='utf-8') for name in REQUIRED};verify(src)
 if args.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(src);broken[name]=broken[name].replace(marker,'BROKEN_COMPAT_CALLER')
    try:verify(broken)
    except AssertionError:continue
    raise AssertionError('Accepted missing compatibility fence: '+marker)
 print('PASS API23 backport callers and signed minimum metadata; runtime NOT_RUN')
