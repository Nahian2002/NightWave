# NightWave NW30 — Searched-Track Offline Source Pilot

**Status: LOCAL SOURCE PILOT — NOT RELEASE APPROVED**

NW30 keeps the existing NW29 offline catalog/import and NW28 Google Drive
source fixes. It does not remove existing app files, existing private music,
playlists, notifications, background playback, lyrics or cloud backup.

## What changed
- Online search result -> song options -> **Save offline**; the same action is
  available in full-screen Now Playing.
- A rights/permission dialog appears before saving. Confirmation does not
  replace publisher, creator or platform authorization.
- A WorkManager job resolves a currently accessible, separate AUDIO-ONLY,
  progressive HTTPS stream. No HLS, DASH, combined video, cookies, account
  bypass, private DNS, non-HTTPS redirects, DRM workaround or conversion.
- The signed source URL is resolved at worker execution and not persisted.
  Workers store only the canonical watch URL and a bounded display title.
- The existing hardened downloader verifies MIME, real file magic, size
  (up to 250 MiB), media tracks (no video), playable samples, staging path,
  private library capacity and atomic checksum-verified copy. Failures and
  cancellations never write a search-to-offline readiness link.
- On verified import, a bounded app-private index connects that canonical
  search result with the actual saved OfflineSong ID. It checks the local file
  again before displaying **Play saved offline**.
- Existing Downloads queue now includes search saves, progress, cancellation,
  failure and a verified **Play saved offline** action.
- The selected track's title and Unicode/Bangla characters are preserved
  within safe filename bounds. The app retains the original source format.

## Limitations and permissions
- **NOT all songs:** streaming access does not mean a complete file can be
  saved. Some tracks are live/manifest-only, restricted, deleted, protected,
  fail MediaExtractor validation or have time-limited URLs. Some will return
  HTTP 403 and need no retry loop that bypasses access controls.
- **Provider rules:** YouTube's API Services Developer Policies prohibit
  downloading, caching, storing and offline playback of YouTube audiovisual
  content without YouTube's prior written approval. User consent alone does
  NOT provide that approval. Do NOT distribute/enable a YouTube-content offline
  saver as a general production feature without securing appropriate platform
  approval and recording rights. The source pilot is NOT production clearance.
- **No MP3 output guarantee:** original audio-only streams may be Opus/WebM,
  M4A/AAC or other supported formats; the feature never falsely relabels
  compressed audio as MP3. DRM is never bypassed.
- **No server:** Android itself schedules the download; availability depends
  on connection, storage, battery/job scheduling and the source. At most 250 MiB
  per song; offline library limits remain 5,000 items / 32 GiB.
- **No installed APK/build was validated in this GitHub source-edit session.**
  A standalone pure Kotlin policy smoke (13 assertions) passed.

## Windows acceptance checklist
1. Extract the FULL NW30 source archive into a clean folder.
2. Run `build-windows.cmd`. Confirm Kotlin and Java compilation, Gradle
   unit tests, Android test source compilation and APK verification.
3. Run `check-device-update.cmd` on a connected phone. Never uninstall or
   clear data just to force a signing mismatch.
4. Search an authorized, public, unprotected recording; choose Save offline.
   Consent, network queue and foreground notification must behave correctly.
5. Verify a complete file is saved before any Ready Offline state appears.
   Disable Wi-Fi and mobile data; play that exact track from the offline queue
   or Offline Library; reopen app and repeat.
6. Test cancellation mid-transfer, no network, 403, private/redirected hosts,
   >250 MiB source, video/manifest-only track, duplicate download, corrupt
   index, full disk and app restart. Do not mark failures as saved.
7. Regression test existing YouTube streaming, playlists, offline imports,
   existing catalog queue, lyrics and Google Drive backup.
8. Do not market or ship unlimited YouTube offline storage without legal
   approval and real-world device QA.
