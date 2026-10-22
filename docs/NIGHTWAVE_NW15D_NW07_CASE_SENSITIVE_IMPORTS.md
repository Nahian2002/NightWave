# NightWave NW-15D — NW07 Case-Sensitive Kotlin Import Gate Hotfix

**Date:** 22 October 2026  
**Base:** NW-15C `bd9db797aa28493ca9b5990e22435de2071b774d`  
**Branch:** `fix/nightwave-v2-nw15d-nw07-exact-imports`  
**Release:** NO-GO until a complete passing Android build, APK signing verification and device testing.

## Evidence from the new Windows build

The user's NW-15C build advanced past both the Windows PowerShell syntax check and the first Kotlin source/import check. The screenshot displays many preflight checks passing, then:

```
PASS: NW07 source gate positive/negative self-tests
FAIL: NW07 source preflight: Duplicate Kotlin import: SeekTimeline
BUILD FAILED (NW07 integrated source preflight): NW07 integrated source gate failed; no Gradle build attempted.
```

The uploaded `NightWave-build-status(2).txt` confirms the failed stage is **NW07 integrated source preflight**; the uploaded `NightWave-build-full(1).log` explicitly states that Gradle has not started. No new APK was verified.

## Root cause: the second independent import checker

NW-15C already repaired `tools/check-kotlin-compat.ps1`, replacing its default case-insensitive `Group-Object` logic with ordinal, case-sensitive equality.

**However, `tools/check-nw07-source-gate.ps1` independently repeated the same mistake**, using:

```powershell
$unique = @($imports | Sort-Object -Unique)
Require ($imports.Count -eq $unique.Count) ("Duplicate Kotlin import: " + $label)
```

Windows PowerShell's `Sort-Object -Unique` groups capitalization-only differences by default. The actual `NightWaveSeekTimeline.kt` has **exactly one each** of the following valid Kotlin declarations:

```kotlin
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
```

One symbol is a class, the other a different Compose semantics property. Kotlin imports are case-sensitive. The reported duplicate is therefore a **false-positive preflight failure**, not a Kotlin compiler error.

## NW-15D implementation

- `tools/check-nw07-source-gate.ps1` now uses a case-sensitive `HashSet[string]` with `System.StringComparer.Ordinal`. It rejects only **identical** repeated import strings.
- A positive PowerShell self-test explicitly allows the exact `ProgressBarRangeInfo` / `progressBarRangeInfo` pair. A negative test checks that the first exact import repeated after that pair is still rejected.
- Source markers, app checks and earlier negative tests are unchanged. The file remains ASCII-only for Windows PowerShell 5.1 compatibility.
- Added `tools/verify_nw15d_nw07_imports.py` to independently check the real SeekTimeline imports and the case-sensitive contracts in **both** import preflights.
- Registered the new NW-15D source verifier and branch in `.github/workflows/android.yml`; the PowerShell NW-07 self-test and real-source gate, the full Android `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` tasks and existing source verification remain in place.
- No modifications to Android/Kotlin runtime, UI, package ID, signer, local music, playlists or Google backup information.

## Verification boundary

- The NW-07 source gate and the SeekTimeline file were read back from the exact GitHub NW-15C commit. The faulty `Sort-Object -Unique` comparison was identified directly.
- The case-sensitive replacement and explicit positive/negative self-test source are committed; targeted GitHub source consistency checks were performed.
- The NW-15B encoding safeguard and NW-15C first Kotlin preflight fix are retained.
- Windows PowerShell self-tests **have not yet been run in the user's environment with this commit**. The connected GitHub runner has previously failed before recording job steps.
- Gradle did **not** run on NW-15C; no Kotlin compile success, JUnit suite result or newly verified APK can be claimed.

## Safe Windows rebuild

1. Download the **complete NW-15D source ZIP** from the exact commit linked in chat, then extract it into a **new, empty Windows directory**.
2. Start `build-windows.cmd`. The NW-07 import gate should no longer reject the capitalization-distinct Compose imports.
3. Require that Gradle actually starts and `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` all complete successfully. Further failures must be investigated separately.
4. If another failure occurs, provide the new screenshot and the files under `dist/`: `NightWave-build-status.txt`, `NightWave-build-errors.txt`, `NightWave-build-full.log` and `NightWave-build-diagnostic.txt`, redacting credentials and private paths as needed.
5. **Do not uninstall the installed NightWave or clear app data.** A signer-compatible, freshly verified APK is required before in-place installation.

**Disposition:** Root cause of the current pre-Gradle false positive corrected in source. Real Windows build and phone verification still pending.
