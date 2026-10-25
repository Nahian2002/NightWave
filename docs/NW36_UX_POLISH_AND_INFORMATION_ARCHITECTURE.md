# NightWave NW36 — UX Polish, Information Architecture and Small Actions

**Status: SOURCE PATCH — complete Android build and on-device layout QA PENDING.**  
**Base:** NW35 `feature/nightwave-nw35-google-download-controls`.  
**Date:** 25 October 2026.

This update favors **predictable placement, smaller touch-friendly actions, no data loss, and keeping the music central** over decorative animation or a large navigation rewrite. The original name, theme engine, bottom navigation, Now Playing, background playback, online Search, Google Drive code, package identity and source build configuration are preserved.

## Screen-by-screen arrangement

| Screen | Main job | What should live here | NW36 work |
| --- | --- | --- | --- |
| **Home** | Resume listening quickly | Greeting → resume last song → recent tracks → personalized mixes → discovery → library route | **Changed:** moved Recently Played above discovery; See all/Show less; remove a single history row via dedicated ×; confirmed Clear history only after expanding |
| **Search** | Find a song or creator | Search field and saved suggestions first → explicit public/YouTube search → offline library shortcut → browse by mood/creator | **Changed:** one-tap Browse offline songs when search box is empty; previous provider/search code preserved |
| **Library** | Organize what you save | Offline Music, Liked Songs, Recently Played, custom playlists/folders; inside playlist: Play/Shuffle, filter, song menu | **Changed:** Recently Played is now a full collection with edit and clear; per-playlist song menu adds removal from *that playlist only*, with confirmation; irrelevant Manage Playlists footer hidden from History/smart/liked views |
| **Now Playing** | Listen, not organize | Artwork/title and large play controls, seek/scrub, favorite, queue; secondary Save offline, playlist, lyrics, sound settings | Existing layout retained; no untested re-layout of the sensitive player or gestures |
| **Downloads** | Control transfers | Running/queued first, speed/progress/pause/cancel; finished cards with Play/Open Library and a non-destructive dismiss option | **Changed:** Hide finished download row with Show dismissed toggle; does not delete actual audio, file IDs or WorkManager data |
| **Offline Library** | Quickly play or find local files | Search saved songs near the top; compact Downloads/Import/Find/Direct action grid; Google Drive as secondary link; sorting, Play all, Shuffle and list | **Changed:** search-first layout and balanced 48dp quick-action grid, fewer stacked tall buttons; existing offline file handling and playback preserved |
| **Profile / You** | Personal preferences and privacy | Account and cloud backup, audio preferences, appearance, listening privacy/statistics, diagnostics/advanced | **Changed:** Manage Recently Played routes directly to editable Library history |
| **Queue** | Adjust upcoming music | Playback order, drag/swipe/dismiss and existing undo | Existing behavior retained; no duplicate destructive actions |

## Detailed safety properties

### Recently Played
- All removal/clear actions call `LibraryMutationPlan` through the existing synchronous `LegacyLibraryTransaction` writer. This preserves the pre-edit recovery snapshot and never directly mutates JSON outside the coordination lane.
- `LibraryChange.RemoveRecent` requires a normalized exact video URL and updates only the `recent` list. `LibraryChange.ClearRecent` removes only the `recent` list. Favorites, playlist contents, offline audio and lyrics remain untouched.
- A confirmation appears **before** removing a recent entry or clearing all; actions are not disguised as clearing app music.
- Profile's existing **Undo last library edit** may restore the history change **only if no later library mutation occurred**; playing another song may legitimately create new history and make undo unavailable.
- The user can see the first six items or all available 100; the list remains lazy and bounded.

### Playlist item removal
- Only a real, named user playlist exposes **Remove from this playlist**.
- Liked Songs and generated smart collections retain ordinary song options. Removing a playlist song never unlikes it and never deletes any offline audio.

### Download card hiding
- `DownloadHistoryVisibility` persists only selected WorkManager UUID strings, capped at 200; no media bytes or URLs are written by the visibility helper.
- Only **finished** jobs can be hidden; active downloads cannot be accidentally dismissed via this path. A hidden job can be shown again from the Downloads screen. Hiding does not stop or delete WorkManager jobs and never removes a saved song.
- `SearchTrackSaveQueue`/NW35 Pause/Resume and actual library readiness checks are unchanged.

## Automated test source and QA
- Added pure Kotlin tests to `LibraryMutationPlanTest` verifying recent removal, clear and invalid entry rejection without favorite/playlist changes.
- Added Android instrumentation `DownloadHistoryVisibilityInstrumentedTest` using a disposable random UUID to exercise hide and restore without touching a real download.
- Reviewed NW36 source callbacks and file names; full Windows Gradle build, emulator and Samsung phone QA **NOT run** in GitHub source-edit environment.

## Device acceptance on a fresh, safely signed update
1. Run `build-windows.cmd` and `check-device-update.cmd` in a clean full NW36 source directory. Never uninstall or clear existing NightWave data to force signing continuity.
2. Open Home: latest played appears above discovery; tap ×; confirm, and verify song disappears from Home *but stays in Liked, custom playlist and Offline Music* where applicable.
3. Home Recently Played → See all; Clear history → Cancel, then Confirm with disposable history. Check Library Recently Played count and Profile shortcut.
4. Inside a named playlist use the song's three-dot menu → Remove from this playlist, confirm and verify another playlist, favorites and offline files are unchanged.
5. Visit Library → Recently Played → sort/filter, tap one song, remove another, clear list. Confirm safe empty-state and Back to Library.
6. Downloads → finished job → Hide from Downloads; Show dismissed → Restore. Verify Offline Library still plays the completed audio with Airplane mode enabled.
7. Offline Library: search is above quick actions, all 48dp buttons work and lists remain reachable on a 360dp screen and with Android font scale 1.3×/1.6×.
8. Search with an empty query offers Browse offline songs; with typed query, provider searches, suggestions and official YouTube action remain as before.
9. Re-test existing Now Playing mini/full overlay, gestures, queue, background playback, offline shuffle and Google Drive state; NW36 does not claim to fix Google error 8.

**Public release:** Unchanged blockers from NW35/NW34: restricted YouTube offline-content storage cannot be promoted without platform/recording permissions, Google OAuth status 8 still needs registration check, Google Play target API upgrade and signed release/AAB remain separate work. This is **not** a certified app release.
