# NightWave Alpha4 startup recovery
**Status: source hotfix committed; Android Gradle build and on-device behavior UNVERIFIED.**

## Symptom
User installed Alpha4 and reports app does not open / seems to crash. Without an Android `FATAL EXCEPTION` trace, the exact runtime cause is unknown. Do not attribute it to Room, Compose, Media3 or stored preferences as fact.

## Recovery changes
- Revert only the startup entry point: `.ui.MainActivity` (previously usable classic player) is again the single Android MAIN/LAUNCHER activity.
- Keep `.v2.ui.NightWaveStudioActivity` installed but not exported; the legacy Home action **Explore the new premium interface** opens it explicitly for QA.
- Media3 notification's app PendingIntent opens classic player rather than the unverified preview.
- Application ID stays `com.nightwave.player`, preferences and SQLite data are untouched. No destructive migrations, no app reset, no uninstall.
- New `capture-nightwave-crash.cmd` saves the phone's Android crash buffer as `NightWave-crash.log` after the user deliberately reproduces the premium crash. It does not delete personal app data.
- Version `2.0.0-alpha4-hotfix1` code 15 to support an in-place install over code 14.

## Local Windows procedure
1. Download ZIP from `fix/alpha4-launcher-recovery`, extract to a fresh folder.
2. Run `build-windows.cmd`. If it fails, upload `dist/NightWave-build-errors.txt`. Do not uninstall or clear NightWave data.
3. Only after BUILD SUCCESS, install `dist/NightWave-debug.apk` over the existing installed debug app. Android will reject incompatible signing certificates; do not uninstall to bypass that.
4. Launch NightWave normally. The classic interface should appear. Test old playlists, play/pause, search and background playback.
5. Connect phone with USB debugging enabled; run `capture-nightwave-crash.cmd`, open classic Home → Explore the new premium interface. If that screen closes, return to PC, press a key and inspect `NightWave-crash.log`.
6. Send the Android `FATAL EXCEPTION` and `Caused by` lines, including relevant stack frames. Review for private data before sending.

## What this does NOT establish
- It does not fix the premium UI's underlying exception.
- It does not prove the native UI works after fixing the default launcher.
- No Android build, automated tests or device tests were run in this execution environment.
- The previous v0.8 `main` branch remains unchanged.

## Next action
Use the real Android crash trace to implement a targeted Compose/runtime fix and verify through a successful local APK build followed by actual phone startup QA. Do not promote the Compose activity to launcher again until confirmed stable.
