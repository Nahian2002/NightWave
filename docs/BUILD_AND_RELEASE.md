# NightWave — Windows Build, Verification and Safe Upgrade

**Maintainer guide updated 19 October 2026.** This is a zero-billing local workflow. The source ZIP is not an installable APK. Do not claim release readiness based on source preflight or a previously built APK.

## 1. Get the latest source

Use the complete [6X8C4 development branch](https://github.com/Nahian2002/NightWave/tree/fix/nightwave-v2-phase6x8c4-daily-mix-unit-test-compile) rather than stacking individual patch ZIPs. Extract to a *new*, short Windows directory. Do not overwrite the currently installed NightWave app or delete its Android data.

## 2. Build on Windows

Double-click `build-windows.cmd` in the extracted project root. The builder checks PowerShell syntax and Kotlin/source guards; detects or downloads upstream JDK/Gradle/Android tools as needed; then runs the Android Gradle tasks:

```text
:app:testDebugUnitTest
:app:assembleDebug
:app:compileDebugAndroidTestKotlin
```

Wait for `BUILD SUCCESSFUL` and a successful local APK identity/signing report. Passing 251 Kotlin *preflight* files is not Kotlin compilation. The app should be verified against the existing installed APK signature; Android may refuse an update if signing identities differ.

**The latest user-provided complete build evidence is the 6X8C3 run:** main Android Kotlin and Android-test Kotlin compiled; the build failed at `:app:compileDebugUnitTestKotlin` from two nested-list arguments in `LocalDailyMixEngineTest.kt`. Phase 6X8C4 fixes those test calls; a complete successful build is still mandatory.

## 3. If it fails

Don't assume the failure is the same as last time, and don't retry with an old ZIP. Submit the exact new reports:

- `dist/NightWave-build-errors.txt` — first compiler errors and failure summary
- `dist/NightWave-build-diagnostic.txt` — compact privacy-conscious diagnostic
- `dist/NightWave-build-status.txt` — authoritative success/failure status
- `dist/NightWave-build-full.log` — full compiler/test output

Alternatively run `send-build-report.cmd` for the compact report. Redact credentials and signed public-stream URL tokens if sharing raw logs.

If Kotlin compilation passes but a **unit test** fails, address that test's original failure rather than disabling it. If an **Android test source** fails to compile, correct the instrumentation source and rerun all three Gradle tasks.

## 4. Never risk the installed library

- **Never uninstall NightWave or clear app data** to bypass APK signing errors. App-private imported audio, LRC, playlists and saved settings may be lost.
- Do not install a GitHub Actions APK over the user's installed NightWave unless the signing identities have been independently compared and confirmed compatible.
- The current source keeps application ID `com.nightwave.player`, version code `35` and version name `2.0.0-alpha5s`. Retaining identifiers alone does **not** guarantee signing compatibility.
- Do not declare victory because `dist/NightWave-debug-previous.apk` exists: that is a preserved earlier file, not a newly verified build.

## 5. Device acceptance after a real build

Use a test library when possible. Verify offline import and persistence, public saved-reference playback and recoverable URL failures, repeat/shuffle and history, gapless/crossfade, normalization/Listening Level, Sleep Timer, EQ and audio-focus/headphone disconnect. Exercise synced lyrics/50ms editor, playlist folders, daily mixes, widget Play/Pause/Next/Previous, background/notification/Bluetooth controls and Google Drive preview/recovery. For Android Auto use DHU/AAOS emulator while stationary, verify browse/search/selection and confirm browsing never auto-plays.

Only after build, signing and the relevant device tests pass should a release branch/installer be marked verified.

## 6. Verification sources

See [VERIFICATION_MATRIX.md](VERIFICATION_MATRIX.md), [CHANGELOG.md](../CHANGELOG.md), [About](../ABOUT.md), and [6X8C3 source regression gate](../tools/verify_v2_phase6x8c3.py).
