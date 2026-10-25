# NW37 — Search & Song-Action Micro-Polish

**Status:** Committed source patch. Full Android Gradle build and on-device validation **not verified**.  
**Base:** NW36 `feature/nightwave-nw36-navigation-history-polish`.  
**Date:** 25 October 2026.

## Scope

- **Search history:** Up to 12 submitted searches, newest first. Blank/control-character inputs are rejected, spacing is normalized, and case-insensitive duplicates move to the top. Search history is **on this device only**, in `nightwave_search_history_v1` SharedPreferences. It is not added to Google Drive, saved playlist content, listening history or any remote analytics.
- **Search UI:** When Search is empty, show prior submissions above discovery. Tap a query to run it; tap × to remove one entry; use the inline Undo action for the most recent removal. Clear all uses an explicit confirmation. Undo refuses to overwrite newer history.
- **Explicit searches only:** Entering text and accepting a local suggestion do not write search history. Actual searches through NightWave, official YouTube, mood shortcuts and saved creator shortcuts do.
- **Song actions:** Existing Like, Save to Playlist, Add to Queue, Play Now and Save Offline behavior is unchanged. Added **Share song link** (Android Sharesheet) and **Copy song link** (Android clipboard). Both require a normalized, public video URL and handle unavailable services without crashing.
- **Search accessibility:** Advanced Search, Creator Back, Creator YouTube Search and browse creators buttons use 48dp minimum touch height.
- Existing NW35 download controls and NW36 playlist/history/offline layout remain on the branch unchanged.

## Non-destructive design

No mutations to `MusicLibrary`, offline indexes, audio bytes, active jobs, playback service, cloud authorization, signing, manifest package or installed app data. Removing a search never removes a song or a listening-history entry. Search-history `clear` does not clear recently played music.

The default GitHub `main` branch remains untouched.

## Checks

- **Standalone Kotlin compiler smoke:** SearchHistoryPolicy compiled with local `kotlinc`; six direct assertions passed (blank/control character, case-insensitive dedupe, ordering, capacity, removal and max length). This is **not** an Android app build.
- **JVM test source:** `SearchHistoryPolicyTest.kt` adds unit coverage for normalization, repeated searches, bounded history, invalid inputs and targeted removal.
- **GitHub Actions:** Push-triggered NW37 verification was enabled. Initial run [40172722981](https://github.com/Nahian2002/NightWave/actions/runs/40172722981) failed **before runner allocation** (runner ID 0, no steps, no logs/artifacts). It does **not** establish a Kotlin or Gradle compile result.

## Build and device verification still required

1. Build the full NW37 branch (not default `main`) with the documented Windows build procedure and review build logs/tests. Avoid installing a differently signed APK over the existing personal library.
2. Search two different songs in NightWave and one through YouTube; verify three entries in Recent searches and no typed-only entry.
3. Search the same name with different case; verify one entry rises to the top. Restart NightWave and verify persistence.
4. Remove the middle history entry, choose Undo and verify its restoration without changing Recently Played or Liked Songs. Clear All → Cancel; then confirm.
5. Share and copy a public song URL; verify no local file paths, tokens, or private metadata are exposed.
6. Exercise Search and creator buttons at 360dp screen width, 1.3× and 1.6× font scales, then rotate.
7. Regression: Home/Library/Recently Played removal, audio playback with screen off, Downloads pause/resume, offline play in airplane mode, and Google Drive account state. **This update does not claim to fix OAuth error 8.**

**Release status:** Development-only until full compile/device testing and previously documented public-release blockers are resolved.
