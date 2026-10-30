# Offline approval only. Never uploads, merges, changes sharing or publishes a binary.
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$PrivateKeyXml,
    [Parameter(Mandatory=$true)][string]$OutputFile,
    [string]$ApkPath = '',
    [long]$VersionCode = 0,
    [string]$VersionName = '',
    [string]$ReleaseUrl = '',
    [string]$Notes = '',
    [switch]$Disable
)
$ErrorActionPreference = 'Stop'
if (Test-Path -LiteralPath $OutputFile) { throw 'Choose a new output; never overwrite an approved feed silently.' }
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$privatePath = (Resolve-Path -LiteralPath $PrivateKeyXml).Path
if ($privatePath.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Private signing keys must stay outside the repository.' }
$payload = [ordered]@{ schema=1; channel='stable'; enabled=(-not $Disable.IsPresent) }
if (-not $Disable) {
    if ($VersionCode -le 35 -or $VersionName.Length -eq 0 -or $Notes.Length -gt 3000) { throw 'A newer version code and bounded release copy are required.' }
    $uri = [uri]$ReleaseUrl
    if ($uri.Scheme -ne 'https' -or $uri.Host -ne 'github.com' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or
        -not $uri.AbsolutePath.StartsWith('/Nahian2002/NightWave/releases/download/') -or -not $uri.AbsolutePath.EndsWith('.apk')) { throw 'Use the approved repository HTTPS APK release URL.' }
    $apk = Get-Item -LiteralPath $ApkPath
    if ($apk.Length -le 0 -or $apk.Length -gt 157286400) { throw 'APK size out of bounds.' }
    $payload.versionCode=$VersionCode; $payload.versionName=$VersionName; $payload.url=$ReleaseUrl
    $payload.bytes=$apk.Length; $payload.sha256=(Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash; $payload.notes=$Notes
}
$bytes = [Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json -Compress))
$rsa = New-Object Security.Cryptography.RSACryptoServiceProvider
$rsa.PersistKeyInCsp = $false
try {
    $rsa.FromXmlString([IO.File]::ReadAllText($privatePath))
    $signature = $rsa.SignData($bytes, [Security.Cryptography.HashAlgorithmName]::SHA256, [Security.Cryptography.RSASignaturePadding]::Pkcs1)
    $envelope = @{payload=[Convert]::ToBase64String($bytes); signature=[Convert]::ToBase64String($signature)} | ConvertTo-Json -Compress
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputFile), $envelope, (New-Object Text.UTF8Encoding($false)))
} finally { $rsa.Dispose() }
Write-Output 'Signed locally. Publishing and enabling a stable release still require explicit approval.'
