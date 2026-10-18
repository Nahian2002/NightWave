# NightWave Phase 6V6A — Immersive synchronized lyrics

## Scope and existing foundation
Extends 6V5C scalable Google Drive Music Vault, earlier offline playback
reliability, and the established user-imported LRC parser. No NightWave
backend, licensed lyric catalog, web scraping, automatic voice-to-text
transcription, account permissions, streaming-media downloading or remote
cloud changes have been added.

## Implementation
- Imported, timestamped LRC remains in the existing
  `nightwave_private_lrc_v1` preferences and is still subject to strict
  64 KiB / 1,200-cue parser limits, UTF-8 validation and explicit import.
- `LyricsSyncPolicy.kt` calculates active lines at the current playback
  position, optionally delayed/advanced by a user-chosen lyric offset.
  Positive timing makes words appear later; negative makes them appear
  earlier. Timing is bounded to +/-10 seconds in 250 ms increments.
- `PrivateLyricsOffsetStore.kt` saves only the numeric lyric delay by
  validated app-private offline audio ID in a **separate** preferences file,
  `nightwave_private_lrc_offset_v1`. No imported lyrics, offline audio,
  playlists, existing lyric backup payload or cloud snapshot is rewritten.
  A zero delay removes the stored offset entry.
- The existing Now Playing overlay retains its compact Lyrics section and
  adds a **full-screen lyrics experience** for a valid imported offline LRC:
  a vertically scrollable lyric list that follows each active cue, with the
  current line highlighted, song context and direct tap-to-seek.
- User tap-to-seek applies the same per-song offset and clamps the seek to
  known duration. Seeking never silently starts remote audio or replaces a
  playlist item. The Media3 playback controller remains the action owner.
- Full-screen controls include -0.25s, Reset and +0.25s. Reduced-motion
  mode uses an immediate scroll rather than an animated follow transition.
  The immersive panel has controls to return to Now Playing or minimize.
- The Activity loads song-specific LRC and timing from local storage and
  checks the current offline song before applying an asynchronous read or
  save result. Online public-video songs do not receive imported local lyrics.
- All changes are on a separate development branch; the previous 6V5C
  cloud-vault source and Android signing identity are preserved.

## Explicit limitations
- This is *not* real-time lyrics extraction from arbitrary audio. It displays
  user-owned/user-authorized `.lrc` files with timestamps.
- No copyrighted lyric catalog is copied from Spotify or a streaming site.
- Lyric offsets are **device-local**, and are not part of the existing
  optional cloud backup/restore. Imported LRC export behavior is unchanged.
- A track with no imported synchronized LRC still displays Import instructions.
- Follow scrolling is cue-based; there is no per-word karaoke animation,
  lyric translation, or automatic transcription in this phase.

## Added tests
`LyricsSyncPolicyTest.kt` covers zero/positive/negative offset timing,
pre-cue silence, tap-to-seek duration bounds, negative seek clamping, valid
quarter-second settings, extreme input protection, labels, and invalid
offset rejection. Existing `TimedLyricsCodecTest.kt` remains in the suite.

`tools/verify_v2_phase6v6a.py` and
`tools/check-phase6v6a-integration.ps1` provide read-only source hooks and
test discovery. The Windows builder runs the new gate before Gradle. GitHub
Actions retains the full `testDebugUnitTest`, `assembleDebug`, and
`compileDebugAndroidTestKotlin` tasks.

An **isolated Kotlin 1.9 JVM smoke check** was performed on local copies
of the LRC/offset timing code and passed representative timing/seek cases.
This is *not* a full Android build, Compose UI compilation, a JUnit run or
an APK acceptance result.

## Integration/phone acceptance (after successful full Gradle build)
1. Extract one pinned full-source ZIP in a fresh directory.
2. Run `build-windows.cmd`; require BUILD SUCCESSFUL, all JVM test tasks,
   Android test-source compilation, APK package/identity and signer checks.
3. Run `check-device-update.cmd` before installing over an existing
   NightWave app. Never uninstall or clear local app data as a workaround.
4. Play a personally owned **offline** MP3 and explicitly import a short
   timestamped LRC file. Open Now Playing > Lyrics > Full Screen.
5. Confirm highlighted lyric follows playback and the list scrolls
   appropriately at each timestamp. Pause/resume and scrub the song.
6. Tap an earlier/later line: the song should seek to the same line's
   timestamp. Reduce-motion mode must not animate the auto-follow scroll.
7. Adjust +0.25 and -0.25 seconds; confirm highlighting and seek times
   both change consistently. Try +/-10-second limits and Reset.
8. Close and reopen the app; confirm the offset remains attached to the
   same song. Switch to another song and confirm no previous offset leaks.
9. Replace/remove LRC and verify the existing import, removal and optional
   cloud lyric backup functionality remains unaffected.
10. Verify public-video tracks do **not** display misleading local lyric
    captions or attempt to fetch streaming lyrics automatically.
11. Smoke-test earlier queue restoration, offline files, Drive inventory
    paging/upload/download and transfer history for regressions.

## Verification state
Source implementation and static review complete. Complete Windows Gradle
build, JUnit execution, Android Compose rendering, Media3 device seek, APK
signature comparison and on-phone tests are **still pending**. Do not install
a previously failed or stale APK.
