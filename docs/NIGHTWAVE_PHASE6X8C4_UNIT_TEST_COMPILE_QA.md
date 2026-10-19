# NightWave 6X8C4 — Daily Mix unit-test compilation recovery

## Proven failure (2026-10-19 Windows build of 6X8C3)

The user's complete Windows Gradle build log shows:
- `:app:compileDebugKotlin` **passed** (real main app Kotlin compilation).
- `:app:compileDebugAndroidTestKotlin` **passed** (real Android instrumentation source compilation).
- `:app:packageDebug` completed, but the overall build **FAILED** (there is no successful, verified current APK).
- `:app:compileDebugUnitTestKotlin` failed with exactly two argument type errors in `LocalDailyMixEngineTest.kt`, at previous lines 102 and 114.

The engine's third parameter is `playlistTracks: List<VideoPreview>`. The two test invocations accidentally passed `List<List<VideoPreview>>` and `List<MutableList<VideoPreview>>`.

## Minimal fix

- In the invalid-link/duplicate-input test, replace `listOf(valid, valid)` with `valid + valid`, intentionally keeping the repeated source tracks while providing a flat `List<VideoPreview>`.
- In the source-immutability test, pass `favorites` directly rather than `listOf(favorites)`.
- No production playback, audio, database, network/streaming, backup, signing, application identity or data-migration changes.
- Preserve all 11 Daily Mix JUnit test methods.
- `tools/check-kotlin-compat.ps1` and `tools/verify_v2_phase6x6.py` now reject the two previously broken call patterns and assert the corrected contract.

## Evidence and outstanding gates

- User-provided 6X8C3 log verifies actual main Android Kotlin compilation and instrumentation-test source compilation **passed**.
- 6X8C4 source review verifies both corrected call sites match `List<VideoPreview>` and all 11 JUnit test methods remain.
- A **local isolated Kotlin 1.9 compiler run** with a minimal `VideoPreview` and `make(..., playlistTracks: List<VideoPreview>, ...)` contract compiled and executed both corrected call expressions successfully. This was a type-contract smoke test, **not** compilation of the Android project nor execution of the 11 JUnit tests.
- Full 6X8C4 `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` **NOT YET EXECUTED**. The change is a compiler hotfix candidate, not a bug-proof or release-ready APK.
- `LockerTransferHistoryStore.kt:29` reported a nullable-`java.io.File` **warning**, not one of the two compilation errors. Its security-sensitive app-private canonical-path validation was left unchanged rather than risking unrelated storage behavior. Media3 and deprecated icon warnings likewise did not fail the build.

## Required safe validation

Download the entire 6X8C4 branch archive and extract into a fresh Windows directory. Run `build-windows.cmd`; wait for a complete **BUILD SUCCESSFUL** plus APK identity/signature verification. The output builder preserves previously successful APKs separately; do not confuse the previous APK with the new build. If failure persists, use `send-build-report.cmd` or upload all files in `dist/NightWave-build-*.{txt,log}`.

**Never uninstall the installed NightWave app, clear its app data or install an APK with a mismatched certificate.** Full phone playback/lyrics/Drive/Android Auto/BT/widget testing remains pending even if Gradle succeeds.
