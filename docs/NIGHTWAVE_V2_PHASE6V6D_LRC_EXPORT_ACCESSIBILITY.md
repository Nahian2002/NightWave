# NightWave Phase 6V6D — LRC export, accessible editing, rotation recovery

## Scope

Extends 6V6C on-device lyrics editing, 6V6B reviewed LRC import and
6V6A synchronized display. It does **not** introduce a NightWave server,
a streaming lyrics catalog, audio transcription, new Google permissions,
unlicensed MP3 downloading, or an automatic background upload.

## Implemented

### Export a standard LRC file

- A full-width button in the lyrics editor uses Android's
  `ActivityResultContracts.CreateDocument("text/plain")` so the **user picks**
  the destination and filename. The app creates no hidden export copies,
  requests no broad storage permission and performs no cloud upload.
- Export can use the **current unsaved draft**, letting users keep a corrected
  `.lrc` separately even before pressing **Save changes** in NightWave.
  The button explicitly calls it **Export unsaved draft .lrc** when applicable.
- `LyricsExportPolicy.prepare` serializes using the existing strict
  `LyricsEditorPolicy`, parses its output with the canonical LRC codec and
  compares all cues. No silent line drop, cue reorder discrepancy or 64 KiB
  overflow is permitted. The suggested title is bounded to 64 characters
  and sanitized against path separators, control characters and invalid
  filename punctuation; `.lrc` isn't duplicated regardless of capitalization.
- A pending export holds at most a validated 64 KiB byte array in memory,
  scoped to the editor request and offline track. If the song or session
  changes, the callback refuses to write a different track's lyrics.
  A canceled picker doesn't write anything. A provider error reports
  possible partial output without claiming the export succeeded.
- **Exporting does not save the draft into NightWave**, overwrite the
  imported private LRC, touch MP3 files, or change optional cloud backups.

### Editor navigation and accessibility

- Lyric rows expose line number, selected state, timestamp and text to
  accessibility services.
- The main per-row Edit, Delete and ±0.25-second actions have at least
  48 dp high touch targets.
- `LyricsEditorNavigationPolicy` centralizes Next/Previous, playing-line
  navigation, offset-aware active cue selection, index clamping and focus
  recovery after moving a timestamp. Huge navigation deltas do not overflow.
- When a corrected timestamp moves a cue to a different sort position,
  the corrected cue remains selected. Undo/deletion cannot leave the
  selection outside the visible cue list.
- Existing reduced-motion behavior remains: jumping to a row scrolls
  without animation when reduced motion is enabled.

### Non-persistent rotation recovery

- `LyricsEditorRetainedState` is an Android `ViewModel` that holds the
  unsaved editor session, request epoch and pending document export across
  **configuration changes such as screen rotation**.
- It does **not** write drafts to disk or synchronize them to Drive.
  System process termination may still lose a draft; saved LRC remains
  unchanged. The Activity waits through a transient blank playback state
  during reconnection instead of discarding the draft immediately.
- If the player actually switches to a different track, the old draft is
  discarded rather than being attached to a new song, and the user receives
  a message if unsaved changes were lost.
- The original strict LRC save still requires the user to press Save and
  passes the fingerprint guard from 6V6B. Cloud lyric backup and playback
  timing-offset formats remain unchanged.

## Regression suites

- 10 JVM `LyricsExportPolicyTest` methods covering saved vs draft output,
  filename sanitization, Unicode names, timestamp fidelity, size limits,
  parser validation and input immutability.
- 10 JVM `LyricsEditorNavigationPolicyTest` methods covering selection
  boundaries, extreme step values, current-line follow, offset handling,
  cue reordering and missing selection targets.
- 2 isolated Android instrumentation tests under
  `LyricsEditorRetainedStateInstrumentedTest` create a disposable
  `ViewModelStore` to confirm retained drafts and explicit clearing.
  They never open or modify user LRC or music.
- Existing 6V6A–6V6C lyrics tests and all earlier build phases stay in
  the Windows and CI pipelines.
- `tools/verify_v2_phase6v6d.py` (CI; self-tested source contract) and
  `tools/check-phase6v6d-integration.ps1` (Windows source preflight)
  are additive checks. Source checks do not replace the Kotlin compiler,
  emulator or runtime safety validation.

## Windows build and phone acceptance

1. Download the **full, pinned source ZIP**. Extract to a fresh short path.
2. Run `build-windows.cmd`. Require all source preflights, Gradle
   `testDebugUnitTest`, `assembleDebug`,
   `compileDebugAndroidTestKotlin`, package and signature checks.
3. Use `check-device-update.cmd` **before** updating an installed app.
   Never uninstall or clear current music data to bypass signer mismatches.
4. Play a personally owned offline track with imported synchronized LRC.
   Open the editor, change a timestamp and line text, then tap
   **Export unsaved draft .lrc**. Pick a new file location.
5. Verify the resulting document re-imports with corrected timestamps.
   Reopen the editor and confirm that **saved private lyrics are unchanged**
   until pressing **Save changes**.
6. Test Cancel in the file picker and a provider failure. No success should
   be claimed; previous private lyrics must remain intact.
7. Rotate the device with unsaved edits; verify the edited lines and undo
   stack survive ordinary configuration change. The editing state must
   not appear on another song.
8. With TalkBack, verify lyric row index, timestamp, selected state, and
   accessible edit/nudge/delete controls. Check small mobile layouts and
   reduced-motion setting.
9. Regress 6V6B import/replacement, 6V6A synced playback, 6V6C Save/Undo,
   6V5C cloud browsing, older transfer history and offline music.
10. On failure use `send-build-report.cmd` and share redacted diagnostics.
    Never install an older APK mistaken for the updated build.

## Limitations and verification state

Export depends on the Android document provider chosen by the user. Some
providers may alter the suggested extension or produce an incomplete file
on I/O failure; NightWave does not promise atomic external file creation.
Only plain text synchronized LRC is exported, not lyrics audio or per-word
karaoke timing. Rotation retains drafts, but a process kill does not.

Full Android Gradle compilation, executed JUnit and instrumentation tests,
Compose rendering, SAF provider operations, and in-place APK compatibility
require a successful Windows/CI build and device test. **This development
branch is not yet a certified APK.**
