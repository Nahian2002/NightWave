# NightWave Verification Matrix

**Status date:** 2026-10-19. **Scope:** integrated development branch `fix/nightwave-v2-phase6x8c3-compiler-and-repo-audit`.

This matrix distinguishes **observed completed tests** from **checks included in source but not executed**. No count of source assertions can prove an entire Android project is bug-free.

| Gate | Evidence / state | Status |
| --- | --- | --- |
| 6X8C2 Windows Kotlin import/source preflight | User screenshot: 251 files passed | PASS on **earlier branch** |
| 6X8C2 Phase 6P and later Windows source preflight | Advanced to full Gradle compilation | PASS on **earlier branch** |
| 6X8C2 Android Kotlin compilation | Actual build log: Media3 builder arity and nonexistent `VideoPreview.duration` | FAILED |
| 6X8C3 Media3 builder source correction | Exact constructor replaced with supported one-argument form | COMMITTED, not compiled |
| 6X8C3 Smart Library source correction | Uses model's declared `durationSeconds` | COMMITTED, not compiled |
| Isolated SmartLibrary Kotlin policy compile and behavior checks | 13/13 executable checks passed using a locally reproduced policy source plus minimal collaborators (not Gradle) | PASS, isolated only |
| Regression tests for 239/240/241 seconds and unknown durations | Added to Android JVM unit-test source | NOT YET EXECUTED by Gradle |
| Windows preflight for both prior Kotlin compiler errors | Added to `tools/check-kotlin-compat.ps1` | SOURCE-REVIEWED, not run on Windows |
| GitHub source-gate contracts | New 6X8C3 gate and historic 6X5/6X8 guards | ADDED; not equivalent to build |
| `:app:testDebugUnitTest` on 6X8C3 | Requires Gradle | PENDING |
| `:app:assembleDebug` on 6X8C3 | Requires Gradle | PENDING |
| `:app:compileDebugAndroidTestKotlin` on 6X8C3 | Requires Gradle | PENDING |
| APK signer continuity with installed NightWave | Requires locally built APK and installed signer check | PENDING |
| Device playback, imports, playlists, LRC, Sleep Timer, Bluetooth, widget, backups | Requires Android phone/emulator | PENDING |
| Android Auto browsing/search/select transport | Requires DHU/AAOS emulator and device checks | PENDING |

## Release blockers

A fresh **successful** Windows Android build and signature check are mandatory. Android Auto integration is development code until the on-device media-browser playback/queue handoff is tested. Existing public-video stream availability depends on external sources and cannot be guaranteed.

Never uninstall NightWave or clear the user's app data to resolve a mismatch. Avoid publishing a release APK until both the build and device gates are satisfied.

For test steps and logs, use [BUILD_AND_RELEASE.md](BUILD_AND_RELEASE.md).
