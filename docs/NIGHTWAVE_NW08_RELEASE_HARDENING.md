# NW-08 — Full Source Attestation & Combined Release Hardening

**Date:** 22 October 2026  
**Starting commit:** NW-07 `19a8f52d470e7e492c8abf2b7ad3c68d2fab3422`  
**Branch:** `fix/nightwave-v2-nw08-full-source-build-attestation`  
**Release decision:** **NO-GO** until a real passing Android build and data-preserving phone verification.

## Problem discovered

NW-07 was correctly preventing publication if one of **ten** selected source files changed during a Windows Gradle build. However, edits to a different Kotlin class, unit test, UI XML or build tool might not alter that short fingerprint. The source-evidence mechanism therefore did **not** yet cover the complete application and its test sources.

An older user-supplied Windows log from 19 October compiled Kotlin and assembled the APK but failed `:app:testDebugUnitTest`: 771 JVM tests, one failure in `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection`. Current source has a later fix for invalid jump state and a visible-order Previous rule, plus new regressions. That log is **not** a test result for NW-08 or a reason to remove the failing test.

## Implemented in NW-08

- Added `tools/get-nw08-source-inputs.ps1`, a dependency-free and read-only source enumerator:
  - Includes **all files** under `app/src/` (Kotlin, Java, Android XML/resources, fixtures, unit and instrumentation test sources).
  - Includes mandatory Gradle configuration, proguard, Windows build entrypoint and source-checking scripts from `tools/`.
  - Excludes generated `app/build/`, `dist/`, caches, old APKs and user private libraries.
  - Rejects missing mandatory configuration and symlinked source/tool files. Emits stable sorted project-relative paths.
- Updated `tools/build-windows.ps1`:
  - Executes the PowerShell enumerator's self-test before Gradle.
  - Creates `dist/NightWave-source-fingerprint.txt` covering the expanded source inventory.
  - Saves SHA-256 hash of the manifest and source-file count in `NightWave-build-status.txt`.
  - Re-enumerates all candidate source paths **after** Gradle and before APK publication. Added/deleted paths, changed content or altered manifest block the new APK.
  - Retains the prior failure-report, APK SHA-256, package/version, signer verification and previous-APK protection.
- Registered the new enumerator in `tools/check-builder-syntax.ps1`.
- Added `tools/verify_nw08_attestation.py` with deterministic local fixtures that catch edits, additions, removals and missing build configuration; it also checks CI and Windows build integration.
- Added NW-08 verification and enumerator self-test to the Android CI workflow while keeping NW-01 through NW-07 controls and Gradle tasks.

## What was actually checked

- GitHub source branch/changes were created and code markers reviewed.
- An **independent Python SHA-manifest fixture exercise** ran successfully outside Android for unchanged, edited, newly added, removed and missing-configuration file scenarios. This checks the algorithm, not PowerShell or Gradle.
- The live PowerShell enumerator, Gradle tests and APK signer were **not executed successfully in this workspace**.
- Recent GitHub Actions NW-07 runs ended as `failure` with **zero recorded job steps**; that is an infrastructure/runner-start issue with unknown root cause, not proof of a Kotlin build failure.

## Release and safety limitations

1. A SHA inventory is a **build-time change detector**, not proof of the compiled binary's source provenance, a signed source attestation or deterministic APK reproduction.
2. The fingerprint records the Android source tree, tests and build tooling. It is not meant to fingerprint external SDK/Gradle dependency binaries; existing Gradle checksum and APK signature checks remain separate.
3. Kotlin, Android instrumentation-source and unit tests are **not passed** merely because a source gate succeeds.
4. The debug APK must still match the certificate of the **installed** NightWave app before a data-preserving upgrade.
5. NightWave Google Drive consent is not full Google identity login. Real network search, EQ audio routing and Cloud consent must still be tested on Android.
6. No user data was migrated, removed or reset by NW-08.

## Deferred combined acceptance

1. Download the exact NW-08 source ZIP and extract to a **new folder**. Do not mix old extracted source files.
2. Double-click `build-windows.cmd`; require all PowerShell self-tests and source checks, `BUILD SUCCESSFUL`, zero Gradle exit status, and the complete `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` suite.
3. Verify `dist/NightWave-build-status.txt` includes `SUCCESS`, an APK SHA-256, source fingerprint SHA-256 and nonzero source-file count.
4. Verify `dist/NightWave-source-fingerprint.txt` has many source/test entries, **not just the ten files from NW-07**. Any modification during build must fail.
5. Run `tools/check-device-update.ps1` only after a passing APK build, with exactly one authorized Android device attached. This comparison does **not** install anything.
6. Install as an update only when existing/candidate signing identities match. **Never uninstall or clear app data** to defeat an incompatible signature.
7. Execute the combined NW-01–NW-07 tests: rapid public Shuffle; offline jump/Previous; seek gestures across song changes; EQ on speaker/headphones; You → Account & backup authorization and safe restore; search Songs/Videos empty-state; clean Now Playing/miniplayer and long background audio.

**Remaining blocking evidence:** First full passing Windows/CI compilation + JVM tests, then signer compatibility and real-device acceptance. Source work can continue separately without claiming Spotify-level reliability or 100% production readiness.

**Recommended NW-09 after a green build:** Narrow real-device fixes only, guided by observed crash/audio/UI/Google logs.
