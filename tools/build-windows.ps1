# NightWave local APK builder. All downloads are from upstream Android/Gradle/Eclipse vendors.
# Runs without administrator privileges or GitHub Actions/billing.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Say([string]$Message) { Write-Host "[NightWave] $Message" -ForegroundColor Cyan }
function Get-Archive([string]$Url, [string]$Destination) {
    if (Test-Path -LiteralPath $Destination) { return }
    Say "Downloading $Url"
    $part = "$Destination.partial"
    if (Test-Path -LiteralPath $part) { Remove-Item -LiteralPath $part -Force }
    try {
        Invoke-WebRequest -Uri $Url -OutFile $part -MaximumRedirection 10 -UseBasicParsing
        if ((Get-Item -LiteralPath $part).Length -lt 1000000) { throw "Download is unexpectedly small: $Url" }
        Move-Item -LiteralPath $part -Destination $Destination -Force
    } catch {
        Remove-Item -LiteralPath $part -Force -ErrorAction SilentlyContinue
        throw
    }
}
function Set-Java([string]$Exe) {
    $homeDir = Split-Path (Split-Path $Exe -Parent) -Parent
    $env:JAVA_HOME = $homeDir
    $env:PATH = "$(Join-Path $homeDir 'bin');$env:PATH"
    Say "Using Java from $homeDir"
}
function Resolve-Java {
    $possible = @()
    if ($env:JAVA_HOME) { $possible += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $possible += @(
        'C:\Program Files\Android\Android Studio\jbr\bin\java.exe',
        'C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe',
        'C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe'
    )
    $onPath = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($onPath) { $possible += $onPath.Source }
    foreach ($exe in $possible) {
        if (-not (Test-Path -LiteralPath $exe)) { continue }
        # Capture stderr without treating java -version's ordinary output as a PowerShell error.
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try { $versionText = (& $exe -version 2>&1 | Out-String) } catch { $versionText = '' }
        finally { $ErrorActionPreference = $previousErrorPreference }
        if ($versionText -match 'version "(?:1\.)?(\d+)') {
            if ([int]$Matches[1] -ge 17) { Set-Java $exe; return }
        } elseif ($versionText -match 'openjdk (\d+)') {
            if ([int]$Matches[1] -ge 17) { Set-Java $exe; return }
        }
    }
    $jdkDir = Join-Path $script:cache 'jdk'
    $jdkExe = Get-ChildItem -Path $jdkDir -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '[\\/]bin[\\/]java\.exe$' } | Select-Object -First 1
    if (-not $jdkExe) {
        $jdkArchive = Join-Path $script:cache 'temurin17-jdk.zip'
        Get-Archive 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' $jdkArchive
        New-Item -ItemType Directory -Path $jdkDir -Force | Out-Null
        Expand-Archive -LiteralPath $jdkArchive -DestinationPath $jdkDir -Force
        $jdkExe = Get-ChildItem -Path $jdkDir -Filter java.exe -Recurse |
            Where-Object { $_.FullName -match '[\\/]bin[\\/]java\.exe$' } | Select-Object -First 1
    }
    if (-not $jdkExe) { throw 'Java 17 was not found inside the downloaded archive.' }
    Set-Java $jdkExe.FullName
}

# Build-state files are written even if Java, Gradle or Android SDK setup fails.
# A stale success APK is archived rather than accidentally delivered as new.
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$outDir = Join-Path $project 'dist'
$buildLog = Join-Path $outDir 'NightWave-build-full.log'
$previousLog = Join-Path $outDir 'NightWave-build-full-previous.log'
$errorReport = Join-Path $outDir 'NightWave-build-errors.txt'
$statusFile = Join-Path $outDir 'NightWave-build-status.txt'
$output = Join-Path $outDir 'NightWave-debug.apk'
$previousApk = Join-Path $outDir 'NightWave-debug-previous.apk'
$stage = 'Preflight'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
if (Test-Path -LiteralPath $errorReport) { Remove-Item -LiteralPath $errorReport -Force }
# Archive the prior log; a preflight failure must not reuse yesterday's compiler errors.
if (Test-Path -LiteralPath $buildLog) {
    Copy-Item -LiteralPath $buildLog -Destination $previousLog -Force
}
'Gradle not yet started for this NightWave build attempt.' |
    Set-Content -LiteralPath $buildLog -Encoding UTF8
'FAILED - build has not yet passed all checks; do not install an old APK.' |
    Set-Content -LiteralPath $statusFile -Encoding UTF8
try {
    if (-not [Environment]::Is64BitOperatingSystem) { throw '64-bit Windows is required.' }
    $script:cache = Join-Path $env:LOCALAPPDATA 'NightWaveBuild'
    New-Item -ItemType Directory -Path $script:cache -Force | Out-Null
    if (Test-Path -LiteralPath $output) {
        Copy-Item -LiteralPath $output -Destination $previousApk -Force
        Remove-Item -LiteralPath $output -Force
        Say "Previous APK preserved as $previousApk"
    }
    Say 'No GitHub billing or payment method is required.'
    Say 'The first build downloads official build tools and may take time and several GB of disk space.'
    $stage = 'Kotlin source compatibility preflight'
    $kotlinPreflight = Join-Path $PSScriptRoot 'check-kotlin-compat.ps1'
    if (-not (Test-Path -LiteralPath $kotlinPreflight -PathType Leaf)) {
        throw 'NightWave Kotlin preflight script is missing.'
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $kotlinPreflight
    if ($LASTEXITCODE -ne 0) {
        throw 'Kotlin source preflight failed. Check the invalid import lines above.'
    }
    Say 'Fast Kotlin source preflight PASS (not an Android compilation).'
    $stage = 'Crash redaction self-test'
    $crashScript = Join-Path $PSScriptRoot 'capture-nightwave-crash.ps1'
    if (-not (Test-Path -LiteralPath $crashScript -PathType Leaf)) {
        throw 'NightWave privacy-filtered crash script is missing.'
    }
    $crashTest = (& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $crashScript -SelfTest 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or
        $crashTest -notmatch 'PASS: NightWave-only crash filtering') {
        throw 'NightWave synthetic crash redaction self-test failed. No APK was published.'
    }
    Say 'Synthetic crash-report privacy test PASS; no device was accessed.'
    $stage = 'Build diagnostic self-test'
    $diagnosticScript = Join-Path $PSScriptRoot 'diagnose-build.ps1'
    if (-not (Test-Path -LiteralPath $diagnosticScript -PathType Leaf)) {
        throw 'NightWave read-only build diagnosis tool is missing.'
    }
    $diagnosticTest = (& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $diagnosticScript -SelfTest 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $diagnosticTest -notmatch 'PASS: NightWave build diagnostic self-test') {
        # The redaction test uses only synthetic placeholders. Surface its
        # exact fail category so a blocked Windows build is debuggable without
        # copying full logs or printing real credentials.
        $testFailure = if ($diagnosticTest -match 'FAIL: ([^\r\n]{1,180})') {
            $Matches[1].Trim()
        } else {
            'The self-test exited without its PASS marker.'
        }
        throw ("NightWave build diagnosis/redaction self-test failed: " +
            $testFailure + " No APK was published.")
    }
    Say 'Build diagnostics self-test PASS; no APK or device was accessed.'
    $stage = 'Phase 6P integration source preflight'
    $integrationGate = Join-Path $PSScriptRoot 'check-phase6p-integration.ps1'
    if (-not (Test-Path -LiteralPath $integrationGate -PathType Leaf)) {
        throw 'NightWave Phase 6P integration gate is missing.'
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $integrationGate
    if ($LASTEXITCODE -ne 0) {
        throw 'Phase 6P integration preflight failed; Android build was not started.'
    }
    Say 'Cross-phase source preflight PASS (not Gradle compilation).'
    $stage = 'Phase 6V cloud and Android integration preflight'
    $phase6v = Join-Path $PSScriptRoot 'check-phase6v-integration.ps1'
    if (-not (Test-Path -LiteralPath $phase6v -PathType Leaf)) {
        throw 'NightWave Phase 6V integration preflight is missing.'
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $phase6v
    if ($LASTEXITCODE -ne 0) {
        throw 'Phase 6V cloud/integration preflight failed. No Android build was started.'
    }
    Say '6V source/cloud integration PASS (still not Kotlin compilation).'
    $stage = 'Phase 6V4 reliability integration preflight'
    $phase6v4 = Join-Path $PSScriptRoot 'check-phase6v4-integration.ps1'
    if (-not (Test-Path -LiteralPath $phase6v4 -PathType Leaf)) {
        throw 'NightWave 6V4 integrated reliability gate is missing. Use the full ZIP.'
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $phase6v4
    if ($LASTEXITCODE -ne 0) {
        throw 'Phase 6V4 integration preflight failed; Gradle was not started.'
    }
    Say '6V4 source/regression discovery PASS (still not compiler/test execution).'

    $stage = 'Phase 6V5 Google Drive Music Vault integration preflight'
    $phase6v5 = Join-Path $PSScriptRoot 'check-phase6v5-integration.ps1'
    if (-not (Test-Path -LiteralPath $phase6v5 -PathType Leaf)) {
        throw 'NightWave 6V5 Music Vault integration gate is missing. Use the full ZIP.'
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $phase6v5
    if ($LASTEXITCODE -ne 0) {
        throw 'Phase 6V5 Music Vault preflight failed; Gradle was not started.'
    }
    Say '6V5 vault source/regression discovery PASS (not a Google or Android runtime test).'

    $stage = 'Java preparation'
    Resolve-Java

    $stage = 'Gradle preparation'
    $gradleDir = Join-Path $script:cache 'gradle-8.11.1'
    $gradleBat = Join-Path $gradleDir 'bin\gradle.bat'
    if (-not (Test-Path -LiteralPath $gradleBat)) {
        $zip = Join-Path $script:cache 'gradle-8.11.1-bin.zip'
        Get-Archive 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' $zip
        Say 'Verifying Gradle SHA-256 checksum'
        $checksumPath = Join-Path $script:cache 'gradle-8.11.1-bin.zip.sha256'
        if (-not (Test-Path -LiteralPath $checksumPath)) {
            Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256' -OutFile $checksumPath -UseBasicParsing
        }
        $checksum = (Get-Content -LiteralPath $checksumPath -Raw).Trim().Split(' ')[0]
        $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash
        if ($actual -ne $checksum) { throw 'Gradle checksum mismatch; stopping.' }
        Expand-Archive -LiteralPath $zip -DestinationPath $script:cache -Force
    }
    if (-not (Test-Path -LiteralPath $gradleBat)) { throw 'Gradle executable missing after installation.' }

    $stage = 'Android SDK preparation'
    $sdk = Join-Path $script:cache 'android-sdk'
    $sdkmanager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
    if (-not (Test-Path -LiteralPath $sdkmanager)) {
        $cliZip = Join-Path $script:cache 'android-commandline-tools.zip'
        Get-Archive 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' $cliZip
        $tmp = Join-Path $script:cache 'sdk-cli-unpack'
        New-Item -ItemType Directory -Path $tmp -Force | Out-Null
        Expand-Archive -LiteralPath $cliZip -DestinationPath $tmp -Force
        $from = Join-Path $tmp 'cmdline-tools'
        if (-not (Test-Path -LiteralPath (Join-Path $from 'bin\sdkmanager.bat'))) {
            throw 'Android command-line tools archive has an unexpected layout.'
        }
        New-Item -ItemType Directory -Path (Join-Path $sdk 'cmdline-tools') -Force | Out-Null
        Move-Item -LiteralPath $from -Destination (Join-Path $sdk 'cmdline-tools\latest')
        Remove-Item -LiteralPath $tmp -Recurse -Force
    }
    if (-not (Test-Path -LiteralPath $sdkmanager)) { throw 'Android SDK manager was not installed.' }
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
    Say 'Android SDK licenses must be accepted once. Read the terms and type y when you agree.'
    & $sdkmanager "--sdk_root=$sdk" --licenses
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK licenses were not accepted.' }
    Say 'Installing Android 35, build tools and read-only device-check platform tools'
    & $sdkmanager "--sdk_root=$sdk" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK package installation failed.' }

    $stage = 'Gradle compile / unit tests'
    Say 'Running unit tests, building APK and compiling Android instrumentation test sources'
    # The complete Gradle output is retained; preflight failures also get a report.
    Say "Saving build output to $buildLog"

    Push-Location $project
    try {
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            & $gradleBat --no-daemon --console=plain --warning-mode=summary :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin 2>&1 |
                ForEach-Object { $_.ToString() } | Tee-Object -FilePath $buildLog
            $gradleResult = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorPreference
        }

        # A zero exit code without Gradle's terminal success marker is not
        # enough to publish a possibly stale APK from another checkout.
        $gradleSucceeded = ($gradleResult -eq 0) -and [bool](
            Select-String -LiteralPath $buildLog -Pattern '^BUILD SUCCESSFUL' -Quiet
        )
        if (-not $gradleSucceeded) {
            $patterns = @(
                '^e:\s',
                '(^|\s)error:\s',
                'Unresolved reference',
                'Type mismatch',
                'Cannot infer',
                'No value passed for parameter',
                'Overload resolution ambiguity',
                '^> Task .* FAILED',
                '^Execution failed for task',
                '^FAILURE:',
                '^Could not resolve',
                'Compilation error',
                'KSP.*error',
                '\s>\s.*\sFAILED$',
                'java\.lang\.AssertionError',
                '\d+ tests completed, \d+ failed',
                'There were failing tests'
            )
            $matchedLines = @(
                Select-String -LiteralPath $buildLog -Pattern $patterns |
                    Select-Object -First 65 |
                    ForEach-Object { $_.Line.TrimEnd() }
            )
            if ($matchedLines.Count -eq 0) {
                $matchedLines = @(Get-Content -LiteralPath $buildLog -Tail 75)
            }
            $report = @(
                'NightWave build failure - diagnostic summary',
                "Build log: $buildLog",
                'Compiler errors and failed tests appear below (deprecation warnings omitted):',
                ''
            ) + $matchedLines
            $report | Set-Content -LiteralPath $errorReport -Encoding UTF8
            Write-Host ''
            Write-Host '===== FIRST COMPILER ERRORS / FAILURE SUMMARY =====' -ForegroundColor Red
            $matchedLines | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
            Write-Host '===================================================' -ForegroundColor Red
            Write-Host "Send this file for repairs: $errorReport" -ForegroundColor Cyan
            Write-Host "Complete build log:       $buildLog" -ForegroundColor Cyan
            throw 'Gradle returned an Android compilation/test failure. A diagnostic report has been saved.'
        }
    } finally {
        Pop-Location
    }

    $stage = 'APK verification'
    $apk = Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) {
        throw 'Gradle exited successfully but the debug APK was not found.'
    }
    # Refuse an unsigned/tampered APK or a debug signer incompatible with
    # the previously built NightWave APK. No installation or data reset occurs.
    $verifier = Join-Path $PSScriptRoot 'verify-apk-signature.ps1'
    if (-not (Test-Path -LiteralPath $verifier)) {
        throw 'APK signature verification script missing from source ZIP.'
    }
    $buildTools = Join-Path $sdk 'build-tools\35.0.0'
    $verifyOutput = (& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $verifier `
        -ApkPath $apk -BuildToolsDir $buildTools `
        -VersionName '2.0.0-alpha5s' -VersionCode '35' `
        -PreviousApkPath $previousApk 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw ('APK signature/identity gate failed: ' + $verifyOutput.Trim())
    }
    $verifyLines = @($verifyOutput -split '\r?\n')
    $signerLine = $verifyLines | Where-Object { $_ -match '^Signer SHA256:' } |
        Select-Object -First 1
    $continuityLine = $verifyLines | Where-Object { $_ -match '^Signing continuity:' } |
        Select-Object -First 1
    if (-not $signerLine -or -not $continuityLine) {
        throw 'APK verifier returned success without signer details.'
    }
    # The verified candidate alone is promoted to the downloadable result.
    Copy-Item -LiteralPath $apk -Destination $output -Force
    $hash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash
    @(
        'SUCCESS - NightWave Android debug APK locally verified',
        'Package: com.nightwave.player',
        'Version: 2.0.0-alpha5s',
        'Version code: 35',
        $signerLine,
        $continuityLine,
        "SHA256: $hash",
        "APK: $output",
        'Installed-device signer compatibility is not assured unless compared separately.'
    ) | Set-Content -LiteralPath $statusFile -Encoding UTF8
    Say $verifyOutput.Trim()
    Say 'BUILD SUCCESS'
    Say "APK: $output"
    Write-Host "SHA256: $hash"
    exit 0
} catch {
    $reason = $_.Exception.Message
    # Preflight failures previously had no errors.txt, causing the user to
    # repeatedly upload stale logs from an older build.
    if (-not (Test-Path -LiteralPath $errorReport)) {
        @(
            'NightWave build failure - diagnostic summary',
            "Stage: $stage",
            "Reason: $reason",
            'No Android APK was verified or published by this build.'
        ) | Set-Content -LiteralPath $errorReport -Encoding UTF8
    }
    @(
        'FAILED - do not install dist\NightWave-debug.apk from this attempt.',
        "Stage: $stage",
        "Reason: $reason",
        "Diagnostic: $errorReport",
        "Previous verified copy, if present: $previousApk"
    ) | Set-Content -LiteralPath $statusFile -Encoding UTF8
    Write-Host "[NightWave] BUILD FAILED ($stage): $reason" -ForegroundColor Red
    Write-Host "Diagnostics: $errorReport" -ForegroundColor Yellow
    # Best-effort failure-only diagnosis; never changes the failed exit code.
    $diagnosticScript = Join-Path $PSScriptRoot 'diagnose-build.ps1'
    if (Test-Path -LiteralPath $diagnosticScript -PathType Leaf) {
        try {
            & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $diagnosticScript
        } catch {
            Write-Host ('Diagnostic summary unavailable: ' + $_.Exception.Message) -ForegroundColor Yellow
        }
    }
    exit 1
}
