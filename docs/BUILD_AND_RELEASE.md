# NightWave — Local Build, Safe Update and Release Procedure

**Updated:** 22 October 2026  
**Current integrated source:** [NightWave NW-07 build-release gate](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-nw07-build-release-gate)  
**Current status:** NW-07 source preflight and build fingerprint code committed; **no fresh successful Gradle build, compatible installed-device signer or phone acceptance is verified**. Release NO-GO.

## NW-07 evidence added

The Windows builder now runs `tools/check-nw07-source-gate.ps1` and its
self-test before Gradle, and saves `dist/NightWave-source-fingerprint.txt`
with SHA-256 hashes for key source files. The manifest hash appears in
`NightWave-build-status.txt` after the build. The builder rechecks those
source hashes before publishing the verified APK; a changed source fails the
attempt. This **does not** replace Android compilation or test execution.

GitHub Actions may show a completed failed run without any job steps. For
example, NW-06 run `39285086913` had no recorded steps and no retrievable
logs; that cannot diagnose Kotlin source failures. When hosted runners do not
execute, use a fresh Windows ZIP and `build-windows.cmd` to obtain real
compiler and test evidence. See [NW-07 QA](NIGHTWAVE_NW07_RELEASE_READINESS.md).

## 1. Get the correct source

Use **Code → Download ZIP** from the linked development branch, **not** the default `main` branch and not a prior extracted folder. Extract the complete archive into a short new Windows directory. The source ZIP is not an APK, and you do not need to merge separate patches.

Do not uninstall the installed NightWave, clear its data or delete imported music, playlists or LRC to solve any build/signing problem.

## 2. Run the local Windows build

Double-click `build-windows.cmd` at the extracted project root.

The builder checks PowerShell scripts and source compatibility, then provisions local build tools if necessary and invokes Gradle:

```text
:app:testDebugUnitTest
:app:assembleDebug
:app:compileDebugAndroidTestKotlin
```

Source-import checks and synthetic privacy self-tests are useful **preflights, not compiled verification**. Wait for **`BUILD SUCCESSFUL`** after every required task and verify the final `dist/NightWave-build-status.txt` has a success status tied to this attempt.

If Gradle exits unsuccessfully—even after `assembleDebug`—the current candidate **failed**. Do not install `dist/NightWave-debug.apk` or a preserved `NightWave-debug-previous.apk` on the assumption that it passed.

## 3. Why this verification is required

The most recent user-supplied Windows run (19 October 2026, 13:34 build label) completed main Kotlin compilation, Android instrumentation-source compilation and APK assembly, but `:app:testDebugUnitTest` failed: **771 tests ran, 1 failed**, `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection` (line 43). The Kotlin warnings listed then were not build-blocking. The development branch has since received the queue fix, further boundary-condition corrections and new JVM regressions. **No subsequent full passing Android build is yet confirmed.**

[Verification Matrix](VERIFICATION_MATRIX.md) is the authoritative test-evidence record, and [Final Acceptance Checklist](NIGHTWAVE_FINAL_ACCEPTANCE_20261019.md) covers runtime features.

## 4. On build failure

Preserve and share these files from the newly extracted project's `dist/` folder:

- `NightWave-build-status.txt` — authoritative result
- `NightWave-build-errors.txt` — failing compiler/test excerpts
- `NightWave-build-diagnostic.txt` — compact redacted summary
- `NightWave-build-full.log` — complete Gradle output

The `send-build-report.cmd` tool can prepare a compact report. Remove credentials, account identifiers, local paths and signed stream URLs before sharing raw logs.

Fix the cause of a failed test; never disable it or delete it simply to get a green build. `:app:compileDebugAndroidTestKotlin` checks instrumentation **compilation** only; it does not execute Android instrumentation tests.

## 5. Check identity and signing before installing

Expected application identity:

```text
Application ID: com.nightwave.player
Version name: 2.0.0-alpha5s
Version code: 35
```

A matching package/version is **not enough** to prove update compatibility. Compare the candidate signing certificate with that of the installed application; Android rejects incompatible in-place signers. GitHub-hosted CI debug APKs may have a temporary signing key different from the Windows local build. Record APK SHA-256 and signing information.

If certificates differ, **stop**. Do not remove the installed app or erase data; preserve the current library and resolve signing with the correct key or a planned safe migration.

## 6. Test actual behavior on Android

Use the [Final Acceptance Checklist](NIGHTWAVE_FINAL_ACCEPTANCE_20261019.md). The highest-priority tests cover:

- Launch, crash/ANR, compact UI and reopen/resume.
- Offline imports, song persistence, repeat/shuffle/previous, single-song Repeat All, local data safety.
- Saved public references, search failure states, duplicate queue selection and background playback.
- Bluetooth/headphone routes, Media3 notification control, widget and Sleep Timer.
- Daily Mix, playlists, LRC import/edit/offsets, crossfade, normalization and EQ.
- Optional Google account/Drive backup, safe restore, vault retries and interrupted transfers.
- Android Auto host browse/search/select/play in a parked or simulated environment, if this feature is to be advertised as verified.

A successful build does not prove an on-device feature works; report failures with reproduction steps and safe redacted diagnostics.

## 7. Publication rule

Do **not** publish/tag NightWave as production-ready until the full build, signer compatibility, critical runtime testing, privacy/data-preservation tests and relevant distribution-license review pass. This project intentionally does not require a dedicated paid NightWave backend, but optional Google/external sources still depend on their providers.

Related: [README](../README.md) · [About](../ABOUT.md) · [Changelog](../CHANGELOG.md) · [Verification Matrix](VERIFICATION_MATRIX.md).
