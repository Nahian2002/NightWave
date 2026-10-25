# NW63 — Download history navigation and cumulative build-gate repair

**Parent:** `feature/nightwave-nw62-download-status-performance`  
**Branch:** `feature/nightwave-nw63-download-history-navigation`  
**Status:** Source integrated. Pure Kotlin paging policy compiled and passed 20 local assertions. **Full Android Gradle compilation, instrumentation and real-phone UI are not yet verified.**

## Why

Previously, `CatalogDownloadsActivity` filtered and sorted WorkManager transfers, but ended in `.take(60)`. Jobs beyond the first 60 **could never be opened from the Downloads screen**, even if WorkManager still remembered them. Repeated retry records were deduplicated for visible rows, but counters used the non-deduplicated list, so the overview could disagree with what the user could browse.

A separate issue was found in `tools/check-nw59-integration.ps1`: its preflight required `OfflineAudioLibrary(this).availableIds(allSavedIds)`, the obsolete **main-thread** scan removed in NW62. The guard contradicted the safer off-UI-thread implementation and could reject an otherwise correct source tree before starting Gradle.

## Changes

- **Newer / Older history** navigation, 60 row views at a time. Each page shows a truthful `Showing X–Y of N jobs` caption. All matching WorkManager jobs remain browsable without inflating thousands of TextViews in one render.
- Active jobs still sort first, then newest known requested time. Retry identities are deduplicated **before** counters, filter and page calculations, so summaries and page counts agree with the browsable history.
- Switching All/Active/Ready/Issues or toggling dismissed jobs starts at page one. A shrunken history clamps an old page, and rotation restores filter, dismissed visibility and the bounded page index.
- Existing NW62 asynchronous file verification still controls **Ready offline** and **Issues**; WorkManager SUCCEEDED alone never means a playable file.
- The Windows preflight now checks the **background** verification call, not the removed blocking call, and requires the NW63 navigation/policy/source tests.
- **No WorkManager jobs are cancelled or deleted by paging.** No downloads, private music files, saved-song IDs, playlists, account scopes, package identity, signing parameters or API target were changed.

## Source and unit verification

- Compiled the production `DownloadHistoryPagePolicy.kt` with installed `kotlinc` / Java 21 in an isolated JVM smoke. **20/20 assertions passed** for 0/60/61/121 jobs, old/negative/huge page indices, Previous/Next bounds and summary text. This validates the pure policy, **not Android UI runtime**.
- Added JUnit tests for page boundaries, safe clamping when a queue shrinks, navigation and last-page captions. These tests **have not been run in Gradle**.
- Cumulative PowerShell gate checks the actual NW63 wiring and prevents accidentally reintroducing the NW62 main-thread scan. Windows execution of the PowerShell gate is still pending.

## Windows build

1. Download the complete ZIP from this NW63 branch into a **new, clean folder**; do not overlay it on an older NW52/NW62 source.
2. Run `build-windows.cmd`. A source preflight PASS is **not** an Android compiler PASS. Retain the full `dist/NightWave-build-*.txt` reports and Gradle log if it fails.
3. Only after a successful build run `check-google-oauth.cmd` and `check-device-update.cmd` before installing onto the existing Samsung. Never uninstall, clear NightWave storage or delete private audio to overcome a signer problem.
4. On the phone, start a few disposable authorized downloads, then verify Downloads filter changes, repeated retries, 61+ historical job rows, Newer/Older controls, hidden-history toggle, rotation and font scaling (1.3×/1.6×). Verify old saved offline songs still play.

**Remaining major blockers:** complete Android compile, phone navigation/frame QA, Google account status 8, offline playback/transfer reliability on device, API/Play release requirements, licensed distribution rights. **Public release remains NO-GO.**
