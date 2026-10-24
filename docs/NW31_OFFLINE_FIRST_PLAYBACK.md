# NightWave NW31 — Offline-first playback and storage safety

Status: **SOURCE PILOT / ANDROID BUILD AND DEVICE VALIDATION PENDING**

NW31 builds upon the full NW30 source tree. No original files, user music,
playlist data, lyrics, Google Drive backup or legacy player were removed.

## Changes
- A song selected in the main NightWave search/collection UI is now
  checked against its verified private app-owned offline audio association
  *before* going to YouTube online playback.
- Search and song-options "Play now" now use the same offline-first route.
  If the local file is unavailable, the normal online player remains the
  fallback; stale or corrupt identifiers do not receive a "ready" status.
- If multiple songs in the same collection have valid local files, a bounded
  offline queue is created and the tapped song starts first. Previous/Next
  operate through saved tracks; unsaved tracks are not inserted as online
  items in this private offline queue.
- This does not mutate playlists or streaming bookmarks or persist
  time-limited CDN URLs.
- SearchTrackOfflineIndex.offlineSelection reads the indexed library once,
  validates all selected file references and caps mapping work to 800 inputs.
- OfflineSearchQueuePolicy has pure Kotlin unit tests covering selected-first,
  duplicate removal, skipped unsaved results, and the 400-song queue cap.
- A conservative storage preflight rejects incoming search-stream audio when
  the server-declared file length plus the verified private library copy
  would exhaust free space. Unknown lengths need minimum free headroom and
  remain constrained to 250 MiB as they stream.
- A low-space failure shows a useful error rather than a generic extraction
  failure. Interrupted/invalid streams still leave no search-offline marker.

## Boundaries
- Saved audio is the source file in NightWave's private offline library;
  just bookmarking the video does not make it playable offline.
- A matching file size in the local library is a lightweight availability
  check, not full ongoing cryptographic integrity revalidation on every tap.
- Full YouTube catalog coverage is not possible from this mechanism.
  Stream access, DRM, signed-media failures and platform permissions remain
  meaningful limitations.
- YouTube developer policy requires prior approval for offline content
  storage; user permission alone cannot authorize redistribution of YouTube
  content. Treat the NW30/NW31 YouTube source pilot as **not approved for
  commercial/public release**, independent of technical test results.

## Local Windows QA
1. Extract the complete NW31 source archive into a fresh folder.
2. Run `build-windows.cmd`; require Kotlin/Java compilation,
   testDebugUnitTest, compileDebugAndroidTestKotlin and final APK signature
   verification to pass.
3. Run `check-device-update.cmd` before installing over an existing version.
   Never uninstall/clear music data merely to bypass a signer mismatch.
4. Save one authorized audio-only track; wait for "Saved in Offline Library".
5. Switch to Airplane mode and tap the **same item** in Search or song options:
   NightWave should start the local audio instead of trying to resolve online.
6. With two or more saved songs in a collection, press Next/Previous and
   confirm offline queue order. Unsaved songs remain omitted offline.
7. Delete one downloaded song using normal library controls and confirm
   the search link is no longer reported offline-ready.
8. Test nearly-full storage, failed/403/manifest URLs, cancellation,
   duplicate save, process restart, and corrupted/offline index.
9. Regression-test online playback, library playlists, lyrics, queue,
   Google OAuth, imports and all existing download sources.
10. Do not distribute the YouTube offline pilot without applicable
    platform and recording rights.

## Verification done in source session
- Pure Kotlin queue + disk-space policies: **10/10 isolated compile/runtime
  assertions passed**, using reproduced policies and a local Kotlin compiler.
- Core callback and tracked-file integrity: verified by exact GitHub content.
- The full Android APK has **not** been built/installed or smoke-tested.
- GitHub Actions was not triggered.
