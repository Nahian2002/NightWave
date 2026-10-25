# NW62 — Downloads file-health responsiveness

**Parent:** `feature/nightwave-nw61-google-oauth-diagnostics`  
**Branch:** `feature/nightwave-nw62-download-status-performance`  
**Status:** Source implemented; full Android compilation/real-device rendering NOT verified. Public release remains **NO-GO**.

## Confirmed issue in previous code

`CatalogDownloadsActivity.render()` ran `OfflineAudioLibrary(this).availableIds(allSavedIds)` synchronously on Android's **main/UI thread** for every screen refresh, including WorkManager progress updates and the five-second active-transfer ticker. That reparsed the private music index and checked indexed song files even when *only progress bytes changed*. With many saved songs or slow storage it could delay button responses and scrolling.

## Fix

- A **single background file-verification worker** handles file-health scans; its pending queue holds only **one waiting update**, dropping obsolete queued checks.
- Only changes to saved-file identity sets trigger additional file checks. WorkManager byte-progress updates reuse the cached verified snapshot instead of touching the private library.
- Returning from Offline Library forces a fresh verification even if WorkManager status is unchanged; a manually deleted audio file cannot permanently retain an old Ready status.
- Snapshot results are guarded by a monotonic generation number and Activity lifecycle. Late results cannot overwrite a newer list or update a destroyed Activity. Background worker is shut down on Activity destruction.
- While verification is pending, the screen says **Checking saved files**. It never treats WorkManager's SUCCEEDED status as proof a real playable file exists, and **does not falsely mark the unverified file as Needs attention**. If an unexpected scan exception occurs, it shows **Could not verify saved files** rather than falsely claiming Ready or Missing.
- Once verification finishes, the familiar Ready and Issues filters reflect actual private audio membership and file-health checks; existing Play Offline, Pause, Cancel, retry and download histories remain unchanged.
- No arbitrary downloads, YouTube restriction workarounds, API scope changes, audio file deletion, database migration, or network requests were added.

## Regression coverage

- `DownloadFileVerificationPolicyTest.kt`: four pure unit-test cases covering stable saved-file set caching, stale generation/lifecycle blocking, pending/failed success never becoming Ready, and unknown health never becoming an Issues alert.
- `tools/verify_nw62_download_health.py --self-test`: source contracts and seven mutation fixtures for queue bounds, generation fence, false-Ready state, worker disposal, and missing tests.
- Existing Windows-only `tools/check-nw59-integration.ps1` now checks NW62 and its files before Gradle; Android CI also runs the source gate when it can acquire a runner.
- Static checks do **not** substitute for full Kotlin compilation, WorkManager runtime or phone battery/frame measurements.

## Acceptance on Samsung after a successful NW62 build

1. While a legitimate offline save transfers, open Downloads, scroll, tap filters and rapidly switch tabs; monitor smoothness and updates, especially with dozens of finished jobs.
2. A new completed transfer must show **Checking saved file** before **Ready offline**; it must not appear as Ready without an actual file.
3. From Offline Library, delete a disposable local sample. Return to Downloads and confirm its old completed work is reclassified under Issues only after verification finishes.
4. Open Downloads with airplane mode on and with 0, 1 and many saved files. Safe local checks must work without a network.
5. Pause, Cancel, retry and WorkManager progress must retain their existing semantics. **Pause/Resume still restarts at byte zero**, not range continuation.
6. Rotate/leave Downloads while checking files. No stale Ready state, app crash or background-worker leak should appear.
7. Verify old playlists and saved audio still play. Signer continuity must be checked before an in-place APK update; never uninstall/clear data to fix a failed build.

**Outstanding:** Google OAuth error 8, true offline playback and download throughput on-device, NW62 full Gradle/JUnit build, visuals after NW53–NW62 changes, production signing, Play target/API compatibility and media-rights compliance.
