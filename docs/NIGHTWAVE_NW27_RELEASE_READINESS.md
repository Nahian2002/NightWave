# NightWave NW27 — Final Source Hardening & Release-Readiness Review

**Date:** 24 October 2026. **Branch:** `fix/nightwave-v2-nw27-final-hardening`, stacked on NW26.
**Release decision:** **NO-GO** until the actual Android Gradle compile, signer continuity and device QA gates are passed. No APK was installed or supplied by NW27.

## 1. What changed

### Crash survival and diagnostic privacy

- `NightWaveApp.kt` now catches `LinkageError` separately from `Exception` when initializing the optional YouTube/NewPipe extractor. A missing/incompatible binary dependency should not prevent the app's offline UI from opening. Fatal VM/resource errors are **not** generically swallowed.
- `NightWaveCrashRecorder.kt` now categorizes custom thread names (main, render, runtime or background) instead of copying potentially private thread names (which could include account names, URLs or file paths). Stack symbols and line numbers are bounded and sanitized; exception messages, tokens, paths and entire HTTP responses remain excluded.
- `NightWaveCrashPrivacyPolicy.kt` is pure Kotlin and directly executable without Android.

### Google Drive and background job reliability

- `NightWaveCloudWorker.kt` explicitly rethrows Kotlin coroutine cancellation instead of treating cancellation as a retryable Drive failure. Its completion and error paths now use guarded, atomic `CloudBackupState.successIfCurrent`/`statusIfCurrent` writes rather than a vulnerable check-then-write sequence.
- `CloudBackupConsentPolicy.kt` and `CloudBackupState.kt` add a persistent consent revision bumped on every connect, disconnect, or backup privacy/network setting change. Disconnect and reconnect to the **same** account cannot make an old worker appear current (the previous consent values alone had an ABA race). The new guarded writes serialize against account/settings updates in one application process. The existing library, backup preference name, tokens policy, and interactive first-backup method remain unchanged.
- `NightWaveDriveClient.kt` refuses streams with nine consecutive zero-byte reads and validates every append with subtraction-based byte limits. This avoids endless zero-read spins and integer-overflow-prone size addition. No extra OAuth scope, server, credential storage or broad redirect following was added.
- `DriveResponseReadPolicy.kt` is pure Kotlin and shared by the actual client.
- Previously implemented NW26 Drive consent snapshots, structured 401/403 vs 408/429/5xx failure handling, app-data-folder-only snapshots and checked remote readback remain in place.

### Native Windows and CI release safety

- Added `tools/check-nw27-integrity.ps1`: a **Windows PowerShell 5.1-compatible** read-only source preflight (17 required source checks) with a positive and negative synthetic self-test. One-click `tools/build-windows.ps1` now runs it before any Gradle build.
- Registered that script in `tools/check-builder-syntax.ps1` so the existing PowerShell syntax/ASCII gate also checks NW27.
- Added `tools/verify_nw27_hardening.py`: fail-closed source/integration scanner checking crash privacy, Google Drive scopes, cancellation, stream limits, unchanged app and local music identity, Gradle tasks and existing APK signer continuity.
- Added `tools/nw27_hardening_smoke.py` and `tools/kotlin/NW27HardeningSmoke.kt`: executable exact-source Kotlin regression tests for crash summary privacy, bounded code symbols/line numbers, disk-free pure Drive byte-limit calculations, stalled responses, and consent-revision fencing.
- Added permanent `tools/nw27_state_smoke.py`, `tools/kotlin/NW27StateAndroidStub.kt` and `tools/kotlin/NW27StateRaceSmoke.kt`. CI compiles the exact production state with the in-memory Android API stub and tests same-account reconnect, privacy toggles, stale success/status rejection and current-worker acceptance whenever Kotlin is installed. The stub is outside application sources and is not packaged.
- Added two JUnit regression suites: `NightWaveCrashPrivacyPolicyTest.kt` and `DriveResponseReadPolicyTest.kt`.
- Main GitHub Android workflow now triggers on NW27 and executes the new Python source and Kotlin proof; existing source checks, Gradle/JUnit/Android test-source compile, package/version, fingerprint, and artifact safeguards remain unchanged.
- Enabled the independent `NightWave Runner Canary` workflow for this branch.

## 2. Executed, verifiable evidence

The following **exact, unmodified GitHub** file versions were downloaded through the repository connector, reproduced locally and verified with `git hash-object`:

| Exact file | Git blob SHA-1 |
| --- | --- |
| `NightWaveCrashPrivacyPolicy.kt` | `3e07ab0a6978cf27dc855c93846756f3498bf510` |
| `DriveResponseReadPolicy.kt` | `88f03fcaf98ce0f4995ba46bd90b1907b580788e` |
| `CloudBackupConsentPolicy.kt` | `c4d71ab2ca51e10c4057346ea29e8eec74ebc30a` |
| `CloudBackupState.kt` | `ddba84603aeb291bb9f3e93f492c531d22850c73` |
| `NW27HardeningSmoke.kt` | `eea87c303160994d88b2c57772d5c6a1217597e6` |
| `NW27StateAndroidStub.kt` (test-only) | `7a32e4309867f401661113d3f944b3f7cd5fca09` |
| `NW27StateRaceSmoke.kt` | `51d95b5587e5a7fdc078a213d3d5290661fa3ed0` |

`kotlinc` compiled the four exact policy/test file versions into a runnable JVM jar; `java -jar` returned:

```text
PASS: NW27 33 exact-source Kotlin safety assertions
LIMIT: Android Gradle compilation and device behavior not tested
```

The test cases exercise real production policy code and catch malformed custom thread labels, redaction regressions, negative/oversize byte reads, exact byte caps, integer overflow, stalls, consent changes, and reconnects with the same Google account.

A separate Kotlin compilation used the exact GitHub `CloudBackupState.kt` blob **and the committed test-only Kotlin stub/test versions** (matching SHA-1 hashes above). It passed simulated disconnect/reconnect, stale-success rejection, stale-status rejection, changed-privacy rejection, and current-epoch success. Its Python launcher is now committed and registered in the Android workflow. **This is not an Android instrumentation test**, nor does it prove behavior under process death. This **does not** mean `NightWaveCrashRecorder` itself, the Android WorkManager service or full Google Drive client has been compiled or executed on Android.

## 3. Independent GitHub Actions diagnosis

The one-step, zero-dependency runner canary itself [failed before any steps](https://github.com/Nahian2002/NightWave/actions/runs/40096251397). The new NW27 one-step canary also [failed before any steps](https://github.com/Nahian2002/NightWave/actions/runs/40098183403), after the source changes; the previous NW26 main Android workflow [also failed before any job steps](https://github.com/Nahian2002/NightWave/actions/runs/40095629581). This supports **RUNNER UNASSIGNED**, not a proven Kotlin compiler failure. The cause (billing/spending limits, disabled Actions, organization policy or GitHub allocation) **cannot be determined** solely from step-less job metadata; check run-page banner and repository/account Actions settings. NW27 cannot change GitHub-hosted runner account configuration through application source code.

If GitHub remains blocked, `build-windows.cmd` is the offline-owned **local Windows build path**. It does need external downloads of official Android/Gradle/JDK tools unless already cached, but no private app data is used by the source preflight.

## 4. Release readiness matrix

| Release domain | NW27 evidence | Remaining required step |
| --- | --- | --- |
| Source-only crash privacy and byte-policy | **PASS**, 33/33 production Kotlin assertions; state transition smoke PASS with Android stub | Compile real Android classes and test crash capture |
| NW27 integrated source scripts | Authored and CI/Windows wired; runtime of full-tree scan **unverified** | Execute preflight scripts on complete checkout |
| GitHub hosted CI | **BLOCKED** (unassigned runner; independent canary failed) | Restore runner execution |
| Full Android Gradle compile & JVM tests | **UNVERIFIED** for NW27 | Run `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` |
| APK package/version and signature | Source gates preserved; **UNVERIFIED APK** | Build and inspect current APK; compare actual installed signer |
| NightWave local library data durability | Existing storage implementation unchanged | Test real upgrade, imports, duplicate, reboot, limited storage without clearing data |
| YouTube online search/playback | Startup LinkageError fallback added | Real device playback and background lifecycle tests |
| Licensed offline downloads | Existing NW23 queue retained | Network loss, cancellation, process kill, 429/503, storage pressure |
| Google OAuth and Drive backup | Source error handling and consent protections | Google Cloud Android client SHA/signing identity, Drive API, real picker and verified upload/readback |
| Lyrics | Existing imported LRC feature untouched | Confirm sync and editor UI on device; **automatic real-time lyric extraction not implemented** |
| UI and accessibility | NW25 adaptive layout retained | Screenshots, TalkBack, large font, split-screen device QA |
| Online YouTube-to-MP3 | **NOT IMPLEMENTED** | Separate authorized-content product/design requirements; no unsupported download claims |

### Release conclusion

**NO-GO for installing an NW27 APK** until all required Android compilation, signer, and phone gates are verified. Do not uninstall NightWave, clear its private storage, reset the signing keystore or discard the previous compatible APK just to work around a certificate mismatch. There is no claim of zero bugs or 100% readiness from a source-only phase.

## 5. Deferred single final device QA round

When a successfully compiled APK with a compatible signer exists, verify: start app with network off; Google chooser and backup; safe library migration from already installed NightWave; imported offline media + NW23 queue; background/lock-screen playback; shuffle/loop/gapless/equalizer; full player and navigation; Bangla/English search; LRC editor/sync; notification permission; crash diagnostics; and restricted storage. Preserve old app state before the test. No phone testing was requested or performed during NW27.

## 6. Local Windows build failure and hotfix (24 October 2026)

The user's NW27 Windows build stopped **before Gradle** at the Kotlin compatibility source preflight. The gate correctly identified two invalid `androidx.compose.ui.test.assertExists` imports in:

- `app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveAdaptiveChromeInstrumentedTest.kt`
- `app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveOfflineDiscoveryInstrumentedTest.kt`

The imported symbol is not a top-level Compose UI testing import; `.assertExists()` is invoked on the returned semantics node interaction. Both incorrect import lines were removed without changing the eight existing assertion calls. Exact GitHub file readback confirms both lines are absent after the fix. The existing `tools/check-kotlin-compat.ps1` guard already rejects this import throughout `app/src` on Windows, so it will detect a regression.

**Verification scope:** source changes were committed and read back; a complete Windows Gradle compile, JVM test run, Android instrumentation-source compilation, APK signature comparison and phone test remain **NOT VERIFIED**. Download a fresh ZIP from this branch before retrying `build-windows.cmd`; older extracted ZIPs do not receive GitHub commits automatically.
