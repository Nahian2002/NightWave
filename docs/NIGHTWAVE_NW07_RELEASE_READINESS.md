# NightWave NW-07 — Build, Evidence and Release Gate

**Date:** 22 October 2026  
**Base:** NW-06 commit `77da336bfc5036723f29ea47428503ab7e86a383`  
**Branch:** `fix/nightwave-v2-nw07-build-release-gate`  
**Release decision:** **NO-GO / UNVERIFIED** until Gradle and on-device evidence passes.

## What NW-07 did

1. Added `tools/check-nw07-source-gate.ps1`, a dependency-free PowerShell check covering the NW-01–NW-06 integration markers, critical source files, duplicate imports and presence of regression test sources. Its **negative self-tests** check that missing markers and duplicate imports really cause failure.
2. Updated `tools/check-builder-syntax.ps1` so PowerShell parses the new gate before any Gradle or APK work.
3. Wired the new gate into `tools/build-windows.ps1` before downloading Java/Gradle/Android tools. Missing or mismatched source files fail closed.
4. Added a per-build `dist/NightWave-source-fingerprint.txt` of SHA-256 hashes for ten critical source/Gradle files, sorted by relative path. The build status records the manifest hash; an earlier build's fingerprint is removed before each attempt.
5. Re-hashes source before APK promotion. If tracked files changed during the build or if the source manifest was altered, the candidate is **not** published as verified.
6. Added a `tools/verify_nw07_release_gate.py` source policy check to GitHub Actions, plus the PowerShell self-test and integrated gate. Existing NW-01–NW-06 gates and required Gradle tasks remain in place.

**A source fingerprint is not a Git commit signature or APK reproducibility guarantee.** It associates a particular Windows build's report with a bounded list of source files and detects later changes during that build. Other source files are still compiled by Gradle but not part of this short fingerprint.

## Existing build behavior retained

- `build-windows.cmd` starts the Windows local build; no subscription or GitHub runner required.
- Required Gradle tasks: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`.
- A passing candidate must have the `BUILD SUCCESSFUL` marker and zero Gradle exit code.
- The output APK is checked for `com.nightwave.player`, version `2.0.0-alpha5s`, version code `35` and an authentic signer.
- The source fingerprint, APK SHA-256, signer and Gradle status are recorded in `dist/`.
- The builder preserves the previous APK separately; a failed attempt never promotes its APK.
- `tools/check-device-update.ps1` compares the verified candidate to an attached Android device **read-only**. No uninstall, reset, install or clearing of private music is permitted by these checks.

## Actual test evidence as of this review

| Gate | Evidence | Current result |
| --- | --- | --- |
| GitHub branch | NW-07 source edits committed | Complete |
| Targeted GitHub-connected source inspection | Phase markers, builder wiring and source import checks | Reviewed, not full Android proof |
| New Windows integrated gate | Added to the build path; no Windows execution in this session | **UNVERIFIED** |
| NW-01–NW-06 Kotlin/JVM regressions | Source files and tests authored in earlier phases | **UNVERIFIED** with no new passing run |
| Latest NW-06 GitHub run `39285086913` | Completed `failure`; `118168644780` has **zero reported steps** and job log unavailable | **CI INFRASTRUCTURE/STARTUP UNKNOWN**, not a Kotlin failure diagnosis |
| NW-07 GitHub Actions | Workflow branch added | Requires a runner execution |
| Full Windows Gradle build + APK | No freshly verified `BUILD SUCCESSFUL` and signed APK from NW-07 | **BLOCKED** |
| Live public search, Google Drive OAuth, speaker/Bluetooth equalizer and UI | Needs physical/emulator execution | **UNVERIFIED** |
| Safe in-place Android update | Candidate/installed signer comparison must pass | **BLOCKED** |

## Later combined test instructions

1. Download the exact NW-07 branch/commit ZIP and extract to a **new** folder, without merging old files.
2. On a Windows PC, double-click `build-windows.cmd`. A preflight error is a real **build block**, not something to bypass by disabling the checker.
3. Require `BUILD SUCCESSFUL` and review `dist/NightWave-build-status.txt` and `dist/NightWave-source-fingerprint.txt` alongside the full Gradle log.
4. Check package identity, version and signer. Connect the existing phone with USB debugging and run `tools/check-device-update.ps1` as a **read-only compatibility check**, after full build success.
5. Install as an update **only if signer compatibility passes**. Never uninstall NightWave or clear app-private storage to force a new signature to install.
6. Test rapid public Shuffle, previous/next queue dedup, mixed offline/streaming, 30-minute background playback, seek cancellation across songs, Sound Studio preset/manual gains on real audio routes, Google Drive permission and safe library recovery, search results and no-results, mini-player UI clipping and Lyrics.
7. Share **status**, **errors**, **diagnostic** and **full build log** if the result fails, redacting personal paths, tokens, private media URLs and account identifiers.

## What still needs work after a passing build

NW-08 should be **device-specific bug remediation and release packaging**, not another large list of unverified features. A successful Gradle build does not establish Google identity login (the app currently offers narrow Drive authorization), live lyrics extraction, or Spotify's licensed catalog.

**No NW-07 APK has been certified in this review. Do not advertise production readiness until all P0 build/runtime/data-preservation gates pass.**
