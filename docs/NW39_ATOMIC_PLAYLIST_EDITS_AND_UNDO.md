# NW39 — Atomic Playlist Edits, Drag Reorder & Guarded Undo

**Branch:** `feature/nightwave-nw39-atomic-playlist-edits` (based on NW38)
**Status:** SOURCE IMPLEMENTATION; full Android compilation, instrumentation and physical-device QA still REQUIRED.
**Date:** 25 October 2026

## Delivered in this branch — in order

### 1. Single-transaction bulk playlist operations
- In a named playlist: **Select songs**, choose up to 20, then **Copy to…** another existing playlist or **Remove** from this playlist.
- Removed songs are taken out of **only that playlist**, after a confirmation dialog explicitly says favorites, history and offline audio are retained.
- Copy skips entries that already exist in the chosen target. Full target playlists, invalid URLs, duplicate inputs, and stale target snapshots reject the batch without partial writes.
- Both commands are added as `LibraryChange` operations to the existing `LegacyLibraryTransaction.mutate` single-writer path, which saves a recovery checkpoint and commits the entire target collection together.
- No new database writer, no new offline file deletion path, and no new server.

### 2. Reorder a named playlist
- The playlist **Reorder** mode switches to **Saved order** and temporarily hides sorting/filter input to prevent editing a misleading temporary view.
- Drag the dedicated **⠿** handle vertically. On release, a bounded row-distance translation calculates the intended target index and issues one guarded reorder transaction. A drag can move a song multiple rows within the visible area.
- Each row also retains **↑** and **↓** buttons for precise movement and assistive-tech use. The drag handle consumes gestures only in its 48dp area, leaving the background LazyColumn scroll and tab-swiping behavior outside that handle untouched.
- Order updates rearrange the exact original track objects by public URL and preserve metadata; invalid permutations, stale snapshots and out-of-bounds positions are rejected.
- Full auto-scroll during drag and visual spring/swap animations are **not** claimed here.

### 3. Immediate, conflict-safe Undo
- Library displays a contextual **Undo** banner after bulk removal, playlist copying and track reordering; single-song removal from playlist options now uses the same guarded path.
- A single saved Undo snapshot contains **before** and **exact after** playlist entries. Undo calls `RestorePlaylistIfUnchanged`; if the affected playlist was edited again, it refuses to overwrite the new changes. Unrelated playlists are never overwritten with an old snapshot.
- Undo is currently **single level, in memory for this Activity**, and is not guaranteed to survive app process death. A dismissed banner does not remove files.
- New selection controls never issue `deleteFile`, `deletePlaylist`, `toggleFavorite` or clear listening history.

## Source additions

- `library/LibraryMutationPlan.kt`: BulkRemoveFromPlaylist, BulkAddToPlaylist, ReorderPlaylist, RestorePlaylistIfUnchanged
- `library/MusicLibrary.kt`: facade helpers calling the existing single-writer coordinator
- `v2/library/PlaylistReorderGesturePolicy.kt`: bounded offset-to-steps and target-index policy
- `v2/ui/NightWaveStudioActivity.kt`: selection actions, drag handle, accessible up/down buttons, Undo banner
- `LibraryMutationPlanTest.kt`: cases for all-or-nothing writes, stale snapshots, copy capacity, preservation of favorites/history/other playlists, permutation-only reorder and guarded Undo
- `PlaylistReorderGesturePolicyTest.kt`: cases for tiny/large drags, NaN/infinite input, invalid rows and boundaries

## Automated QA status

- Verified via source audit: change types appear in the exhaustive `LibraryMutationPlan.apply` dispatcher; new facade calls, UI callbacks, undo guard and drag modifier are wired to their intended paths.
- Test cases were **written, not confirmed executed**. Full Kotlin/Android Gradle compilation has **not** passed.
- [NW39 GitHub Actions run](https://github.com/Nahian2002/NightWave/actions/runs/40176072917): failed before the job reached any runner (runner_id=0, zero steps). This provides no Kotlin compiler result.
- Do **not** treat source review or previously passed NW38 policy smoke as evidence that NW39 builds successfully.

## Acceptance gates before changing the installed app

1. Build the exact NW39 branch through the documented Windows build script; inspect Kotlin compilation and unit-test output. Keep the current app/signing identity; do not clear Android app data to install.
2. Create disposable playlists A and B containing 3–5 saved public tracks. Select 2 in A; copy to B. Verify the additions preserve order, do not duplicate existing B tracks and leave A unchanged.
3. Select 2 tracks in A; Remove; Cancel the first dialog; confirm the second. Verify only A changed, favorites/history/offline audio preserved, banner displayed, and Undo restores the exact prior order.
4. After a removal, make another edit to A and tap Undo. Expect **refusal**, not loss of the intervening change. Repeat with an unrelated edit to B; Undo for A should still be safe.
5. Enter Reorder on A; verify sorted and filtered views reset/hidden; drag a handle upward/downward, and use ↑/↓ to move one position. Verify persisted ordering after force-stop/relaunch and no accidental play or cross-tab swipe triggered by the drag handle.
6. Test 360dp mobile and large-font display for dense control rows, rotating during selection, 20-track limit, long titles, keyboard/gesture navigation and TalkBack labels.
7. Regression: Home, Search history, pinned playlists, recent-play removals, downloaded media, lock-screen/background playback, offline in airplane mode, Google Drive account UI and queue.
8. Confirm successful release signing continuity and previously documented public-release legal/privacy/OAuth gates separately.

**Do not merge or publish this unverified source as a release APK.**
