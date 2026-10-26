# NW105 — Real Offline JUnit Execution and Remaining Compiler Gate

Date: 27 October 2026. Parent: `6656510f0d7e398e5341e4a75c1ba504f9ddee65`. Development branch: `feature/nightwave-nw105-offline-junit-execution`.

## Actual tests executed

Exact GitHub-blob-matched production Kotlin sources from NW104:
- `OfflinePlaybackQueue.kt` (blob `299a4c5bcc35254944395417c5b752e1bbca22fd`)
- `OfflineGaplessPolicy.kt` (blob `235055d7badf9b960c467c81bd8bc74631fb5382`)

Exact GitHub-blob-matched, original JUnit files:
- `OfflineManualNextAvailabilityTest.kt` (6 `@Test` methods; blob `6ea1cf54adce729a750bc1ab8ca633866db552cf`)
- `OfflineShuffleCycleIntegrityTest.kt` (8 `@Test` methods; blob `45777753d970f770dd0365c8c0ceeee4e2bb09f8`)

Both files compiled on a local standalone Kotlin/JDK environment and **14/14 real existing JUnit test methods executed and passed** using a temporary JUnit API shim. `NW105OfflineRepeatAdapter.kt` stands in ONLY for the public `PlaybackQueue.Repeat` enum dependency of the offline engine. It is located outside `app/src` and **does not test the public queue implementation**.

Prior NW104: 33/33 real JUnit methods executed in five other test classes. **Cumulative independent evidence: 47 of 53 methods**, not a single full-matrix 53-method run. The remaining six `PlaybackQueueManualNextAvailabilityTest` methods still require full current-source JVM execution. The existing NW102 53-test runner remains in CI and still must pass.

## New automated test gate

`tools/nw105_offline_junit_gate.py` compiles the real offline Kotlin policy and original JUnit test sources with the tiny enum adapter plus a temporary Java JUnit API bridge; runs all 14 test methods; rejects missing compiler, missing methods, failed assertions and mid-run source changes; and writes a fail-closed JSON receipt explicitly labeled `OFFLINE_JVM_ONLY_PASS` or `NOT_VERIFIED`. `tools/verify_nw105_offline_junit.py` checks the source and CI wiring. New CI step runs before Android Gradle and uploads the scoped receipt when a runner is allocated.

## Limitations / next phases

GitHub-hosted runner allocations, including the empty canary, have repeatedly failed before steps. Local Android compilation, Compose/Media3 linkage, signed APK, Google Drive restore, target API review and device certification remain NOT VERIFIED. Do not interpret the offline adapter or 47 cumulative independent JVM tests as a full-app certification. No app production source, Android package, audio files, library, auth or signing identity changes in NW105.

**Release status: NO-GO.**
