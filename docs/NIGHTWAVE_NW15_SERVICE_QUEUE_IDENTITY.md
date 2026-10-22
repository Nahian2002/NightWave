# NightWave NW-15 — Safe Public Queue Service Intent Selection

**Date:** 22 October 2026
**Base:** NW-14 `e2b47027401632eb809c495177be7deb084cb0e0`
**Branch:** `fix/nightwave-v2-nw15-queue-intent-identity`
**Release state:** **NO-GO** until a successful complete Gradle build, APK signer comparison, and combined Samsung tests.

## Source-confirmed playback bug

`PlaybackService` accepts a list of public-video URLs and a selected row number through `ACTION_PLAY_QUEUE` or `ACTION_REPLACE`. The old service removed invalid URLs **before** applying the incoming selection index:

- Original request: [invalid URL, Song A, Song B]; selected row 2 = **Song B**.
- Filtered service queue: [Song A, Song B]; selected numeric index 2 was out of bounds.
- The queue's index fallback could clamp to a different valid item. With other combinations of invalid rows and duplicates it could select an unrelated track.

The UI normally sends normalized URLs, but the playback service must not rely on every internal caller always constructing a perfect array. Queue mutations may outlive their originating UI state.

## NW-15 implementation

1. Added `PublicQueueIntentPolicy.retainedSelectionIndex()`. It takes the **original submitted URLs**, the requested index and the validated entries, checks the original selected URL's canonical YouTube watch identity, then resolves that URL's index in the retained entries.
2. `PlaybackService.ACTION_PLAY_QUEUE` and `ACTION_REPLACE` now drop invalid/malformed rows using `QueueRestorePolicy.isCanonicalPublicWatchUrl()` instead of checking only a permissive URL prefix.
3. The original selected index is converted to its matching index in the valid entries **before** deduplicated queue fast-path calculation and `queue.replace()`.
4. If a nonempty request names an invalid, missing or beyond-cap selected URL, the service **ignores the command** instead of playing the wrong song. The existing empty-list replacement behavior remains available.
5. `ACTION_ENQUEUE` also validates canonical URLs and only writes the queue if it actually accepted the item. This avoids dirtying persisted state for duplicate/full or invalid queue additions.
6. No change to playback signing, package/version, local-file access, existing library storage, cloud accounts or stream extraction URLs.

## Regression cases added (JUnit source)

Six `PublicQueueIntentPolicyTest.kt` cases check:

- An invalid row before a valid selected song must not shift identity.
- Duplicate earlier rows must preserve the intended URL identity.
- A malformed canonical-looking selected watch URL must not play another song.
- Negative/out-of-range selected positions are rejected rather than clamped.
- A selected row excluded by the 100-track cap is not substituted.
- An ordinary canonical request retains its original selection.

The `tools/verify_nw15_queue_intent.py` source contract, NW-15 CI branch and earlier NW-01 through NW-14 source/Gradle gates are all retained.

## Verification ledger

| Gate | Result | Meaning |
| --- | --- | --- |
| Source changes committed to GitHub | **Completed** | Latest integrated branch has actual playback service and unit-test code |
| Targeted source consistency audit | Pending final source review | Inspects wiring, signatures and guard paths, not test execution |
| New six JUnit tests | **Authored, not executed** | No fresh Gradle pass available |
| Kotlin compiler in current runtime | **Unusable** | `kotlinc -version` timed out and a direct compiler invocation also stalled while compiling Hello World |
| GitHub Actions Android verification | **Blocked outside build steps** | Earlier branch runs failed with no executed job steps |
| Windows Gradle + signer-safe APK | **Unverified** | No new complete NW-15 `BUILD SUCCESSFUL` |
| Samsung UI / playback / background audio | **Deferred** | Requires a signer-compatible verified candidate |

An older uploaded Windows Gradle log (19 October) compiled and assembled the previous version but failed one offline queue test out of 771. It is **not** evidence for NW-15; the branch has changed substantially since then.

## Combined acceptance after obtaining a full build

1. Extract an exact NW-15 source ZIP in a new Windows directory. Run `build-windows.cmd`; preserve full logs, failing test summary, source hash inventory and APK identity/signing checks.
2. Require `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin` to pass. Do not consider a source audit a substitute.
3. Without installing over the existing app, confirm signer compatibility using the read-only device checker; **never uninstall or clear private music data just to force an update**.
4. Exercise public playlists containing deliberately invalid rows and duplicates ahead of the selected video. The original intended song must be selected or the request rejected, never silently replaced.
5. Retest NW-13 Previous and NW-14 long-queue song neighbors, background playback, search, Google Drive authorization, Sound Studio and the Now Playing UI.

**Disposition:** Source-level integrity defect corrected; release remains **NO-GO** pending executed compiler, unit test, APK-signing and phone evidence.
