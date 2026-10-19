# NightWave Phase 6X8C5 — Offline Previous / Jump Navigation Regression

## User-observed Gradle result (19 October 2026)

The user's complete Phase 6X8C4 Windows log confirms:
- **PASS** `:app:compileDebugKotlin`
- **PASS** `:app:compileDebugAndroidTestKotlin`
- **PASS** `:app:compileDebugUnitTestKotlin`
- **PASS** `:app:assembleDebug` (debug APK was assembled, but the overall build **FAILED**)
- **FAIL** `:app:testDebugUnitTest`: 771 tests executed, **770 passed, one failed**, `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection` at line 43 (a JUnit ComparisonFailure)

The nullable-file message in `LockerTransferHistoryStore.kt:29` is a **warning**, not the reason the build failed. The above is evidence for 6X8C4, not proof that 6X8C5 has completed Gradle.

## Root cause

`OfflinePlaybackQueue.previous()` used its `visited` history stack **before** checking whether Shuffle was enabled. A normal non-shuffled queue `a,b,c` with `select(a)`, then `jump(2)`, then `previous()` therefore returned the *jump origin* a rather than the preceding visible track **b**. The established JUnit test expects the visible queue order.

Correct distinction:
- **Shuffle Off** — Next/Previous follow visible queue positions; Repeat All wraps at bounds; invalid jump keeps current index.
- **Shuffle On** — Previous follows the actual visit/jump history; non-repeating unplayed song selections remain guarded.

The production patch therefore confines history pushes and pops to Shuffle mode. No queued IDs are mutated, no track file accesses occur, no data format or persisted settings change.

## Code changes
- `app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt`: push history only when shuffle is enabled in `next()`/`jump()`; pop `visited` in `previous()` only when shuffle is enabled. Normal Previous follows `index - 1` (or Repeat All wrap).
- `OfflinePlaybackQueueTest.kt`: original six tests retained, including the failing test, plus four new regression tests for manual jump, stale history, invalid jumps under Shuffle, and Repeat All wrap. `OfflinePlaybackQueueModesTest.kt` retained unchanged with eight shuffled/repeat tests.
- `tools/verify_v2_phase6x8c5.py`: source contract guard for both modes, preserving previous source-gate and Gradle tasks. Added push trigger for 6X8C5 in `.github/workflows/android.yml`.

## Tests **actually executed** here
1. Using Kotlin/JVM 1.9 and a minimal `PlaybackQueue.Repeat` type fixture, compiled a faithful local copy of the GitHub `OfflinePlaybackQueue.kt` production implementation and exercised **62 assertions in 15 scenarios — all passed**.
2. Compiled copied GitHub Kotlin source for both `OfflinePlaybackQueueTest.kt` and `OfflinePlaybackQueueModesTest.kt` with a small JUnit-compatible assertion/annotation shim, then invoked their methods. **18/18 Kotlin test methods passed**. This includes the original failing `jumpHandlesBoundsWithoutDamagingSelection`.
3. Checked source files on GitHub for both original and new method names, queue history/Repeat logic and workflow wiring.

These are **isolated Kotlin executions**, not real `./gradlew :app:testDebugUnitTest` results. This runtime has Kotlin/JVM but no usable Android SDK or full repo checkout. A complete Android Gradle build **remains unverified for 6X8C5**.

## Mandatory follow-up

Extract a new **complete** 6X8C5 source ZIP to a fresh Windows folder. Run `build-windows.cmd`. Require full `BUILD SUCCESSFUL`, successful unit-test execution, a newly verified APK, and a matching installed signer before installing over the current NightWave. **Never uninstall NightWave or clear music/app data.**

After successful build, verify imported queue jump and previous from 3+ songs with and without Shuffle, Repeat Off/All/One, lock-screen and headset Previous, 6X1 gapless transition and 6X2 crossfade. Test longer queues and ensure the library, LRC and Drive backups remain unchanged.

If there is another failure, share **fresh** `dist/NightWave-build-full.log`, `dist/NightWave-build-errors.txt`, `dist/NightWave-build-diagnostic.txt` and `dist/NightWave-build-status.txt`. Avoid reusing diagnostic logs from old ZIPs.
