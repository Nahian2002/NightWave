# NightWave 2.0 — Phase 5C Offline Music Library

**Version:** 2.0.0-alpha5c / code 19  
**Branch:** `feature/nightwave-v2-phase5c-offline-queue`  
**Status:** implementation on a *draft* development branch. No compiled Android APK or device acceptance yet.

## Delivered
- Search imported audio by file-derived title from inside the offline library. The search is local: NightWave does not upload private filenames.
- Sort the visible collection by recently imported (default), title A–Z, smallest files, or largest files.
- Play all the currently visible songs (including the chosen search/sort order), or tap any row to play that track and queue the rest of the visible collection.
- Offline playback advances automatically to the next imported song at completion; media notification/headset Next and Previous operate on the *offline list*, not the old YouTube list. Previous restarts a track after three seconds, consistent with the existing player.
- Missing local files are skipped in a bounded pass. The offline queue is limited to 400 private file IDs; no arbitrary incoming file paths, signed streams or external URIs are trusted.
- The Compose playback bridge hides stale public-video queue rows, bookmark positions, and artwork while an imported offline song is playing. Local song metadata stays visible.
- Retains SAF user-selected imports, app-private storage, existing library index, previous data backups, legacy launcher and network playback routes. No YouTube downloading, DRM bypass, automatic cloud sync or Google login.

## Known limitations
- The offline queue is **in-memory only**. Imported audio files survive ordinary app restarts and remain visible in the Offline library, but the queue/order is not yet restored after a process kill.
- Offline playback uses linear progression; shuffle/repeat controls remain scoped to the public-video queue. End of list stops instead of silently repeating.
- Deleted/missing files cannot be played. Removing a file from a running local queue may interrupt playback.
- This is **not** a new permission to download or cache copyrighted YouTube tracks. Only audio the user selects from the Android system file picker is imported.

## Verification accomplished
- Audited source wiring for private-ID validation, UI list order, player handoff, on-completion navigation and YouTube/public queue isolation.
- Compiled the pure `OfflinePlaybackQueue` with the installed JVM Kotlin compiler and executed seven focused behavior checks (ordering, Next/Previous boundaries, selection validity, blank/duplicate filtering, max length, reset and a single file). **These are not Android JUnit/instrumentation results.**
- Added five repository JUnit cases in `OfflinePlaybackQueueTest` and `tools/verify_v2_phase5c.py` source gate.
- **Not run:** Gradle Android compilation, full JVM test suite, instrumentation tests, physical S24 Ultra/Bluetooth/headset testing. The original recurring app-crash cause has not been proven fixed.

## Required release acceptance
1. Download and extract the full Phase 5C branch into a clean folder; run `build-windows.cmd`. It should execute debug JVM tests and `:app:assembleDebug`. Do not install anything unless BUILD SUCCESS.
2. Optionally run `python tools/verify_v2_phase5c.py` for *source-only* checks. This alone cannot guarantee Android compatibility.
3. Update the existing debug-signed app in place. **Never uninstall/clear NightWave data** to work around a build or signing failure.
4. Import three owned audio files, disconnect Wi-Fi/mobile data, search for a title, sort, press Play all and verify the exact order; Next, Previous, completion and single-file end should work with the display locked.
5. Search for a nonexistent term, delete one imported song with confirmation, verify list/UI updates, and ensure the remaining entries are intact.
6. Play a public YouTube result afterward; confirm the old streaming queue and bookmarks are still correct, with no imported audio saved as a YouTube favorite.
7. Relaunch the app and confirm imported files remain. Queue persistence after process death is **not** a Phase 5C promise.
8. Confirm the premium preview, classic launcher, background audio, media keys and diagnostics remain safe. If Android crashes or warns about frequent crashes, run `capture-nightwave-crash.cmd` and share a redacted FATAL EXCEPTION trace.

Keep the PR **DRAFT** and do **not** merge into stable `main` until phone QA and a successful Android build validate this phase.
