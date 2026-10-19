# NightWave Phase 6X5 — Smart collections and playlist folders

## Scope / parent branch
Base branch: `feature/nightwave-v2-phase6x4-offline-repeat-shuffle`; this phase retains the previous EQ fixes, Sleep Timer dial, fast Repeat, 6X1 offline gapless, 6X2 local crossfade, 6X3 volume normalization and 6X4 offline shuffle/repeat. No backend, network subscription, permission, signing change, app identity change or playlist-data migration.

## New features
### Smart collections (read-only, automatically refreshed)
- **Recent rotation:** distinct actual recently played public tracks, max 100.
- **Rediscover your likes:** distinct saved favorites shown in a different order, max 60.
- **Long listens:** from existing public library data, only tracks with declared duration >=4 minutes; requires at least two.
- **Creator mixes:** up to remaining slots of an eight-collection cap, grouping two or more distinct saved tracks by their displayed creator/uploader metadata.
- Sources: at most 600 distinct input candidates from liked tracks, recent history and first 60 saved playlists. Collections recompute when local library changes, with no saved duplicate playlist and no new listening tracking or user-account requirement.
- These are derived local lists of **public saved/recent tracks**. They are not smart offline-file playlists, nor verified genres, moods or play-count rankings. Missing or untrusted metadata never invents genre/year or listening counts.
- Smart collection tiles open a read-only Library page using existing song filters, sorting and Play / Shuffle play. They cannot be shared as if they were saved user playlists.

### Playlist folders (optional local metadata)
- **Organize playlist folders** in Library: Create, Rename, Delete, and choose a playlist to move.
- Folder filter chips include **All folders**, **Unfiled**, and each saved folder. Playlist rows show current folder status.
- Open an existing playlist and tap **Folder · [name] · Change** to reassign it or remove its folder.
- Folder changes are atomic changes only to `nightwave_playlist_folders_v1`, not the original `nightwave_library_v1`. Deleting a folder **unfiles** the associated playlists; does **not** delete the playlists or their songs.
- 40 folders maximum, 300 placements maximum, names 1–60 characters, case-insensitive duplicate rejection.
- Invalid/malformed snapshots are *not* overwritten by a follow-up mutation. Keep originals intact for diagnosis or manual recovery.
- Playlist rename/delete through NightWave Studio's built-in **Manage playlists** action updates the placement metadata after the legacy library operation succeeds. Classic/other older surfaces may rename independently and leave a stale folder placement; users can reassign with the folder manager.
- Folder metadata is currently **local-only** and is not yet included in the existing guest export / Google Drive backups. Do not represent it as backed up until a separately verified schema extension exists.

## Verification
- Unit test source added:
  - `PlaylistFolderPolicyTest.kt`: 6 cases — safe name handling, folder lifecycle, moving, preserving playlist rename linkage, bounds and original-state immutability.
  - `SmartLibraryPolicyTest.kt`: 5 cases — empty sources, actual creator metadata grouping, no playlist mutation, time-based collection correctness, size cap.
- Source gate `tools/verify_v2_phase6x5.py` added and invoked by `.github/workflows/android.yml`. Android CI push targeting this branch configured.
- The new source references and structure have been reviewed, but **no Android Gradle compile, device screenshot or JUnit run has yet been confirmed**. Source presence is not an APK release gate.

## Phone acceptance test after real build (deferred)
1. Run `build-windows.cmd` and require BUILD SUCCESS. Install only if signer matches the already installed NightWave. Never uninstall/clear library data to fix signing.
2. Go to Library > Organize playlist folders > New folder. Create "Chill" and "Energy". Confirm duplicate "chill" and invalid folder names are rejected.
3. Move existing playlist A into Chill and B into Energy; check All, Unfiled, Chill, Energy filters, including search and sorting.
4. Open playlist A and change its folder; confirm song order, count, streaming URLs, cover, sharing and queue playback are unchanged.
5. Rename folder, then delete it with confirmation. Confirm playlists are all still present and become Unfiled.
6. Via Manage playlists, rename a playlist that is already filed, then check its folder assignment still follows it. Delete a temporary playlist and verify its placement metadata disappears (without touching other playlists).
7. Check the smart section: Recent rotation matches local history; Rediscover your likes contains only favorites; Long listens show songs with duration >=4 min when available; Creator mixes require >=2 distinct entries by the same displayed artist/uploader.
8. Like a new public song or edit a saved playlist; reopen Library and check smart collections refresh without creating duplicate persisted playlists.
9. Open a smart collection; Play, Shuffle play, search and sort. No "Share playlist" option should claim to export a virtual smart collection.
10. Switch to Offline music, then return; the imported files, lyrics, Sleep Timer, EQ, gapless, crossfade, normalization, 6X4 repeat/shuffle, existing Drive backup and legacy library should still work.
11. Relaunch normally and verify folders persist without auto-starting music. Check portrait/landscape and enlarged Android font scale.
12. Check old guest backups still work; folder metadata is deliberately outside their current schema.

## Known limits / non-goals
- No user-editable compound smart rules yet. No offline imported-file smart mixes, automatic genre inference, AI cloud recommendations or verification of official artist identity.
- Existing playlist management UI still performs legacy playlist writes; folder management never changes them.
- No automatic migration/repair of legacy playlist renames from other screens. Stale placements are ignored when rendering the actual current playlist list.
