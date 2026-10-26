# NW97 — Offline Shuffle-Cycle & Gapless Transition Integrity

Date: 26 October 2026. Parent: `1d4040b470922923a7f2ec9e0a24b8276242bc87`. Cumulative source: `feature/nightwave-nw97-offline-shuffle-cycle-integrity`.

## Confirmed reproducible regression

The fallback `OfflinePlaybackQueue.previous()` added the departed (already heard) song back into `unplayed`. After a user branched with `jump()`, the forward trail was correctly cleared but the newly re-added heard song stayed in the random candidate pool. **Repeat Off could then replay a previously heard song before the shuffle cycle completed.**

This was exercised directly using production-equivalent queue source with a deterministic random picker:
`a → Next(b) → Previous(a) → Jump(d) → Next(c) → Next(e) → Next`.
Before this fix the last Next wrongly returned `b`. The pre-fix scenario exited nonzero as expected. After this fix, Next returns null and the new smoke suite passes.

## Implemented corrections

1. `OfflinePlaybackQueue.previous()` preserves bounded `forwardVisits` and removes the selected prior song from the unplayed pool, but does **not** add the departed previously heard song back to the pool. Forward retrace still uses its actual history until an explicit new Jump branch.
2. `OfflineGaplessPolicy.transitionIndex()` requires an already valid selected slot and a completely valid prepared ID list for **both automatic and seek transitions**, rather than only checking current index during automatic transitions. Stale detached seek callbacks, duplicate IDs and unchecked private paths cannot be accepted by the policy. Existing Media3 verified shuffled target, adjacent linear Next and Repeat All wrapping remain compatible.
3. Added eight Android JUnit test cases for repeat/shuffle/forward/gapless edge cases and an exact-source Kotlin CLI smoke suite.
4. Updated CI branch triggers, smoke suite, source checker and milestone docs; changes remain cumulative from NW96.

## Evidence and status

- **33/33 deterministic Kotlin/JVM smoke assertions PASS** locally with the actual patched `OfflinePlaybackQueue.kt` and `OfflineGaplessPolicy.kt` and a type-compatible `PlaybackQueue.Repeat` stub. GitHub CI additionally compiles the complete actual public PlaybackQueue source with its ShuffleRecoveryPolicy when a runner is available.
- Negative regression proof: restoring the original `unplayed.add(index)` line makes the suite FAIL specifically at the Repeat Off replay guard, demonstrating that the test detects the bug.
- Eight JUnit regression tests added but **not yet executed** under Android Gradle.
- A full Android debug build, media-session/device gapless timing, Bluetooth behavior, signing/installed-upgrade, and NW90 139-case manual acceptance are **NOT VERIFIED**. GitHub hosted runners have historically failed before their first step; a failed pre-step job does not indicate successful or failed compilation.

## Safety / compatibility

No changes to private downloaded songs, library indices, public IDs, queue preference schemas, Drive auth, signing certificate, Android app package, AndroidManifest, server architecture, notifications, Media3 audio focus, equalizer or UI. The fallback queue's shuffle history is session-local; no destructive state migration occurs.

**Release certification: NO-GO** until current Android build/JUnit and genuine device acceptance pass.
