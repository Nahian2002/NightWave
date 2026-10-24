# NightWave Phase 6V: read-only cross-phase cloud/build preflight.
# PowerShell 5.1 compatible. NO Google access, no Gradle, no APK, no device I/O.
[CmdletBinding()]
param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$issues = New-Object 'System.Collections.Generic.List[string]'
function Source([string]$Relative) {
    $absolute = Join-Path $root $Relative
    if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) {
        $issues.Add("Missing expected file: $Relative")
        return ''
    }
    return [System.IO.File]::ReadAllText($absolute)
}
function Require([string]$Body, [string]$Fragment, [string]$Label) {
    if (-not $Body.Contains($Fragment)) { $issues.Add("Missing $Label") }
}
function Forbid([string]$Body, [string]$Fragment, [string]$Label) {
    if ($Body.Contains($Fragment)) { $issues.Add("Unsafe $Label") }
}
if ($SelfTest) {
    Require 'ready' 'ready' 'synthetic valid'
    if ($issues.Count -ne 0) { throw 'Phase 6V valid-case self-test failed.' }
    Require 'ready' 'absent' 'synthetic invalid'
    if ($issues.Count -ne 1) { throw 'Phase 6V invalid-case self-test failed.' }
    $issues.Clear()
    Forbid 'safe' 'secret' 'synthetic absence'
    if ($issues.Count -ne 0) { throw 'Phase 6V absence self-test failed.' }
    Forbid 'secret' 'secret' 'synthetic rejection'
    if ($issues.Count -ne 1) { throw 'Phase 6V rejection self-test failed.' }
    Write-Host 'PASS: 6V synthetic verifier detects missing and forbidden contracts.'
    exit 0
}
$b = 'app\src\main\java\com\nightwave\player\'
$manifest = Source 'app\src\main\AndroidManifest.xml'
$gradle = Source 'app\build.gradle.kts'
$workflow = Source '.github\workflows\android.yml'
$windows = Source 'tools\build-windows.ps1'
$backup = Source ($b + 'v2\cloud\CloudBackupCodec.kt')
$state = Source ($b + 'v2\cloud\CloudBackupState.kt')
$auth = Source ($b + 'v2\cloud\NightWaveCloudAuthorization.kt')
$cloud = Source ($b + 'v2\cloud\NightWaveDriveClient.kt')
$backupWorker = Source ($b + 'v2\cloud\NightWaveCloudWorker.kt')
$consentPolicy = Source ($b + 'v2\cloud\CloudBackupConsentPolicy.kt')
$recovery = Source ($b + 'v2\data\LibraryArchiveManager.kt')
$lyrics = Source ($b + 'v2\lyrics\PrivateLyricsStore.kt')
$lockerAuth = Source ($b + 'v2\locker\NightWaveLockerAuthorization.kt')
$lockerClient = Source ($b + 'v2\locker\NightWaveMusicDriveClient.kt')
$lockerUI = Source ($b + 'v2\locker\NightWaveLockerActivity.kt')
$checkpoint = Source ($b + 'v2\locker\LockerDownloadCheckpointStore.kt')
$offline = Source ($b + 'offline\OfflineAudioLibrary.kt')
$policy = Source ($b + 'v2\locker\LockerUploadResumePolicy.kt')
$range = Source ($b + 'v2\locker\LockerResumePolicy.kt')
$sourceGate = Source 'tools\verify_v2_phase6v.py'
$files = @(
    'tools\verify_v2_phase6p.py', 'tools\verify_v2_phase6q1.py',
    'tools\verify_v2_phase6q2.py', 'tools\verify_v2_phase6q3.py',
    'tools\verify_v2_phase6r1.py', 'tools\verify_v2_phase6r2.py',
    'tools\verify_v2_phase6r3.py', 'tools\verify_v2_phase6r3b.py',
    'app\src\test\java\com\nightwave\player\v2\cloud\CloudOptionalDataTest.kt',
    'app\src\test\java\com\nightwave\player\v2\locker\LockerDownloadPolicyTest.kt',
    'app\src\test\java\com\nightwave\player\v2\locker\LockerResumePolicyTest.kt',
    'app\src\test\java\com\nightwave\player\v2\locker\LockerUploadResumePolicyTest.kt'
)
foreach ($relative in $files) { [void](Source $relative) }

Require $gradle 'applicationId = "com.nightwave.player"' 'fixed package name'
Require $gradle 'versionCode = 35' 'expected version code'
Require $gradle 'versionName = "2.0.0-alpha5s"' 'expected version name'
Require $gradle 'testImplementation("junit:junit:4.13.2")' 'JVM test dependency'
Require $gradle 'testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")' 'HTTP test dependency'
Require $manifest 'android:allowBackup="false"' 'no unrestricted Android backup'
Require $manifest 'android:usesCleartextTraffic="false"' 'TLS-only app'
Require $manifest 'NightWaveLockerActivity" android:exported="false"' 'private locker screen'

Require $auth 'https://www.googleapis.com/auth/drive.appdata' 'restricted JSON backup scope'
Require $lockerAuth 'https://www.googleapis.com/auth/drive.file' 'separate locker scope'
Require $backup 'version == 2 && keys == oldKeys' 'legacy v2 backup decoding'
Require $backup 'version == 3 && keys == oldKeys +' 'optional v3 backup decoding'
Require $backup 'digest(payload)' 'backed up checksums'
Require $state 'includeLyrics: Boolean = false' 'lyrics opt-in default'
Require $state 'includePreferences: Boolean = false' 'preferences opt-in default'
Require $backupWorker 'CloudBackupConsentPolicy.same(consent,' 'worker consent recheck'
Require $backupWorker 'beforeUpload.consentSnapshot()' 'worker current privacy/settings snapshot recheck'
Require $backupWorker 'store.successIfCurrent(consent,' 'worker consent-fenced success write'
Require $backupWorker 'store.statusIfCurrent(consent,' 'worker consent-fenced status write'
Require $consentPolicy 'expected.lyrics == current.lyrics' 'worker lyrics privacy opt-in'
Require $consentPolicy 'expected.preferences == current.preferences' 'worker preferences privacy opt-in'
Require $consentPolicy 'expected.revision == current.revision' 'worker same-account reconnect fence'
Require $state 'putLong(REVISION_KEY, nextRevision())' 'persistent backup consent revision'
Require $cloud 'backUpIfChangedVerified(bytes: ByteArray)' 'verified smart backup'
Require $cloud 'appDataFolder' 'metadata backup appDataFolder'
Require $recovery 'restoreCloudIfUnchanged(' 'restore preview-fence'
Require $recovery 'CloudRestoreApplyResult.STALE_PREVIEW' 'stale-restore refusal'
Require $lyrics '!existing.containsKey("lrc:" + it.localSongId)' 'no LRC overwrite'
Require $lockerClient 'fun upload(song: OfflineSong, file: File, progress: (Int) -> Unit)' 'audio upload'
Require $lockerClient 'fun downloadWithRecovery(' 'resumable audio download'
Require $lockerClient 'private fun queryUploadStatus(' 'Drive-upload status probe'
Require $lockerClient 'LockerAudioPolicy.hashes(partial)' 'download integrity'
Require $lockerClient 'LockerDriveReceiptPolicy.requireCompletedUpload(' 'upload integrity delegate'
Require $lockerClient 'progress(100)' 'only verified transfer completion'
Require $lockerClient 'coerceAtMost(99)' 'upload progress cap'
Require $lockerUI 'setTitle("Resume verified download?")' 'explicit download resume'
Require $lockerUI 'Upload could not be verified.' 'safe upload failure feedback'
Require $checkpoint 'AtomicFile(File(root,' 'atomic local transfer journal'
Require $policy 'MAX_CONSECUTIVE_FAILURES = 3' 'bounded upload retries'
Require $range 'MAX_ATTEMPTS = 3' 'bounded download retries'
Require $offline 'importVerifiedCloud(' 'checksummed offline import'
Require $offline 'CloudOfflineImportResult(song, created = false' 'existing audio dedupe'

Forbid $lockerClient '.method("DELETE"' 'remote Drive media deletion'
Forbid $lockerClient '.method("PATCH"' 'remote Drive mutation'
Forbid $lockerClient '.addQueryParameter("spaces", "appDataFolder")' 'locker reading hidden JSON backups'
Forbid $checkpoint 'access_token' 'checkpoint token leak'
Forbid $checkpoint 'refresh_token' 'checkpoint refresh-token leak'
Forbid $checkpoint 'sessionUrl' 'persisted upload-session capability'

Require $workflow ':app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin' 'compiled Android/JVM/test gate'
Require $workflow 'python3 tools/verify_v2_phase6r3b.py' 'previous cloud upload safety gate'
Require $workflow 'python3 tools/verify_v2_phase6v.py' 'current cross-phase source gate'
Require $windows 'check-phase6p-integration.ps1' 'prior integration gate'
Require $windows 'check-phase6v-integration.ps1' 'new integration gate'
Require $windows 'verify-apk-signature.ps1' 'APK signer continuity'
Require $sourceGate 'def main() -> int:' 'portable integration source checker'

if ($issues.Count -gt 0) {
    Write-Host ('FAIL: Phase 6V cross-phase preflight found {0} issue(s):' -f $issues.Count) -ForegroundColor Red
    $issues | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host 'PASS: NightWave 6V cross-phase cloud backup, music locker and Android build source contracts.' -ForegroundColor Green
Write-Host 'UNVERIFIED: Kotlin compilation, JUnit, Google OAuth, Drive E2E and APK signing until Gradle runs.' -ForegroundColor Yellow
exit 0
