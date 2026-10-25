# NW69 — Queue transitions, Shuffle forward navigation and Kotlin compatibility

**Parent:** `feature/nightwave-nw68-audio-session-crossfade-safety`  
**Branch:** `feature/nightwave-nw69-queue-transition-consistency`  
**Status:** Source changes committed, exact production Kotlin queue files compiled with Kotlin/JVM 1.9.0 and independently exercised through **25,229/25,229 isolated assertions PASS**. Full Android Gradle compilation, JUnit suite and Samsung playback QA remain **UNVERIFIED**. Public release is **NO-GO**.

## Real source bugs addressed

1. **Offline Shuffle forward history lost:** The legacy `OfflinePlaybackQueue` tracks the actual Previous path in `visited`, but moving back added departed tracks to an unordered `unplayed` list rather than retaining the listening order. After several Previous operations, Next could select a different track than the user had just left. NW69 adds a bounded session-only `forwardVisits` stack. Next retraces that path before random selection, and removes returned indices from random candidates to avoid duplicating a played song. A new selection, explicit Jump or Shuffle toggle invalidates stale forward steps. Repeat One automatic replay never consumes a forward step.
2. **Public Play Next could jump backwards:** `PlaybackQueue.next()` previously looked only at the first outstanding manual-priority URL and accepted it even after a user explicitly jumped past its queue position. NW69 removes priorities at or before the current index, without removing those tracks from the playlist. Legitimate upcoming priority requests remain intact. This fixes *forced queue priority*, not the ability of Shuffle to randomly choose a previously heard track in a new cycle.
3. **Kotlin collection API compile errors caught by actual smoke compilation:** Previous queue source used `ArrayDeque.addFirst()` / `addLast()` calls, including NW67's new forward-history calls. These failed with the local Kotlin 1.9 compiler. NW69 uses supported `add(index, element)` / `add(element)` methods while preserving manual-priority ordering. This concrete source compatibility issue was not detected by older string-only audits.

## Scope and preserved behavior

- Offline changes affect **legacy per-track navigation fallback** only. Media3 still owns the order and gapless transitions in a prepared offline timeline; no decoder, stream URL, audio file, offline library entry or playlist is rewritten.
- Public queue remains a list of canonical user-selected media URLs with the existing size bound and `Play Next` priority. NW69 changes navigation state only, never music source eligibility or recommendations.
- Repeat Off/All/One, NW67 shuffle forward handling, file verification, downloads NW62–NW66, crossfade/EQ NW68, Google scopes and package/signing identity were not modified.

## Verification actually completed

The exact committed Kotlin production queue blobs were copied into an isolated JVM test, verified by `git hash-object`, compiled using `kotlinc-jvm 1.9.0`, and exercised together with no Android/Media3 dependencies:

- `OfflinePlaybackQueue.kt` blob: `a9e05b6ed5cec6055edef4f5b3c335fa42e3ab6c`
- `PlaybackQueue.kt` blob: `09b576679200d8cf7884ff2c39f938ab23c922a4`
- **25,229/25,229 assertions PASS**, including 5,000 seeded public transitions (15,000 invariants), 5,000 seeded offline transitions (10,000 invariants), exact Previous/Next, explicit branches, Repeat One, priority ordering and bounded queues.
- `tools/NW69QueueSmoke.kt` added to provide a reproducible Android-free version of the smoke.
- Added Android Gradle JUnit tests: `OfflineShuffleForwardNavigationTest.kt` and `PublicQueuePastPriorityTest.kt`. These test sources are committed, **not yet run with Gradle**. A successful pure Kotlin smoke does not prove Android compilation or real device playback.
- Updated `tools/check-nw59-integration.ps1` to require NW69 behavior and forbid the incompatible `ArrayDeque` call sites. Existing NW53–NW68 source checks are retained. PowerShell execution remains pending on Windows.
- GitHub workflow run [40230681563](https://github.com/Nahian2002/NightWave/actions/runs/40230681563) returned **failure** with no available executed-step details. That does not establish whether Kotlin or Android compilation started or failed; real Android build status remains **unverified**.

To reproduce the standalone JVM test with `kotlinc` installed, from the project root execute:

```sh
kotlinc app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt tools/NW69QueueSmoke.kt -include-runtime -d nw69-queues.jar
java -jar nw69-queues.jar
```

## Deferred Samsung acceptance after a successful signed build

1. Play a **public** 5-song queue, enqueue two songs as Play Next, and Jump beyond both. Next must move to the real next available track, not force an old priority backwards. Repeat with Shuffle enabled and verify priorities no longer appear in Up Next.
2. Verify the legacy offline queue with more than 64 private tracks (forcing the non-gapless fallback). Press Next three times, Previous three times, Next three times: the forward path should exactly retrace the songs previously heard.
3. Repeat with Repeat One, Repeat All, new queue selection, Shuffle toggle and explicit Jump; the UI must retain the selected song and not mutate library files.
4. Independently verify Media3's prepared offline timeline, gapless playback, crossfade, EQ and prior decoder-error pause safety. NW69 deliberately does not impose a separate shuffle ordering on Media3.
5. Before upgrading the existing Samsung APK, run `build-windows.cmd` to completion, `check-google-oauth.cmd`, and `check-device-update.cmd` to verify installed signer compatibility. Do **not** uninstall/clear music as an OAuth workaround.

**Still blocked:** Android Gradle/JUnit build evidence, full on-device queue/codec/background tests, Google OAuth error 8, media rights/source eligibility and privacy/Play compliance. Public APK/Play remains **NO-GO**.
