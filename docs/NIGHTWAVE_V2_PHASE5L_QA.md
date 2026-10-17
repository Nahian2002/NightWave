# NightWave 2.0 Phase 5L — Private crash evidence, without a data reset

**Version:** `2.0.0-alpha5l` / versionCode **28**  
**Branch:** `feature/nightwave-v2-phase5l-private-crash-capture`  
**Status:** Draft source implementation, Android APK build **not run here**.

## What Phase 5L fixes

**1. Read-only Windows crash evidence with minimal disclosure.** The existing
`capture-nightwave-crash.cmd` saved full-device crash and system logs, which
could contain details of unrelated apps. It also overlooked the SDK installed
by NightWave's own Windows builder. The new one-click launcher finds Platform
Tools in the user-local NightWaveBuild SDK, standard Android SDK paths and PATH.

`tools/capture-nightwave-crash.ps1` reads a bounded portion of the Android
**crash buffer** in memory and writes **only** a sanitized
`dist/NightWave-crash-summary.txt`. It selects only FATAL EXCEPTION blocks
for `com.nightwave.player` and prints at most five recent crash timestamps,
exception **class names** and up to 12 NightWave class/method frames per crash.
It excludes exception messages, raw lines, other applications, device IDs,
process IDs, URLs, full Android/system traces and user file paths. It never
clears crash buffers, stops an app or writes anything to the phone.

**2. Crash-capture self-test.** Before Gradle/Android build preparation, the
Windows builder runs the real PowerShell filter using fabricated records
containing a NightWave crash, an unrelated app crash and deliberately fake
private strings. A FAIL blocks APK publication. You can run the same test
without a device:

```bat
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\capture-nightwave-crash.ps1 -SelfTest
```

**3. Fix stale version checks permanently.** Earlier phase build scripts
updated version variables while leaving an older alpha string in the
`check-device-update.ps1` status regex. Phase 5L parses the status version and
version code, then compares them to its single `$versionName` / `$versionCode`
declarations. No separate alpha-specific status regex is required in future.
The old Phase 5I and 5K checks were updated to assert this behavior.

**4. Media3 resource recovery.** On Android session disconnection, the
premium controller bridge now releases its stale MediaController future
before allowing a new connection. No saved songs, queues or imported files
are changed. This is a targeted resource-lifecycle improvement, **not
proof that the original frequent-crash warning is solved**.

## What is verified and what is not

- Source files were read back from GitHub and checked for release/version
  alignment, privacy and forbidden Android commands. The source-only
  `tools/verify_v2_phase5l.py` was added.
- Previously isolated JVM Kotlin smoke JARs still run in this environment,
  including 14 Android exit-history test assertions and 8 player snapshot
  policy assertions. They **do not** validate the new PowerShell runtime
  nor compile the Android application.
- **Pending:** actual PowerShell script syntax and self-test execution,
  Windows Gradle/JUnit/Compose build, APK signing, Android instrumentation,
  in-place phone update and phone crash/playback reproduction.
- Existing `NightWave-crash.log` or `NightWave-system-log.log` files from
  older capture scripts, if present on your PC, are **not deleted**
  automatically. They may contain personal data and should not be uploaded
  without review.

## How to use Phase 5L on Windows

1. Download the whole Phase 5L ZIP into a **new folder**. Do not uninstall
   NightWave, reset the phone or clear saved music.
2. If NightWave is already crashing, connect exactly one authorized phone
   with USB debugging and double-click `capture-nightwave-crash.cmd`.
   This only needs ADB Platform Tools; it does **not** require rebuilding
   the APK first, but the included builder can provision Platform Tools.
3. Open **`dist/NightWave-crash-summary.txt`** and review it. This is the
   report to share, **not** a raw whole-phone log.
4. On Android 11+, you can also use NightWave's existing **You → App health →
   View / share crash summary**, which includes up to five coarse Android
   process-exit reasons (ANR, low-memory, etc.) even when logcat contains no
   FATAL EXCEPTION block.
5. To test the filter on Windows without the phone, run the `-SelfTest`
   command above. Expected: `PASS: NightWave-only crash filtering`.
6. To build: run `build-windows.cmd`; success requires both redaction
   self-test and the existing Android Gradle/JUnit/signature checks.
   `dist/NightWave-build-status.txt` must show **2.0.0-alpha5l / 28**.
7. After a successful build, run `check-device-update.cmd` with your
   current phone before attempting an **in-place** update. Inconclusive
   signing/version results mean **do not install**.
8. On an emulator/disposable profile, run Android instrumentation smoke
   tests; then manually validate offline resume, online queue, player
   seek/Play/Pause, background/lock-screen controls and music after relaunch.

### Important caveat

The system may evict old crash-buffer entries. No matching FATAL exception
does **not** mean the phone never crashed; native exceptions, ANRs, low-memory
terminations, UI rendering bugs and repeated service restarts can require a
separate, carefully reviewed Android system diagnostic. Phase 5L remains a
**draft, not a release candidate** until actual Android build/device results
are available. Stable `main` is untouched.
