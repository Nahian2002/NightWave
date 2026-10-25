# NW62 — Accurate Downloads history when saving/retrying songs

**Parent:** `feature/nightwave-nw61-google-oauth-diagnostics`  
**Current source branch:** `feature/nightwave-nw62-download-history-accuracy`  
**Status:** Source implemented; Android compilation/JUnit and phone verification pending. No APK was published.

## Confirmed source-level bug

The Downloads page first sorted all historical WorkManager jobs, then deduplicated retries with the same source ID, **and only afterward filtered by All / Active / Ready / Issues**.

If an earlier search-download request had successfully saved a still-valid file but a newer attempt for the same recording failed, the newer failed request consumed the shared identity. The older verified success was filtered out of **Ready**. Similarly, an active new attempt could hide the existing playable song.

This was a visibility problem, not lost offline audio. A historical WorkManager success is not proof that a saved file still exists.

## Changes

1. Introduced pure, read-only `DownloadHistoryProjectionPolicy`: apply the requested filter using WorkManager state AND actual saved-file availability, sort active requests before terminal requests and newer requests before older ones, then deduplicate the canonical **public search-track identity** within that filter.
2. `CatalogDownloadsActivity` now creates one projection of WorkManager metadata and verified private-file availability, uses it to produce the selected rows and calculates Active / Ready / Issues counts after filter-specific deduplication.
3. A newer failed or running retry will no longer hide an older verified saved song **in the Ready filter**. The latest failed retry still appears **in Issues**, and the latest active retry in **Active**; All continues displaying one latest row per public search identity.
4. Direct authorized audio and catalog jobs with no search-video retry tag keep independent records. The 60-row visible-card limit, WorkManager cancellation/pause/restart semantics, query/search audio rights, source-validation rules, local audio files and download workers are unchanged.
5. Never mark offline-ready based only on a WorkManager `SUCCEEDED` record. The existing `OfflineAudioLibrary.availableIds()` batch check still verifies saved files before any green Ready status.

## Tests

- `DownloadHistoryProjectionPolicyTest` has seven focused JVM tests: newer FAILED vs older verified saved file; active retry vs older success; duplicate failures; missing file; unrelated direct/catalog jobs; deterministic priority; empty/invalid states.
- `tools/verify_nw62_download_history.py --self-test` protects five deliberately broken source fixtures and the real file-health/queue/pause control wiring.
- The Windows `tools/check-nw59-integration.ps1` cumulative source guard now checks NW62 before `build-windows.cmd` starts Gradle.
- GitHub Android CI invokes both new Python guards, provided a runner becomes available. Earlier CI jobs have ended without any runner steps, so there is **no verified new Gradle pass**.

## Device acceptance

Use a test song whose offline file exists. Start a separate retry for the same recording, let it fail or remain active, then examine the **All**, **Ready**, **Active**, **Issues** filters and summary counters. The earlier validated song must still be shown under Ready; the new attempt should be visible in its appropriate state. If the offline file is later deleted, its historical SUCCEEDED WorkManager record must appear as an Issue, **not** Ready. Do not delete real music for testing; use a disposable authorized recording.

Also test a large history (over 60 WorkManager jobs), filters after rotation, screen-reader labels, slow transfer speed, and offline playback with networking disabled.

**Google account authorization error 8 remains an external/device-verification blocker.** NW62 does not change Google Cloud OAuth credentials, app signing, Google Play services, audio ownership, or user library data. Public release remains NO-GO pending full build, device QA and permissions/media-rights checks.
