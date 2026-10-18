# NightWave Phase 6V6B — Reviewed LRC import and safer replacement

## What changed
Extends the full-screen synchronized lyrics introduced in 6V6A. This update
focuses on **manual importing and replacing user-provided, timestamped LRC**,
not transcription or a licensed lyric catalog.

- Selecting an LRC now **opens a preview instead of writing immediately**.
  It shows the currently selected offline song, chosen filename, parsed cue
  count, last timestamp, a short sample, and up to five actionable warnings.
- Local filename and the optional `[ti:]` metadata are compared with the
  **currently selected imported audio song's title**. Names are normalized
  for punctuation/case and optional "official lyrics" suffixes. Mismatches
  are flagged. A plausible match is **advisory only**; no lyrics are searched,
  fetched, assigned to a different song or installed automatically.
- Diagnostics identify malformed or empty timestamp rows ignored by the
  existing parser, exact repeated cues collapsed, timestamps out of order
  that the parser sorts, a lyric end more than five seconds after a known
  song duration, and filenames without a normal `.lrc` suffix.
- Invalid UTF-8, empty/untimed files, more than 1,200 timed lines or files
  over 64 KiB are still **rejected** by the original `TimedLyricsCodec`.
  Error messages are selected from fixed categories rather than echoing
  document-provider exceptions, file paths or lyric text.
- An existing LRC is **never replaced merely by picking a file**. A dialog
  offers "Keep existing lyrics" or "Replace LRC", with the same explicit
  confirmation used for first-time imports.
- Import review keeps selected bytes only in memory until the dialog is
  canceled/confirmed. A previously saved lyric SHA-256 fingerprint is
  collected during preview and rechecked in `saveReviewed` before commit.
  When old lyrics changed in the meantime, replacement is declined.
  Song identity and the current file-picker request are also rechecked
  before accepting the imported file.
- Lyrics remain saved in the existing `nightwave_private_lrc_v1`
  preferences using the same normalized LRC representation. The existing
  `nightwave_private_lrc_offset_v1` offset settings are unchanged, as are
  optional cloud-lyrics backup and restore. No existing LRC or audio is
  automatically migrated, replaced, or deleted.
- The compact Now Playing lyrics panel now has an expandable **LRC quick
  guide** showing examples of time-coded lyrics and explaining the manual
  review process. Full-screen lyric sync and tap-to-seek remain available.

## No new backend or permissions
NightWave still has no dedicated backend server. This phase does not add
Google Drive permissions, remote lyrics APIs, automatic lyrics extraction,
MP3 download, copyrighted lyric scraping, new cloud storage, or upload tasks.
The Android document picker continues to require the user's explicit action.

## Regression tests
`LyricsImportReviewPolicyTest` includes tests for matching/conflicting
filename and `[ti:]` metadata, unrelated titles, malformed timestamps,
duplicates, out-of-order cues, known song duration, UTF-8 and input limits,
safe error messages, non-LRC filenames and control-character sanitation.
Existing `TimedLyricsCodecTest` and `LyricsSyncPolicyTest` remain enabled.

The build adds:
- `tools/verify_v2_phase6v6b.py --self-test` (CI source gate)
- `tools/check-phase6v6b-integration.ps1` (Windows source gate)
- Existing `testDebugUnitTest`, `assembleDebug`,
  `compileDebugAndroidTestKotlin`, APK signer and package identity checks

**Source checks do not prove an Android build passes.** CI and Windows must
actually execute their Gradle gates before release certification.

## Windows build and device acceptance
1. Extract the complete pinned 6V6B GitHub source ZIP into a fresh folder,
   then run `build-windows.cmd` from the project root.
2. Require **BUILD SUCCESSFUL**, all Gradle unit tests, Android-test source
   compilation, APK signature/identity checks and a current output APK.
3. Before updating an installed app, run `check-device-update.cmd`.
   Do **not** uninstall or clear data to bypass an incompatible signature.
4. On a disposable emulator or after all safe upgrade checks, play one
   personally owned imported audio file and select a matching UTF-8 LRC.
   Review cue counts, timestamps and the sample; confirm Import.
5. Choose another LRC for the same song. Cancel the Replace dialog and
   confirm the existing LRC is unchanged. Then preview and explicitly
   replace it; verify the new cues appear.
6. Use a conflicting `[ti:]` title and an unrelated LRC filename. Confirm
   a warning is displayed and selecting Cancel does not mutate lyrics.
7. Try out-of-order, duplicate and invalid timestamp rows; confirm counts
   and descriptions. Invalid UTF-8 and over-64 KiB inputs must fail safely.
8. Switch tracks after opening the file picker. NightWave must not attach
   the picked LRC to a different song. Reopen the app, verify existing LRC
   remains and per-song timing offsets are still correct.
9. Smoke-test full-screen lyric auto-follow, tap-to-seek, offset controls,
   previously imported offline audio, and existing optional cloud lyric
   backup/restore behavior.
10. On build failure, run `send-build-report.cmd` and share the redacted
    diagnostics; never install a previous/stale APK from `dist`.

## Known limitations
- Filename/metadata association is **not** audio-content matching. The user
  must decide whether the chosen lyrics correspond to the selected track.
- Imported LRC with some ignored invalid rows can still be valid if at least
  one cue parses. The preview discloses this, while the strict parser
  remains authoritative.
- Lyric replacement fingerprinting checks the existing local preferences
  before commit, but does not coordinate external file edits or transactions
  across separate processes. It does not replace the need for user review.
- Automatic real-time audio lyric extraction, licensed online providers,
  per-word karaoke, translations, and cross-device offset synchronization
  are outside the 6V6B phase.
- Full Android compilation, device UI testing and signed APK compatibility
  have **not** yet been verified for this development branch.
