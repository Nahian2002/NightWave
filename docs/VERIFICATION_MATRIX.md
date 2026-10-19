# NightWave Verification Matrix

**Status date:** 2026-10-19. **Development branch:** `fix/nightwave-v2-phase6x8c5-offline-previous-queue-order`. **Not a certified APK release.**

This table separates Android Gradle tasks the user actually completed, source-level changes, isolated Kotlin runs, and on-device checks. A source check is not a build, and assembling an APK does not imply the required unit tests passed.

| Stage | Actual evidence | Result |
| --- | --- | --- |
| 6X8C1 duplicated import | Fixed after Windows Kotlin preflight rejected repeated `UnstableApi` import | RESOLVED; later app Kotlin compilations passed |
| 6X8C2 Phase 6P old single-player assertion | Updated for one main player and one bounded local crossfade deck | RESOLVED; later Windows checks advanced to Gradle |
| 6X8C2 Android app compilation | Media3 connection-builder arity and nonexistent `VideoPreview.duration` | FAILED on 6X8C2 |
| 6X8C3 Android app Kotlin compilation | User's Gradle log completed `:app:compileDebugKotlin` | PASS on 6X8C3 |
| 6X8C3 Android instrumentation-source compilation | Completed `:app:compileDebugAndroidTestKotlin` | PASS on 6X8C3 |
| 6X8C3 JVM test compilation | Two nested-list arguments in Daily Mix test source | FAILED on 6X8C3 |
| 6X8C4 corrected Daily Mix test types | 6X8C4 Windows Gradle successfully compiled JVM unit-test Kotlin sources | PASS on 6X8C4 |
| 6X8C4 main Android Kotlin compilation | `:app:compileDebugKotlin` completed | PASS on 6X8C4 |
| 6X8C4 Android instrumentation-source compilation | `:app:compileDebugAndroidTestKotlin` completed | PASS on 6X8C4 |
| 6X8C4 debug APK assembly | `:app:assembleDebug` completed but overall build failed unit tests | PASS assembly, **NOT RELEASE** |
| 6X8C4 full JVM unit-test execution | 771 tests: **770 passed, 1 failed**, `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection` | FAILED on 6X8C4 |
| 6X8C5 production fix | History only influences Shuffle; normal Previous follows visible queue order after jump | COMMITTED, not yet Android-built |
| 6X8C5 original and new queue tests | Isolated Kotlin 1.9 run of copied JUnit-source methods using minimal fixture/shim: **18/18 passed** | PASS, isolated only |
| 6X8C5 extra navigation smoke test | Isolated Kotlin 1.9, 62 assertions in 15 scenarios: **62/62 passed** | PASS, isolated only |
| 6X8C5 new CI/source gate | `tools/verify_v2_phase6x8c5.py` committed and added to GitHub Actions | COMMITTED; not equivalent to Gradle |
| 6X8C5 `:app:testDebugUnitTest` and full Android build | Requires fresh 6X8C5 Windows Gradle execution | **PENDING** |
| APK signing compatibility with installed NightWave | Requires local APK and installed application certificate comparison | **PENDING** |
| Playback/lyrics/download/backup/widget/headphone device tests | Requires physical Android device or emulator | **PENDING** |
| Android Auto browsing/search/playback | Requires DHU or AAOS emulator | **PENDING** |

## Release blockers

A new **BUILD SUCCESSFUL** on 6X8C5 is required. The previously assembled 6X8C4 APK does not count as a verified new release because its unit-test task failed. Real-phone and Android Auto QA must follow before declaring those features release-ready.

Do not uninstall NightWave, clear app data or replace the installed APK with a different signer. Use [build instructions](BUILD_AND_RELEASE.md), [phase QA evidence](NIGHTWAVE_PHASE6X8C5_OFFLINE_PREVIOUS_QA.md), and [changelog](../CHANGELOG.md).
