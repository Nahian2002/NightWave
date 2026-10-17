# NightWave 2.0 Alpha 5K — Crash triage and race-safe transport

**Version:** 2.0.0-alpha5k (code 27)  
**Branch:** `feature/nightwave-v2-phase5k-crash-triage`  
**Status:** Draft source implementation, not a verified phone APK.

## What was changed

### 1. Avoid a specific controller-disconnect crash race

The premium player's Play/Pause and Seek controls previously called Media3 Controller methods without locally catching synchronous `RuntimeException` failures. If Android disconnected or released the session between composing the UI and a user's action, the button callback could throw out of the Activity. The callbacks now catch these failures and place the UI into an explicit **Reconnect** state, dropping only the stale Activity-level controller. This does not clear the saved queue or stop the background service, and the fix cannot be claimed to address the earlier device warning until its crash trace is inspected.

### 2. App health in premium NightWave

The premium **You** profile now includes **App health → View / share crash summary**, available without opening classic NightWave. The button displays the **exact report it will share** on the phone first and requires an explicit **Share summary** confirmation. The newest app exception history is capped at 5,000 characters so the important Android exit-history section is never hidden behind old entries. No data is uploaded automatically. Classic NightWave retains its own diagnostic share action.

### 3. Android system exit history, privacy-minimized

New `NightWaveExitHistory.kt` reads up to five process-exit reasons from Android's `ActivityManager.getHistoricalProcessExitReasons` on Android 11+; Android 10 and earlier report the limitation clearly.

It shows only **UTC timestamps and coarse exit reasons**, such as Java/Kotlin crash, native crash, ANR, low-memory termination, startup failure, user stop, or other. It explicitly does **not** read system trace streams, human-readable system exit descriptions (which may contain private details), device identifiers, user account info, process names, song titles, or file paths. This is useful when the existing uncaught-exception recorder is empty but Android says the app frequently crashes. It cannot guarantee that every event appears in the system's limited history.

An Android instrumentation test was added to ensure the summary is bounded and can be read without modifying app data. It is **authored but not run** in this environment.

## Additional release-blocking fix

The read-only Windows device-check script still compared its build-status text
against **alpha5i**, even though the app, builder and device-check version
variables had moved forward. A correctly built newer APK could therefore be
rejected as stale. Phase 5K updates that *regex itself* to alpha5k / code 27
and strengthens the older Phase 5I plus current Phase 5K source gates to check
the exact status comparison, not only the version variable.

## What still needs a Windows / Android run

1. Download/extract a fresh full ZIP for Phase 5K. Run `build-windows.cmd` and require **SUCCESS**, `2.0.0-alpha5k`, code **27**, signer digest and SHA-256 in `dist/NightWave-build-status.txt`.
2. If compilation fails, send `dist/NightWave-build-errors.txt`, plus relevant `dist/NightWave-build-full.log` lines. A zero-length/missing APK is not a valid build.
3. If build succeeds, connect exactly one authorized Android phone and run `check-device-update.cmd`. This is a **read-only** APK signing/version check against the **installed** NightWave; it does not install.
4. Do not uninstall or clear app data to bypass a signature mismatch. Do not overwrite offline files or old playlists.
5. On a **disposable emulator**, execute `connectedDebugAndroidTest` to run the read-only crash/exit tests, and the existing classic/premium launch tests.
6. On the S24 Ultra, open NightWave 2.0 → **You** → **View / share crash summary**. If the previous crash came from the same package, check whether the OS lists `Java/Kotlin crash`, `Native crash`, `App not responding`, `System low-memory termination` or something else. Share the app's previewed summary if comfortable.
7. Verify Play/Pause and Seek under screen-lock, headset disconnect, app background/foreground, and quick Next/Previous transitions. Keep existing offline queue and Resume available. Check Bluetooth/notification controls.
8. If the summary is empty or only reports system termination while crashes persist, collect a **redacted** NightWave-specific logcat `FATAL EXCEPTION` or ANR detail; the raw full-device log may contain private information.

## Verification limitation

- New code and connected GitHub source gates were reviewed. There is no Android SDK or Gradle distribution in this authoring environment, and the distribution host cannot be reached due DNS, so no APK was assembled or installed.
- The prior pure Kotlin smoke harnesses remain runnable locally, but they do not validate the new Android OS APIs or Media3 race behavior. Neither the diagnostic Android instrumentation nor device runtime actions have yet been executed.
- **Do not merge this draft as a release candidate** until the Windows build, in-place signing check, emulator and real-phone playback/crash acceptance tests succeed. Stable `main` remains unchanged.
