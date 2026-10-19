# NightWave — Final Source Audit Record

**Date:** 19 October 2026  
**Branch:** `fix/nightwave-v2-phase6x8c4-daily-mix-unit-test-compile`  
**Type:** Static source review, targeted logic corrections and regression-test additions. **Not** a certified app execution or full Gradle build.

## Scope and audit approach

Reviewed the latest user-supplied Windows build diagnostics and directly inspected the connected GitHub sources, tests, build scripts, application identity and documentation. Two batches of focused static screenings covered **30 implementation files / 11,521 source lines** across playback/media session, offline import and queue, public search, Compose screens, private library, lyric handling, Daily Mix, Google Drive, vault transfer and storage/database policies.

Screening included duplicate Kotlin import statements, `TODO()` and `NotImplementedError` placeholders, global coroutine scopes, embedded Google API-key-looking values, high concentrations of `!!` and blocking-sleep candidates. In those 30 files, no duplicate imports, placeholder `TODO()` operations, global coroutine scopes or embedded Google API keys were detected by these checks. Two Drive retry sleeps were identified and their caller was reviewed: the upload/download action is launched through `worker.execute`, not directly on the UI thread. This is a bounded source review, **not** a complete security proof or line-by-line execution of every class.

The queued playback regression tests and related existing mode/gapless/display-order tests were read separately to check expectations against the new logic. The external NewPipe v0.26.5 API documentation confirms that `VideoStream.isVideoOnly()` is the supported alternative to the deprecated field.

## Issues addressed in the audited branch

| Item | Failure mode and correction | Verification level |
| --- | --- | --- |
| Non-shuffled offline Previous after jump | Previous could return to an earlier visited track rather than the visible previous song; linear navigation now follows the ordered list and shuffle retains listening history | Source patched; regression tests added |
| Sequential non-shuffle history | Normal Next added stale historical indices; history now reserved for shuffled navigation | Source patched; regression tests added |
| Shuffle one-song Repeat All | Queue could signal end even when the sole song should repeat | Source patched; regression test added |
| URL deduplication in public queue | Removal of duplicate earlier entries could shift the intended selected track | Selection now follows requested URL identity; regression tests added |
| Transfer journal warning | Nullable `File.parentFile` used in app-private path check | Explicit null rejection plus canonical files/cache parent validation |
| Deprecated NewPipe API | Field `isVideoOnly` reported deprecated | Updated to documented `isVideoOnly()` method |
| Media3 annotation | `@OptIn` had no effect for `UnstableApi` | Removed ineffective Kotlin opt-in; reconciled source gate |
| Compose icons | Deprecated ArrowBack/QueueMusic variants | Auto-mirrored imports/usages adopted |
| Repository documentation | README and source status lagged the latest build failure | Concise README, archived phase history, corrected About, changelog, build guide and verification/acceptance documents |
| Verification wiring | Current build-recovery branch omitted from push checks | Added current branch to Android CI trigger |

## Automated evidence versus implementation

The prior complete Windows log **passed** Android app Kotlin compilation, instrumentation-test Kotlin compilation and debug APK assembly, but **failed** `:app:testDebugUnitTest`: **771 executed, 1 failed**. The exact test was `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection`. Subsequent source corrections introduce nine new JVM test methods, for approximately **780 expected tests**, but these added tests have **not been executed as part of a verified full Android build**.

No source search or code review can establish that all Google APIs, device drivers, Bluetooth routes, Media3 lifecycle paths, external search providers, pixels/layouts or data migrations work on the user's device. Those must be covered by a fresh build and actual Android acceptance.

## Remaining blockers before download/install certification

1. A fresh Windows Gradle run must report `BUILD SUCCESSFUL` across unit tests, assembly and Android instrumentation-source compilation.
2. Verify APK identity, SHA-256 and signing certificate compatibility with the currently installed NightWave.
3. Execute all P0 and relevant P1 steps from [Final Acceptance Checklist](NIGHTWAVE_FINAL_ACCEPTANCE_20261019.md), including cloud failure recovery and no user-data loss.
4. Record unresolved device-specific limitations honestly; do not mark Android Auto or live external streaming certified without exercising it.
5. Review GPL/NewPipe redistribution obligations before distribution to others.

**Disposition:** Source corrections are committed and repository instructions are consistent. **Release status remains unverified pending a passing build and on-device tests.**

Related documents: [README](../README.md), [Verification Matrix](VERIFICATION_MATRIX.md), [Build & Release](BUILD_AND_RELEASE.md), [Changelog](../CHANGELOG.md).
