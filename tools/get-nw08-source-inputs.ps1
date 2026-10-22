# NW-08 source manifest enumerator. No network, ADB, signing or library mutation.
# Outputs only sorted project-relative paths. Called once before the build and
# again before promoting the APK, so even newly added source files are detected.
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [switch]$SelfTest
)
$ErrorActionPreference = 'Stop'

function Collect-Sources([string]$root) {
    $root = [IO.Path]::GetFullPath($root).TrimEnd([IO.Path]::DirectorySeparatorChar)
    $prefix = $root + [IO.Path]::DirectorySeparatorChar
    $source = Join-Path $root 'app/src'
    if (-not (Test-Path -LiteralPath $source -PathType Container)) {
        throw 'Missing app/src: download the complete NightWave source ZIP.'
    }
    $files = [Collections.Generic.List[string]]::new()
    # All app source/resources/tests, including layouts, drawables and sample fixtures.
    foreach ($file in @(Get-ChildItem -LiteralPath $source -File -Recurse -Force -ErrorAction Stop)) {
        if (($file.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw 'Symlink or reparse point inside app/src is unsupported.'
        }
        if (-not $file.FullName.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Source file outside project tree.'
        }
        $files.Add($file.FullName.Substring($prefix.Length).Replace('\', '/'))
    }
    $configFiles = @(
        'app/build.gradle.kts',
        'app/proguard-rules.pro',
        'build.gradle.kts',
        'settings.gradle.kts',
        'gradle.properties',
        'build-windows.cmd'
    )
    foreach ($relative in $configFiles) {
        $full = Join-Path $root ($relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
        if (-not (Test-Path -LiteralPath $full -PathType Leaf)) {
            throw ("Missing mandatory build configuration: " + $relative)
        }
        $files.Add($relative)
    }
    # The actual build and preflight scripts can affect the published outcome.
    $tools = Join-Path $root 'tools'
    # NW-12: recursive to include the real, versioned Kotlin smoke sources
    # under tools/kotlin, not just scripts directly inside tools/.
    foreach ($file in @(Get-ChildItem -LiteralPath $tools -File -Recurse -Force -ErrorAction Stop |
        Where-Object { $_.Extension -in @('.ps1', '.py', '.cmd', '.kt') })) {
        if (($file.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw 'Symlinked source tooling is unsupported.'
        }
        if (-not $file.FullName.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Source tool outside project tree.'
        }
        $files.Add($file.FullName.Substring($prefix.Length).Replace('\', '/'))
    }
    $sorted = @($files.ToArray() | Sort-Object -Unique)
    if ($sorted.Count -lt 50) {
        throw "Incomplete source tree: only $($sorted.Count) files in manifest."
    }
    return $sorted
}

try {
    if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
        $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
    }
    $list = @(Collect-Sources $ProjectRoot)
    if ($SelfTest) {
        if ($list.Count -lt 50) { throw 'Source enumeration is unexpectedly empty.' }
        if (@($list | Where-Object { $_ -notmatch '^(app/src/|app/|tools/|build.gradle.kts$|settings.gradle.kts$|gradle.properties$|build-windows.cmd$)' }).Count -ne 0) {
            throw 'Source enumeration escaped the selected project folders.'
        }
        if (@($list | Where-Object { $_ -match '(^|/)(?:build|\.gradle|dist)/' }).Count -ne 0) {
            throw 'Source enumeration included generated output.'
        }
        Write-Host "PASS: NW08 source-tree enumeration ($($list.Count) files, no generated APKs)"
        exit 0
    }
    # Output one relative path per line, nothing else. A failed call uses exit 1.
    $list
    exit 0
} catch {
    [Console]::Error.WriteLine('NW08 source enumeration FAILED: ' + $_.Exception.Message)
    exit 1
}
