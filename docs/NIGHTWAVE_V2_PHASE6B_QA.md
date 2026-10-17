# NightWave Phase 6B — Private synchronized lyrics + listening insights

**Branch:** `feature/nightwave-v2-phase6b-private-lyrics-listening-insights` (built on 6A2D). **Draft source; NOT an Android-verified release.** Stable main and existing user-installed app data remain unchanged.

## What 6B adds

### Lyrics — only from your own selected LRC file
- Open an **imported offline audio track** in NightWave's full Now Playing screen, expand **Lyrics**, then tap **Import synchronized lyrics** and select a `.lrc` text file through Android's Documents picker.
- Supports conventional `[mm:ss]`, `[mm:ss.xx]`, `[mm:ss.xxx]`, multiple timestamps per line and UTF-8 optional BOM. The active line is highlighted using Media3's **current track position**. Six nearby lyric lines are displayed, not a resource-heavy 1200-line scroll.
- File size capped at 64 KiB, timed cues capped at 1200, each lyric line bounded to 220 characters, malformed UTF-8 rejected, invalid/no timestamp files rejected, duplicate timestamps/text pairs deduplicated, cues ordered by time.
- Associate each import with the exact user-selected **offline audio ID**. If the track changes while the picker is open, refuse the import rather than attach lyrics to the wrong track. An offline ID must already exist in NightWave's local-audio library. No arbitrary file paths or URLs are accepted as targets.
- Store a normalized copy in app-private `nightwave_private_lrc_v1` SharedPreferences. Files are not copied to public storage or sent to servers. **Replace .lrc** and **Remove lyrics** act only on the current imported offline track.
- The lyrics preference is intentionally **not part of 5U Google Drive backups** or 5Z cloud merges, and is not included in the existing v1 manual guest-library format.
- Public YouTube-based videos display an honest unavailable message. **No automatic scraping of lyrics, no promise of globally synced lyrics, and no licensed lyrics service is integrated.**

### You — personal listening snapshot
- Shows genuine counts from NightWave's **existing bounded recent-song list**: distinct recent tracks, represented artists, liked-song count and saved-playlist count. Lists up to three creators represented among distinct recent tracks.
- These are not song play counts, listening minutes, time-of-day patterns, weekly/yearly totals, or Spotify-style historical trends. The existing recent list deduplicates songs, so those claims cannot be inferred and are not displayed.
- No new background telemetry, microphone permissions, API keys, trackers, uploads or recording service was introduced.

### Preserved
Phase 6A2D compact Home/Search, 6A2 mini-player X and swipe-down hide, 5Y non-destructive Spotlight resume, offline-audio file ownership, playlists and saved histories, Phase 5U Drive backups and 5Z safe manual merge, Media3 ownership of playback and existing package/signing identity.

## Deferred verification and release checklist

1. Run `tools/verify_v2_phase6b.py` plus inherited source checks; execute `TimedLyricsCodecTest` (10 cases) and `ListeningInsightsTest` (5 cases) via `build-windows.cmd`, plus `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`. **None has run in this phase.**
2. Test user-imported MP3, FLAC and M4A offline audio with a genuinely user-supplied valid LRC, including UTF-8 BOM and multiple timestamps. Verify highlighting aligns with seek/play/pause/time updates without stopping audio.
3. Test a malformed LRC, overly large file, invalid timestamp, zero-length document, cancellation, unsupported encoding, an unreadable URI, duplicate import, song deletion and playlist navigation while picker is open. The existing track/library must remain intact.
4. Confirm LRC import/remove are available only for an imported local audio track; for a public YouTube/video track show the explanation and do not contact lyric/translation services.
5. Check Now Playing on 320dp/360dp/412dp devices, tablet and landscape, with font scale 1.0–1.8 and reduced motion: lyrics panel, progress slider, Next/Previous, queue, background playback, mini X and Up Next remain reachable. The dynamic lyric view should not jump the page uncontrollably.
6. Populate recent lists with empty, repeated and mixed artists, test artist variety, liked/playlist counts, and ensure the UI never claims total plays or listening hours.
7. Verify full regression of 6A2D search, 5Y mixes, 6A2 mini hide/restore, Home spotlight, 5U backup, 5Z merge and privacy boundaries. Do not use a personal Google account for destructive sync testing.
8. Before any in-place Android update, check current installed APK package/signing certificate via `check-device-update.cmd`. Never uninstall or clear saved data to force installation.

## Deferred future upgrades
- Optional explicit opt-in **play-session event logging** with timestamp/duration, local data-retention control, migration and privacy review would be required for real listening minutes, play counts and date-aware analytics.
- Licensed lyrics-provider integration or strictly authorized metadata retrieval is a separate legal and product decision. The 6B importer does not implement it.

**Status:** Source implementation + future regression tests authored on a draft branch. No successful Android compilation, instrumentation run, device screenshot audit, signer comparison, Google OAuth verification or public-release readiness is claimed.
