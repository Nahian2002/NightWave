# NightWave NW-11 — JVM Failure Evidence and Offline Build Recovery

**Date:** 22 October 2026  
**Base:** NW-10 `ca6a4eb53a127019324e55d294d669efd4ce10f3`  
**Branch:** `fix/nightwave-v2-nw11-offline-compiler-proof`  
**Release state:** **NO-GO** until a full Android build with passing JVM tests and signer-safe device verification.

## Why this phase

GitHub Actions runner canary `39771087799` and full Android workflow `39771087753` both failed without executing any steps. Another fresh workflow push cannot diagnose Kotlin while jobs cannot begin; a local Windows build is the actionable alternate compiler path.

The last user-provided Windows build log (19 October) is from a substantially earlier branch. It compiled Kotlin and assembled an APK but failed 1 of 771 JUnit tests: `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection`. More recent source includes a changed queue implementation, **but those tests have not been rerun on NW-11**.

Historically the Windows builder summarized a failed test's **name** but required opening the full local XML/HTML Gradle report to inspect test expectations. NW-11 helps preserve a small, current-attempt error report without collecting private music data.

## Implemented

- New `tools/nw11-junit-report.ps1` parses `app/build/test-results/testDebugUnitTest/TEST-*.xml` (JVM failure/error entries only), not raw logs or user media.
- The XML reader explicitly rejects DTD processing and external entity resolution; source reports are truncated to 300 test suites and up to 50 reported failures.
- Test identity and error message fields receive bounded privacy reductions for Windows and Unix home paths, auth headers and common token query arguments. The report is **privacy-reduced, not a guarantee of removing every possible secret**.
- Unit failure entries without a `message` attribute fall back to one brief line rather than causing an entire JUnit suite to be discarded.
- New PowerShell self-test uses synthetic JUnit XML with a passing case, a failing case with a message, a failing case without a message, and synthetic credential/path redaction checks.
- `tools/build-windows.ps1` clears only disposable **Gradle test-result XML** before the new test run. This avoids reporting a previous build's results when a new build fails during Kotlin compilation.
- On a Gradle failure, the builder invokes the reporter as an **optional, non-blocking diagnostic**; reporter errors do not change the original **FAILED** outcome. It writes `dist/NightWave-jvm-failure-summary.txt` when available.
- `tools/check-builder-syntax.ps1` checks the reporter's PowerShell syntax; `tools/verify_nw11_junit_source.py` adds Python fixture and integration checks to CI.
- NW-01–NW-10 features, application/package/signing configuration, personal library and cloud backup format remain unchanged.

## Verified vs unverified

**Source evidence:** New files and integrations were committed to GitHub, with targeted string/source contract review. Python's independent sample JUnit fixture logic was authored; if a Python test pass is reported separately, that does **not** establish the PowerShell tool's real execution.

**Still unverified:** The NW-11 PowerShell reporter under Windows, the full `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` sequence, test XML from an actual current build, signed APK compatibility, and Samsung phone acceptance.

A GitHub job that fails with zero executed steps is **not a Kotlin test result**, even if its overall conclusion reads `failure`.

## Combined Windows acceptance later

1. Download the exact NW-11 source ZIP into a **new** extracted directory.
2. Run `build-windows.cmd`. Never bypass its source, signer or test gates.
3. If the Gradle stage fails, review `dist/NightWave-build-status.txt`, `dist/NightWave-build-errors.txt` and **`dist/NightWave-jvm-failure-summary.txt`** if it exists. The latter should contain only failing test names/bounded messages, and never mark missing XML as success.
4. If Kotlin compilation fails before the JUnit task, expect a **NO TEST XML** diagnostic rather than old test failures.
5. If Gradle passes, confirm a new APK hash and source manifest. Run the existing **read-only** installed-device signer comparison before installing anything.
6. Preserve app data: **do not uninstall the existing NightWave app** or clear storage to force an incompatible debug signer.
7. Later perform combined Samsung UI, live playback, Google consent, Sound Studio EQ, Shuffle, queues, search and background playback tests.

## Next release gate

The next meaningful evidence is a **fresh successful Windows build** (or restoration of actual GitHub hosted-runner execution), not another source-only upgrade. The updated failure reporter makes subsequent test failures easier to identify and repair when that build runs.
