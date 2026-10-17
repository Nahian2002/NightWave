# NightWave 2.0 — Phase 5G Build & Crash Reliability

**Version:** 2.0.0-alpha5g / versionCode 23  
**Source:** `feature/nightwave-v2-phase5g-reliability-diagnostics`  
**Status:** Source changes prepared; Android device crash root cause **not verified**.

## What was repaired

1. **Recoverable Media3 controller:** If a session handshake fails, the premium Activity now releases its failed pending connection and clears the latch that used to block all reconnection. A visible **Reconnect** action allows a bounded retry. On foreground return a new attempt occurs only if no connection is active or pending.
2. **Real disconnect callback:** `MediaController.Listener.onDisconnected` clears stale state when Android terminates or restarts the media service. It ignores callbacks when the Activity is being destroyed, avoiding a resurrection/reconnect loop.
3. **Player UI resilience:** Exceptions while reading a playback state snapshot no longer terminate the premium screen. The screen retains an error message for investigation.
4. **Offline-first startup:** Failure to initialize the optional public-video extractor will be logged without preventing the local app UI from opening. The optional Room/DataStore read-only preview is separately guarded so an initialization error does not replace or delete the original library.
5. **Versioned crash report:** The existing local privacy-minimal crash summary now includes the installed NightWave version. It does not include exception messages, URLs, file paths, account information or audio content.
6. **Windows build failure clarity:** `build-windows.cmd` still uses the free, local builder. The builder now writes `dist/NightWave-build-status.txt` at the start and at all caught failures, even during Java/Gradle/SDK preparation; prior failure reports are removed before the run.
7. **No stale APK confusion:** The previous debug APK is backed up to `dist/NightWave-debug-previous.apk` before an attempt. New `dist/NightWave-debug.apk` is published **only** when Gradle succeeds, Android Build Tools `aapt` validates package identity `com.nightwave.player`, versionCode `23` and versionName `2.0.0-alpha5g`, and a SHA-256 checksum is recorded.

## Verification completed

- GitHub source reviewed against actual project files, existing service/manifest/library boundaries and installer scripts.
- Added `tools/verify_v2_phase5g.py` source-only regression gate. This checks version, Media3 connection lifecycle, startup fallback, build status, old-APK archive, package identity, original data stores and manifest.
- The Android Gradle Plugin 8.9.2 / Gradle 8.11.1 / JDK 17 / API 35 toolchain is within Android's published compatibility table.
- **Not tested in this environment:** PowerShell syntax/runtime on Windows, Java/Gradle Android dependency resolution, Kotlin/Compose Android compilation, repository JUnit/instrumentation, Bluetooth/headset, or real device crash recovery. GitHub workflow runs are not available on this branch.

## Local, non-destructive acceptance

1. Download this branch's full source ZIP to a new folder. Do **not** overwrite the previously working installation or use app-data clearing.
2. Optionally run `python tools/verify_v2_phase5g.py`. This checks source strings, not Kotlin/Android compilation.
3. Run `build-windows.cmd` once. A fresh `dist/NightWave-build-status.txt` must say **SUCCESS** and identify `com.nightwave.player`, `2.0.0-alpha5g`, versionCode 23 and a SHA-256. **If it says FAILED, do not install the APK.** The previous successful debug APK is kept as `dist/NightWave-debug-previous.apk`. The error report is `dist/NightWave-build-errors.txt`.
4. If the status and Gradle output pass, install as an **in-place update** only if the debug signature matches your existing installation. Never uninstall to bypass a signature mismatch.
5. Start offline with Wi-Fi disabled. Verify classic UI opens, imported files remain, and the premium UI opens even if native video search is unavailable.
6. Exercise Home, Search, Library, offline Resume, the full-screen player, Back navigation, artwork swipe, Play/Pause, Up Next, Bluetooth and screen-off media buttons.
7. To check a controller restart on an **emulator or disposable test account**, force-stop or kill the media process, return to the app and tap **Reconnect**. It must not spin in an infinite reconnect loop or play unexpectedly when a MediaController binds.
8. On the S24 Ultra, use classic Home's **Diagnostics · Share recent crash summary** first. It contains exception classes and NightWave stack frames only. Include the app version shown in the summary.
9. If that summary is empty and Android still warns of crashes, run `capture-nightwave-crash.cmd` on your PC with USB debugging and share only a carefully redacted `FATAL EXCEPTION` block for `com.nightwave.player`. The raw log file may contain personal and other-app information: do **not** upload the whole unreviewed raw log.
10. If the SDK/Gradle preflight fails, upload `NightWave-build-errors.txt` plus the status. There is no reason to reinstall or reset the phone.

## Known limitations and next release gate

- This phase fixes identifiable **recovery and diagnostic gaps**, but no device trace has yet established the original frequent-crash cause. The app is **not proven crash-free**.
- Build 23 is not a verified APK until a real Android build and phone acceptance succeed.
- No changes were made to YouTube download behavior, signing keys, data storage, legacy library entries, imported audio bytes, or app identity.
- Stable main remains unchanged. Do not merge this draft into earlier unverified phases until actual build and runtime acceptance.
