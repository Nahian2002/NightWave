# NW27 Windows PowerShell 5.1 safety gate; source inspection only.
# No network, user music, sign-in, private keys or Android device access.
[CmdletBinding()]
param([switch]$SelfTest)

$ErrorActionPreference = 'Stop'

function Has-Source([string]$Body, [string]$Needle) {
    return $Body.Contains($Needle)
}

if ($SelfTest) {
    if (-not (Has-Source 'working health check' 'health')) { throw 'Synthetic match failed.' }
    if (Has-Source 'normal text' 'SECRET') { throw 'Synthetic negative check failed.' }
    Write-Host 'PASS: NW27 Windows source preflight positive and negative fixtures.'
    exit 0
}

$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$checks = @(
    @('app\src\main\java\com\nightwave\player\NightWaveApp.kt',
        'catch (error: LinkageError)'),
    @('app\src\main\java\com\nightwave\player\diagnostics\NightWaveCrashRecorder.kt',
        'NightWaveCrashPrivacyPolicy.threadLabel(thread.name)'),
    @('app\src\main\java\com\nightwave\player\diagnostics\NightWaveCrashRecorder.kt',
        'NightWaveCrashPrivacyPolicy.safeSourceSymbol('),
    @('app\src\main\java\com\nightwave\player\v2\cloud\NightWaveCloudWorker.kt',
        'catch (cancel: CancellationException)'),
    @('app\src\main\java\com\nightwave\player\v2\cloud\NightWaveCloudWorker.kt',
        'throw cancel'),
    @('app\src\main\java\com\nightwave\player\v2\cloud\CloudBackupConsentPolicy.kt',
        'expected.revision == current.revision'),
    @('app\src\main\java\com\nightwave\player\v2\cloud\CloudBackupState.kt',
        'fun successIfCurrent('),
    @('app\src\main\java\com\nightwave\player\v2\cloud\NightWaveCloudWorker.kt',
        'store.statusIfCurrent('),
    @('app\src\main\java\com\nightwave\player\v2\cloud\NightWaveDriveClient.kt',
        'DriveResponseReadPolicy.canAppend('),
    @('app\src\main\java\com\nightwave\player\v2\cloud\NightWaveDriveClient.kt',
        'DriveResponseReadPolicy.stalled('),
    @('app\src\main\java\com\nightwave\player\v2\cloud\NightWaveCloudAuthorization.kt',
        'https://www.googleapis.com/auth/drive.appdata'),
    @('app\src\main\java\com\nightwave\player\offline\OfflineAudioLibrary.kt',
        'nightwave_offline_v1'),
    @('app\src\main\AndroidManifest.xml', 'android:allowBackup="false"'),
    @('app\src\main\AndroidManifest.xml', 'android:usesCleartextTraffic="false"'),
    @('tools\verify-apk-signature.ps1', 'TrustedSignerSHA256'),
    @('app\build.gradle.kts', 'applicationId = "com.nightwave.player"'),
    @('app\build.gradle.kts', 'versionCode = 35')
)
$failureCount = 0
foreach ($entry in $checks) {
    $relative = [string]$entry[0]
    $needle = [string]$entry[1]
    $full = Join-Path $project $relative
    if (-not (Test-Path -LiteralPath $full -PathType Leaf)) {
        Write-Host ("FAIL: NW27 source file missing: " + $relative)
        $failureCount++
        continue
    }
    $text = [System.IO.File]::ReadAllText(
        $full, [System.Text.Encoding]::UTF8)
    if (-not (Has-Source $text $needle)) {
        Write-Host ("FAIL: NW27 safety check missing in " + $relative)
        $failureCount++
    }
}
if ($failureCount -gt 0) {
    Write-Host ("FAIL: NW27 Windows source preflight (" + $failureCount + " issue(s)).")
    exit 1
}
Write-Host 'PASS: NW27 Windows 17-source privacy, startup, backup and release safety checks.'
Write-Host 'LIMIT: This is not an Android compiler or on-device playback test.'
exit 0
