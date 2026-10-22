# NightWave NW-15C — Case-Sensitive Kotlin Import Preflight Hotfix

**Date:** 22 October 2026
**Starting commit:** NW-15B `545b6c37853e52df41f339072e5765cd1dfe32a4`
**Branch:** `fix/nightwave-v2-nw15c-case-sensitive-import-preflight`
**Release:** **NO-GO** pending a real, complete Gradle build and Android signer-safe acceptance.

## User-confirmed build failure

The uploaded Windows screenshot and status/full log show:

- `Checking PowerShell build script syntax...` completed with **PASS**. This verifies that the previous NW-15B encoding hotfix resolved the earlier PowerShell parser blocker.
- The next `check-kotlin-compat.ps1` preflight failed with
  `Duplicate Kotlin import in app/src/main/java/com/nightwave/player/v2/ui/NightWaveSeekTimeline.kt: androidx.compose.ui.semantics.ProgressBarRangeInfo`.
- `NightWave-build-full.log` states **Gradle not yet started**.
- No newly verified APK or code-level Kotlin compiler error resulted from this attempt.

## Root cause confirmed from source and Kotlin/PowerShell semantics

At the exact NW-15B GitHub commit, `NightWaveSeekTimeline.kt` contains
two distinct, individually occurring, valid imports:

```kotlin
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
```

`ProgressBarRangeInfo` is a Compose semantics class and
`progressBarRangeInfo` is the separate semantics property used inside
`Modifier.semantics`. Kotlin identifiers are case-sensitive.

The old Windows preflight used
`$imports | Group-Object | Where-Object { $_.Count -gt 1 }`.
Windows PowerShell's `Group-Object` groups strings case-insensitively by
default, treating these two different imported symbols as a duplicate.
**The error is a preflight false positive, not an invalid Kotlin import.**
Removing either valid Kotlin import would be the wrong fix.

## Patch

1. `tools/check-kotlin-compat.ps1` now uses an ordinal, case-sensitive
   `System.Collections.Generic.HashSet[string]` to identify **exact**
   duplicate import paths. The existing user-friendly duplicate diagnostic
   remains for genuinely repeated import statements.
2. The related import membership checks use PowerShell's case-sensitive
   `-ccontains` operator to agree with Kotlin symbol matching.
3. A `-SelfTest` flag verifies that the two different Compose imports are
   accepted and an actual identical repeated import is rejected.
4. Added `tools/verify_nw15c_kotlin_imports.py`: it independently scans
   every Kotlin source file for **exact** repeated imports, contains a
   positive/negative case-sensitive fixture, and checks the original
   `NightWaveSeekTimeline.kt` pair.
5. GitHub CI now invokes the Python source checker plus the PowerShell
   `-SelfTest` and complete Kotlin compatibility preflight when a hosted
   runner actually executes steps.
6. The NW-15B PowerShell ASCII encoding guard, prior NW-01 through NW-15
   features, complete Gradle build gates, package ID, signing configuration,
   Google data and music library are unchanged.

## Evidence and remaining limits

- The exact GitHub source at commit
  `545b6c37853e52df41f339072e5765cd1dfe32a4` was verified to contain
  **one** `ProgressBarRangeInfo` import and **one** `progressBarRangeInfo`
  import. No source-level Kotlin fix is required for that pair.
- The PowerShell comparator is corrected and regression checks were
  committed to GitHub. Their execution on Windows remains to be verified.
- Source verification and PowerShell syntax checks **are not Kotlin
  compilation**.
- The uploaded `NightWave-build-full.log` does not contain Gradle errors
  because Gradle never started during this attempt.
- GitHub-hosted Actions runs have previously failed before executing their
  first step. The CI additions are not counted as passed until they run.
- No newly built APK, APK signing comparison, or device playback test exists
  for NW-15C.

## Clean Windows rebuild

1. Download the exact **NW-15C ZIP** linked in chat.
2. Extract into a **new empty directory** (do not overwrite an old extraction).
3. Run `build-windows.cmd`. The source check should no longer call the valid
   Compose imports duplicates. If the next check fails, the builder should
   stop with a new, specific failure report.
4. Send the new screenshot and the new `NightWave-build-status.txt`,
   `NightWave-build-errors.txt`, and `NightWave-build-full.log` only
   if it fails. Redact tokens and other secrets.
5. A successful build must still complete all three Gradle tasks and produce
   a verified APK with a signer compatible with the already-installed app.
   **Do not uninstall or clear the current NightWave app to bypass signing.**

**Disposition:** Confirmed false-positive Kotlin preflight fixed in integrated
source. The next full Windows build is still the release gate.
