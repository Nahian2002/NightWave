# NightWave Phase 5H — APK update-safety and executable build gates

**Version:** `2.0.0-alpha5h` · **versionCode:** `24`  
**Branch:** `feature/nightwave-v2-phase5h-apk-signing-gates`  
**Status:** Source ready for local build; no Android APK or real-device verification has yet succeeded here.

## What was added

1. New `tools/verify-apk-signature.ps1`, executed by the existing Windows builder, verifies **Android package name and version** via `aapt` and cryptographic **APK signatures** via Android Build Tools `apksigner verify`, using Android API 26 as the minimum. Refuses unreadable, unsigned, wrong-package/wrong-version, or unexpected multiple-signer candidates.
2. **Signer continuity:** When the builder has a previously verified `dist/NightWave-debug.apk` (archived automatically as `dist/NightWave-debug-previous.apk`), the new APK must have the **same SHA-256 signing certificate digest**. Otherwise it is blocked to protect in-place updates and existing app data. This compares the two local APKs, **not the version currently installed on the phone**. The latter still requires a separate device/install signature check.
3. The builder publishes `dist/NightWave-debug.apk` **only after** Gradle's BUILD SUCCESSFUL, the package/version gate, apksigner verification and applicable signer continuity. A success status includes the APK's SHA-256 checksum, signer digest and continuity outcome.
4. The preflight `tools/check-builder-syntax.ps1` now parses **both** the Windows builder and signing verifier. If either is missing/invalid, it writes a failed status and a diagnostic report, archives an old published APK (if safely possible), and stops before Gradle.
5. Added `app/src/androidTest/java/com/nightwave/player/diagnostics/NightWaveStartupSmokeTest.kt`. On a **disposable emulator/profile**, two instrumented tests launch classic MainActivity and premium NightWaveStudioActivity and assert neither immediately finishes/destroys. These are not tests of YouTube service or background playback.
6. Added `tools/verify_v2_phase5h.py` static source gate. Older phase source gates accept build 24. The Media3 service, YouTube extractor, offline library, imported audio files, profiles and preferences are unchanged.

## Tests and exact limitations

- The optional `tools/verify_v2_phase5h.py` is a **source-only** consistency check. It is not an APK build or a signature-verification execution.
- Real JVM behavior checks from prior phases were re-run on this environment: 23 PlayerGesture/Progress rules plus 15 offline session/queue checks **passed**. These are unchanged sources and **do not compile Android UI code**.
- New source check and signing verifier script were read back and inspected, but **Windows PowerShell runtime and its actual parser are not installed in this environment**. Do not confuse matched strings or balanced braces with successful PowerShell execution.
- No Android SDK or Gradle distribution is installed in this environment, and downloads are blocked. Therefore **no APK was assembled, no Android JUnit/instrumented suite was run, and no device crash root cause is verified**.
- This gate prevents **many** incompatible builds but cannot prove signing compatibility with an already-installed APK unless that exact APK is supplied/verified; Android's Package Manager remains the authority on update acceptance.
- If a previous APK uses a different signing certificate, **do not uninstall** and do not clear the app to work around the verifier. Preserve existing music, recover the original signing key, or seek an explicit migration plan.

## Windows build and non-destructive Android acceptance

1. Download the complete source ZIP for this branch, extract to a new folder, and launch `build-windows.cmd`. This remains a local build and does **not** spend GitHub Actions minutes.
2. Optionally run `python tools/verify_v2_phase5h.py` first. This checks source wiring only.
3. When the builder completes, review `dist/NightWave-build-status.txt`. A **SUCCESS** must contain `com.nightwave.player`, versionCode **24**, `2.0.0-alpha5h`, a **Signer SHA256** value and **SHA256** for the APK.
4. On failure, send `dist/NightWave-build-status.txt` and `dist/NightWave-build-errors.txt`. If the error references Gradle/Kotlin, include relevant excerpts from `dist/NightWave-build-full.log`. An old successful APK is kept as `dist/NightWave-debug-previous.apk`, where possible.
5. Verify Android's in-place update acceptance using the previously used signing key. Never uninstall or clear app data to circumvent a signature mismatch.
6. On a disposable Android emulator with this build, run `connectedDebugAndroidTest` to execute the new ActivityScenario startup tests, avoiding a real device containing irreplaceable music.
7. On the Samsung phone, test classic launch, premium launch, music discovery, offline files, explicit Resume, full-screen player/swipe gestures, background playback, media notification, lock-screen/Bluetooth transport controls, and relaunch after long background idle.
8. If Android still says NightWave crashes, use classic Home's **Diagnostics · Share recent crash summary** or `capture-nightwave-crash.cmd` on Windows. Send a **redacted** NightWave FATAL EXCEPTION block, not an unreviewed whole-device log.

**Release gate:** Keep the Phase 5H PR in DRAFT until real Gradle success, APK signature verification, emulator checks and physical-device playback/launch QA are complete. Stable `main` stays unchanged.
