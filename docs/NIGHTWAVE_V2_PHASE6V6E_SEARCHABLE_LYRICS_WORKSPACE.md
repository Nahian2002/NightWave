# NightWave Phase 6V6E — Searchable Lyrics Studio and cumulative A–E review

## Purpose

Extends 6V6D's LRC export/accessibility editor, 6V6C's manual time/text editing,
6V6B's reviewed import, and 6V6A's synchronized display. No NightWave backend
server, new Google scope, licensed lyric catalog, streaming lyric scraping or
audio transcription is added.

## Delivered

### Fast search inside long synchronized lyrics

- **Find a lyric line** expands a compact search panel only when needed; it
  stays collapsed by default so the existing editor has more room on mobile.
- Search operates solely on the **current unsaved LRC draft**, not just the
  original saved lyrics. Any text edit, timestamp correction, deletion or
  Undo recomputes results immediately from the current sorted cue list.
- Searches are case-insensitive, Unicode NFC-normalized, treat runs of
  whitespace consistently and interpret user text as a **literal substring**
  (not a regular expression). Bangla and other Unicode lyrics remain intact.
- Queries of up to **80 characters** search all **1,200 permitted cues**.
  Empty/whitespace queries intentionally show no highlights.
- Time queries such as `00:12`, `00:04.750` and fractional `04.750`
  can match existing formatted timestamps.
- The panel displays a **match count and current match ordinal**, plus
  **Previous match**, **Next match** and **Clear**. Navigation wraps safely.
  A zero-result query disables the navigation controls rather than scrolling
  to a nonexistent row.
- Matching rows receive a distinct, theme-aware background and **MATCH**
  marker. Existing PLAYING and selected-row indicators are preserved.
- Search and selection state are scoped by the offline song ID in
  Compose `rememberSaveable`, so another track cannot reuse the current
  query. Compose may retain UI query input across configuration changes;
  NightWave does not add an application-owned query database or cloud sync.
- Search does not seek playback, alter timing offsets, save an LRC,
  automatically export, or make any network request.

### Cumulative regression protection

`tools/verify_v2_phase6v6e.py` and
`tools/check-phase6v6e-integration.ps1` audit the old and new contracts:

1. 6V6A imported LRC timing and timestamp-offset support.
2. 6V6B canonical parser, import review, privacy and fingerprint-guarded edits.
3. 6V6C in-memory editing, parser round-trip, explicit Save and Undo.
4. 6V6D Android user-selected export, retained editor state and navigation.
5. 6V6E bounded draft search, visual result highlights and safe match navigation.

The new guards are integrated into both the Windows build and the Android CI
workflow **before** the full Gradle tests, and do not weaken existing checks.

### New tests

- **16 JVM `LyricsEditorSearchPolicyTest`** methods covering Unicode/Bangla,
  whitespace/case folding, literal search, timestamps, query limits,
  wrap-around navigation, zero results, edited/reordered cue matches,
  and searching all 1,200 permitted lines.
- **3 Android Compose instrumentation tests** in a disposable test Activity,
  checking match counts, next-match behavior, empty results and Clear. These
  tests do not access personal music or Google accounts.
- Older sync/import/editor/export/navigation tests remain in the suite.
- An isolated local Kotlin/JVM smoke test using the exact 6V6E search policy
  and stubbed cue types passed **17 assertions**, including long lists and
  fractional timestamp lookups. This is a policy compilation/smoke result,
  **not** executed Gradle/JUnit or an Android/Compose build.
- A GitHub source review verified all audited contracts across the existing
  lyrics and build files. That is not a substitute for compiled runtime tests.

## Windows build and Android acceptance

1. Download the complete pinned GitHub source ZIP into a **fresh short
   directory** and run `build-windows.cmd`.
2. Require **BUILD SUCCESSFUL**, the existing `testDebugUnitTest`,
   `assembleDebug`, `compileDebugAndroidTestKotlin` and APK
   package/signature checks.
3. Never install a stale APK from a failed build. Run
   `check-device-update.cmd` before an in-place phone update; do not
   uninstall/clear the existing personal music library.
4. On a disposable emulator, open an imported offline track with user-supplied
   valid LRC; open the editor and **Find a lyric line**.
5. Search case-insensitively and with Bangla text, then use Previous/Next
   match. Verify wrap-around and result highlighting.
6. Search a nonexistent phrase: the result count must say none and
   match navigation must be disabled.
7. Correct a lyric and timestamp; verify search results and indices refresh
   without saving. Undo, add and remove a draft cue; search again.
8. On long 1,200-line test LRC, jump to matches near the end.
   Reduce-motion mode must avoid animated scrolling.
9. Rotate the emulator and switch offline songs; the search/selected cue
   must never attach to another song. A retained unsaved draft must still
   be protected by its original fingerprint and track ID.
10. Regress 6V6A lyric sync/offset, 6V6B replacement preview, 6V6C save and
    discard, 6V6D export, offline playback, 6V5C Music Vault and queue recovery.
11. Report failures using `send-build-report.cmd` without personal media
    files, OAuth credentials, private song paths or unsanitized logs.

## Limitations

This phase does not provide automatic lyric transcription, licensed online
lyrics, multi-language translations or karaoke/per-word timing. Searching
the user's own imported LRC is local and does not verify whether lyrics
belong to the selected song. The edited LRC remains unchanged until
**Save changes**; export still uses an explicit Android document picker.
A process kill can lose an unsaved draft, even though ordinary rotation
has its own retained-state path.

**Full Android/Compose compilation, executed Gradle JVM/instrumentation,
Google Drive and SAF provider behavior, APK signature compatibility and
phone tests must be verified before treating this branch as release-ready.**
