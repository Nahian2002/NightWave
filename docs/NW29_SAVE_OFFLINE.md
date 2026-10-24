# NW29 — Offline-first NightWave player UX

## Goal
Make offline access feel like an integrated feature, without promising that
every YouTube song can be downloaded or confusing a different recording for
the track originally selected.

## Implemented
- Full-screen Now Playing: **SAVE OFFLINE** action for streamed tracks,
  **READY OFFLINE · OPEN LIBRARY** for actual local offline playback.
- YouTube song options: **Save Offline (eligible audio)** opens the
  licensed-audio discovery screen directly with the selected title and creator.
- An original video title is used to search up to two sanitized queries:
  title + creator, then title only. It never automatically selects an
  alternative recording or claims exact audio identity.
- The discovery screen shows explicit source/recording disclaimers.
  Open-catalog files still require eligible licenses, file validation and
  user's choice before queueing/saving.
- One screen links to the existing **Import my audio** (offline library) and
  **Authorized audio link** (direct HTTPS original-format download) flows.
- NW28's WorkManager queue and original-quality audio preservation remain
  available, as do the legacy library, online search and playback.
- Pure Kotlin `OfflineDiscoveryPolicy` with dedicated unit tests for
  title/artist fallback, decoration removal, version preservation and
  bounded search strings.

## Important boundaries
- An eligible independent recording is not assumed to be the same as
  a YouTube track, even when its title matches.
- This update does **not** download, cache, demux or transcode YouTube streams.
- A favorite/playlist bookmark is not offline audio. The app only shows
  "Ready offline" when playing an actual offline library item.
- If no eligible recording exists, the YouTube track remains online-only.
- Music must be legally available for downloading/retention; rights and
  availability determine catalog coverage.

## Local Windows acceptance
1. Download the **complete source ZIP** from this commit, extract fresh.
2. Run `build-windows.cmd` on Windows. Require Android Gradle compile,
   JVM tests, instrumentation test-source compile and APK signer validation.
3. Run `check-device-update.cmd` before installing over an existing app.
   Never uninstall or clear data to get past signer mismatch.
4. Test a YouTube search result -> three-dot menu -> Save Offline:
   eligible-source discovery prepopulates title/creator and searches without
   extra typing. Verify unknown/different recordings are clearly identified.
5. Open Now Playing -> SAVE OFFLINE, then test offline playback's
   READY OFFLINE state and open Offline Library.
6. Test import, direct authorized audio, and catalog background queue.
   Toggle Airplane mode; verify only truly saved files are playable.
7. Confirm playback, search, playlists, queue, lyrics and Google backup
   still work as in NW28. A source-level test does not prove these on-device.

## Verification performed
- Source refs and callbacks were checked by direct committed-file readback.
- OfflineDiscoveryPolicy search behavior passed an isolated local Kotlin smoke
  using a matching source implementation and safeSearchTerm.
- Full Android build and phone behavior are NOT verified in this environment.
- GitHub Actions was NOT invoked.
