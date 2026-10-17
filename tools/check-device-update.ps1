# NightWave Phase 5I: read-only compatibility check for one connected Android device.
# Never installs, uninstalls, force-stops, clears data or opens private storage.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $root 'dist'
$reportPath = Join-Path $dist 'NightWave-device-update-check.txt'
$apk = Join-Path $dist 'NightWave-debug.apk'
$status = Join-Path $dist 'NightWave-build-status.txt'
$buildTools = Join-Path $env:LOCALAPPDATA 'NightWaveBuild\android-sdk\build-tools\35.0.0'
$versionName = '2.0.0-alpha5k'
$versionCode = 27
$packageName = 'com.nightwave.player'
$checked = 'INCONCLUSIVE'
$details = 'Device and APK compatibility have not been verified.'
$temporaryApk = $null

function Find-Adb {
    $candidates = @()
    $fromPath = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($fromPath) { $candidates += $fromPath.Source }
    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe')
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe')
    }
    $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe')
    $candidates += (Join-Path $env:LOCALAPPDATA 'NightWaveBuild\android-sdk\platform-tools\adb.exe')
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
    }
    throw 'adb.exe not found. Install Android SDK Platform Tools first.'
}

try {
    New-Item -Path $dist -ItemType Directory -Force | Out-Null
    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) {
        throw 'No freshly verified NightWave-debug.apk exists. Run build-windows.cmd first.'
    }
    if (-not (Test-Path -LiteralPath $status -PathType Leaf)) {
        throw 'Build status is missing; APK origin cannot be verified.'
    }
    $summary = Get-Content -LiteralPath $status -Raw
    if ($summary -notmatch '(?m)^SUCCESS - NightWave Android debug APK locally verified') {
        throw 'The latest local build has not passed its verification gates.'
    }
    if ($summary -notmatch '(?m)^Version: 2\.0\.0-alpha5k\s*$' -or
        $summary -notmatch '(?m)^Version code: 27\s*$') {
        throw 'The build status does not identify NightWave Phase 5I.'
    }
    if ($summary -match '(?m)^SHA256: ([0-9A-Fa-f]{64})\s*$') {
        $expected = $Matches[1]
    } else {
        throw 'Build status lacks a valid APK SHA256.'
    }
    $actual = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
    if ($actual -ne $expected) { throw 'APK SHA256 differs from the last verified local build.' }

    $signerVerifier = Join-Path $PSScriptRoot 'verify-apk-signature.ps1'
    $aapt = Join-Path $buildTools 'aapt.exe'
    if (-not (Test-Path -LiteralPath $signerVerifier -PathType Leaf) -or
        -not (Test-Path -LiteralPath $aapt -PathType Leaf)) {
        throw 'APK signing verifier or Android Build Tools 35.0.0 is unavailable.'
    }

    $adb = Find-Adb
    $listing = @(& $adb devices 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'adb devices failed.' }
    $devices = @($listing | ForEach-Object {
        if ($_ -match '^(\S+)\s+(device|offline|unauthorized)(?:\s|$)') {
            [PSCustomObject]@{ Serial = $Matches[1]; State = $Matches[2] }
        }
    })
    if ($devices.Count -ne 1 -or $devices[0].State -ne 'device') {
        throw 'Connect and authorize exactly one Android phone/emulator. No device was modified.'
    }
    $serial = $devices[0].Serial
    $pmOutput = @(& $adb -s $serial shell pm path $packageName 2>&1)
    if ($LASTEXITCODE -ne 0) { throw 'Android package-manager query failed.' }

    $remotePaths = @($pmOutput | ForEach-Object {
        if ($_ -match '^package:(/.+\.apk)\s*$') { $Matches[1].Trim() }
    })
    if ($remotePaths.Count -eq 0) {
        if (($pmOutput -join '').Trim().Length -ne 0) {
            throw 'Android package manager returned unexpected text. Existing-app state is inconclusive.'
        }
        $checked = 'PASS - FRESH INSTALL'
        $details = 'NightWave is not installed on this device. There is no existing NightWave data/signature to preserve.'
        $verifyResult = @(& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $signerVerifier -ApkPath $apk -BuildToolsDir $buildTools -VersionName $versionName -VersionCode $versionCode 2>&1)
    } else {
        $base = @($remotePaths | Where-Object { $_ -match '/base\.apk$' })
        if ($base.Count -ne 1) {
            throw 'Installed NightWave base.apk was not uniquely identified. Compatibility is inconclusive.'
        }

        # Pull only the installed app's APK package, never private music or data.
        $temporaryApk = Join-Path $dist ('NightWave-installed-' +
            [Guid]::NewGuid().ToString('N') + '.apk')
        & $adb -s $serial pull $base[0] $temporaryApk *> $null
        if ($LASTEXITCODE -ne 0 -or
            -not (Test-Path -LiteralPath $temporaryApk -PathType Leaf)) {
            throw 'Could not read the installed NightWave APK. No install was attempted.'
        }

        $installedInfo = (& $aapt dump badging $temporaryApk 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) {
            throw 'Android aapt cannot inspect installed NightWave.'
        }
        if ($installedInfo -match "(?m)^package:\s+name='com\.nightwave\.player'\s+versionCode='(\d+)'") {
            $installedCode = [int]$Matches[1]
        } else {
            throw 'Installed package identity/version could not be verified.'
        }
        if ($installedCode -gt $versionCode) {
            throw 'Installed NightWave version is newer. Do not downgrade or uninstall.'
        }
        $verifyResult = @(& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $signerVerifier -ApkPath $apk -BuildToolsDir $buildTools -VersionName $versionName -VersionCode $versionCode -PreviousApkPath $temporaryApk 2>&1)
        $checked = 'PASS - IN-PLACE UPDATE SIGNER MATCH'
        $details = 'Installed NightWave APK signer matches the new APK. This is only a read-only preflight.'
    }
    $verifyText = $verifyResult -join [Environment]::NewLine
    if ($LASTEXITCODE -ne 0 -or
        $verifyText -notmatch 'NightWave APK verification PASSED') {
        $checked = 'INCONCLUSIVE'
        throw 'Candidate/installed APK verification failed. Do not install or uninstall.'
    }
    Write-Host '[NightWave] Verified APK update compatibility without changing the phone.' -ForegroundColor Green
} catch {
    $checked = 'INCONCLUSIVE'
    $details = $_.Exception.Message
    Write-Host "[NightWave] $checked - $details" -ForegroundColor Yellow
} finally {
    if ($null -ne $temporaryApk -and
        (Test-Path -LiteralPath $temporaryApk -PathType Leaf)) {
        Remove-Item -LiteralPath $temporaryApk -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path -LiteralPath $dist -PathType Container) {
        @(
            'NightWave Phase 5I - read-only installed APK compatibility',
            "Result: $checked",
            "Detail: $details",
            "Candidate: $packageName / $versionName / build $versionCode",
            'No app installation, uninstallation or music-data change occurred.',
            'This check does not prove playback or crash-free operation.'
        ) | Set-Content -LiteralPath $reportPath -Encoding UTF8
        Write-Host "Report: $reportPath"
    }
}
if ($checked -like 'PASS*') { exit 0 }
exit 1
