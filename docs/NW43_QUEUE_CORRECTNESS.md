# NW43 — Queue Correctness, Play Next and Honest Up Next

**Branch:** `feature/nightwave-nw43-queue-correctness`  
**Base:** NW42 expressive-motion source.  
**Status:** CODED, SOURCE-AUDITED; full Android Gradle compilation and real-device verification **NOT YET DONE**.  
**Testing preference:** User will build after later batches; no APK/install/data migration attempted.

## Primary defect fixed

Before NW43, each Search result-row action passed the entire returned result list (often 20 loosely related videos) to `playSearchedOfflineFirst(video, collection)`. `QueueSelectionRules.plan` then legitimately created a 20-item playback queue: a mismatch between the user's tap and the implicit result-list playback policy.

Now **all four** Search tap paths (creator-view, saved suggestions, matching saved tracks and public results) pass **only `listOf(song)`**. Playlist, personal mix and dedicated collection Play controls still send their actual collection, preserving expected sequence. Online search no longer pretends to be a manually chosen playlist. Saved/offline search prefers only the selected verified private audio item as well.

**Existing queue caveat:** This code does not erase already saved 20-result queue snapshots on upgrade. Tapping another single Search result replaces such a queue with exactly one song; this is intentional data preservation.

## Intentional queue commands

- **Play Next** is an explicit action in regular and named-playlist song options, separate from Play Now / Play Playlist and Add to Queue.
- `ACTION_PLAY_NEXT` is owned by `PlaybackService`, not by a Compose component. It validates a canonical public watch URL and refuses cross-mode queue changes during imported offline playback.
- `PlaybackQueue.insertNext` inserts a new URL directly after the playing item, or moves an existing **upcoming** item to that position without duplicating it. Previously played/current songs are not silently reordered. Full 100-item queues may reorder an existing entry, but cannot grow beyond capacity.
- Multiple Play Next choices work in latest-first order. Explicit manual next choices outrank random shuffle picks; Repeat One still repeats automatically until the user manually skips or changes Repeat.
- Add to Queue remains append-only; no source-based implicit enqueue. Unsupported URLs never enter online queue. Queue actions from the Android UI are asynchronous and must not claim success until service state is visible.
- Play Next on an empty public queue selects and begins the requested track, avoiding a phantom waiting state.

## Persistence and UI clarity

- `QueueStateStore` records only a bounded list of canonical public watch URL IDs in a backwards-compatible optional JSON `manual_next` array. It rejects priority IDs that do not occur in the restored upcoming queue. No signed stream addresses or offline file paths are saved.
- Playback bridge reconciles user-initiated queue changes promptly and exposes `manualNextUrls` for genuine on-screen labeling.
- The queue sheet now has separate **PLAYING NOW**, **PLAY NEXT** (if user-selected), **UP NEXT** (unshuffled future), **SHUFFLE POOL — ORDER VARIES** (no false order guarantee), and **EARLIER IN THIS QUEUE** (does not assert that earlier list slots were listened to).
- Swipe-to-remove and Undo preserve their previous guards: only unshuffled, public, ahead-of-current entries are removable. The current or previously indexed item is never removed by a swipe.
- Existing compact mini-player, transport, playback service, public streaming resolution and imported audio storage remain unchanged.

## Not implemented or falsely promised

- No recommendations/autoplay engine is introduced. There is **no automatic addition of "similar songs"** by NW43; any optional Autoplay recommender must be explicit, off by default, accurate and separately verified after NW44 improves search relevance.
- A source check cannot prove Android compilation, correct media stream resolution, persisted queue handling on an actual Samsung phone or shuffle behavior through real Media3. Those are device acceptance gates.
- This update does not address Google authorization error 8, download speed, universal offline saving, lyrics, EQ or other future roadmap batches.

## Regression coverage committed

- `PlaybackQueuePlayNextTest.kt`: **15 JVM test declarations** for Play Next vs Add to Queue, exact insertion, multiple priorities, de-duplication, no history rewrite, 100-item capacity, shuffle priority, Repeat One, reset, removal, shuffled mode toggles and process-memory priority reconstruction.
- `NightWaveQueueSectionsInstrumentedTest.kt`: **4 Android Compose instrumentation test declarations** for single-song queue, genuine Up Next, unranked shuffle pool and explicit priority section.
- `QueuePlayNextPersistenceInstrumentedTest.kt`: **2 Android instrumentation test declarations** for private-pref isolated persistence and poisoned `manual_next` metadata.
- `tools/verify_nw43_queue.py`: source wiring/negative regression gate. `tools/check-kotlin-compat.ps1` now guards against Search passing a whole result list into a playback queue. CI hooks enabled for the new feature branch.
- Connected GitHub source audit: **18/18 code-wiring assertions passed** and no duplicate imports in the 12 audited Kotlin/script files. **Tests have not been compiled or executed** yet.

## Exact later build and device validation

1. At the end of planned updates, run the **exact branch** `build-windows.cmd` (the script runs source gates and Gradle). Inspect `dist/NightWave-build-status.txt`, Gradle test reports and errors. Don't mistake a preserved prior APK for a new verified build.
2. Run `check-device-update.cmd` to verify package/signing before updating the user's installed app. Never uninstall or clear music to force replacement.
3. Search for `Alone 2`, tap just one result, inspect queue: **one** song only, not 20 unrelated videos. Repeat for saved matches and artist group search.
4. Tap a playlist song: collection order remains intact. Test Repeat One/All, Shuffle, Back/Next, and queue clearing.
5. From song options choose Play Next; verify immediate next song, repeated manual requests latest-first, and Add to Queue at the end. Test Shuffle ON and OFF and order after process recreation.
6. Test existing URL repositioning, unsupported URL rejection, 100-item queue, Play Next with empty queue, expired stream recovery, Play Now interrupting a prior song, and no unexpected online song injected during private offline playback.
7. Verify queue sheet displays actual currently selected song, manual priority, future unshuffled tracks and shuffle pool accurately; swiping to remove a future track + Undo must preserve the current song.
8. With one search-selected song and Repeat OFF, pressing Next does not silently create a recommendation queue. Do not overstate this as a complete recommendations implementation.
9. Confirm NW39 Library and NW42 premium player visuals, Google error reporting and offline media remain intact.

**Release gate:** Must pass build, automated tests and phone checks before labeling NW43 build verified or shipping it publicly.
