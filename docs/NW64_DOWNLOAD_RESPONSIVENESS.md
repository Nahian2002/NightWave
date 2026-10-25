# NW64 — Download refresh responsiveness and bounded UI work

**Parent:** `feature/nightwave-nw63-download-history-navigation`  
**Branch:** `feature/nightwave-nw64-download-refresh-performance`  
**Status:** Source committed. Standalone production Kotlin timing policy compiled; **814/814 boundary/sweep assertions PASS**, with verified Git blob SHA-1 matching committed source. Full Android Gradle compilation, JUnit and Samsung visual/performance QA remain **NOT VERIFIED**.

## Confirmed source-level performance problem

Three separate WorkManager LiveData observers (catalog, direct, searched downloads) called `refreshJobs()` immediately on every progress update. Each invocation re-sorted the full history, rebuilt up to 60 Android view cards, rebuilt all filter tabs, rechecked saved-job identities and queued a scroll correction. If several transfer progress callbacks arrived in a burst, the Downloads UI performed repeated full layout work instead of using the most recent state once. The 5-second stalled-transfer ticker also directly refreshed the screen.

NW63 pagination limited *how many* cards were shown but did not bound *how frequently* they were rebuilt.

## Changes

- A single main-thread **coalesced progress repaint**, with at most one pending callback and a 400 ms minimum interval between ordinary observer-driven renders. The callback reads **fresh WorkManager LiveData lists at execution time**; a burst cannot overwrite a later terminal state with captured old data. This throttles **UI repaints only**; it does not delay or cancel WorkManager downloads, network reads or work completion.
- User-controlled filter and dismissed-history changes, initial/resumed paint and real saved-file verification completion remain **immediate**; pending observer repaint is cancelled before an explicit synchronous refresh.
- Pending refresh callbacks are discarded on Pause/Destroy; speed/ETA samples are reset when leaving the screen so reopening does not show stale throughput.
- The four static filter chips are reconstructed **only when the selected filter changes**, instead of on each byte-progress event.
- UI-only speed caches are pruned for non-running jobs, including jobs on older history pages or hidden from the visible list.
- NW63's 60-item page is exposed using `subList()` of an already filtered in-memory list, rather than `drop().take()` creating another potentially large copied list.
- The Windows cumulative source preflight now requires NW64 coalescer, bounded queue, lifecycle cleanup, policy/tests and memory-light pager while preserving NW62 file-health and NW63 history invariants.

## Tested, and not tested

- Compiled **exact committed** `DownloadRefreshPolicy.kt` with local Kotlin/JVM toolchain. Its Git blob SHA-1 was verified as `ea41efb44fa91a8d17c8b319edb3a38cb10d5a61` before testing.
- **814/814 PASS:** first paint, 400 ms timer boundaries, clock reset, near-`Long.MAX_VALUE`, and an 801-case sweep across elapsed times 0–800 ms.
- Added `DownloadRefreshPolicyTest.kt` JUnit regression coverage, not yet run under Android Gradle.
- **Not proven:** Android compilation, actual WorkManager callback ordering under load, downloaded-music completeness, true download throughput, responsiveness/frame time on Samsung, correct UI across rotation/large fonts, Google OAuth status 8, installed APK signer compatibility.

## Acceptance after future complete Windows build

1. With two or more legitimate, permitted transfers active, open Downloads; scroll and change All / Active / Ready / Issues rapidly while progress updates.
2. Verify progress numbers update within about a second, terminal records move to Ready/Issues after NW62 file verification, and controls react without a stream of full-screen jumps. A 400 ms repaint floor is **not a download transfer delay**.
3. Put a transfer on pause, cancel another, then navigate away and back. Paused state remains honest (Resume **restarts from zero**) and there are no stale speeds or ghost jobs.
4. Browse 61+ historical jobs with NW63 Older/Newer; use hidden-job controls and rotate. The selected history page and any already-saved files remain intact.
5. Exercise airplane mode, 1.3×/1.6× fonts, large queue history, and a blocked or stalled permitted audio source. No crash or false offline-ready state.
6. Only install a build after `build-windows.cmd` **SUCCESS**, `check-google-oauth.cmd`, and `check-device-update.cmd` verify the current signed APK. Never uninstall or clear saved audio as a workaround.

**Public-release decision: NO-GO.** This change is focused on Downloads display efficiency. It does **not** increase network/download speeds, create byte-range resume, fix Google OAuth, change content-provider eligibility or grant rights to store restricted audio.
