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

try {
    if (-not [Environment]::Is64BitOperatingSystem) { throw '64-bit Windows is required.' }
    $project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
    $script:cache = Join-Path $env:LOCALAPPDATA 'NightWaveBuild'
    New-Item -ItemType Directory -Path $script:cache -Force | Out-Null
    Say 'No GitHub billing or payment method is required.'
    Say 'The first build downloads official build tools and may take time and several GB of disk space.'
    Resolve-Java

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
    Say 'Installing Android platform 35 and build tools 35.0.0'
    & $sdkmanager "--sdk_root=$sdk" 'platforms;android-35' 'build-tools;35.0.0'
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK package installation failed.' }

    Say 'Running Android unit tests and compiling the debug APK'
    # Always capture the FIRST compiler diagnostics, not just Gradle's stack trace.
    # Save reports outside Gradle's own build/ directory so Clean won't erase them.
    $outDir = Join-Path $project 'dist'
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    $buildLog = Join-Path $outDir 'NightWave-build-full.log'
    $errorReport = Join-Path $outDir 'NightWave-build-errors.txt'
    if (Test-Path -LiteralPath $errorReport) { Remove-Item -LiteralPath $errorReport -Force }
    Say "Saving complete Gradle output to $buildLog"
    Push-Location $project
    try {
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            & $gradleBat --no-daemon --console=plain --warning-mode=summary :app:testDebugUnitTest :app:assembleDebug 2>&1 |
                ForEach-Object { $_.ToString() } | Tee-Object -FilePath $buildLog
            $gradleResult = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorPreference
        }
        if ($gradleResult -ne 0) {
            $patterns = @(
                '^e:\s', '\.kt:\d+(:\d+)?', '(^|\s)error:\s',
                'Unresolved reference', 'Type mismatch', 'Cannot infer',
                'No value passed for parameter', 'Overload resolution ambiguity',
                '^> Task .* FAILED', '^Execution failed for task', '^FAILURE:',
                '^Could not resolve', 'Compilation error', 'KSP.*error'
            )
            $matches = @(Select-String -LiteralPath $buildLog -Pattern $patterns |
                Select-Object -First 65 | ForEach-Object { $_.Line.TrimEnd() })
            if ($matches.Count -eq 0) {
                $matches = @(Get-Content -LiteralPath $buildLog -Tail 75)
            }
            $report = @(
                'NightWave build failure - diagnostic summary'
                "Build log: $buildLog"
                'The first Kotlin compiler errors are the lines beginning with e: or mentioning .kt:'
                ''
            ) + $matches
            $report | Set-Content -LiteralPath $errorReport -Encoding UTF8
            Write-Host ''
            Write-Host '===== FIRST COMPILER ERRORS / FAILURE SUMMARY =====' -ForegroundColor Red
            $matches | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
            Write-Host '===================================================' -ForegroundColor Red
            Write-Host "Send this file for repairs: $errorReport" -ForegroundColor Cyan
            Write-Host "Complete build log:       $buildLog" -ForegroundColor Cyan
            throw 'Gradle returned an Android compilation/test failure. A diagnostic report has been saved.'
        }
    } finally { Pop-Location }

    $apk = Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw 'Gradle exited successfully but the debug APK was not found.' }
    $outDir = Join-Path $project 'dist'
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    $output = Join-Path $outDir 'NightWave-debug.apk'
    Copy-Item -LiteralPath $apk -Destination $output -Force
    Say 'BUILD SUCCESS'
    Say "APK: $output"
    Write-Host "SHA256: $((Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash)"
    exit 0
} catch {
    Write-Host "[NightWave] BUILD FAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
