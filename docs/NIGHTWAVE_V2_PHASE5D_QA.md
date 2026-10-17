# NightWave 2.0 — Phase 5D Offline Session Recovery

**Branch**: `feature/nightwave-v2-phase5d-offline-resume`  
**Version**: 2.0.0-alpha5d / versionCode 20  
**Status**: Draft source implementation. Android compilation, instrumentation and phone QA **not verified**.

## What changed
- Adds an app-private **offline listening checkpoint** in separate `nightwave_offline_session_v1` preferences. Saves only validated IDs of previously imported tracks, list order, last selected ID and playback position, not file paths or stream URLs.
- Encodes a capped 400-track session with a version marker, bounded playback time and SHA-256 corruption check. Invalid, truncated, or unknown-version sessions fail closed without mutating the imported audio index.
- Media3 checkpoints offline positions every 2.5 seconds during actual playback, with synchronous flush on pause, service destruction and switching back to online content.
- The saved offline queue does **not** start itself at service creation. Users explicitly resume from either Offline library's **Resume** control or the premium Library's **Continue Listening** card. Resume validates current files and skips missing later tracks in bounded order.
- The premium Up Next sheet displays imported song names and permits direct jumping; offline shuffle/repeat, YouTube favorites and playlist-save actions stay disabled rather than modifying online-only playback state.
- Retains Phase 5C search, sorting, Play All, sequential offline Next/Previous and pre-existing YouTube playback, local profiles, library index and Room shadow.
- Includes six pure JVM decoder tests and an extra queue-jump test, plus `tools/verify_v2_phase5d.py`. Previous phase checkers updated to accept code 20.

## Limits and safeguards
- No YouTube audio downloads, no external document path persistence, no cloud sync and no automatic playback on app startup.
- This is an **app-private** checkpoint; clearing app data or uninstalling NightWave removes it and any imported files. Existing external backups of favorites/playlists do not include offline files.
- Device OS/permission termination may lose up to the most recent progress checkpoint. It is not a 100% seamless cross-process audio engine.
- If a restored selected file was deleted outside NightWave, Resume will not start it. Already-imported files remain untouched; an unavailable *later* track is skipped.
- SHA-256 protects against accidental preference corruption; it is not a security signature for a hostile local device.
- Offline list has linear order only; shuffle and repeat intentionally remain disabled until designed specifically for local audio. No preference migration or new permission is required.
- The previously observed Android crash warning is still **not diagnosed** without an Android FATAL EXCEPTION / ANR trace.

## Verification achieved
- Compiled a matching standalone copy of `OfflineSessionCodec` and `OfflinePlaybackQueue` with the installed Kotlin CLI compiler, and ran 15 focused checks for valid 400-item round trip, corruption, malicious path rejection, limits, position, ordering and jump. The remote repository JUnit suite was **not executed**.
- Source-level inspection and dedicated static gate added. This is not proof of Android/Compose/Media3 runtime compatibility.

## Required Windows / Android tests
1. Extract the Phase 5D source ZIP into a clean directory. Run `build-windows.cmd` to execute Gradle unit tests and build a debug APK. On failure inspect `dist/NightWave-build-errors.txt` and `dist/NightWave-build-full.log`. Do not install a failed build or uninstall an existing app.
2. Optional source-only gate: `python tools/verify_v2_phase5d.py`.
3. Install only if BUILD SUCCESS and signing is compatible with the existing debug app. Do not clear app data.
4. Import 3 personally owned audio files; play the second offline with Wi-Fi off, seek to a known time, pause, close the app and reopen. Tap Resume in Offline library and confirm the song and seek position.
5. While playing, verify Android media notification, lock-screen controls and Bluetooth Next/Previous. Switch to a public YouTube song; verify the old offline checkpoint can still resume on demand and streaming bookmarks are separate.
6. Open premium Library and use Continue Listening. In premium Up Next, tap a different imported song, then restart the app and use Resume again. Offline favorite, playlist, repeat, shuffle controls should not modify the online-only collections.
7. Remove the last offline song via the existing confirmation, then confirm Resume hides gracefully if its selected ID is no longer present.
8. Simulate a process kill only on a disposable test profile; verify no unprompted audio on app reopen, and the explicit Resume still works. Test storage failure/corrupt session preferences only on a disposable emulator, never personal data.
9. If the phone reports crashes, use `capture-nightwave-crash.cmd` and share the redacted `FATAL EXCEPTION` trace.

Keep the PR DRAFT, and do not merge into Phase 5C or main until full build and device acceptance.
