# NightWave Phase 6V6C — On-device synchronized lyrics editor

## Foundation
This branch extends **6V6B** reviewed LRC import and **6V6A** synchronized
lyric presentation, plus prior Google Drive Music Vault and reliability
phases. The app remains serverless. No additional accounts, OAuth scopes,
cloud service, online lyric catalog, voice transcription or music download
capability was added.

## Implemented
- **Edit existing user-imported offline LRC inside NightWave**. The entry is
  available from the compact Now Playing Lyrics area and the immersive
  synchronized lyrics view. Public streaming tracks are excluded.
- A full-screen, scrollable native Compose editor shows current lyric text,
  `mm:ss.SSS` timecodes, current-playing line and previous/next navigation.
  Jump to the current playing line is explicit so editing is not repeatedly
  interrupted by automatic scrolling.
- Tap a line to select it. **Edit** opens a two-field dialog to correct the
  lyric's text and precise timestamp. **±0.25s** nudges adjust one line.
  **Add at playhead** creates a draft placeholder at the current position.
  **Delete** asks before removing a line from the draft.
- **Undo** maintains at most the last 15 changes. Saved music and the existing
  `nightwave_private_lrc_v1` record are not modified by any intermediate
  editing operation. A changed line is kept only in Activity memory.
- **Discard** asks for confirmation when the draft contains changes; leaving
  without saving does not touch persisted lyrics.
- **Save changes** is disabled for a pristine draft. On an explicit Save,
  the editor validates time bounds, cue count, UTF-8 normalized size,
  non-empty lyric text and precise parser round-trip. It then uses the
  existing `PrivateLyricsStore.saveReviewed()` fingerprint gate to ensure
  the record hasn't changed since the editor opened.
- The editor session is scoped to a single offline song and monotonic
  request token; stale open/save callbacks and song switches cannot silently
  attach changes to the new song. If another action changed the underlying
  saved LRC, Save fails and leaves the user's unsaved draft visible.
- No changes were made to saved audio bytes, existing per-song lyric timing
  offset preferences, cloud lyric backup/restore payloads or previous LRCs
  during this source upgrade.

## Validation behavior and limits
- Every intermediate draft is checked by the **existing production LRC
  parser** after serialization to ensure it reads back to exactly the
  intended sorted cue list. Ambiguous embedded timestamp syntax is rejected
  rather than silently producing extra cues.
- Accepted `mm:ss.SSS` time range is **00:00.000 through 99:59.999**.
  A maximum of **1,200 cues** and **64 KiB** remains enforced.
  An edited text line must be 1–220 characters; overly long lines are
  rejected rather than truncated.
- Nudge arithmetic uses `Long` and clamps to safe time boundaries.
  Duplicate timestamp-and-text pairs are rejected; different text at the
  same timestamp remains possible under the existing parser.
- When a timestamp changes, cues are reordered chronologically. Selections
  stay usable, but the moved row may appear at a new position.

## Tests and build gates
- New `LyricsEditorPolicyTest` covers baseline preservation, quarter-second
  nudges, exact time parsing, boundary arithmetic, row reordering, add/delete,
  no silent Unicode truncation, ambiguous embedded timecodes, parser
  round-trips, duplicate protection and the 64 KiB cap.
- `tools/verify_v2_phase6v6c.py --self-test` is a read-only source audit
  with synthetic checks for missing fingerprint saves and parser validation.
- `tools/check-phase6v6c-integration.ps1` is the Python-free Windows
  source preflight. Both are registered alongside existing 6V6A/B gates.
- The original Windows pipeline must still pass
  `testDebugUnitTest`, `assembleDebug`,
  `compileDebugAndroidTestKotlin`, and APK identity/signature checks.
- **No APK is certified by source contract checks alone.** Full Compose
  compilation, unit-test execution and emulator/device smoke checks are
  independent release requirements.

## User/device smoke test after successful build
1. Extract the pinned **complete ZIP** into a new directory and run
   `build-windows.cmd`. Stop on any failure, then send the redacted build
   diagnostic. Do not install an old APK from `dist`.
2. Run `check-device-update.cmd` and require package/signature compatibility
   before installing over the current NightWave on a personal phone.
3. Play a personally owned imported offline MP3 with an explicitly imported
   UTF-8 `.lrc`. Open Lyrics and select **Edit lyric lines & timestamps**.
4. Edit a line, nudge its time, add one near the playhead, and delete an
   unneeded line. Check the playing-line navigation and chronology.
5. Tap **Discard**; confirm all previously saved lyrics are unchanged.
6. Reopen, make changes, tap **Undo**, then **Save changes**. Restart and
   confirm edited cues persist with synchronized playback.
7. On a disposable emulator, concurrently alter the saved LRC after opening
   the editor and before Save. Save must refuse to replace newer lyrics.
8. Try invalid timecodes, empty text, >220-character lines, duplicate cues,
   and near-limit LRC files. The draft must be rejected without deleting
   private music or the previously saved lyrics.
9. Switch songs while editing. Unsaved edits must not attach to the newly
   selected song. Verify existing lyrics offset controls and import/replace
   previews still work.
10. Smoke-test cloud lyric backup, offline file-health, queue restore,
    Google Drive vault transfers and previous-song restoration for regressions.

## Known limitations
- The editor is on-device only and depends on an existing user-imported
  synchronized LRC. It does not transcribe audio, download a licensed lyrics
  catalog or align words using machine learning.
- Unsaved drafts are not persisted across Android Activity/process
  recreation; previously saved lyrics remain untouched.
- A successful Save overwrites that song's *previous saved LRC* intentionally;
  undo is for unsaved changes, not a historical version archive.
- Full Kotlin/Android compilation, executed JVM tests, Compose device
  rendering and update signing remain unverified until a build actually runs.
