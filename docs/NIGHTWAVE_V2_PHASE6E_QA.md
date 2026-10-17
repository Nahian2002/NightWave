# NightWave Phase 6E — Playback Health & Safe Controller Continuity

**Branch:** `feature/nightwave-v2-phase6e-playback-health-continuity` stacked on Phase 6D. This is a **source implementation in a draft branch**, not a verified Android release. The user requested postponing builds and phone tests.

## What changed

### Reliable, consistent playback phases
- Added a pure `PlaybackHealthPolicy`, classifying the currently observed Media3/bridge state as **Disconnected**, **Playback needs attention**, **Nothing playing**, **Finding audio**, **Buffering audio**, **Playing**, or **Paused or ready**. Precedence ensures errors and disconnections aren't mislabeled as buffering or playing.
- Separated **`resolving`** (public stream extraction pending) from Media3 **`buffering`** (a stream was handed to the player but isn't ready). The existing bridge retains its dynamic pending marker from Phase 6D; a new state field exposes lookup as a distinct condition.
- The player mini-card subtitle and the full Now Playing loading label now use the same policy, so **Finding audio** is not mistaken for local file buffering. The mini-player X/swipe gestures, repeat/shuffle, queue and song metadata remain in place.
- These are interface descriptions, **not telemetry**, and they do not claim anything about unknown third-party extractor availability or network reliability.

### One-shot, foreground-only media-controller recovery
- The Activity-owned MediaController already supported explicit Reconnect and a single attempt upon foreground return. Phase 6E adds **one delayed (1.2s) recovery attempt** after an established controller disconnects *while the UI is actively visible*.
- A budget flag allows one such attempt per foreground lifecycle; it is consumed before calling connect, and no repeated timer is started on failure. Returning to the app on a later foreground event permits the existing bounded attempt.
- `onStop`/UI pause and `release` remove the scheduled task. Callbacks also guard `released`, current `controller`, `future`, and the visible `ticking` flag.
- Automatic recovery merely **rebinds the controller**; it never selects a song, rewrites a saved queue, extracts a stream, seeks, resumes or autoplays. If connection remains unavailable, the UI retains a manual Reconnect action.

### You > App health > Playback Health
- Added a compact live panel containing the **observed playback phase**, a plain-language description, source category (imported local audio or public stream), and current queue count. It never shows a watch URL, signed audio URL, local path, personal profile ID, device information or cloud details.
- When disconnected or in a failed state, the user can explicitly choose **Reconnect controls** or **Retry selected track**. No background health uploads and no additional permissions.
- Existing **Crash diagnostics** and classic UI escape hatch are preserved.

### Preserved boundaries
The old Media3 PlaybackService remains sole playback owner. Phase 6D's bounded extraction, 35-second timeout, generation guard and pending Pause behavior remain; no service or data-store migration, analytics event recorder, cloud archive schema or Google OAuth change was introduced. Phase 6B lyrics, 6C/6C2 private opt-in memory, Phase 6A2 mini-player hide/restore, local audio and playlists remain untouched.

## Deferred QA — NOT executed
1. Run `tools/verify_v2_phase6e.py` and all inherited source gates. Compile the app with `build-windows.cmd`, Gradle JVM tests and `:app:compileDebugAndroidTestKotlin`; fix all compiler/test failures before any APK installation.
2. Run the **8 authored `PlaybackHealthPolicyTest` pure JVM cases**: disconnection precedence, errors during buffering, idle state, public lookup, local file buffering, true Media3 buffering, play/pause, and no private details in the health message.
3. On an emulator, simulate controller loss while **visible**: verify at most one delayed rebind attempt and no playback command, queue mutation, seek or automatic stream extraction. Repeat with onStop and destroyed Activity to ensure pending reconnect never runs. After failure, manual Reconnect remains.
4. With public online playback, inspect progression: **Finding audio** during extractor resolution, then **Buffering audio** when Media3 buffers, then **Playing** when audio starts. Force a 35-second 6D lookup timeout and verify **Playback needs attention** and Retry. For imported offline audio, never display **Finding audio**.
5. Verify mini-player, full Now Playing and You > App health show consistent status when paused, playing, disconnected, errored and without any track. Confirm accurate queue count and that no URLs, tokens or local file paths appear in panel text.
6. Test display width 320/360/412dp, tablet and landscape, accessibility font sizes 1.0–1.8, TalkBack, dark/light/AMOLED and reduced motion. Verify health panel doesn't push destructive controls off screen and no overlay obscures Retry.
7. Regress 6D fast track switching/timeout/Repeat One, mini-player X and swipe-down hide (must never stop audio), user Pause during extraction, notification transport, previous song restore, local file import and lyrics, private listening-memory erase/opt-out, playlists and backup/merge.
8. Verify unchanged application ID/version/signing. Check in-place update compatibility via `check-device-update.cmd`; **do not uninstall or clear existing NightWave user data to work around a signature mismatch**.

**Result so far:** source implementation and future tests authored, Android compilation and device QA **not yet performed**. Keep draft and don't distribute as a validated APK.
