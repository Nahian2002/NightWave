# NightWave NW-14 — Preserve Neighbouring Songs in Long Queues

**Date:** 22 October 2026  
**Base:** NW-13 commit `5797ebdc7d706e513e1b6c2ed20a481e9ecb58ad`  
**Branch:** `fix/nightwave-v2-nw14-large-queue-window`  
**Release:** **NO-GO** pending complete Gradle build, signer verification and real-device testing.

## Source-confirmed issue

`QueueSelectionRules.plan()` already kept a selected song beyond the public playback service's 100-track queue cap. However, when the listener tapped an item outside the first 100 tracks, it **prepended the selected song at index zero** and filled the remainder with unrelated tracks from the *beginning* of the original playlist.

Example: A user selects Song 150 in a 201-song playlist. The resulting queue was [Song 150, Song 0, Song 1, ..., Song 98]. The selected song played, but pressing **Next** would play Song 0, not Song 151. Previous also lost the actual Song 149 neighbour. NW-13 corrected the meaning of Previous in non-shuffled playback, but it could not fix missing neighbours that were excluded when the queue was first created.

## NW-14 behavior

The existing URL normalization and deduplication run first. Then:

- If the selected URL is **already in the collection** and the normalized list fits the cap, retain the full order unchanged.
- If it is in a longer list, take one **contiguous capacity-sized window around the selected track**, clamped to the start or end. For Song 150 / 201 tracks / capacity 100, the queue includes Songs 100–199, with Song 150 at index 50; Next and Previous therefore point to Songs 151 and 149 respectively.
- If the listener selects the **last** song, keep the trailing window, so the previous song is still available.
- If the selected video is **not part of the supplied collection**, keep it as the first playable item, followed by up to 99 existing collection items. The caller cannot infer true neighbours that aren't in the list.
- If the URL is invalid, the selection remains empty rather than playing the wrong video.
- With `capacity=1`, the selected song remains the only item.
- The service still receives at most 100 validated canonical public-video references. No signed audio URLs or private offline file locations are persisted.

## Regression coverage

`QueueSelectionRulesTest.kt` now contains **11 JUnit methods**, including six new tests for selected middle/last/early positions in long lists, outside-collection selection, invalid and duplicate entries, and capacity one.

A new preflight `tools/verify_nw14_queue_window.py` asserts selection mechanics, playback bridge wiring, relevant tests, and retention of earlier NW-01–NW-13 source and full Android Gradle build gates.

## Verification evidence and limits

- The current `VideoLink.kt` and updated `QueueSelection.kt` were recreated as exact local bytes and matched the GitHub source **blob SHA-1**:
  - `VideoLink.kt`: `59ec2ae949f250133a6c2261445a52010af6e8e7`.
  - `QueueSelection.kt`: `a2d68b6948861179f21eb58a4b8af7deb9267f61`.
- The local Kotlin compiler attempt was **terminated after becoming unresponsive**. No new NW-14 JVM test or compilation pass is claimed.
- The connected GitHub Actions runner has previously failed with **zero job steps**; a new source workflow push cannot be counted as compilation without executed steps.
- Android SDK and a private-repo Git clone are not available in this current environment, preventing a complete standalone APK build.
- No user library files, installed Android package, Google Drive authorization settings or signing certificates were changed.

**Release remains NO-GO.** The queue-window change and authored regression coverage are source-verified, but actual Kotlin/Gradle execution and device acceptance are still required.

## Deferred acceptance

1. Download the exact NW-14 branch ZIP to a fresh Windows folder.
2. Use `build-windows.cmd` and require passing `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
3. If the build fails, review the current-run `NightWave-build-errors.txt` and the optional bounded JUnit failure report rather than older build logs.
4. Before installation, check APK signing compatibility using the existing read-only tool. **Do not uninstall or erase app data to bypass a signature mismatch.**
5. With public videos in a playlist longer than 100 entries, tap a middle or last item, verify the selected track and its neighbours, test non-shuffled Next/Previous and Shuffle toggling. Repeat with duplicate and invalid entries.
6. Regress NW-01–NW-13 functionality, including search, Sound Studio EQ, account/backup permission and long-running background playback.
