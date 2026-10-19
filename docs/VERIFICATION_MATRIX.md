# NightWave Verification Matrix

**Status date:** 2026-10-19. **Scope:** integrated development branch `fix/nightwave-v2-phase6x8c5-offline-previous-queue-order`.

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

| 6X8C3 actual app Kotlin compilation | User full Windows log: `:app:compileDebugKotlin` completed | **PASS on 6X8C3** |
| 6X8C3 Android instrumentation source compilation | User full Windows log: `:app:compileDebugAndroidTestKotlin` completed | **PASS on 6X8C3** |
| 6X8C3 JVM test compilation | Two nested-list arguments in Daily Mix tests | **FAILED on 6X8C3** |
| 6X8C4 corrected Daily Mix input types | Flat `List<VideoPreview>`, all 11 tests preserved | COMMITTED, needs fresh Gradle |
| 6X8C4 Windows + 6X6 CI regression guards | Reject both old nested-list call sites | COMMITTED, source-checked |
| 6X8C4 app Kotlin, Android test-source and JVM test-source compilation | All completed in actual user Windows build | **PASS on 6X8C4** |
| 6X8C4 debug assembly | `:app:assembleDebug` completed; full build blocked by JUnit failure | **PASS assembly, NOT release** |
| 6X8C4 JVM tests | **771 executed, 770 passed, 1 failed**: OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection | **FAIL on 6X8C4** |
| 6X8C5 offline Previous correction | History limited to Shuffle; sequential Previous follows queue order | **COMMITTED, not Android-built** |
| 6X8C5 isolated queue test methods | Kotlin/JVM with minimal JUnit-compatible assertions, **18/18 passed** | **PASS, isolated only** |
| 6X8C5 extra navigation assertions | Kotlin/JVM pure queue, **62/62 passed** | **PASS, isolated only** |
| 6X8C5 Android Gradle, signing, phone tests | A fresh 6X8C5 Windows run is required | **PENDING** |

## Release blockers

A fresh **successful** Windows Android build and signature check are mandatory. Android Auto integration is development code until the on-device media-browser playback/queue handoff is tested. Existing public-video stream availability depends on external sources and cannot be guaranteed.

Never uninstall NightWave or clear the user's app data to resolve a mismatch. Avoid publishing a release APK until both the build and device gates are satisfied.

For test steps and logs, use [BUILD_AND_RELEASE.md](BUILD_AND_RELEASE.md).
