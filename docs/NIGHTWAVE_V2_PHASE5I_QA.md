# NightWave Phase 5I — Connected-device update safety

**Version:** 2.0.0-alpha5i (versionCode 25)  
**Branch:** `feature/nightwave-v2-phase5i-device-update-check`  
**Status:** Draft source implementation. No new APK or Android-device build has been verified in this environment.

## New feature: read-only device compatibility verification

- A new one-click `check-device-update.cmd` launches `tools/check-device-update.ps1` **only when you choose to run it**.
- It requires `dist/NightWave-debug.apk` from the current build, a SUCCESS build status, the expected version code (25) and an exact matching APK SHA-256.
- It uses Android SDK `adb.exe` (Android Platform Tools), requires exactly **one authorized device**, and runs `adb shell pm path com.nightwave.player` to locate any existing NightWave installation.
- If NightWave is installed, it retrieves **only the APK package** (`base.apk`), not app-private data, music or credentials. Android Build Tools then checks its package name, old version code and signing certificate against the new APK. The temporary pulled APK copy is deleted from the Windows build folder after the check.
- It refuses version downgrades, mismatched signing certificates, unknown installed APK layouts, multiple/unauthorized phones, missing Android SDK tools, incomplete release status, stale/altered APKs and failed verification. Ambiguous results are **INCONCLUSIVE** rather than falsely safe.
- If NightWave is not installed, it verifies the candidate APK and reports **PASS - FRESH INSTALL**, without performing an installation.
- A successful installed-app comparison reports **PASS - IN-PLACE UPDATE SIGNER MATCH** in `dist/NightWave-device-update-check.txt`.
- This is **read-only**: no install/uninstall, no reset, no `pm clear`, no force-stop, no ADB file push, and no modification to the phone or saved music. It doesn't guarantee an update installation will succeed, or that music playback is crash-free.

## Build chain changes

The local Windows `build-windows.cmd` tool now downloads SDK **Platform Tools** alongside Android API 35 and Build Tools 35.0.0. It still runs `:app:testDebugUnitTest` and `:app:assembleDebug`, then validates package name/version, APK signatures and any previous **local** signer comparison before producing a downloadable candidate.

The earlier signing checker compares against a previous locally built APK. Phase 5I adds an explicit comparison against the APK installed **on the connected device**, which is the missing step for preventing data-loss-prone signing mismatch updates.

The PowerShell preflight parses the builder, the signing verifier, **and the new device check** before starting Gradle.

## How to use on Windows

1. Download and extract this Phase 5I branch ZIP into a **new folder**. Do not overwrite your last working build or uninstall NightWave.
2. Double-click `build-windows.cmd`. If it reports failure, do not use an APK from a previous build. Share `dist/NightWave-build-status.txt`, `dist/NightWave-build-errors.txt`, and relevant `dist/NightWave-build-full.log` excerpts.
3. On success, check `dist/NightWave-build-status.txt`: it must name **NightWave 2.0.0-alpha5i**, **build 25**, and an APK SHA-256 and signer digest.
4. Connect only your phone over USB, enable/authorize USB debugging, then double-click **`check-device-update.cmd`**. This checks whether the already-installed NightWave can be updated **without uninstalling**.
5. Read `dist/NightWave-device-update-check.txt`. Only **PASS - IN-PLACE UPDATE SIGNER MATCH** indicates signer/version compatibility with an existing installation. If the phone has no NightWave, the result can be **PASS - FRESH INSTALL**.
6. If it reports **INCONCLUSIVE**, stop. Do not uninstall/clear data or install over a mismatched build. This commonly means ADB is unauthorized, multiple devices are connected, the installed base.apk was unreadable, the installed version is newer, or signatures differ.
7. A PASS is **not an automated installation**. If you explicitly choose to install later, Android Package Manager must still approve the update. Keep a user-accessible backup of your own music and app data where supported.
8. On a disposable emulator, run connected Android instrumentation tests (including classic and premium startup). On the phone, manually verify launch, offline music, explicit Resume, full player, media notification, lock-screen/Bluetooth controls, search fallback and continued listening after reopening.
9. For the previously reported frequent-crash warning, use classic Home's privacy-minimal **Diagnostics · Share recent crash summary** or the Windows USB crash script. Share only reviewed/redacted NightWave exception lines.

## Validation and release gate

The environment used to author this phase has Java/Kotlin but **no Android SDK or Gradle distribution**; a direct attempt to reach the Gradle distribution host could not resolve DNS. **No Android APK, PowerShell script, device check or physical-phone playback test was executed here.**

The previously prepared isolated Kotlin smoke JARs were re-run: 15 offline session/queue checks and 23 gesture/progress checks passed. These are not Android builds or the repository JUnit suite. Source wiring and PowerShell script structure were reviewed and a new `tools/verify_v2_phase5i.py` static gate was added.

Keep this PR **DRAFT**. Stable `main` and the earlier phase branches are unchanged. Complete a real local Windows build, actual signer check on the target device, emulator startup QA and phone playback/crash checks before considering a release.
