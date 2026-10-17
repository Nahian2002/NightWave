# NightWave 2.0 — Phase 4 premium player and session recovery

Version: **2.0.0-alpha4 / code 14**
Branch: \`feature/nightwave-v2-phase4-premium-session\`
Status: Source implementation committed. **Full Android Gradle build, Compose rendering, Media3 stream restoration and real-device QA NOT yet executed here.**

## What changed

### Reopen previous song — bug fix
- The MediaSessionService restores the previously saved public YouTube queue on creation.
- It resolves the previous song's fresh stream and prepares the player **paused**, without unexpectedly playing audio.
- A new bookmark stores the last public video URL, playback position and known duration. No signed/expiring audio stream URL is saved.
- While streaming, the service persists progress about every 2.5 seconds and again on service destruction. A completed track near its end restarts from 0 on reopen.
- The old player and new Compose UI can show the last song title, artwork, creator, queue and approximate position even before new stream resolution completes.
- Pressing Play when Media3 has no current media item sends an explicit resume request to the service, resolving the saved video again if necessary.
- Last-session restoration requires an accessible network/public video; deleted, geo-blocked or unavailable streams cannot be restored to playable audio.

### Premium visual update
- **NightWave** branding is consistent in the new Compose header; dark slate/mint Midnight palette, violet Aurora and tailored Pearl/AMOLED choices.
- New Compose Home/Search/Library/Profile becomes the launcher in this alpha branch. The legacy player still exists; the top-right Settings icon opens the classic controls.
- Reworked typography, concise copy and music-list rows with true saved thumbnails; made hero cards 164dp, mood tiles 112dp and track artwork 52dp.
- Compact navigation: four items in a 60dp bar with restrained selected states, instead of the oversized old Home/Search/Library strip.
- Compact Now Playing mini-player: 58dp content plus a thin progress indicator. Swipe **down** to hide, **up** to expand. The compact "Now playing · Tap to show" control remains available above the bottom navigation, so playback is never stopped by hiding the card.
- Expanded player has capped 330dp artwork, tighter spacings, full queue, seeking, favorite, shuffle and repeat. Swipe down from the top header to minimize; Android Back first closes the queue or player.
- Advanced Room/DataStore storage checks are behind an Advanced toggle in Profile, not crowding everyday controls.
- Existing backup/export/playlist operations and the unverified YouTube web-search flow remain available. This phase does **not** claim that upstream YouTube search is fixed.

## Local build without GitHub billing
1. Download the Phase 4 ZIP branch and extract it to a **new** folder.
2. Run \`build-windows.cmd\`. The builder checks PowerShell syntax, runs JVM tests and creates \`dist\NightWave-debug.apk\` if compilation succeeds.
3. If the builder fails, upload \`dist\NightWave-build-errors.txt\` and the first relevant lines of the full log.
4. Do not uninstall NightWave or clear its data. Use an external guest backup before testing any import/restore functionality.
5. For instrumented tests, use a **disposable emulator/test device**, never the phone's personal library: some pre-existing storage tests clear SharedPreferences.

## Phone acceptance matrix
- **Launch:** New NightWave Compose Home loads by default; classic player remains reachable through the top-right Settings icon. Android intent share-to-NightWave should still reach classic import.
- **Last session:** Play a song, seek to 1:30, wait 5 seconds, exit the interface, reopen and verify song/title/art, progress near 1:30, queue, and paused audio. Press Play and confirm it resumes close to the bookmark; audio must NOT begin automatically on app launch.
- **Process death:** On disposable data, close UI/process, reopen, confirm persisted bookmark; repeat with Wi-Fi briefly unavailable, then reconnect and retry playback.
- **Finished track:** Play to the end, relaunch and ensure it does not reopen on the final second.
- **Mini-player:** Verify a roughly 60dp bar, progress display, tap to expand, swipe down to hide and "Now playing" control to restore. Switch tabs; player should not cover Library buttons.
- **Navigation:** All four tabs usable on the S24 Ultra; no text clipped or oversized tabs. Use Android Back from full player and queue. Verify selected state, visual contrast and text scaling.
- **Now Playing:** Seekbar, shuffle, repeat, next/previous, queued tracks, art, title and background audio. Swipe down on header to minimize.
- **Themes:** Test Midnight, Aurora, Pearl and AMOLED; close/reopen and verify persisted choice.
- **Library:** Like, save, create/rename/delete playlists, recent tracks, export guest backup. No changes should wipe preexisting library. Room remains shadow-only.
- **Headphones/background:** Lock screen, Media3 notification controls, Bluetooth play/pause/skip, Wi-Fi/mobile switching, call interruptions and 15+ minutes screen-off playback.
- **Search:** Search Saiyara, Baby Shark and a new term; if the web-browser search fails, report it separately. This release targets playback restoration and visual quality, not a rewritten YouTube search provider.

## Tests added and honest limitations
- Five Android instrumented tests for saved queue bookmarks, wrong-track isolation, end-of-song position reset, signed URL exclusion and value clamping.
- Source verification script: \`python tools/verify_v2_phase4.py\`.
- The previous unit-test suite still runs through \`build-windows.cmd\`.
- **No Android Gradle/Compose build or hardware tests have run in this environment.** This work is a draft until your next successful Windows build and visual playback checks.
- A signed-in Google account and cloud recovery are NOT included; keep local backups secure.

## Release gate
Do not merge Alpha 4 to \`main\` until previous storage migration tests and Phase 4 UI/playback tests pass. Keep the last stable v0.8 installer available and do not uninstall/clear data during in-place upgrades.
