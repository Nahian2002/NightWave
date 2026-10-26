# NW75 Hotfix 5 — JVM offline shuffle history regression

**Parent cumulative branch:** `fix/nightwave-nw75-backup-badge-compile`  
**Latest cumulative branch:** `fix/nightwave-nw75-offline-shuffle-junit`  
**Release status:** NO-GO until complete `build-windows.cmd`, APK signer/update compatibility and Samsung runtime QA actually pass.

## Windows evidence — 2026-10-26

The previous build succeeded through Windows source gates and **real** Gradle Kotlin/Java production compilation, `:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin`. Gradle then executed the full JVM suite:

```
1117 tests completed, 1 failed
OfflinePlaybackQueueTest > invalidShuffleJumpKeepsHistoryAndUnplayedOrder FAILED
org.junit.ComparisonFailure at OfflinePlaybackQueueTest.kt:92
expected:<[c]> but was:<[b]>
> Task :app:testDebugUnitTest FAILED
```

Build reported 58 actionable tasks (58 executed). It did not complete its release/build verification, and the builder correctly reported **NO new verified APK**. A previously preserved APK, if present, is NOT a new NW75 build.

## Root cause

The old test's expected shuffle order was incompatible with the intentional NW69 behavior of `OfflinePlaybackQueue`:

1. Select A and explicitly jump to B. The original track A is retained in actual listening history.
2. Invalid jumps (`-1`, `99`) return `null` and do **not** mutate selected track or navigation trails.
3. `previous()` retraces from B to A and remembers B in a bounded **forward redo** stack.
4. The next `next()` must replay the actual forward-visited track **B**, not select fresh unplayed **C**.
5. Only the following Next moves to C (Repeat Off). Another Next stops rather than repeating heard tracks.

The JUnit test still expected C immediately after returning to A, a pre-NW69 contract. Changing production behavior to satisfy it would regress precise shuffle back/forward navigation.

## Fix, scope and verification

- **Updated only the stale assertion scenario** in `app/src/test/java/com/nightwave/player/offline/OfflinePlaybackQueueTest.kt`: it now expects A → B redo → C, verifies invalid jumps do not destroy redo history, Repeat Off stops at the end, a subsequent Previous+Next returns to C and queue contents remain unchanged.
- Extended `tools/check-nw59-integration.ps1` to assert the corrected test's chronological expectations. This source guard is not a substitute for JUnit execution.
- No production Kotlin/Media3, download/playlist/user data, package ID, Google OAuth or signing files were changed.
- In isolated JVM validation, compiled the **exact production `OfflinePlaybackQueue.kt` Git blob** `a9e05b6ed5cec6055edef4f5b3c335fa42e3ab6c` with a minimal `PlaybackQueue.Repeat` stub and executed **13/13 targeted assertions** (invalid jumps, shuffle forward redo, new unplayed track, Repeat Off, Previous+Next and ID snapshot). The source was SHA-verified via `git hash-object`.
- The complete Android JUnit suite has **not yet been rerun** for this branch; the previous attempt still has 1 failure. A new Windows build may reveal further issues.

## Safe next Windows attempt

Download the new branch as a complete ZIP, extract to a **fresh** Windows folder and run `build-windows.cmd`. This reruns all source gates plus the same 1,117+ JVM tests, assembles/debug-signs the APK, compiles Android instrumentation sources and verifies the result.

If it fails, send the newly generated `dist/NightWave-build-diagnostic.txt`, `dist/NightWave-build-status.txt`, `dist/NightWave-build-errors.txt` and `dist/NightWave-build-full.log`. If the full build succeeds, run `check-google-oauth.cmd` and `check-device-update.cmd` before any install. **Do not uninstall the existing app or clear offline music.** Public release remains NO-GO without real device and rights/auth checks.
