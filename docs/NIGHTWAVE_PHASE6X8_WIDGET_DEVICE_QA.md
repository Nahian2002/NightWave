# NightWave Phase 6X8 — Android Widget & Device Controls

## Branch and preservation
Based on `feature/nightwave-v2-phase6x7-lyrics-follow-fine-sync`, including the prior Equalizer visibility, Sleep Timer dial, streaming/offline Repeat, local gapless/crossfade/normalization, playlist folders, Daily Mixes and synchronized LRC edits. This phase does not change the app ID, signing, Room/legacy library, Google Drive backup schema, downloads, offline media index, queue format or Internet use. No NightWave-owned server.

## Implemented
1. **NightWave home-screen widget:** A compact dark 4×3 launcher widget with high-contrast NightWave branding, currently playing title/artist, Previous, Play/Pause, Next, and tap-to-open app. No online artwork downloads, polling, analytics or new background worker. Root size is 4×3 to avoid clipping touch targets.
2. **Real Media3 transport:** Widget actions bind to the *existing* `PlaybackService` Media3 session through a short-lived asynchronous MediaController; it is not a second player. A 7-second deadline and `BroadcastReceiver.goAsync()` avoid indefinite widget receivers, and `MediaController.releaseFuture` closes the async controller even on failure/cancel.
3. **Queue-safe controls:** Public NightWave streams maintain a one-item player timeline, so the widget sends app-private Media3 session commands for Next/Previous. The playback service routes these to its existing public and offline queue logic. Ordinary Play/Pause uses Media3's public Player commands.
4. **Permissions:** The launcher widget provider is exported only for system widget updates. Control intents target a separate `android:exported="false"` receiver, created by immutable `PendingIntent` objects. The service advertises Next/Previous custom session commands only to controllers with NightWave's app package, and independently checks the sender before executing.
5. **No auto-resume on restart:** A newly created service publishes an empty widget rather than restoring playback or resolving network URLs. The Play/Pause widget does not start an unknown saved queue if the session has no current item. It opens from the widget title; a user can intentionally start/resume a track inside the app.
6. **Bluetooth/headphone output loss:** ExoPlayer's documented `setHandleAudioBecomingNoisy(true)` is enabled on the main player and the temporary crossfade deck. Android can automatically pause when headphones disconnect, instead of suddenly playing through a phone speaker. Existing audio-focus handling and MediaSession media buttons remain preserved.
7. **Transport controls in cars and headsets:** Existing MediaSession standard Android transport interoperability is preserved. **Full Android Auto browsing/search/library certification is NOT part of this phase.** Android Auto content browsing requires a deliberate MediaLibraryService migration with car-emulator testing, and would be unsafe to claim complete without it.

## Tests and evidence
- Local `kotlinc` compilation of the new pure `NowPlayingWidgetPolicy.kt` with **13/13 executed runtime assertions**: empty/stale state, metadata bounds/sanitization, accessible labels and paused/playing state.
- Eight new JVM JUnit source cases in `NowPlayingWidgetPolicyTest.kt`. These are included in `:app:testDebugUnitTest`, but execution is not yet verified.
- Three AndroidJUnit4 instrumentation cases in `NightWaveWidgetIntegrationTest.kt`: exported system provider, private transport receiver and cold widget rendering without starting music. These are compiled by `:app:compileDebugAndroidTestKotlin` in CI but require a device/emulator to execute.
- `tools/verify_v2_phase6x8.py`: Python XML parser verifies actual Android manifest/layout/widget metadata/vector assets, references and source contracts. It also checks the installed receiver boundary, future cleanup, service media keys, noisy handling, no server and historic CI gates.
- `.github/workflows/android.yml` includes the 6X8 branch and gate, then runs Gradle JVM tests, debug APK assembly and Android instrumentation **source compilation**. There is no signed-device-install gate and debug CI signing may differ from the user's installed app.
- **Still unverified:** Full Android Gradle build result, actual Bluetooth route disconnection, widget display on Samsung One UI, lock-screen media controls, Android Auto display, running instrumentation tests and signing compatibility with the existing APK. Do not install before build success.

## Mandatory on-device validation (when user chooses to test)
1. Build with `build-windows.cmd` and require BUILD SUCCESS. Do **not** uninstall NightWave or clear app data to solve signing issues. Use a compatible signing certificate.
2. Long-press the phone home screen -> Widgets -> NightWave -> add its 4×3 widget. Verify title/artist, control readability, no horizontal/vertical clipping at normal and enlarged font scale. Rotate phone; no crash.
3. With no song playing, the widget must say **Ready when you are** and clicking the title opens NightWave. Widget Play must not silently resume a saved/old stream.
4. Play a public streaming song from NightWave and return home. Test widget Pause/Resume, Next/Previous across a 3–4 song queue; verify the expected queue item rather than Media3's single public item. No extra NightWave task or second audio stream should appear.
5. Play an imported offline song in a 3-track queue. Test widget Next/Previous with Shuffle and Repeat modes, plus Phase 6X1/6X2 transitions. The widget must follow song metadata after a gapless or crossfade handoff.
6. Lock screen, use notification media buttons, headset controls and widget in alternation. Confirm no duplicate skip, stale widget state, or queue desynchronization.
7. With wired/Bluetooth headphones playing, **disconnect the output**. Playback should pause rather than jump unexpectedly to speaker. Reconnect and verify it does **not auto-play** without user input. Check speaker-only playback still works normally.
8. Reboot or force-stop through normal Android UI, then add/open widget. Ensure no previously playing track automatically resumes from merely rendering or attaching the widget.
9. Change song title metadata using an existing local library test copy; verify title truncation without crashing. Try 4×3 widget on multiple launcher grid sizes and Android large font.
10. Run Android instrumentation tests on an emulator/device using `:app:connectedDebugAndroidTest` if an appropriate spare test profile is available. Do not run destructive tests on an existing media library.
11. Confirm Sleep Timer expiry stops all audio even with widget present; legacy playlists, lyrics, EQ, local files and Google Drive backups remain intact.

## Limits / deferred work
- Widget transport requires Android's media session to be connectable; a cold widget click does not bypass Android 12+ background start restrictions.
- No waveform/album artwork in widget, no forced polling, no continuous service while idle.
- Android Auto **transport** continues through the existing Media3 session, but browseable catalog, car UI search and Google Auto certification require separate engineering and car-emulator validation.
- Only source/policy tests ran locally. This branch is development source, **not a certified APK**.
