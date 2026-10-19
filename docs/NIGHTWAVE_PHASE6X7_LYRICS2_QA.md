# NightWave Phase 6X7 — Lyrics 2.0: Follow, Timing and Editor Precision

## Base / preservation
Built on `feature/nightwave-v2-phase6x6-private-daily-mixes`, carrying the previous EQ / Sleep Timer dial / responsive Repeat / gapless / crossfade / offline normalization / offline repeat-shuffle / smart folders / private Daily Mixes work.

All changes are local display, timing policy, and draft-only editor refinements. Existing `nightwave_private_lrc_v1` LRC files, lyrics fingerprint checks, per-song offset storage, offline audio, music library, user account, Google Drive cloud backups, app ID, and signing remain intact.

## User features
1. **Smart lyric follow**: Imported offline LRC full-screen lyrics scroll with playback automatically until the listener intentionally drags the lyric list. Manual drag pauses auto-follow so the page stops fighting the listener. Tap **Follow** to return to synchronized scrolling. Follow resets to On for a different song; programmatic scrolling does not register as a user drag.
2. **Line-progress indicator**: A small bar beneath the active cue shows fractional progress toward the next distinct line's timestamp. This is line-level guidance, **not** word-by-word/karaoke timing and cannot infer missing timestamps.
3. **Fine offset calibration**: Existing ±0.25-second global timing buttons remain the default. Toggle **Fine timing · 50 ms** to adjust per-song playback/lyric offset in 50 ms steps. The ±10-second existing safety bound remains. The existing private offset store accepts 50 ms increments and does not rewrite the imported LRC.
4. **Fine per-cue editing**: The existing full-screen LRC editor gains a 250ms/50ms nudge toggle. Every change remains in the unsaved draft, honors existing undo, timestamp sorting and 64KB/1200-line limits, and only the explicit **Save changes** action writes source LRC through existing fingerprint-checked storage.
5. **Accessibility, motion and layout**: Full-screen lyrics keep existing readable current-line treatment, tap-to-seek and separate Edit button. Reduced-motion users receive direct scroll instead of animation. The Follow and Fine Timing controls have stable UI tags.

## Technical limits
- This phase operates on **user-imported synchronized offline .lrc**. No live extraction from online music, copyrighted lyric scraping, Google API or app-owned server is introduced.
- Existing export/import/review, draft-only undo/discard, and cloud lyric backup/restore schemas remain unchanged.
- The progress bar is interpolated from authored line timestamps and the current Media3 position. Accuracy is limited by timestamps, playback polling cadence and device audio latency.
- A final cue has no defined end timestamp, so the progress bar is hidden unless a later distinct authored timestamp exists. Empty/before-first cue has no active bar.

## Verification performed
- Pure policy `LyricsPresentationPolicy.kt` added, with **8** JUnit source cases for context anchoring, cue progress, timestamps, offsets, duplicate timestamp handling and extreme clock values.
- **3** existing `LyricsSyncPolicyTest` cases added for 50ms validation, seek/highlight agreement and safety clamping.
- **2** existing `LyricsEditorPolicyTest` cases added for draft-only 50ms nudge, strict LRC reserialization and invalid steps.
- Added `tools/verify_v2_phase6x7.py` to Android CI source gates while retaining the prior 6V6A–6V6G lyrics integration checks.
- Targeted GitHub source checks only until Gradle run confirms. No physical Android playback or device font/accessibility QA has been claimed.
- On branch push, CI is configured to run JVM unit tests, Android debug assembly and Android test-source compile. Results need to be inspected before installing a build.

## Deferred phone acceptance plan
1. Before updating, **do not uninstall or clear NightWave app data**. Require `build-windows.cmd` BUILD SUCCESS and matching installed APK signing identity.
2. Open an imported MP3 with a valid timed LRC, then Now Playing > full-screen lyrics. Confirm playback follows highlighted lines with no premature line changes.
3. Drag to browse older lyrics while the song plays. The list must stay where you browsed; now tap **Follow** and verify the active line returns smoothly. With Android Reduce Motion, it should return without animation.
4. Tap a future lyric line. Confirm actual seek matches the cue timestamp plus the selected per-song offset, clamped to track duration.
5. Observe the progress line between two authored timestamps. Its bar should grow toward the next line; it should not pretend to highlight individual words.
6. Toggle Fine timing, use +0.05s and −0.05s; verify offset label precision and clamping at ±10s. Switch back to regular ±0.25s. Relaunch ordinarily and verify the offset persists for only that imported track.
7. Open Edit LRC; switch to 50ms nudge mode, nudge one cue, Undo, then try Save/Discard separately. Confirm previous LRC remains intact unless explicitly saved and imported audio playback never stops.
8. Test duplicate-timestamp cues, first/last lyrics, 1,200-line files, screen lock/resume and song switching. Check no crash, navigation issue, lyric mix-up or excessive battery use.
9. Verify 6X6 Daily Mixes, 6X5 folders, 6X4 repeat, 6X3 normalization, 6X2 crossfade, 6X1 gapless and all earlier features.
10. On a narrow mobile screen and increased Android font scale, check the Follow/Edit row and Fine controls don't clip.
