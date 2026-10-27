# NW126: read-only verification of the identity actually embedded in an APK.
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [string]$ApkPath = '',
    [string]$OutputFile = '',
    [ValidateSet('debug','release')][string]$ExpectedVariant = 'debug',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'

function Assert-Identity($identity, [string]$hash, [int]$count, [string]$variant) {
    if (($identity.schema -isnot [int] -and $identity.schema -isnot [long]) -or $identity.schema -ne 1 -or
        $identity.algorithm -cne 'NightWave-source-v1' -or
        $identity.variant -cne $variant -or
        $identity.sourceSha256 -isnot [string] -or
        $identity.sourceSha256 -cnotmatch '^[A-F0-9]{64}$' -or
        $identity.sourceSha256 -cne $hash -or
        ($identity.inputCount -isnot [int] -and $identity.inputCount -isnot [long]) -or $identity.inputCount -ne $count -or
        $count -lt 50 -or $count -gt 100000 -or
        $identity.revisionContext -isnot [string] -or
        $identity.revisionContext -cnotmatch '^(unavailable|[0-9a-f]{40})$') {
        throw 'Embedded build identity is missing, invalid or different from this source snapshot.'
    }
}

function Read-Identity([string]$path) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $entries = @($archive.Entries | Where-Object { $_.FullName -ceq 'assets/nightwave-build.json' })
        if ($entries.Count -ne 1 -or $entries[0].Length -gt 4096 -or $entries[0].Length -le 0) {
            throw 'APK must contain one bounded NightWave build identity.'
        }
        $reader = [IO.StreamReader]::new($entries[0].Open(), [Text.Encoding]::UTF8)
        try { return ($reader.ReadToEnd() | ConvertFrom-Json) } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
}

try {
    if ($SelfTest) {
        $good = @{ schema=1; algorithm='NightWave-source-v1'; variant='debug';
            sourceSha256=('A' * 64); inputCount=100; revisionContext='unavailable' }
        Assert-Identity ([pscustomobject]$good) ('A' * 64) 100 'debug'
        foreach ($field in @('schema','algorithm','variant','sourceSha256','inputCount','revisionContext')) {
            $bad = $good.Clone()
            $bad[$field] = 'invalid'
            $rejected = $false
            try { Assert-Identity ([pscustomobject]$bad) ('A' * 64) 100 'debug' } catch { $rejected = $true }
            if (-not $rejected) { throw "Malformed identity accepted: $field" }
        }
        foreach ($case in @(@(('B' * 64),100,'debug'), @(('A' * 64),101,'debug'), @(('A' * 64),100,'release'))) {
            $rejected = $false
            try { Assert-Identity ([pscustomobject]$good) $case[0] $case[1] $case[2] } catch { $rejected = $true }
            if (-not $rejected) { throw 'Mismatched snapshot/variant accepted' }
        }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        Add-Type -AssemblyName System.IO.Compression
        $tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('nightwave-provenance-' + [Guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $tempRoot | Out-Null
        try {
            $payload = ([pscustomobject]$good | ConvertTo-Json -Compress)
            foreach ($kind in @('valid','missing','oversized','duplicate','invalid-json')) {
                $zipPath = Join-Path $tempRoot ($kind + '.apk')
                $zip = [IO.Compression.ZipFile]::Open($zipPath, [IO.Compression.ZipArchiveMode]::Create)
                try {
                    if ($kind -ne 'missing') {
                        $copies = if ($kind -eq 'duplicate') { 2 } else { 1 }
                        for ($i = 0; $i -lt $copies; $i++) {
                            $entry = $zip.CreateEntry('assets/nightwave-build.json')
                            $writer = [IO.StreamWriter]::new($entry.Open(), [Text.UTF8Encoding]::new($false))
                            try {
                                if ($kind -eq 'oversized') { $writer.Write(' ' * 4097) }
                                elseif ($kind -eq 'invalid-json') { $writer.Write('not JSON') }
                                else { $writer.Write($payload) }
                            } finally { $writer.Dispose() }
                        }
                    }
                } finally { $zip.Dispose() }
                if ($kind -eq 'valid') {
                    Assert-Identity (Read-Identity $zipPath) ('A' * 64) 100 'debug'
                } else {
                    $rejected = $false
                    try { Read-Identity $zipPath | Out-Null } catch { $rejected = $true }
                    if (-not $rejected) { throw "Invalid APK identity container accepted: $kind" }
                }
            }
        } finally {
            # Only the five literal test files are removed; no recursive cleanup.
            foreach ($kind in @('valid','missing','oversized','duplicate','invalid-json')) {
                $testFile = Join-Path $tempRoot ($kind + '.apk')
                if (Test-Path -LiteralPath $testFile) { Remove-Item -LiteralPath $testFile -Force }
            }
            Remove-Item -LiteralPath $tempRoot
        }
        Write-Host 'PASS: NW126 identity field, source, count and variant negative fixtures'
        exit 0
    }
    if ([string]::IsNullOrWhiteSpace($ProjectRoot)) { $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path }
    if ([string]::IsNullOrWhiteSpace($ApkPath)) { $ApkPath = Join-Path $ProjectRoot 'app/build/outputs/apk/debug/app-debug.apk' }
    if ([string]::IsNullOrWhiteSpace($OutputFile)) { $OutputFile = Join-Path $ProjectRoot 'build/nw126-build-provenance.json' }
    $outputParent = Split-Path -Parent ([IO.Path]::GetFullPath($OutputFile))
    New-Item -ItemType Directory -Path $outputParent -Force | Out-Null
    '{"status":"NOT_VERIFIED"}' | Set-Content -LiteralPath $OutputFile -Encoding UTF8
    $paths = [string[]]@(& (Join-Path $PSScriptRoot 'get-nw08-source-inputs.ps1') -ProjectRoot $ProjectRoot)
    if ($LASTEXITCODE -ne 0 -or $paths.Count -lt 50) { throw 'Source enumeration failed' }
    [Array]::Sort($paths, [StringComparer]::Ordinal)
    $manifest = [Text.StringBuilder]::new("NightWave-source-v1`n")
    foreach ($relative in $paths) {
        $hash = (Get-FileHash -LiteralPath (Join-Path $ProjectRoot $relative) -Algorithm SHA256).Hash.ToUpperInvariant()
        [void]$manifest.Append($hash).Append('  ').Append($relative).Append("`n")
    }
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $sourceHash = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($manifest.ToString())))).Replace('-','')
    } finally { $sha.Dispose() }
    $identity = Read-Identity ([IO.Path]::GetFullPath($ApkPath))
    Assert-Identity $identity $sourceHash $paths.Count $ExpectedVariant
    $receipt = [ordered]@{ status='PASS_EMBEDDED_SOURCE_MATCH';
        apkSha256=(Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash;
        buildId=('NW-' + $identity.variant + '-' + $sourceHash.Substring(0,16));
        sourceSha256=$sourceHash; inputCount=$paths.Count; variant=$identity.variant;
        revisionContext=$identity.revisionContext; algorithm=$identity.algorithm;
        limit='Source identity only; device acceptance and installed signer require separate proof.' }
    $receipt | ConvertTo-Json | Set-Content -LiteralPath $OutputFile -Encoding UTF8
    Write-Host ('PASS: NW126 embedded build ID matches actual source: ' + $receipt.buildId)
} catch {
    Write-Host ('FAIL: NW126 build identity: ' + $_.Exception.Message)
    exit 1
}
