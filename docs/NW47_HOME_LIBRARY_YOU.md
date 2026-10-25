# NW47 — Home, Library & You redesign

**Parent:** `feature/nightwave-nw46-immersive-now-playing`  
**Branch:** `feature/nightwave-nw47-home-library-you`  
**Status:** SOURCE IMPLEMENTED. GitHub code reviewed against original UI actions; full Android compilation and device visual QA **NOT VERIFIED**.

## What changed

### Home
- Genuine compact recently-played cover shelf replaces up to six full-width rows. Every song retains separate Play, Options and Remove actions. Removing history uses the **same existing** confirmation/safe callback as before; it does not delete audio or playlists.
- Faster first screen: horizontally scrollable, 48dp-minimum quick shortcuts: **Continue** (only if a real recent track exists), **Offline**, **Liked songs**, **Find music**.
- Continue uses the existing identity-aware `onResumeSpotlight` decision, avoiding a blind service restart. Offline opens the existing offline library; Liked songs opens the actual liked collection in Library; Search uses the existing native search transition.
- Library counts now follow the listening spotlight rather than interrupting the first action rail. Personal mixes and mood shelf still use their original ranking, tracks and search actions.
- Empty recent history retains the original honest empty state. The See all toggle still exposes the original 100-entry cap as a lazy horizontal rail; Clear Recently Played remains available via the expanded view.

### Library
- A compact top shortcut shelf for Offline, Liked, History and New playlist; all use **existing** routing and dialogs.
- Folder filtering, pinned order, user-created playlists, smart collections, per-playlist play/shuffle, item options, bulk edit, reorder, share, history removal and undo are unchanged.
- No library writer, index, offline bytes or playlist format modified.

### You
- Smaller editorial header: **You**, with a local-only profile description that remains accurate even if optional Drive backup is connected.
- Direct shortcuts to Account & backup, Sound Studio and History before the advanced settings groups.
- A connected-but-not-backed-up account shows **CONNECTED**, rather than implying a completed Drive backup. Verified backup continues to require a successful upload, not an opened Google chooser.
- All existing theme, avatar, equalizer, memory opt-in, import/export, recovery and playback-diagnostic controls are preserved.

## Implementation notes

New component `NightWaveEverydaySurfaces.kt` keeps artwork requests on Coil and reuses `NightWaveArtworkPolicy` for safe YouTube thumbnail quality fallback. It uses `VideoPreview.author` (not a nonexistent artist field) and `MusicIdentity.display` for display only; saved identities are not changed. The fallback is a simple decorative musical note, not fabricated art. The main activity keeps the existing `onRemoveRecent` callback. Recent items are lazy-loaded horizontally rather than composing all 100 upfront.

All quick actions use at least 48dp tap heights; song options and Remove have their own 48dp buttons and content descriptions. Buttons are kept outside the song clickable surface to avoid accidental playback when removing an entry.

## Tests & evidence

- **Source-contract review: PASS**. Existing playback, library edits, Drive settings and search callbacks remain wired to the same state and actions; Home shortcuts appear before the history shelf, Library shortcuts before playlist lists, You shortcuts before backup controls.
- `tools/verify_nw47_surfaces.py` is wired into GitHub Actions with a negative self-test; checks source wiring, semantics, true `VideoPreview.author`, and preservation of library/backup actions.
- New `NightWaveEverydaySurfacesInstrumentedTest` defines two isolated Compose tests: 320dp quick-action click and independent recently-played Play, Options and Remove actions, with blank artwork (no network).
- **Execution limitation:** source contracts checked in the connected GitHub snapshot; no successful Gradle compile or Android instrumentation is claimed. The project's GitHub Actions runner has recently failed before executing steps. A failed job without execution steps is not proof of a Kotlin compile failure or success.

## Deferred combined-build acceptance

1. Build the stacked branch on Windows only after completing the agreed NW48–NW52 upgrades; preserve the installed app's signing key/data.
2. On a compact screen, tap every Home quick action and verify it opens the intended real destination; test while a current song plays and while offline. Continue must not restart a song unnecessarily.
3. Use recent shelf Play, Options and Remove separately. Remove must show the existing confirmation and leave liked/playlist/offline data untouched. See all and Clear Recently Played must still work.
4. Open Library Liked, History, Offline and New playlist; verify filtering, pinning, playlist sorting, selection, bulk edits and undo remain functional.
5. On You, test Account & backup, Sound Studio, History, profile rename, theme changes and reduce-motion; verify cloud **CONNECTED** never becomes **BACKED UP** without a verified backup.
6. Check 320/360/390dp, landscape/split-screen and 1.3–2.0× font size for clipping, especially horizontal shelves. Test dark/light palette, empty lists, no thumbnails and screen readers.

**Next:** NW48 Downloads & Offline 2.0. NW45 Google OAuth and full integrated Android compilation remain outstanding release gates.
