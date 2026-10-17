# Phase 5N — Compact premium You screen

**NightWave 2.0.0-alpha5n / build 30** · Branch: `feature/nightwave-v2-phase5n-compact-profile`  
**Status:** Source implemented, draft PR pending verified Android build.

## What was improved

- Replaced tall isolated buttons and large empty gaps with five compact grouped
  surfaces: a small editable profile card, **Appearance & playback**,
  **Library & backup**, **App health**, and **Interface**.
- The profile avatar is now 52dp and the display name is condensed to one line.
  Edit name opens an inline field and an explicit Save action; empty names
  are blocked.
- Themes and avatar styles are grouped under **Theme → Change**. Theme and
  avatar selections still use the original profile save pipeline. Reduced
  motion remains a one-row switch.
- Backup Export, Import and protected Undo are retained under an expandable
  **Manage backups** row. Import still requires its pre-existing confirmation,
  and exported backups do not contain imported audio bytes.
- Room/DataStore mirrors and read parity controls are now tucked into
  **Advanced storage → Details**. They remain optional previews; legacy
  music library writes stay authoritative.
- Crash diagnostics and the classic UI fallback remain easy to find.
- No changes to Media3, music playback, user files, imported songs, bookmarks,
  account storage, signing keys, launcher routes or manifest permissions.

## Gesture roadmap — staged for safe QA

**5O · Horizontal tab navigation:** Swipe left/right between Home, Search,
Library and You, with edge/velocity thresholds and haptic/visual selection.
Use a real pager coordinated with bottom navigation, preserve the current tab
on rotation, do not steal vertical list scrolling, and respect reduced motion.

**5P · Song and player gestures:** Swipe the mini-player upward to open full
Now Playing and downward to minimize/hide; downward dismiss on full-screen
player is already supported and should remain. In **Up Next**, swiping a
temporary queue entry sideways should offer **Remove from queue + Undo**, but
never delete original saved songs, downloaded/imported files or playlists.
Use an explicit secondary action if a protected/current track cannot be removed
safely. Artwork left/right skips must remain separate from pager swipes.

**5Q · Discovery/search recovery:** Address the actual **“Search needs a
different route”** failure seen on the phone; restore useful native results
when an upstream provider responds and keep reliable clear browser fallback
when public search is unavailable. Do not advertise unsupported music access.

**5R · Motion, accessibility and stability:** Tune interaction feedback,
transitions, swipe cancellation and touch accessibility for font scaling,
rotation, accessibility services and reduced-motion mode; require a real APK,
emulator smoke checks, in-place device update and crash/playback QA.

## Validation and limitations

- Added source-only gate `tools/verify_v2_phase5n.py` checking the new UI
  structure and existing data actions.
- Added `NightWaveProfileCompactInstrumentedTest.kt` for premium You
  navigation, default compact groups and appearance expansion on an
  emulator/disposable Android profile.
- The historical Room/backup regression assertions now refer to the same
  live actions in their new compact rows.
- **Android Gradle compilation and UI tests have not been executed** here;
  the environment lacks Android SDK/Gradle. Kotlin/Compose layout, typography,
  dark/light theme rendering and tap/swipe behavior must be validated on an
  actual device before marking the release stable.

## Windows acceptance

Extract the branch ZIP to a new folder and run `build-windows.cmd`.
Require SUCCESS, alpha5n, build 30, correct signature and SHA-256. Then use
`check-device-update.cmd` against the installed phone before an in-place
install. Do not uninstall or clear data to bypass a signing mismatch.
Check the premium You tab at default, 1.3× and 1.5× font scale; test
both AMOLED/dark and Pearl/light, avatar/theme saving, Undo, backup export,
backup restore confirmation, diagnostics, offline resume, and classic fallback.
