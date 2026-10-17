# NightWave 2.0 — Alpha 5F Player Motion & Safety

**Version:** 2.0.0-alpha5f (code 22)  
**Branch:** `feature/nightwave-v2-phase5f-player-motion`  
**Status:** Draft. Source implementation, not a verified Android APK.

## Implemented

- Swipe horizontally across the **full-screen artwork** to request Next/Previous when a player is connected, not buffering, and at least two tracks are in the current queue. An 88dp threshold ignores small accidental gestures. Swipes only call existing Media3 controller actions; they do not use arbitrary URLs or write playlists.
- The full-screen artwork changes with a short Compose Crossfade, disabled under the user's Reduced Animations preference. The visual fallback remains visible even when a non-empty upstream artwork URL fails.
- Subtle theme-derived player backdrop and a visible on-artwork buffering indicator; source-sensitive **NOW PLAYING** / **OFFLINE AUDIO** heading.
- **Next** on the compact mini-player when a multi-track queue exists, without replacing tap-to-expand or drag up/down-to-expand/hide. More compact full-screen transport controls on narrow screens.
- A direct **Retry playback** action in the full-screen player when the existing controller reports an error.
- Seek/scrub state is remembered **per track**, not across song changes. A pure Kotlin progress policy bounds requests, avoids NaN/Infinity, and rejects unknown duration. The mini-player's progress indicator uses the same safe fraction.
- Queue sheet and offline separation from Phase 5D are preserved. Shuffle, Repeat, Favorites and Playlist actions remain disabled for imported offline files.

## Regression boundaries and limitations

- This change does **not** modify Media3 playback service, YouTube gateway, offline file index, legacy playlist storage, profile/Room migration, crash handler or Android manifest.
- Gestures are a convenience; Next/Previous buttons and headset commands remain primary and available. At the first or last track, actual behavior follows the existing queue/repeat rules; a swipe cannot invent another playable track.
- Scrubbing while duration is unknown is disabled. Playback services and upstream YouTube endpoints may still fail independently.
- **The earlier recurring Android crash warning remains unverified and its root cause is not proven fixed.** A visual change must never be presented as a crash repair.

## Validation achieved

- The pure `PlayerGestureRules.kt` and `PlayerProgressRules.kt` logic compiled under Kotlin CLI in an isolated smoke runner and passed **23 behavior assertions**, including zero/unknown duration, large positions, NaN/Infinity, threshold boundaries, buffering, disconnected states and single-item queues. The repository Gradle/JUnit suites have not been executed.
- Added six gesture JVM JUnit tests and four progress JVM JUnit tests; added `tools/verify_v2_phase5f.py` source gate and updated prior phase version gates.
- Reviewed repository source wiring for horizontal swipe action routing, player UI state, reduced-motion behavior, offline action safeguards, and one launcher. This does **not** verify Android Compose compilation, touch dispatch or layout.

## Windows and real-device acceptance

1. Download/extract a **fresh copy** of Phase 5F's complete ZIP. Run `build-windows.cmd`. It must complete `:app:testDebugUnitTest` and `:app:assembleDebug` before an APK is considered. If it fails, send `dist/NightWave-build-errors.txt` and the relevant compiler lines of `dist/NightWave-build-full.log`.
2. Optional source-only check: `python tools/verify_v2_phase5f.py`. Passing this is not an Android build.
3. Install only if BUILD SUCCESS and signing is compatible with the existing app. Never uninstall NightWave or clear storage to work around signature/build failures.
4. On your Galaxy S24 Ultra, test portrait and split-screen/narrow layout, font scaling, 4 NightWave themes, and Reduced Animations.
5. Select a public queue of at least 3 songs. Open the full player, perform a short sideways drag (must not skip), then an intentional left and right drag across the artwork (must request Next and Previous). Swipe up/down elsewhere to check normal scrolling; drag down on header to collapse.
6. Test mini-player upward/downward gesture, tap-to-expand, progress line, play/pause, and **Next**. Verify the next button doesn't clip the title or safe edges.
7. While scrubbing, change to a different track. The next track must not inherit the previous track's unfinished seek. Try unknown duration, slow buffering, and an unavailable video; Retry must not start an infinite loop.
8. Import a personal MP3 and verify offline controls, saved explicit Resume from Phase 5D, artwork placeholder, background controls and Up Next still work. The offline song must not be favorited or pushed into an online-only playlist.
9. Check Bluetooth/headset lock-screen controls, player state after the app is backgrounded/reopened, full-screen Back handling, and screen reader control labels. Avoid running destructive Android instrumentation on a phone with important saved music.
10. If NightWave still reports frequent crashes, use `capture-nightwave-crash.cmd` and share a reviewed/redacted FATAL EXCEPTION or ANR trace so the actual cause can be isolated.

## Release gate

Keep this PR **DRAFT** and stable main unchanged until Android compilation, device playback/navigation and crash-log review pass. This phase is not yet a release candidate.
