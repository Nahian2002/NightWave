# NightWave 2.0 Alpha 4 — UI, premium-startup and offline-library repair

**Version:** 2.0.0-alpha4-hotfix2 (code 16)
**Branch:** feature/nightwave-alpha4-ui-offline-repair
**Status:** Source committed. Android Gradle build and physical-phone tests NOT yet executed here.

## User-reported issues
- Header branding said "nightwave." and had a decorative "NIGHT MODE" badge, stealing width on smaller/high-font-size screens.
- Classic Home/Search/Library tabs, cards and Now Playing strip covered too much of the phone screen.
- "Explore the new premium interface" returned to Android home screen, consistent with a runtime crash. Without an Android crash trace, that outcome is not a confirmed diagnosis.
- User wants durable offline audio playback.

## Implemented
- Classic header now says **NightWave**, with responsive text sizing, colored "Wave" and reduced padding; the decorative night-mode badge is removed.
- Classic nav is 56dp with concise tab names and a selected pill. Home/library tiles and hero layout are smaller.
- Classic Now Playing is compact: redundant controls moved to existing expanded Now Playing dialog. Collapse control and downward artwork swipe hide the strip; a 33dp Now Playing pill restores it, without pausing audio.
- Compose header uses the same two-tone **NightWave** wordmark.
- Corrected a **real Compose color API bug**: ARGB palette longs (e.g. 0xFF090D14) must become \`Color(raw.toInt())\`, not \`Color(raw.toULong())\`. The latter is an already-packed Compose color representation with color-space encoding, and might explain the premium crash. Actual cause remains unconfirmed until logcat.
- The previous classic Activity remains the sole Android launcher; premium remains available as an opt-in preview from Home until verified.
- New **Offline library** is reachable from classic Home/Library and Compose Library. Users select one or more local audio files in the Android document picker. Files are copied into NightWave's app-private storage and indexed for later playback without internet.
- The Media3 session adds a local-file datasource and a separate offline playback action; it does not try to automatically skip a YouTube queue when offline playback finishes.
- Supported import formats by extension: MP3, M4A, AAC, FLAC, WAV, OGG, OPUS, MP4, WebM and 3GP. Actual codec decoding depends on Android/Media3 support. 250 MiB maximum per file, 3 GiB library maximum, 400 entries. The original selected files are never deleted.
- No new broad external storage permissions. Imported copies are private to NightWave and will be removed if app data is cleared or the app is uninstalled.
- **YouTube audio downloading is not implemented.** Do not label YouTube streaming favorites as available offline; video URLs and signed transient streams are not stored as downloadable offline media.
- Existing local likes/playlists, Room shadow database and profile preferences retain their original namespaces. No destructive migration or sign-in is active.

## Build / local verification
1. Extract the ZIP into a clean directory. Run \`build-windows.cmd\`.
2. Optionally run \`python tools/verify_alpha4_ui_offline.py\` to check XML/resources, wiring and offline source.
3. If Gradle fails, share \`dist/NightWave-build-errors.txt\`. Do not uninstall or clear an existing installation.
4. Install \`dist/NightWave-debug.apk\` over the current signed debug package **only if BUILD SUCCESS**.
5. Connected Android instrumentation tests require a disposable profile/emulator. The broader preexisting test suite contains tests that clear storage; do not run it against a personal library.

## Phone acceptance tests
- Launch: classic Home opens without crash. Check wordmark fits within the device width, no night-mode badge, navigation clear.
- Open Library with 0 and multiple playlists. Ensure content isn't covered by the player.
- Play a YouTube video; Now Playing should be compact. Tap cover/title to expand. Swipe down on artwork or tap collapse, then restore via Now Playing pill. Music must continue when panel is hidden.
- Open new premium preview through Home; verify it doesn't exit to Android home. If it still fails, capture \`NightWave-crash.log\` with the existing \`capture-nightwave-crash.cmd\` script and share FATAL EXCEPTION stack.
- Enter Offline library. Import a personally owned MP3 or FLAC. Check file shows in list. Turn on Airplane mode and play it. Lock screen and verify Media3 controls. Reboot app and verify file remains in offline library.
- Delete an offline copy after confirmation; original document must remain.
- Try an invalid extension and overly large file on a disposable profile and confirm no saved entry.
- Reopen existing playlists and likes; they should be unchanged.
- Change Android font/display size and check branded header/navigation don't clip.

## Regression gates
- Three Android instrumentation tests for imported file retention, invalid extension rejection and deletion without touching the original, plus one Android palette test covering all four NightWave themes.
- Static source gate at \`tools/verify_alpha4_ui_offline.py\`.
- Full Android/Kotlin/KSP build and physical phone acceptance are pending. Do not merge to stable \`main\` until complete.
