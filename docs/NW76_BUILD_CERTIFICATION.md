# NW76 — Build Certification and Core Reliability Gate

**Cumulative branch:** `feature/nightwave-nw76-build-certification`  
**Parent:** `fix/nightwave-nw75-offline-shuffle-junit` (all previous NW75 Windows hotfixes included)  
**Status:** Source integration committed; full NW76 Windows/Android build and fresh Gradle JUnit suite **NOT executed yet**. Public APK / Play release remains **NO-GO**.

## Why this phase comes before visual redesign

The most recent real Windows evidence was **1,117 executed JVM tests with one failure** in `OfflinePlaybackQueueTest.invalidShuffleJumpKeepsHistoryAndUnplayedOrder`. The Hotfix 5 test expectation was corrected to agree with NW69's intended A → B shuffle forward history, but the complete Gradle suite has not been rerun on that branch. The latest user recording also demonstrated Google OAuth internal status 8 after account selection; successful Drive sign-in cannot be claimed.

NW76 does NOT modify the Android playback engine or user data. It strengthens the acceptance gate so a misleading or partial build cannot be treated as a verified APK.

## Implemented

1. **Current-attempt JUnit receipt** in `tools/nw76-junit-proof.ps1`:
   - Load `TEST-*.xml` files from the Gradle `app/build/test-results/testDebugUnitTest` directory. The Windows builder deletes this directory before its Gradle invocation, preventing stale reports from passing.
   - Demand **at least 1,000 tests**—the last real run executed 1,117. This is a regression floor, not a claim every possible test was executed.
   - Require nonnegative suite counters and agreement between `tests` and the number of `testcase` elements.
   - Cross-check `failure`, `error` and `skipped` nodes against suite attributes. Reject any failures/errors; report skips explicitly instead of counting them as passing.
   - Use an XML reader with DTD processing prohibited to avoid loading untrusted external XML entities.
   - Never touch app data, credentials, network services, installed apps, or actual music files.
2. **Fail-closed Windows build**, `tools/build-windows.ps1`:
   - Delete any previous `dist/NightWave-jvm-test-proof.txt` receipt at the start of every attempt.
   - Run self-test fixtures before downloading/building. The self-test checks successful, skipped, missing, undercounted, contradictory and hostile XML.
   - After Gradle returns success for `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, verify the **fresh** unit-test reports with the minimum test floor.
   - Promote the signed/fingerprinted APK to `dist/NightWave-debug.apk` **only after** JUnit certification and existing identity/signature/source-fingerprint checks pass.
   - Record exact summary totals, skips, and the `NightWave-jvm-test-proof.txt` path in the build status. Any certification failure preserves the previous APK separately and leaves the current build failed.
3. **Hosted workflow** `.github/workflows/android.yml`: NW76 push events, exact current JUnit certification after Gradle, and the receipt included with successful CI debug artifacts (still not a signer-compatible phone installation).
4. **Cumulative PowerShell source guard** `tools/check-nw59-integration.ps1`: require proof script, counter checks, stale receipt cleanup, test-count floor and correct certificate-before-APK-promotion ordering. Existing NW53–NW75 source and playback safeguards remain in place.

## Evidence and limitations

- Source changes were read back from GitHub and inspected. A passing static audit or parser fixture does **not** certify Android compilation.
- This assistant does not have the user's installed Windows SDK/Gradle environment, and Windows PowerShell is not installed here. The newly committed PowerShell scripts must be **executed by `build-windows.cmd`** to confirm syntax/runtime compatibility.
- No Gradle JVM/Android test suite was executed for this branch in the assistant environment. The historical 1,116/1,117 run predates NW76 and Hotfix 5's corrected expectation; it is **not a current pass**.
- [Latest hosted Android workflow 40296645208](https://github.com/Nahian2002/NightWave/actions/runs/40296645208) concluded **failure**, but its connected job API provided **no executed-step details**. Its root cause is unknown and cannot be described as a JUnit, PowerShell, or Kotlin compiler failure. A fresh local Windows build remains required.
- No new public build, Google login success, full catalog/offline authorization, audio effects, performance score or phone QA is implied.

## Single-source Windows acceptance

1. Use **Code → Download ZIP** from `feature/nightwave-nw76-build-certification`. Extract into a completely **new Windows folder**, keeping the existing installed app and offline music untouched.
2. Run `build-windows.cmd`. Expected stages include Windows preflights → NW76 JUnit parser self-test → Java/SDK/Gradle → JVM/Android compile → **NW76 JUnit result certification** → signer and source-fingerprint verification → promotion to `dist/NightWave-debug.apk`.
3. A successful build must create `dist/NightWave-build-status.txt` with **SUCCESS**, a valid signer, source fingerprint and JVM receipt; `dist/NightWave-jvm-test-proof.txt` must report **0 failures, 0 errors**, and at least 1,000 tests. A skipped count must be explicitly reported.
4. If failed at any stage, run `send-build-report.cmd` and supply the **fresh** `dist/NightWave-build-diagnostic.txt` and `dist/NightWave-build-errors.txt`. If Gradle started, include `dist/NightWave-build-full.log`; if JVM failed, also include `dist/NightWave-jvm-failure-summary.txt`.
5. Only after fresh complete success, run `check-google-oauth.cmd` and `check-device-update.cmd` before installing over the old Samsung app. **Never uninstall or clear existing app data to fix signing or OAuth failures.**
6. Begin functional device tests (background playback, shuffle, downloads of owned/permitted audio, offline no-network playback, Sound Studio effects, lyrics, repeated cold launches). Verify the OAuth error 8 separately; do not mark backup working on an account chooser alone.

**Exit criterion for NW76:** a user-provided complete **successful NW76 build receipt**, passing JVM XML certificate, APK signer compatibility and first critical Samsung smoke tests. Until then, NW76 source implementation is delivered but **build certification remains pending**. NW77 music-source rights/architecture review should follow actual build certification, not bypass it.
