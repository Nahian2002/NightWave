# NightWave NW-13 — Public Queue Navigation Correction

**Date:** 22 October 2026
**Base:** NW-12 `e9b43e13c302e48599eaffe41ceaa4b1a6b34bd2`
**Branch:** `fix/nightwave-v2-nw13-public-queue-previous`
**Release status:** **NO-GO** until full Gradle, signer continuity and Samsung acceptance pass.

## Concrete bug fixed

The public `PlaybackQueue` used a `visited` history stack when playing linearly
with Shuffle **off**, even though the offline queue had already switched to
display-order Previous. This causes observable inconsistent navigation:

1. Select public track 0 from tracks 0,1,2,3,4.
2. Tap track 4 directly, which pushes track 0 onto the history stack.
3. Press Previous.
4. **Old behavior:** jumps to track 0. **Expected:** track 3 (one row back).

This is a source-confirmed behavior, independent of network search or app artwork.

## Implementation

- `PlaybackQueue.previous()` now follows the displayed queue in linear mode,
  including after arbitrary taps/jumps and after removing an upcoming song.
- When Shuffle is on, Previous follows actual shuffled listening history; with
  no prior shuffled track, it declines to invent a linear predecessor.
- `next()` and `jump()` only add history when Shuffle is on.
- Changing Shuffle mode clears obsolete history and shuffled-seen records so
  a new Shuffle session cannot revive tracks from an old listening sequence.
- Existing Repeat All wrap behavior is preserved for linear Previous.
- `useExplicitDisplayOrder()` still clears shuffle state and selection history.
- No change to saved music storage, package ID, Android manifest, signing or
  Google authorization. No library delete/migration.

## Regression tests authored

- Nine new JUnit methods in `PlaybackQueueNavigationModeTest.kt`:
  - linear Previous after a jump;
  - invalid jump must not change selection/history;
  - repeated linear Next/Previous in displayed order;
  - shuffled Previous retraces actual selection order;
  - switching modes discards previous shuffled history;
  - Repeat All wrap;
  - deduplicated selected URL then linear Previous;
  - empty queue handling;
  - shuffled Previous with no history must not invent a song.
- Updated `PlaybackQueueTest.jumpHandlesBoundsAndPreservesPrevious` to
  assert visible-order previous instead of the old history-based behavior.
- Updated `PlaybackQueueUpcomingTest` to confirm removal/undo retains correct
  linear Next/Previous after jumps.
- Added `tools/verify_nw13_queue_navigation.py` to existing GitHub Actions
  source preflight. All prior NW01-NW12 source/verifier and Gradle gates remain.

## Verified evidence and limitations

- The changed public queue source and the updated existing tests were read
  directly from GitHub. Static/source contracts were reviewed.
- Four previously selected queue-source files were reconstructed in the local
  workspace and their **Git blob hashes matched GitHub**:
  `OfflinePlaybackQueue.kt`, `PlaybackQueue.kt`,
  `PublicQueueSelectionPolicy.kt` and `RepeatModePolicy.kt`.
- An attempt to start `kotlinc` for an expanded queue test did **not complete**:
  even `kotlinc -version` stalled in the current container. Do **not** treat
  this as a Kotlin failure or a passing test.
- No Android SDK or usable authenticated Git clone is available in this
  runtime, so a complete new Gradle build was not possible here.
- The earlier successful NW12 compilation of **six unaffected policy sources**
  remains valid evidence for those exact blobs only. It does **not** verify
  the newly modified `PlaybackQueue.kt`.
- GitHub Actions currently fails before executing steps. Its overall failure
  status does not establish the cause or outcome of source compilation.

**Production decision: NO-GO.** The queue regression is implemented and
regression test source exists, but Kotlin/Gradle execution of this change,
Android runtime behavior, APK signing and data-preserving update compatibility
remain unverified.

## Deferred acceptance

On a clean Windows checkout of this exact NW-13 source, use
`build-windows.cmd` and require `:app:testDebugUnitTest`,
`:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, a fresh
`BUILD SUCCESSFUL` and valid APK/source SHA records.

Before installation, run the existing read-only signer/device compatibility
checks. Never uninstall NightWave or clear its library to force an update.

Once build-safe, verify public-video queue A→E tap, Previous → D, Repeat All
wrap, Shuffle actual history, Shuffle toggle restart, queue remove/undo and
offline queue navigation on the Samsung phone.
