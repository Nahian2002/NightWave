<#
.NightWave NW40 read-only source gate for local Windows builds.
No Python dependency. No music files, device or account data are accessed.
This is NOT an Android compile or emulator/device test.
#>
param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Has-Required([string]$Text, [string]$Needle) {
    return $Text.Contains($Needle)
}

function Assert-Required([string]$Label, [string]$Text, [string]$Needle) {
    if (-not (Has-Required $Text $Needle)) {
        throw ("NW40 source integration failed: " + $Label)
    }
}

try {
    if ($SelfTest) {
        if (-not (Has-Required 'nightwave-test' 'wave')) {
            throw 'Source-match self-test did not recognize an expected token.'
        }
        if (Has-Required 'nightwave-test' 'not-there') {
            throw 'Source-match self-test accepted a missing token.'
        }
        Write-Output 'PASS: NW40 PowerShell source gate self-test'
        exit 0
    }

    $sources = @{}
    $required = @{
        'activity' = 'app/src/main/java/com/nightwave/player/v2/ui/NightWaveStudioActivity.kt'
        'mutations' = 'app/src/main/java/com/nightwave/player/library/LibraryMutationPlan.kt'
        'writer' = 'app/src/main/java/com/nightwave/player/library/LegacyLibraryTransaction.kt'
        'facade' = 'app/src/main/java/com/nightwave/player/library/MusicLibrary.kt'
        'history' = 'app/src/main/java/com/nightwave/player/v2/ui/SearchHistoryStore.kt'
        'pins' = 'app/src/main/java/com/nightwave/player/v2/library/PlaylistPinStore.kt'
        'gestures' = 'app/src/main/java/com/nightwave/player/v2/library/PlaylistReorderGesturePolicy.kt'
        'instrumentation' = 'app/src/androidTest/java/com/nightwave/player/v2/ui/SearchHistoryStoreInstrumentedTest.kt'
    }
    foreach ($name in $required.Keys) {
        $file = Join-Path $repoRoot ($required[$name].Replace('/', [IO.Path]::DirectorySeparatorChar))
        if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
            throw ("NW40 source file missing: " + $required[$name])
        }
        $sources[$name] = [IO.File]::ReadAllText($file, [Text.Encoding]::UTF8)
    }
    $a = $sources['activity']
    $m = $sources['mutations']
    $h = $sources['history']

    foreach ($operation in @('BulkRemoveFromPlaylist', 'BulkAddToPlaylist',
        'ReorderPlaylist', 'RestorePlaylistIfUnchanged')) {
        Assert-Required ("Atomic " + $operation) $m ("is LibraryChange." + $operation + " ->")
    }
    Assert-Required 'Target playlist snapshot guard' $m 'operation.expectedUrls'
    Assert-Required 'Undo refuses conflicts' $m 'old.songs(operation.name) != operation.expected'
    Assert-Required 'Reorder checks exact permutations' $m 'operation.orderedUrls.toSet() != current.toSet()'
    Assert-Required 'Recovery checkpoint is retained' $sources['writer'] 'if (!editor.commit())'
    Assert-Required 'Facade uses same single writer' $sources['facade'] 'LegacyLibraryTransaction.mutate(app,'
    Assert-Required 'Library Undo is connected' $a 'restorePlaylistIfUnchanged('
    Assert-Required 'Drag handle is connected' $a 'rememberDraggableState'
    Assert-Required 'Reorder up accessibility label' $a 'contentDescription = strings.get(R.string.nw141_move_up, song.title)'
    Assert-Required 'Reorder down accessibility label' $a 'contentDescription = strings.get(R.string.nw141_move_down, song.title)'
    Assert-Required 'Copy uses fresh source validation' $a 'val latestSource = library.playlist(fromName)'
    Assert-Required 'Offline menu handles index errors' $a 'val offlineReady = runCatching {'
    Assert-Required 'Pin refresh after Activity resume' $a 'pinnedPlaylistNames = pinStore.snapshot(library.playlistNames())'
    Assert-Required 'Search corruption is distinguished from empty' $h 'private fun decode(): List<String>?'
    Assert-Required 'Corrupt search history rejects rewrites' $h 'decode() ?: return emptyList()'
    Assert-Required 'Instrumentation is isolated' $sources['instrumentation'] 'name + suffix'
    Assert-Required 'Pin store uses an isolated preference file' $sources['pins'] 'nightwave_playlist_pins_v1'

    Write-Output 'PASS: NW40 local source integrity (source-only; Android compile still required)'
    exit 0
} catch {
    Write-Error ("FAIL: " + $_.Exception.Message)
    exit 1
}
