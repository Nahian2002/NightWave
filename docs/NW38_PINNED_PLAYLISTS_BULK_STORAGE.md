# NightWave NW38 — Pinned Playlists, Multi-select and Offline Storage

**Status:** Source branch complete; Android build and Samsung device validation **PENDING**.
**Base:** NW37 `feature/nightwave-nw37-search-and-song-actions`.
**Date:** 25 October 2026.

## What changed

### Playlist pinning
- A 48dp star toggle sits beside each named playlist in Library. Open a named playlist for a full-width Pin/Unpin control.
- Pinned playlists appear ahead of unpinned playlists while preserving the user's active A–Z, Z–A, most-songs or saved-order sort **within** each group, and respecting folder/search filters.
- Preferences are isolated under `nightwave_playlist_pins_v1`. The store never writes song metadata, changes playback order or deletes audio.
- Playlist rename/delete via the existing management UI updates pin metadata after the playlist library write succeeds.
- Invalid, oversized or corrupt pin preferences reject changes rather than silently replacing existing data; the normal library remains readable.

### Non-destructive bulk selection
- Open any **named playlist** and use **Select songs**. Select up to 20 tracks, including "Select first 20" of the currently filtered view.
- In selection mode, tapping a track toggles its selection instead of unexpectedly starting playback.
- **Copy links** copies normalized public URLs to the Android clipboard; **Share links** invokes Android's share sheet for the selected URLs. Ineligible URLs are omitted, and actions never expose private file paths.
- Press Cancel selection to return to normal playback and song menus.
- Bulk deletion, playlist transfer and drag-to-reorder are **not** in this patch. They would require verified atomic playlist edits and UI gesture collision tests. No fake controls.

### Offline Music storage feedback
- Directly under the Offline Music title, a compact line reports playable imported-audio file count and bytes, actual free storage from the app's file system, and any unavailable index entries.
- Playable bytes are computed exclusively from health-checked imported entries, *not* all index entries, and not the separate searched-song download vault.
- An unreadable index shows **Storage totals unavailable**, without attempting recovery or deleting files.
- Unit formatting is locale-stable and defensive against unknown free space.

## Guardrails
- Existing Google Drive, search, music playback, Media3 service, downloaded files, folder assignments, saved playlists, package identifier and signing settings were not modified.
- `main` remains untouched; branch contains the NW37 UI source changes.
- Unit tests added: `PlaylistPinPolicyTest`, `LibraryBulkSelectionPolicyTest`, `OfflineStorageSummaryTest`.
- Local standalone `kotlinc` policy smoke: 14 assertions passed (9 pin/storage plus 5 bulk selection), including bounded selection, sorting, pin removal and free-space formatting. This is **not** a full app build.

## Full test gate before installation
1. On a full NW38 checkout, run `build-windows.cmd` and `check-device-update.cmd`. Match the installed signing certificate before updating. Do not uninstall or clear NightWave to force an APK update.
2. In Library, pin two playlists, sort A–Z and Z–A, apply a folder filter, rotate/restart, and verify pins stay ahead of unpinned entries with the selected intra-group order.
3. Rename and delete a disposable pinned playlist; confirm no phantom pin and no unrelated playlist content or offline audio changed.
4. Select songs inside a named playlist. Select, deselect, select all (20 cap), copy and share; confirm URLs are public and in filtered/display order. Exit selection and play normally.
5. Verify short and very long titles at 360dp and Android font scaling 1.3× and 1.6×; verify tapping the new star doesn't start playback.
6. In Offline Music check the displayed imported-audio size against healthy entries and device free space. Corrupt/missing index entries must not be counted as playable; do not test corruption on your actual saved data.
7. Regression check: NW36 history removal, NW37 Search history, playback and notifications, downloads and pause/resume, offline airplane-mode play, queue/shuffle, Google connection and backup UI.

**CI note:** [Initial NW38 Android Actions run](https://github.com/Nahian2002/NightWave/actions/runs/40174507127) failed before any job steps executed, with runner ID 0 and no compiler logs. It is an infrastructure-level blocked check, **not** proof of a successful or failed Kotlin build. Device build remains unverified.

**Public release:** No approval implied. Previous OAuth, licensing, release signing, target API and privacy-policy gates remain outstanding.
