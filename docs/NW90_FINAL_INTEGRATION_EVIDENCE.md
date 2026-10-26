# NW90 — Final integration evidence and release decision

**Audited on:** 26 October 2026  
**Cumulative parent:** `feature/nightwave-nw89-lyrics-accessibility-performance`  
**Audited branch:** `feature/nightwave-nw90-final-integration-certification`  
**Release decision:** **NO-GO** for public distribution and **NOT YET CERTIFIED** as a final Android update. The NW76–NW89 feature-source roadmap is complete; the NW90 runtime/device/release acceptance gates are not.

## What NW90 actually delivered

- Restored the `.github/workflows/android.yml` push trigger for the cumulative NW90 branch. Its Gradle plan still includes `:app:testDebugUnitTest`, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`, JUnit XML proof with a 1,000-test floor, APK identity, NW77 release source report, and redacted failure diagnostics.
- Added NW81–NW89 Python source contracts and NW90 fail-closed evidence report steps to the workflow. If a runner actually reaches a test, source/JVM/build information will not be misreported as a real-device certificate. CI artifacts are explicitly developer/debug only.
- Added `tools/nw90_certification.py`: standard-library source/test evidence tool; rejects malformed/duplicate 139-case rows, cannot mark a case PASS without evidence, parses fresh JUnit `TEST-*.xml` safely (rejects DTDs and bad counters), and hashes supplied APKs without claiming signer compatibility.
- Added `tools/nw90-windows-gate.ps1` and `run-nw90-final.cmd`: one-click Windows source/matrix preflight → existing current-attempt official debug builder → independent post-build JUnit/APK SHA check. The scripts **do not install/uninstall**, clear app storage or connect to Google. The separate `check-device-update.cmd` remains mandatory before updating the installed Samsung app.
- Added `docs/NW90_EXPANDED_139_CASE_MATRIX.csv`: **139 newly enumerated** specific cases across startup, audio playback, queues, offline storage, transfers, Search, playlists, Drive, EQ, lyrics, visual accessibility and public release. **All 139 are `NOT_RUN`**, with no invented ratings or screenshots. This expands the 31-case checklist found in `docs/NIGHTWAVE_FINAL_ACCEPTANCE_20261019.md`; it is not falsely identified as a previously executed original 139-case report.
- Repaired the README's unusably long phase front matter (including literal `\\n` escapes); the historical announcements were moved to `docs/NW90_ARCHIVED_PHASE_BANNERS.md`. The main README now leads with the actual current source, source-rights restrictions, evidence path and Windows verification.
- The initial NW90 handoff compared ten production files with NW89 and found them identical. A subsequent internal defect audit deliberately changed **two production files**: `OfflineLibraryActivity.kt` (file-health scans off the UI thread) and `PlaybackService.kt` (Manual EQ needs a complete band curve). The remaining eight protected files remain unchanged; no installed music, saved library schema, OAuth credentials, signing or media-source permissions were altered.

## Final internal defect sweep

Three concrete code issues were addressed after the initial NW90 source handoff:

1. **Offline Library UI responsiveness:** On Resume previously cleared the cached report and directly called a `refresh()` that synchronously ran the full `library.healthReport()` scan. Import, delete and filtered redraws could do the same. The existing background worker now performs health checks; repeated requests coalesce and rendered results are generation-guarded. Real 1,000-file phone responsiveness remains unmeasured.
2. **Manual EQ false active receipt:** `PlaybackService.applyEqualizer()` previously accepted manual mode with an incomplete or empty set of band levels, and could mark it active without a complete device curve. It now rejects missing band indices. Legacy single-band commands seed untouched bands from the validated current-session snapshot, or reject safely.
3. **Stale JVM test pass evidence:** `tools/nw90_certification.py` previously treated valid JUnit XML counters as PASS without a successful current Gradle build log. It now demands `--build-log`; otherwise the counters-only evidence is `NOT_VERIFIED`. CI passes the current build log.

Added `tools/verify_nw90_final_audit.py` to CI. Connected GitHub source readback verified the new bindings and braces. **A full current-branch Android/JUnit build and on-phone runtime testing remain unexecuted.**

## Actual hosted runner evidence

The triggered Android workflow ran on the NW90 branch, but failed before performing a single step. Representative GitHub Actions runs:

- <https://github.com/Nahian2002/NightWave/actions/runs/40349898651> — Android workflow; failed before any steps.
- <https://github.com/Nahian2002/NightWave/actions/runs/40350426930> — Android workflow; one job, zero steps, failure in about 3 seconds.
- <https://github.com/Nahian2002/NightWave/actions/runs/40350741728> — deliberately minimal runner canary (no checkout/dependencies), also one failed job and **zero steps**.
- <https://github.com/Nahian2002/NightWave/actions/runs/40350741905> — full Android job on the same commit, zero steps.

Because even the no-checkout canary could not execute, this evidence indicates a hosted-runner allocation/account/workflow-start blocker. The available API does **not** establish the exact GitHub billing/security/runners failure. **It does not establish a Kotlin compiler error, nor does it establish that the Kotlin application compiles successfully.** Do not label these jobs as Android regression failures.

**Fallback:** use the included Windows one-click build on a clean checkout of the exact NW90 branch (or fix GitHub Actions runner provisioning first). Keep your installed NightWave app untouched until the signer check explicitly matches.

## Evidence status by gate

| Gate | Evidence/status | What is required to clear |
| --- | --- | --- |
| Exact cumulative NW90 source | **PASS — repository source committed and read back** | Confirm fresh Windows clone/ZIP matches branch and fingerprint |
| Nine source-contract runners | **NOT RUN in hosted workflow** | Complete each against the current checkout, without assertion failures |
| Complete JVM suite, >=1,000 tests, zero failures | **NOT VERIFIED for NW90** | Fresh `testDebugUnitTest` receipt, verified `TEST-*.xml` and no skipped critical tests |
| Android app compilation/debug APK | **NOT VERIFIED for NW90** | Successful `assembleDebug` and source-fingerprinted current APK |
| Android test compilation/instrumented runtime | **NOT VERIFIED** | `compileDebugAndroidTestKotlin` and run instrumented tests on emulator/device |
| Signing compatibility with installed app | **NOT VERIFIED** | Pass `check-device-update.cmd`; do not uninstall or replace app data |
| 139 Android acceptance scenarios | **0/139 executed; all NOT_RUN** | Execute on owned test files, test Google account, and real device |
| Google OAuth production/onboarding | **Owner previously confirmed personal Google connection** | Production consent/signing + fresh account, Drive backup/readback and safe restore |
| Bluetooth, lock-screen, offline/EQ/crossfade/lyrics | **NOT VERIFIED on NW90** | Real Samsung plus other representative route/emulator tests |
| Google Play Android target API | **BLOCKED for new phone submissions**: source targets API 35 | Migrate to API 36+ and repeat behavior/device tests; check extensions if applicable |
| Release AAB/production signing | **NOT VERIFIED** | Owner-controlled upload key, Play App Signing and release-signer testing |
| User data, privacy/security/history scan | **NOT VERIFIED** | Full history secret scan, privacy policy, Play Data safety declarations and content provenance |
| YouTube independent/background/offline audio rights | **BLOCKED (NW77)** | Compliant redesign or authorization + actual recording/content rights |

### Why the Play API gate is separate from the private debug build

On 26 October 2026, the project's `app/build.gradle.kts` still has `compileSdk = 35` and `targetSdk = 35`. For new Android phone apps/updates submitted after 31 August 2026, Google Play ordinarily requires targeting Android 16 / API 36 (subject to applicable extension). Documentation: <https://support.google.com/googleplay/android-developer/answer/11926878>.

Changing the two numbers without validating edge-to-edge behavior, SDK/tool compatibility, media services, permissions and Android 16 runtime changes is *not* a certified migration. NW90 explicitly blocks Play certification rather than silently making an untested API bump.

### Why source-rights review remains NO-GO even after a successful local build

The original NW77 source audit identifies YouTube-derived standalone audio/background extraction, YouTube audio offline copying and associated CDN URLs. An APK compiling, a user personally owning music, or an OAuth account connecting is not authorization for app-wide public distribution of restricted recordings or platform content. Public release requires compliant media-source design and actual rights/approval, plus separate Google Play policy review.

## Final user-side execution — do not uninstall NightWave

1. Open the NW90 branch using **Code → Download ZIP** and extract to a **new directory** rather than overlaying older source. Do not delete any installed audio data.
2. On Windows run **`run-nw90-final.cmd`**. It checks matrix integrity, launches the existing fail-closed full build, and validates current JUnit/APK output. If it fails, run `send-build-report.cmd` and review/share only the fresh redacted diagnostic.
3. If and only if successful, use **`check-device-update.cmd`** with the Samsung device connected and USB debugging authorized to check the currently installed signature. A GitHub CI-signed APK is **not** assumed compatible with the owner's APK. **Never uninstall to fix a mismatch.**
4. Exercise all P0 and relevant P1 cases in `docs/NW90_EXPANDED_139_CASE_MATRIX.csv`, entering the actual observed status and a non-sensitive evidence reference. Use disposable backups/audio before any destructive restore.
5. Complete Drive account/backup/readback/preview, background audio, search, app lifecycle, EQ on each available audio route, accessibility, crash and battery evidence. Only then measure UI/music quality against the 9.5+/10 goal.
6. For public distribution separately address API 36, production AAB/signing, privacy/security/license and NW77 source authorization. No debug APK should be advertised as a certified public release.

## Internal product rating — provisional, source-informed

**7.7/10 estimated overall product experience**, based on implemented source features and previous UI evidence. This is not a measured NW90 APK review or a guarantee of correctness.

| Dimension | Estimate /10 | Main remaining uncertainty |
| --- | ---: | --- |
| UI and visual design | 8.2 | Current Samsung screenshots and accessibility at large fonts |
| Search and music discovery | 8.0 | Provider coverage and real-world multilingual result quality |
| Playback and queue | 8.0 | Actual Bluetooth, interruptions, shuffle/seek stress |
| Offline library/transfers | 7.7 | Large indexed libraries, permitted downloads and recovery |
| Playlists, collections and You | 8.3 | Bulk edits, retention and process-restart persistence |
| Sound Studio and audio fidelity | 7.3 | Audible device-route EQ, transition quality, normalization |
| Lyrics and accessibility | 7.6 | User-imported LRC only; real TalkBack and long-session profiling |
| Google backup and recovery | 7.4 | Connected account but backup/readback/restore not fully certified |
| Build stability and engineering assurance | 6.5 | No fresh complete Android/JUnit/device test run |
| Public-release readiness | 2.5 | Source rights, API 36 migration, privacy/AAB and acceptance |

Scores are **subjective estimates**; no new phone/video or final completed 139-case data are available. NightWave's 9.5+/10 target remains unverified.

## Final engineering disposition

**NW76–NW89 source milestones: delivered. NW90 verification infrastructure and expanded acceptance matrix: delivered. NW90 build/phone certification: blocked/pending. Public APK/Google Play approval: NO-GO.**

There are no observed full current-branch compile results, device-test passes or production-release artifacts at the end of this audit. Further defect fixes must be based on real source/compiler/phone evidence, not speculative changes.
