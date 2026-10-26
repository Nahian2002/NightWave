# NW77 read-only public music-source release gate.
# This is NOT called by build-windows.cmd: private debug QA must remain usable.
[CmdletBinding()]
param([string]$ProjectRoot = '', [switch]$SelfTest)
$ErrorActionPreference = 'Stop'

function Test-Contains([string]$bodyText, [string[]]$tokens) {
    foreach ($token in $tokens) {
        if ($bodyText.IndexOf($token, [StringComparison]::Ordinal) -lt 0) {
            return $false
        }
    }
    return $true
}

function Get-Source([string]$root, [string]$relative) {
    $path = Join-Path $root ($relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw ('Missing release audit source: ' + $relative)
    }
    return [IO.File]::ReadAllText($path)
}

if ($SelfTest) {
    $unsafeGateway = 'NewPipe.getService("YouTube") ... fun resolve(url: String) ... info.audioStreams'
    $unsafePlayer = 'gateway.resolve(entry.url) ... setUri(playable.audioUrl)'
    if (-not (Test-Contains $unsafeGateway @('NewPipe.getService("YouTube")',
        'fun resolve(url: String)', 'info.audioStreams'))) { throw 'NW77 gateway positive fixture failed.' }
    if (-not (Test-Contains $unsafePlayer @('gateway.resolve(entry.url)',
        'setUri(playable.audioUrl)'))) { throw 'NW77 player positive fixture failed.' }
    if (Test-Contains 'Safe local file' @('resolveAudioOnlyForOffline(canonical)',
        'downloadResolvedSearchAudio(')) { throw 'NW77 false blocker in a local file.' }
    Write-Host 'NW77 Windows public-release audit self-test PASS (source signature checks only).' -ForegroundColor Green
    exit 0
}

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
}
$root = (Resolve-Path -LiteralPath $ProjectRoot).Path
$reportFile = Join-Path (Join-Path $root 'dist') 'NightWave-NW77-public-release-report.txt'
New-Item -ItemType Directory -Path (Split-Path -Parent $reportFile) -Force | Out-Null
$findings = [Collections.Generic.List[string]]::new()
try {
    $gateway = Get-Source $root 'app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt'
    $playback = Get-Source $root 'app/src/main/java/com/nightwave/player/playback/PlaybackService.kt'
    $worker = Get-Source $root 'app/src/main/java/com/nightwave/player/offline/SearchTrackSaveWorker.kt'
    $queue = Get-Source $root 'app/src/main/java/com/nightwave/player/offline/SearchTrackSaveQueue.kt'
    $sourcePolicy = Get-Source $root 'app/src/main/java/com/nightwave/player/offline/SearchTrackSavePolicy.kt'
    $directPolicy = Get-Source $root 'app/src/main/java/com/nightwave/player/offline/PermittedAudioDownloadPolicy.kt'
    $catalog = Get-Source $root 'app/src/main/java/com/nightwave/player/offline/FreeAudioCatalogPolicy.kt'
    $cloud = Get-Source $root 'app/src/main/java/com/nightwave/player/v2/cloud/NightWaveCloudAuthorization.kt'
    if ((Test-Contains $gateway @('NewPipe.getService("YouTube")',
        'fun resolve(url: String)', 'info.audioStreams')) -and
        (Test-Contains $playback @('gateway.resolve(entry.url)',
            'setUri(playable.audioUrl)'))) {
        $findings.Add('BLOCKER NW77-YOUTUBE-SEPARATE-BACKGROUND-AUDIO: extracted YouTube audio plays inside standalone Media3 playback.')
    }
    if ((Test-Contains $worker @('resolveAudioOnlyForOffline(canonical)',
        'downloadResolvedSearchAudio(')) -and
        (Test-Contains $queue @('SearchTrackSaveWorker'))) {
        $findings.Add('BLOCKER NW77-YOUTUBE-OFFLINE-COPY: search worker saves extracted YouTube audio to local files.')
    }
    if (Test-Contains $sourcePolicy @('googlevideo.com', 'safeAudioEndpoint(')) {
        $findings.Add('BLOCKER NW77-YOUTUBE-CDN-AUDIO-ENDPOINT: search-audio save accepts YouTube CDN audio addresses.')
    }
    if (-not (Test-Contains $directPolicy @('forbiddenHosts', '"youtube.com"', '"googlevideo.com"'))) {
        $findings.Add('BLOCKER NW77-DIRECT-ORIGIN-SAFETY: direct downloader origin exclusions missing.')
    }
    if (-not (Test-Contains $catalog @('permittedLicense(shortName: String)', '"cc0"', '"public domain"'))) {
        $findings.Add('BLOCKER NW77-CATALOG-LICENSE-GATE: public catalog license allowlist missing.')
    }
    if (-not (Test-Contains $cloud @('const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"'))) {
        $findings.Add('BLOCKER NW77-CLOUD-SCOPE: narrow Drive backup scope missing.')
    }
    $state = if ($findings.Count -gt 0) { 'NO-GO' } else { 'REVIEW REQUIRED' }
    $lines = @(
        'NightWave NW77 PUBLIC RELEASE AUDIT - READ ONLY',
        "Public release: $state",
        'A successful debug build is not proof of public content rights.',
        "Identified blocking source paths: $($findings.Count)",
        ''
    ) + @($findings.ToArray()) + @(
        '',
        'Manual gates: authorized content sources, third-party provider policy,',
        'Google OAuth production status, backup readback, signed APK and device QA.',
        'DO NOT publish a public APK based solely on this static result.',
        'Private development builds and existing local music were not changed.'
    )
    $lines | Set-Content -LiteralPath $reportFile -Encoding UTF8
    $lines | ForEach-Object { Write-Host $_ }
    Write-Host "Report: $reportFile" -ForegroundColor Cyan
    exit 2
} catch {
    @(
        'NO-GO - NW77 release check cannot be completed.',
        'Missing/invalid source. No public APK was approved.',
        'No local music or build files were modified.'
    ) | Set-Content -LiteralPath $reportFile -Encoding UTF8
    Write-Host ('NW77 public release check FAILED CLOSED: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
