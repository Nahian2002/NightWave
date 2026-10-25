# NW62 — Download Center off-main file-health verification

**Parent:** `feature/nightwave-nw61-google-oauth-diagnostics`  
**Branch:** `feature/nightwave-nw62-download-ui-reliability`  
**Status:** Source changes committed. **Gradle, JVM test execution, UI smoothness and real offline playback remain unverified.**

## Confirmed issue

`CatalogDownloadsActivity.render()` previously executed `OfflineAudioLibrary.availableIds()` synchronously on the Android UI thread for all historical completed downloads. That helper parses the saved index and checks the canonical private file paths/metadata for every relevant item. WorkManager LiveData events, filter changes, foreground resume and a 5-second stalled-transfer ticker can all trigger the render. With a large saved library, this risks frame stalls and unresponsive touch controls.

## Changes

- `refreshJobs()` snapshots the current three download job lists, obtains the completed download IDs, and verifies actual saved files under `Dispatchers.IO`. It renders on the main thread **only after that background health check finishes**.
- Each refresh has a monotonically increasing generation. The previous job is cancelled; late results from replaced reads or paused/destroyed/finishing Activity instances cannot overwrite newer downloaded/ready states.
- `onResume()` requests a fresh physical-file check, so deleting a track from Offline Library while Downloads was in the background will not leave a stale *Ready offline* label when the screen returns.
- `onPause()` cancels pending UI-only work. No foreground download, WorkManager transfer, offline file, playlist, permission or playback session is modified.
- Production readiness still depends on `OfflineDownloadStatusPolicy.isReady(... availableIds)`, not solely on `WorkInfo.State.SUCCEEDED`. A malformed offline index fails closed to an empty verified-ID set rather than lying about saved files.
- NW48, NW52 and NW55 legacy source checks were updated to require the correct `OfflineAudioLibrary(applicationContext).availableIds(savedIds)` off-main path. The Windows NW59 cumulative preflight now guards this new NW62 behavior.
- No user-facing claim of faster transfer throughput or HTTP Range partial-byte resume is introduced. This update targets **UI responsiveness**, not download network speed.

## Tests, source evidence and limits

- `DownloadHealthRefreshPolicyTest` adds three JVM checks for latest-generation-only publication, cancellation upon pause/destroy, and a refreshed generation on resume.
- `tools/verify_nw62_download_health.py --self-test` checks IO-thread placement, real file validation, non-destructive card rendering, lifecycle fencing and old regressions with seven broken-code fixtures.
- Windows `build-windows.cmd` runs the NW59 cumulative source gate, now including NW62's policy and test files. Hosted Android workflow also includes the NW62 Python gate when a runner is available.
- **Not verified by source checks:** full Android Kotlin/JUnit compilation, performance gains, a real WorkManager download, actual speed/pause/resume, visual clipping, Google authorization error 8 or public-release eligibility.

## Acceptance on device

1. Build NW62 using the full latest ZIP. All JVM tests, Android test-source compilation and debug APK signing checks must pass; do not install a failed/preserved APK.
2. With 0, 10, 100 and many completed WorkManager jobs, open Downloads and rapidly change **All/Active/Ready/Issues** while a transfer is running. Scroll should remain responsive and the correct download buttons must stay in place.
3. Move to Offline Library, delete a disposable saved file, return to Downloads. That successful historical job should show **Issues**, not falsely Ready.
4. Pause, cancel and restart an eligible authorized transfer. Progress/ETA must remain honest; restart still begins from zero and must never destroy a separate saved track.
5. Lock/unlock, rotate, background the app and come back during a file-health scan. Old status must not overwrite current UI. Confirm local audio plays with networking disabled.
6. Recheck Google setup via `check-google-oauth.cmd` on a successful exact APK from NW61; an OAuth report alone is not proof that error 8 is resolved.

**Public release status:** NO-GO until full on-device acceptance, OAuth, platform/media rights, Android API target and privacy review are completed.
