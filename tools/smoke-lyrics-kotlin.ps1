# Standalone Kotlin/JVM lyric policy smoke check.
# Runs only with a local 'kotlinc' installation; never launches Android,
# touches private app storage, APK signatures, Drive, or installed music.
[CmdletBinding()]
param([switch]$RequireCompiler)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compiler = Get-Command 'kotlinc' -ErrorAction SilentlyContinue
if (-not $compiler) {
    $message = 'Kotlin command-line compiler not on PATH. Gradle Android build remains required.'
    if ($RequireCompiler) { Write-Host "FAIL: $message" -ForegroundColor Red; exit 2 }
    Write-Host "SKIP: $message" -ForegroundColor Yellow
    exit 0
}
$relative = @(
    'TimedLyrics.kt',
    'LyricsSyncPolicy.kt',
    'LyricsEditorPolicy.kt',
    'LyricsEditorNavigationPolicy.kt',
    'LyricsEditorSearchPolicy.kt',
    'LyricsExportPolicy.kt',
    'LyricsImportReview.kt'
)
$base = Join-Path $root 'app\src\main\java\com\nightwave\player\v2\lyrics'
$inputs = @()
foreach ($filename in $relative) {
    $file = Join-Path $base $filename
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        Write-Host "FAIL: Missing pure Kotlin source $filename" -ForegroundColor Red
        exit 2
    }
    $inputs += $file
}
$work = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null
try {
    $fixture = Join-Path $work 'LyricsPolicySmoke.kt'
    $jar = Join-Path $work 'lyrics-policy-smoke.jar'
@'
import com.nightwave.player.v2.lyrics.*

fun main() {
    val raw = "[00:01.000]Hello\\n[00:02.000]World\\n[00:03.000]HELLO"
    val original = TimedLyricsCodec.parse(raw.toByteArray(Charsets.UTF_8))
    check(original.cues.size == 3)
    val base = LyricsEditorPolicy.begin(original)
    check(LyricsEditorSearchPolicy.matches(base, "hello") == listOf(0, 2))
    check(LyricsEditorSearchPolicy.nearest(listOf(0, 2), 2, true) == 0)
    check(LyricsEditorSearchPolicy.nearest(listOf(0, 2), 0, false) == 2)
    check(LyricsEditorSearchPolicy.matches(base, "00:02") == listOf(1))
    check(LyricsEditorSearchPolicy.matches(base, "") == emptyList<Int>())
    check(LyricsEditorNavigationPolicy.step(0, 3, Int.MAX_VALUE) == 2)
    check(LyricsSyncPolicy.activeIndex(original, 1500, 500) == 0)
    check(LyricsSyncPolicy.activeIndex(original, 500, 1000) == -1)
    check(LyricsSyncPolicy.seekPosition(original.cues[2], 500, 3200) == 3200L)
    val revised = LyricsEditorPolicy.updateText(base, 1, "New words")
    check(base.cues[1].text == "World")
    check(revised.cues[1].text == "New words")
    val moved = LyricsEditorPolicy.updateTime(revised, 2, "00:00.500")
    check(moved.cues[0].text == "HELLO")
    check(LyricsEditorSearchPolicy.matches(moved, "hello") == listOf(0, 1))
    check(LyricsEditorPolicy.serialize(moved).size <= TimedLyricsCodec.MAX_BYTES)
    check(TimedLyricsCodec.parse(LyricsEditorPolicy.serialize(moved)).cues == moved.cues)
    val exported = LyricsExportPolicy.prepare("Test / song", base, moved)
    check(exported.suggestedName == "Test song.lrc")
    check(exported.unsavedDraft)
    check(TimedLyricsCodec.parse(exported.bytes).cues == moved.cues)
    val review = LyricsImportReviewPolicy.inspect(
        "[ti:Different Title]\\n[00:01.000]One".replace("\\n", "\n")
            .toByteArray(Charsets.UTF_8), "Test.lrc", "Other Title", 5000
    )
    check(review.match == LyricsImportMatch.DIFFERENT)
    var denied = false
    try { LyricsEditorSearchPolicy.matches(base, "X".repeat(81)) }
    catch (_: IllegalArgumentException) { denied = true }
    check(denied)
    denied = false
    try { LyricsEditorPolicy.updateText(base, 0, "A".repeat(221)) }
    catch (_: IllegalArgumentException) { denied = true }
    check(denied)
    println("PASS: standalone compiled lyrics policy smoke checks")
}
'@ | Set-Content -LiteralPath $fixture -Encoding UTF8

    Write-Host 'Compiling pure Kotlin lyrics policy with local kotlinc...' -ForegroundColor Cyan
    & $compiler.Source @inputs $fixture '-include-runtime' '-d' $jar
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $jar)) {
        throw 'Kotlin policy compilation failed. Full Android build was not attempted.'
    }
    $java = Get-Command 'java' -ErrorAction SilentlyContinue
    if (-not $java) { throw 'Java is missing; compiled lyrics policy cannot run.' }
    & $java.Source '-jar' $jar
    if ($LASTEXITCODE -ne 0) { throw 'Compiled lyrics policy assertions failed.' }
    Write-Host 'PASS: Kotlin policy syntax and runtime assertions verified locally.' -ForegroundColor Green
    Write-Host 'NOT VERIFIED: Android Compose/Gradle/JUnit, SAF, Drive or signed APK.' -ForegroundColor Yellow
} catch {
    Write-Host ('FAIL: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}
