# NightWave Phase 5Y — On-device personalized mixes & Resume protection

## What changed

- **Made for your listening** on Home: daily rotation, recent replay, rediscovery, and artist-affinity choices, limited to three nonduplicate mixes. Every suggested track must already exist in the device-local favorites, recent public tracks, or playlists; no fabricated audio or external AI/music service.
- Deterministic day-aware ordering and transparent reason text; mix selection **expands actual songs** before playback. Tapping a mix card does not interrupt the currently playing song.
- Liked and recent sources are capped; playlist lists are read without network and deduplicated, with up to 12 tracks per mix and up to 6 initially displayed. Empty personal libraries don't display pretend playlists.
- **Home "Pick up where you left off" fix:** previous code always replaced queue and re-resolved YouTube when clicked, even if the displayed last-played song was currently loaded. This could stop playback and wait for a slow extractor.
- New `SpotlightResumePolicy` chooses KEEP_CURRENT for a playing/buffering same song; RESUME_CURRENT for a paused/loaded one; RETRY_CURRENT for known error; START_SELECTED if different/offline/unavailable.
- Media3 bridge now has **resume-only** (not toggle) to prevent inadvertently pausing an already-playing controller. An already-ended, still-loaded media item seeks to the beginning and prepares locally before playing. Playback service suppresses duplicate identical queue-play and duplicate RESUME_SAVED while an extractor request is already pending.
- Playlist-only edits invalidate the local recommendation/search cache via the UI library revision; unchanged playlist names no longer leave old mix contents displayed.
- No new permissions, schema changes, cloud exports, new radio streaming APIs, database deletion, or changes to music ownership. The 5U optional Google Drive backup remains distinct.

## Limits

A brand-new public-video audio request still depends on third-party YouTube extraction and may take a long time or be unplayable. This patch avoids needless extraction for the **same queued track**, but cannot guarantee upstream speed or license external catalog audio. "Daily" means a deterministic order drawn from locally known music, **not** Spotify's full catalog recommendations. Imported private audio is not included in public-video mixes or backed up to Google.

## Required checks

1. Run `tools/verify_v2_phase5y.py` plus the earlier source gates; do not treat these as Android compilation.
2. Execute JUnit classes `PersonalMixEngineTest` and `SpotlightResumePolicyTest`. Verify same-day reproducibility, no fabricated tracks, empty profiles, duplicate filtering, replay chronology, and bounded output.
3. With an **active track** displayed as the last played spotlight, tap card many times: no pause, seek reset, queue replacement, or fresh network extraction should occur.
4. **Paused** same track: card only resumes the existing media item/queue position. **Buffering** same track: tapping must not queue redundant extractor requests. **Failed** same track: one explicit retry; **different** last track: selected track starts intentionally.
5. Rapid duplicate queue requests via service: same queue/selection must not drop an already loaded stream. Switching to genuinely different queue must still work; wrong queue identity must not be silently ignored.
6. Test Home with no recent/likes, one track, many liked/playlist tracks, duplicates, and malformed URLs. The selected mix reveals real titles, and only a song tap begins playback.
7. Confirm imported offline audio, background playback, mini-player, queue swipes/Undo, four tab pager, official-first search, manual backup, cloud controls, and rotation/launch remain unaffected.
8. Run `build-windows.cmd` to check JVM tests, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin` before APK identity/signature checks. The previously uploaded failed-build log was 5S instrumentation imports; current branch inherits corrected 5S sources.
9. On device use `check-device-update.cmd` before install-over-existing. **Never uninstall or clear saved data to bypass signing mismatch.** Capture private crash diagnostic for any remaining stall (without sharing account/token data).

## Current status

GitHub source delivered, JUnit added, source checks pending actual execution. Android Gradle compilation, device time-to-first-audio and cloud E2E verification are still unverified. Do not merge to stable main or claim a working released APK.
