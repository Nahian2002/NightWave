# NightWave NW32 — Offline Library Integrity & Readiness

**Status: SOURCE REVISION. Windows Android build and device QA pending.**

The NightWave NW31 code allows online search tracks to prefer a user-saved
local file and supports search-track offline queues. NW32 fixes real issues in
its state reporting and collection lookup without changing storage formats.

## Changes

1. `OfflineAudioLibrary.availableIds(requested)` checks requested private
   library IDs with **one** library-index JSON parse and individual on-disk
   file health checks. A missing, corrupt-length, wrong-extension, unreadable
   or unindexed file never passes the availability check.
2. `SearchTrackOfflineIndex.offlineSelection` reads the saved-link mapping
   once, checks the linked IDs in a batch, and only forwards available IDs to
   `OfflineSearchQueuePolicy.plan`. The actual PlaybackService separately
   revalidates a file on playback to handle later deletions.
3. The Downloads screen no longer equates `WorkInfo.State.SUCCEEDED` with
   music availability. For searched, direct-source and catalog audio jobs,
   completion is shown as **Ready offline** only when its saved ID still
   belongs to an available on-disk library item.
4. If the audio was removed since the transfer finished, the previous job
   is correctly described as **Saved previously — audio missing/unavailable**,
   and only the Offline Library is offered, not a misleading play button.
5. The Downloads screen refreshes the verification on `onResume`: returning
   from deletion in Offline Library cannot leave a stale readiness display
   just because WorkManager didn't emit another progress update.
6. Only the at-most 60 visible jobs are checked during each render rather
   than repeatedly checking old hidden successes.
7. `OfflineDownloadStatusPolicy` is a pure testable readiness policy;
   `OfflineDownloadStatusPolicyTest` covers missing/deleted/local file
   identity and false completion claims.
8. Android instrumentation test `batchAvailableIdsRejectsMissingOrChangedFilesWithoutRewritingIndex`
   validates the new batched lookup with a real file, a missing file and a
   changed-size file. The test restores the previous index; it never removes
   a preexisting user song.

## Data durability and limits

- No migration, rebuild, cleanup, automatic deletion, changed song IDs or
  library clearing. Stale search mappings remain read-only until the user
  explicitly resaves a track.
- Full library capacity remains 5,000 tracks / 32 GiB, 250 MiB per file.
- The saved audio and search-link index are still private to the app.
- YouTube catalog coverage is NOT improved here. Some streams may not be
  savable; YouTube offline copying needs platform approval and applicable
  recording rights before any public distribution.

## Local acceptance checks

1. Extract full source ZIP in a clean Windows folder; run
   `build-windows.cmd`. Confirm testDebugUnitTest, assembleDebug,
   Android test source compilation and APK signature verification.
2. Run `check-device-update.cmd` before installing. Do not uninstall the
   existing version or clear NightWave storage to force a signer match.
3. Save a supported authorized audio source and verify the job shows
   **Ready offline**, then play it with Airplane mode enabled.
4. Delete that audio using Offline Library; return to Downloads.
   The old completed job must immediately show **missing/unavailable** and
   must not offer a misleading Play button.
5. With multiple saved songs in the same collection, verify Next and
   Previous still use the offline queue, including after an app restart.
6. Corrupt or remove a disposable test file (never personal music), inspect
   file health and ensure NightWave does not mark it ready.
7. Re-test direct authorized downloads, catalog downloads, streaming,
   playlists, Google backup and lyrics; no other features should regress.

## Verification

- Focused Kotlin policy smoke: 8/8 assertions passed in an isolated JVM
  compilation. Source callbacks and file-read guards were inspected.
- New Android instrumentation test is **authored but NOT executed**.
- Full Gradle Android build, APK signature verification and phone validation
  have NOT run in the connected GitHub editing environment.
- GitHub Actions was not triggered.
